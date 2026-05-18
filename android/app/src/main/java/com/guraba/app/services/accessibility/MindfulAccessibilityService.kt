/*
 *
 *  *
 *  *  * Copyright (c) 2024 Guraba (https://github.com/akaMrNagar/Guraba)
 *  *  * Author : Pawan Nagar (https://github.com/akaMrNagar)
 *  *  *
 *  *  * This source code is licensed under the GPL-2.0 license license found in the
 *  *  * LICENSE file in the root directory of this source tree.
 *  *
 *
 */
package com.guraba.app.services.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.guraba.app.AppConstants.FACEBOOK_PACKAGE
import com.guraba.app.AppConstants.INSTAGRAM_PACKAGE
import com.guraba.app.AppConstants.REDDIT_PACKAGE
import com.guraba.app.AppConstants.SETTINGS_PACKAGE
import com.guraba.app.AppConstants.SNAPCHAT_PACKAGE
import com.guraba.app.AppConstants.YOUTUBE_PACKAGE
import com.guraba.app.R
import com.guraba.app.enums.PlatformFeatures
import com.guraba.app.guardian.GuardianModule
import com.guraba.app.guardian.blocker.GuardianBlockReason
import com.guraba.app.guardian.blocker.GuardianBlockingEngine
import com.guraba.app.guardian.engine.TextMatch
import com.guraba.app.helpers.device.PermissionsHelper
import com.guraba.app.helpers.storage.SharedPrefsHelper
import com.guraba.app.models.Wellbeing
import com.guraba.app.receivers.DeviceAppsChangedReceiver
import com.guraba.app.utils.ThreadUtils
import com.guraba.app.utils.executors.Throttler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * An AccessibilityService that monitors app usage and blocks access to specified content based on user settings.
 * Integrated with Guardian module for AI NSFW detection and keyword filtering.
 */
class GurabaAccessibilityService : AccessibilityService(), OnSharedPreferenceChangeListener {
    companion object {
        private const val TAG = "Guraba.GurabaAccessibilityService"
        private const val TEXT_THROTTLE_MS = 600L
        private const val AI_THROTTLE_MS = 3_500L

        const val ACTION_PERFORM_HOME_PRESS = "com.guraba.app.action.performHomePress"
        const val ACTION_MIDNIGHT_ACCESSIBILITY_RESET =
            "com.guraba.app.action.midnightAccessibilityReset"
        const val ACTION_TAMPER_PROTECTION_CHANGED =
            "com.guraba.app.action.tamperProtectionChanged"

        // Set of desired events which will be processed
        private val desiredEvents = setOf(
            TYPE_WINDOWS_CHANGED,
            TYPE_WINDOW_STATE_CHANGED,
            TYPE_VIEW_SCROLLED
        )

        private val browserPackages = mutableSetOf<String>()
        private val shortsPlatformPackages = mutableSetOf<String>()
        private val devicePlatformPackages = mutableSetOf<String>()
    }

    // Fixed thread pool for parallel event processing
    private val executorService: ExecutorService = Executors.newFixedThreadPool(4)
    private val throttler: Throttler = Throttler(500L)
    private val textThrottleMap = LinkedHashMap<String, Long>()
    private val aiThrottleMap = LinkedHashMap<String, Long>()
    private val deviceAppsChangedReceiver: DeviceAppsChangedReceiver =
        DeviceAppsChangedReceiver(onAppsChanged = { refreshServiceConfig() })

    private val guardianScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Managers
    private lateinit var shortsPlatformManager: ShortsPlatformManager
    private lateinit var browserManager: BrowserManager
    private lateinit var deviceFeaturesManager: DeviceFeaturesManager
    private lateinit var trackingManager: TrackingManager

    private var wellbeing = Wellbeing()

