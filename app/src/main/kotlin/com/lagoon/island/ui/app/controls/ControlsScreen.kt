package com.lagoon.island.ui.app.controls

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagoon.island.controls.AppInfo

/**
 * Choose and order what the control deck shows: tiles, sliders, widgets, and the app shortcuts.
 * Reordering is up/down buttons (robust inside a scrolling column; drag can come later).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsScreen(
    onBack: () -> Unit,
    viewModel: ControlsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    // Re-evaluated when Enhanced mode comes and goes (screenshot / lock / recents tiles).
    val globals by viewModel.globalsAvailable.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control deck") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { TextButton(onClick = viewModel::resetToDefaults) { Text("Reset") } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "Long-press the island to open the deck. Pick what it shows and in which order.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ControlsSection.entries.forEach { section ->
                val chosen = remember(settings, globals, section) { viewModel.chosen(section, settings) }
                val remaining = remember(settings, globals, section) { viewModel.remaining(section, settings) }
                OrderedSection(
                    title = when (section) {
                        ControlsSection.Tiles -> "Tiles"
                        ControlsSection.Sliders -> "Sliders"
                        ControlsSection.Widgets -> "Widgets"
                    },
                    hint = when (section) {
                        ControlsSection.Tiles ->
                            if (globals) "Three per row." else "Three per row. Screenshot, Lock and Recents appear with Enhanced mode."
                        ControlsSection.Sliders -> "Brightness and rotation need the \"Modify system settings\" grant; the slider asks when first used."
                        ControlsSection.Widgets -> "Next event needs calendar access, granted from the system app settings."
                    },
                    chosen = chosen.map { ControlsItem(it, viewModel.labelOf(it)) },
                    remaining = remaining,
                    onMove = { from, to -> viewModel.move(section, from, to) },
                    onRemove = { viewModel.remove(section, it) },
                    onAdd = { viewModel.add(section, it) },
                )
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("App shortcuts", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A row of app icons at the bottom of the deck.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    settings.shortcuts.forEachIndexed { index, pkg ->
                        ShortcutRow(
                            packageName = pkg,
                            label = viewModel.shortcutLabel(pkg),
                            icon = viewModel.shortcutIcon(pkg),
                            canUp = index > 0,
                            canDown = index < settings.shortcuts.lastIndex,
                            onUp = { viewModel.moveShortcut(index, index - 1) },
                            onDown = { viewModel.moveShortcut(index, index + 1) },
                            onRemove = { viewModel.removeShortcut(pkg) },
                        )
                    }
                    FilledTonalButton(onClick = { picking = true; viewModel.loadApps() }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add app")
                    }
                }
            }
        }
    }

    if (picking) {
        AppPickerDialog(
            apps = viewModel.apps.collectAsStateWithLifecycle().value,
            chosen = settings.shortcuts.toSet(),
            onPick = { viewModel.addShortcut(it) },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun OrderedSection(
    title: String,
    hint: String,
    chosen: List<ControlsItem>,
    remaining: List<ControlsItem>,
    onMove: (Int, Int) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            if (chosen.isEmpty()) {
                Text("Nothing shown", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            chosen.forEachIndexed { index, item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onMove(index, index - 1) }, enabled = index > 0) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move up")
                    }
                    IconButton(onClick = { onMove(index, index + 1) }, enabled = index < chosen.lastIndex) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move down")
                    }
                    IconButton(onClick = { onRemove(item.id) }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove")
                    }
                }
            }
            if (remaining.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Available", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    remaining.forEach { item ->
                        AssistChip(
                            onClick = { onAdd(item.id) },
                            label = { Text(item.label) },
                            leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutRow(
    packageName: String,
    label: String,
    icon: android.graphics.drawable.Drawable?,
    canUp: Boolean,
    canDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val density = LocalDensity.current
    val bitmap = remember(packageName, icon) {
        val px = with(density) { 40.dp.roundToPx() }
        runCatching { icon?.toBitmap(px, px)?.asImageBitmap() }.getOrNull()
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, modifier = Modifier.size(32.dp))
        } else {
            Box(Modifier.size(32.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onUp, enabled = canUp) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move up") }
        IconButton(onClick = onDown, enabled = canDown) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move down") }
        IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, contentDescription = "Remove") }
    }
}

@Composable
private fun AppPickerDialog(
    apps: List<AppInfo>?,
    chosen: Set<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query, chosen) {
        apps.orEmpty().filter { it.packageName !in chosen && (query.isBlank() || it.label.contains(query, ignoreCase = true)) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Add app") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                if (apps == null) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            ListItem(
                                headlineContent = { Text(app.label) },
                                supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall) },
                                trailingContent = {
                                    IconButton(onClick = { onPick(app.packageName) }) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Add ${app.label}")
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}
