package com.guraba.app.guardian.bridge

import android.content.Context
import android.net.Uri
import com.guraba.app.guardian.GuardianModule
import com.guraba.app.guardian.engine.KeywordRulesEngine
import com.guraba.app.guardian.engine.KeywordRule
import com.guraba.app.guardian.model.ModelImportManager
import com.guraba.app.guardian.pin.PinManager
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * MethodChannel handler that exposes the Guardian module to the Flutter UI.
 * Channel name: "guraba.guardian"
 */
class GuardianMethodHandler(
    private val context: Context
) : MethodChannel.MethodCallHandler {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            GuardianModule.init(context)
        } catch (t: Throwable) {
            result.error("INIT_FAILED", t.message, null); return
        }

        when (call.method) {
            // ─── PIN ────────────────────────────────────────────
            "isPinSet"  -> result.success(GuardianModule.pinManager.isPinSet())
            "setPin"    -> {
                val pin = call.argument<String>("pin") ?: ""
                result.success(GuardianModule.pinManager.setPin(pin))
            }
            "verifyPin" -> {
                val pin = call.argument<String>("pin") ?: ""
                when (val r = GuardianModule.pinManager.verifyPin(pin)) {
                    is PinManager.VerifyResult.Success ->
                        result.success(mapOf("status" to "success"))
                    is PinManager.VerifyResult.Wrong ->
                        result.success(mapOf("status" to "wrong", "remaining" to r.remainingAttempts))
                    is PinManager.VerifyResult.LockedOut ->
                        result.success(mapOf("status" to "locked", "msRemaining" to r.msRemaining))
                    is PinManager.VerifyResult.NotSet ->
                        result.success(mapOf("status" to "not_set"))
                }
            }
            "clearPin"  -> { GuardianModule.pinManager.clearPin(); result.success(true) }

            // ─── AI prefs / thresholds ──────────────────────────
            "getAiSettings" -> result.success(
                mapOf(
                    "aiEnabled"         to GuardianModule.aiDetector.cachedAiEnabled,
                    "userGender"        to GuardianModule.aiDetector.cachedUserGender,
                    "aiThreshold"       to GuardianModule.aiDetector.cachedThreshold,
                    "nsfwGateThreshold" to GuardianModule.aiDetector.cachedNsfwGateThreshold,
                    "genderThreshold"   to GuardianModule.aiDetector.cachedGenderThreshold,
                    "gridVoteCount"     to GuardianModule.aiDetector.cachedGridVoteCount
                )
            )
            "setAiEnabled"        -> setPref(call, "value", result) { GuardianModule.preferences.setAiDetection(it as Boolean) }
            "setUserGender"       -> setPref(call, "value", result) { GuardianModule.preferences.setUserGender(it as String) }
            "setAiThreshold"      -> setPref(call, "value", result) { GuardianModule.preferences.setAiThreshold((it as Number).toFloat()) }
            "setNsfwGateThreshold"-> setPref(call, "value", result) { GuardianModule.preferences.setNsfwGateThreshold((it as Number).toFloat()) }
            "setGenderThreshold"  -> setPref(call, "value", result) { GuardianModule.preferences.setGenderThreshold((it as Number).toFloat()) }
            "setGridVoteCount"    -> setPref(call, "value", result) { GuardianModule.preferences.setGridVoteCount((it as Number).toInt()) }

            // ─── Model management ───────────────────────────────
            "getModelStatus" -> result.success(
                mapOf(
                    "legacyImported"  to GuardianModule.modelImporter.isModelImported(ModelImportManager.MODEL_LEGACY),
                    "legacyBytes"     to GuardianModule.modelImporter.modelSizeBytes(ModelImportManager.MODEL_LEGACY),
                    "nsfwImported"    to GuardianModule.modelImporter.isModelImported(ModelImportManager.MODEL_NSFW),
                    "nsfwBytes"       to GuardianModule.modelImporter.modelSizeBytes(ModelImportManager.MODEL_NSFW),
                    "genderImported"  to GuardianModule.modelImporter.isModelImported(ModelImportManager.MODEL_GENDER),
                    "genderBytes"     to GuardianModule.modelImporter.modelSizeBytes(ModelImportManager.MODEL_GENDER)
                )
            )
            "importModel" -> {
                val uri = call.argument<String>("uri")
                val modelName = call.argument<String>("modelName")
                if (uri == null || modelName == null) {
                    result.error("BAD_ARGS", "uri/modelName missing", null); return
                }
                scope.launch {
                    val r = GuardianModule.modelImporter.importModel(Uri.parse(uri), modelName)
                    if (r.isSuccess) {
                        GuardianModule.aiDetector.ensureLoaded()
                        result.success(true)
                    } else {
                        result.error("IMPORT_FAILED", r.exceptionOrNull()?.message, null)
                    }
                }
            }
            "deleteModel" -> {
                val modelName = call.argument<String>("modelName") ?: run {
                    result.error("BAD_ARGS", "modelName missing", null); return
                }
                result.success(GuardianModule.modelImporter.deleteModel(modelName))
            }

            // ─── Keyword rules ──────────────────────────────────
            "getKeywordRules" -> {
                val list = GuardianModule.keywordEngine.rules.value.map {
                    mapOf("keyword" to it.keyword, "isRegex" to it.isRegex, "enabled" to it.enabled)
                }
                result.success(list)
            }
            "setKeywordRules" -> {
                @Suppress("UNCHECKED_CAST")
                val rawRules = call.argument<List<Map<String, Any?>>>("rules") ?: emptyList()
                val rules = rawRules.mapNotNull { m ->
                    val kw = m["keyword"] as? String ?: return@mapNotNull null
                    KeywordRule(
                        keyword = kw,
                        isRegex = (m["isRegex"] as? Boolean) ?: false,
                        enabled = (m["enabled"] as? Boolean) ?: true
                    )
                }
                scope.launch {
                    GuardianModule.keywordEngine.setRules(rules)
                    result.success(true)
                }
            }
            "evaluateText" -> {
                val text = call.argument<String>("text") ?: ""
                val match = GuardianModule.keywordEngine.evaluateText(text)
                result.success(match is com.guraba.app.guardian.engine.TextMatch.Hit)
            }

            // ─── Block-event log ────────────────────────────────
            "getBlockEvents" -> scope.launch {
                val events = GuardianModule.eventLogger.readAll().map {
                    mapOf(
                        "timestamp"   to it.timestamp,
                        "packageName" to it.packageName,
                        "reason"      to it.reason,
                        "detail"      to it.detail
                    )
                }
                result.success(events)
            }
            "clearBlockEvents" -> scope.launch {
                GuardianModule.eventLogger.clear(); result.success(true)
            }
            "exportBlockEventsCsv" -> scope.launch {
                val f = GuardianModule.eventLogger.exportCsv()
                result.success(f.absolutePath)
            }

            else -> result.notImplemented()
        }
    }

    private fun setPref(
        call: MethodCall, key: String, result: MethodChannel.Result,
        apply: suspend (Any) -> Unit
    ) {
        val value = call.argument<Any>(key) ?: run {
            result.error("BAD_ARGS", "$key missing", null); return
        }
        scope.launch {
            try { apply(value); result.success(true) }
            catch (t: Throwable) { result.error("PREF_FAILED", t.message, null) }
        }
    }
}