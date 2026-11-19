package com.aether.executor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aether.executor.core.execution.ExecutionStatus
import com.aether.executor.core.runtime.LogLevel

private val Navy = Color(0xFF07111F)
private val Panel = Color(0xFF0D1B2A)
private val PanelAlt = Color(0xFF13263A)
private val Cyan = Color(0xFF42D9F5)
private val Muted = Color(0xFF8EA4B8)
private val Green = Color(0xFF5CE0A0)
private val Red = Color(0xFFFF6B78)
private val Amber = Color(0xFFFFC66D)

@Composable
fun AetherApp(vm: AetherViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val running = state.status == ExecutionStatus.RUNNING

    Surface(modifier = Modifier.fillMaxSize(), color = Navy) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Header(state.status)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = vm::execute,
                    enabled = !running,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan,
                        contentColor = Navy,
                        disabledContainerColor = PanelAlt,
                        disabledContentColor = Muted
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (running) "RUNNING" else "▶ EXECUTE", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
                Button(
                    onClick = vm::stop,
                    enabled = running,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF512B39),
                        contentColor = Color(0xFFFFA6B0),
                        disabledContainerColor = PanelAlt,
                        disabledContentColor = Muted
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("■ STOP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            SectionTitle("SCRIPT EDITOR", "JAVASCRIPT")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.25f)
                    .background(Panel, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                BasicTextField(
                    value = state.source,
                    onValueChange = vm::updateSource,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    textStyle = TextStyle(
                        color = Color(0xFFD7E6F5),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(Cyan)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("CONSOLE", "LIVE OUTPUT")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = vm::clearConsole) {
                    Text("CLEAR", color = Muted, fontSize = 11.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.85f)
                    .background(Panel, RoundedCornerShape(14.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (state.console.isEmpty()) {
                    Text("Console is empty. Execute a script to see output.", color = Muted, fontSize = 12.sp)
                } else {
                    state.console.forEach { entry ->
                        val color = when (entry.level) {
                            LogLevel.INFO -> Color(0xFFBBD3E8)
                            LogLevel.WARNING -> Amber
                            LogLevel.ERROR -> Red
                        }
                        Text(
                            "${entry.timestamp}  ${entry.level.name}  ${entry.message}",
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Text(
                "M1 • LOCAL JAVASCRIPT RUNTIME • NO ROOT REQUIRED",
                color = Muted,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun Header(status: ExecutionStatus) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("AETHER", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("EXECUTOR  /  M1 FOUNDATION", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp)
        }
        val (label, color) = when (status) {
            ExecutionStatus.RUNNING -> "RUNNING" to Cyan
            ExecutionStatus.COMPLETED -> "COMPLETED" to Green
            ExecutionStatus.STOPPED -> "STOPPED" to Amber
            ExecutionStatus.FAILED -> "FAILED" to Red
            ExecutionStatus.IDLE -> "IDLE" to Muted
        }
        Text(
            "● $label",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.background(PanelAlt, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun SectionTitle(title: String, tag: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.weight(1f))
        Text(tag, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
    }
}
