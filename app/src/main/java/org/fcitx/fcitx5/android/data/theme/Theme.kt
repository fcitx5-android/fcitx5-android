/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.theme

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.fcitx.fcitx5.android.utils.DarkenColorFilter
import org.fcitx.fcitx5.android.utils.RectSerializer
import org.fcitx.fcitx5.android.utils.alpha
import org.fcitx.fcitx5.android.utils.appContext
import java.io.File

@Serializable
sealed interface Theme : Parcelable {

    val name: String
    val isDark: Boolean

    val backgroundColor: Int
    val barColor: Int
    val keyboardColor: Int

    val keyBackgroundColor: Int
    val keyTextColor: Int

    //  Color of candidate text
    val candidateTextColor: Int
    val candidateLabelColor: Int
    val candidateCommentColor: Int

    val altKeyBackgroundColor: Int
    val altKeyTextColor: Int

    val accentKeyBackgroundColor: Int
    val accentKeyTextColor: Int

    val keyPressHighlightColor: Int
    val keyShadowColor: Int

    val popupBackgroundColor: Int
    val popupTextColor: Int

    val spaceBarColor: Int
    val dividerColor: Int
    val clipboardEntryColor: Int

    val genericActiveBackgroundColor: Int
    val genericActiveForegroundColor: Int

    fun backgroundDrawable(keyBorder: Boolean = false): Drawable {
        return ColorDrawable(if (keyBorder) backgroundColor else keyboardColor)
    }

    @Serializable
    @Parcelize
    data class Custom(
        override val name: String,
        override val isDark: Boolean,
        /**
         * absolute paths of cropped and src png files
         */
        val backgroundImage: CustomBackground?,
        override val backgroundColor: Int,
        override val barColor: Int,
        override val keyboardColor: Int,
        override val keyBackgroundColor: Int,
        override val keyTextColor: Int,
        override val candidateTextColor: Int,
        override val candidateLabelColor: Int,
        override val candidateCommentColor: Int,
        override val altKeyBackgroundColor: Int,
        override val altKeyTextColor: Int,
        override val accentKeyBackgroundColor: Int,
        override val accentKeyTextColor: Int,
        override val keyPressHighlightColor: Int,
        override val keyShadowColor: Int,
        override val popupBackgroundColor: Int,
        override val popupTextColor: Int,
        override val spaceBarColor: Int,
        override val dividerColor: Int,
        override val clipboardEntryColor: Int,
        override val genericActiveBackgroundColor: Int,
        override val genericActiveForegroundColor: Int,
        val suggestedPreferences: SuggestedPreferences? = null
    ) : Theme {
        @Parcelize
        @Serializable
        data class CustomBackground(
            val croppedFilePath: String,
            val srcFilePath: String,
            val brightness: Int = 70,
            val cropRect: @Serializable(RectSerializer::class) Rect?,
            val cropRotation: Int = 0
        ) : Parcelable {
            fun toDrawable(): Drawable? {
                val cropped = File(croppedFilePath)
                if (!cropped.exists()) return null
                val bitmap = BitmapFactory.decodeStream(cropped.inputStream()) ?: return null
                return BitmapDrawable(appContext.resources, bitmap).apply {
                    colorFilter = DarkenColorFilter(100 - brightness)
                }
            }
        }

        override fun backgroundDrawable(keyBorder: Boolean): Drawable {
            return backgroundImage?.toDrawable() ?: super.backgroundDrawable(keyBorder)
        }

        /**
         * A custom theme may suggest some theme preferences to the user.
         * These fields should be sync with ThemePrefs.kt
         */
        @Parcelize
        @Serializable
        data class SuggestedPreferences(
            val keyBorder: Boolean? = null,
            val keyRippleEffect: Boolean? = null,
            val keyHorizontalMargin: Int? = null,
            val keyHorizontalMarginLandscape: Int? = null,
            val keyVerticalMargin: Int? = null,
            val keyVerticalMarginLandscape: Int? = null,
            val keyRadius: Int? = null,
            val textEditingButtonRadius: Int? = null,
            val clipboardEntryRadius: Int? = null,
            /**
             * 0: None, 1: Bottom, 2: Top right
             */
            val punctuationPosition: Int? = null,
            /**
             * 0: None, 1: Color only, 2: Full
             */
            val navbarBackground: Int? = null
        ) : Parcelable

    }

