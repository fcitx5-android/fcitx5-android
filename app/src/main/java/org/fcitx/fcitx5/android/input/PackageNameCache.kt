package org.fcitx.fcitx5.android.input

import android.content.Context

class PackageNameCache(private val ctx: Context) : HashMap<Int, String>() {
    fun forUid(uid: Int): String {
        val cached = get(uid)
        if (cached != null) return cached
        val name = ctx.packageManager.getNameForUid(uid)
        if (name != null) {
            put(uid, name)
            return name
        }
        return uid.toString()
    }
}
