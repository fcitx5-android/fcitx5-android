package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.ui.common.withLoadingDialog
import org.fcitx.fcitx5.android.utils.applyNavBarInsetsBottomPadding
import org.fcitx.fcitx5.android.utils.bindOnNotNull
import org.fcitx.fcitx5.android.utils.errorDialog
import org.fcitx.fcitx5.android.utils.queryFileName
import org.fcitx.fcitx5.android.utils.toast
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.resolveThemeAttribute
import splitties.resources.styledColor
import splitties.resources.styledDrawable
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageButton
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityVerticalCenter
import splitties.views.imageDrawable
import splitties.views.textAppearance
import java.util.UUID

class ThemeListFragment : Fragment() {

    private lateinit var imageLauncher: ActivityResultLauncher<Theme.Custom?>

    private lateinit var importLauncher: ActivityResultLauncher<String>

    private lateinit var exportLauncher: ActivityResultLauncher<String>

    private lateinit var previewUi: KeyboardPreviewUi

    private lateinit var adapter: ThemeListAdapter

    private lateinit var themeList: RecyclerView

    private var beingExported: Theme.Custom? = null

    @Keep
    private val onThemeChangeListener = ThemeManager.OnThemeChangeListener {
        lifecycleScope.launch {
            previewUi.setTheme(it)
            adapter.setCheckedTheme(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageLauncher = registerForActivityResult(CustomThemeActivity.Contract()) { result ->
            if (result != null) {
                when (result) {
                    is CustomThemeActivity.BackgroundResult.Created -> {
                        val theme = result.theme
                        adapter.prependTheme(theme)
                        ThemeManager.saveTheme(theme)
                        ThemeManager.switchTheme(theme)
                    }
                    is CustomThemeActivity.BackgroundResult.Deleted -> {
                        val name = result.name
                        // Update the list first, as we rely on theme changed listener
                        // in the case that the deleted theme was active
                        adapter.removeTheme(name)
                        ThemeManager.deleteTheme(name)
                    }
                    is CustomThemeActivity.BackgroundResult.Updated -> {
                        val theme = result.theme
                        adapter.replaceTheme(theme)
                        ThemeManager.saveTheme(theme)
                    }
                }
            }
        }
        importLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri == null) return@registerForActivityResult
                val ctx = requireContext()
                lifecycleScope.withLoadingDialog(ctx) {
                    withContext(NonCancellable + Dispatchers.IO) {
                        val name = uri.queryFileName(ctx.contentResolver) ?: return@withContext
                        if (!name.endsWith(".zip")) {
                            importErrorDialog(getString(R.string.exception_theme_filename))
                        }
                        ctx.contentResolver.openInputStream(uri)?.use {
                            ThemeManager.importTheme(it)
                                .onSuccess { (newCreated, theme, migrated) ->
                                    withContext(Dispatchers.Main) {
                                        if (newCreated) {
                                            adapter.prependTheme(theme)
                                        } else {
                                            adapter.replaceTheme(theme)
                                        }
                                        if (migrated) {
                                            ctx.toast(R.string.theme_migrated)
                                        }
                                    }
                                }
                                .onFailure { e ->
                                    importErrorDialog(e.localizedMessage ?: e.stackTraceToString())
                                }
                        }
                    }
                }
            }
        exportLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                lifecycleScope.withLoadingDialog(requireContext()) {
                    withContext(NonCancellable + Dispatchers.IO) {
                        runCatching {
                            uri?.let { requireContext().contentResolver.openOutputStream(it) }
                        }.bindOnNotNull {
                            requireNotNull(beingExported)
                            ThemeManager.exportTheme(beingExported!!, it).also {
                                beingExported = null
                            }
                        }?.toast(requireContext())
                    }
                }
            }
    }

    private suspend fun importErrorDialog(message: String) {
        errorDialog(requireContext(), getString(R.string.import_error), message)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = with(requireContext()) {
        val activeTheme = ThemeManager.getActiveTheme()

        previewUi = KeyboardPreviewUi(this, activeTheme)
        val preview = previewUi.root.apply {
            scaleX = 0.5f
            scaleY = 0.5f
            outlineProvider = ViewOutlineProvider.BOUNDS
            elevation = dp(4f)
        }

        val settingsText = textView {
            setText(R.string.configure_theme)
            textAppearance = resolveThemeAttribute(android.R.attr.textAppearanceListItem)
            gravity = gravityVerticalCenter
        }
        val settingsButton = imageButton {
            imageDrawable = drawable(R.drawable.ic_baseline_settings_24)
            background = styledDrawable(android.R.attr.actionBarItemBackground)
            setOnClickListener {
                findNavController().navigate(R.id.action_themeListFragment_to_themeSettingsFragment)
            }
        }

        val previewWrapper = constraintLayout {
            add(preview, lParams(wrapContent, wrapContent) {
                topOfParent(dp(-52))
                startOfParent()
                endOfParent()
            })
            add(settingsText, lParams(wrapContent, dp(48)) {
                startOfParent(dp(64))
                bottomOfParent(dp(4))
            })
            add(settingsButton, lParams(dp(48), dp(48)) {
                endOfParent(dp(64))
                bottomOfParent(dp(4))
            })
            backgroundColor = styledColor(android.R.attr.colorPrimary)
            elevation = dp(4f)
        }

        themeList = ResponsiveThemeListView(this).apply {
            this@ThemeListFragment.adapter = object : ThemeListAdapter() {
                override fun onAddNewTheme() = addTheme()
                override fun onSelectTheme(theme: Theme) = selectTheme(theme)
                override fun onEditTheme(theme: Theme.Custom) = editTheme(theme)
                override fun onExportTheme(theme: Theme.Custom) = exportTheme(theme)
            }.apply {
                setThemes(ThemeManager.getAllThemes(), activeTheme)
            }
            adapter = this@ThemeListFragment.adapter
            applyNavBarInsetsBottomPadding()
        }

        ThemeManager.addOnChangedListener(onThemeChangeListener)

        constraintLayout {
            add(previewWrapper, lParams(height = wrapContent) {
                topOfParent()
                startOfParent()
                endOfParent()
            })
            add(themeList, lParams {
                below(previewWrapper)
                startOfParent()
                endOfParent()
                bottomOfParent()
            })
        }
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
                                adapter.prependTheme(newTheme)
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
