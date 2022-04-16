package org.fcitx.fcitx5.android.utils

import android.content.Context

fun interface ContextF<out T> {

    fun resolve(context: Context): T

    fun <U> map(block: (T) -> U): ContextF<U> =
        ContextF { context -> block(this@ContextF.resolve(context)) }

    fun <U> flatMap(f: (T) -> ContextF<U>): ContextF<U> =
        ContextF { context -> f(this@ContextF.resolve(context)).resolve(context) }
}