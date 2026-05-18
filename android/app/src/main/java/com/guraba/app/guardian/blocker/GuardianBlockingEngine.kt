package com.guraba.app.guardian.blocker

import android.content.Context
import android.content.Intent
import android.util.Log
import com.guraba.app.guardian.log.BlockEventLogger
import com.guraba.app.guardian.log.BlockEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GuardianBlockingEngine {

    private const val TAG = "Guraba.GuardianBlockingEngine"
    private const val BLOCK_THROTTLE_MS = 3_000L
    private const val MAX_THROTTLE_MAP = 50
    private const val STRIKE_THRESHOLD = 2
    private const val STRIKE_RESET_MS = 10 * 60 * 1_000L
    private const val DEFAULT_TEMP_BLOCK_MS = 15 * 60 * 1_000L

    private val blockThrottleMap = LinkedHashMap<String, Long>()
    private val strikes = LinkedHashMap<String, Int>()
    private val strikeTime = LinkedHashMap<String, Long>()
    private val tempBlocks = LinkedHashMap<String, TempBlock>()

    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun block(
        context: Context,
        pkg: String,
        reason: GuardianBlockReason,
        detail: String,
        logger: BlockEventLogger
    ) {
        val now = System.currentTimeMillis()
        val throttleMs = when (reason) {
            GuardianBlockReason.APP_BLOCKED,
            GuardianBlockReason.KEYWORD_MATCH -> 500L
            else -> BLOCK_THROTTLE_MS
        }

        synchronized(blockThrottleMap) {
            val last = blockThrottleMap[pkg] ?: 0L
            if (now - last < throttleMs) return
            blockThrottleMap[pkg] = now
            if (blockThrottleMap.size > MAX_THROTTLE_MAP) {
                val it = blockThrottleMap.entries.iterator()
                if (it.hasNext()) { it.next(); it.remove() }
            }
        }

        val finalDetail = if (reason == GuardianBlockReason.AI_DETECTION) {
            val tempBlocked = recordAiDetection(pkg, DEFAULT_TEMP_BLOCK_MS)
            if (tempBlocked) {
                val block = isTempBlocked(pkg)
                "temp_block:${block?.remainingMinutes ?: 15}min"
            } else {
                val strike = getStrikeCount(pkg)
                "$detail (strike $strike/$STRIKE_THRESHOLD)"
            }
        } else detail

        launchOverlay(context, pkg, reason, finalDetail)

        ioScope.launch {
            try {
                logger.log(BlockEvent(timestamp = System.currentTimeMillis(), packageName = pkg, reason = reason.name, detail = finalDetail))
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to log block event", t)
            }
        }
    }

    @Synchronized
    fun isTempBlocked(pkg: String): TempBlock? {
        val block = tempBlocks[pkg] ?: return null
        return if (block.isExpired) {
            tempBlocks.remove(pkg)
            strikes.remove(pkg)
            null
        } else block
    }

    @Synchronized
    private fun recordAiDetection(pkg: String, durationMs: Long): Boolean {
        val lastStrike = strikeTime[pkg] ?: 0L
        if (System.currentTimeMillis() - lastStrike > STRIKE_RESET_MS) {
            strikes[pkg] = 0
        }
        val count = (strikes[pkg] ?: 0) + 1
        strikes[pkg] = count
        strikeTime[pkg] = System.currentTimeMillis()
        Log.d(TAG, "AI Strike $count/$STRIKE_THRESHOLD for $pkg")

        return if (count >= STRIKE_THRESHOLD) {
            tempBlocks[pkg] = TempBlock(pkg, System.currentTimeMillis(), durationMs)
            strikes[pkg] = 0
            strikeTime.remove(pkg)
            true
        } else false
    }

    @Synchronized
    private fun getStrikeCount(pkg: String): Int = strikes[pkg] ?: 0

    private fun launchOverlay(
        context: Context,
        pkg: String,
        reason: GuardianBlockReason,
        detail: String
    ) {
        runCatching {
            context.startActivity(
                Intent(context, GuardianBlockOverlayActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(GuardianBlockOverlayActivity.EXTRA_PACKAGE, pkg)
                    putExtra(GuardianBlockOverlayActivity.EXTRA_REASON, reason.name)
                    putExtra(GuardianBlockOverlayActivity.EXTRA_DETAIL, detail)
                }
            )
        }.onFailure { Log.e(TAG, "Failed to launch overlay", it) }
    }
}

enum class GuardianBlockReason {
    APP_BLOCKED,
    KEYWORD_MATCH,
    AI_DETECTION,
    MANUAL,
    SCHEDULE_BLOCKED
}

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
