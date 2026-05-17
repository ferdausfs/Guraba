package com.guraba.app.guardian.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Encrypted key/value storage used for PIN hash and lockout state. */
class SecureStorage(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        // Fallback: encryption unavailable on this device (rare). Use normal prefs.
        context.getSharedPreferences(FILE_NAME + "_plain", Context.MODE_PRIVATE)
    }

    fun getString(key: String, def: String? = null): String? = prefs.getString(key, def)
    fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    fun getInt(key: String, def: Int = 0): Int = prefs.getInt(key, def)
    fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    fun getLong(key: String, def: Long = 0L): Long = prefs.getLong(key, def)
    fun putLong(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }
    fun remove(key: String) { prefs.edit().remove(key).apply() }

    companion object {
        private const val FILE_NAME = "guraba_secure_prefs"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_ATTEMPTS = "pin_attempts"
        const val KEY_PIN_LOCKOUT_UNTIL = "pin_lockout_until"
    }
}
