package com.aether.executor.data.model

import com.aether.executor.core.execution.ExecutionStatus
import java.util.UUID

data class EditorTab(
    val id: String,
    val title: String,
    val content: String,
    val linkedScriptId: String? = null,
    val dirty: Boolean = false
) {
    companion object {
        fun create(
            title: String,
            content: String = "",
            linkedScriptId: String? = null,
            dirty: Boolean = false
        ) = EditorTab(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            linkedScriptId = linkedScriptId,
            dirty = dirty
        )
    }
}

data class TabsSnapshot(
    val activeTabId: String?,
    val tabs: List<EditorTab>
)

data class ScriptSummary(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val sizeBytes: Long
)

data class ScriptFile(
    val id: String,
    val name: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class ExecutionRecord(
    val id: String,
    val scriptId: String?,
    val scriptName: String,
    val source: String,
    val executedAt: Long,
    val durationMillis: Long,
    val status: ExecutionStatus,
    val errorMessage: String?
)

data class SettingsData(
    val fontSizeSp: Float = 14f,
    val executionTimeoutSeconds: Int = 10,
    val tabSize: Int = 4,
    val wordWrap: Boolean = false,
    val lineNumbers: Boolean = true
)

enum class AppSection {
    EDITOR,
    SCRIPTS,
    HISTORY,
    SETTINGS
}
