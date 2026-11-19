package com.aether.executor.core.runtime

/**
 * Console levels exposed to JavaScript through the Aether bridge.
 *
 * The native bridge deliberately supports only these console methods. No
 * filesystem, network, reflection, or Android APIs are exposed to scripts.
 */
enum class LogLevel {
    LOG,
    INFO,
    WARN,
    ERROR
}
