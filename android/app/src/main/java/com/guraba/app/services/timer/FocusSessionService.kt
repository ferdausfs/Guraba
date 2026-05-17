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
package com.guraba.app.services.timer

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import android.service.quicksettings.TileService
import android.util.Log
import com.guraba.app.AppConstants.FOCUS_SESSION_SERVICE_NOTIFICATION_ID
import com.guraba.app.R
import com.guraba.app.enums.DndWakeLock
import com.guraba.app.generics.SafeServiceConnection
import com.guraba.app.generics.ServiceBinder
import com.guraba.app.helpers.device.NotificationHelper
import com.guraba.app.helpers.device.NotificationHelper.FOCUS_CHANNEL_ID
import com.guraba.app.helpers.storage.SharedPrefsHelper
import com.guraba.app.models.FocusSession
import com.guraba.app.services.quickTiles.FocusQuickTileService
import com.guraba.app.services.tracking.GurabaTrackerService
import com.guraba.app.utils.AppUtils
import com.guraba.app.utils.DateTimeUtils
import java.util.Calendar
import kotlin.math.max

class FocusSessionService : Service() {
    private val mBinder = ServiceBinder(this@FocusSessionService)
    private lateinit var mTrackerServiceConn: SafeServiceConnection<GurabaTrackerService>
    private lateinit var mNotificationTimer: NotificationTimer


    private var session: FocusSession? = null

    override fun onCreate() {
        mTrackerServiceConn = SafeServiceConnection(
            context = this,
            serviceClass = GurabaTrackerService::class.java
        )
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ServiceBinder.ACTION_START_GURABA_SERVICE) {
            return START_STICKY
        }

        stopSelf()
        return START_NOT_STICKY
    }


    /**
     * Starts a countdown timer for a focus session. Configures notifications to show the remaining time
     * and handles DND mode if needed.
     */
    fun startFocusSession(focusSession: FocusSession) {
        try {
            if (focusSession.distractingApps.isEmpty()) {
                stopSelf()
                return
            }

            session = focusSession
            initializeSessionTimer(focusSession)

            startForeground(
                FOCUS_SESSION_SERVICE_NOTIFICATION_ID,
                mNotificationTimer.getInitialNotification
            )

            /// Start and bind tracking service
            mTrackerServiceConn.setOnConnectedCallback { service: GurabaTrackerService ->
                service.getRestrictionManager.updateFocusedApps(
                    focusSession.distractingApps
                )
            }
            mTrackerServiceConn.startAndBind()

            // Toggle DND according to the session configurations
            if (focusSession.toggleDnd) NotificationHelper.toggleDnd(
                this,
                DndWakeLock.FOCUS_MODE,
                true
            )

            mNotificationTimer.startTimer()

            /// Update focus quick tile
            TileService.requestListeningState(
                this,
                ComponentName(this, FocusQuickTileService::class.java)
            )
            Log.d(TAG, "startFocusSession: FOCUS service started successfully")
        } catch (e: Exception) {
            Log.d(TAG, "startFocusSession: Failed to start FOCUS service", e)
            SharedPrefsHelper.insertCrashLogToPrefs(this, e)
            stopSelf()
        }
    }

    private fun initializeSessionTimer(session: FocusSession) {
        val isFiniteSession = session.durationSecs > 0
        val elapsedTimeMs = System.currentTimeMillis() - session.startTimeMsEpoch

        val timerDuration: Long = if (isFiniteSession) {
            session.durationSecs.toLong()
        } else {
            val cal = DateTimeUtils.todToTodayCal(0)
            cal.add(Calendar.HOUR, 24)
            cal.timeInMillis / 1000L
        }


        mNotificationTimer = NotificationTimer(
            context = this,
            ongoingPendingIntent = AppUtils.getPendingIntentForGurabaUri(
                this,
                "com.guraba.app://open/activeSession"
            ),
            finishedPendingIntent = AppUtils.getPendingIntentForGurabaUri(
                this,
                "com.guraba.app://open/focus?tab=1"
            ),
            isFinite = isFiniteSession,
            title = getString(R.string.focus_session_notification_title),
            timerDurationSeconds = timerDuration,
            alreadyElapsedTimeSecond = max(0, elapsedTimeMs / 1000L),
            notificationId = FOCUS_SESSION_SERVICE_NOTIFICATION_ID,
            notificationChannelId = FOCUS_CHANNEL_ID,
            onTicked = { remainingTime ->
                getString(
                    if (isFiniteSession) R.string.focus_session_notification_info
                    else R.string.focus_session_infinite_notification_info,
                    DateTimeUtils.secondsToTimeStr(remainingTime)
                )
            },
            onFinished = { getString(R.string.focus_session_success_notification_info) },
            onDispose = { stopSelf() },
        )
    }


    fun updateFocusSession(session: FocusSession) {
        mTrackerServiceConn.service?.getRestrictionManager?.updateFocusedApps(session.distractingApps)
        Log.d(
            TAG,
            "updateDistractingApps: Focus session's distracting app's list updated successfully"
        )
    }


    fun giveUpOrStopFocusSession(isTheSessionSuccessful: Boolean) {
        if (session?.toggleDnd == true) {
            NotificationHelper.toggleDnd(this, DndWakeLock.FOCUS_MODE, false)
        }

        mNotificationTimer.forceDisposeTimer(
            getString(
                if (isTheSessionSuccessful) R.string.focus_session_success_notification_info
                else R.string.focus_session_giveup_notification_info
            )
        )
    }


    override fun onDestroy() {
        mTrackerServiceConn.service?.getRestrictionManager?.updateFocusedApps(null)
        mTrackerServiceConn.unBindService()
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "onDestroy: FOCUS service destroyed successfully")

        /// Update focus quick tile
        TileService.requestListeningState(
            this,
            ComponentName(this, FocusQuickTileService::class.java)
        )
        super.onDestroy()
    }


    override fun onBind(intent: Intent): IBinder? {
        return if (intent.action == ServiceBinder.ACTION_BIND_TO_GURABA) mBinder
        else null
    }

    companion object {
        private const val TAG = "Guraba.FocusSessionService"
    }
}