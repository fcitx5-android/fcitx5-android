package me.rocka.fcitx5test

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import me.rocka.fcitx5test.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {


    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: debug
        startActivity(Intent(this, TestActivity::class.java))
        // now fragments use fcitx instance from activity
        FcitxDaemonManager.bindFcitxDaemonAsync(this, javaClass.name) {
            viewModel.fcitx = getFcitxInstance()
        }
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        val appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf(R.id.mainFragment),
            fallbackOnNavigateUpListener = ::onSupportNavigateUp
        )
        val navHostFragment =
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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main_menu, menu)
        return true
    }

    override fun onDestroy() {
        FcitxDaemonManager.unbind(this, javaClass.name)
        super.onDestroy()
    }
}
