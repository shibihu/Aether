package com.aether.executor.core.console

import com.aether.executor.core.runtime.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

data class ConsoleEntry(
    val id: Long,
    val executionId: Long,
    val timestampMillis: Long,
    val level: LogLevel,
    val message: String
)

/**
 * Owns the console buffer and associates every entry with the execution that
 * produced it. Late callbacks from a stopped runtime are discarded.
 */
class ConsoleManager {

    private val lock = Any()
    private val nextEntryId = AtomicLong(0)
    private val nextExecutionId = AtomicLong(0)

    private val _entries = MutableStateFlow<List<ConsoleEntry>>(emptyList())
    val entries: StateFlow<List<ConsoleEntry>> = _entries.asStateFlow()

    private val _activeExecutionId = MutableStateFlow<Long?>(null)
    val activeExecutionId: StateFlow<Long?> = _activeExecutionId.asStateFlow()

    fun beginExecution(): Long = synchronized(lock) {
        val id = nextExecutionId.incrementAndGet()
        _activeExecutionId.value = id
        _entries.value = emptyList()
        id
    }

    fun append(executionId: Long, level: LogLevel, message: String) {
        synchronized(lock) {
            if (_activeExecutionId.value != executionId) return
            val entry = ConsoleEntry(
                id = nextEntryId.incrementAndGet(),
                executionId = executionId,
                timestampMillis = System.currentTimeMillis(),
                level = level,
                message = message
            )
            _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
        }
    }

    fun endExecution(executionId: Long) {
        synchronized(lock) {
            if (_activeExecutionId.value == executionId) {
                _activeExecutionId.value = null
            }
        }
    }

    /** Clears visible output without ending the active execution. */
    fun clear() {
        synchronized(lock) {
            _entries.value = emptyList()
        }
    }

    companion object {
        const val MAX_ENTRIES = 500
    }
}
