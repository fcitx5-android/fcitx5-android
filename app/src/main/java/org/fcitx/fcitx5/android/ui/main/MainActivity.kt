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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.databinding.ActivityMainBinding
import org.fcitx.fcitx5.android.ui.main.settings.PinyinDictionaryFragment
import org.fcitx.fcitx5.android.ui.setup.SetupActivity
import org.fcitx.fcitx5.android.utils.applyTranslucentSystemBars
import org.fcitx.fcitx5.android.utils.navigateFromMain
import splitties.dimensions.dp

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTranslucentSystemBars()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.appbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
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
            // always show back icon regardless of `navController.currentDestination`
            topLevelDestinationIds = setOf()
        )
        navController = binding.navHostFragment.getFragment<NavHostFragment>().navController
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        binding.toolbar.setNavigationOnClickListener {
            // prevent navigate up when child fragment has enabled `OnBackPressedCallback`
            if (onBackPressedDispatcher.hasEnabledCallbacks()) {
                onBackPressedDispatcher.onBackPressed()
                return@setNavigationOnClickListener
            }
            // "minimize" the activity if we can't go back
            navController.navigateUp() || onSupportNavigateUp() || moveTaskToBack(false)
        }
        viewModel.toolbarTitle.observe(this) {
            binding.toolbar.title = it
        }
        viewModel.appbarShadow.observe(this) {
            binding.appbar.stateListAnimator = StateListAnimator().apply {
                addState(
                    intArrayOf(android.R.attr.state_enabled),
                    ObjectAnimator.ofFloat(binding.appbar, "elevation", dp(if (it) 4f else 0f))
                )
            }
        }
        navController.addOnDestinationChangedListener { _, dest, _ ->
            when (dest.id) {
                R.id.themeListFragment -> viewModel.disableAppbarShadow()
                else -> viewModel.enableAppbarShadow()
            }
        }
        if (SetupActivity.shouldShowUp() && intent.action == Intent.ACTION_MAIN)
            startActivity(Intent(this, SetupActivity::class.java))
        processIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        processIntent(intent)
        super.onNewIntent(intent)
        navController.handleDeepLink(intent)
    }

    private fun processIntent(intent: Intent?) {
        listOf(::processAddDictIntent).firstOrNull { it(intent) }
    }

    private fun processAddDictIntent(intent: Intent?): Boolean {
        if (intent != null && intent.action == Intent.ACTION_VIEW) {
            intent.data?.let {
                AlertDialog.Builder(this)
                    .setTitle(R.string.pinyin_dict)
                    .setMessage(R.string.whether_import_dict)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .setPositiveButton(R.string.import_) { _, _ ->
                        navController.navigateFromMain(
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean = menu.run {
        add(R.string.save).apply {
            setIcon(R.drawable.ic_baseline_save_24)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            viewModel.toolbarSaveButtonOnClickListener.apply {
                observe(this@MainActivity) { listener -> isVisible = listener != null }
                setValue(value)
            }
            setOnMenuItemClickListener {
                viewModel.toolbarSaveButtonOnClickListener.value?.invoke()
                true
            }
        }
        val aboutMenus = mutableListOf<MenuItem>()
        add(R.string.developer).apply {
            aboutMenus.add(this)
            setOnMenuItemClickListener {
                navController.navigate(R.id.action_mainFragment_to_developerFragment)
                true
            }
        }
        add(R.string.about).apply {
            aboutMenus.add(this)
            setOnMenuItemClickListener {
                navController.navigate(R.id.action_mainFragment_to_aboutFragment)
                true
            }
        }
        viewModel.aboutButton.apply {
            observe(this@MainActivity) { enabled ->
                aboutMenus.forEach { menu -> menu.isVisible = enabled }
            }
            setValue(value)
        }
        true
    }

}