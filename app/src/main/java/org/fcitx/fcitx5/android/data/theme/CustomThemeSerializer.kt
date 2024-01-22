/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.theme

import arrow.core.compose
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.fcitx.fcitx5.android.utils.NostalgicSerializer

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

    private val EmptyTransform: (JsonObject) -> JsonObject = { it }

    private fun applyStrategy(oldVersion: String, obj: JsonObject) =
        strategies
            .takeWhile { it.version != oldVersion }
            .foldRight(EmptyTransform) { it, acc -> it.transformation compose acc }
            .invoke(obj)

    data class MigrationStrategy(
        val version: String,
        val transformation: (JsonObject) -> JsonObject
    )

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
            MigrationStrategy("1.0", EmptyTransform)
        )

    private const val VERSION = "version"

    private const val CURRENT_VERSION = "2.0"
    private const val FALLBACK_VERSION = "1.0"

    private val knownVersions = strategies.map { it.version }

}