package org.fcitx.fcitx5.android.utils.resource

import android.content.Context

class CachedContextF<out T>(private val block: (Context) -> T) : ContextF<T> {

    private var cached: T? = null

    override fun resolve(context: Context): T =
        cached ?: block(context).also { cached = it }

    override fun <U> map(block: (T) -> U): CachedContextF<U> =
        super.map(block).toCached()

    override fun <U> flatMap(f: (T) -> ContextF<U>): CachedContextF<U> =
        super.flatMap(f).toCached()
}

fun <T> ContextF<T>.toCached(): CachedContextF<T> = CachedContextF { resolve(it) }