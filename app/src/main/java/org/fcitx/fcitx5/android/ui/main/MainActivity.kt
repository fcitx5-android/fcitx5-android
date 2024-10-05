/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.databinding.ActivityMainBinding
import org.fcitx.fcitx5.android.ui.main.settings.PinyinDictionaryFragment
import org.fcitx.fcitx5.android.ui.setup.SetupActivity
import org.fcitx.fcitx5.android.utils.Const
import org.fcitx.fcitx5.android.utils.item
import org.fcitx.fcitx5.android.utils.startActivity
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.topPadding

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = navBars.left
                rightMargin = navBars.right
            }
            binding.toolbar.topPadding = statusBars.top
            windowInsets
        }
        setContentView(binding.root)
        // always show toolbar back arrow icon
        // https://android.googlesource.com/platform/frameworks/support/+/32e643112d0217619237a0d7101b50919c6caf51/navigation/navigation-ui/src/main/java/androidx/navigation/ui/AbstractAppBarOnDestinationChangedListener.kt#80
        binding.toolbar.navigationIcon = DrawerArrowDrawable(this).apply { progress = 1f }
        // show menu icon and other action icons on toolbar
        // don't use `setSupportActionBar(binding.toolbar)` here,
        // because navController would change toolbar title, we need to control it by ourselves
        setupToolbarMenu(binding.toolbar.menu)
        navController = binding.navHostFragment.getFragment<NavHostFragment>().navController
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
        viewModel.toolbarShadow.observe(this) {
            binding.toolbar.elevation = dp(if (it) 4f else 0f)
        }
        navController.addOnDestinationChangedListener { _, dest, _ ->
            dest.label?.let { viewModel.setToolbarTitle(it.toString()) }
            when (dest.id) {
                R.id.themeFragment -> viewModel.disableToolbarShadow()
                else -> viewModel.enableToolbarShadow()
            }
        }
        if (intent?.action == Intent.ACTION_MAIN && SetupActivity.shouldShowUp()) {
            startActivity<SetupActivity>()
        } else {
            processIntent(intent)
        }
        addOnNewIntentListener {
            processIntent(it)
        }
        checkNotificationPermission()
    }

    private fun processIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let {
                AlertDialog.Builder(this)
                    .setTitle(R.string.pinyin_dict)
                    .setMessage(R.string.whether_import_dict)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        navController.popBackStack(R.id.mainFragment, false)
                        navController.navigate(
                            R.id.action_mainFragment_to_pinyinDictionaryFragment,
                            bundleOf(PinyinDictionaryFragment.INTENT_DATA_URI to it)
                        )
                    }
                    .show()
            }
        }
    }

    private fun setupToolbarMenu(menu: Menu) {
        val iconTint = styledColor(android.R.attr.colorControlNormal)
        menu.item(R.string.save, R.drawable.ic_baseline_save_24, iconTint, true) {
            viewModel.toolbarSaveButtonOnClickListener.value?.invoke()
        }.apply {
            viewModel.toolbarSaveButtonOnClickListener
                .observe(this@MainActivity) { listener -> isVisible = listener != null }
        }
        val aboutMenuItems = listOf(
            menu.item(R.string.faq) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Const.faqUrl)))
            },
            menu.item(R.string.developer) {
                navController.navigate(R.id.action_mainFragment_to_developerFragment)
            },
            menu.item(R.string.about) {
                navController.navigate(R.id.action_mainFragment_to_aboutFragment)
            }
        )
        viewModel.aboutButton.observe(this@MainActivity) { enabled ->
            aboutMenuItems.forEach { menu -> menu.isVisible = enabled }
        }
        menu.item(R.string.edit, R.drawable.ic_baseline_edit_24, iconTint, true) {
            viewModel.toolbarEditButtonOnClickListener.value?.invoke()
        }.apply {
            viewModel.toolbarEditButtonVisible.observe(this@MainActivity) { isVisible = it }
        }
        menu.item(R.string.delete, R.drawable.ic_baseline_delete_24, iconTint, true) {
            viewModel.toolbarDeleteButtonOnClickListener.value?.invoke()
        }.apply {
            viewModel.toolbarDeleteButtonOnClickListener
                .observe(this@MainActivity) { listener -> isVisible = listener != null }
        }
        // all menus should be invisible and enabled on demand
        menu.forEach { it.isVisible = false }
    }

    private var needNotifications by AppPrefs.getInstance().internal.needNotifications

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                needNotifications = true
                return
            }
            // do not ask again if user denied the request
            if (!needNotifications) return
            // always show a dialog to explain why we need notification permission,
            // regardless of `shouldShowRequestPermissionRationale(...)`
            AlertDialog.Builder(this)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.notification_permission_title)
                .setMessage(R.string.notification_permission_message)
                .setNegativeButton(R.string.i_do_not_need_it) { _, _ ->
                    // do not ask again if user denied the request
                    needNotifications = false
                }
                .setPositiveButton(R.string.grant_permission) { _, _ ->
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
                }
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 0) return
        // do not ask again if user denied the request
        needNotifications = grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED
    }

    override fun onStop() {
        viewModel.fcitx.runIfReady {
            save()
        }
        super.onStop()
    }

}
