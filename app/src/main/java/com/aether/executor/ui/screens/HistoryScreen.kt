package com.aether.executor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.executor.core.execution.ExecutionStatus
import com.aether.executor.data.model.ExecutionRecord
import com.aether.executor.ui.components.formatDuration
import com.aether.executor.ui.components.formatTimestamp
import com.aether.executor.ui.components.statusColor
import com.aether.executor.ui.components.statusLabel
import com.aether.executor.ui.theme.AetherPalette

@Composable
fun HistoryScreen(
    records: List<ExecutionRecord>,
    onReopen: (ExecutionRecord) -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "HISTORY",
                    color = AetherPalette.Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "${records.size} stored runs",
                    color = AetherPalette.Muted,
                    fontSize = 10.sp
                )
            }
            OutlinedButton(
                onClick = { confirmClear = true },
                enabled = records.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AetherPalette.Red)
            ) {
                Icon(
                    Icons.Filled.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text("CLEAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No executions recorded yet.",
                    color = AetherPalette.Muted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records, key = { it.id }) { record ->
                    HistoryRow(
                        record = record,
                        onReopen = { onReopen(record) },
                        onDelete = { onDelete(record.id) }
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("All stored execution records and their source code will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    confirmClear = false
                }) { Text("Clear", color = AetherPalette.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HistoryRow(
    record: ExecutionRecord,
    onReopen: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AetherPalette.Panel)
            .clickable(onClick = onReopen)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = statusLabel(record.status),
                color = statusColor(record.status),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor(record.status).copy(alpha = 0.14f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = record.scriptName,
                color = AetherPalette.Text,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onReopen,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Reopen ${record.scriptName}",
                    tint = AetherPalette.Cyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete history entry",
                    tint = AetherPalette.Muted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${formatTimestamp(record.executedAt)}  ·  ${formatDuration(record.durationMillis)}" +
                "  ·  ${record.source.length} chars",
            color = AetherPalette.Muted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        if (!record.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = record.errorMessage,
                color = if (record.status == ExecutionStatus.FAILED) {
                    AetherPalette.Red
                } else {
                    AetherPalette.Amber
                },
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
