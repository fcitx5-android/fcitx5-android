package me.rocka.fcitx5test.keyboard.layout

object Preset {
    val Qwerty: List<List<BaseKey>> = listOf(
        listOf(
            AltTextKey("Q", "1"),
            AltTextKey("W", "2"),
            AltTextKey("E", "3"),
            AltTextKey("R", "4"),
            AltTextKey("T", "5"),
            AltTextKey("Y", "6"),
            AltTextKey("U", "7"),
            AltTextKey("I", "8"),
            AltTextKey("O", "9"),
            AltTextKey("P", "0")
        ),
        listOf(
            AltTextKey("A", "@"),
            AltTextKey("S", "*"),
            AltTextKey("D", "+"),
            AltTextKey("F", "-"),
            AltTextKey("G", "="),
            AltTextKey("H", "/"),
            AltTextKey("J", "#"),
            AltTextKey("K", "("),
            AltTextKey("L", ")")
        ),
        listOf(
            CapsKey(),
            AltTextKey("Z", "'"),
            AltTextKey("X", ":"),
            AltTextKey("C", "\""),
            AltTextKey("V", "?"),
            AltTextKey("B", "!"),
            AltTextKey("N", "~"),
            AltTextKey("M", "\\"),
            BackspaceKey()
        ),
        listOf(
            LayoutSwitchKey(),
            QuickPhraseKey(),
            LangSwitchKey(),
            SpaceKey(),
            AltTextKey(",", "."),
            ReturnKey()
        ),
    )
    val T9: List<List<BaseKey>> = listOf(
        listOf(
            TextKey("+", 0.15F),
            TextKey("1", 0F),
            TextKey("2", 0F),
            TextKey("3", 0F),
            TextKey("/", 0.15F),
        ),
        listOf(
            TextKey("-", 0.15F),
            TextKey("4", 0F),
            TextKey("5", 0F),
            TextKey("6", 0F),
            MiniSpaceKey()
        ),
        listOf(
            TextKey("*", 0.15F),
            TextKey("7", 0F),
            TextKey("8", 0F),
            TextKey("9", 0F),
            BackspaceKey()
        ),
        listOf(
            LayoutSwitchKey(),
            TextKey("#", 0F),
            TextKey("0", 0F),
            AltTextKey(".", "=", 0F),
            ReturnKey()
        ),
    )
}