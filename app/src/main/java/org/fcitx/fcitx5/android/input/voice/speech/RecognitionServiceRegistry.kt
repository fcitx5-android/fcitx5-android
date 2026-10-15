/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.voice.speech

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.speech.RecognitionService

/**
 * A RecognitionService implementation installed on the device that can be bound with
 * `SpeechRecognizer.createSpeechRecognizer(context, component)`.
 */
data class RecognitionServiceCandidate(
    val component: ComponentName,
    val label: String
)

/**
 * Enumerates installed [RecognitionService] implementations. This requires the
 * `android.speech.RecognitionService` intent to be declared in the manifest `<queries>`.
 */
object RecognitionServiceRegistry {

    fun query(context: Context): List<RecognitionServiceCandidate> {
        val pm = context.packageManager
        val intent = Intent(RecognitionService.SERVICE_INTERFACE)
        return try {
            pm.queryIntentServices(intent, 0).orEmpty()
                .mapNotNull { info ->
                    val si = info.serviceInfo ?: return@mapNotNull null
                    RecognitionServiceCandidate(
                        ComponentName(si.packageName, si.name),
                        si.loadLabel(pm).toString()
                    )
                }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isAvailable(context: Context, component: ComponentName): Boolean =
        query(context).any { it.component == component }

    /**
     * Parse a stored flattened component name (`package/class`); empty or unparseable
     * values mean "use the system default service".
     */
    fun parseComponent(raw: String?): ComponentName? {
        if (raw.isNullOrBlank()) return null
        return try {
            ComponentName.unflattenFromString(raw)
        } catch (e: Exception) {
            null
        }
    }
}
