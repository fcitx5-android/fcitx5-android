package me.rocka.fcitx5test.keyboard.layout

object Preset {
    val Qwerty = listOf(
        listOf(
            LongPressButton("Q", "1"),
            LongPressButton("W", "2"),
            LongPressButton("E", "3"),
            LongPressButton("R", "4"),
            LongPressButton("T", "5"),
            LongPressButton("Y", "6"),
            LongPressButton("U", "7"),
            LongPressButton("I", "8"),
            LongPressButton("O", "9"),
            LongPressButton("P", "0")
        ),
        listOf(
            LongPressButton("A", "@"),
            LongPressButton("S", "*"),
            LongPressButton("D", "+"),
            LongPressButton("F", "-"),
            LongPressButton("G", "="),
            LongPressButton("H", "/"),
            LongPressButton("J", "#"),
            LongPressButton("K", "("),
            LongPressButton("L", ")")
        ),
        listOf(
            CapsButton(),
            LongPressButton("Z", "'"),
            LongPressButton("X", ":"),
            LongPressButton("C", "\""),
            LongPressButton("V", "?"),
            LongPressButton("B", "!"),
            LongPressButton("N", "~"),
            LongPressButton("M", "\\"),
            BackspaceButton()
        ),
        listOf(
            QuickPhraseButton(),
            LangSwitchButton(),
            SpaceButton(),
            LongPressButton(",", "."),
            ReturnButton()
        ),
    )
}