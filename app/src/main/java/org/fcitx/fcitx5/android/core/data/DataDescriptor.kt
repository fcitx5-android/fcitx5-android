/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core.data

import kotlinx.serialization.Serializable

typealias SHA256 = String

/**
 * A list of files with sha256
 */
@Serializable
data class DataDescriptor(
    /**
     * Implementation dependent, will be used to quick check if two descriptors are the same
     */
    val sha256: SHA256,
    /**
     * path -> sha256
     * sha256 will be empty if the path is a directory
     */
    val files: Map<String, SHA256>,
    /**
     * Symbolic links from target -> source
     */
    val symlinks: Map<String, String> = mapOf()
)