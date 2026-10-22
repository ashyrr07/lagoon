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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lagoon.island.R

/** "Try it": fire fake activities at the island (also what the store screenshots are made from). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScreen(
    onBack: () -> Unit,
    viewModel: DemoViewModel = hiltViewModel(),
) {
    val res = LocalResources.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.island_demo_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.island_demo_back))
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
                stringResource(R.string.island_demo_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DemoButton(stringResource(R.string.island_demo_now_playing)) { viewModel.music(res) }
            DemoButton(stringResource(R.string.island_demo_incoming_call)) { viewModel.incomingCall(res) }
            DemoButton(stringResource(R.string.island_demo_active_call)) { viewModel.activeCall(res) }
            DemoButton(stringResource(R.string.island_demo_timer)) { viewModel.timer(res) }
            DemoButton(stringResource(R.string.island_demo_charging), viewModel::charging)
            DemoButton(stringResource(R.string.island_demo_earbuds), viewModel::earbuds)
            DemoButton(stringResource(R.string.island_demo_message)) { viewModel.message(res) }
            DemoButton(stringResource(R.string.island_demo_navigation)) { viewModel.navigation(res) }
            DemoButton(stringResource(R.string.island_demo_progress)) { viewModel.progress(res) }
            OutlinedButton(onClick = viewModel::clearAll, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.island_demo_clear))
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
