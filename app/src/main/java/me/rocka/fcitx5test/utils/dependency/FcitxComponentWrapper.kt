package me.rocka.fcitx5test.utils.dependency

import me.rocka.fcitx5test.core.Fcitx
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.DependencyManager

class FcitxComponentWrapper(val fcitx: Fcitx) : UniqueComponent<FcitxComponentWrapper>()

fun wrapFcitx(fcitx: Fcitx) = FcitxComponentWrapper(fcitx)

fun DependencyManager.fcitx() = must<FcitxComponentWrapper, Fcitx>({ true }) { it.fcitx }