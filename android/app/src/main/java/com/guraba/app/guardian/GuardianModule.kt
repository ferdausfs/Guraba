/*
 * Guraba — Guardian Module
 *
 * Service-locator entry point for all Guardian features (AI NSFW detection,
 * keyword filtering, PIN protection, model import, block-event logging).
 *
 * Ported & refactored from "Guardian Shield" (Dogs of KAHAF) and integrated
 * into the Guraba Flutter app. The Hilt DI of the original has been replaced
 * with a lightweight singleton container so it composes cleanly with Mindful's
 * existing service-based architecture.
 */
package com.guraba.app.guardian

import android.content.Context
import com.guraba.app.guardian.ai.AiDetector
import com.guraba.app.guardian.engine.KeywordRulesEngine
import com.guraba.app.guardian.log.BlockEventLogger
import com.guraba.app.guardian.model.ModelImportManager
import com.guraba.app.guardian.pin.PinManager
import com.guraba.app.guardian.prefs.GuardianPreferences
import com.guraba.app.guardian.prefs.SecureStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Lazy singleton container for the Guardian module. */
object GuardianModule {

    @Volatile private var initialized = false

    lateinit var preferences: GuardianPreferences
        private set
    lateinit var secureStorage: SecureStorage
        private set
    lateinit var pinManager: PinManager
        private set
    lateinit var modelImporter: ModelImportManager
        private set
    lateinit var aiDetector: AiDetector
        private set
    lateinit var keywordEngine: KeywordRulesEngine
        private set
    lateinit var eventLogger: BlockEventLogger
        private set

    /** Long-running module scope (cancelled on process death). */
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        val app = context.applicationContext
        preferences = GuardianPreferences(app)
        secureStorage = SecureStorage(app)
        pinManager = PinManager(secureStorage)
        modelImporter = ModelImportManager(app)
        aiDetector = AiDetector(app, preferences)
        keywordEngine = KeywordRulesEngine(app, preferences)
        eventLogger = BlockEventLogger(app)
        aiDetector.startPrefsCache(scope)
        keywordEngine.startPrefsCache(scope)
        initialized = true
    }
}
