package org.fcitx.fcitx5.android.core

import android.text.InputType
import android.view.inputmethod.EditorInfo
import splitties.bitflags.hasFlag

/**
 * translated from
 * [fcitx-utils/capabilityflags.h](https://github.com/fcitx/fcitx5/blob/5.1.1/src/lib/fcitx-utils/capabilityflags.h)
 */
@Suppress("unused")
enum class CapabilityFlag(val flag: ULong) {
    NoFlag(0UL),

    /**
     * Deprecated, because this flag is not compatible with fcitx 4.
     */
    ClientSideUI(1UL shl 0),
    Preedit(1UL shl 1),
    ClientSideControlState(1UL shl 2),
    Password(1UL shl 3),
    FormattedPreedit(1UL shl 4),
    ClientUnfocusCommit(1UL shl 5),
    SurroundingText(1UL shl 6),
    Email(1UL shl 7),
    Digit(1UL shl 8),
    Uppercase(1UL shl 9),
    Lowercase(1UL shl 10),
    NoAutoUpperCase(1UL shl 11),
    Url(1UL shl 12),
    Dialable(1UL shl 13),
    Number(1UL shl 14),
    NoOnScreenKeyboard(1UL shl 15),
    SpellCheck(1UL shl 16),
    NoSpellCheck(1UL shl 17),
    WordCompletion(1UL shl 18),
    UppercaseWords(1UL shl 19),
    UppercaseSentences(1UL shl 20),
    Alpha(1UL shl 21),
    Name(1UL shl 22),
    GetIMInfoOnFocus(1UL shl 23),
    RelativeRect(1UL shl 24),
    // 25 ~ 31 are reserved for fcitx 4 compatibility.

    // New addition in fcitx 5.
    Terminal(1UL shl 32),
    Date(1UL shl 33),
    Time(1UL shl 34),
    Multiline(1UL shl 35),
    Sensitive(1UL shl 36),
    KeyEventOrderFix(1UL shl 37),

    /**
     * Whether client will set KeyState::Repeat on the key event.
     */
    ReportKeyRepeat(1UL shl 38),

    /**
     * Whether client display input panel by itself.
     */
    ClientSideInputPanel(1UL shl 39),

    /**
     * Whether client request input method to be disabled.
     * Usually this means only allow to type with raw keyboard.
     */
    Disable(1UL shl 40),

    /**
     * Whether client support commit string with cursor location.
     */
    CommitStringWithCursor(1UL shl 41),

    PasswordOrSensitive(Password.flag or Sensitive.flag);

}

operator fun ULong.plus(other: CapabilityFlag) = or(other.flag)
operator fun ULong.minus(other: CapabilityFlag) = and(other.flag.inv())

@JvmInline
value class CapabilityFlags constructor(val flags: ULong) {
    companion object {
        fun mergeFlags(arr: Array<out CapabilityFlag>): ULong =
            arr.fold(CapabilityFlag.NoFlag.flag) { acc, it -> acc or it.flag }

        val DefaultFlags = CapabilityFlags(
            CapabilityFlag.Preedit,
            CapabilityFlag.ClientUnfocusCommit,
            CapabilityFlag.CommitStringWithCursor
        )

        fun fromEditorInfo(info: EditorInfo): CapabilityFlags {
            var flags = DefaultFlags.flags
            info.imeOptions.let {
                if (it.hasFlag(EditorInfo.IME_FLAG_FORCE_ASCII)) {
                    flags += CapabilityFlag.Alpha
                }
                if (it.hasFlag(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)) {
                    flags += CapabilityFlag.Sensitive
                }
            }
            info.inputType.let {
                when (it and InputType.TYPE_MASK_CLASS) {
                    InputType.TYPE_NULL -> {
                        flags -= CapabilityFlag.Preedit
                        flags += CapabilityFlag.NoSpellCheck
                    }
                    InputType.TYPE_CLASS_TEXT -> {
                        (it and InputType.TYPE_MASK_FLAGS).run {
                            if (hasFlag(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) ||
                                hasFlag(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
                            ) {
                                flags += CapabilityFlag.NoSpellCheck
                            }
                            if (hasFlag(InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)) {
                                flags += CapabilityFlag.SpellCheck
                            }
                            if (hasFlag(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)) {
                                flags += CapabilityFlag.Uppercase
                            }
                            if (hasFlag(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)) {
                                flags += CapabilityFlag.UppercaseSentences
                            }
                            if (hasFlag(InputType.TYPE_TEXT_FLAG_CAP_WORDS)) {
                                flags += CapabilityFlag.UppercaseWords
                            }
                            if (hasFlag(InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE) ||
                                hasFlag(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                            ) {
                                flags += CapabilityFlag.Multiline
                            }
                        }
                        (it and InputType.TYPE_MASK_VARIATION).run {
                            if (equals(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) ||
                                equals(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
                            ) {
                                flags += CapabilityFlag.Email
                            }
                            if (equals(InputType.TYPE_TEXT_VARIATION_PASSWORD) ||
                                equals(InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
                            ) {
                                flags += CapabilityFlag.Password
                            }
                            if (equals(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)) {
                                flags += CapabilityFlag.Sensitive
                            }
                            if (equals(InputType.TYPE_TEXT_VARIATION_URI)) {
                                flags += CapabilityFlag.Url
                            }
                        }
                    }
                    InputType.TYPE_CLASS_NUMBER -> {
                        flags += CapabilityFlag.NoSpellCheck
                        flags += CapabilityFlag.Number
                    }
                    InputType.TYPE_CLASS_PHONE -> {
                        flags += CapabilityFlag.NoSpellCheck
                        flags += CapabilityFlag.Dialable
                    }
                    InputType.TYPE_CLASS_DATETIME -> {
                        flags += CapabilityFlag.NoSpellCheck
                        (it and InputType.TYPE_MASK_VARIATION).run {
                            if (hasFlag(InputType.TYPE_DATETIME_VARIATION_DATE)) {
                                flags += CapabilityFlag.Date
                            }
                            if (hasFlag(InputType.TYPE_DATETIME_VARIATION_TIME)) {
                                flags += CapabilityFlag.Time
                            }
                        }
                    }
                }
            }
            return CapabilityFlags(flags)
        }
    }

    constructor(vararg flags: CapabilityFlag) : this(mergeFlags(flags))

    fun has(flag: ULong) = flags.hasFlag(flag)

    fun has(flag: CapabilityFlag) = flags.hasFlag(flag.flag)

    fun toLong() = flags.toLong()
}