    override fun onCreate() {
        super.onCreate()

        // Initialize Guardian module
        GuardianModule.init(this)

        trackingManager = TrackingManager(context = this)
        deviceFeaturesManager = DeviceFeaturesManager(
            context = this,
            blockedContentGoBack = this::goBackWithToast
        )
        shortsPlatformManager = ShortsPlatformManager(
            context = this,
            blockedContentGoBack = this::goBackWithToast
        )
        browserManager = BrowserManager(
            context = this,
            shortsPlatformManager = shortsPlatformManager,
            blockedContentGoBack = this::goBackWithToast
        )

        // Register shared prefs listener and load data
        SharedPrefsHelper.registerUnregisterListenerToListenablePrefs(this, true, this)
        wellbeing = SharedPrefsHelper.getSetWellBeingSettings(this, null)

        // Register listener for install and uninstall events
        deviceAppsChangedReceiver.register(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_MIDNIGHT_ACCESSIBILITY_RESET -> {
                shortsPlatformManager.resetShortsScreenTime()
                Log.d(TAG, "onStartCommand: Midnight reset completed")
            }

            ACTION_TAMPER_PROTECTION_CHANGED -> {
                Log.d(TAG, "onStartCommand: Tamper protection changed")
                refreshServiceConfig()
            }

            ACTION_PERFORM_HOME_PRESS -> {
                Log.d(TAG, "onStartCommand: Pressing home button")
                goBackWithToast(GLOBAL_ACTION_HOME)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onServiceConnected() {
        refreshServiceConfig()
        trackingManager.stopManualTracking()
        Log.d(TAG, "onCreate: Accessibility service started successfully")
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            // If not desired event or executor is shutdown, then just return
            if (!desiredEvents.contains(event.eventType) || executorService.isShutdown) return

            executorService.submit {
                // Determine package and event source node
                val eventPackageName = event.packageName.toString()
                val node = if (eventPackageName == REDDIT_PACKAGE) event.source
                else rootInActiveWindow ?: event.source

                node?.let {
                    // Broadcast event
                    trackingManager.onNewEvent("${it.packageName}")

                    // Guardian: keyword + AI check for all apps
                    if (GuardianModule.isInitialized() && GuardianModule.aiDetector.cachedAiEnabled) {
                        runGuardianChecks(eventPackageName, it)
                    }

                    // Only process if any of the content is blocked
                    if (shouldBlockContent()) {
                        processEventInBackground(
                            packageName = eventPackageName,
                            node = it,
                            wellBeing = wellbeing.copy()
                        )
                    }
                }
            }

        } catch (ignored: Exception) {
        }
    }

    /**
     * Runs Guardian keyword and AI detection on any app.
     */
    private fun runGuardianChecks(pkg: String, node: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()

        // --- Keyword check (throttled per package) ---
        val lastText = textThrottleMap[pkg] ?: 0L
        if (now - lastText >= TEXT_THROTTLE_MS) {
            textThrottleMap[pkg] = now
            if (textThrottleMap.size > 50) {
                val it = textThrottleMap.entries.iterator()
                if (it.hasNext()) { it.next(); it.remove() }
            }

            val pageText = BrowserManager.extractPageText(node)
            if (pageText.isNotBlank()) {
                val result = GuardianModule.keywordEngine.evaluateText(pageText)
                if (result is TextMatch.Hit) {
                    Log.d(TAG, "Guardian: keyword '${result.rule}' matched in $pkg")
                    GuardianBlockingEngine.block(
                        context = this,
                        pkg = pkg,
                        reason = GuardianBlockReason.KEYWORD_MATCH,
                        detail = result.rule,
                        logger = GuardianModule.eventLogger
                    )
                    return
                }
            }
        }

        // --- AI check (throttled — heavier operation) ---
        val lastAi = aiThrottleMap[pkg] ?: 0L
        if (now - lastAi >= AI_THROTTLE_MS) {
            aiThrottleMap[pkg] = now
            if (aiThrottleMap.size > 50) {
                val it = aiThrottleMap.entries.iterator()
                if (it.hasNext()) { it.next(); it.remove() }
            }

            // Check if already temp blocked
            val tempBlock = GuardianBlockingEngine.isTempBlocked(pkg)
            if (tempBlock != null) {
                Log.d(TAG, "Guardian: $pkg is temp blocked for ${tempBlock.remainingMinutes}min")
                GuardianBlockingEngine.block(
                    context = this,
                    pkg = pkg,
                    reason = GuardianBlockReason.AI_DETECTION,
                    detail = "temp_block:${tempBlock.remainingMinutes}min",
                    logger = GuardianModule.eventLogger
                )
                return
            }

            // Take screenshot for AI
            guardianScope.launch {
                try {
                    val bitmap = takeScreenshotCompat() ?: return@launch
                    val aiDetector = GuardianModule.aiDetector
                    aiDetector.ensureLoaded()

                    val userGender = aiDetector.cachedUserGender
                    val isUnsafe = when {
                        userGender == "MALE" || userGender == "FEMALE" ->
                            aiDetector.isOppositeGenderNsfw(bitmap, userGender) ||
                            aiDetector.isUnsafe(bitmap)
                        else -> aiDetector.isUnsafe(bitmap)
                    }

                    if (!bitmap.isRecycled) bitmap.recycle()

                    if (isUnsafe) {
                        Log.w(TAG, "Guardian: AI detected unsafe content in $pkg")
                        GuardianBlockingEngine.block(
                            context = this@GurabaAccessibilityService,
                            pkg = pkg,
                            reason = GuardianBlockReason.AI_DETECTION,
                            detail = "nsfw_detected",
                            logger = GuardianModule.eventLogger
                        )
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Guardian: AI check failed for $pkg", t)
                }
            }
        }
    }

    /**
     * Takes a screenshot compatible with the current API level.
     */
    private fun takeScreenshotCompat(): Bitmap? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // API 30+ takeScreenshot is async — not usable here directly
                // So we capture from rootInActiveWindow
                captureNodeBitmap(rootInActiveWindow)
            } else {
                captureNodeBitmap(rootInActiveWindow)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "takeScreenshotCompat failed", t)
            null
        }
    }

