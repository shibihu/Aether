package com.aether.executor.data.repository

import com.aether.executor.data.model.ScriptFile
import com.aether.executor.data.model.ScriptSummary
import kotlinx.coroutines.flow.StateFlow

interface ScriptRepository {
    val scripts: StateFlow<List<ScriptSummary>>

    suspend fun refresh()
    suspend fun load(id: String): ScriptFile?
    suspend fun create(name: String, content: String): ScriptFile
    suspend fun update(id: String, content: String): ScriptFile?
    suspend fun rename(id: String, name: String): ScriptFile?
    suspend fun delete(id: String)
}
