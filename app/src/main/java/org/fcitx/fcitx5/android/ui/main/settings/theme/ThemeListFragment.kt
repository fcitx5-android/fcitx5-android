package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.view.View
import android.view.ViewOutlineProvider
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import org.fcitx.fcitx5.android.ui.main.settings.ProgressFragment
import org.fcitx.fcitx5.android.utils.*
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.resolveThemeAttribute
import splitties.resources.styledColor
import splitties.resources.styledDrawable
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageButton
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityVerticalCenter
import splitties.views.imageDrawable
import splitties.views.textAppearance
import java.util.*

class ThemeListFragment : ProgressFragment() {

    private lateinit var imageLauncher: ActivityResultLauncher<Theme.Custom?>

    private lateinit var importLauncher: ActivityResultLauncher<String>

    private lateinit var exportLauncher: ActivityResultLauncher<String>

    private lateinit var previewUi: KeyboardPreviewUi

    private lateinit var adapter: ThemeListAdapter

    private lateinit var themeList: RecyclerView

    private var beingExported: Theme.Custom? = null

    private val onThemeChangedListener = ThemeManager.OnThemeChangedListener {
        lifecycleScope.launch {
            previewUi.setTheme(it)
            adapter.setCheckedTheme(it)
        }
    }

    override fun beforeCreateView() {
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
                lifecycleScope.withLoadingDialog(requireContext()) {
                    withContext(NonCancellable + Dispatchers.IO) {
                        runCatching {
                            uri?.let {
                                it.queryFileName(requireContext().contentResolver)
                                    .orNull()
                                    ?.let { name ->
                                        name.endsWith(".zip")
                                            .takeIf(Boolean::identity)
                                            ?: errorArg(R.string.exception_theme_filename, name)
                                    }
                                requireContext().contentResolver.openInputStream(it)
                            }
                        }.bindOnNotNull {
                            ThemeManager.importTheme(it)
                        }?.onSuccess { (newCreated, theme) ->
                            withContext(Dispatchers.Main) {
                                if (newCreated)
                                    adapter.prependTheme(theme)
                                else
                                    adapter.replaceTheme(theme)
                            }
                        }?.onFailure {
                            errorDialog(
                                requireContext(),
                                getString(R.string.import_error),
                                it.localizedMessage ?: it.stackTraceToString()
                            )
                        }
                    }
                }
            }
        exportLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
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

    override suspend fun initialize(): View = with(requireContext()) {
        previewUi = KeyboardPreviewUi(this, ThemeManager.getActiveTheme())
        val preview = previewUi.root.apply {
            scaleX = 0.5f
            scaleY = 0.5f
            outlineProvider = ViewOutlineProvider.BOUNDS
            elevation = dp(4f)
        }

        val settingsText = textView {
            setText(R.string.configure_theme)
            textAppearance = resolveThemeAttribute(androidx.appcompat.R.attr.textAppearanceListItem)
            gravity = gravityVerticalCenter
        }
        val settingsButton = imageButton {
            imageDrawable = drawable(R.drawable.ic_baseline_settings_24)
            background = styledDrawable(androidx.appcompat.R.attr.actionBarItemBackground)
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
            backgroundColor = styledColor(androidx.appcompat.R.attr.colorPrimary)
            elevation = dp(4f)
        }

        themeList = ResponsiveThemeListView(this).apply {
            this@ThemeListFragment.adapter = object : ThemeListAdapter() {
                override fun onAddNewTheme() = addTheme()
                override fun onSelectTheme(theme: Theme) = selectTheme(theme)
                override fun onEditTheme(theme: Theme.Custom) = editTheme(theme)
                override fun onExportTheme(theme: Theme.Custom) = exportTheme(theme)
            }.apply {
                setThemes(ThemeManager.getAllThemes(), ThemeManager.getActiveTheme())
            }
            adapter = this@ThemeListFragment.adapter
            applyNavBarInsetsBottomPadding()
        }

        ThemeManager.addOnChangedListener(onThemeChangedListener)

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

    override fun onResume() {
        super.onResume()
        viewModel.setToolbarTitle(requireContext().getString(R.string.theme))
        viewModel.disableToolbarShadow()
        if (this::previewUi.isInitialized) {
            previewUi.setTheme(ThemeManager.getActiveTheme())
        }
    }

    private fun addTheme() {
        val actions =
            arrayOf(
                getString(R.string.choose_image),
                getString(R.string.import_from_file),
                getString(R.string.duplicate_builtin_theme)
            )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.new_theme)
            .setNegativeButton(android.R.string.cancel, null)
            .setItems(actions) { _, i ->
                when (i) {
                    0 -> imageLauncher.launch(null)
                    1 -> importLauncher.launch("application/zip")
                    2 -> {
                        val view = ResponsiveThemeListView(requireContext()).apply {
                            // force AlertDialog's customPanel to grow
                            minimumHeight = Int.MAX_VALUE
                        }
                        val dialog = AlertDialog.Builder(requireContext())
                            .setTitle(R.string.duplicate_builtin_theme)
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

    override fun onPause() {
        viewModel.enableToolbarShadow()
        super.onPause()
    }

    override fun onDestroy() {
        ThemeManager.removeOnChangedListener(onThemeChangedListener)
        super.onDestroy()
    }
}
