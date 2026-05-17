package com.guraba.app.guardian.pin

import com.guraba.app.guardian.prefs.SecureStorage
import java.security.MessageDigest

/**
 * PIN protection for Guardian/Guraba settings.
 * 4–6 digit PIN, SHA-256 hashed, with progressive lockout after failed attempts.
 */
class PinManager(private val secure: SecureStorage) {

    fun isPinSet(): Boolean =
        !secure.getString(SecureStorage.KEY_PIN_HASH).isNullOrBlank()

    fun setPin(pin: String): Boolean {
        if (pin.length !in 4..6 || !pin.all { it.isDigit() }) return false
        secure.putString(SecureStorage.KEY_PIN_HASH, hash(pin))
        secure.putInt(SecureStorage.KEY_PIN_ATTEMPTS, 0)
        secure.putLong(SecureStorage.KEY_PIN_LOCKOUT_UNTIL, 0L)
        return true
    }

    fun verifyPin(pin: String): VerifyResult {
        val now = System.currentTimeMillis()
        val lockedUntil = secure.getLong(SecureStorage.KEY_PIN_LOCKOUT_UNTIL, 0L)
        if (now < lockedUntil) return VerifyResult.LockedOut(lockedUntil - now)

        val stored = secure.getString(SecureStorage.KEY_PIN_HASH) ?: return VerifyResult.NotSet
        return if (stored == hash(pin)) {
            secure.putInt(SecureStorage.KEY_PIN_ATTEMPTS, 0)
            VerifyResult.Success
        } else {
            val attempts = secure.getInt(SecureStorage.KEY_PIN_ATTEMPTS, 0) + 1
            secure.putInt(SecureStorage.KEY_PIN_ATTEMPTS, attempts)
            if (attempts >= PIN_MAX_ATTEMPTS) {
                secure.putLong(SecureStorage.KEY_PIN_LOCKOUT_UNTIL, now + PIN_LOCKOUT_MS)
                secure.putInt(SecureStorage.KEY_PIN_ATTEMPTS, 0)
                VerifyResult.LockedOut(PIN_LOCKOUT_MS)
            } else {
                VerifyResult.Wrong(PIN_MAX_ATTEMPTS - attempts)
            }
        }
    }

    fun clearPin() {
        secure.remove(SecureStorage.KEY_PIN_HASH)
        secure.remove(SecureStorage.KEY_PIN_ATTEMPTS)
        secure.remove(SecureStorage.KEY_PIN_LOCKOUT_UNTIL)
    }

    private fun hash(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    sealed class VerifyResult {
        data object Success : VerifyResult()
        data class Wrong(val remainingAttempts: Int) : VerifyResult()
        data class LockedOut(val msRemaining: Long) : VerifyResult()
        data object NotSet : VerifyResult()
    }

    companion object {
        const val PIN_MAX_ATTEMPTS = 5
        const val PIN_LOCKOUT_MS = 5L * 60 * 1000  // 5 minutes
    }
}
