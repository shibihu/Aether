package com.aether.executor.data.repository

import android.content.Context
import com.aether.executor.data.model.SettingsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Device-local editor and execution preferences backed by SharedPreferences.
 */
class SettingsRepository(context: Context) {

    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<SettingsData> = _settings.asStateFlow()

    fun update(transform: (SettingsData) -> SettingsData) {
        val updated = sanitize(transform(_settings.value))
        preferences.edit()
            .putFloat(KEY_FONT_SIZE, updated.fontSizeSp)
            .putInt(KEY_TIMEOUT, updated.executionTimeoutSeconds)
            .putInt(KEY_TAB_SIZE, updated.tabSize)
            .putBoolean(KEY_WORD_WRAP, updated.wordWrap)
            .putBoolean(KEY_LINE_NUMBERS, updated.lineNumbers)
            .apply()
        _settings.value = updated
    }

    private fun read(): SettingsData {
        val defaults = SettingsData()
        return sanitize(
            SettingsData(
                fontSizeSp = preferences.getFloat(KEY_FONT_SIZE, defaults.fontSizeSp),
                executionTimeoutSeconds = preferences.getInt(
                    KEY_TIMEOUT,
                    defaults.executionTimeoutSeconds
                ),
                tabSize = preferences.getInt(KEY_TAB_SIZE, defaults.tabSize),
                wordWrap = preferences.getBoolean(KEY_WORD_WRAP, defaults.wordWrap),
                lineNumbers = preferences.getBoolean(KEY_LINE_NUMBERS, defaults.lineNumbers)
            )
        )
    }

    private fun sanitize(value: SettingsData) = value.copy(
        fontSizeSp = value.fontSizeSp.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE),
        executionTimeoutSeconds = value.executionTimeoutSeconds.coerceIn(MIN_TIMEOUT, MAX_TIMEOUT),
        tabSize = value.tabSize.coerceIn(MIN_TAB_SIZE, MAX_TAB_SIZE)
    )

    companion object {
        private const val PREFERENCES_NAME = "aether_settings"
        private const val KEY_FONT_SIZE = "font_size_sp"
        private const val KEY_TIMEOUT = "execution_timeout_seconds"
        private const val KEY_TAB_SIZE = "tab_size"
        private const val KEY_WORD_WRAP = "word_wrap"
        private const val KEY_LINE_NUMBERS = "line_numbers"

        const val MIN_FONT_SIZE = 10f
        const val MAX_FONT_SIZE = 26f
        const val MIN_TIMEOUT = 1
        const val MAX_TIMEOUT = 60
        const val MIN_TAB_SIZE = 2
        const val MAX_TAB_SIZE = 8
    }
}