    @Parcelize
    data class Builtin(
        override val name: String,
        override val isDark: Boolean,
        override val backgroundColor: Int,
        override val barColor: Int,
        override val keyboardColor: Int,
        override val keyBackgroundColor: Int,
        override val keyTextColor: Int,
        override val candidateTextColor: Int,
        override val candidateLabelColor: Int,
        override val candidateCommentColor: Int,
        override val altKeyBackgroundColor: Int,
        override val altKeyTextColor: Int,
        override val accentKeyBackgroundColor: Int,
        override val accentKeyTextColor: Int,
        override val keyPressHighlightColor: Int,
        override val keyShadowColor: Int,
        override val popupBackgroundColor: Int,
        override val popupTextColor: Int,
        override val spaceBarColor: Int,
        override val dividerColor: Int,
        override val clipboardEntryColor: Int,
        override val genericActiveBackgroundColor: Int,
        override val genericActiveForegroundColor: Int
    ) : Theme {

        // an alias to use 0xAARRGGBB color literal in code
        // because kotlin compiler treats `0xff000000` as Long, not Int
        constructor(
            name: String,
            isDark: Boolean,
            backgroundColor: Number,
            barColor: Number,
            keyboardColor: Number,
            keyBackgroundColor: Number,
            keyTextColor: Number,
            candidateTextColor: Number,
            candidateLabelColor: Number,
            candidateCommentColor: Number,
            altKeyBackgroundColor: Number,
            altKeyTextColor: Number,
            accentKeyBackgroundColor: Number,
            accentKeyTextColor: Number,
            keyPressHighlightColor: Number,
            keyShadowColor: Number,
            popupBackgroundColor: Number,
            popupTextColor: Number,
            spaceBarColor: Number,
            dividerColor: Number,
            clipboardEntryColor: Number,
            genericActiveBackgroundColor: Number,
            genericActiveForegroundColor: Number
        ) : this(
            name,
            isDark,
            backgroundColor.toInt(),
            barColor.toInt(),
            keyboardColor.toInt(),
            keyBackgroundColor.toInt(),
            keyTextColor.toInt(),
            candidateTextColor.toInt(),
            candidateLabelColor.toInt(),
            candidateCommentColor.toInt(),
            altKeyBackgroundColor.toInt(),
            altKeyTextColor.toInt(),
            accentKeyBackgroundColor.toInt(),
            accentKeyTextColor.toInt(),
            keyPressHighlightColor.toInt(),
            keyShadowColor.toInt(),
            popupBackgroundColor.toInt(),
            popupTextColor.toInt(),
            spaceBarColor.toInt(),
            dividerColor.toInt(),
            clipboardEntryColor.toInt(),
            genericActiveBackgroundColor.toInt(),
            genericActiveForegroundColor.toInt()
        )

        fun deriveCustomNoBackground(name: String) = Custom(
            name,
            isDark,
            null,
            backgroundColor,
            barColor,
            keyboardColor,
            keyBackgroundColor,
            keyTextColor,
            candidateTextColor,
            candidateLabelColor,
            candidateCommentColor,
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
            genericActiveForegroundColor
        )

        fun deriveCustomBackground(
            name: String,
            croppedBackgroundImage: String,
            originBackgroundImage: String,
            brightness: Int = 70,
            cropBackgroundRect: Rect? = null,
            cropBackgroundRotation: Int = 0
        ) = Custom(
            name,
            isDark,
            Custom.CustomBackground(
                croppedBackgroundImage,
                originBackgroundImage,
                brightness,
                cropBackgroundRect,
                cropBackgroundRotation
            ),
            backgroundColor,
            barColor,
            keyboardColor,
            keyBackgroundColor,
            keyTextColor,
            candidateTextColor,
            candidateLabelColor,
            candidateCommentColor,
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
            genericActiveForegroundColor
        )
    }

