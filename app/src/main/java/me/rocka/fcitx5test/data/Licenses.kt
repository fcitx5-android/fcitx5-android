package me.rocka.fcitx5test.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rocka.fcitx5test.utils.appContext
import org.json.JSONObject

object Licenses {
    data class LibraryLicense(val libraryName: String, val licenseUrl: String?)

    private var parsed: List<LibraryLicense>? = null

    suspend fun getAll(): Result<List<LibraryLicense>> = runCatching {
        parsed?.let { return@runCatching it }
        withContext(Dispatchers.IO) {
            val content = appContext.assets.open(licensesJSON).bufferedReader().readText()
            val jObject = JSONObject(content)
            val jArray = jObject.getJSONArray("libraries")
            val list = mutableListOf<LibraryLicense>()
            for (i in 0 until jArray.length()) {
                val library = jArray.getJSONObject(i)
                val libraryName = library.getString("libraryName")
                val licenseUrl = runCatching { library.getString("licenseUrl") }.getOrNull()
                list.add(LibraryLicense(libraryName, licenseUrl))
            }
            parsed = list
            list
        }
    }

    private const val licensesJSON = "licenses.json"
}