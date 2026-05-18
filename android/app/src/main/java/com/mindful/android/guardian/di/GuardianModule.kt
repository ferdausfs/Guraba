package com.mindful.android.guardian.di

import android.content.Context
import androidx.room.Room
import com.mindful.android.guardian.db.AppRuleDao
import com.mindful.android.guardian.db.BlockEventDao
import com.mindful.android.guardian.db.GuardianDatabase
import com.mindful.android.guardian.db.KeywordDao
import com.mindful.android.guardian.db.ScheduleRuleDao
import com.mindful.android.guardian.domain.repository.RulesRepository
import com.mindful.android.guardian.domain.repository.RulesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GuardianDatabaseModule {

    @Provides
    @Singleton
    fun provideGuardianDatabase(@ApplicationContext ctx: Context): GuardianDatabase =
        Room.databaseBuilder(ctx, GuardianDatabase::class.java, GuardianDatabase.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun appRuleDao(db: GuardianDatabase): AppRuleDao = db.appRuleDao()
    @Provides fun keywordDao(db: GuardianDatabase): KeywordDao = db.keywordDao()
    @Provides fun blockEventDao(db: GuardianDatabase): BlockEventDao = db.blockEventDao()
    @Provides fun scheduleRuleDao(db: GuardianDatabase): ScheduleRuleDao = db.scheduleRuleDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class GuardianRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRulesRepository(impl: RulesRepositoryImpl): RulesRepository
}
