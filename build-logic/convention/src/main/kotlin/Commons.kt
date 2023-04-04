import org.gradle.api.Project

val Project.buildVersionName
    get() = eep("buildVersionName", "BUILD_VERSION_NAME") {
        runCmd("git describe --tags --long --always")
    }

val Project.buildCommitHash
    get() = eep("buildCommitHash", "BUILD_COMMIT_HASH") {
        runCmd("git rev-parse HEAD")
    }
val Project.buildTimestamp
    get() = eep("buildTimestamp", "BUILD_TIMESTAMP") {
        System.currentTimeMillis().toString()
    }

// Change default ABI here
val Project.buildABI
    get() = eep("buildABI", "BUILD_ABI") {
//        "armeabi-v7a"
        "arm64-v8a"
//        "x86"
//        "x86_64"
    }

const val dataDescriptorName = "descriptor.json"