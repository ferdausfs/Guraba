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
package com.guraba.app.receivers.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.guraba.app.generics.SafeServiceConnection
import com.guraba.app.helpers.AlarmTasksSchedulingHelper.scheduleMidnightResetTask
import com.guraba.app.helpers.storage.SharedPrefsHelper
import com.guraba.app.services.accessibility.GurabaAccessibilityService
import com.guraba.app.services.accessibility.GurabaAccessibilityService.Companion.ACTION_MIDNIGHT_ACCESSIBILITY_RESET
import com.guraba.app.services.tracking.GurabaTrackerService
import com.guraba.app.utils.Utils
import com.guraba.app.workers.FlutterBgExecutionWorker
import com.guraba.app.workers.FlutterBgExecutionWorker.Companion.FLUTTER_TASK_ID

class MidnightResetReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "Guraba.MidnightResetReceiver"
        const val ACTION_START_MIDNIGHT_RESET = "com.guraba.app.action.startMidnightReset"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_START_MIDNIGHT_RESET) {
            WorkManager.getInstance(context).let {

                /// Enqueue midnight worker for services
                it.enqueueUniqueWork(
                    "Guraba.MidnightResetReceiver.Native",
                    ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequest.Builder(MidnightResetWorker::class.java).build()
                )

                /// Enqueue flutter bg worker to backup apps usage
                it.enqueueUniqueWork(
                    "Guraba.MidnightResetReceiver.FlutterBg",
                    ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequest
                        .Builder(FlutterBgExecutionWorker::class.java)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setInputData(
                            Data.Builder().putString(FLUTTER_TASK_ID, "onMidnightReset")
                                .build()
                        ).build()
                )
            }
        }
    }

    class MidnightResetWorker(
        private val context: Context,
        params: WorkerParameters,
    ) : Worker(context, params) {
        private val mTrackerServiceConn = SafeServiceConnection(
            context = context,
            serviceClass = GurabaTrackerService::class.java,
        )


        override fun doWork(): Result {
            try {
                // Let tracking service know about midnight reset
                mTrackerServiceConn.setOnConnectedCallback { service: GurabaTrackerService -> service.onMidnightReset() }
                mTrackerServiceConn.bindService()

                // Let accessibility service know about midnight reset
                if (Utils.isServiceRunning(context, GurabaAccessibilityService::class.java)) {
                    val serviceIntent = Intent(
                        context.applicationContext,
                        GurabaAccessibilityService::class.java
                    ).setAction(ACTION_MIDNIGHT_ACCESSIBILITY_RESET)
                    context.startService(serviceIntent)
                } else {
                    // Else at least reset short content screen time
                    SharedPrefsHelper.getSetShortsScreenTimeMs(context, 0L)
                }

                Log.d(TAG, "doWork: Midnight reset work completed successfully")
                return Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "doWork: Error during work execution", e)
                SharedPrefsHelper.insertCrashLogToPrefs(context, e)
                return Result.failure()
            } finally {
                // Unbind service and schedule task for the next day
                mTrackerServiceConn.unBindService()
                scheduleMidnightResetTask(context, false)
            }
        }
    }
}