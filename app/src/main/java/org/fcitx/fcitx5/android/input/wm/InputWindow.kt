/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.wm

import android.view.Gravity
import android.view.View
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.Transition
import org.fcitx.fcitx5.android.input.dependency.context
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.IUniqueComponent
import org.mechdancer.dependency.ScopeEvent
import org.mechdancer.dependency.manager.DependencyManager
import kotlin.reflect.KClass

sealed class InputWindow : Dependent {

    protected val manager: DependencyManager = DependencyManager()

    protected val context by manager.context()

    /**
     * Animation when the window is added to the layout
     */
    open fun enterAnimation(lastWindow: InputWindow): Transition? = Slide().apply {
        slideEdge = Gravity.TOP
    }

    /**
     * Animation when the window is removed from the layout
     */
    open fun exitAnimation(nextWindow: InputWindow): Transition? = Fade()

    /**
     * After the window was set up in dynamic scope
     */
    abstract fun onCreateView(): View

    /**
     * After the view was added to window manager's layout
     */
    abstract fun onAttached()

    /**
     * Before the view is removed from window manager's layout
     */
    abstract fun onDetached()

    final override fun handle(scopeEvent: ScopeEvent) = manager.handle(scopeEvent)

    abstract class SimpleInputWindow<T : SimpleInputWindow<T>> : IUniqueComponent<T>,
        InputWindow() {

        override val type: KClass<out IUniqueComponent<*>> by lazy { defaultType() }

        override fun equals(other: Any?): Boolean = defaultEquals(other)

        override fun hashCode(): Int = defaultHashCode()

        override fun toString(): String = javaClass.name
    }

    abstract class ExtendedInputWindow<T : ExtendedInputWindow<T>> : IUniqueComponent<T>,
        InputWindow() {

        open val showTitle: Boolean = true

        open val title: String = ""

        open fun onCreateBarExtension(): View? {
            return null
        }

        override val type: KClass<out IUniqueComponent<*>> by lazy { defaultType() }

        override fun equals(other: Any?): Boolean = defaultEquals(other)

        override fun hashCode(): Int = defaultHashCode()

        override fun toString(): String = javaClass.name

    }
}