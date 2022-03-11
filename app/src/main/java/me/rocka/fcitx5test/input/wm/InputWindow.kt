package me.rocka.fcitx5test.input.wm

import android.view.View
import me.rocka.fcitx5test.input.dependency.context
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.IUniqueComponent
import org.mechdancer.dependency.ScopeEvent
import org.mechdancer.dependency.manager.DependencyManager
import kotlin.reflect.KClass

sealed class InputWindow : Dependent {

    protected val manager: DependencyManager = DependencyManager()

    protected val context by manager.context()

    abstract val view: View

    abstract fun onAttached()

    abstract fun onDetached()

    var isAttached = false
        private set

    open fun onShow() {}

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

        abstract val title: String

        open val barExtension: View? = null

        override val type: KClass<out IUniqueComponent<*>> by lazy { defaultType() }

        override fun equals(other: Any?): Boolean = defaultEquals(other)

        override fun hashCode(): Int = defaultHashCode()

        override fun toString(): String = javaClass.name

    }
}