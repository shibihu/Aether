package com.aether.executor.core.execution

enum class ExecutionStatus {
    IDLE,
    RUNNING,
    STOPPING,
    COMPLETED,
    STOPPED,
    FAILED;

    val isActive: Boolean
        get() = this == RUNNING || this == STOPPING
}
