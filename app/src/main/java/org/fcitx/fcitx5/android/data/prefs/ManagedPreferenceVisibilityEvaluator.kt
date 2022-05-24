package org.fcitx.fcitx5.android.data.prefs

class ManagedPreferenceVisibilityEvaluator(
    private val managedPreferences: Map<String, ManagedPreference<*, *>>,
    private val onVisibilityChanged: (Map<String,Boolean>) -> Unit
) {

    private val visibility = mutableMapOf<String, Boolean>()

    // it would be better to declare the dependency relationship, rather than reevaluating on each value changed
    private val onValueChangeListener = ManagedPreference.OnChangeListener<Any> {
        evaluateVisibility()
    }

    init {
        managedPreferences.forEach { (_, pref) ->
            pref.registerOnChangeListener(
                onValueChangeListener
            )
        }
    }

    fun evaluateVisibility() {
        val changed = mutableMapOf<String,Boolean>()
        managedPreferences.forEach { (key, pref) ->
            val old = visibility[key]
            val new = pref.enableUiOn()
            if (old != null && old != new) {
                changed[key] = new
            }
            visibility[key] = new
        }
        if (changed.isNotEmpty())
            onVisibilityChanged(changed)
    }

}