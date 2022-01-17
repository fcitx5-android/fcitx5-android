package me.rocka.fcitx5test.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.databinding.ActivityMainBinding
import me.rocka.fcitx5test.ui.setup.SetupActivity

class MainActivity : AppCompatActivity() {


    private val viewModel: MainViewModel by viewModels()

    private lateinit var navHostFragment: NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        val appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf(R.id.mainFragment),
            fallbackOnNavigateUpListener = ::onSupportNavigateUp
        )
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.toolbar.setupWithNavController(navHostFragment.navController, appBarConfiguration)
        viewModel.toolbarTitle.observe(this) {
            binding.toolbar.title = it
        }
        viewModel.toolbarSaveButtonOnClickListener.observe(this) {
            binding.toolbar.menu.findItem(R.id.activity_main_menu_save).run {
                isVisible = it != null
                setOnMenuItemClickListener { _ -> it?.invoke(); true }
            }
        }
        if (SetupActivity.shouldShowUp() && intent.action == Intent.ACTION_MAIN)
            startActivity(Intent(this, SetupActivity::class.java))
        processAddDictIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        processAddDictIntent(intent)
        super.onNewIntent(intent)
    }

    private fun processAddDictIntent(intent: Intent?) {
        if (intent != null && intent.action == Intent.ACTION_VIEW) {
            intent.data?.let {
                AlertDialog.Builder(this)
                    .setTitle(R.string.pinyin_dict)
                    .setMessage(R.string.whether_import_dict)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .setPositiveButton(R.string.import_) { _, _ ->
                        // ensure we are at top level
                        navHostFragment.navController.popBackStack(R.id.mainFragment, false)
                        // navigate to dictionary manager with uri to import
                        navHostFragment.navController.navigate(
                            R.id.action_mainFragment_to_pinyinDictionaryFragment,
                            bundleOf("uri" to it)
                        )
                    }
                    .show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main_menu, menu)
        return true
    }

    override fun onStop() {
        Log.d(javaClass.name, "onStop")
        if (viewModel.isFcitxInitialized)
            viewModel.fcitx.save()
        super.onStop()
    }
}