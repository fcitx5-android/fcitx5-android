package org.fcitx.fcitx5.android.input.dependency

import android.view.ContextThemeWrapper
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.InputView
import org.mechdancer.dependency.UniqueComponentWrapper
import org.mechdancer.dependency.manager.DependencyManager
import org.mechdancer.dependency.manager.mustWrapped


fun DependencyManager.fcitx() =
    mustWrapped<UniqueComponentWrapper<Fcitx>, Fcitx>()

fun DependencyManager.context() =
    mustWrapped<UniqueComponentWrapper<ContextThemeWrapper>, ContextThemeWrapper>()

fun DependencyManager.inputView() =
    mustWrapped<UniqueComponentWrapper<InputView>, InputView>()

fun DependencyManager.inputMethodService() =
    mustWrapped<UniqueComponentWrapper<FcitxInputMethodService>, FcitxInputMethodService>()
