package com.autoclicker.app.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autoclicker.app.R

/**
 * Floating control panel — the always-on-top chip the user sees over every app.
 * Pressing 📍 here hands off to [PickerController], a separate draggable marker
 * window; the panel itself is *not* the picker anymore.
 *
 * The whole card is a drag surface. detectDragGestures only fires once the user
 * moves, so quick taps on the inner buttons still reach them.
 */
@Composable
fun FloatingPanel(
    running: Boolean,
    pickerShown: Boolean,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    onRequestPick: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDragDelta(drag.x, drag.y)
                }
            },
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Drag handle / visual header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
                        .size(width = 64.dp, height = 4.dp),
                )
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                        .size(width = 6.dp, height = 6.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50),
                        )
                        .size(40.dp),
                ) {
                    if (running) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = stringResource(R.string.floating_stop),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.floating_start),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Text(
                    text = if (running) "运行中" else "已暂停",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = onRequestPick,
                    enabled = !pickerShown,
                    modifier = Modifier
                        .background(
                            color = if (pickerShown) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f)
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(50),
                        )
                        .size(40.dp),
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.floating_pick),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.floating_close),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}
