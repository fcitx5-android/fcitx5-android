/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import java.io.ByteArrayOutputStream
import java.io.File

inline fun envOrDefault(env: String, default: () -> String) =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: default()

inline fun Project.propertyOrDefault(prop: String, default: () -> String) =
    runCatching { property(prop)!!.toString() }.getOrElse {
        default()
    }

fun Project.runCmd(cmd: String): String = ByteArrayOutputStream().use {
    project.exec {
        commandLine = cmd.split(" ")
        standardOutput = it
    }
    it.toString().trim()
}

val json = Json { prettyPrint = true }

internal inline fun Project.ep(env: String, prop: String, block: () -> String) =
    envOrDefault(env) {
        propertyOrDefault(prop) {
            block()
        }
    }

val Project.versionCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

val Project.assetsDir: File
    get() = file("src/main/assets").also { it.mkdirs() }

val Project.cleanTask: Task
    get() = tasks.getByName("clean")

// Change default ABI here
val Project.buildABI
    get() = ep("BUILD_ABI", "buildABI") {
//        "armeabi-v7a"
        "arm64-v8a"
//        "x86"
//        "x86_64"
    }

val Project.buildVersionName
    get() = ep("BUILD_VERSION_NAME", "buildVersionName") {
        runCmd("git describe --tags --long --always")
    }

val Project.buildCommitHash
    get() = ep("BUILD_COMMIT_HASH", "buildCommitHash") {
        runCmd("git rev-parse HEAD")
    }

val Project.buildTimestamp
    get() = ep("BUILD_TIMESTAMP", "buildTimestamp") {
        System.currentTimeMillis().toString()
    }
