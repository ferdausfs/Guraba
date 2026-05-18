package com.mindful.android.guardian.detection

import android.content.Context
import com.mindful.android.guardian.domain.model.BlockReason
import com.mindful.android.guardian.domain.model.DetectionResult
import com.mindful.android.guardian.domain.model.ScheduleRule
import com.mindful.android.guardian.domain.repository.RulesRepository
import com.mindful.android.guardian.util.GuardianAppClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class RulesSnapshot(
    val blocked: Set<String> = emptySet(),
    val whitelist: Set<String> = emptySet(),
    val keywords: List<Pair<String, Boolean>> = emptyList(),
    val inputMethods: Set<String> = emptySet(),
    val scheduleRules: Map<String, ScheduleRule> = emptyMap()
)

@Singleton
class RulesEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: RulesRepository
) {
    private val mutex = Mutex()
    @Volatile private var snapshot: RulesSnapshot = RulesSnapshot()
    private val _rulesChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val rulesChanged: SharedFlow<Unit> = _rulesChanged.asSharedFlow()
    private val ownPkg: String by lazy { context.packageName }

    suspend fun reload() {
        mutex.withLock {
            try {
                val blocked = repo.blockedPackages().toSet()
                val whitelist = repo.whitelistPackages().toSet()
                val kws = repo.enabledKeywords().map { it.keyword to it.isRegex }
                val imes = GuardianAppClassifier.loadInputMethodPackages(context)
                val sched = repo.allSchedules().associateBy { it.packageName }
                snapshot = RulesSnapshot(blocked, whitelist, kws, imes, sched)
                _rulesChanged.tryEmit(Unit)
            } catch (t: Throwable) { Timber.e(t, "Failed to reload rules") }
        }
    }

    fun current() = snapshot

    fun canBlock(pkg: String): Boolean {
        val s = snapshot
        if (GuardianAppClassifier.isAlwaysAllowedPackage(ownPkg, pkg, s.inputMethods)) return false
        if (s.whitelist.contains(pkg)) return false
        return true
    }

    fun evaluatePackage(pkg: String): DetectionResult {
        val s = snapshot
        if (GuardianAppClassifier.isAlwaysAllowedPackage(ownPkg, pkg, s.inputMethods)) return DetectionResult.Allow
        if (s.whitelist.contains(pkg)) return DetectionResult.Allow
        if (s.blocked.contains(pkg)) return DetectionResult.Block(BlockReason.APP_BLOCKED, pkg)
        if (isScheduleBlocked(pkg)) return DetectionResult.Block(BlockReason.SCHEDULE_BLOCKED, pkg)
        return DetectionResult.Allow
    }

    fun evaluateText(text: String): DetectionResult {
        if (text.isBlank() || text.length < 10) return DetectionResult.Allow
        val s = snapshot
        for ((kw, isRegex) in s.keywords) {
            try {
                val matched = if (isRegex) {
                    Regex(kw, RegexOption.IGNORE_CASE).containsMatchIn(text)
                } else if (kw.length < 4) {
                    Regex("\\b${Regex.escape(kw)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
                } else {
                    text.contains(kw, ignoreCase = true)
                }
                if (matched) return DetectionResult.Block(BlockReason.KEYWORD_MATCH, kw)
            } catch (_: Throwable) { /* invalid regex */ }
        }
        return DetectionResult.Allow
    }

    private fun isScheduleBlocked(pkg: String): Boolean {
        val rule = snapshot.scheduleRules[pkg] ?: return false
        if (!rule.enabled) return false
        val cal = Calendar.getInstance()
        val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
        if (!rule.enabledDays.contains(dayOfWeek)) return false
        val nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = rule.startHour * 60 + rule.startMinute
        val end = rule.endHour * 60 + rule.endMinute
        return if (start <= end) nowMins in start until end else nowMins >= start || nowMins < end
    }
}
