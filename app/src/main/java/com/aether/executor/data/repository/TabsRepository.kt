package com.aether.executor.data.repository

import android.content.Context
import com.aether.executor.data.model.EditorTab
import com.aether.executor.data.model.TabsSnapshot
import com.aether.executor.data.storage.AtomicJsonStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Restores open tabs after process death. Unsaved drafts are written verbatim,
 * so a crash or a low-memory kill never loses editor content.
 */
class TabsRepository(context: Context) {

    private val file = File(context.filesDir, "tabs.json")

    suspend fun load(): TabsSnapshot? = withContext(Dispatchers.IO) {
        val root = AtomicJsonStore.readObject(file) ?: return@withContext null
        val array = root.optJSONArray("tabs") ?: return@withContext null
        val tabs = ArrayList<EditorTab>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id", "").ifBlank { continue }
            tabs.add(
                EditorTab(
                    id = id,
                    title = item.optString("title", "Untitled"),
                    content = item.optString("content", ""),
                    linkedScriptId = item.optString("linkedScriptId")
                        .takeIf { it.isNotBlank() },
                    dirty = item.optBoolean("dirty", false)
                )
            )
        }
        if (tabs.isEmpty()) return@withContext null
        val active = root.optString("activeTabId").takeIf { candidate ->
            tabs.any { it.id == candidate }
        }
        TabsSnapshot(activeTabId = active ?: tabs.first().id, tabs = tabs)
    }

    suspend fun save(snapshot: TabsSnapshot) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        snapshot.tabs.forEach { tab ->
            array.put(
                JSONObject()
                    .put("id", tab.id)
                    .put("title", tab.title)
                    .put("content", tab.content)
                    .put("linkedScriptId", tab.linkedScriptId ?: "")
                    .put("dirty", tab.dirty)
            )
        }
        val document = JSONObject()
            .put("version", 1)
            .put("activeTabId", snapshot.activeTabId ?: "")
            .put("tabs", array)
        AtomicJsonStore.writeText(file, document.toString())
    }
}
