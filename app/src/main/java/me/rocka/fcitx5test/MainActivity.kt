package me.rocka.fcitx5test

import android.content.ServiceConnection
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import me.rocka.fcitx5test.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var connection: ServiceConnection? = null

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // keep connection with daemon, so that it won't exit when fragment switches
        connection = bindFcitxDaemon { }
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        val appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf(R.id.mainFragment),
            fallbackOnNavigateUpListener = ::onSupportNavigateUp
        )
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.toolbar.setupWithNavController(
            navHostFragment.navController,
            appBarConfiguration
        )
        viewModel.toolbarTitle.observe(this) {
            binding.toolbar.title = it
        }
        viewModel.toolbarSaveButtonOnClickListener.observe(this) {
            val saveButton = binding.toolbar[0]
            if (it != null) {
                saveButton.visibility = View.VISIBLE
                saveButton.setOnClickListener { _ -> it() }
            } else
                saveButton.visibility = View.INVISIBLE
        }

    }

    override fun onDestroy() {
        connection?.let { unbindService(it) }
        super.onDestroy()
    }
}
