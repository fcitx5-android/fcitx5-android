/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.theme

import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.ui.main.modified.MySwitchPreference
import org.fcitx.fcitx5.android.utils.LongClickPreference

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsPreferenceContent(adapter: PreferenceGroupAdapter?) {
    var revision by remember(adapter) { mutableIntStateOf(0) }
    DisposableEffect(adapter) {
        val observer = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() { revision++ }
            override fun onItemRangeChanged(start: Int, count: Int) { revision++ }
            override fun onItemRangeChanged(start: Int, count: Int, payload: Any?) { revision++ }
            override fun onItemRangeInserted(start: Int, count: Int) { revision++ }
            override fun onItemRangeRemoved(start: Int, count: Int) { revision++ }
            override fun onItemRangeMoved(from: Int, to: Int, count: Int) { revision++ }
        }
        adapter?.registerAdapterDataObserver(observer)
        onDispose { adapter?.unregisterAdapterDataObserver(observer) }
    }
    // Read mutable preference values into a fresh snapshot after every adapter notification.
    val rows = remember(adapter, revision) {
        (0 until (adapter?.itemCount ?: 0)).mapNotNull { adapter?.getItem(it) }.map {
            PreferenceRow(it, it.title?.toString().orEmpty(), it.summary?.toString(),
                it.isEnabled, (it as? TwoStatePreference)?.isChecked)
        }
    }
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        itemsIndexed(rows, key = { index, row -> row.preference.key?.let { "key:$it" } ?: "index:$index" }) { index, row ->
            if (row.preference is PreferenceCategory) {
                Text(row.title, Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            } else {
                val first = index == 0 || rows[index - 1].preference is PreferenceCategory
                val last = index == rows.lastIndex || rows[index + 1].preference is PreferenceCategory
                val shape = RoundedCornerShape(
                    topStart = if (first) 20.dp else 0.dp, topEnd = if (first) 20.dp else 0.dp,
                    bottomStart = if (last) 20.dp else 0.dp, bottomEnd = if (last) 20.dp else 0.dp)
                val click = { row.preference.performClick() }
                val modifier = Modifier.fillMaxWidth().clip(shape).background(surface)
                    .combinedClickable(
                        enabled = row.enabled,
                        onClick = click,
                        onLongClick = when (val p = row.preference) {
                            is MySwitchPreference -> { { p.showResetDialog() } }
                            is LongClickPreference -> { { p.performLongClick(); Unit } }
                            else -> null
                        }
                    )
                ListItem(
                    modifier = modifier.alpha(if (row.enabled) 1f else .38f),
                    headlineContent = { Text(row.title) },
                    supportingContent = row.summary?.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
                    leadingContent = row.preference.icon?.let { { PreferenceIcon(row.preference) } },
                    trailingContent = row.checked?.let { checked -> {
                        Switch(
                            checked = checked,
                            enabled = row.enabled,
                            onCheckedChange = { click() },
                            modifier = Modifier.semantics { contentDescription = row.title }
                        )
                    } },
                    colors = ListItemDefaults.colors(containerColor = surface)
                )
            }
        }
        item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }
}

private data class PreferenceRow(val preference: Preference, val title: String,
    val summary: String?, val enabled: Boolean, val checked: Boolean?)

@Composable
private fun PreferenceIcon(preference: Preference) {
    AndroidView(factory = { ImageView(it) }, update = { it.setImageDrawable(preference.icon) },
        modifier = Modifier.size(24.dp))
}
