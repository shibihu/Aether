package com.aether.executor.data.repository

import android.content.Context
import com.aether.executor.data.model.ScriptFile
import com.aether.executor.data.model.ScriptSummary
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
import java.util.UUID

/**
 * App-private .js storage. Script bodies live in filesDir/scripts/<uuid>.js and
 * display metadata lives in an atomically replaced index. The index is
 * reconciled with the directory on load so a lost index never hides scripts.
 */
class FileScriptRepository(context: Context) : ScriptRepository {

    private val root = File(context.filesDir, "scripts")
    private val indexFile = File(root, "index.json")
    private val mutex = Mutex()

    private val _scripts = MutableStateFlow<List<ScriptSummary>>(emptyList())
    override val scripts: StateFlow<List<ScriptSummary>> = _scripts.asStateFlow()

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val entries = loadAndReconcile()
            writeIndex(entries)
            publish(entries)
        }
    }

    override suspend fun load(id: String): ScriptFile? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val entries = loadAndReconcile()
            val entry = entries.firstOrNull { it.id == id } ?: return@withLock null
            val file = fileFor(entry.id)
            if (!file.isFile) {
                entries.removeAll { it.id == id }
                writeIndex(entries)
                publish(entries)
                return@withLock null
            }
            ScriptFile(
                id = entry.id,
                name = entry.name,
                content = file.readText(Charsets.UTF_8),
                createdAt = entry.createdAt,
                updatedAt = entry.updatedAt
            )
        }
    }

    override suspend fun create(name: String, content: String): ScriptFile =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                root.mkdirs()
                val entries = loadAndReconcile()
                val normalized = normalizeName(name)
                val unique = uniqueName(normalized, entries)
                val id = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val entry = Entry(id, unique, now, now)
                fileFor(id).writeText(content, Charsets.UTF_8)
                entries.add(entry)
                writeIndex(entries)
                publish(entries)
                ScriptFile(id, unique, content, now, now)
            }
        }

    override suspend fun update(id: String, content: String): ScriptFile? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val entries = loadAndReconcile()
                val index = entries.indexOfFirst { it.id == id }
                if (index < 0) return@withLock null
                val now = System.currentTimeMillis()
                val updated = entries[index].copy(updatedAt = now)
                entries[index] = updated
                fileFor(id).writeText(content, Charsets.UTF_8)
                writeIndex(entries)
                publish(entries)
                ScriptFile(id, updated.name, content, updated.createdAt, updated.updatedAt)
            }
        }

    override suspend fun rename(id: String, name: String): ScriptFile? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val entries = loadAndReconcile()
                val index = entries.indexOfFirst { it.id == id }
                if (index < 0) return@withLock null
                val normalized = normalizeName(name)
                val duplicate = entries.any {
                    it.id != id && it.name.equals(normalized, ignoreCase = true)
                }
                require(!duplicate) { "A script named \"$normalized\" already exists" }
                val now = System.currentTimeMillis()
                val updated = entries[index].copy(name = normalized, updatedAt = now)
                entries[index] = updated
                writeIndex(entries)
                publish(entries)
                ScriptFile(
                    id = id,
                    name = normalized,
                    content = fileFor(id).readText(Charsets.UTF_8),
                    createdAt = updated.createdAt,
                    updatedAt = updated.updatedAt
                )
            }
        }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val entries = loadAndReconcile()
            val removed = entries.removeAll { it.id == id }
            if (removed) {
                fileFor(id).delete()
                writeIndex(entries)
                publish(entries)
            }
        }
    }

    private fun loadAndReconcile(): MutableList<Entry> {
        root.mkdirs()
        val entries = mutableListOf<Entry>()
        val seen = mutableSetOf<String>()

        val array = AtomicJsonStore.readObject(indexFile)?.optJSONArray(KEY_SCRIPTS)
        if (array != null) {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optString("id", "")
                if (!ID_PATTERN.matches(id) || !seen.add(id)) continue
                val file = fileFor(id)
                if (!file.isFile) continue
                val name = item.optString("name", id).ifBlank { id }
                entries.add(
                    Entry(
                        id = id,
                        name = name,
                        createdAt = item.optLong("createdAt", file.lastModified()),
                        updatedAt = item.optLong("updatedAt", file.lastModified())
                    )
                )
            }
        }

        root.listFiles { file -> file.isFile && file.name.endsWith(".js") }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                val id = file.nameWithoutExtension
                if (!ID_PATTERN.matches(id) || !seen.add(id)) return@forEach
                val modified = file.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis()
                entries.add(Entry(id, id, modified, modified))
            }

        return entries.sortedByDescending { it.updatedAt }.toMutableList()
    }

    private fun writeIndex(entries: List<Entry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("name", entry.name)
                    .put("createdAt", entry.createdAt)
                    .put("updatedAt", entry.updatedAt)
            )
        }
        val document = JSONObject()
            .put("version", 1)
            .put(KEY_SCRIPTS, array)
        AtomicJsonStore.writeText(indexFile, document.toString())
    }

    private fun publish(entries: List<Entry>) {
        _scripts.value = entries.map { entry ->
            ScriptSummary(
                id = entry.id,
                name = entry.name,
                createdAt = entry.createdAt,
                updatedAt = entry.updatedAt,
                sizeBytes = fileFor(entry.id).length()
            )
        }
    }

    private fun fileFor(id: String) = File(root, "$id.js")

    private fun uniqueName(desired: String, entries: List<Entry>): String {
        if (entries.none { it.name.equals(desired, ignoreCase = true) }) return desired
        var index = 2
        while (true) {
            val candidate = "$desired ($index)"
            if (entries.none { it.name.equals(candidate, ignoreCase = true) }) {
                return candidate
            }
            index++
        }
    }

    private fun normalizeName(raw: String): String {
        val trimmed = raw.trim()
        val withoutExtension = if (trimmed.endsWith(".js", ignoreCase = true)) {
            trimmed.dropLast(3)
        } else {
            trimmed
        }
        val cleaned = withoutExtension
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "-")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '.')
            .take(MAX_NAME_LENGTH)
            .trim()
        require(cleaned.isNotBlank()) { "Script name is required" }
        return cleaned
    }

    private data class Entry(
        val id: String,
        val name: String,
        val createdAt: Long,
        val updatedAt: Long
    )

    companion object {
        private const val KEY_SCRIPTS = "scripts"
        private const val MAX_NAME_LENGTH = 64
        private val ID_PATTERN = Regex("[A-Za-z0-9-]{1,64}")
    }
}
