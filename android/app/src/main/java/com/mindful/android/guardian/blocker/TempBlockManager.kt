package com.mindful.android.guardian.blocker

import com.mindful.android.guardian.util.GuardianConstants
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class TempBlock(
    val packageName: String,
    val blockedAt: Long,
    val durationMs: Long
) {
    val expiresAt get() = blockedAt + durationMs
    val isExpired get() = System.currentTimeMillis() > expiresAt
    val remainingMs get() = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
    val remainingMinutes get() = (remainingMs / 60_000) + 1
}

@Singleton
class TempBlockManager @Inject constructor() {
    private val strikes = LinkedHashMap<String, Int>()
    private val strikeTime = LinkedHashMap<String, Long>()
    private val tempBlocks = LinkedHashMap<String, TempBlock>()

    @Synchronized
    fun recordAiDetection(pkg: String, durationMs: Long): Boolean {
        val lastStrike = strikeTime[pkg] ?: 0L
        if (System.currentTimeMillis() - lastStrike > GuardianConstants.STRIKE_RESET_MS) {
            strikes[pkg] = 0
        }
        val count = (strikes[pkg] ?: 0) + 1
        strikes[pkg] = count
        strikeTime[pkg] = System.currentTimeMillis()
        Timber.d("AI Strike $count/${GuardianConstants.STRIKE_THRESHOLD} for $pkg")
        return if (count >= GuardianConstants.STRIKE_THRESHOLD) {
            applyTempBlock(pkg, durationMs)
            strikes[pkg] = 0; strikeTime.remove(pkg)
            true
        } else false
    }

    @Synchronized
    fun applyTempBlock(pkg: String, durationMs: Long) {
        tempBlocks[pkg] = TempBlock(pkg, System.currentTimeMillis(), durationMs)
        Timber.w("TempBlock: $pkg for ${durationMs / 60_000} min")
    }

    @Synchronized
    fun isTempBlocked(pkg: String): TempBlock? {
        val block = tempBlocks[pkg] ?: return null
        return if (block.isExpired) { tempBlocks.remove(pkg); strikes.remove(pkg); null } else block
    }

    @Synchronized
    fun clearTempBlock(pkg: String) { tempBlocks.remove(pkg); strikes.remove(pkg); strikeTime.remove(pkg) }

    @Synchronized
    fun getStrikeCount(pkg: String) = strikes[pkg] ?: 0
}
