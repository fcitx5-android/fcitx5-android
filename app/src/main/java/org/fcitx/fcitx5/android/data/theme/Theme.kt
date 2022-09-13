package org.fcitx.fcitx5.android.data.theme

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.fcitx.fcitx5.android.utils.ColorInt
import org.fcitx.fcitx5.android.utils.RectSerializer
import org.fcitx.fcitx5.android.utils.appContext
import org.fcitx.fcitx5.android.utils.darkenColorFilter

@Serializable
sealed class Theme : Parcelable {

    abstract val name: String

    abstract val backgroundColor: ColorInt
    abstract val barColor: ColorInt
    abstract val keyboardColor: ColorInt

    abstract val keyBackgroundColor: ColorInt
    abstract val keyTextColor: ColorInt

    abstract val altKeyBackgroundColor: ColorInt
    abstract val altKeyTextColor: ColorInt

    abstract val accentKeyBackgroundColor: ColorInt
    abstract val accentKeyTextColor: ColorInt

    abstract val keyPressHighlightColor: ColorInt
    abstract val keyShadowColor: ColorInt

    abstract val popupBackgroundColor: ColorInt
    abstract val popupTextColor: ColorInt

    abstract val spaceBarColor: ColorInt
    abstract val dividerColor: ColorInt
    abstract val clipboardEntryColor: ColorInt

    abstract val genericActiveBackgroundColor: ColorInt
    abstract val genericActiveForegroundColor: ColorInt

    abstract val isDark: Boolean

    open fun backgroundDrawable(keyBorder: Boolean = false): Drawable {
        return ColorDrawable(if (keyBorder) backgroundColor.color else keyboardColor.color)
    }

    @Serializable
    @Parcelize
    data class Custom(
        override val name: String,
        // absolute file paths of cropped and src png files
        val backgroundImage: CustomBackground?,
        override val backgroundColor: ColorInt,
        override val barColor: ColorInt,
        override val keyboardColor: ColorInt,
        override val keyBackgroundColor: ColorInt,
        override val keyTextColor: ColorInt,
        override val altKeyBackgroundColor: ColorInt,
        override val altKeyTextColor: ColorInt,
        override val accentKeyBackgroundColor: ColorInt,
        override val accentKeyTextColor: ColorInt,
        override val keyPressHighlightColor: ColorInt,
        override val keyShadowColor: ColorInt,
        override val popupBackgroundColor: ColorInt,
        override val popupTextColor: ColorInt,
        override val spaceBarColor: ColorInt,
        override val dividerColor: ColorInt,
        override val clipboardEntryColor: ColorInt,
        override val genericActiveBackgroundColor: ColorInt,
        override val genericActiveForegroundColor: ColorInt,
        override val isDark: Boolean
    ) : Theme() {
        @Parcelize
        @Serializable
        data class CustomBackground(
            val croppedFilePath: String,
            val srcFilePath: String,
            val brightness: Int = 70,
            val cropRect: @Serializable(with = RectSerializer::class) Rect?,
        ) : Parcelable {
            fun toDrawable(): Drawable? {
                val bitmap = BitmapFactory.decodeFile(croppedFilePath) ?: return null
                return BitmapDrawable(appContext.resources, bitmap).apply {
                    colorFilter = darkenColorFilter(100 - brightness)
                }
            }
        }

        override fun backgroundDrawable(keyBorder: Boolean) =
            backgroundImage?.toDrawable() ?: super.backgroundDrawable(keyBorder)
    }

    @Parcelize
    data class Builtin(
        override val name: String,
        override val backgroundColor: ColorInt,
        override val barColor: ColorInt,
        override val keyboardColor: ColorInt,
        override val keyBackgroundColor: ColorInt,
        override val keyTextColor: ColorInt,
        override val altKeyBackgroundColor: ColorInt,
        override val altKeyTextColor: ColorInt,
        override val accentKeyBackgroundColor: ColorInt,
        override val accentKeyTextColor: ColorInt,
        override val keyPressHighlightColor: ColorInt,
        override val keyShadowColor: ColorInt,
        override val popupBackgroundColor: ColorInt,
        override val popupTextColor: ColorInt,
        override val spaceBarColor: ColorInt,
        override val dividerColor: ColorInt,
        override val clipboardEntryColor: ColorInt,
        override val genericActiveBackgroundColor: ColorInt,
        override val genericActiveForegroundColor: ColorInt,
        override val isDark: Boolean
    ) : Theme() {
        fun deriveCustomNoBackground(name: String) = Custom(
            name,
            null,
            backgroundColor,
            barColor,
            keyboardColor,
            keyBackgroundColor,
            keyTextColor,
            altKeyBackgroundColor,
            altKeyTextColor,
            accentKeyBackgroundColor,
            accentKeyTextColor,
            keyPressHighlightColor,
            keyShadowColor,
            popupBackgroundColor,
            popupTextColor,
            spaceBarColor,
            dividerColor,
            clipboardEntryColor,
            genericActiveBackgroundColor,
            genericActiveForegroundColor,
            isDark
        )

        fun deriveCustomBackground(
            name: String,
            croppedBackgroundImage: String,
            originBackgroundImage: String,
            brightness: Int = 70,
            cropBackgroundRect: Rect? = null,
        ) = Custom(
            name,
            Custom.CustomBackground(
                croppedBackgroundImage,
                originBackgroundImage,
                brightness,
                cropBackgroundRect
            ),
            backgroundColor,
            barColor,
            keyboardColor,
            keyBackgroundColor,
            keyTextColor,
            altKeyBackgroundColor,
            altKeyTextColor,
            accentKeyBackgroundColor,
            accentKeyTextColor,
            keyPressHighlightColor,
            keyShadowColor,
            popupBackgroundColor,
            popupTextColor,
            spaceBarColor,
            dividerColor,
            clipboardEntryColor,
            genericActiveBackgroundColor,
            genericActiveForegroundColor,
            isDark
        )
    }

}