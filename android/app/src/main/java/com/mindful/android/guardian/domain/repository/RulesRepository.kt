package com.mindful.android.guardian.domain.repository

import com.mindful.android.guardian.domain.model.AppRule
import com.mindful.android.guardian.domain.model.BlockEvent
import com.mindful.android.guardian.domain.model.KeywordRule
import com.mindful.android.guardian.domain.model.ScheduleRule
import kotlinx.coroutines.flow.Flow

interface RulesRepository {
    // App Rules
    fun observeAppRules(): Flow<List<AppRule>>
    suspend fun blockedPackages(): List<String>
    suspend fun whitelistPackages(): List<String>
    suspend fun upsertAppRule(rule: AppRule)
    suspend fun deleteAppRule(pkg: String)

    // Keywords
    fun observeKeywords(): Flow<List<KeywordRule>>
    suspend fun enabledKeywords(): List<KeywordRule>
    suspend fun upsertKeyword(rule: KeywordRule): Long
    suspend fun deleteKeyword(id: Long)
    suspend fun clearKeywords()

    // Block Events
    fun observeRecentEvents(limit: Int = 50): Flow<List<BlockEvent>>
    suspend fun allBlockEvents(): List<BlockEvent>
    suspend fun insertBlockEvent(pkg: String, reason: String, detail: String?)
    suspend fun clearBlockEvents()

    // Schedule Rules
    fun observeScheduleRules(): Flow<List<ScheduleRule>>
    suspend fun allSchedules(): List<ScheduleRule>
    suspend fun upsertScheduleRule(rule: ScheduleRule)
    suspend fun deleteScheduleRule(pkg: String)
}
