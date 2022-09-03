package org.fcitx.fcitx5.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.utils.appContext

object Licenses {

    @Serializable
    data class LibraryLicense(
        val artifactId: LibraryArtifactID,
        val license: String? = null,
        val licenseUrl: String? = null,
        val normalizedLicense: String? = null,
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
                    ListSerializer(LibraryLicense.serializer())
                ),
                content
            )["libraries"]!!
                .filter { !it.licenseUrl.isNullOrEmpty() }
                .sortedBy { it.libraryName }
            parsed = list
            list
        }
    }

    private const val licensesJSON = "licenses.json"
}