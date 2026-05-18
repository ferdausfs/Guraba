package com.mindful.android.guardian.db

import com.mindful.android.guardian.domain.model.AppRule
import com.mindful.android.guardian.domain.model.BlockEvent
import com.mindful.android.guardian.domain.model.BlockReason
import com.mindful.android.guardian.domain.model.KeywordRule
import com.mindful.android.guardian.domain.model.ScheduleRule

// AppRule
fun AppRuleEntity.toDomain() = AppRule(packageName, appName, isBlocked, isWhitelisted, createdAt)
fun AppRule.toEntity() = AppRuleEntity(packageName, appName, isBlocked, isWhitelisted, createdAt)

// KeywordRule
fun KeywordRuleEntity.toDomain() = KeywordRule(id, keyword, isRegex, severity, enabled)
fun KeywordRule.toEntity() = KeywordRuleEntity(id, keyword, isRegex, severity, enabled)

// BlockEvent
fun BlockEventEntity.toDomain() = BlockEvent(
    id = id,
    packageName = packageName,
    reason = runCatching { BlockReason.valueOf(reason) }.getOrDefault(BlockReason.MANUAL),
    matchedTerm = matchedTerm,
    timestamp = timestamp
)

// ScheduleRule
fun ScheduleRuleEntity.toDomain() = ScheduleRule(
    packageName = packageName,
    startHour = startHour,
    startMinute = startMinute,
    endHour = endHour,
    endMinute = endMinute,
    enabledDays = decodeDaysMask(enabledDaysMask),
    enabled = enabled,
    createdAt = createdAt
)
fun ScheduleRule.toEntity() = ScheduleRuleEntity(
    packageName = packageName,
    startHour = startHour,
    startMinute = startMinute,
    endHour = endHour,
    endMinute = endMinute,
    enabledDaysMask = encodeDaysMask(enabledDays),
    enabled = enabled,
    createdAt = createdAt
)

private fun encodeDaysMask(days: Set<Int>): Int = days.fold(0) { acc, d -> acc or (1 shl d) }
private fun decodeDaysMask(mask: Int): Set<Int> = (0..6).filter { mask and (1 shl it) != 0 }.toSet()
