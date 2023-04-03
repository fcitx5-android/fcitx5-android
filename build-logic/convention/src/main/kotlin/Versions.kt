import org.gradle.api.Project

object Versions {

    const val jvmVersion = "11"
    const val compileSdkVersion = 33
    const val buildToolsVersion = "33.0.0"
    const val minSdkVersion = 23
    const val targetSdkVersion = 33

    // NOTE: increase this value to bump version code
    private const val baseVersionCode = 3
    private const val defaultCmakeVersion = "3.22.1"
    private const val defaultNDKVersion = "25.0.8775105"

    fun calculateVersionCode(abi: String): Int {
        val abiId = when (abi) {
            "armeabi-v7a" -> 1
            "arm64-v8a" -> 2
            "x86" -> 3
            "x86_64" -> 4
            else -> 0
        }
        return baseVersionCode * 10 + abiId
    }

    val Project.cmakeVersion
        get() = eep("cmakeVersion", "CMAKE_VERSION") { defaultCmakeVersion }

    val Project.ndkVersion
        get() = eep("ndkVersion", "NDK_VERSION") { defaultNDKVersion }
}