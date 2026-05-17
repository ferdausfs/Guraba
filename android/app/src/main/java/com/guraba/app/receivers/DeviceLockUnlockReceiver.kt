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
package com.guraba.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.WorkerThread
import com.guraba.app.helpers.storage.SharedPrefsHelper
import com.guraba.app.utils.Utils

/**
 * BroadcastReceiver that monitors device lock/unlock events and tracks app launches while the device is unlocked.
 */
class DeviceLockUnlockReceiver(
    @WorkerThread
    private val onDeviceLockChanged: (isUnLocked: Boolean) -> Unit,
) : BroadcastReceiver() {
    companion object {
        private const val TAG = "Guraba.DeviceLockUnlockReceiver"
    }

    fun register(context: Context) {
        try {
            Utils.safelyRegisterReceiver(
                context = context,
                receiver = this,
                exported = true,
                intentFilter = IntentFilter().apply {
                    addAction(Intent.ACTION_USER_PRESENT)
                    addAction(Intent.ACTION_SCREEN_OFF)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "register: Failed to register receiver", e)
            SharedPrefsHelper.insertCrashLogToPrefs(context, e)
        }
    }

    fun unRegister(context: Context) {
        try {
            context.unregisterReceiver(this)
        } catch (e: Exception) {
            Log.e(TAG, "register: Failed to un-register receiver", e)
            SharedPrefsHelper.insertCrashLogToPrefs(context, e)
        }
    }


    override fun onReceive(context: Context, intent: Intent) {
        Thread {
            when (intent.action) {
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "onReceive: User UNLOCKED the device and device is ACTIVE")
                    onDeviceLockChanged.invoke(true)
                }

                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "onReceive: User LOCKED the device and device is INACTIVE")
                    onDeviceLockChanged.invoke(false)
                }
            }
        }.start()
    }
}
