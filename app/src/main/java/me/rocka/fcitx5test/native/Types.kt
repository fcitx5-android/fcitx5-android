package me.rocka.fcitx5test.native


data class InputMethodEntry(
    val uniqueName: String,
    val name: String,
    val icon: String,
    val nativeName: String,
    val label: String,
    val languageCode: String,
    val isConfigurable: Boolean
)
