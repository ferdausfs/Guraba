package com.guraba.app.guardian.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.guardianDataStore by preferencesDataStore(name = "guardian_prefs")

/**
 * DataStore-backed preferences for the Guardian module.
 * All thresholds are live-observable via Flow.
 */
class GuardianPreferences(private val context: Context) {

    private val ds get() = context.guardianDataStore

    val aiDetection: Flow<Boolean> = ds.data.map { it[KEY_AI_ENABLED] ?: false }
    val userGender: Flow<String>   = ds.data.map { it[KEY_USER_GENDER] ?: "NONE" }
    val aiThreshold: Flow<Float>   = ds.data.map { it[KEY_AI_THRESHOLD] ?: 0.65f }
    val nsfwGateThreshold: Flow<Float> = ds.data.map { it[KEY_NSFW_GATE] ?: 0.60f }
    val genderThreshold: Flow<Float>   = ds.data.map { it[KEY_GENDER_THRESHOLD] ?: 0.70f }
    val gridVoteCount: Flow<Int>       = ds.data.map { it[KEY_GRID_VOTES] ?: 2 }
    val keywordFilterEnabled: Flow<Boolean> = ds.data.map { it[KEY_KW_ENABLED] ?: true }

    suspend fun setAiDetection(v: Boolean) = ds.edit { it[KEY_AI_ENABLED] = v }
    suspend fun setUserGender(v: String)   = ds.edit { it[KEY_USER_GENDER] = v }
    suspend fun setAiThreshold(v: Float)   = ds.edit { it[KEY_AI_THRESHOLD] = v.coerceIn(0f, 1f) }
    suspend fun setNsfwGateThreshold(v: Float) = ds.edit { it[KEY_NSFW_GATE] = v.coerceIn(0f, 1f) }
    suspend fun setGenderThreshold(v: Float)   = ds.edit { it[KEY_GENDER_THRESHOLD] = v.coerceIn(0f, 1f) }
    suspend fun setGridVoteCount(v: Int)       = ds.edit { it[KEY_GRID_VOTES] = v.coerceIn(1, 6) }
    suspend fun setKeywordFilterEnabled(v: Boolean) = ds.edit { it[KEY_KW_ENABLED] = v }

    companion object {
        private val KEY_AI_ENABLED       = booleanPreferencesKey("ai_enabled")
        private val KEY_USER_GENDER      = stringPreferencesKey("user_gender")
        private val KEY_AI_THRESHOLD     = floatPreferencesKey("ai_threshold")
        private val KEY_NSFW_GATE        = floatPreferencesKey("nsfw_gate_threshold")
        private val KEY_GENDER_THRESHOLD = floatPreferencesKey("gender_threshold")
        private val KEY_GRID_VOTES       = intPreferencesKey("grid_vote_count")
        private val KEY_KW_ENABLED       = booleanPreferencesKey("kw_filter_enabled")
    }
}
