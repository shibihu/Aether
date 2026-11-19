package com.aether.executor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Aether identity: deep navy surfaces, electric cyan accents.
 */
object AetherPalette {
    val Navy = Color(0xFF07111F)
    val NavyDeep = Color(0xFF040B14)
    val Panel = Color(0xFF0D1B2A)
    val PanelAlt = Color(0xFF13263A)
    val Border = Color(0xFF1E3A52)
    val Cyan = Color(0xFF42D9F5)
    val CyanDim = Color(0xFF1B6E80)
    val Text = Color(0xFFD7E6F5)
    val Muted = Color(0xFF8EA4B8)
    val Green = Color(0xFF5CE0A0)
    val Red = Color(0xFFFF6B78)
    val Amber = Color(0xFFFFC66D)
    val Blue = Color(0xFF7CC7FF)
    val Purple = Color(0xFFB9A2FF)
}

private val AetherColors = darkColorScheme(
    primary = AetherPalette.Cyan,
    onPrimary = AetherPalette.Navy,
    primaryContainer = AetherPalette.CyanDim,
    onPrimaryContainer = AetherPalette.Text,
    secondary = AetherPalette.Blue,
    onSecondary = AetherPalette.Navy,
    tertiary = AetherPalette.Purple,
    background = AetherPalette.Navy,
    onBackground = AetherPalette.Text,
    surface = AetherPalette.Panel,
    onSurface = AetherPalette.Text,
    surfaceVariant = AetherPalette.PanelAlt,
    onSurfaceVariant = AetherPalette.Muted,
    outline = AetherPalette.Border,
    error = AetherPalette.Red,
    onError = AetherPalette.Navy
)

@Composable
fun AetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AetherColors, content = content)
}
