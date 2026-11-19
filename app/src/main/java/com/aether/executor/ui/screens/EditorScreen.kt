package com.aether.executor.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.executor.ui.AetherUiState
import com.aether.executor.ui.components.ConsolePanel
import com.aether.executor.ui.components.EditorTabBar
import com.aether.executor.ui.components.statusColor
import com.aether.executor.ui.components.statusLabel
import com.aether.executor.ui.editor.AetherCodeEditor
import com.aether.executor.ui.theme.AetherPalette

@Composable
fun EditorScreen(
    state: AetherUiState,
    onSelectTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onCloseTab: (String) -> Unit,
    onRenameTab: (String, String) -> Unit,
    onContentChange: (String, String) -> Unit,
    onSave: () -> Unit,
    onExecute: () -> Unit,
    onStop: () -> Unit,
    onClearConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTab = state.activeTab
    var renameTabId by remember { mutableStateOf<String?>(null) }
    var closeTabId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EditorTabBar(
            tabs = state.tabs,
            activeTabId = state.activeTabId,
            onSelect = onSelectTab,
            onClose = { tabId ->
                val tab = state.tabs.firstOrNull { it.id == tabId }
                if (tab != null && tab.dirty) {
                    closeTabId = tabId
                } else {
                    onCloseTab(tabId)
                }
            },
            onRename = { renameTabId = it },
            onNew = onNewTab
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor(state.status).copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusLabel(state.status),
                    color = statusColor(state.status),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = activeTab?.let {
                    if (it.dirty) "unsaved changes" else "saved"
                } ?: "no tab",
                color = if (activeTab?.dirty == true) AetherPalette.Amber else AetherPalette.Muted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )

            OutlinedButton(
                onClick = onSave,
                enabled = activeTab != null,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text("SAVE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = if (state.isRunning) onStop else onExecute,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRunning) {
                        AetherPalette.Red.copy(alpha = 0.22f)
                    } else {
                        AetherPalette.Cyan
                    },
                    contentColor = if (state.isRunning) AetherPalette.Red else AetherPalette.Navy
                )
            ) {
                Icon(
                    imageVector = if (state.isRunning) Icons.Filled.Clear else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = if (state.isRunning) "STOP" else "RUN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(AetherPalette.Panel)
        ) {
            if (activeTab == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No script open",
                        color = AetherPalette.Muted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onNewTab) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("New tab")
                    }
                }
            } else {
                key(activeTab.id) {
                    AetherCodeEditor(
                        tabId = activeTab.id,
                        content = activeTab.content,
                        settings = state.settings,
                        onContentChange = { onContentChange(activeTab.id, it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        ConsolePanel(
            entries = state.console,
            status = state.status,
            onClear = onClearConsole,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        )
    }

    val renameTab = state.tabs.firstOrNull { it.id == renameTabId }
    if (renameTab != null) {
        RenameTabDialog(
            initial = renameTab.title,
            onDismiss = { renameTabId = null },
            onConfirm = { newTitle ->
                onRenameTab(renameTab.id, newTitle)
                renameTabId = null
            }
        )
    }

    val closeTab = state.tabs.firstOrNull { it.id == closeTabId }
    if (closeTab != null) {
        AlertDialog(
            onDismissRequest = { closeTabId = null },
            title = { Text("Discard changes?") },
            text = {
                Text("\"${closeTab.title}\" has unsaved changes. Close it anyway?")
            },
            confirmButton = {
                TextButton(onClick = {
                    onCloseTab(closeTab.id)
                    closeTabId = null
                }) { Text("Discard", color = AetherPalette.Red) }
            },
            dismissButton = {
                TextButton(onClick = { closeTabId = null }) { Text("Keep editing") }
            }
        )
    }
}

@Composable
private fun RenameTabDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename tab") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("Tab name") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank()
            ) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
