package com.autoclicker.app.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * Independent picker marker UI. Renders as a circular floating chip:
 *  - Center: 📍 + "拖动选点" hint
 *  - Bottom: ✓ confirm / ✕ cancel buttons
 *
 * The whole chip is a drag surface. The ✓/✕ buttons swallow taps so dragging
 * never accidentally triggers them (detectDragGestures only fires once the
 * user moves; a quick tap is delivered to the underlying Button).
 *
 * Size must match [PickerController.PICKER_DP_SIZE].
 */
@Composable
fun PickerPanel(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .shadow(10.dp, RoundedCornerShape(50))
            .background(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(50),
            )
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDragDelta(drag.x, drag.y)
                }
            }
            .padding(6.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top: hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onTertiary,
                )
                Text(
                    text = stringResource(R.string.picker_hint_drag),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
            }
            // Bottom: ✓ / ✕
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50),
                        ),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.picker_btn_cancel),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onTertiary,
                    )
                }
                IconButton(
                    onClick = onConfirm,
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.32f),
                            shape = RoundedCornerShape(50),
                        ),
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.picker_btn_confirm),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }
        }
    }
}
