package com.aether.executor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.executor.data.model.SettingsData
import com.aether.executor.data.repository.SettingsRepository
import com.aether.executor.ui.theme.AetherPalette
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: SettingsData,
    onChange: ((SettingsData) -> SettingsData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SETTINGS",
            color = AetherPalette.Cyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        SettingsGroup(title = "EDITOR") {
            SliderRow(
                label = "Font size",
                value = "${settings.fontSizeSp.roundToInt()} sp",
                slider = {
                    Slider(
                        value = settings.fontSizeSp,
                        onValueChange = { value ->
                            onChange { it.copy(fontSizeSp = value) }
                        },
                        valueRange = SettingsRepository.MIN_FONT_SIZE..SettingsRepository.MAX_FONT_SIZE
                    )
                }
            )
            SliderRow(
                label = "Tab size",
                value = "${settings.tabSize} spaces",
                slider = {
                    Slider(
                        value = settings.tabSize.toFloat(),
                        onValueChange = { value ->
                            onChange { it.copy(tabSize = value.roundToInt()) }
                        },
                        valueRange = SettingsRepository.MIN_TAB_SIZE.toFloat()..SettingsRepository.MAX_TAB_SIZE.toFloat(),
                        steps = SettingsRepository.MAX_TAB_SIZE - SettingsRepository.MIN_TAB_SIZE - 1
                    )
                }
            )
            ToggleRow(
                label = "Word wrap",
                description = "Wrap long lines instead of horizontal scrolling",
                checked = settings.wordWrap,
                onCheckedChange = { value -> onChange { it.copy(wordWrap = value) } }
            )
            ToggleRow(
                label = "Line numbers",
                description = "Show the line number gutter",
                checked = settings.lineNumbers,
                onCheckedChange = { value -> onChange { it.copy(lineNumbers = value) } }
            )
        }

        SettingsGroup(title = "EXECUTION") {
            SliderRow(
                label = "Timeout",
                value = "${settings.executionTimeoutSeconds} seconds",
                slider = {
                    Slider(
                        value = settings.executionTimeoutSeconds.toFloat(),
                        onValueChange = { value ->
                            onChange { it.copy(executionTimeoutSeconds = value.roundToInt()) }
                        },
                        valueRange = SettingsRepository.MIN_TIMEOUT.toFloat()..SettingsRepository.MAX_TIMEOUT.toFloat()
                    )
                }
            )
            Text(
                text = "Stop uses the QuickJS interrupt handler, not thread killing. " +
                    "If native code ignores the interrupt, the runtime is closed and the " +
                    "execution is marked stopped.",
                color = AetherPalette.Muted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        }

        SettingsGroup(title = "SANDBOX") {
            Text(
                text = "Scripts run in-process with QuickJS. Only console.log, " +
                    "console.info, console.warn and console.error are exposed. " +
                    "There is no network, filesystem, Android API or cross-app access. " +
                    "A memory limit and the execution timeout are enforced.",
                color = AetherPalette.Muted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        }

        SettingsGroup(title = "ABOUT") {
            InfoRow("Application", "Aether Executor")
            InfoRow("Version", "0.2.0 (M2)")
            InfoRow("Runtime", "QuickJS via quickjs-kt 1.0.15")
            InfoRow("Editor", "Sora Editor 0.23.6 + Aether JS analyzer")
            InfoRow("Storage", "App-private files only")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AetherPalette.Panel)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            color = AetherPalette.Blue,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        content()
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: String,
    slider: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = AetherPalette.Text, fontSize = 12.sp)
            Text(
                value,
                color = AetherPalette.Cyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        slider()
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = AetherPalette.Text, fontSize = 12.sp)
            Text(
                description,
                color = AetherPalette.Muted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AetherPalette.Navy,
                checkedTrackColor = AetherPalette.Cyan,
                uncheckedThumbColor = AetherPalette.Muted,
                uncheckedTrackColor = AetherPalette.PanelAlt
            )
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AetherPalette.Muted, fontSize = 11.sp)
        Text(
            value,
            color = AetherPalette.Text,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
