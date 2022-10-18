package org.fcitx.fcitx5.android.data.prefs

class ManagedPreferenceVisibilityEvaluator(
    private val provider: ManagedPreferenceProvider,
    private val onVisibilityChanged: (Map<String, Boolean>) -> Unit
) {

    private val visibility = mutableMapOf<String, Boolean>()

    // it would be better to declare the dependency relationship, rather than reevaluating on each value changed
    private val onValueChangeListener = ManagedPreference.OnChangeListener<Any> { _, _ ->
        evaluateVisibility()
    }

    init {
        provider.managedPreferences.forEach { (_, pref) ->
            pref.registerOnChangeListener(
                onValueChangeListener
            )
        }
    }

    fun evaluateVisibility() {
        val changed = mutableMapOf<String, Boolean>()
        provider.managedPreferencesUi.forEach { ui ->
            val old = visibility[ui.key]
            val new = ui.enableUiOn()
            if (old != null && old != new) {
                changed[ui.key] = new
            }
            visibility[ui.key] = new
        }
        if (changed.isNotEmpty())
            onVisibilityChanged(changed)
    }

}