package me.rocka.fcitx5test

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import me.rocka.fcitx5test.databinding.ActivityMainBinding
import me.rocka.fcitx5test.native.Fcitx

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fcitx: Fcitx

    private fun alertActivateInputMethod() {
        AlertDialog.Builder(this)
            .setTitle("!")
            .setMessage("Can't find fcitx instance; have you selected 'Fcitx5Test' as default input method?")
            .setNegativeButton("Don't event think about it!") { _, _ -> }
            .setPositiveButton("Take me there!") { _, _ ->
                Handler(Looper.getMainLooper()).postDelayed({
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                }, 200)
            }
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FcitxService.getFcitx()?.let { fcitx = it }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        findViewById<Button>(R.id.open_ime_settings).also {
            it.setOnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!this::fcitx.isInitialized) {
            FcitxService.getFcitx()?.let { fcitx = it }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_main_actionbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!this::fcitx.isInitialized) {
            val f = FcitxService.getFcitx()
            if (f != null) {
                fcitx = f
            } else {
                alertActivateInputMethod()
                return super.onOptionsItemSelected(item)
            }
        }
        return when (item.itemId) {
            R.id.activity_main_get_available -> {
                val list = fcitx.listIme()
                val status = fcitx.imeStatus()
                val current = list.indexOfFirst { status.uniqueName == it.uniqueName }
                AlertDialog.Builder(this)
                    .setTitle("Change IME")
                    .setSingleChoiceItems(
                        list.map { it.uniqueName }.toTypedArray(),
                        current
                    ) { dialog, choice ->
                        val ime = list[choice]
                        fcitx.setIme(ime.uniqueName)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { _, _ -> }
                    .show()
                true
            }
            R.id.activity_main_set_enabled -> {
                val enabled = fcitx.listIme()
                val available = fcitx.availableIme()
                val nameArray = available.map { it.uniqueName }.toTypedArray()
                val stateArray = available.map { avail ->
                    enabled.indexOfFirst { it.uniqueName == avail.uniqueName } >= 0
                }.toBooleanArray()
                AlertDialog.Builder(this)
                    .setTitle("Enabled IME")
                    .setMultiChoiceItems(nameArray, stateArray) { _, which, checked ->
                        stateArray[which] = checked
                    }
                    .setNegativeButton("Cancel") { _, _ -> }
                    .setPositiveButton("OK") { _, _ ->
                        fcitx.setEnabledIme(nameArray.filterIndexed { i, _ -> stateArray[i] }
                            .toTypedArray())
                    }
                    .show()
                true
            }
            R.id.activity_main_addons -> {
                val addons = fcitx.addons()
                val nameArray = addons.map { it.uniqueName }.toTypedArray()
                val stateArray = addons.map { it.enabled }.toBooleanArray()
                AlertDialog.Builder(this)
                    .setTitle("Enabled Addons")
                    .setMultiChoiceItems(nameArray, stateArray) { _, which, checked ->
                        stateArray[which] = checked
                    }
                    .setNegativeButton("Cancel") { _, _ -> }
                    .setPositiveButton("OK") { _, _ ->
                        fcitx.setAddonState(nameArray, stateArray)
                    }
                    .show()
                true
            }
            R.id.activity_main_save_config -> {
                fcitx.saveConfig()
                true
            }
            R.id.activity_main_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                AlertDialog.Builder(this)
                    .setTitle("Settings")
                    .setItems(arrayOf("global", "addon", "inputmethod")) { _, type ->
                        when (type) {
                            0 -> intent.apply {
                                putExtra("type", "global")
                                startActivity(this)
                            }
                            1 -> {
                                val addons =
                                    fcitx.addons().filter { it.enabled and it.isConfigurable }
                                        .map { it.uniqueName }.toTypedArray()
                                AlertDialog.Builder(this)
                                    .setTitle("addon config")
                                    .setItems(addons) { _, addon ->
                                        intent.apply {
                                            putExtra("type", "addon")
                                            putExtra("addon", addons[addon])
                                            startActivity(this)
                                        }
                                    }
                                    .show()
                            }
                            2 -> {
                                val inputMethods =
                                    fcitx.listIme().map { it.uniqueName }.toTypedArray()
                                AlertDialog.Builder(this)
                                    .setTitle("inputmethod config")
                                    .setItems(inputMethods) { _, im ->
                                        intent.apply {
                                            putExtra("type", "im")
                                            putExtra("im", inputMethods[im])
                                            startActivity(this)
                                        }
                                    }
                                    .show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ -> }
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
