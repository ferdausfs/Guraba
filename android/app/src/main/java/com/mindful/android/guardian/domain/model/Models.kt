package com.mindful.android.guardian.domain.model

// ── Block Reason ──────────────────────────────────────────────────────────────
enum class BlockReason { APP_BLOCKED, KEYWORD_MATCH, AI_DETECTION, MANUAL, SCHEDULE_BLOCKED }

// ── Detection Result ──────────────────────────────────────────────────────────
sealed class DetectionResult {
    data object Allow : DetectionResult()
    data class Block(val reason: BlockReason, val detail: String) : DetectionResult()
}

// ── Domain Models ─────────────────────────────────────────────────────────────
data class AppRule(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean,
    val isWhitelisted: Boolean,
    val createdAt: Long
)

data class BlockEvent(
    val id: Long,
    val packageName: String,
    val reason: BlockReason,
    val matchedTerm: String?,
    val timestamp: Long
)

data class KeywordRule(
    val id: Long,
    val keyword: String,
    val isRegex: Boolean,
    val severity: Int,
    val enabled: Boolean
)

data class ScheduleRule(
    val packageName: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val enabledDays: Set<Int> = (0..6).toSet(),
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
