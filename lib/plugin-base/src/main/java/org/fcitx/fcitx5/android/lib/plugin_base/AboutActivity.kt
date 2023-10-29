@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

/**
 * Copyright (C) 2021-2023 Fcitx 5 for Android Contributors
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.lib.plugin_base

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.Preference.OnPreferenceClickListener
import android.preference.PreferenceActivity
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.widget.Toast
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.License
import org.xmlpull.v1.XmlPullParser

@SuppressLint("ExportedPreferenceActivity")
class AboutActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction()
            .replace(android.R.id.content, AboutContentFragment())
            .commit()
    }

    class AboutContentFragment : PreferenceFragment() {

        private val copyPreferenceSummaryListener = OnPreferenceClickListener {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("", it.summary))
            Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
            true
        }

        private fun PreferenceScreen.addCategory(
            title: Int,
            init: PreferenceCategory.() -> Unit
        ) {
            PreferenceCategory(context).also {
                it.title = getString(title)
                addPreference(it)
                init.invoke(it)
            }
        }

        private fun PreferenceCategory.addPreference(
            title: String,
            summary: String,
            onClick: OnPreferenceClickListener? = null
        ) {
            addPreference(Preference(context).apply {
                setTitle(title)
                setSummary(summary)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isSingleLineTitle = false
                    isIconSpaceReserved = false
                }
                onPreferenceClickListener = onClick ?: copyPreferenceSummaryListener
            })
        }

        private fun PreferenceCategory.addPreference(
            title: Int,
            summary: String,
            onClick: OnPreferenceClickListener? = null
        ) {
            addPreference(getString(title), summary, onClick)
        }

        @SuppressLint("DiscouragedApi")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val screen = preferenceManager.createPreferenceScreen(context)
            val pkg = context.packageName
            val pluginXmlRes = resources.getIdentifier("plugin", "xml", pkg)
            if (pluginXmlRes == 0) return
            val parser = resources.getXml(pluginXmlRes)
            var eventType = parser.eventType
            var domain = ""
            var apiVersion = ""
            var description = ""
            var text = ""
            var hasService = false
            while ((eventType != XmlPullParser.END_DOCUMENT)) {
                when (eventType) {
                    XmlPullParser.TEXT -> text = parser.text
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "apiVersion" -> apiVersion = text
                        "domain" -> domain = text
                        "description" -> description = text.let {
                            if (it.startsWith("@string/")) {
                                val resName = it.removePrefix("@string/")
                                val resId = resources.getIdentifier(resName, "string", pkg)
                                if (resId != 0) getString(resId) else it
                            } else it
                        }
                        "hasService" -> hasService = text.lowercase() == "true"
                    }
                }
                eventType = parser.next()
            }
            screen.addCategory(R.string.plugin_info) {
                addPreference(R.string.pkg_name, pkg)
                addPreference(R.string.api_version, apiVersion)
                addPreference(R.string.gettext_domain, domain)
                addPreference(R.string.plugin_description, description)
                addPreference(R.string.has_service, hasService.toString())
            }
            resources.getIdentifier("aboutlibraries", "raw", pkg).also { resId ->
                if (resId == 0) return@also
                val jsonString = resources.openRawResource(resId)
                    .bufferedReader()
                    .use { reader -> reader.readText() }
                val libraries = Libs.Builder()
                    .withJson(jsonString)
                    .build()
                    .libraries
                    .sortedBy {
                        if (it.tag == "native") it.uniqueId.uppercase() else it.uniqueId.lowercase()
                    }
                screen.addCategory(R.string.licenses) {
                    libraries.forEach {
                        addPreference(
                            title = "${it.uniqueId}:${it.artifactVersion}",
                            summary = it.licenses.joinToString { l -> l.spdxId ?: l.name }
                        ) { _ ->
                            showLicenseDialog(it.uniqueId, it.licenses)
                        }
                    }
                }
            }
            preferenceScreen = screen
        }

        private fun showLicenseDialog(uniqueId: String, licenses: Set<License>): Boolean {
            when (licenses.size) {
                0 -> {}
                1 -> showLicenseContent(licenses.first())
                else -> {
                    val licenseArray = licenses.toTypedArray()
                    val licenseNames = licenseArray.map { it.spdxId ?: it.name }.toTypedArray()
                    AlertDialog.Builder(context)
                        .setTitle(uniqueId)
                        .setItems(licenseNames) { _, idx ->
                            showLicenseContent(licenseArray[idx])
                        }
                        .setPositiveButton(android.R.string.cancel, null)
                        .show()
                }
            }
            return true
        }

        private fun showLicenseContent(license: License) {
            if (license.url?.isNotBlank() == true) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(license.url)))
            }
        }
    }

}
