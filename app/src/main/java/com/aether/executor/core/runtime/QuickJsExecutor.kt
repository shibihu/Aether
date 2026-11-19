package com.aether.executor.core.runtime

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.define
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.evaluate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin, lifecycle-safe wrapper around one QuickJS instance.
 *
 * M1 behaviour is preserved: only the console bridge is exposed and every
 * execution owns an isolated runtime. M2 adds cooperative interruption through
 * [interrupt], bounded memory, and explicit resource release.
 */
class QuickJsExecutor(
    private val onLog: (LogLevel, String) -> Unit
) {

    @Volatile
    private var runtime: QuickJs? = null

    @Volatile
    private var closed = false

    @Volatile
    var isActive: Boolean = false
        private set

    suspend fun execute(
        source: String,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MS
    ) = withContext(Dispatchers.Default) {
        if (closed) throw IllegalStateException("Runtime has already been closed")

        val js = QuickJs.create(Dispatchers.Default)
        runtime = js
        try {
            js.evaluationTimeoutMillis = timeoutMillis.coerceIn(MIN_TIMEOUT_MS, MAX_TIMEOUT_MS)
            js.memoryLimit = MAX_MEMORY_BYTES
            js.define("console") {
                function("log") { args -> emit(LogLevel.LOG, args) }
                function("debug") { args -> emit(LogLevel.LOG, args) }
                function("info") { args -> emit(LogLevel.INFO, args) }
                function("warn") { args -> emit(LogLevel.WARN, args) }
                function("error") { args -> emit(LogLevel.ERROR, args) }
            }
            isActive = true
            js.evaluate<Unit>(source, filename = SCRIPT_FILENAME)
        } finally {
            isActive = false
            closed = true
            runtime = null
            runCatching { js.close() }
        }
    }

    /**
     * Requests interruption of the JavaScript interrupt handler. This is safe
     * to call from the main thread while [execute] is running on a worker.
     */
    fun interrupt() {
        val js = runtime ?: return
        runCatching {
            if (!js.isClosed) js.interruptEvaluation()
        }
    }

    fun close() {
        closed = true
        val js = runtime
        runtime = null
        runCatching { js?.close() }
    }

    private fun emit(level: LogLevel, args: Array<Any?>) {
        onLog(level, args.joinToString(" ") { it.toDisplayString() })
    }

    private fun Any?.toDisplayString(): String = when (this) {
        null -> "null"
        is String -> this
        is Array<*> -> joinToString(", ", "[", "]") { it.toDisplayString() }
        else -> toString()
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 10_000L
        const val MIN_TIMEOUT_MS = 1_000L
        const val MAX_TIMEOUT_MS = 60_000L
        private const val MAX_MEMORY_BYTES = 64L * 1024L * 1024L
        private const val SCRIPT_FILENAME = "aether-script.js"
    }
}
