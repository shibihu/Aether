package com.aether.executor.data.repository

import android.content.Context
import com.aether.executor.core.execution.ExecutionStatus
import com.aether.executor.data.model.ExecutionRecord
import com.aether.executor.data.storage.AtomicJsonStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persistent execution history. Every completed, failed or stopped run is
 * stored with its source so it can be reopened in the editor. The newest
 * [MAX_RECORDS] entries are kept.
 */
interface HistoryRepository {
    val records: StateFlow<List<ExecutionRecord>>

    suspend fun refresh()
    suspend fun add(record: ExecutionRecord)
    suspend fun delete(id: String)
    suspend fun clear()
}

class FileHistoryRepository(context: Context) : HistoryRepository {

    private val file = File(context.filesDir, "history.json")
    private val mutex = Mutex()

    private val _records = MutableStateFlow<List<ExecutionRecord>>(emptyList())
    override val records: StateFlow<List<ExecutionRecord>> = _records.asStateFlow()

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val loaded = readRecords()
            _records.value = loaded
        }
    }

    override suspend fun add(record: ExecutionRecord) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = (listOf(record) + readRecords())
                .distinctBy { it.id }
                .take(MAX_RECORDS)
            writeRecords(updated)
            _records.value = updated
        }
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = readRecords().filterNot { it.id == id }
            writeRecords(updated)
            _records.value = updated
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            writeRecords(emptyList())
            _records.value = emptyList()
        }
    }

    private fun readRecords(): List<ExecutionRecord> {
        val array = AtomicJsonStore.readArray(file) ?: return emptyList()
        val result = ArrayList<ExecutionRecord>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val status = runCatching {
                ExecutionStatus.valueOf(item.optString("status", "FAILED"))
            }.getOrDefault(ExecutionStatus.FAILED)
            result.add(
                ExecutionRecord(
                    id = item.optString("id", ""),
                    scriptId = item.optString("scriptId").takeIf { it.isNotBlank() },
                    scriptName = item.optString("scriptName", "Untitled"),
                    source = item.optString("source", ""),
                    executedAt = item.optLong("executedAt", 0L),
                    durationMillis = item.optLong("durationMillis", 0L),
                    status = status,
                    errorMessage = item.optString("errorMessage").takeIf { it.isNotBlank() }
                )
            )
        }
        return result
            .filter { it.id.isNotBlank() }
            .sortedByDescending { it.executedAt }
    }

    private fun writeRecords(records: List<ExecutionRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("id", record.id)
                    .put("scriptId", record.scriptId ?: "")
                    .put("scriptName", record.scriptName)
                    .put("source", record.source)
                    .put("executedAt", record.executedAt)
                    .put("durationMillis", record.durationMillis)
                    .put("status", record.status.name)
                    .put("errorMessage", record.errorMessage ?: "")
            )
        }
        AtomicJsonStore.writeText(file, array.toString())
    }

    companion object {
        const val MAX_RECORDS = 100
    }
}
