package com.aether.executor.core.runtime

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.define
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.evaluate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * M1 runtime. Executes JavaScript in an isolated QuickJS instance.
 * Only console methods are exposed; no Android, file, network, reflection,
 * or cross-process APIs are available to scripts.
 */
class QuickJsExecutor(
    private val onLog: (LogLevel, String) -> Unit
) {
    suspend fun execute(source: String) = withContext(Dispatchers.Default) {
        val runtime = QuickJs.create(Dispatchers.Default)
        try {
            runtime.evaluationTimeoutMillis = DEFAULT_TIMEOUT_MS
            runtime.define("console") {
                function("log") { args ->
                    onLog(LogLevel.INFO, args.joinToString(" ") { it.toDisplayString() })
                }
                function("info") { args ->
                    onLog(LogLevel.INFO, args.joinToString(" ") { it.toDisplayString() })
                }
                function("warn") { args ->
                    onLog(LogLevel.WARNING, args.joinToString(" ") { it.toDisplayString() })
                }
                function("error") { args ->
                    onLog(LogLevel.ERROR, args.joinToString(" ") { it.toDisplayString() })
                }
            }
            runtime.evaluate<Unit>(source, filename = "aether-script.js")
        } finally {
            runtime.close()
        }
    }

    private fun Any?.toDisplayString(): String = when (this) {
        null -> "null"
        is String -> this
        else -> toString()
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 10_000L
    }
}

enum class LogLevel { INFO, WARNING, ERROR }
