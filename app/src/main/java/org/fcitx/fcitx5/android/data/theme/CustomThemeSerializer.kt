/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.theme

import arrow.core.compose
import kotlinx.serialization.json.*
import org.fcitx.fcitx5.android.utils.NostalgicSerializer
import org.fcitx.fcitx5.android.utils.identity
import org.fcitx.fcitx5.android.utils.upcast

object CustomThemeSerializer : JsonTransformingSerializer<Theme.Custom>(Theme.Custom.serializer()) {

    val WithMigrationStatus = NostalgicSerializer(this) {
        it.jsonObject[VERSION]?.jsonPrimitive?.content != CURRENT_VERSION
    }

    override fun transformSerialize(element: JsonElement): JsonElement =
        element.jsonObject.addVersion()

    override fun transformDeserialize(element: JsonElement): JsonElement {
        val version = element.jsonObject[VERSION]?.let {
            val version = it.jsonPrimitive.content
            if (version !in knownVersions)
                error("$version is not in known versions: $knownVersions")
            version
        } ?: FALLBACK_VERSION
        return applyStrategy(version, element.jsonObject).removeVersion()
    }

    private fun JsonObject.addVersion() =
        JsonObject(this + (VERSION to JsonPrimitive(CURRENT_VERSION)))

    private fun JsonObject.removeVersion() =
        JsonObject(this - VERSION)


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
            MigrationStrategy("2.0") {
                JsonObject(it.toMutableMap().apply {
                    if (get("backgroundImage") != null) {
                        val popupBkgColor = if (getValue("isDark").jsonPrimitive.boolean) {
                            ThemePreset.PixelDark.popupBackgroundColor
                        } else {
                            ThemePreset.PixelLight.popupBackgroundColor
                        }
                        put("popupBackgroundColor", JsonPrimitive(popupBkgColor))
                        put("popupTextColor", getValue("keyTextColor"))
                        put("genericActiveBackgroundColor", getValue("accentKeyBackgroundColor"))
                        put("genericActiveForegroundColor", getValue("accentKeyTextColor"))
                    } else {
                        put("popupBackgroundColor", getValue("barColor"))
                        put("popupTextColor", getValue("keyTextColor"))
                        put("genericActiveBackgroundColor", getValue("accentKeyBackgroundColor"))
                        put("genericActiveForegroundColor", getValue("accentKeyTextColor"))
                    }
                })
            },
            MigrationStrategy("1.0", JsonObject::identity),
        )

    private const val VERSION = "version"

    private const val CURRENT_VERSION = "2.0"
    private const val FALLBACK_VERSION = "1.0"

    private val knownVersions = strategies.map { it.version }

}