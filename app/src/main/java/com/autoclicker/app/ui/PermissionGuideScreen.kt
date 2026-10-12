package com.autoclicker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoclicker.app.R
import com.autoclicker.app.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionGuideScreen(
    onBack: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.guide_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .windowInsetsPadding(WindowInsets.systemBars)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GuideStep(
                index = 1,
                title = stringResource(R.string.guide_step_1_title),
                desc = stringResource(R.string.guide_step_1_desc),
                granted = state.accessibilityGranted,
                actionLabel = stringResource(R.string.btn_open_accessibility),
                onAction = { ctx.startActivity(PermissionUtils.accessibilitySettingsIntent()) },
            )
            GuideStep(
                index = 2,
                title = stringResource(R.string.guide_step_2_title),
                desc = stringResource(R.string.guide_step_2_desc),
                granted = state.overlayGranted,
                actionLabel = stringResource(R.string.btn_open_overlay),
                onAction = { ctx.startActivity(PermissionUtils.overlaySettingsIntent(ctx)) },
            )
            GuideStep(
                index = 3,
                title = stringResource(R.string.guide_step_3_title),
                desc = stringResource(R.string.guide_step_3_desc),
                granted = true, // we don't probe POST_NOTIFICATIONS, just a hint
                actionLabel = "知道了",
                onAction = onBack,
            )
        }
    }

    // Refresh permission statuses when this screen is composed (e.g. user returns from settings).
    androidx.compose.runtime.LaunchedEffect(Unit) {
        vm.refreshPermissionStatuses()
    }
}

@Composable
private fun GuideStep(
    index: Int,
    title: String,
    desc: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("步骤 $index · $title", style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodyMedium)
            if (granted) {
                Text(
                    text = "✓ 已授予",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
