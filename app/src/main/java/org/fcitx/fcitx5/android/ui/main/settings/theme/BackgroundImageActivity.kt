package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.SeekBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import kotlinx.parcelize.Parcelize
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.ThemePreset
import org.fcitx.fcitx5.android.utils.darkenColorFilter
import splitties.dimensions.dp
import splitties.views.dsl.core.*
import splitties.views.gravityCenter
import splitties.views.gravityEnd
import splitties.views.horizontalPadding
import java.io.File

class BackgroundImageActivity : AppCompatActivity() {


    @Parcelize
    data class Result(val theme: Theme.Custom, val newCreated: Boolean) : Parcelable

    class Contract : ActivityResultContract<Theme.Custom?, Result?>() {
        override fun createIntent(context: Context, input: Theme.Custom?): Intent =
            Intent(context, BackgroundImageActivity::class.java).apply {
                putExtra(ORIGIN_THEME, input)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): Result? =
            intent?.getParcelableExtra(RESULT)
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
    private lateinit var src: Bitmap
    private lateinit var cropped: Bitmap

    private lateinit var theme: Theme.Custom
    private lateinit var srcImageFile: File
    private lateinit var croppedImageFile: File

    // TODO
    private val Theme.Custom.background
        get() = backgroundImage
            ?: throw IllegalStateException("Custom theme only supports backgroundImage for now")

    private var newCreated = true

    private fun setDark(isDark: Boolean) {
        if (theme.isDark == isDark)
            return

        theme = if (isDark)
            ThemePreset.PreviewDark.deriveCustomBackground(
                theme.name,
                theme.background.first,
                theme.background.second
            ) else
            ThemePreset.PreviewLight.deriveCustomBackground(
                theme.name,
                theme.background.first,
                theme.background.second
            )
        preview.setTheme(theme)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val originTheme = intent?.getParcelableExtra<Theme.Custom>(ORIGIN_THEME)?.also {
            it.background.run {
                croppedImageFile = File(first)
                srcImageFile = File(second)
            }
        }
        originTheme?.let {
            theme = it
            newCreated = false
        } ?: run {
            val (n, c, s) = ThemeManager.newCustomBackgroundImages()
            croppedImageFile = c
            srcImageFile = s
            theme = originTheme ?: ThemePreset.PreviewDark.deriveCustomBackground(
                n, c.path, s.path
            )
        }
        preview = KeyboardPreviewUi(this, theme)
        setContentView(ui)
        launcher = registerForActivityResult(CropImageContract()) {
            if (!it.isSuccessful)
                cancel()
            else {
                @Suppress("DEPRECATION")
                src = when {
                    Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
                        contentResolver,
                        it.originalUri!!
                    )
                    else -> {
                        val source = ImageDecoder.createSource(contentResolver, it.originalUri!!)
                        ImageDecoder.decodeBitmap(source)
                    }
                }
                cropped = it.getBitmap(this)!!
                image = BitmapDrawable(resources, cropped)
                brightnessSeekBar.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(bar: SeekBar) {}
                    override fun onStopTrackingTouch(bar: SeekBar) {}

                    @SuppressLint("SetTextI18n")
                    override fun onProgressChanged(
                        bar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
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
        launcher.launch(options(srcImageFile.takeIf { it.exists() }?.toUri()) {
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
        val bitmap = createBitmap(preview.intrinsicWidth, preview.intrinsicHeight)
        bitmap.applyCanvas {
            drawBitmap(
                cropped,
                null,
                Rect(0, 0, width, height),
                Paint().apply {
                    colorFilter = darkenColorFilter(100 - brightnessSeekBar.progress)
                })
        }
        croppedImageFile.delete()
        srcImageFile.delete()
        croppedImageFile.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        srcImageFile.outputStream().use {
            src.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        setResult(
            Activity.RESULT_OK,
            Intent().apply {
                putExtra(
                    RESULT,
                    Result(theme, newCreated)
                )
            })
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
        const val ORIGIN_THEME = "origin_theme"
    }
}