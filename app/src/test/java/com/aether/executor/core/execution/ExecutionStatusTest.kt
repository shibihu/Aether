package com.aether.executor.core.execution

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionStatusTest {

    @Test
    fun onlyRunningAndStoppingAreActive() {
        assertTrue(ExecutionStatus.RUNNING.isActive)
        assertTrue(ExecutionStatus.STOPPING.isActive)
        ExecutionStatus.entries
            .filterNot { it == ExecutionStatus.RUNNING || it == ExecutionStatus.STOPPING }
            .forEach { assertFalse("$it must be inactive", it.isActive) }
    }
}
