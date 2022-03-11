package me.rocka.fcitx5test.input.dependency

import android.view.View
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.IUniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import kotlin.reflect.KClass

abstract class UniqueViewComponent<T : UniqueViewComponent<T, V>, V : View> :
    IUniqueComponent<T>,
    Dependent,
    ManagedHandler by managedHandler() {

    abstract val view: V

    override val type: KClass<out IUniqueComponent<*>> by lazy { defaultType() }

    override fun equals(other: Any?): Boolean = defaultEquals(other)

    override fun hashCode(): Int = defaultHashCode()

}

