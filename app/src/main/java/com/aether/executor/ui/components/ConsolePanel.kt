package com.aether.executor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.executor.core.console.ConsoleEntry
import com.aether.executor.core.execution.ExecutionStatus
import com.aether.executor.core.runtime.LogLevel
import com.aether.executor.ui.theme.AetherPalette
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

@Composable
fun ConsolePanel(
    entries: List<ConsoleEntry>,
    status: ExecutionStatus,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= info.totalItemsCount - 2
        }
    }

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty() && atBottom) {
            listState.scrollToItem(entries.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AetherPalette.Panel)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AetherPalette.PanelAlt)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CONSOLE",
                    color = AetherPalette.Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = statusLabel(status),
                    color = statusColor(status),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onClear, modifier = Modifier.height(34.dp)) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "Clear console",
                    tint = AetherPalette.Muted,
                    modifier = Modifier.height(16.dp)
                )
            }
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Console output appears here",
                    color = AetherPalette.Muted,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 10.dp,
                    vertical = 2.dp
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(entries.size, key = { entries[it].id }) { index ->
                    ConsoleRow(entries[index])
                }
            }
        }
    }
}

@Composable
private fun ConsoleRow(entry: ConsoleEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = timeFormat.format(Instant.ofEpochMilli(entry.timestampMillis)),
            color = AetherPalette.Muted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp, end = 6.dp)
        )
        Text(
            text = entry.level.name.take(1),
            color = levelColor(entry.level),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .padding(top = 1.dp, end = 6.dp)
                .background(levelColor(entry.level).copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
        SelectionContainer {
            Text(
                text = entry.message,
                color = levelColor(entry.level),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
        }
    }
}

fun statusLabel(status: ExecutionStatus): String = when (status) {
    ExecutionStatus.IDLE -> "IDLE"
    ExecutionStatus.RUNNING -> "RUNNING"
    ExecutionStatus.STOPPING -> "STOPPING"
    ExecutionStatus.COMPLETED -> "COMPLETED"
    ExecutionStatus.STOPPED -> "STOPPED"
    ExecutionStatus.FAILED -> "FAILED"
}

fun statusColor(status: ExecutionStatus): Color = when (status) {
    ExecutionStatus.IDLE -> AetherPalette.Muted
    ExecutionStatus.RUNNING -> AetherPalette.Cyan
    ExecutionStatus.STOPPING -> AetherPalette.Amber
    ExecutionStatus.COMPLETED -> AetherPalette.Green
    ExecutionStatus.STOPPED -> AetherPalette.Amber
    ExecutionStatus.FAILED -> AetherPalette.Red
}

fun levelColor(level: LogLevel): Color = when (level) {
    LogLevel.LOG -> AetherPalette.Text
    LogLevel.INFO -> AetherPalette.Blue
    LogLevel.WARN -> AetherPalette.Amber
    LogLevel.ERROR -> AetherPalette.Red
}
