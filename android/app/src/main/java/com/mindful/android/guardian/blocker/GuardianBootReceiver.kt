package com.mindful.android.guardian.blocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class GuardianBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Timber.i("GuardianBootReceiver: starting ContentGuardService")
                ContentGuardService.start(context)
            }
        }
    }
}
