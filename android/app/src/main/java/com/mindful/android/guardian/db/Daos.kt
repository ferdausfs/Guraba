package com.mindful.android.guardian.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM g_app_rules ORDER BY appName ASC")
    fun observeAll(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM g_app_rules")
    suspend fun getAll(): List<AppRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AppRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<AppRuleEntity>)

    @Query("DELETE FROM g_app_rules WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("SELECT packageName FROM g_app_rules WHERE isBlocked = 1")
    suspend fun blockedPackages(): List<String>

    @Query("SELECT packageName FROM g_app_rules WHERE isWhitelisted = 1")
    suspend fun whitelistPackages(): List<String>
}

@Dao
interface KeywordDao {
    @Query("SELECT * FROM g_keyword_rules ORDER BY id DESC")
    fun observeAll(): Flow<List<KeywordRuleEntity>>

    @Query("SELECT * FROM g_keyword_rules WHERE enabled = 1")
    suspend fun getEnabled(): List<KeywordRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: KeywordRuleEntity): Long

    @Query("DELETE FROM g_keyword_rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM g_keyword_rules")
    suspend fun clear()
}

@Dao
interface BlockEventDao {
    @Query("SELECT * FROM g_block_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<BlockEventEntity>>

    @Query("SELECT * FROM g_block_events ORDER BY timestamp DESC")
    suspend fun getAll(): List<BlockEventEntity>

    @Query("SELECT COUNT(*) FROM g_block_events WHERE timestamp >= :since")
    fun countSinceFlow(since: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: BlockEventEntity): Long

    @Query("DELETE FROM g_block_events")
    suspend fun clear()
}

@Dao
interface ScheduleRuleDao {
    @Query("SELECT * FROM g_schedule_rules ORDER BY packageName ASC")
    fun observeAll(): Flow<List<ScheduleRuleEntity>>

    @Query("SELECT * FROM g_schedule_rules")
    suspend fun getAll(): List<ScheduleRuleEntity>

    @Query("SELECT * FROM g_schedule_rules WHERE packageName = :pkg LIMIT 1")
    suspend fun get(pkg: String): ScheduleRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: ScheduleRuleEntity)

    @Query("DELETE FROM g_schedule_rules WHERE packageName = :pkg")
    suspend fun delete(pkg: String)
}
