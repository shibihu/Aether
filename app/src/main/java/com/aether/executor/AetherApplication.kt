package com.aether.executor

import android.app.Application
import com.aether.executor.data.repository.FileHistoryRepository
import com.aether.executor.data.repository.FileScriptRepository
import com.aether.executor.data.repository.HistoryRepository
import com.aether.executor.data.repository.ScriptRepository
import com.aether.executor.data.repository.SettingsRepository
import com.aether.executor.data.repository.TabsRepository

/**
 * Holds process-wide repositories. All state is app-private and local-only.
 */
class AetherApplication : Application() {

    val scriptRepository: ScriptRepository by lazy { FileScriptRepository(this) }
    val historyRepository: HistoryRepository by lazy { FileHistoryRepository(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val tabsRepository: TabsRepository by lazy { TabsRepository(this) }
}
