package me.rocka.fcitx5test.utils.dependency

import me.rocka.fcitx5test.input.InputView
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.DependencyManager

class InputViewWrapper(val inputView: InputView) : UniqueComponent<InputViewWrapper>()

fun wrapInputView(inputView: InputView) = InputViewWrapper(inputView)

fun DependencyManager.inputView() =
    must<InputViewWrapper, InputView>({ true }) { it.inputView }