package org.fcitx.fcitx5.android.data

import arrow.core.flattenOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.utils.OptionSerializer
import org.fcitx.fcitx5.android.utils.appContext

object Licenses {

    @Serializable
    data class LibraryLicense(
        val artifactId: LibraryArtifactID,
        val license: String,
        val licenseUrl: String,
        val normalizedLicense: String,
        val url: String? = null,
        val libraryName: String,
    )

    @Serializable
    data class LibraryArtifactID(
        val name: String,
        val group: String,
        val version: String
    )

    private var parsed: List<LibraryLicense>? = null

    suspend fun getAll(): Result<List<LibraryLicense>> = runCatching {
        parsed?.let { return@runCatching it }
        withContext(Dispatchers.IO) {
            val content =
                appContext.assets.open(licensesJSON).bufferedReader().use { x -> x.readText() }
            val list = Json.decodeFromString(
                MapSerializer(
                    String.serializer(),
                    ListSerializer(OptionSerializer(LibraryLicense.serializer()))
                ),
                content
            )["libraries"]!!.flattenOption().sortedBy { it.libraryName }
            parsed = list
            list
        }
    }

    private const val licensesJSON = "licenses.json"
}