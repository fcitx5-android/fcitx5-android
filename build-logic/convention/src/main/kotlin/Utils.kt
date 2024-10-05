/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import kotlinx.serialization.json.Json
import org.gradle.api.Project

val json = Json { prettyPrint = true }

inline fun envOrDefault(env: String, default: () -> String) =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: default()

inline fun Project.propertyOrDefault(prop: String, default: () -> String) =
    runCatching { property(prop)!!.toString() }.getOrElse { default() }

internal inline fun Project.ep(env: String, prop: String, block: () -> String) =
    envOrDefault(env) {
        propertyOrDefault(prop) {
            block()
        }
    }

fun Project.epn(env: String, prop: String) =
    System.getenv(env)?.takeIf { it.isNotBlank() }
        ?: runCatching { property(prop)!!.toString() }.getOrNull()

fun Project.epr(env: String, prop: String) = ep(env, prop) {
    error("Neither environment variable $env nor project property $prop is set")
}

fun String.capitalized(): String =
    if (get(0).isUpperCase()) this else replaceFirstChar { get(0).uppercaseChar() }
