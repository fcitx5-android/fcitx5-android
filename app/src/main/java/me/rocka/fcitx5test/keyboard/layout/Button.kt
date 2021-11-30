package me.rocka.fcitx5test.keyboard.layout

open class BaseButton(val text: String) {
    open fun onPress() : ButtonAction<*> = ButtonAction.FcitxKeyAction(text)
}

open class LongPressButton(text: String, val altText: String) : BaseButton(text) {
    open fun onLongPress() : ButtonAction<*> = ButtonAction.FcitxKeyAction(altText)
}

open class CapsButton() : BaseButton("") {
    override fun onPress() = ButtonAction.CapsAction()
}

open class BackspaceButton() : BaseButton("") {
    override fun onPress() = ButtonAction.BackspaceAction()
}

open class QuickPhraseButton() : BaseButton(""){
    override fun onPress() = ButtonAction.QuickPhraseAction()
}

open class LangSwitchButton() : LongPressButton("", ""){
    override fun onPress() = ButtonAction.LangSwitchAction()
    override fun onLongPress() = ButtonAction.InputMethodSwitchAction()
}

open class SpaceButton() : BaseButton(""){
    override fun onPress() = ButtonAction.FcitxKeyAction(" ")
}

open class ReturnButton : BaseButton(""){
    override fun onPress() = ButtonAction.ReturnAction()
}
