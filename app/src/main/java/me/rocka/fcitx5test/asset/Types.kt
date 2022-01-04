package me.rocka.fcitx5test.asset

typealias SHA256 = String
typealias AssetDescriptor = Pair<SHA256, Map<String, SHA256>>

sealed class Diff {
    abstract val key: String

    data class New(override val key: String, val new: String) : Diff()
    data class Update(override val key: String, val old: String, val new: String) : Diff()
    data class Delete(override val key: String, val old: String) : Diff()
}