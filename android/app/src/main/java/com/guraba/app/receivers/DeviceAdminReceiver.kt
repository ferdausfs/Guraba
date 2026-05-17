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

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.guraba.app.services.accessibility.GurabaAccessibilityService
import com.guraba.app.services.accessibility.GurabaAccessibilityService.Companion.ACTION_TAMPER_PROTECTION_CHANGED
import com.guraba.app.utils.Utils

/**
 * A DeviceAdminReceiver for handling device administration events for the Guraba app.
 */
class DeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Tamper protection enabled", Toast.LENGTH_LONG).show()
        refreshWellbeingSettings(context)
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Tamper protection disabled", Toast.LENGTH_LONG).show()
        refreshWellbeingSettings(context)
        super.onDisabled(context, intent)
    }

    private fun refreshWellbeingSettings(context: Context) {
        if (Utils.isServiceRunning(context, GurabaAccessibilityService::class.java)) {
            val serviceIntent = Intent(
                context.applicationContext,
                GurabaAccessibilityService::class.java
            ).setAction(ACTION_TAMPER_PROTECTION_CHANGED)

            context.startService(serviceIntent)
        }
    }
}

