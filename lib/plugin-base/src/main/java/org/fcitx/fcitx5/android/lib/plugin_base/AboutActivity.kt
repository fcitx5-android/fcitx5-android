@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package org.fcitx.fcitx5.android.lib.plugin_base

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
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

        private fun PreferenceScreen.addPreference(title: String, summary: String) {
            addPreference(Preference(context).apply {
                setTitle(title)
                setSummary(summary)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isSingleLineTitle = false
                    isIconSpaceReserved = false
                }
            })
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val pkg = context.packageName
            val pluginXmlRes = resources.getIdentifier("plugin", "xml", pkg)
            if (pluginXmlRes == 0) return
            val parser = resources.getXml(pluginXmlRes)
            var eventType = parser.eventType
            var domain = ""
            var apiVersion = ""
            var description = ""
            var text = ""
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
                    }
                }
                eventType = parser.next()
            }
            preferenceScreen = preferenceManager.createPreferenceScreen(context).apply {
                addPreference(getString(R.string.pkg_name), pkg)
                addPreference(getString(R.string.api_version), apiVersion)
                addPreference(getString(R.string.gettext_domain), domain)
                addPreference(getString(R.string.plugin_description), description)
            }
        }
    }

}