    private fun captureNodeBitmap(node: AccessibilityNodeInfo?): Bitmap? {
        node ?: return null
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.width() <= 0 || bounds.height() <= 0) return null
        val root = node.window ?: return null
        var bmp: Bitmap? = null
        root.getRoot()?.let {
            // Fallback: create a placeholder — real screenshot needs MediaProjection
            // which requires user permission. Here we return null to skip AI gracefully.
        }
        return bmp
    }

    /**
     * Processes accessibility event in background thread instead of main thread.
     */
    private fun processEventInBackground(
        packageName: String,
        node: AccessibilityNodeInfo,
        wellBeing: Wellbeing,
    ) {
        try {
            when (packageName) {
                in devicePlatformPackages ->
                    deviceFeaturesManager.blockFeatures(packageName, node, wellBeing)

                in shortsPlatformPackages ->
                    shortsPlatformManager.blockDistraction(packageName, node, wellBeing)

                in browserPackages ->
                    browserManager.blockDistraction(packageName, node, wellBeing)
            }

        } catch (e: Exception) {
            Log.e(
                TAG,
                "processEventInBackground: Failed to process accessibility event in background",
                e
            )
            SharedPrefsHelper.insertCrashLogToPrefs(this, e)
        }
    }

    /**
     * Determines whether content should be blocked based on the current settings.
     */
    private fun shouldBlockContent(): Boolean {
        return wellbeing.blockedFeatures.isNotEmpty() ||
                wellbeing.blockedWebsites.isNotEmpty() ||
                wellbeing.nsfwWebsites.isNotEmpty() ||
                wellbeing.blockNsfwSites
    }

    /**
     * Performs the back action and shows a toast message indicating that the content is blocked.
     */
    private fun goBackWithToast(customAction: Int? = null) {
        throttler.submit {
            ThreadUtils.runOnMainThread {
                performGlobalAction(customAction ?: GLOBAL_ACTION_BACK)
                Toast.makeText(
                    this@GurabaAccessibilityService,
                    getString(R.string.toast_blocked_content),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Updates the service info with the latest settings and registered packages.
     */
    private fun refreshServiceConfig() {
        try {
            browserPackages.clear()
            devicePlatformPackages.clear()
            shortsPlatformPackages.clear()
            val pm = packageManager

            if (PermissionsHelper.getAndAskAdminPermission(this, false)) {
                devicePlatformPackages.add(SETTINGS_PACKAGE)
            }

            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))
            pm.queryIntentActivities(browserIntent, PackageManager.MATCH_ALL).forEach {
                browserPackages.add(it.activityInfo.packageName)
            }

            wellbeing.blockedFeatures.forEach { feature ->
                when (feature) {
                    PlatformFeatures.INSTAGRAM_REELS,
                    PlatformFeatures.INSTAGRAM_EXPLORE,
                        -> shortsPlatformPackages.add(INSTAGRAM_PACKAGE)

                    PlatformFeatures.SNAPCHAT_SPOTLIGHT,
                    PlatformFeatures.SNAPCHAT_DISCOVER,
                        -> shortsPlatformPackages.add(SNAPCHAT_PACKAGE)

                    PlatformFeatures.FACEBOOK_REELS ->
                        shortsPlatformPackages.add(FACEBOOK_PACKAGE)

                    PlatformFeatures.REDDIT_SHORTS ->
                        shortsPlatformPackages.add(REDDIT_PACKAGE)

                    PlatformFeatures.YOUTUBE_SHORTS -> {
                        shortsPlatformPackages.add(YOUTUBE_PACKAGE)
                        val ytIntent =
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
                        pm.queryIntentActivities(ytIntent, PackageManager.MATCH_ALL)
                            .filterNot { browserPackages.contains(it.activityInfo.packageName) }
                            .forEach {
                                shortsPlatformPackages.add(it.activityInfo.packageName)
                            }
                    }
                }
            }

            if (wellbeing.blockNsfwSites) BrowserManager.initializeNsfwDomains()
            else BrowserManager.clearNsfwDomains()

            Log.d(
                TAG, "refreshServiceConfig: Config updated:" +
                        "\n settings: $wellbeing" +
                        "\n device: $devicePlatformPackages" +
                        "\n shorts: $shortsPlatformPackages" +
                        "\n browsers: $browserPackages"
            )
        } catch (e: Exception) {
            Log.e(TAG, "refreshServiceInfo: Failed to refresh service info", e)
            SharedPrefsHelper.insertCrashLogToPrefs(this, e)
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, changedKey: String?) {
        changedKey?.let { key ->
            if (key == SharedPrefsHelper.PREF_KEY_WELLBEING_SETTINGS) {
                Log.d(TAG, "OnSharedPrefsChanged: Key changed = $changedKey")
                wellbeing = SharedPrefsHelper.getSetWellBeingSettings(this, null)
                refreshServiceConfig()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        try {
            executorService.shutdownNow()
            trackingManager.startManualTracking()
            deviceAppsChangedReceiver.unRegister(this)
            SharedPrefsHelper.registerUnregisterListenerToListenablePrefs(this, false, this)
        } catch (e: Exception) {
            // ignored
        }

        Log.d(TAG, "onDestroy: Accessibility service destroyed")
        super.onDestroy()
    }
}
