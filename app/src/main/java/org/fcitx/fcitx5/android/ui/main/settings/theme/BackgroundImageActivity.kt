package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.SeekBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.ThemePreset
import org.fcitx.fcitx5.android.ui.common.withLoadingDialog
import org.fcitx.fcitx5.android.utils.darkenColorFilter
import splitties.dimensions.dp
import splitties.resources.color
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


    sealed interface BackgroundResult : Parcelable {
        @Parcelize
        data class Updated(val theme: Theme.Custom) : BackgroundResult

        @Parcelize
        data class Created(val theme: Theme.Custom) : BackgroundResult

        @Parcelize
        data class Deleted(val name: String) : BackgroundResult
    }

    class Contract : ActivityResultContract<Theme.Custom?, BackgroundResult?>() {
        override fun createIntent(context: Context, input: Theme.Custom?): Intent =
            Intent(context, BackgroundImageActivity::class.java).apply {
                putExtra(ORIGIN_THEME, input)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): BackgroundResult? =
            intent?.getParcelableExtra(RESULT)
    }

    private lateinit var previewUi: KeyboardPreviewUi

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
    private val deleteButton by lazy {
        androidStyles.button.borderless {
            visibility = View.GONE
            setText(R.string.delete)
            setTextColor(color(R.color.red_400))
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
            add(deleteButton, lParams(wrapContent, wrapContent) {
                after(cancelButton, dp(8))
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
            add(previewUi.root, lParams(wrapContent, wrapContent) {
                topOfParent()
                centerHorizontally()
                above(variantLabel)
                verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
            })
            add(variantLabel, lParams(wrapContent, lineHeight) {
                below(previewUi.root, dp(16))
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
    private var src: Bitmap? = null
    private lateinit var cropped: Bitmap
    private var rect: Rect? = null
    private var newCreated = true

    private lateinit var theme: Theme.Custom
    private lateinit var srcImageFile: File
    private lateinit var croppedImageFile: File

    // TODO
    private val Theme.Custom.background
        get() = backgroundImage
            ?: throw IllegalStateException("Custom theme only supports backgroundImage for now")

    private fun setKeyVariant(darkKeys: Boolean) {
        theme = if (darkKeys)
            ThemePreset.TransparentLight.deriveCustomBackground(
                theme.name,
                theme.background.croppedFilePath,
                theme.background.srcFilePath,
                brightnessSeekBar.progress,
                theme.background.cropRect
            ) else
            ThemePreset.TransparentDark.deriveCustomBackground(
                theme.name,
                theme.background.croppedFilePath,
                theme.background.srcFilePath,
                brightnessSeekBar.progress,
                theme.background.cropRect
            )
        previewUi.setTheme(theme)
        previewUi.setBackground(image)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        variantSwitch.setOnCheckedChangeListener { _, isChecked ->
            setKeyVariant(darkKeys = isChecked)
        }
        brightnessSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(bar: SeekBar) {}
            override fun onStopTrackingTouch(bar: SeekBar) {}

            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                brightnessValue.text = "$progress%"
                image.colorFilter = darkenColorFilter(100 - progress)
                previewUi.setBackground(image)
            }
        })
        cancelButton.setOnClickListener {
            cancel()
        }
        finishButton.setOnClickListener {
            done()
        }
        // recover from bundle
        val originTheme = intent?.getParcelableExtra<Theme.Custom>(ORIGIN_THEME)?.also { t ->
            theme = t
            t.background.also {
                croppedImageFile = File(it.croppedFilePath)
                srcImageFile = File(it.srcFilePath)
                rect = it.cropRect
                cropped = BitmapFactory.decodeFile(it.croppedFilePath)
                image = BitmapDrawable(resources, cropped)
            }
            newCreated = false
        }
        // create new
        if (originTheme == null) {
            val (n, c, s) = ThemeManager.newCustomBackgroundImages()
            croppedImageFile = c
            srcImageFile = s
            theme =
                (if (variantSwitch.isChecked) ThemePreset.TransparentLight else ThemePreset.TransparentDark)
                    .deriveCustomBackground(n, c.path, s.path)
        }
        previewUi = KeyboardPreviewUi(this, theme)
        if (!newCreated) {
            deleteButton.visibility = View.VISIBLE
            deleteButton.setOnClickListener {
                delete()
            }
        }
        setContentView(ui)
        // after image was initialized
        val initValues = {
            brightnessSeekBar.progress = theme.background.brightness
            variantSwitch.isChecked = !theme.isDark
        }
        if (!newCreated) {
            initValues()
        }
        launcher = registerForActivityResult(CropImageContract()) {
            if (!it.isSuccessful && newCreated)
                cancel()
            else {
                rect = it.cropRect!!
                theme = theme.copy(backgroundImage = theme.backgroundImage?.copy(cropRect = rect))
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
                initValues()
            }
        }

        if (newCreated)
            previewUi.onSizeMeasured = { w, h ->
                launcher.launch(options(srcImageFile.takeIf { it.exists() }?.toUri()) {
                    setInitialCropWindowRectangle(rect)
                    setGuidelines(CropImageView.Guidelines.ON_TOUCH)
                    setBorderLineColor(Color.WHITE)
                    setBorderLineThickness(dp(1f))
                    setBorderCornerColor(Color.WHITE)
                    setBorderCornerOffset(0f)
                    setImageSource(includeGallery = true, includeCamera = false)
                    setAspectRatio(w, h)
                    setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                })
            }
    }

    private fun cancel() {
        setResult(Activity.RESULT_CANCELED, Intent().apply { putExtra(RESULT, null as BackgroundResult?) })
        finish()
    }

    private fun done() {
        lifecycleScope.withLoadingDialog(this) {
            withContext(Dispatchers.IO) {
                croppedImageFile.delete()
                croppedImageFile.outputStream().use {
                    cropped.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                if (newCreated) {
                    srcImageFile.delete()
                    srcImageFile.outputStream().use {
                        requireNotNull(src).compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
            }
            setResult(
                Activity.RESULT_OK,
                Intent().apply {
                    val newTheme =
                        theme.copy(backgroundImage = theme.background.copy(brightness = brightnessSeekBar.progress))
                    putExtra(
                        RESULT,
                        if (newCreated)
                            BackgroundResult.Created(newTheme)
                        else
                            BackgroundResult.Updated(newTheme)
                    )
                })
            finish()
        }
    }

    private fun delete() {
        setResult(Activity.RESULT_OK,
            Intent().apply {
                putExtra(RESULT, BackgroundResult.Deleted(theme.name))
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
        const val RESULT = "result"
        const val ORIGIN_THEME = "origin_theme"
    }
}