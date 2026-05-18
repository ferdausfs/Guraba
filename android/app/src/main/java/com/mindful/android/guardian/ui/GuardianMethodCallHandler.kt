package com.mindful.android.guardian.ui

import android.content.Context
import com.mindful.android.guardian.blocker.ContentGuardService
import com.mindful.android.guardian.blocker.TempBlockManager
import com.mindful.android.guardian.data.GuardianPreferences
import com.mindful.android.guardian.detection.PinManager
import com.mindful.android.guardian.domain.model.AppRule
import com.mindful.android.guardian.domain.model.KeywordRule
import com.mindful.android.guardian.domain.model.ScheduleRule
import com.mindful.android.guardian.domain.repository.RulesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuardianMethodCallHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: RulesRepository,
    private val prefs: GuardianPreferences,
    private val pinManager: PinManager,
    private val tempBlockManager: TempBlockManager
) : MethodChannel.MethodCallHandler {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {

            // ── Service ────────────────────────────────────────────────────────
            "startGuardian" -> {
                ContentGuardService.start(context)
                result.success(true)
            }
            "stopGuardian" -> {
                ContentGuardService.stop(context)
                result.success(true)
            }

            // ── Prefs ──────────────────────────────────────────────────────────
            "setProtectionEnabled" -> ioScope.launch {
                runCatching { prefs.setProtectionEnabled(call.argument<Boolean>("enabled") ?: true) }
                result.success(true)
            }
            "setAiDetection" -> ioScope.launch {
                runCatching { prefs.setAiDetection(call.argument<Boolean>("enabled") ?: false) }
                result.success(true)
            }
            "setUserGender" -> ioScope.launch {
                runCatching { prefs.setUserGender(call.argument<String>("gender") ?: "NONE") }
                result.success(true)
            }
            "setKeywordFilter" -> ioScope.launch {
                runCatching { prefs.setKeywordFilter(call.argument<Boolean>("enabled") ?: true) }
                result.success(true)
            }
            "setTempBlockDuration" -> ioScope.launch {
                runCatching { prefs.setTempBlockDurationMins(call.argument<Int>("minutes") ?: 15) }
                result.success(true)
            }
            "setAiThreshold" -> ioScope.launch {
                val v = (call.argument<Double>("value") ?: 0.65).toFloat()
                runCatching { prefs.setAiThreshold(v) }
                result.success(true)
            }
            "setNsfwGateThreshold" -> ioScope.launch {
                val v = (call.argument<Double>("value") ?: 0.60).toFloat()
                runCatching { prefs.setNsfwGateThreshold(v) }
                result.success(true)
            }
            "setGenderThreshold" -> ioScope.launch {
                val v = (call.argument<Double>("value") ?: 0.70).toFloat()
                runCatching { prefs.setGenderThreshold(v) }
                result.success(true)
            }
            "setGridVoteCount" -> ioScope.launch {
                runCatching { prefs.setGridVoteCount(call.argument<Int>("count") ?: 2) }
                result.success(true)
            }

            // ── Keywords ───────────────────────────────────────────────────────
            "addKeyword" -> ioScope.launch {
                try {
                    val json = JSONObject(call.argument<String>("json") ?: "{}")
                    val rule = KeywordRule(
                        id = 0,
                        keyword = json.getString("keyword"),
                        isRegex = json.optBoolean("isRegex", false),
                        severity = json.optInt("severity", 1),
                        enabled = true
                    )
                    val id = repo.upsertKeyword(rule)
                    prefs.bumpRulesVersion()
                    result.success(id)
                } catch (t: Throwable) { Timber.e(t); result.error("ADD_KW", t.message, null) }
            }
            "removeKeyword" -> ioScope.launch {
                try {
                    repo.deleteKeyword(call.argument<Int>("id")?.toLong() ?: 0L)
                    prefs.bumpRulesVersion()
                    result.success(true)
                } catch (t: Throwable) { result.error("DEL_KW", t.message, null) }
            }

            // ── App Rules ─────────────────────────────────────────────────────
            "blockApp" -> ioScope.launch {
                try {
                    val json = JSONObject(call.argument<String>("json") ?: "{}")
                    repo.upsertAppRule(
                        AppRule(
                            packageName = json.getString("packageName"),
                            appName = json.optString("appName", json.getString("packageName")),
                            isBlocked = true,
                            isWhitelisted = false,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    prefs.bumpRulesVersion()
                    result.success(true)
                } catch (t: Throwable) { result.error("BLOCK_APP", t.message, null) }
            }
            "unblockApp" -> ioScope.launch {
                try {
                    repo.deleteAppRule(call.argument<String>("packageName") ?: "")
                    prefs.bumpRulesVersion()
                    result.success(true)
                } catch (t: Throwable) { result.error("UNBLOCK_APP", t.message, null) }
            }
            "whitelistApp" -> ioScope.launch {
                try {
                    val json = JSONObject(call.argument<String>("json") ?: "{}")
                    repo.upsertAppRule(
                        AppRule(
                            packageName = json.getString("packageName"),
                            appName = json.optString("appName", json.getString("packageName")),
                            isBlocked = false,
                            isWhitelisted = true,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    prefs.bumpRulesVersion()
                    result.success(true)
                } catch (t: Throwable) { result.error("WHITELIST", t.message, null) }
            }
            "removeWhitelist" -> ioScope.launch {
                try {
                    repo.deleteAppRule(call.argument<String>("packageName") ?: "")
                    prefs.bumpRulesVersion()
                    result.success(true)
                } catch (t: Throwable) { result.error("RM_WHITE", t.message, null) }
            }

            // ── Schedule Rules ────────────────────────────────────────────────
            "setSchedule" -> ioScope.launch {
                try {
                    val json = JSONObject(call.argument<String>("json") ?: "{}")
                    val daysArray = json.optJSONArray("enabledDays")
                    val days = if (daysArray != null) {
                        (0 until daysArray.length()).map { daysArray.getInt(it) }.toSet()
                    } else (0..6).toSet()

                    repo.upsertScheduleRule(
                        ScheduleRule(
                            packageName = json.getString("packageName"),
                            startHour = json.optInt("startHour", 22),
                            startMinute = json.optInt("startMinute", 0),
                            endHour = json.optInt("endHour", 6),
                            endMinute = json.optInt("endMinute", 0),
                            enabledDays = days,
                            enabled = json.optBoolean("enabled", true),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    prefs.bumpRulesVersion()
                    result.success(true)
                } catch (t: Throwable) { result.error("SET_SCHED", t.message, null) }
            }
            "removeSchedule" -> ioScope.launch {
                try {
                    repo.deleteScheduleRule(call.argument<String>("packageName") ?: "")
                    prefs.bumpRulesVersion()
                    result.success(true)
                } catch (t: Throwable) { result.error("RM_SCHED", t.message, null) }
            }

            // ── Block Events ──────────────────────────────────────────────────
            "getBlockEvents" -> ioScope.launch {
                try {
                    val events = repo.allBlockEvents()
                    val arr = JSONArray()
                    events.forEach { e ->
                        arr.put(JSONObject().apply {
                            put("id", e.id)
                            put("packageName", e.packageName)
                            put("reason", e.reason.name)
                            put("matchedTerm", e.matchedTerm ?: "")
                            put("timestamp", e.timestamp)
                        })
                    }
                    result.success(arr.toString())
                } catch (t: Throwable) { result.error("GET_EVENTS", t.message, null) }
            }
            "clearBlockEvents" -> ioScope.launch {
                try { repo.clearBlockEvents(); result.success(true) }
                catch (t: Throwable) { result.error("CLEAR_EVENTS", t.message, null) }
            }

            // ── PIN ───────────────────────────────────────────────────────────
            "setPinEnabled" -> {
                val pin = call.argument<String>("pin") ?: ""
                val ok = pinManager.setPin(pin)
                result.success(ok)
            }
            "verifyPin" -> {
                val pin = call.argument<String>("pin") ?: ""
                val res = when (val v = pinManager.verifyPin(pin)) {
                    is PinManager.VerifyResult.Success         -> "SUCCESS"
                    is PinManager.VerifyResult.Wrong           -> "WRONG:${v.remainingAttempts}"
                    is PinManager.VerifyResult.LockedOut       -> "LOCKED:${v.msRemaining}"
                    is PinManager.VerifyResult.NotSet          -> "NOT_SET"
                }
                result.success(res)
            }
            "clearPin" -> {
                pinManager.clearPin()
                result.success(true)
            }

            else -> result.notImplemented()
        }
    }
}
