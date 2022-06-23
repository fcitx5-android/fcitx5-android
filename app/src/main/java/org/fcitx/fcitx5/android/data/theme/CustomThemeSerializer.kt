package org.fcitx.fcitx5.android.data.theme

import arrow.core.compose
import kotlinx.serialization.json.*
import org.fcitx.fcitx5.android.utils.identity
import org.fcitx.fcitx5.android.utils.upcast

object CustomThemeSerializer : JsonTransformingSerializer<Theme.Custom>(Theme.Custom.serializer()) {
    override fun transformSerialize(element: JsonElement): JsonElement =
        element.jsonObject.addVersion()

    override fun transformDeserialize(element: JsonElement): JsonElement {
        val version = element.jsonObject["version"]?.let {
            val version = it.jsonPrimitive.content
            if (version !in knownVersions)
                error("$version is not in known versions: $knownVersions")
            version
        } ?: FALLBACK_VERSION
        return applyStrategy(version, element.jsonObject).removeVersion()
    }

    private fun JsonObject.addVersion() =
        JsonObject(this + ("version" to JsonPrimitive(CURRENT_VERSION)))

    private fun JsonObject.removeVersion() =
        JsonObject(this - "version")


    private fun applyStrategy(oldVersion: String, obj: JsonObject) =
        strategies
            .takeWhile { it.version != oldVersion }
            .foldRight(JsonObject::identity.upcast()) { f, acc -> f compose acc }
            .invoke(obj)

    data class MigrationStrategy(
        val version: String,
        val transformation: (JsonObject) -> JsonObject
    ) : (JsonObject) -> JsonObject by transformation

    private val strategies: List<MigrationStrategy> =
        // Add migrations here
        listOf(
            // Nothing to do for the initial version...
            MigrationStrategy("1.0", JsonObject::identity)
        ).sortedByDescending { it.version }


    private const val CURRENT_VERSION = "1.0"
    private const val FALLBACK_VERSION = "1.0"

    private val knownVersions = strategies.map { it.version }

}