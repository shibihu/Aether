package com.aether.executor.core.execution

import android.os.SystemClock
import com.aether.executor.core.console.ConsoleManager
import com.aether.executor.core.runtime.LogLevel
import com.aether.executor.core.runtime.QuickJsExecutor
import com.dokar.quickjs.QuickJsException
import com.dokar.quickjs.QuickJsInterruptedException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

data class ExecutionRequest(
    val source: String,
    val scriptName: String,
    val scriptId: String?
)

data class ExecutionOutcome(
    val executionId: Long,
    val status: ExecutionStatus,
    val durationMillis: Long,
    val errorMessage: String?
)

/**
 * Single-owner execution coordinator. Only one QuickJS runtime is allowed at a
 * time, Stop genuinely interrupts the native interrupt handler, and all
 * resources are released when the run ends.
 */
class ExecutionManager(
    private val console: ConsoleManager,
    private val scope: CoroutineScope
) {

    private val _status = MutableStateFlow(ExecutionStatus.IDLE)
    val status: StateFlow<ExecutionStatus> = _status.asStateFlow()

    private val _activeExecutionId = MutableStateFlow<Long?>(null)
    val activeExecutionId: StateFlow<Long?> = _activeExecutionId.asStateFlow()

    private val stopRequested = AtomicBoolean(false)

    @Volatile
    private var runtime: QuickJsExecutor? = null

    @Volatile
    private var executionJob: Job? = null

    fun execute(
        request: ExecutionRequest,
        timeoutMillis: Long,
        onFinished: (ExecutionOutcome) -> Unit
    ): Boolean {
        if (_status.value.isActive || executionJob?.isActive == true) {
            return false
        }

        stopRequested.set(false)
        val executionId = console.beginExecution()
        _activeExecutionId.value = executionId
        _status.value = ExecutionStatus.RUNNING
        console.append(executionId, LogLevel.INFO, "QuickJS runtime started")

        val startedAt = SystemClock.elapsedRealtime()
        val job = scope.launch(Dispatchers.Default) {
            val executor = QuickJsExecutor { level, message ->
                console.append(executionId, level, message)
            }
            runtime = executor

            var finalStatus = ExecutionStatus.COMPLETED
            var errorMessage: String? = null

            try {
                executor.execute(request.source, timeoutMillis)
            } catch (cancelled: CancellationException) {
                finalStatus = ExecutionStatus.STOPPED
                errorMessage = "Execution stopped by user"
            } catch (interrupted: QuickJsInterruptedException) {
                if (stopRequested.get()) {
                    finalStatus = ExecutionStatus.STOPPED
                    errorMessage = "Execution stopped by user"
                } else {
                    finalStatus = ExecutionStatus.FAILED
                    errorMessage =
                        "Execution timed out after ${timeoutMillis / 1000.0} seconds"
                    console.append(executionId, LogLevel.ERROR, errorMessage)
                }
            } catch (error: Throwable) {
                finalStatus = ExecutionStatus.FAILED
                errorMessage = formatError(error)
                console.append(executionId, LogLevel.ERROR, errorMessage)
            } finally {
                executor.close()
                runtime = null
                if (finalStatus == ExecutionStatus.STOPPED) {
                    console.append(executionId, LogLevel.WARN, "Execution interrupted")
                } else if (finalStatus == ExecutionStatus.COMPLETED) {
                    console.append(executionId, LogLevel.INFO, "Execution completed")
                }
                console.endExecution(executionId)
                _activeExecutionId.value = null
                _status.value = finalStatus
                val outcome = ExecutionOutcome(
                    executionId = executionId,
                    status = finalStatus,
                    durationMillis = SystemClock.elapsedRealtime() - startedAt,
                    errorMessage = errorMessage
                )
                withContext(NonCancellable) {
                    runCatching { onFinished(outcome) }
                }
                executionJob = null
            }
        }
        executionJob = job
        return true
    }

    fun stop() {
        if (!_status.value.isActive) return
        if (!stopRequested.compareAndSet(false, true)) return

        _status.value = ExecutionStatus.STOPPING
        runtime?.let { executor ->
            console.activeExecutionId.value?.let {
                console.append(it, LogLevel.WARN, "Stop requested")
            }
            executor.interrupt()
        }
        executionJob?.cancel()
    }

    fun shutdown() {
        stop()
        runtime?.close()
        runtime = null
    }

    private fun formatError(error: Throwable): String {
        if (error is QuickJsException) {
            val location = buildString {
                error.lineNumber?.let { append("line $it") }
                error.columnNumber?.let {
                    if (isNotEmpty()) append(", ")
                    append("column $it")
                }
            }
            return buildString {
                append(error.message ?: "JavaScript error")
                if (location.isNotEmpty()) append(" ($location)")
                val stack = error.stack
                if (!stack.isNullOrBlank() && !stack.contains(error.message ?: "")) {
                    append("\n").append(stack)
                }
            }
        }
        return error.message ?: error::class.java.simpleName
    }
}
