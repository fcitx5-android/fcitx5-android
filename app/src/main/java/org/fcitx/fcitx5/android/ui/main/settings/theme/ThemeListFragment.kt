package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.ui.common.withLoadingDialog
import org.fcitx.fcitx5.android.utils.applyNavBarInsetsBottomPadding
import org.fcitx.fcitx5.android.utils.errorDialog
import org.fcitx.fcitx5.android.utils.queryFileName
import org.fcitx.fcitx5.android.utils.toast
import splitties.resources.styledDrawable
import java.util.UUID

class ThemeListFragment : Fragment() {

    private lateinit var imageLauncher: ActivityResultLauncher<Theme.Custom?>

    private lateinit var importLauncher: ActivityResultLauncher<String>

    private lateinit var exportLauncher: ActivityResultLauncher<String>

    private lateinit var themeListAdapter: ThemeListAdapter

    private var followSystemDayNightTheme by ThemeManager.prefs.followSystemDayNightTheme

    private var beingExported: Theme.Custom? = null

    @Keep
    private val onThemeChangeListener = ThemeManager.OnThemeChangeListener {
        lifecycleScope.launch {
            updateSelectedThemes(it)
        }
    }

    private suspend fun importErrorDialog(message: String) {
        errorDialog(requireContext(), getString(R.string.import_error), message)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageLauncher = registerForActivityResult(CustomThemeActivity.Contract()) { result ->
            if (result != null) {
                when (result) {
                    is CustomThemeActivity.BackgroundResult.Created -> {
                        val theme = result.theme
                        themeListAdapter.prependTheme(theme)
                        ThemeManager.saveTheme(theme)
                        if (!followSystemDayNightTheme) {
                            ThemeManager.switchTheme(theme)
                        }
                    }
                    is CustomThemeActivity.BackgroundResult.Deleted -> {
                        val name = result.name
                        // Update the list first, as we rely on theme changed listener
                        // in the case that the deleted theme was active
                        themeListAdapter.removeTheme(name)
                        ThemeManager.deleteTheme(name)
                    }
                    is CustomThemeActivity.BackgroundResult.Updated -> {
                        val theme = result.theme
                        themeListAdapter.replaceTheme(theme)
                        ThemeManager.saveTheme(theme)
                    }
                }
            }
        }
        importLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri == null) return@registerForActivityResult
                val ctx = requireContext()
                val cr = ctx.contentResolver
                lifecycleScope.withLoadingDialog(ctx) {
                    withContext(NonCancellable + Dispatchers.IO) {
                        val name = cr.queryFileName(uri) ?: return@withContext
                        if (!name.endsWith(".zip")) {
                            importErrorDialog(getString(R.string.exception_theme_filename))
                            return@withContext
                        }
                        try {
                            val inputStream = cr.openInputStream(uri)!!
                            val (newCreated, theme, migrated) = ThemeManager.importTheme(inputStream)
                                .getOrThrow()
                            withContext(Dispatchers.Main) {
                                if (newCreated) {
                                    themeListAdapter.prependTheme(theme)
                                } else {
                                    themeListAdapter.replaceTheme(theme)
                                }
                                if (migrated) {
                                    ctx.toast(R.string.theme_migrated)
                                }
                            }
                        } catch (e: Exception) {
                            importErrorDialog(e.localizedMessage ?: e.stackTraceToString())
                        }
                    }
                }
            }
        exportLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                if (uri == null) return@registerForActivityResult
                val ctx = requireContext()
                val exported = beingExported ?: return@registerForActivityResult
                beingExported = null
                lifecycleScope.withLoadingDialog(requireContext()) {
                    withContext(NonCancellable + Dispatchers.IO) {
                        try {
                            val outputStream = ctx.contentResolver.openOutputStream(uri)!!
                            ThemeManager.exportTheme(exported, outputStream).getOrThrow()
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                ctx.toast(e.localizedMessage ?: e.stackTraceToString())
                            }
                        }
                    }
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        themeListAdapter = object : ThemeListAdapter() {
            override fun onAddNewTheme() = addTheme()
            override fun onSelectTheme(theme: Theme) = selectTheme(theme)
            override fun onEditTheme(theme: Theme.Custom) = editTheme(theme)
            override fun onExportTheme(theme: Theme.Custom) = exportTheme(theme)
        }
        themeListAdapter.setThemes(ThemeManager.getAllThemes())
        updateSelectedThemes()
        ThemeManager.addOnChangedListener(onThemeChangeListener)
        return ResponsiveThemeListView(requireContext()).apply {
            adapter = themeListAdapter
            applyNavBarInsetsBottomPadding()
        }
    }

    private fun updateSelectedThemes(activeTheme: Theme? = null) {
        val active = activeTheme ?: ThemeManager.getActiveTheme()
        var light: Theme? = null
        var dark: Theme? = null
        if (followSystemDayNightTheme) {
            light = ThemeManager.prefs.lightModeTheme.getValue()
            dark = ThemeManager.prefs.darkModeTheme.getValue()
        }
        themeListAdapter.setSelectedThemes(active, light, dark)
    }

    private fun addTheme() {
        val ctx = requireContext()
        val actions = arrayOf(
            getString(R.string.choose_image),
            getString(R.string.import_from_file),
            getString(R.string.duplicate_builtin_theme)
        )
        AlertDialog.Builder(ctx)
            .setTitle(R.string.new_theme)
            .setNegativeButton(android.R.string.cancel, null)
            .setItems(actions) { _, i ->
                when (i) {
                    0 -> imageLauncher.launch(null)
                    1 -> importLauncher.launch("application/zip")
                    2 -> {
                        val view = ResponsiveThemeListView(ctx).apply {
                            // force AlertDialog's customPanel to grow
                            minimumHeight = Int.MAX_VALUE
                        }
                        val dialog = AlertDialog.Builder(ctx)
                            .setTitle(getString(R.string.duplicate_builtin_theme).removeSuffix("…"))
                            .setNegativeButton(android.R.string.cancel, null)
                            .setView(view)
                            .create()
                        view.adapter = object :
                            SimpleThemeListAdapter<Theme.Builtin>(ThemeManager.builtinThemes) {
                            override fun onClick(theme: Theme.Builtin) {
                                val newTheme =
                                    theme.deriveCustomNoBackground(UUID.randomUUID().toString())
                                themeListAdapter.prependTheme(newTheme)
                                ThemeManager.saveTheme(newTheme)
                                dialog.dismiss()
                            }
                        }
                        dialog.show()
                    }
                }
            }
            .show()
    }

    private fun selectTheme(theme: Theme) {
        if (followSystemDayNightTheme) {
            val ctx = requireContext()
            AlertDialog.Builder(ctx)
                .setIcon(ctx.styledDrawable(android.R.attr.alertDialogIcon))
                .setTitle(R.string.configure)
                .setMessage(R.string.theme_message_follow_system_day_night_mode_enabled)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.disable_it) { _, _ ->
                    followSystemDayNightTheme = false
                    lifecycleScope.launch {
                        updateSelectedThemes()
                    }
                }
                .show()
            return
        }
        ThemeManager.switchTheme(theme)
    }

    private fun editTheme(theme: Theme.Custom) {
        imageLauncher.launch(theme)
    }

    private fun exportTheme(theme: Theme.Custom) {
        beingExported = theme
        exportLauncher.launch(theme.name + ".zip")
    }

    override fun onDestroy() {
        ThemeManager.removeOnChangedListener(onThemeChangeListener)
        super.onDestroy()
    }
}
