/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import org.gradle.api.Project
import java.io.File

object PlayRelease {
    private fun Project.epn(env: String, prop: String) =
        System.getenv(env)?.takeIf { it.isNotBlank() }
            ?: runCatching { property(prop)!!.toString() }.getOrNull()

    private fun Project.epr(env: String, prop: String) = ep(
        env,
        prop
    ) { error("Neither environment variable $env nor project property $prop is set") }

    val Project.storeFile
        get() = epn("PLAY_STORE_FILE", "playStoreFile")

    val Project.storePassword
        get() = epr("PLAY_STORE_PASSWORD", "playStorePassword")

    val Project.keyAlias
        get() = epr("PLAY_KEY_ALIAS", "playKeyAlias")

    val Project.keyPassword
        get() = epr("PLAY_KEY_PASSWORD", "playKeyPassword")

    val Project.buildPlayRelease
        get() = storeFile?.let { File(it).exists() } ?: false
}

