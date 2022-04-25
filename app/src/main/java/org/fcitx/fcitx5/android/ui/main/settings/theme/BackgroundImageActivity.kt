package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.MenuItem
import android.widget.SeekBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.ThemePreset
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.darkenColorFilter
import splitties.dimensions.dp
import splitties.views.dsl.core.*
import splitties.views.gravityCenter
import splitties.views.gravityEnd
import splitties.views.horizontalPadding
import java.io.File

class BackgroundImageActivity : AppCompatActivity() {

    class Contract : ActivityResultContract<Unit, String?>() {
        override fun createIntent(context: Context, input: Unit): Intent =
            Intent(context, BackgroundImageActivity::class.java)

        override fun parseResult(resultCode: Int, intent: Intent?): String? =
            intent?.getStringExtra(RESULT)
    }

    private lateinit var preview: KeyboardPreviewUi

    val brightnessLabel by lazy {
        textView {
            setText(R.string.brightness)
        }
    }

    val brightnessValue by lazy {
        textView {
            gravity = gravityEnd
        }
    }

    val brightness by lazy {
        horizontalLayout {
            horizontalPadding = dp(16)
            add(brightnessLabel, lParams())
            add(brightnessValue, lParams(width = matchParent))
        }
    }

    private val brightnessSeekBar by lazy {
        seekBar {
            max = 100
        }
    }

    private val finishButton by lazy {
        button {
            setText(R.string.done)
        }
    }

    private val ui by lazy {
        verticalLayout {
            gravity = gravityCenter
            add(preview.root, lParams(wrapContent, wrapContent))
            add(brightness, lParams(width = dp(300)))
            add(brightnessSeekBar, lParams(width = dp(300)))
            add(finishButton, lParams())
        }
    }

    private lateinit var launcher: ActivityResultLauncher<CropImageContractOptions>

    private lateinit var image: BitmapDrawable

    private lateinit var cropped: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preview = KeyboardPreviewUi(this, ThemePreset.PreviewDark)
        setContentView(ui)
        launcher = registerForActivityResult(CropImageContract()) {
            if (!it.isSuccessful)
                cancel()
            else {
                cropped = it.getBitmap(this)!!
                image = BitmapDrawable(resources, cropped)
                brightnessSeekBar.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(bar: SeekBar) {}
                    override fun onStopTrackingTouch(bar: SeekBar) {}
                    @SuppressLint("SetTextI18n")
                    override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                        brightnessValue.text = "$progress%"
                        image.colorFilter = darkenColorFilter(100 - progress)
                        preview.setBackground(image)
                    }
                })
                brightnessSeekBar.progress = 70
                finishButton.setOnClickListener {
                    done()
                }
            }
        }
        launcher.launch(options {
            setGuidelines(CropImageView.Guidelines.ON)
            setImageSource(includeGallery = true, includeCamera = false)
            setAspectRatio(preview.intrinsicWidth, preview.intrinsicHeight)
            setOutputCompressFormat(Bitmap.CompressFormat.PNG)
        })
    }

    private fun cancel() {
        setResult(Activity.RESULT_CANCELED, Intent().apply { putExtra(RESULT, null as String?) })
        finish()
    }

    private fun done() {
        val bitmap = Bitmap
            .createBitmap(preview.intrinsicWidth, preview.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(cropped, null, Rect(0, 0, canvas.width, canvas.height), Paint().apply {
            colorFilter = darkenColorFilter(100 - brightnessSeekBar.progress)
        })
        val file = File.createTempFile(
            "img", ".png",
            appContext.getExternalFilesDir(null)
        )
        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        setResult(Activity.RESULT_OK, Intent().apply { putExtra(RESULT, file.path) })
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        cancel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                cancel()
                return true
            }
        }
        return false
    }

    companion object {
        const val RESULT = "result_file_path"
    }
}