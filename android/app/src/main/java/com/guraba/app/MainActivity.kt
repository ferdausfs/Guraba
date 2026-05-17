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
package com.guraba.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.guraba.app.guardian.GuardianModule
import com.guraba.app.guardian.bridge.GuardianMethodHandler
import com.guraba.app.helpers.AlarmTasksSchedulingHelper.scheduleMidnightResetTask
import com.guraba.app.helpers.device.NotificationHelper
import com.guraba.app.helpers.storage.SharedPrefsHelper
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterFragmentActivity() {
    private lateinit var fgMethodCallHandler: FgMethodCallHandler
    private lateinit var vpnPermissionLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {

        // Store uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler { _: Thread?, exception: Throwable ->
            SharedPrefsHelper.insertCrashLogToPrefs(
                this, exception
            )
        }

        // Register notification channels
        NotificationHelper.registerNotificationChannels(this)

        // Init Guardian module (AI NSFW filter, PIN, keyword rules, block-event log)
        GuardianModule.init(this)

        // Register VPN permission launcher
        vpnPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { }

        // initialize method channel and bind to services
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
        val methodChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            AppConstants.FLUTTER_METHOD_CHANNEL_FG
        )
        methodChannel.setMethodCallHandler(fgMethodCallHandler)

        // Guardian module channel (NSFW filter, PIN, keyword rules, event log)
        val guardianChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            AppConstants.FLUTTER_METHOD_CHANNEL_GUARDIAN
        )
        guardianChannel.setMethodCallHandler(GuardianMethodHandler(this))

        // Get the self start status
        val isSelfStart =
            intent.getBooleanExtra(AppConstants.INTENT_EXTRA_IS_SELF_RESTART, false)

        // Update self start status on flutter side
        methodChannel.invokeMethod("updateSelfStartStatus", isSelfStart)
        super.configureFlutterEngine(flutterEngine)
    }


    override fun onDestroy() {
        fgMethodCallHandler.dispose()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Guraba.MainActivity"
    }
}
