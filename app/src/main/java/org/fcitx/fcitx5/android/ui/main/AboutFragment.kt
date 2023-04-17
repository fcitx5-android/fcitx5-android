package org.fcitx.fcitx5.android.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.navigation.fragment.findNavController
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.common.PaddingPreferenceFragment
import org.fcitx.fcitx5.android.utils.Const
import org.fcitx.fcitx5.android.utils.formatDateTime

class AboutFragment : PaddingPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)

        screen.addPreference(Preference(context).apply {
            setTitle(R.string.privacy_policy)
            isIconSpaceReserved = false
            isSingleLineTitle = false
            setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Const.privacyPolicyUrl)))
                true
            }
        })

        screen.addPreference(Preference(context).apply {
            setTitle(R.string.open_source_licenses)
            setSummary(R.string.licenses_of_third_party_libraries)
            isIconSpaceReserved = false
            isSingleLineTitle = false
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_aboutFragment_to_licensesFragment)
                true
            }
        })

        screen.addPreference(Preference(context).apply {
            setTitle(R.string.source_code)
            setSummary(R.string.github_repo)
            isIconSpaceReserved = false
            isSingleLineTitle = false
            setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Const.githubRepo)))
                true
            }
        })

        screen.addPreference(Preference(context).apply {
            setTitle(R.string.license)
            summary = Const.licenseSpdxId
            isIconSpaceReserved = false
            isSingleLineTitle = false
            setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Const.licenseUrl)))
                true
            }
        })

        val version = PreferenceCategory(context)
            .also { screen.addPreference(it) }

        version.apply {
            setTitle(R.string.version)
            isIconSpaceReserved = false
            isSingleLineTitle = false
        }

        val versionName = Preference(context).apply {
            setTitle(R.string.current_version)
            summary = Const.versionName
            isCopyingEnabled = true
            isIconSpaceReserved = false
            isSingleLineTitle = false
        }
        version.addPreference(versionName)

        version.addPreference(Preference(context).apply {
            setTitle(R.string.build_git_hash)
            isIconSpaceReserved = false
            isSingleLineTitle = false
            summary = Const.buildGitHash
            isCopyingEnabled = true
            setOnPreferenceClickListener {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("${Const.githubRepo}/commit/${Const.buildGitHash.takeWhile { it != '-' }}")
                    )
                )
                true
            }
        })
        version.addPreference(Preference(context).apply {
            setTitle(R.string.build_time)
            isIconSpaceReserved = false
            isSingleLineTitle = false
            isCopyingEnabled = true
            summary = formatDateTime(Const.buildTime)
        })

        preferenceScreen = screen
    }
}
