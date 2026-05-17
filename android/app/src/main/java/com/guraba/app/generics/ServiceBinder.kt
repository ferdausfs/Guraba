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
package com.guraba.app.generics

import android.app.Service
import android.os.Binder

/**
 * ServiceBinder is a generic binder class used to provide a reference to a service.
 * It allows the client to retrieve the service instance that is bound to it.
 *
 * @param <T> The type of the service being bound.
</T> */
class ServiceBinder<T : Service?>(val service: T) : Binder() {
    companion object {
        const val ACTION_START_GURABA_SERVICE: String =
            "com.guraba.app.action.startGurabaService"

        const val ACTION_BIND_TO_GURABA: String =
            "com.guraba.app.action.bindToGuraba"
    }
}
