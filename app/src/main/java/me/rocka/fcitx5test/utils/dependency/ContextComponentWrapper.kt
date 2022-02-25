package me.rocka.fcitx5test.utils.dependency

import android.content.Context
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.DependencyManager

class ContextComponentWrapper(val context: Context) : UniqueComponent<ContextComponentWrapper>()

fun wrapContext(context: Context) = ContextComponentWrapper(context)

fun DependencyManager.context() =
    must<ContextComponentWrapper, Context>({ true }) { it.context }