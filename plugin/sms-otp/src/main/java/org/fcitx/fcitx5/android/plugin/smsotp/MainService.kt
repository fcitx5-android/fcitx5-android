package org.fcitx.fcitx5.android.plugin.smsotp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import java.util.regex.Pattern
import org.fcitx.fcitx5.android.common.FcitxPluginService
import org.fcitx.fcitx5.android.common.ipc.FcitxRemoteConnection
import org.fcitx.fcitx5.android.common.ipc.bindFcitxRemoteService

fun extractOtp(message: String?): String? {
  if (message == null) return null
  val pattern =
    Pattern.compile("""(?<!(\d|\d.|\d\s{1,4}|/))(\d{4,8}|\d{3}-\d{3})(?!(\d|\.\d|\s+\d|/|-))""")
  val matcher = pattern.matcher(message)
  val results = mutableListOf<String>()
  val codePositions = mutableListOf<Pair<String, Int>>()
  while (matcher.find()) {
    val code = matcher.group(2).replace("-", "")
    results.add(code)
    codePositions.add(Pair(code, matcher.start(2)))
  }
  if (results.size > 1) {
    val keywords = listOf("code", "otp", "码", "碼", "コード", "코드", "код", "kodo")
    val keywordPositions = keywords.flatMap { keyword ->
      Regex(Regex.escape(keyword), RegexOption.IGNORE_CASE)
        .findAll(message)
        .map { it.range.first }
        .toList()
    }
    if (keywordPositions.isNotEmpty()) {
      // find the nearest one
      val nearest = codePositions.minByOrNull { (code, pos) ->
        keywordPositions.minOf { kp -> kotlin.math.abs(pos - kp) }
      }
      return nearest?.first
    } else {
      return results.firstOrNull()
    }
  }
  else {
    return results.firstOrNull()
  }
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
              log("OTP Detected: $code")
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

  private fun tryInputOtp(context: Context, code: String) {
    try {
      connection.remoteService?.updateOtp(code)
    } catch (e: Exception) {
      log("updateOtp failed")
    }
  }
}
