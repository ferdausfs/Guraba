package com.mindful.android.guardian.blocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.mindful.android.MainActivity
import com.mindful.android.guardian.data.GuardianPreferences
import com.mindful.android.guardian.util.GuardianConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ContentGuardService : Service() {

    @Inject lateinit var prefs: GuardianPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watchdogJob: Job? = null
    @Volatile private var protectionEnabled = true
    private var consecutiveFailCount = 0

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForegroundCompat()
        observePrefs()
        startWatchdog()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Timber.w("Task removed — restarting ContentGuardService")
        val restart = Intent(applicationContext, ContentGuardService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(restart)
            else startService(restart)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(GuardianConstants.CHANNEL_GUARDIAN) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        GuardianConstants.CHANNEL_GUARDIAN,
                        "Guardian Shield",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply { setShowBadge(false) }
                )
            }
        }
    }

    private fun startForegroundCompat() {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(GuardianConstants.GUARDIAN_NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(GuardianConstants.GUARDIAN_NOTIF_ID, notif)
        }
    }

    private fun observePrefs() {
        serviceScope.launch {
            try { prefs.protectionEnabled.collect { protectionEnabled = it } }
            catch (t: Throwable) { Timber.e(t) }
        }
    }

    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = serviceScope.launch {
            while (isActive) {
                delay(GuardianConstants.ACCESSIBILITY_WATCHDOG_MS)
                try {
                    if (!protectionEnabled) { consecutiveFailCount = 0; continue }
                    if (!isAccessibilityEnabled()) {
                        consecutiveFailCount++
                        Timber.w("Accessibility fail $consecutiveFailCount/3")
                        if (consecutiveFailCount >= 3) { consecutiveFailCount = 0 }
                    } else { consecutiveFailCount = 0 }
                } catch (t: Throwable) { Timber.e(t, "Watchdog error"); consecutiveFailCount = 0 }
            }
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        return try {
            val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            enabled.contains(packageName, ignoreCase = true)
        } catch (_: Throwable) { true }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, GuardianConstants.CHANNEL_GUARDIAN)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Guardian Shield Active")
            .setContentText("Content protection is running")
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        watchdogJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ContentGuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
        fun stop(context: Context) = context.stopService(Intent(context, ContentGuardService::class.java))
    }
}
