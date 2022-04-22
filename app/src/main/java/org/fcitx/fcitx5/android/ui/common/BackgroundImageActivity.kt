package org.fcitx.fcitx5.android.ui.common

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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.keyboard.TextKeyboard
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.darkenColorFilter
import org.fcitx.fcitx5.android.utils.keyboardWindowAspectRatio
import splitties.dimensions.dp
import splitties.resources.color
import splitties.resources.styledColorSL
import splitties.views.backgroundColor
import splitties.views.dsl.core.*
import splitties.views.dsl.material.slider
import splitties.views.gravityCenter
import java.io.File

class BackgroundImageActivity : AppCompatActivity() {

    class Contract : ActivityResultContract<Unit, String?>() {
        override fun createIntent(context: Context, input: Unit): Intent =
            Intent(context, BackgroundImageActivity::class.java)

        override fun parseResult(resultCode: Int, intent: Intent?): String? =
            intent?.getStringExtra(RESULT)
    }

    private val text by lazy {
        textView {
            textSize = 15f
            setText(R.string.brightness)
        }
    }

    private val fakeKeyboard by lazy {
        verticalLayout {
            add(frameLayout {
                backgroundColor = color(R.color.darken_background)
            }, lParams(matchParent, dp(40)))
            add(TextKeyboard(context), lParams(matchParent, matchParent))
            scaleX = .8f
            scaleY = .8f
        }
    }

    private val brightness by lazy {
        slider {
            valueFrom = 0f
            valueTo = 100f
            stepSize = 1f
            val colorAccent = styledColorSL(android.R.attr.colorAccent)
            thumbTintList = colorAccent
            haloTintList = colorAccent
            trackTintList = colorAccent
            trackInactiveTintList = styledColorSL(android.R.attr.colorButtonNormal)
            isTickVisible = false
            setLabelFormatter {
                "${it.toInt()}%"
            }
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
            add(fakeKeyboard, lParams(inputViewWidth, inputViewHeight))
            add(text, lParams())
            add(brightness, lParams(width = dp(300)))
            add(finishButton, lParams())
        }
    }

    private var inputViewWidth = 0
    private var inputViewHeight = 0

    private lateinit var launcher: ActivityResultLauncher<CropImageContractOptions>

    private lateinit var image: BitmapDrawable

    private lateinit var cropped: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val (x, y) = keyboardWindowAspectRatio()
        inputViewWidth = x
        // bar height
        inputViewHeight = y + dp(40)
        // bottom padding
        ViewCompat.getRootWindowInsets(this.fakeKeyboard)?.let {
            inputViewHeight += it.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        }
        setContentView(ui)
        launcher = registerForActivityResult(CropImageContract()) {
            if (!it.isSuccessful)
                cancel()
            else {
                cropped = it.getBitmap(this)!!
                image = BitmapDrawable(resources, cropped)
                brightness.addOnChangeListener { _, value, _ ->
                    image.colorFilter = darkenColorFilter(100 - value.toInt())
                    fakeKeyboard.background = image
                }
                brightness.value = 70f
                finishButton.setOnClickListener {
                    done()
                }
            }
        }
        launcher.launch(options {
            setGuidelines(CropImageView.Guidelines.ON)
            setImageSource(includeGallery = true, includeCamera = false)
            setAspectRatio(inputViewWidth, inputViewHeight)
            setOutputCompressFormat(Bitmap.CompressFormat.PNG)
        })

    }

    private fun cancel() {
        setResult(Activity.RESULT_CANCELED, Intent().apply { putExtra(RESULT, null as String?) })
        finish()
    }

    private fun done() {
        val bitmap = Bitmap.createBitmap(inputViewWidth, inputViewHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(
            cropped, null, Rect(0, 0, inputViewWidth, inputViewHeight),
            Paint().apply {
                colorFilter = darkenColorFilter(100 - brightness.value.toInt())
            }
        )
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