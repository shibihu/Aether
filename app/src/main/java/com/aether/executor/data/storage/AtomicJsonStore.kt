package com.aether.executor.data.storage

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

internal object AtomicJsonStore {

    fun writeText(file: File, text: String) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        FileOutputStream(temp).use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
            output.flush()
            output.fd.sync()
        }
        if (file.exists() && !file.delete()) {
            temp.delete()
            error("Unable to replace ${file.name}")
        }
        if (!temp.renameTo(file)) {
            file.writeText(text, Charsets.UTF_8)
            temp.delete()
        }
    }

    fun readText(file: File): String? {
        if (!file.isFile) return null
        return runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
    }

    fun readArray(file: File): JSONArray? {
        val text = readText(file) ?: return null
        return runCatching { JSONArray(text) }.getOrNull()
    }

    fun readObject(file: File): JSONObject? {
        val text = readText(file) ?: return null
        return runCatching { JSONObject(text) }.getOrNull()
    }
}
