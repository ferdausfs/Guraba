/*
 *
 *  *
 *  *  * Copyright (c) 2024 Mindful (https://github.com/akaMrNagar/Mindful)
 *  *  * Author : Pawan Nagar (https://github.com/akaMrNagar)
 *  *  *
 *  *  * This source code is licensed under the GPL-2.0 license license found in the
 *  *  * LICENSE file in the root directory of this source tree.
 *  *
 *
 */
package com.mindful.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.mindful.android.guardian.ui.GuardianMethodCallHandler
import com.mindful.android.helpers.AlarmTasksSchedulingHelper.scheduleMidnightResetTask
import com.mindful.android.helpers.device.NotificationHelper
import com.mindful.android.helpers.storage.SharedPrefsHelper
import dagger.hilt.android.AndroidEntryPoint
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FlutterFragmentActivity() {

    private lateinit var fgMethodCallHandler: FgMethodCallHandler
    private lateinit var vpnPermissionLauncher: ActivityResultLauncher<Intent>

    @Inject lateinit var guardianMethodCallHandler: GuardianMethodCallHandler

    override fun onCreate(savedInstanceState: Bundle?) {

        // Store uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler { _: Thread?, exception: Throwable ->
            SharedPrefsHelper.insertCrashLogToPrefs(this, exception)
        }

        // Register notification channels
        NotificationHelper.registerNotificationChannels(this)

        // Register VPN permission launcher
        vpnPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { }

        // initialize Mindful method channel handler
        fgMethodCallHandler = FgMethodCallHandler(
            context = this,
            activity = this,
            vpnPermLauncher = vpnPermissionLauncher
        )

        // Schedule midnight 12 task if already not scheduled
        scheduleMidnightResetTask(this, true)
        super.onCreate(savedInstanceState)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        // ── Mindful channel (existing) ────────────────────────────────────────
        val mindfulChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            AppConstants.FLUTTER_METHOD_CHANNEL_FG
        )
        mindfulChannel.setMethodCallHandler(fgMethodCallHandler)

        val isSelfStart = intent.getBooleanExtra(AppConstants.INTENT_EXTRA_IS_SELF_RESTART, false)
        mindfulChannel.invokeMethod("updateSelfStartStatus", isSelfStart)

        // ── Guardian channel (new) ────────────────────────────────────────────
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            GUARDIAN_CHANNEL
        ).setMethodCallHandler(guardianMethodCallHandler)

        super.configureFlutterEngine(flutterEngine)
    }

    override fun onDestroy() {
        fgMethodCallHandler.dispose()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Mindful.MainActivity"
        const val GUARDIAN_CHANNEL = "com.mindful.android/guardian"
    }
}
