package org.fcitx.fcitx5.android.ui.main.settings.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.clipboard.SpacesItemDecoration
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.resolveThemeAttribute
import splitties.resources.styledColor
import splitties.resources.styledDrawable
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.gravityVerticalCenter
import splitties.views.imageDrawable
import splitties.views.textAppearance
import timber.log.Timber

class ThemeListFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var launcher: ActivityResultLauncher<Unit>

    private lateinit var previewUi: KeyboardPreviewUi

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = with(requireContext()) ctx@{
        launcher = registerForActivityResult(BackgroundImageActivity.Contract()) {
            Timber.d("Background: $it")
        }

        previewUi = KeyboardPreviewUi(this, ThemeManager.currentTheme)
        val preview = frameLayout {
            add(previewUi.root, lParams())
            scaleX = 0.5f
            scaleY = 0.5f
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
                topOfParent(dp(-40))
                startOfParent()
                endOfParent()
            })
            add(settingsText, lParams(matchConstraints, dp(48)) {
                below(preview, dp(-48))
                startOfParent(dp(64))
                before(settingsButton)
                bottomOfParent(dp(8))
            })
            add(settingsButton, lParams(dp(48), dp(48)) {
                below(preview, dp(-48))
                endOfParent(dp(64))
                bottomOfParent(dp(8))
            })
            backgroundColor = styledColor(R.attr.colorPrimary)
            elevation = dp(4f)
        }

        val themeList = recyclerView {
            layoutManager = GridLayoutManager(this@ctx, 2)
            adapter = object : ThemeListAdapter() {
                override fun onChooseImage() = launchImageSelector()
                override fun onSelectTheme(theme: Theme) = updatePreviewTheme(theme)
            }.apply {
                // TODO space items evenly
                addItemDecoration(SpacesItemDecoration(dp(24)))
                entries = ThemeManager.getAllThemes().toTypedArray()
            }
        }

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
            previewUi.setTheme(ThemeManager.currentTheme)
        }
    }

    private fun launchImageSelector() {
        launcher.launch(Unit)
    }

    private fun updatePreviewTheme(theme: Theme) {
        previewUi.setTheme(theme)
    }
}