    @Parcelize
    data class Monet(
        override val name: String,
        override val isDark: Boolean,
        override val backgroundColor: Int,
        override val barColor: Int,
        override val keyboardColor: Int,
        override val keyBackgroundColor: Int,
        override val keyTextColor: Int,
        override val candidateTextColor: Int,
        override val candidateLabelColor: Int,
        override val candidateCommentColor: Int,
        override val altKeyBackgroundColor: Int,
        override val altKeyTextColor: Int,
        override val accentKeyBackgroundColor: Int,
        override val accentKeyTextColor: Int,
        override val keyPressHighlightColor: Int,
        override val keyShadowColor: Int,
        override val popupBackgroundColor: Int,
        override val popupTextColor: Int,
        override val spaceBarColor: Int,
        override val dividerColor: Int,
        override val clipboardEntryColor: Int,
        override val genericActiveBackgroundColor: Int,
        override val genericActiveForegroundColor: Int
    ) : Theme {
        constructor(
            isDark: Boolean,
            surfaceContainer: Int,
            surfaceContainerHigh: Int,
            surfaceBright: Int,
            onSurface: Int,
            primary: Int,
            onPrimary: Int,
            secondaryContainer: Int,
            onSurfaceVariant: Int,
        ) : this(
            name = "Monet" + if (isDark) "Dark" else "Light",
            isDark = isDark,
            backgroundColor = surfaceContainer,
            barColor = surfaceContainerHigh,
            keyboardColor = surfaceContainer,
            keyBackgroundColor = surfaceBright,
            keyTextColor = onSurface,
            candidateTextColor = onSurface,
            candidateLabelColor = onSurface,
            candidateCommentColor = onSurfaceVariant,
            altKeyBackgroundColor = secondaryContainer,
            altKeyTextColor = onSurfaceVariant,
            accentKeyBackgroundColor = primary,
            accentKeyTextColor = onPrimary,
            keyPressHighlightColor = onSurface.alpha(if (isDark) 0.2f else 0.12f),
            keyShadowColor = 0x000000,
            popupBackgroundColor = surfaceContainer,
            popupTextColor = onSurface,
            spaceBarColor = surfaceBright,
            dividerColor = surfaceBright,
            clipboardEntryColor = surfaceBright,
            genericActiveBackgroundColor = primary,
            genericActiveForegroundColor = onPrimary
        )

        @OptIn(ExperimentalStdlibApi::class)
        fun toCustom() = Custom(
            name = name + "#" + this.accentKeyBackgroundColor.toHexString(), // Use primary color as identifier
            isDark = isDark,
            backgroundImage = null,
            backgroundColor = backgroundColor,
            barColor = barColor,
            keyboardColor = keyboardColor,
            keyBackgroundColor = keyBackgroundColor,
            keyTextColor = keyTextColor,
            candidateTextColor = candidateTextColor,
            candidateLabelColor = candidateLabelColor,
            candidateCommentColor = candidateCommentColor,
            altKeyBackgroundColor = altKeyBackgroundColor,
            altKeyTextColor = altKeyTextColor,
            accentKeyBackgroundColor = accentKeyBackgroundColor,
            accentKeyTextColor = accentKeyTextColor,
            keyPressHighlightColor = keyPressHighlightColor,
            keyShadowColor = keyShadowColor,
            popupBackgroundColor = popupBackgroundColor,
            popupTextColor = popupTextColor,
            spaceBarColor = spaceBarColor,
            dividerColor = dividerColor,
            clipboardEntryColor = clipboardEntryColor,
            genericActiveBackgroundColor = genericActiveBackgroundColor,
            genericActiveForegroundColor = genericActiveForegroundColor
        )
    }
}
