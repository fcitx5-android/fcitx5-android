package org.fcitx.fcitx5.android

import android.content.ContextWrapper
import android.content.res.Resources
import android.os.Bundle
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import org.fcitx.fcitx5.android.ui.main.theme.settingsColorScheme
import org.fcitx.fcitx5.android.ui.main.theme.defaultSettingsColorScheme
import android.widget.FrameLayout
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.material3.MaterialTheme
import org.fcitx.fcitx5.android.ui.main.theme.SettingsThemeContent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import org.fcitx.fcitx5.android.utils.resolveThemeColor
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.ui.main.modified.MyPreferenceFragment
import org.fcitx.fcitx5.android.ui.main.theme.SettingsActivity
import org.fcitx.fcitx5.android.ui.main.theme.SettingsTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 28, 36], application = FcitxApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsThemeTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val pref get() = AppPrefs.getInstance().advanced.settingsTheme

    @Before fun reset() {
        // Each Robolectric case receives a new application storage directory.
        AppPrefs::class.java.getDeclaredField("instance").apply {
            isAccessible = true
            set(null, null)
        }
        AppPrefs.init(androidx.preference.PreferenceManager.getDefaultSharedPreferences(
            RuntimeEnvironment.getApplication()))
        pref.sharedPreferences.edit().clear().commit()
    }

    @Test fun defaultAndInvalidSettingRestoreMaterialDesign() {
        assertEquals(SettingsTheme.MaterialDesign, SettingsTheme.current())
        pref.sharedPreferences.edit().putString(pref.key, "unknown").commit()
        assertEquals(SettingsTheme.MaterialDesign, SettingsTheme.current())
        assertEquals("MaterialDesign", pref.sharedPreferences.getString(pref.key, null))
    }

    @Test fun themeSelectorRecreatesSettingsAndKeepsSelection() {
        val controller = Robolectric.buildActivity(HostActivity::class.java).setup().visible()
        try {
            val original = controller.get()
            val home = org.fcitx.fcitx5.android.ui.main.MainFragment()
            original.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, home).commitNow()
            assertNull(home.findPreference<ListPreference>(pref.key))
            val fragment = org.fcitx.fcitx5.android.ui.main.settings.behavior.AdvancedSettingsFragment()
            original.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment).commitNow()
            val selector = fragment.findPreference<ListPreference>(pref.key)!!
            assertArrayEquals(arrayOf("MaterialDesign", "Material3"), selector.entryValues)
            assertEquals("MaterialDesign", selector.value)
            selector.value = "Material3"
            shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(100))
            assertEquals(SettingsTheme.Material3, SettingsTheme.current())
            val recreated = controller.get()
            assertNotSame(original, recreated)
            assertEquals(SettingsTheme.Material3, SettingsTheme.current())
            val restored = recreated.supportFragmentManager.findFragmentById(android.R.id.content)
                as org.fcitx.fcitx5.android.ui.main.settings.behavior.AdvancedSettingsFragment
            assertEquals("Material3", restored.findPreference<ListPreference>(pref.key)!!.value)
            compose.onNodeWithText("Settings interface theme").assertExists()
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test fun material3ControlsPersistAndRetainPreferenceDependencies() {
        exerciseControls(SettingsTheme.Material3)
    }

    private fun exerciseControls(theme: SettingsTheme) {
        pref.setValue(theme)
        val controller = Robolectric.buildActivity(HostActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            val fragment = TestPreferences()
            activity.supportFragmentManager.beginTransaction().replace(android.R.id.content, fragment).commitNow()
            shadowOf(android.os.Looper.getMainLooper()).idle()
            compose.onNodeWithText("Test switch").performClick()
            compose.runOnIdle {
                assertTrue(fragment.toggle.isChecked)
                assertTrue(fragment.choice.isEnabled)
            }
            compose.onNodeWithText("Choice").performClick()
            compose.runOnIdle {
                val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
                    as androidx.appcompat.app.AlertDialog
                dialog.listView.performItemClick(dialog.listView.getChildAt(1), 1, 1)
            }
            compose.runOnIdle { assertEquals("second", fragment.choice.value) }
            compose.onNodeWithText("Second").assertExists()
        } finally {
            controller.pause().stop().destroy()
        }
        val sp = androidx.preference.PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication())
        assertTrue(sp.getBoolean("theme_test_switch", false))
        assertEquals("second", sp.getString("theme_test_choice", null))
        assertEquals(theme, SettingsTheme.current())
    }

    @Test fun launcherIconSwitchOnlyDisablesLauncherAlias() {
        val context = RuntimeEnvironment.getApplication()
        val alias = org.fcitx.fcitx5.android.utils.LauncherIcon.component(context)
        val main = android.content.ComponentName(context,
            org.fcitx.fcitx5.android.ui.main.MainActivity::class.java)
        val ime = android.content.ComponentName(context,
            org.fcitx.fcitx5.android.input.FcitxInputMethodService::class.java)
        val pm = context.packageManager
        val mainState = pm.getComponentEnabledSetting(main)
        val imeState = pm.getComponentEnabledSetting(ime)
        val controller = Robolectric.buildActivity(HostActivity::class.java).setup().visible()
        try {
            val fragment = org.fcitx.fcitx5.android.ui.main.settings.behavior.AdvancedSettingsFragment()
            controller.get().supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment).commitNow()
            val provider = AppPrefs.getInstance().advanced
            val switch = fragment.findPreference<SwitchPreference>(provider.hideLauncherIcon.key)!!
            assertEquals(context.getString(R.string.hide_launcher_icon_summary), switch.summary)
            assertEquals(provider.settingsTheme.key, fragment.preferenceScreen.getPreference(0).key)
            assertEquals(switch.key, fragment.preferenceScreen.getPreference(1).key)
            assertFalse(switch.isChecked)
            switch.performClick()
            assertTrue(provider.hideLauncherIcon.getValue())
            assertEquals(android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                pm.getComponentEnabledSetting(alias))
            assertEquals(mainState, pm.getComponentEnabledSetting(main))
            assertEquals(imeState, pm.getComponentEnabledSetting(ime))
            switch.performClick()
            assertFalse(provider.hideLauncherIcon.getValue())
            assertEquals(android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                pm.getComponentEnabledSetting(alias))
            assertFalse(controller.get().isFinishing)
        } finally {
            org.fcitx.fcitx5.android.utils.LauncherIcon.setHidden(context, false)
            controller.pause().stop().destroy()
        }
    }

    @Test fun material3HomeResolvesIconAndTextColorStateLists() {
        pref.setValue(SettingsTheme.Material3)
        val controller = Robolectric.buildActivity(HostActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            val fragment = org.fcitx.fcitx5.android.ui.main.MainFragment()
            activity.supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment).commitNow()
            compose.onNodeWithText(activity.getString(R.string.global_options)).assertExists()
            assertNotNull((fragment.preferenceScreen.getPreference(0) as PreferenceCategory)
                .getPreference(0).icon)
            val attrs = intArrayOf(android.R.attr.colorControlNormal,
                android.R.attr.textColorPrimary, android.R.attr.textColorSecondary)
            for (attr in attrs) {
                val colors = activity.obtainStyledAttributes(intArrayOf(attr))
                try {
                    assertEquals(colors.getColorStateList(0)!!.defaultColor,
                        activity.resolveThemeColor(attr))
                } finally {
                    colors.recycle()
                }
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test @Config(sdk = [36]) fun monetColorsArePreferredForBothModes() {
        val context = RuntimeEnvironment.getApplication()
        for (dark in listOf(false, true)) {
            val expected = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            val actual = settingsColorScheme(context, dark)
            assertEquals(expected.primary, actual.primary)
            assertEquals(expected.secondary, actual.secondary)
            assertEquals(expected.surfaceContainerLow, actual.surfaceContainerLow)
            assertNotEquals(defaultSettingsColorScheme(dark).primary, actual.primary)
        }
    }

    @Test @Config(sdk = [36]) fun missingMonetResourcesUseDefaultColors() {
        val context = RuntimeEnvironment.getApplication()
        val original = context.resources
        val unavailable = object : ContextWrapper(context) {
            private val missingColors = object : Resources(original.assets, original.displayMetrics, original.configuration) {
                override fun getColor(id: Int, theme: Theme?): Int {
                    if (getResourceEntryName(id).startsWith("system_")) {
                        throw NotFoundException("Monet color unavailable")
                    }
                    return original.getColor(id, theme)
                }
            }
            override fun getResources() = missingColors
        }
        for (dark in listOf(false, true)) {
            val expected = defaultSettingsColorScheme(dark)
            val actual = settingsColorScheme(unavailable, dark)
            assertEquals(expected.primary, actual.primary)
            assertEquals(expected.background, actual.background)
            assertEquals(expected.surfaceContainerLow, actual.surfaceContainerLow)
        }
    }

    @Test @Config(qualifiers = "night") fun everyThemeUsesNightResources() {
        assertSystemPalette(dark = true)
    }

    @Test @Config(qualifiers = "notnight") fun everyThemeUsesDayResources() {
        assertSystemPalette(dark = false)
    }

    private fun assertSystemPalette(dark: Boolean) {
        for (theme in SettingsTheme.entries) {
            pref.setValue(theme)
            val controller = Robolectric.buildActivity(HostActivity::class.java).setup().visible()
            val activity = controller.get()
            val value = android.util.TypedValue()
            activity.theme.resolveAttribute(android.R.attr.colorBackground, value, true)
            if (theme == SettingsTheme.Material3) {
                val colors = settingsColorScheme(activity, dark)
                val primary = android.util.TypedValue()
                activity.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, primary, true)
                assertEquals("View and Compose primary colors", colors.primary.toArgb(), primary.data)
            }
            val luminance = androidx.core.graphics.ColorUtils.calculateLuminance(value.data)
            assertEquals("$theme", dark, luminance < .5)
            assertEquals(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode())
            if (theme != SettingsTheme.MaterialDesign) {
                var background: Int? = null
                activity.setContentView(ComposeView(activity).apply {
                    setContent {
                        SettingsThemeContent {
                            val color = MaterialTheme.colorScheme.background
                            SideEffect { background = color.toArgb() }
                        }
                    }
                })
                compose.runOnIdle {
                    assertNotNull(background)
                    assertEquals("$theme Compose palette", dark,
                        androidx.core.graphics.ColorUtils.calculateLuminance(background!!) < .5)
                }
            }
            controller.pause().stop().destroy()
        }
    }

    class HostActivity : SettingsActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(FrameLayout(this).apply { id = android.R.id.content })
        }
    }

    class TestPreferences : MyPreferenceFragment() {
        lateinit var toggle: SwitchPreference
        lateinit var choice: ListPreference
        override fun onCreatePreferences(state: Bundle?, key: String?) {
            preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
                addPreference(PreferenceCategory(context).apply { title = "Section" })
                toggle = SwitchPreference(context).apply {
                    this.key = "theme_test_switch"
                    title = "Test switch"
                    setDefaultValue(false)
                }
                choice = ListPreference(context).apply {
                    this.key = "theme_test_choice"
                    title = "Choice"
                    entries = arrayOf("First", "Second")
                    entryValues = arrayOf("first", "second")
                    setDefaultValue("first")
                    summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                }
                addPreference(toggle)
                addPreference(choice)
            }
            choice.dependency = toggle.key
        }
    }
}
