package com.aether.executor.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault())

fun formatTimestamp(millis: Long): String =
    if (millis <= 0L) "unknown" else dateTimeFormat.format(Instant.ofEpochMilli(millis))

fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

fun formatDuration(millis: Long): String = when {
    millis < 1000 -> "$millis ms"
    millis < 60_000 -> "%.2f s".format(millis / 1000.0)
    else -> "%d m %d s".format(millis / 60_000, (millis / 1000) % 60)
}
