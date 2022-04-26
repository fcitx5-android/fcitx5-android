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
import android.view.ViewOutlineProvider
import android.widget.SeekBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.view.updateLayoutParams
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
import splitties.resources.resolveThemeAttribute
import splitties.resources.styledColor
import splitties.views.backgroundColor
import splitties.views.bottomPadding
import splitties.views.dsl.appcompat.switch
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.dsl.core.styles.AndroidStyles
import splitties.views.gravityVerticalCenter
import splitties.views.textAppearance
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

    private fun createTextView(@StringRes string: Int? = null) = textView {
        if (string != null) {
            setText(string)
        }
        gravity = gravityVerticalCenter
        textAppearance = resolveThemeAttribute(R.attr.textAppearanceListItem)
    }

    val variantLabel by lazy {
        createTextView(R.string.dark_keys)
    }
    val variantSwitch by lazy {
        switch { }
    }

    val brightnessLabel by lazy {
        createTextView(R.string.brightness)
    }
    val brightnessValue by lazy {
        createTextView()
    }
    private val brightnessSeekBar by lazy {
        seekBar {
            max = 100
        }
    }

    private val androidStyles by lazy {
        AndroidStyles(this)
    }
    private val cancelButton by lazy {
        androidStyles.button.borderless {
            setText(android.R.string.cancel)
        }
    }
    private val finishButton by lazy {
        androidStyles.button.borderless {
            setText(android.R.string.ok)
        }
    }
    private val buttonsBar by lazy {
        constraintLayout {
            backgroundColor = styledColor(android.R.attr.colorBackground)
            outlineProvider = ViewOutlineProvider.BOUNDS
            elevation = dp(8f)
            add(cancelButton, lParams(wrapContent, wrapContent) {
                topOfParent()
                startOfParent()
                bottomOfParent()
            })
            add(finishButton, lParams(wrapContent, wrapContent) {
                topOfParent()
                endOfParent()
                bottomOfParent()
            })
        }
    }

    private val scrollView by lazy {
        val lineHeight = dp(48)
        constraintLayout {
            bottomPadding = dp(24)
            add(preview.root, lParams(wrapContent, wrapContent) {
                topOfParent()
                centerHorizontally()
                above(variantLabel)
                verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
            })
            add(variantLabel, lParams(wrapContent, lineHeight) {
                below(preview.root, dp(16))
                startOfParent(dp(46))
                above(brightnessLabel)
            })
            add(variantSwitch, lParams(wrapContent, lineHeight) {
                topToTopOf(variantLabel)
                endOfParent(dp(46))
            })
            add(brightnessLabel, lParams(wrapContent, lineHeight) {
                below(variantLabel)
                startOfParent(dp(46))
                above(brightnessSeekBar)
            })
            add(brightnessValue, lParams(wrapContent, lineHeight) {
                topToTopOf(brightnessLabel)
                endOfParent(dp(46))
            })
            add(brightnessSeekBar, lParams(matchConstraints, wrapContent) {
                below(brightnessLabel)
                centerHorizontally(dp(30))
                bottomOfParent()
            })
        }.wrapInScrollView {
            isFillViewport = true
        }
    }

    private val ui by lazy {
        constraintLayout {
            add(scrollView, lParams {
                topOfParent()
                centerHorizontally()
                above(buttonsBar)
            })
            add(buttonsBar, lParams(matchConstraints, wrapContent) {
                centerHorizontally()
                bottomOfParent()
            })
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
            ThemePreset.TransparentDark.deriveCustomBackground(
                theme.name,
                theme.background.first,
                theme.background.second
            ) else
            ThemePreset.TransparentLight.deriveCustomBackground(
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
            theme = originTheme
                ?: (if (variantSwitch.isChecked) ThemePreset.TransparentDark else ThemePreset.TransparentLight)
                    .deriveCustomBackground(n, c.path, s.path)
        }
        preview = KeyboardPreviewUi(this, theme)
        variantSwitch.isChecked = theme.isDark
        setContentView(ui)
        preview.root.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = preview.intrinsicWidth
            height = preview.intrinsicHeight
        }
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
                variantSwitch.setOnCheckedChangeListener { _, isChecked ->
                    setDark(isChecked)
                    preview.setBackground(image)
                }
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
                cancelButton.setOnClickListener {
                    cancel()
                }
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