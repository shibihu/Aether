package com.aether.executor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AetherColors = darkColorScheme(
    primary = Color(0xFF42D9F5),
    onPrimary = Color(0xFF07111F),
    secondary = Color(0xFF8EA4B8),
    background = Color(0xFF07111F),
    surface = Color(0xFF0D1B2A),
    onSurface = Color(0xFFD7E6F5)
)

@Composable
fun AetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AetherColors, content = content)
}
