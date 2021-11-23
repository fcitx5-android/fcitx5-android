package me.rocka.fcitx5test

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import me.rocka.fcitx5test.databinding.ActivityMainBinding
import me.rocka.fcitx5test.native.Fcitx
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fcitx: Fcitx

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindFcitxDaemon {
            fcitx = it.getFcitxInstance()
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.openImeSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        val buildTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ROOT)
            .format(Date(BuildConfig.BUILD_TIME))
        binding.versionInfo.text =
            "Build Git Hash: ${BuildConfig.BUILD_GIT_HASH}\nBuild Date: $buildTime"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_main_actionbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.activity_main_set_enabled -> {
                val enabled = fcitx.enabledIme()
                val available = fcitx.availableIme()
                val ids = available.map { it.uniqueName }.toTypedArray()
                val names = available.map { it.name }.toTypedArray()
                val state = available.map { enabled.contains(it) }.toBooleanArray()
                AlertDialog.Builder(this)
                    .setTitle(R.string.input_methods)
                    .setMultiChoiceItems(names, state) { _, idx, checked -> state[idx] = checked }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .setPositiveButton(R.string.save) { _, _ ->
                        fcitx.setEnabledIme(ids.filterIndexed { i, _ -> state[i] }.toTypedArray())
                    }.show()
                true
            }
            R.id.activity_main_addons -> fcitx.addons().run {
                val ids = map { it.uniqueName }.toTypedArray()
                val names = map { it.name }.toTypedArray()
                val state = map { it.enabled }.toBooleanArray()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.addons)
                    .setMultiChoiceItems(names, state) { _, idx, checked -> state[idx] = checked }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .setPositiveButton(R.string.save) { _, _ -> fcitx.setAddonState(ids, state) }
                    .show()
                true
            }
            R.id.activity_main_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                AlertDialog.Builder(this)
                    .setTitle(R.string.conf)
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .setItems(R.array.conf_types) { _, type ->
                        when (type) {
                            0 -> intent.run {
                                putExtra("type", "global")
                                startActivity(this)
                            }
                            1 -> fcitx.enabledIme().run {
                                val ids = map { it.uniqueName }.toTypedArray()
                                val names = map { it.name }.toTypedArray()
                                AlertDialog.Builder(this@MainActivity)
                                    .setTitle(R.string.input_methods_conf)
                                    .setNegativeButton(R.string.cancel) { _, _ -> }
                                    .setItems(names) { _, idx ->
                                        intent.run {
                                            putExtra("type", "im")
                                            putExtra("im", ids[idx])
                                            startActivity(this)
                                        }
                                    }.show()
                            }
                            2 -> fcitx.addons().filter { it.isConfigurable }.run {
                                val ids = map { it.uniqueName }.toTypedArray()
                                val names = map { it.name }.toTypedArray()
                                AlertDialog.Builder(this@MainActivity)
                                    .setTitle(R.string.addons_conf)
                                    .setNegativeButton(R.string.cancel) { _, _ -> }
                                    .setItems(names) { _, idx ->
                                        intent.run {
                                            putExtra("type", "addon")
                                            putExtra("addon", ids[idx])
                                            startActivity(this)
                                        }
                                    }.show()
                            }

                        }
                    }.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
