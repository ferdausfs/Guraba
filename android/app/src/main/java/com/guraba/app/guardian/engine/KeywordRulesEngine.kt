package com.guraba.app.guardian.engine

import android.content.Context
import com.guraba.app.guardian.prefs.GuardianPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.io.File

data class KeywordRule(val keyword: String, val isRegex: Boolean, val enabled: Boolean = true)

sealed class TextMatch {
    data object None : TextMatch()
    data class Hit(val rule: String) : TextMatch()
}

/**
 * Lightweight, on-device keyword & regex matcher used by the accessibility-driven
 * content filter. Backed by a JSON file in filesDir so users can edit rules
 * from the Flutter UI without a Room migration.
 *
 * Ported & simplified from Guardian Shield's RulesEngine.
 */
class KeywordRulesEngine(
    private val context: Context,
    private val prefs: GuardianPreferences
) {
    private val mutex = Mutex()
    private val rulesFile by lazy { File(context.filesDir, RULES_FILE) }

    @Volatile private var enabled: Boolean = true

    private val _rules = MutableStateFlow<List<KeywordRule>>(emptyList())
    val rules: StateFlow<List<KeywordRule>> = _rules.asStateFlow()

    fun startPrefsCache(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) { prefs.keywordFilterEnabled.collect { enabled = it } }
        scope.launch(Dispatchers.IO) { reload() }
    }

    suspend fun reload() {
        mutex.withLock {
            _rules.value = try {
                if (!rulesFile.exists()) emptyList() else {
                    val arr = JSONArray(rulesFile.readText())
                    (0 until arr.length()).mapNotNull { i ->
                        val o = arr.optJSONObject(i) ?: return@mapNotNull null
                        val kw = o.optString("keyword").ifBlank { null } ?: return@mapNotNull null
                        KeywordRule(
                            keyword = kw,
                            isRegex = o.optBoolean("isRegex", false),
                            enabled = o.optBoolean("enabled", true)
                        )
                    }
                }
            } catch (_: Throwable) { emptyList() }
        }
    }

    suspend fun setRules(rules: List<KeywordRule>) {
        mutex.withLock {
            val arr = JSONArray()
            for (r in rules) {
                val o = org.json.JSONObject()
                o.put("keyword", r.keyword)
                o.put("isRegex", r.isRegex)
                o.put("enabled", r.enabled)
                arr.put(o)
            }
            rulesFile.writeText(arr.toString())
            _rules.value = rules
        }
    }

    fun evaluateText(text: String): TextMatch {
        if (!enabled || text.isBlank() || text.length < 10) return TextMatch.None
        for (r in _rules.value) {
            if (!r.enabled) continue
            val kw = r.keyword
            try {
                if (r.isRegex) {
                    val rx = Regex(kw, RegexOption.IGNORE_CASE)
                    if (rx.containsMatchIn(text)) return TextMatch.Hit(kw)
                } else {
                    val matched = if (kw.length < 4) {
                        Regex("\\b${Regex.escape(kw)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
                    } else text.contains(kw, ignoreCase = true)
                    if (matched) return TextMatch.Hit(kw)
                }
            } catch (_: Throwable) { /* invalid regex – skip */ }
        }
        return TextMatch.None
    }

    companion object {
        private const val RULES_FILE = "guardian_keyword_rules.json"
    }
}
