package org.fcitx.fcitx5.android.input.dependency

import android.view.ContextThemeWrapper
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.InputView
import org.mechdancer.dependency.UniqueComponentWrapper
import org.mechdancer.dependency.manager.DependencyManager
import org.mechdancer.dependency.manager.mustWrapped


fun DependencyManager.fcitx() =
    mustWrapped<UniqueComponentWrapper<FcitxConnection>, FcitxConnection>()

fun DependencyManager.context() =
    mustWrapped<UniqueComponentWrapper<ContextThemeWrapper>, ContextThemeWrapper>()

fun DependencyManager.inputView() =
    mustWrapped<UniqueComponentWrapper<InputView>, InputView>()

fun DependencyManager.inputMethodService() =
    mustWrapped<UniqueComponentWrapper<FcitxInputMethodService>, FcitxInputMethodService>()

fun DependencyManager.theme() =
    mustWrapped<UniqueComponentWrapper<Theme>, Theme>()