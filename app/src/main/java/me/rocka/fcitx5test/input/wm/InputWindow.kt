package me.rocka.fcitx5test.input.wm

import android.view.View
import me.rocka.fcitx5test.input.dependency.context
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.IUniqueComponent
import org.mechdancer.dependency.ScopeEvent
import org.mechdancer.dependency.manager.DependencyManager
import kotlin.reflect.KClass

abstract class InputWindow<T : InputWindow<T>> : IUniqueComponent<T>, Dependent {

    protected val manager: DependencyManager = DependencyManager()

    protected val context by manager.context()

    abstract val view: View

    open val title: String?
        get() = null

    open fun onShow() {}

    open val barExtension: View? = null

    val isAttached
        get() = view.isAttachedToWindow

    override val type: KClass<out IUniqueComponent<*>> by lazy { defaultType() }

    override fun equals(other: Any?): Boolean = defaultEquals(other)

    override fun hashCode(): Int = defaultHashCode()

    override fun handle(scopeEvent: ScopeEvent) = manager.handle(scopeEvent)

}