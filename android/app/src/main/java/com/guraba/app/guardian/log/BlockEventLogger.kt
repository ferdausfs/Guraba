package com.guraba.app.guardian.log

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BlockEvent(
    val timestamp: Long,
    val packageName: String,
    val reason: String,
    val detail: String
)

/**
 * Append-only JSONL log of block events for the Guardian filter.
 * Kept intentionally small (no Room): rotates at MAX_EVENTS and supports CSV export.
 */
class BlockEventLogger(private val context: Context) {

    private val mutex = Mutex()
    private val logFile by lazy { File(context.filesDir, LOG_FILE) }

    suspend fun log(event: BlockEvent) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val o = JSONObject().apply {
                    put("ts", event.timestamp)
                    put("pkg", event.packageName)
                    put("reason", event.reason)
                    put("detail", event.detail)
                }
                logFile.appendText(o.toString() + "\n")
                trimIfNeeded()
            } catch (_: Throwable) { }
        }
    }

    suspend fun readAll(limit: Int = 500): List<BlockEvent> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!logFile.exists()) return@withLock emptyList()
            val lines = logFile.readLines().takeLast(limit)
            lines.mapNotNull { line ->
                try {
                    val o = JSONObject(line)
                    BlockEvent(
                        timestamp = o.optLong("ts"),
                        packageName = o.optString("pkg"),
                        reason = o.optString("reason"),
                        detail = o.optString("detail")
                    )
                } catch (_: Throwable) { null }
            }
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock { logFile.delete() }
    }

    suspend fun exportCsv(): File = withContext(Dispatchers.IO) {
        val events = readAll(limit = MAX_EVENTS)
        val csv = File(context.cacheDir, "guraba_block_events.csv")
        csv.bufferedWriter().use { w ->
            w.appendLine("timestamp,package,reason,detail")
            for (e in events) {
                val esc = { s: String -> "\"" + s.replace("\"", "\"\"") + "\"" }
                w.append(e.timestamp.toString()).append(',')
                    .append(esc(e.packageName)).append(',')
                    .append(esc(e.reason)).append(',')
                    .append(esc(e.detail)).append('\n')
            }
        }
        csv
    }

    suspend fun readAllAsJson(): JSONArray = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        for (e in readAll(limit = MAX_EVENTS)) {
            arr.put(JSONObject().apply {
                put("timestamp", e.timestamp)
                put("packageName", e.packageName)
                put("reason", e.reason)
                put("detail", e.detail)
            })
        }
        arr
    }

    private fun trimIfNeeded() {
        try {
            if (!logFile.exists()) return
            val lines = logFile.readLines()
            if (lines.size > MAX_EVENTS) {
                logFile.writeText(lines.takeLast(MAX_EVENTS).joinToString("\n") + "\n")
            }
        } catch (_: Throwable) { }
    }

    companion object {
        private const val LOG_FILE = "guardian_block_events.jsonl"
        private const val MAX_EVENTS = 5_000
    }
}
