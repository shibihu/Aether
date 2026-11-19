package com.aether.executor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.executor.core.execution.ExecutionStatus
import com.aether.executor.core.runtime.LogLevel
import com.aether.executor.core.runtime.QuickJsExecutor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ConsoleEntry(
    val timestamp: String,
    val level: LogLevel,
    val message: String
)

data class AetherUiState(
    val source: String = DEFAULT_SCRIPT,
    val status: ExecutionStatus = ExecutionStatus.IDLE,
    val console: List<ConsoleEntry> = emptyList()
) {
    companion object {
        val DEFAULT_SCRIPT = listOf(
            "// Aether Executor - M1 QuickJS test",
            "console.log('Aether Executor initialized');",
            "",
            "const values = [10, 20, 30];",
            "const total = values.reduce((sum, value) => sum + value, 0);",
            "",
            "console.info('Values:', values.join(', '));",
            "console.log('Total:', total);",
            "console.warn('M1 runtime is running locally.');"
        ).joinToString("\n")
    }
}

class AetherViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AetherUiState())
    val uiState: StateFlow<AetherUiState> = _uiState.asStateFlow()

    private var executionJob: Job? = null
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun updateSource(value: String) {
        _uiState.update { it.copy(source = value) }
    }

    fun execute() {
        if (executionJob?.isActive == true) return

        val source = _uiState.value.source
        _uiState.update { it.copy(status = ExecutionStatus.RUNNING, console = emptyList()) }
        append(LogLevel.INFO, "Starting QuickJS runtime")

        executionJob = viewModelScope.launch {
            try {
                QuickJsExecutor(::append).execute(source)
                _uiState.update { it.copy(status = ExecutionStatus.COMPLETED) }
                append(LogLevel.INFO, "Execution completed")
            } catch (cancelled: CancellationException) {
                _uiState.update { it.copy(status = ExecutionStatus.STOPPED) }
                append(LogLevel.WARNING, "Execution stopped")
                throw cancelled
            } catch (error: Throwable) {
                _uiState.update { it.copy(status = ExecutionStatus.FAILED) }
                append(LogLevel.ERROR, error.message ?: error::class.java.simpleName)
            }
        }
    }

    fun stop() {
        if (executionJob?.isActive == true) {
            executionJob?.cancel()
            _uiState.update { it.copy(status = ExecutionStatus.STOPPED) }
            append(LogLevel.WARNING, "Stop requested")
        }
    }

    fun clearConsole() {
        _uiState.update { it.copy(console = emptyList()) }
    }

    private fun append(level: LogLevel, message: String) {
        val entry = ConsoleEntry(LocalTime.now().format(timeFormat), level, message)
        _uiState.update { state ->
            state.copy(console = (state.console + entry).takeLast(300))
        }
    }
}
