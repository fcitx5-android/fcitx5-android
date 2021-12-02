package me.rocka.fcitx5test.keyboard.layout

object Preset {
    val Qwerty: List<List<BaseButton>> = listOf(
        listOf(
            AltTextButton("Q", "1"),
            AltTextButton("W", "2"),
            AltTextButton("E", "3"),
            AltTextButton("R", "4"),
            AltTextButton("T", "5"),
            AltTextButton("Y", "6"),
            AltTextButton("U", "7"),
            AltTextButton("I", "8"),
            AltTextButton("O", "9"),
            AltTextButton("P", "0")
        ),
        listOf(
            AltTextButton("A", "@"),
            AltTextButton("S", "*"),
            AltTextButton("D", "+"),
            AltTextButton("F", "-"),
            AltTextButton("G", "="),
            AltTextButton("H", "/"),
            AltTextButton("J", "#"),
            AltTextButton("K", "("),
            AltTextButton("L", ")")
        ),
        listOf(
            CapsButton(),
            AltTextButton("Z", "'"),
            AltTextButton("X", ":"),
            AltTextButton("C", "\""),
            AltTextButton("V", "?"),
            AltTextButton("B", "!"),
            AltTextButton("N", "~"),
            AltTextButton("M", "\\"),
            BackspaceButton()
        ),
        listOf(
            LayoutSwitchButton(),
            QuickPhraseButton(),
            LangSwitchButton(),
            SpaceButton(),
            AltTextButton(",", "."),
            ReturnButton()
        ),
    )
    val T9: List<List<BaseButton>> = listOf(
        listOf(
            TextButton("+", 0.15F),
            TextButton("1", 0F),
            TextButton("2", 0F),
            TextButton("3", 0F),
            TextButton("/", 0.15F),
        ),
        listOf(
            TextButton("-", 0.15F),
            TextButton("4", 0F),
            TextButton("5", 0F),
            TextButton("6", 0F),
            MiniSpaceButton()
        ),
        listOf(
            TextButton("*", 0.15F),
            TextButton("7", 0F),
            TextButton("8", 0F),
            TextButton("9", 0F),
            BackspaceButton()
        ),
        listOf(
            QuickPhraseButton(),
            TextButton("#", 0F),
            TextButton("0", 0F),
            AltTextButton(".", "=", 0F),
            ReturnButton()
        ),
    )
}