package me.rocka.fcitx5test.utils.dependency

import android.view.View
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.firstGenericType
import org.mechdancer.dependency.manager.DependencyManager
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

abstract class UniqueViewComponent<T : UniqueViewComponent<T, V>, V : View>(type: KClass<T>? = null) :
    Dependent,
    ManagedHandler by managedHandler() {

    private val type = type ?: javaClass.kotlin.firstGenericType(UniqueViewComponent::class)

    abstract val view: V

    override fun equals(other: Any?) =
        this === other || type.safeCast(other) !== null

    override fun hashCode() =
        type.hashCode()
}

inline fun <reified C : UniqueViewComponent<C, *>>
        DependencyManager.uniqueView() = must<C> { true }