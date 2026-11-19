package com.aether.executor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aether.executor.AetherApplication
import com.aether.executor.core.console.ConsoleEntry
import com.aether.executor.core.console.ConsoleManager
import com.aether.executor.core.execution.ExecutionManager
import com.aether.executor.core.execution.ExecutionRequest
import com.aether.executor.core.execution.ExecutionStatus
import com.aether.executor.data.model.AppSection
import com.aether.executor.data.model.EditorTab
import com.aether.executor.data.model.ExecutionRecord
import com.aether.executor.data.model.ScriptSummary
import com.aether.executor.data.model.SettingsData
import com.aether.executor.data.repository.HistoryRepository
import com.aether.executor.data.repository.ScriptRepository
import com.aether.executor.data.repository.SettingsRepository
import com.aether.executor.data.repository.TabsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AetherUiState(
    val section: AppSection = AppSection.EDITOR,
    val tabs: List<EditorTab> = emptyList(),
    val activeTabId: String? = null,
    val tabsReady: Boolean = false,
    val status: ExecutionStatus = ExecutionStatus.IDLE,
    val console: List<ConsoleEntry> = emptyList(),
    val scripts: List<ScriptSummary> = emptyList(),
    val history: List<ExecutionRecord> = emptyList(),
    val settings: SettingsData = SettingsData(),
    val message: String? = null
) {
    val activeTab: EditorTab?
        get() = tabs.firstOrNull { it.id == activeTabId }

    val isRunning: Boolean
        get() = status.isActive
}

class AetherViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AetherApplication
    private val scriptRepository: ScriptRepository = app.scriptRepository
    private val historyRepository: HistoryRepository = app.historyRepository
    private val settingsRepository: SettingsRepository = app.settingsRepository
    private val tabsRepository: TabsRepository = app.tabsRepository

    private val consoleManager = ConsoleManager()
    private val executionManager = ExecutionManager(consoleManager, viewModelScope)

    private val _uiState = MutableStateFlow(AetherUiState())
    val uiState: StateFlow<AetherUiState> = _uiState.asStateFlow()

    private var persistJob: Job? = null
    private var untitledCounter = 1

    init {
        observeConsole()
        observeExecutionStatus()
        observeScripts()
        observeHistory()
        observeSettings()
        restoreSession()
    }

    // ---------------------------------------------------------------- session

    private fun restoreSession() {
        viewModelScope.launch {
            val restored = runCatching { tabsRepository.load() }.getOrNull()
            if (restored != null) {
                _uiState.update {
                    it.copy(
                        tabs = restored.tabs,
                        activeTabId = restored.activeTabId ?: restored.tabs.first().id,
                        tabsReady = true
                    )
                }
                untitledCounter = restored.tabs.count { it.linkedScriptId == null } + 1
            } else {
                val tab = EditorTab.create(
                    title = "Untitled.js",
                    content = DEFAULT_SCRIPT
                )
                _uiState.update {
                    it.copy(tabs = listOf(tab), activeTabId = tab.id, tabsReady = true)
                }
            }
            scriptRepository.refresh()
            historyRepository.refresh()
        }
    }

    private fun observeConsole() {
        viewModelScope.launch {
            consoleManager.entries.collect { entries ->
                _uiState.update { it.copy(console = entries) }
            }
        }
    }

    private fun observeExecutionStatus() {
        viewModelScope.launch {
            executionManager.status.collect { status ->
                _uiState.update { it.copy(status = status) }
            }
        }
    }

    private fun observeScripts() {
        viewModelScope.launch {
            scriptRepository.scripts.collect { scripts ->
                _uiState.update { it.copy(scripts = scripts) }
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            historyRepository.records.collect { records ->
                _uiState.update { it.copy(history = records) }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    // ------------------------------------------------------------- navigation

    fun selectSection(section: AppSection) {
        _uiState.update { it.copy(section = section) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    // ------------------------------------------------------------------- tabs

    fun selectTab(tabId: String) {
        if (_uiState.value.tabs.none { it.id == tabId }) return
        _uiState.update { it.copy(activeTabId = tabId) }
        schedulePersist()
    }

    fun newTab() {
        val tab = EditorTab.create(title = "Untitled ${untitledCounter++}.js")
        _uiState.update {
            it.copy(tabs = it.tabs + tab, activeTabId = tab.id, section = AppSection.EDITOR)
        }
        schedulePersist()
    }

    fun closeTab(tabId: String) {
        _uiState.update { state ->
            val remaining = state.tabs.filterNot { it.id == tabId }
            val nextActive = when {
                remaining.isEmpty() -> null
                state.activeTabId == tabId -> {
                    val index = state.tabs.indexOfFirst { it.id == tabId }
                    remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))?.id
                }
                else -> state.activeTabId
            }
            state.copy(tabs = remaining, activeTabId = nextActive)
        }
        schedulePersist()
    }

    fun updateTabContent(tabId: String, content: String) {
        _uiState.update { state ->
            val tabs = state.tabs.map { tab ->
                if (tab.id == tabId && tab.content != content) {
                    tab.copy(content = content, dirty = true)
                } else {
                    tab
                }
            }
            state.copy(tabs = tabs)
        }
        schedulePersist()
    }

    fun renameTab(tabId: String, newTitle: String) {
        val title = newTitle.trim()
        if (title.isEmpty()) {
            showMessage("Tab name is required")
            return
        }
        val tab = _uiState.value.tabs.firstOrNull { it.id == tabId } ?: return
        if (tab.linkedScriptId == null) {
            _uiState.update { state ->
                state.copy(tabs = state.tabs.map {
                    if (it.id == tabId) it.copy(title = title) else it
                })
            }
            schedulePersist()
            return
        }
        viewModelScope.launch {
            runCatching {
                scriptRepository.rename(tab.linkedScriptId, title.removeSuffix(".js"))
            }.onSuccess { script ->
                if (script == null) {
                    _uiState.update { state ->
                        state.copy(tabs = state.tabs.map {
                            if (it.id == tabId) {
                                it.copy(title = title, linkedScriptId = null, dirty = true)
                            } else {
                                it
                            }
                        })
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(tabs = state.tabs.map {
                            if (it.id == tabId) it.copy(title = script.name) else it
                        })
                    }
                }
                schedulePersist()
            }.onFailure { error ->
                showMessage(error.message ?: "Unable to rename tab")
            }
        }
    }

    // ---------------------------------------------------------------- scripts

    fun openScript(scriptId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val existing = state.tabs.firstOrNull { it.linkedScriptId == scriptId }
            if (existing != null) {
                _uiState.update { it.copy(activeTabId = existing.id, section = AppSection.EDITOR) }
                schedulePersist()
                return@launch
            }
            val script = runCatching { scriptRepository.load(scriptId) }.getOrNull()
            if (script == null) {
                showMessage("Script no longer exists")
                scriptRepository.refresh()
                return@launch
            }
            val current = _uiState.value
            val reusable = current.tabs.firstOrNull {
                it.linkedScriptId == null && !it.dirty && it.content.isBlank()
            }
            val opened = EditorTab.create(
                title = script.name,
                content = script.content,
                linkedScriptId = script.id
            )
            _uiState.update {
                if (reusable != null) {
                    it.copy(
                        tabs = it.tabs.map { tab ->
                            if (tab.id == reusable.id) {
                                opened.copy(id = reusable.id)
                            } else {
                                tab
                            }
                        },
                        activeTabId = reusable.id,
                        section = AppSection.EDITOR
                    )
                } else {
                    it.copy(
                        tabs = it.tabs + opened,
                        activeTabId = opened.id,
                        section = AppSection.EDITOR
                    )
                }
            }
            schedulePersist()
        }
    }

    fun createScript(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) {
            showMessage("Script name is required")
            return
        }
        viewModelScope.launch {
            runCatching { scriptRepository.create(clean, "") }
                .onSuccess { script -> openScript(script.id) }
                .onFailure { showMessage(it.message ?: "Unable to create script") }
        }
    }

    fun saveActiveTab() {
        val tab = _uiState.value.activeTab ?: return
        viewModelScope.launch {
            val saved = runCatching {
                val linkedId = tab.linkedScriptId
                if (linkedId != null) {
                    scriptRepository.update(linkedId, tab.content)
                        ?: scriptRepository.create(tab.title, tab.content)
                } else {
                    scriptRepository.create(tab.title, tab.content)
                }
            }.getOrElse { error ->
                showMessage(error.message ?: "Unable to save script")
                null
            } ?: return@launch

            _uiState.update { state ->
                state.copy(
                    tabs = state.tabs.map { current ->
                        if (current.id == tab.id) {
                            current.copy(
                                title = saved.name,
                                linkedScriptId = saved.id,
                                dirty = false
                            )
                        } else {
                            current
                        }
                    }
                )
            }
            showMessage("Saved ${saved.name}")
            schedulePersist()
        }
    }

    fun renameScript(scriptId: String, newName: String) {
        val name = newName.trim()
        if (name.isEmpty()) {
            showMessage("Script name is required")
            return
        }
        viewModelScope.launch {
            runCatching { scriptRepository.rename(scriptId, name) }
                .onSuccess { script ->
                    if (script != null) {
                        _uiState.update { state ->
                            state.copy(tabs = state.tabs.map { tab ->
                                if (tab.linkedScriptId == scriptId) {
                                    tab.copy(title = script.name)
                                } else {
                                    tab
                                }
                            })
                        }
                        schedulePersist()
                        showMessage("Renamed to ${script.name}")
                    }
                }
                .onFailure { error ->
                    showMessage(error.message ?: "Unable to rename script")
                }
        }
    }

    fun deleteScript(scriptId: String) {
        viewModelScope.launch {
            runCatching { scriptRepository.delete(scriptId) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(tabs = state.tabs.map { tab ->
                            if (tab.linkedScriptId == scriptId) {
                                tab.copy(linkedScriptId = null, dirty = true)
                            } else {
                                tab
                            }
                        })
                    }
                    schedulePersist()
                    showMessage("Script deleted. Open tabs keep their content.")
                }
                .onFailure { error ->
                    showMessage(error.message ?: "Unable to delete script")
                }
        }
    }

    // -------------------------------------------------------------- execution

    fun execute() {
        val state = _uiState.value
        val tab = state.activeTab
        if (tab == null) {
            showMessage("No open editor tab")
            return
        }
        if (tab.content.isBlank()) {
            showMessage("Nothing to execute")
            return
        }
        val timeout = state.settings.executionTimeoutSeconds * 1000L
        val startedAt = System.currentTimeMillis()
        val accepted = executionManager.execute(
            request = ExecutionRequest(
                source = tab.content,
                scriptName = tab.title,
                scriptId = tab.linkedScriptId
            ),
            timeoutMillis = timeout
        ) { outcome ->
            val record = ExecutionRecord(
                id = UUID.randomUUID().toString(),
                scriptId = tab.linkedScriptId,
                scriptName = tab.title,
                source = tab.content,
                executedAt = startedAt,
                durationMillis = outcome.durationMillis,
                status = outcome.status,
                errorMessage = outcome.errorMessage
            )
            viewModelScope.launch {
                runCatching { historyRepository.add(record) }
            }
        }
        if (!accepted) {
            showMessage("Execution already running")
        } else {
            _uiState.update { it.copy(section = AppSection.EDITOR) }
        }
    }

    fun stop() {
        executionManager.stop()
    }

    fun clearConsole() {
        consoleManager.clear()
    }

    // ---------------------------------------------------------------- history

    fun reopenHistory(record: ExecutionRecord) {
        val tab = EditorTab.create(
            title = "${record.scriptName} (history)",
            content = record.source,
            linkedScriptId = record.scriptId,
            dirty = true
        )
        _uiState.update {
            it.copy(
                tabs = it.tabs + tab,
                activeTabId = tab.id,
                section = AppSection.EDITOR
            )
        }
        schedulePersist()
    }

    fun deleteHistory(recordId: String) {
        viewModelScope.launch {
            runCatching { historyRepository.delete(recordId) }
                .onFailure { showMessage(it.message ?: "Unable to delete history entry") }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            runCatching { historyRepository.clear() }
                .onFailure { showMessage(it.message ?: "Unable to clear history") }
        }
    }

    // --------------------------------------------------------------- settings

    fun updateSettings(transform: (SettingsData) -> SettingsData) {
        settingsRepository.update(transform)
    }

    // ---------------------------------------------------------------- helpers

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private fun schedulePersist() {
        if (!_uiState.value.tabsReady) return
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(350)
            persistNow()
        }
    }

    private suspend fun persistNow() {
        val state = _uiState.value
        if (!state.tabsReady) return
        runCatching {
            tabsRepository.save(
                com.aether.executor.data.model.TabsSnapshot(
                    activeTabId = state.activeTabId,
                    tabs = state.tabs
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        executionManager.shutdown()
    }

    companion object {
        val DEFAULT_SCRIPT = listOf(
            "// Aether Executor - QuickJS sandbox",
            "// Only console.* is exposed to scripts.",
            "",
            "const values = [10, 20, 30];",
            "const total = values.reduce((sum, value) => sum + value, 0);",
            "",
            "console.log('Aether Executor initialized');",
            "console.info('Values:', values.join(', '));",
            "console.log('Total:', total);",
            "console.warn('This runtime is running locally.');"
        ).joinToString("\n")
    }
}
