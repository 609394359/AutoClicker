package com.autoclicker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoclicker.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenGuide: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val snack by vm.snackMessage.collectAsStateWithLifecycle(null)

    LaunchedEffect(cfg) {
        // Save on every meaningful change to keep prefs in sync.
        vm.persist()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_home),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    OutlinedButton(onClick = onOpenGuide) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("权限设置")
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .windowInsetsPadding(WindowInsets.systemBars)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PermissionBanner(
                accessibilityGranted = state.accessibilityGranted,
                overlayGranted = state.overlayGranted,
                onClickGuide = onOpenGuide,
            )

            FloatingControlCard(
                overlayGranted = state.overlayGranted,
                onShowFloating = { vm.showFloating() },
                onHideFloating = { vm.hideFloating() },
            )

            StatusCard(
                running = state.running,
                performedCount = state.performedCount,
                onStart = { vm.start() },
                onStop = { vm.stop() },
            )

            CoordinateCard(
                x = cfg.x,
                y = cfg.y,
                onXChange = { v -> vm.update(cfg.copy(x = v)) },
                onYChange = { v -> vm.update(cfg.copy(y = v)) },
                onPick = { vm.startPicker() },
                onTest = { vm.testClick() },
            )

            IntervalCard(
                intervalMs = cfg.intervalMs,
                onIntervalChange = { v -> vm.update(cfg.copy(intervalMs = v)) },
                jitter = cfg.randomizeJitter,
                onJitterChange = { v -> vm.update(cfg.copy(randomizeJitter = v)) },
            )

            RepeatCard(
                repeat = cfg.repeatCount,
                onRepeatChange = { v -> vm.update(cfg.copy(repeatCount = v)) },
            )

            Button(
                onClick = { vm.save() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_save))
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = snack.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    // The coordinate picker overlay (cancellable) is launched as a separate activity-like intent
    // by the ViewModel; we keep the screen declarative here.
    DisposableEffect(Unit) {
        onDispose {
            // No-op: ViewModel handles its own cleanup.
        }
    }
}

@Composable
private fun PermissionBanner(
    accessibilityGranted: Boolean,
    overlayGranted: Boolean,
    onClickGuide: () -> Unit,
) {
    if (accessibilityGranted && overlayGranted) return
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val msg = when {
                !accessibilityGranted -> stringResource(R.string.status_service_required)
                !overlayGranted -> stringResource(R.string.status_overlay_required)
                else -> ""
            }
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium,
            )
            FilledTonalButton(onClick = onClickGuide) {
                Text(stringResource(R.string.guide_title))
            }
        }
    }
}

@Composable
private fun StatusCard(
    running: Boolean,
    performedCount: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val status = if (running) {
                stringResource(R.string.status_running, performedCount)
            } else stringResource(R.string.status_idle)
            Text(status, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onStart,
                    enabled = !running,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_start))
                }
                OutlinedButton(
                    onClick = onStop,
                    enabled = running,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_stop))
                }
            }
        }
    }
}

@Composable
private fun FloatingControlCard(
    overlayGranted: Boolean,
    onShowFloating: () -> Unit,
    onHideFloating: () -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("悬浮控制台", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (overlayGranted) {
                    "悬浮控制台会显示在所有 App 之上，\n可随时启动 / 停止自动点击。"
                } else {
                    "需要在系统设置中授予「显示悬浮窗」权限后才能使用。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onShowFloating,
                    enabled = overlayGranted,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("显示悬浮控制台")
                }
                OutlinedButton(
                    onClick = onHideFloating,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

@Composable
private fun CoordinateCard(
    x: Int,
    y: Int,
    onXChange: (Int) -> Unit,
    onYChange: (Int) -> Unit,
    onPick: () -> Unit,
    onTest: () -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("坐标", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = x.toString(),
                    onValueChange = { s -> s.toIntOrNull()?.let(onXChange) },
                    label = { Text(stringResource(R.string.label_x)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = y.toString(),
                    onValueChange = { s -> s.toIntOrNull()?.let(onYChange) },
                    label = { Text(stringResource(R.string.label_y)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = onPick,
                    label = { Text(stringResource(R.string.btn_pick_xy)) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                )
                AssistChip(
                    onClick = onTest,
                    label = { Text("测试单击") },
                    leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun IntervalCard(
    intervalMs: Int,
    onIntervalChange: (Int) -> Unit,
    jitter: Boolean,
    onJitterChange: (Boolean) -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("间隔", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = intervalMs.toString(),
                onValueChange = { s -> s.toIntOrNull()?.let(onIntervalChange) },
                label = { Text(stringResource(R.string.label_interval)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = jitter, onCheckedChange = onJitterChange)
                Spacer(Modifier.width(12.dp))
                Text("启用随机抖动（±15%）", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun RepeatCard(
    repeat: Int,
    onRepeatChange: (Int) -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("次数", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = repeat.toString(),
                onValueChange = { s -> s.toIntOrNull()?.let(onRepeatChange) },
                label = { Text(stringResource(R.string.label_repeat)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "提示：填 0 表示无限循环，直到手动停止。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
