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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import com.aether.executor.data.model.ScriptSummary
import com.aether.executor.ui.components.formatSize
import com.aether.executor.ui.components.formatTimestamp
import com.aether.executor.ui.theme.AetherPalette

@Composable
fun ScriptsScreen(
    scripts: List<ScriptSummary>,
    onCreate: (String) -> Unit,
    onOpen: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var createDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ScriptSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<ScriptSummary?>(null) }

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
                    text = "SCRIPTS",
                    color = AetherPalette.Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "${scripts.size} stored in app-private storage",
                    color = AetherPalette.Muted,
                    fontSize = 10.sp
                )
            }
            Button(
                onClick = { createDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AetherPalette.Cyan,
                    contentColor = AetherPalette.Navy
                )
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text("NEW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (scripts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No saved scripts yet.\nCreate one or save the current tab.",
                    color = AetherPalette.Muted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scripts, key = { it.id }) { script ->
                    ScriptRow(
                        script = script,
                        onOpen = { onOpen(script.id) },
                        onRename = { renameTarget = script },
                        onDelete = { deleteTarget = script }
                    )
                }
            }
        }
    }

    if (createDialog) {
        NameDialog(
            title = "New script",
            label = "Script name",
            initial = "script",
            confirmLabel = "Create",
            onDismiss = { createDialog = false },
            onConfirm = { name ->
                onCreate(name)
                createDialog = false
            }
        )
    }

    renameTarget?.let { script ->
        NameDialog(
            title = "Rename script",
            label = "Script name",
            initial = script.name,
            confirmLabel = "Rename",
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                onRename(script.id, name)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { script ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete script?") },
            text = {
                Text(
                    "\"${script.name}\" will be removed from app-private storage. " +
                        "Open editor tabs keep their current content."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(script.id)
                    deleteTarget = null
                }) { Text("Delete", color = AetherPalette.Red) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ScriptRow(
    script: ScriptSummary,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AetherPalette.Panel)
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = script.name,
                color = AetherPalette.Text,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${formatTimestamp(script.updatedAt)}  ·  ${formatSize(script.sizeBytes)}",
                color = AetherPalette.Muted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        IconButton(onClick = onRename) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Rename ${script.name}",
                tint = AetherPalette.Blue,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete ${script.name}",
                tint = AetherPalette.Red,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun NameDialog(
    title: String,
    label: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text(label) },
                supportingText = { Text("Stored as ${value.trim().ifBlank { "script" }}.js") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank()
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
