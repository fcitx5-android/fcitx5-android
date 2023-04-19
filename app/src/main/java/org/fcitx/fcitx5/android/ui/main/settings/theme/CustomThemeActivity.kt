package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.SeekBar
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.ThemePreset
import org.fcitx.fcitx5.android.ui.common.withLoadingDialog
import org.fcitx.fcitx5.android.utils.applyTranslucentSystemBars
import org.fcitx.fcitx5.android.utils.darkenColorFilter
import org.fcitx.fcitx5.android.utils.parcelable
import splitties.dimensions.dp
import splitties.resources.color
import splitties.resources.drawable
import splitties.resources.resolveThemeAttribute
import splitties.resources.styledColor
import splitties.resources.styledDrawable
import splitties.views.*
import splitties.views.dsl.appcompat.switch
import splitties.views.dsl.appcompat.toolbar
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.dsl.material.defaultLParams
import java.io.File

class CustomThemeActivity : AppCompatActivity() {

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
            Intent(context, CustomThemeActivity::class.java).apply {
                putExtra(ORIGIN_THEME, input)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): BackgroundResult? =
            intent?.extras?.parcelable(RESULT)
    }

    private val toolbar by lazy {
        toolbar()
    }

    private val appBar by lazy {
        view(::AppBarLayout) {
            add(toolbar, defaultLParams())
        }
    }

    private lateinit var previewUi: KeyboardPreviewUi

    private fun createTextView(@StringRes string: Int? = null, ripple: Boolean = false) = textView {
        if (string != null) {
            setText(string)
        }
        gravity = gravityVerticalCenter
        textAppearance = resolveThemeAttribute(android.R.attr.textAppearanceListItem)
        horizontalPadding = dp(16)
        if (ripple) {
            background = styledDrawable(android.R.attr.selectableItemBackground)
        }
    }

    private val variantLabel by lazy {
        createTextView(R.string.dark_keys, ripple = true)
    }
    private val variantSwitch by lazy {
        switch { }
    }

    private val brightnessLabel by lazy {
        createTextView(R.string.brightness)
    }
    private val brightnessValue by lazy {
        createTextView()
    }
    private val brightnessSeekBar by lazy {
        seekBar {
            max = 100
        }
    }

    private val cropLabel by lazy {
        createTextView(R.string.recrop_image, ripple = true)
    }

    private val scrollView by lazy {
        val lineHeight = dp(48)
        val itemMargin = dp(30)
        constraintLayout {
            bottomPadding = dp(24)
            add(previewUi.root, lParams(wrapContent, wrapContent) {
                topOfParent()
                centerHorizontally()
                above(cropLabel, dp(8))
                verticalChainStyle = packed
            })
            add(cropLabel, lParams(matchConstraints, lineHeight) {
                below(previewUi.root)
                centerHorizontally(itemMargin)
                above(variantLabel)
            })
            add(variantLabel, lParams(matchConstraints, lineHeight) {
                below(cropLabel)
                startOfParent(itemMargin)
                before(variantSwitch)
                above(brightnessLabel)
            })
            add(variantSwitch, lParams(wrapContent, lineHeight) {
                topToTopOf(variantLabel)
                endOfParent(itemMargin)
            })
            add(brightnessLabel, lParams(matchConstraints, lineHeight) {
                below(variantLabel)
                startOfParent(itemMargin)
                before(brightnessValue)
                above(brightnessSeekBar)
            })
            add(brightnessValue, lParams(wrapContent, lineHeight) {
                topToTopOf(brightnessLabel)
                endOfParent(itemMargin)
            })
            add(brightnessSeekBar, lParams(matchConstraints, wrapContent) {
                below(brightnessLabel)
                centerHorizontally(itemMargin)
                bottomOfParent()
            })
        }.wrapInScrollView {
            isFillViewport = true
        }
    }

    private val ui by lazy {
        constraintLayout {
            add(appBar, lParams(matchParent, wrapContent) {
                topOfParent()
                centerHorizontally()
            })
            add(scrollView, lParams {
                below(appBar)
                centerHorizontally()
                bottomOfParent()
            })
        }
    }

    private var newCreated = true

    private lateinit var theme: Theme.Custom

    private class BackgroundStates {
        lateinit var launcher: ActivityResultLauncher<CropImageContractOptions>
        var srcImageExtension: String? = null
        var srcImageBuffer: ByteArray? = null
        var tempImageFile: File? = null
        var cropRect: Rect? = null
        lateinit var croppedBitmap: Bitmap
        lateinit var filteredDrawable: BitmapDrawable
        lateinit var srcImageFile: File
        lateinit var croppedImageFile: File
    }

    private val backgroundStates by lazy { BackgroundStates() }

    private inline fun whenHasBackground(
        block: BackgroundStates.(Theme.Custom.CustomBackground) -> Unit,
    ) {
        if (theme.backgroundImage != null)
            block(backgroundStates, theme.backgroundImage!!)
    }

    private fun BackgroundStates.setKeyVariant(
        background: Theme.Custom.CustomBackground,
        darkKeys: Boolean
    ) {
        theme = if (darkKeys)
            ThemePreset.TransparentLight.deriveCustomBackground(
                theme.name,
                background.croppedFilePath,
                background.srcFilePath,
                brightnessSeekBar.progress,
                background.cropRect
            ) else
            ThemePreset.TransparentDark.deriveCustomBackground(
                theme.name,
                background.croppedFilePath,
                background.srcFilePath,
                brightnessSeekBar.progress,
                background.cropRect
            )
        previewUi.setTheme(theme, filteredDrawable)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // recover from bundle
        val originTheme = intent?.extras?.parcelable<Theme.Custom>(ORIGIN_THEME)?.also { t ->
            theme = t
            whenHasBackground {
                croppedImageFile = File(it.croppedFilePath)
                srcImageFile = File(it.srcFilePath)
                cropRect = it.cropRect
                croppedBitmap = BitmapFactory.decodeFile(it.croppedFilePath)
                filteredDrawable = BitmapDrawable(resources, croppedBitmap)
            }
            newCreated = false
        }
        // create new
        if (originTheme == null) {
            val (n, c, s) = ThemeManager.newCustomBackgroundImages()
            backgroundStates.apply {
                croppedImageFile = c
                srcImageFile = s
            }
            theme =
                (if (variantSwitch.isChecked) ThemePreset.TransparentLight else ThemePreset.TransparentDark)
                    .deriveCustomBackground(n, c.path, s.path)
        }
        previewUi = KeyboardPreviewUi(this, theme)
        whenHasBackground {
            cropLabel.setOnClickListener {
                launchCrop(previewUi.intrinsicWidth, previewUi.intrinsicHeight)
            }
            variantLabel.setOnClickListener {
                variantSwitch.isChecked = !variantSwitch.isChecked
            }
            variantSwitch.setOnCheckedChangeListener { _, isChecked ->
                setKeyVariant(it, darkKeys = isChecked)
            }
            brightnessSeekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar: SeekBar) {}
                override fun onStopTrackingTouch(bar: SeekBar) {}

                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) updateState()
                }
            })
        }
        if (theme.backgroundImage == null) {
            brightnessLabel.visibility = View.GONE
            cropLabel.visibility = View.GONE
            variantLabel.visibility = View.GONE
            variantSwitch.visibility = View.GONE
            brightnessSeekBar.visibility = View.GONE
        }
        applyTranslucentSystemBars()
        ViewCompat.setOnApplyWindowInsetsListener(ui) { _, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            appBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = navBars.left
                rightMargin = navBars.right
            }
            toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBars.top
            }
            scrollView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = navBars.left
                rightMargin = navBars.right
            }
            windowInsets
        }
        // show Activity label on toolbar
        setSupportActionBar(toolbar)
        // show back button
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        setContentView(ui)
        whenHasBackground { background ->
            brightnessSeekBar.progress = background.brightness
            variantSwitch.isChecked = !theme.isDark
            launcher = registerForActivityResult(CropImageContract()) {
                if (!it.isSuccessful) {
                    if (newCreated)
                        cancel()
                    else
                        return@registerForActivityResult
                } else {
                    if (newCreated) {
                        srcImageExtension = MimeTypeMap.getSingleton()
                            .getExtensionFromMimeType(contentResolver.getType(it.originalUri!!))
                        srcImageBuffer =
                            contentResolver.openInputStream(it.originalUri!!)!!
                                .use { x -> x.readBytes() }
                    }
                    cropRect = it.cropRect!!
                    croppedBitmap = Bitmap.createScaledBitmap(
                        it.getBitmap(this@CustomThemeActivity)!!,
                        previewUi.intrinsicWidth,
                        previewUi.intrinsicHeight,
                        true
                    )
                    filteredDrawable = BitmapDrawable(resources, croppedBitmap)
                    updateState()
                }
            }
        }

        if (newCreated) {
            cropLabel.visibility = View.GONE
            whenHasBackground {
                previewUi.onSizeMeasured = { w, h ->
                    launchCrop(w, h)
                }
            }
        } else {
            whenHasBackground {
                updateState()
            }
        }

        onBackPressedDispatcher.addCallback {
            cancel()
        }
    }

    private fun BackgroundStates.launchCrop(w: Int, h: Int) {
        if (tempImageFile == null || tempImageFile?.exists() != true) {
            tempImageFile = File.createTempFile("cropped", ".png", cacheDir)
        }
        launcher.launch(
            CropImageContractOptions(
                uri = srcImageFile.takeIf { it.exists() }?.toUri(),
                CropImageOptions(
                    initialCropWindowRectangle = cropRect,
                    guidelines = CropImageView.Guidelines.ON_TOUCH,
                    borderLineColor = Color.WHITE,
                    borderLineThickness = dp(1f),
                    borderCornerColor = Color.WHITE,
                    borderCornerOffset = 0f,
                    imageSourceIncludeGallery = true,
                    imageSourceIncludeCamera = false,
                    aspectRatioX = w,
                    aspectRatioY = h,
                    fixAspectRatio = true,
                    customOutputUri = tempImageFile!!.toUri(),
                    outputCompressFormat = Bitmap.CompressFormat.PNG,
                    cropMenuCropButtonIcon = R.drawable.ic_baseline_done_24,
                    showProgressBar = true,
                    progressBarColor = styledColor(android.R.attr.colorAccent),
                    activityMenuIconColor = styledColor(android.R.attr.colorControlNormal),
                    activityMenuTextColor = styledColor(android.R.attr.colorForeground),
                    activityBackgroundColor = styledColor(android.R.attr.colorBackground),
                    toolbarColor = styledColor(android.R.attr.colorPrimary),
                    toolbarBackButtonColor = styledColor(android.R.attr.colorControlNormal)
                )
            )
        )
    }

    @SuppressLint("SetTextI18n")
    private fun BackgroundStates.updateState() {
        val progress = brightnessSeekBar.progress
        brightnessValue.text = "$progress%"
        filteredDrawable.colorFilter = darkenColorFilter(100 - progress)
        previewUi.setBackground(filteredDrawable)
    }

    private fun cancel() {
        whenHasBackground {
            tempImageFile?.delete()
        }
        setResult(
            Activity.RESULT_CANCELED,
            Intent().apply { putExtra(RESULT, null as BackgroundResult?) }
        )
        finish()
    }

    private fun done() {
        lifecycleScope.withLoadingDialog(this) {
            whenHasBackground {
                withContext(Dispatchers.IO) {
                    tempImageFile?.delete()
                    croppedImageFile.delete()
                    croppedImageFile.outputStream().use {
                        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    if (newCreated) {
                        if (srcImageExtension != null) {
                            srcImageFile = File("${srcImageFile.absolutePath}.$srcImageExtension")
                            theme = theme.copy(
                                backgroundImage = it.copy(
                                    srcFilePath = srcImageFile.absolutePath
                                )
                            )
                        }
                        srcImageFile.writeBytes(srcImageBuffer!!)
                    }
                }
            }
            setResult(
                Activity.RESULT_OK,
                Intent().apply {
                    var newTheme = theme
                    whenHasBackground {
                        newTheme = theme.copy(
                            backgroundImage = it.copy(
                                brightness = brightnessSeekBar.progress,
                                cropRect = cropRect
                            )
                        )
                    }
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
        setResult(
            Activity.RESULT_OK,
            Intent().apply {
                putExtra(RESULT, BackgroundResult.Deleted(theme.name))
            }
        )
        finish()
    }

    private fun promptDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_theme)
            .setMessage(getString(R.string.delete_theme_msg, theme.name))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                delete()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!newCreated) {
            menu.add(R.string.delete).apply {
                icon = drawable(R.drawable.ic_baseline_delete_24)!!.apply {
                    colorFilter =
                        PorterDuffColorFilter(color(R.color.red_400), PorterDuff.Mode.SRC_IN)
                }
                setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                setOnMenuItemClickListener {
                    promptDelete()
                    true
                }
            }
        }
        menu.add(R.string.save).apply {
            setIcon(R.drawable.ic_baseline_done_24)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            setOnMenuItemClickListener {
                done()
                true
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            cancel()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        const val RESULT = "result"
        const val ORIGIN_THEME = "origin_theme"
    }
}