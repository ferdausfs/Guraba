package com.mindful.android.guardian.domain.repository

import com.mindful.android.guardian.db.AppRuleDao
import com.mindful.android.guardian.db.BlockEventDao
import com.mindful.android.guardian.db.BlockEventEntity
import com.mindful.android.guardian.db.KeywordDao
import com.mindful.android.guardian.db.ScheduleRuleDao
import com.mindful.android.guardian.db.toDomain
import com.mindful.android.guardian.db.toEntity
import com.mindful.android.guardian.domain.model.AppRule
import com.mindful.android.guardian.domain.model.BlockEvent
import com.mindful.android.guardian.domain.model.KeywordRule
import com.mindful.android.guardian.domain.model.ScheduleRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RulesRepositoryImpl @Inject constructor(
    private val appRuleDao: AppRuleDao,
    private val keywordDao: KeywordDao,
    private val blockEventDao: BlockEventDao,
    private val scheduleRuleDao: ScheduleRuleDao
) : RulesRepository {

    override fun observeAppRules(): Flow<List<AppRule>> =
        appRuleDao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun blockedPackages(): List<String> = appRuleDao.blockedPackages()
    override suspend fun whitelistPackages(): List<String> = appRuleDao.whitelistPackages()
    override suspend fun upsertAppRule(rule: AppRule) = appRuleDao.upsert(rule.toEntity())
    override suspend fun deleteAppRule(pkg: String) = appRuleDao.delete(pkg)

    override fun observeKeywords(): Flow<List<KeywordRule>> =
        keywordDao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun enabledKeywords(): List<KeywordRule> =
        keywordDao.getEnabled().map { it.toDomain() }

    override suspend fun upsertKeyword(rule: KeywordRule): Long =
        keywordDao.upsert(rule.toEntity())

    override suspend fun deleteKeyword(id: Long) = keywordDao.delete(id)
    override suspend fun clearKeywords() = keywordDao.clear()

    override fun observeRecentEvents(limit: Int): Flow<List<BlockEvent>> =
        blockEventDao.observeRecent(limit).map { it.map { e -> e.toDomain() } }

    override suspend fun allBlockEvents(): List<BlockEvent> =
        blockEventDao.getAll().map { it.toDomain() }

    override suspend fun insertBlockEvent(pkg: String, reason: String, detail: String?) {
        blockEventDao.insert(
            BlockEventEntity(
                packageName = pkg,
                reason = reason,
                matchedTerm = detail,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearBlockEvents() = blockEventDao.clear()

    override fun observeScheduleRules(): Flow<List<ScheduleRule>> =
        scheduleRuleDao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun allSchedules(): List<ScheduleRule> =
        scheduleRuleDao.getAll().map { it.toDomain() }

    override suspend fun upsertScheduleRule(rule: ScheduleRule) =
        scheduleRuleDao.upsert(rule.toEntity())

    override suspend fun deleteScheduleRule(pkg: String) = scheduleRuleDao.delete(pkg)
}
