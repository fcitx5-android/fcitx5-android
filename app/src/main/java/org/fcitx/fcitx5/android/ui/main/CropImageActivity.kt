/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.ui.main

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.item
import org.fcitx.fcitx5.android.utils.parcelable
import org.fcitx.fcitx5.android.utils.subMenu
import org.fcitx.fcitx5.android.utils.toast
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapContent
import splitties.views.topPadding
import timber.log.Timber
import java.io.File

class CropImageActivity : AppCompatActivity(), CropImageView.OnCropImageCompleteListener {

    companion object {
        const val CROP_OPTIONS = "crop_options"
        const val CROP_RESULT = "crop_result"
    }

    sealed class CropOption() : Parcelable {
        abstract val width: Int
        abstract val height: Int

        @Parcelize
        data class New(override val width: Int, override val height: Int) : CropOption()

        @Parcelize
        data class Edit(
            override val width: Int,
            override val height: Int,
            val sourceUri: Uri,
            val initialRect: Rect? = null,
            val initialRotation: Int = 0
        ) : CropOption()
    }

    sealed class CropResult : Parcelable {
        @Parcelize
        data object Fail : CropResult()

        @Parcelize
        data class Success(
            val rect: Rect,
            val rotation: Int,
            val file: File,
            val srcUri: Uri
        ) : CropResult() {
            @IgnoredOnParcel
            private var _bitmap: Bitmap? = null
            val bitmap: Bitmap
                get() {
                    _bitmap?.let { return it }
                    return BitmapFactory.decodeFile(file.path).also {
                        _bitmap = it
                        file.delete()
                    }
                }
        }
    }

    class CropContract : ActivityResultContract<CropOption, CropResult>() {
        override fun createIntent(context: Context, input: CropOption): Intent {
            return Intent(context, CropImageActivity::class.java).putExtra(CROP_OPTIONS, input)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): CropResult {
            val result = intent?.parcelable<CropResult.Success>(CROP_RESULT)
            if (resultCode != RESULT_OK || result == null) {
                return CropResult.Fail
            }
            return result
        }
    }

    private lateinit var cropOption: CropOption

    private lateinit var root: ConstraintLayout
    private lateinit var toolbar: Toolbar
    private lateinit var cropView: CropImageView

    private fun getDefaultCropImageOptions() = CropImageOptions(
        // CropImageView
        snapRadius = 0f,
        guidelines = CropImageView.Guidelines.ON_TOUCH,
        showProgressBar = true,
        progressBarColor = styledColor(android.R.attr.colorAccent),
        // CropOverlayView
        borderLineThickness = dp(1f),
        borderCornerOffset = 0f,
    )

    private var selectedImageUri: Uri? = null

    private val launcher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            setResult(RESULT_CANCELED)
            finish()
        } else {
            selectedImageUri = uri
            cropView.setImageUriAsync(uri)
        }
    }

    private lateinit var tempOutFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cropOption = intent.parcelable<CropOption>(CROP_OPTIONS) ?: CropOption.New(1, 1)
        enableEdgeToEdge()
        setupRootView()
        setContentView(root)
        setupCropView(cropOption)
        onBackPressedDispatcher.addCallback {
            setResult(RESULT_CANCELED)
            finish()
        }
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRootView() {
        toolbar = view(::Toolbar) {
            backgroundColor = styledColor(android.R.attr.colorPrimary)
            elevation = dp(4f)
            navigationIcon = DrawerArrowDrawable(context).apply { progress = 1f }
            setupToolbarMenu(menu)
        }
        cropView = CropImageView(this).apply {
            setOnCropImageCompleteListener(this@CropImageActivity)
            setImageCropOptions(getDefaultCropImageOptions())
        }
        root = constraintLayout {
            add(toolbar, lParams(matchParent, wrapContent) {
                topOfParent()
                centerHorizontally()
            })
            add(cropView, lParams(matchParent) {
                below(toolbar)
                centerHorizontally()
                bottomOfParent()
            })
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = navBars.left
                rightMargin = navBars.right
                bottomMargin = navBars.bottom
            }
            toolbar.topPadding = statusBars.top
            windowInsets
        }
    }

    private fun setupToolbarMenu(menu: Menu) {
        val iconTint = styledColor(android.R.attr.colorControlNormal)
        menu.item(R.string.rotate, R.drawable.ic_baseline_rotate_right_24, iconTint, true) {
            cropView.rotateImage(90)
        }
        menu.subMenu(R.string.flip, R.drawable.ic_baseline_flip_24, iconTint, true) {
            item(R.string.flip_vertically) {
                cropView.flipImageVertically()
            }
            item(R.string.flip_horizontally) {
                cropView.flipImageHorizontally()
            }
        }
        menu.item(R.string.crop, R.drawable.ic_baseline_check_24, iconTint, true) {
            onCropImage()
        }
    }

    private fun setupCropView(option: CropOption) {
        cropView.setAspectRatio(option.width, option.height)
        when (option) {
            is CropOption.New -> {
                launcher.launch("image/*")
            }
            is CropOption.Edit -> {
                cropView.setOnSetImageUriCompleteListener { view, uri, e ->
                    view.cropRect = option.initialRect
                    view.rotatedDegrees = option.initialRotation
                }
                cropView.setImageUriAsync(option.sourceUri)
            }
        }
    }

    private fun onCropImage() {
        tempOutFile = File.createTempFile("cropped", ".png", cacheDir)
        cropView.croppedImageAsync(
            saveCompressFormat = Bitmap.CompressFormat.PNG,
            reqWidth = cropOption.width,
            reqHeight = cropOption.height,
            options = CropImageView.RequestSizeOptions.RESIZE_INSIDE,
            customOutputUri = Uri.fromFile(tempOutFile)
        )
    }

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        try {
            result
            val success = CropResult.Success(
                result.cropRect!!,
                result.rotation,
                tempOutFile,
                (cropOption as? CropOption.Edit)?.sourceUri ?: selectedImageUri!!
            )
            setResult(RESULT_OK, Intent().putExtra(CROP_RESULT, success))
        } catch (e: Exception) {
            Timber.e("Exception when cropping image: $e")
            toast(e)
            setResult(RESULT_CANCELED)
        }
        finish()
    }
}
