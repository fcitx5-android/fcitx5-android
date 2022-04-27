package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.view.View
import android.view.ViewOutlineProvider
import androidx.activity.result.ActivityResultLauncher
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.ui.main.settings.ProgressFragment
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
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.gravityVerticalCenter
import splitties.views.imageDrawable
import splitties.views.textAppearance
import timber.log.Timber

class ThemeListFragment : ProgressFragment() {

    private lateinit var launcher: ActivityResultLauncher<Theme.Custom?>

    private lateinit var previewUi: KeyboardPreviewUi

    private lateinit var adapter: ThemeListAdapter

    private lateinit var themeList: RecyclerView

    private val onThemeChangedListener = ThemeManager.OnThemeChangedListener {
        Timber.d(it.toString())
        previewUi.setTheme(it)
        adapter.setCheckedTheme(it)
    }

    override fun beforeCreateView() {
        launcher = registerForActivityResult(BackgroundImageActivity.Contract()) { result ->
            if (result != null) {
                when (result) {
                    is BackgroundImageActivity.BackgroundResult.Created -> {
                        val theme = result.theme
                        ThemeManager.saveTheme(theme)
                        adapter.prependTheme(theme)
                        ThemeManager.switchTheme(theme)
                    }
                    is BackgroundImageActivity.BackgroundResult.Deleted -> {
                        val name = result.name
                        // Update the list first, as we rely on theme changed listener
                        // in the case that the deleted theme was active
                        adapter.removeTheme(name)
                        ThemeManager.deleteTheme(name)
                    }
                    is BackgroundImageActivity.BackgroundResult.Updated -> {
                        val theme = result.theme
                        ThemeManager.saveTheme(theme)
                        // An active theme has been updated
                        if (theme.name == ThemeManager.getActiveTheme().name)
                            ThemeManager.fireChange()
                        adapter.replaceTheme(theme)
                    }
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
            textAppearance = resolveThemeAttribute(R.attr.textAppearanceListItem)
            gravity = gravityVerticalCenter
        }
        val settingsButton = imageButton {
            imageDrawable = drawable(R.drawable.ic_baseline_settings_24)
            background = styledDrawable(R.attr.actionBarItemBackground)
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
            backgroundColor = styledColor(R.attr.colorPrimary)
            elevation = dp(4f)
        }

        themeList = recyclerView {
            val spanCount = 2
            val itemWidth = dp(128)
            val itemHeight = dp(88)
            layoutManager = object : GridLayoutManager(context, spanCount) {
                override fun generateDefaultLayoutParams() = LayoutParams(itemWidth, itemHeight)
            }
            this@ThemeListFragment.adapter = object : ThemeListAdapter() {
                override fun onChooseImage() = launchImageSelector()
                override fun onSelectTheme(theme: Theme) = selectTheme(theme)
                override fun onEditTheme(theme: Theme.Custom) = editTheme(theme)
            }.apply {
                setThemes(ThemeManager.getAllThemes(), ThemeManager.getActiveTheme())
            }
            adapter = this@ThemeListFragment.adapter
            // evenly spaced items
            addItemDecoration(ThemeListItemDecoration(itemWidth, spanCount))
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
        if (this::previewUi.isInitialized) {
            previewUi.setTheme(ThemeManager.getActiveTheme())
        }
    }

    private fun launchImageSelector() {
        launcher.launch(null)
    }

    private fun selectTheme(theme: Theme) {
        ThemeManager.switchTheme(theme)
    }

    private fun editTheme(theme: Theme.Custom) {
        launcher.launch(theme)
    }

    override fun onDestroy() {
        ThemeManager.removeOnChangedListener(onThemeChangedListener)
        super.onDestroy()
    }
}
