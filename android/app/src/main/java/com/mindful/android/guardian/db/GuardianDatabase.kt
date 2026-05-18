package com.mindful.android.guardian.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppRuleEntity::class,
        KeywordRuleEntity::class,
        BlockEventEntity::class,
        ScheduleRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun appRuleDao(): AppRuleDao
    abstract fun keywordDao(): KeywordDao
    abstract fun blockEventDao(): BlockEventDao
    abstract fun scheduleRuleDao(): ScheduleRuleDao

    companion object {
        const val DB_NAME = "guardian.db"
    }
}
