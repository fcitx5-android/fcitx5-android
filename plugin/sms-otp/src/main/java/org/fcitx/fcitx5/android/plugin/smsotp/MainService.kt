package org.fcitx.fcitx5.android.plugin.smsotp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.util.regex.Pattern
import org.fcitx.fcitx5.android.common.FcitxPluginService
import org.fcitx.fcitx5.android.common.ipc.FcitxRemoteConnection
import org.fcitx.fcitx5.android.common.ipc.bindFcitxRemoteService

fun extractOtp(message: String?): String? {
    if (message == null) return null
    val pattern = Pattern.compile(
        "(?<!(\\d|\\d.|\\d\\s+|\\/))(\\d{4,8}|\\d{3}-\\d{3})(?!(\\d|.\\d|\\s+\\d|\\/|\\-))"
    )
    val matcher = pattern.matcher(message)
    return if (matcher.find()) matcher.group(2).replace("-", "") else null
}

class MainService : FcitxPluginService() {

  private lateinit var connection: FcitxRemoteConnection

  override fun start() {
    registerSmsReceiver(this)
    connection =
            bindFcitxRemoteService(BuildConfig.MAIN_APPLICATION_ID) { log("Bind to fcitx remote") }
  }

  override fun stop() {
    unregisterSmsReceiver(this)
    unbindService(connection)
    log("Unbind from fcitx remote")
  }
  private fun log(msg: String) {
    Log.d("SmsOtpService", msg)
  }

  private var receiver: BroadcastReceiver? = null

  private fun registerSmsReceiver(context: Context) {
    if (receiver != null) return
    receiver =
            object : BroadcastReceiver() {
              override fun onReceive(context: Context, intent: Intent) {
                val messages = android.provider.Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (sms in messages) {
                  val message = sms.messageBody
                  val code = extractOtp(message)
                  if (code != null) {
                    // showToast(context, "OTP Detected: $code")
                    tryInputOtp(context, code)
                  }
                }
              }
            }
    val filter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    } else {
      context.registerReceiver(receiver, filter)
    }
  }

  private fun unregisterSmsReceiver(context: Context) {
    receiver?.let {
      context.unregisterReceiver(it)
      receiver = null
    }
  }

  private fun showToast(context: Context, text: String) {
    Handler(Looper.getMainLooper()).post { Toast.makeText(context, text, Toast.LENGTH_LONG).show() }
  }

  private fun tryInputOtp(context: Context, code: String) {
    try {
      connection.remoteService?.updateOtp(code)
    } catch (e: Exception) {
      log("updateOtp failed")
    }
  }
}
