package com.lagoon.island.ui.app.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/** "Try it": fire fake activities at the island (also what the store screenshots are made from). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScreen(
    onBack: () -> Unit,
    viewModel: DemoViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Try it") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Each button posts a fake activity to the island. Tap the pill to expand, long-press for the app, swipe up to dismiss.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DemoButton("Now playing", viewModel::music)
            DemoButton("Incoming call", viewModel::incomingCall)
            DemoButton("Active call", viewModel::activeCall)
            DemoButton("Timer 5:00", viewModel::timer)
            DemoButton("Charging 27 W", viewModel::charging)
            DemoButton("Earbuds connected", viewModel::earbuds)
            DemoButton("Message with reply", viewModel::message)
            DemoButton("Navigation", viewModel::navigation)
            DemoButton("Delivery progress", viewModel::progress)
            OutlinedButton(onClick = viewModel::clearAll, modifier = Modifier.fillMaxWidth()) {
                Text("Clear all")
            }
        }
    }
}

@Composable
private fun DemoButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}
