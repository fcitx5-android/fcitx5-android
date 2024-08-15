/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
import com.android.build.gradle.tasks.ProcessLibraryArtProfileTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType

class AndroidLibConventionPlugin : AndroidBaseConventionPlugin() {

    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.library")

        super.apply(target)

        // disable baseline profile tasks
        target.tasks.withType<ProcessLibraryArtProfileTask> { enabled = false }
    }

}
