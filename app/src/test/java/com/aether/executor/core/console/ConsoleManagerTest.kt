package com.aether.executor.core.console

import com.aether.executor.core.runtime.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsoleManagerTest {

    @Test
    fun beginExecutionClearsConsoleAndSetsActiveId() {
        val manager = ConsoleManager()
        val first = manager.beginExecution()
        manager.append(first, LogLevel.LOG, "one")
        assertEquals(1, manager.entries.value.size)

        val second = manager.beginExecution()
        assertTrue(second > first)
        assertEquals(second, manager.activeExecutionId.value)
        assertTrue(manager.entries.value.isEmpty())
    }

    @Test
    fun staleExecutionCannotWriteToNewExecution() {
        val manager = ConsoleManager()
        val first = manager.beginExecution()
        manager.endExecution(first)
        val second = manager.beginExecution()

        manager.append(first, LogLevel.ERROR, "late callback")
        assertTrue(manager.entries.value.isEmpty())

        manager.append(second, LogLevel.INFO, "current")
        assertEquals(1, manager.entries.value.size)
        assertEquals("current", manager.entries.value.first().message)
        assertEquals(LogLevel.INFO, manager.entries.value.first().level)
    }

    @Test
    fun endedExecutionRejectsCallbacks() {
        val manager = ConsoleManager()
        val execution = manager.beginExecution()
        manager.endExecution(execution)
        assertFalse(manager.activeExecutionId.value == execution)

        manager.append(execution, LogLevel.WARN, "after end")
        assertTrue(manager.entries.value.isEmpty())
    }

    @Test
    fun entriesAreCapped() {
        val manager = ConsoleManager()
        val execution = manager.beginExecution()
        repeat(ConsoleManager.MAX_ENTRIES + 25) { index ->
            manager.append(execution, LogLevel.LOG, "line $index")
        }
        assertEquals(ConsoleManager.MAX_ENTRIES, manager.entries.value.size)
        assertEquals(
            "line ${ConsoleManager.MAX_ENTRIES + 24}",
            manager.entries.value.last().message
        )
    }

    @Test
    fun clearKeepsActiveExecution() {
        val manager = ConsoleManager()
        val execution = manager.beginExecution()
        manager.append(execution, LogLevel.LOG, "visible")
        manager.clear()
        assertTrue(manager.entries.value.isEmpty())

        manager.append(execution, LogLevel.LOG, "still accepted")
        assertEquals(1, manager.entries.value.size)
    }
}
