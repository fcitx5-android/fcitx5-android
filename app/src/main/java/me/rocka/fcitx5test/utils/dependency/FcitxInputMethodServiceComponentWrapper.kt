package me.rocka.fcitx5test.utils.dependency

import me.rocka.fcitx5test.keyboard.FcitxInputMethodService
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.DependencyManager

class FcitxInputMethodServiceComponentWrapper(val service: FcitxInputMethodService) :
    UniqueComponent<FcitxInputMethodServiceComponentWrapper>()

fun wrapFcitxInputMethodService(service: FcitxInputMethodService) =
    FcitxInputMethodServiceComponentWrapper(service)

fun DependencyManager.service() =
    must<FcitxInputMethodServiceComponentWrapper, FcitxInputMethodService>({ true }) { it.service }