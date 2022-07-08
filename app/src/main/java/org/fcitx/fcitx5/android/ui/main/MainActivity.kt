package org.fcitx.fcitx5.android.ui.main

import android.animation.ObjectAnimator
import android.animation.StateListAnimator
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.databinding.ActivityMainBinding
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.ui.main.settings.PinyinDictionaryFragment
import org.fcitx.fcitx5.android.ui.setup.SetupActivity
import org.fcitx.fcitx5.android.utils.applyTranslucentSystemBars
import org.fcitx.fcitx5.android.utils.navigateFromMain
import splitties.dimensions.dp

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var navHostFragment: NavHostFragment

    private fun onNavigateUpListener(): Boolean {
        val navController = navHostFragment.navController
        return when (navController.currentDestination?.id) {
            R.id.mainFragment -> {
                // "minimize" the app, don't exit activity
                moveTaskToBack(false)
                true
            }
            else -> onSupportNavigateUp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTranslucentSystemBars()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.appBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = navBars.left
                rightMargin = navBars.right
            }
            binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBars.top
            }
            binding.navHostFragment.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = navBars.left
                rightMargin = navBars.right
            }
            windowInsets
        }
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        val appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf(),
            fallbackOnNavigateUpListener = ::onNavigateUpListener
        )
        navHostFragment = binding.navHostFragment.getFragment()
        binding.toolbar.setupWithNavController(navHostFragment.navController, appBarConfiguration)
        viewModel.toolbarTitle.observe(this) {
            binding.toolbar.title = it
        }
        viewModel.appbarShadow.observe(this) {
            binding.appBar.stateListAnimator = StateListAnimator().apply {
                addState(
                    intArrayOf(android.R.attr.state_enabled),
                    ObjectAnimator.ofFloat(binding.appBar, "elevation", dp(if (it) 4f else 0f))
                )
            }
        }
        navHostFragment.navController.addOnDestinationChangedListener { _, dest, _ ->
            when (dest.id) {
                R.id.themeListFragment -> viewModel.disableAppbarShadow()
                else -> viewModel.enableAppbarShadow()
            }
        }
        viewModel.toolbarSaveButtonOnClickListener.observe(this) {
            binding.toolbar.menu.findItem(R.id.activity_main_menu_save).isVisible = it != null
        }
        viewModel.aboutButton.observe(this) {
            binding.toolbar.menu.findItem(R.id.activity_main_menu_about).isVisible = it
        }
        if (SetupActivity.shouldShowUp() && intent.action == Intent.ACTION_MAIN)
            startActivity(Intent(this, SetupActivity::class.java))
        processIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        processIntent(intent)
        super.onNewIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        listOf(
            ::processAddDictIntent,
            ::processConfigIntent,
            ::processAddInputMethodIntent
        ).firstOrNull { it(intent) }
    }

    private fun processAddDictIntent(intent: Intent?): Boolean {
        if (intent != null && intent.action == Intent.ACTION_VIEW) {
            intent.data?.let {
                AlertDialog.Builder(this)
                    .setTitle(R.string.pinyin_dict)
                    .setMessage(R.string.whether_import_dict)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .setPositiveButton(R.string.import_) { _, _ ->
                        navHostFragment.navController.navigateFromMain(
                            R.id.action_mainFragment_to_pinyinDictionaryFragment,
                            bundleOf(PinyinDictionaryFragment.INTENT_DATA_URI to it)
                        )
                    }
                    .show()
            }
            return true
        }
        return false
    }

    private fun processConfigIntent(intent: Intent?): Boolean {
        intent?.getStringExtra(INTENT_DATA_CONFIG)?.let {
            val target = when (it) {
                INTENT_DATA_CONFIG_GLOBAL ->
                    R.id.action_mainFragment_to_globalConfigFragment to null
                INTENT_DATA_CONFIG_IM ->
                    R.id.action_mainFragment_to_imConfigFragment to
                            (intent.getBundleExtra(INTENT_DATA_CONFIG_IM) ?: return false)
                INTENT_DATA_CONFIG_BEHAVIOR ->
                    R.id.action_mainFragment_to_behaviorSettingsFragment to null
                INTENT_DATA_CONFIG_THEME ->
                    R.id.action_mainFragment_to_themeListFragment to null
                else -> return false
            }
            // we need fcitx instance
            viewModel.onBindFcitxInstance {
                navHostFragment.navController.navigateFromMain(target.first, target.second)
            }
        }
        return false
    }

    private fun processAddInputMethodIntent(intent: Intent?): Boolean {
        if (intent != null && intent.hasExtra(INTENT_DATA_ADD_IM)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.no_more_input_methods)
                .setMessage(R.string.add_more_input_methods)
                .setPositiveButton(R.string.add) { _, _ ->
                    navHostFragment.navController.navigateFromMain(R.id.action_mainFragment_to_imListFragment)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
            return true
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main_menu, menu)
        menu.apply {
            findItem(R.id.activity_main_menu_about).isVisible = viewModel.aboutButton.value ?: true
            findItem(R.id.activity_main_menu_save).isVisible =
                viewModel.toolbarSaveButtonOnClickListener.value != null
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.activity_main_menu_about -> {
            navHostFragment.navController.navigate(R.id.action_mainFragment_to_aboutFragment)
            true
        }
        R.id.activity_main_menu_save -> {
            viewModel.toolbarSaveButtonOnClickListener.value?.invoke()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        // nothing needs to be saved if keyboard is not running
        if (FcitxInputMethodService.isBoundToFcitxDaemon && viewModel.isFcitxReady)
            runBlocking {
                viewModel.fcitx.save()
            }
        super.onStop()
    }

    companion object {
        const val INTENT_DATA_ADD_IM = "add_im"
        const val INTENT_DATA_CONFIG = "config"
        const val INTENT_DATA_CONFIG_GLOBAL = "global"
        const val INTENT_DATA_CONFIG_IM = "im"
        const val INTENT_DATA_CONFIG_BEHAVIOR = "behavior"
        const val INTENT_DATA_CONFIG_THEME = "theme"
    }
}