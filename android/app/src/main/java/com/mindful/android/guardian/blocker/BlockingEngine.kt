package com.mindful.android.guardian.blocker

import android.content.Context
import android.content.Intent
import com.mindful.android.guardian.data.GuardianPreferences
import com.mindful.android.guardian.domain.model.BlockReason
import com.mindful.android.guardian.domain.repository.RulesRepository
import com.mindful.android.guardian.ui.GuardianBlockOverlayActivity
import com.mindful.android.guardian.util.GuardianConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockingEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: RulesRepository,
    private val tempBlockManager: TempBlockManager,
    private val prefs: GuardianPreferences
) {
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val blockThrottleMap = LinkedHashMap<String, Long>()

    @Volatile private var cachedTempBlockMins: Int = 15

    init {
        ioScope.launch {
            try { prefs.tempBlockDurationMins.collect { cachedTempBlockMins = it } }
            catch (t: Throwable) { Timber.e(t) }
        }
    }

    fun block(pkg: String, reason: BlockReason, detail: String) {
        val now = System.currentTimeMillis()
        val throttleMs = when (reason) {
            BlockReason.APP_BLOCKED, BlockReason.SCHEDULE_BLOCKED -> 500L
            else -> GuardianConstants.BLOCK_THROTTLE_MS
        }
        synchronized(blockThrottleMap) {
            val last = blockThrottleMap[pkg] ?: 0L
            if (now - last < throttleMs) return
            blockThrottleMap[pkg] = now
            if (blockThrottleMap.size > GuardianConstants.MAX_THROTTLE_MAP) {
                val it = blockThrottleMap.entries.iterator(); if (it.hasNext()) { it.next(); it.remove() }
            }
        }

        val finalDetail = if (reason == BlockReason.AI_DETECTION) {
            val durationMs = cachedTempBlockMins * 60 * 1_000L
            val tempBlocked = tempBlockManager.recordAiDetection(pkg, durationMs)
            if (tempBlocked) {
                "temp_block:${tempBlockManager.isTempBlocked(pkg)?.remainingMinutes ?: cachedTempBlockMins}min"
            } else {
                "$detail (strike ${tempBlockManager.getStrikeCount(pkg)}/${GuardianConstants.STRIKE_THRESHOLD})"
            }
        } else detail

        launchOverlay(pkg, reason, finalDetail)
        logEvent(pkg, reason, finalDetail)
    }

    fun isTempBlocked(pkg: String): TempBlock? = tempBlockManager.isTempBlocked(pkg)

    private fun launchOverlay(pkg: String, reason: BlockReason, detail: String) {
        runCatching {
            context.startActivity(
                Intent(context, GuardianBlockOverlayActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(GuardianBlockOverlayActivity.EXTRA_PACKAGE, pkg)
                    putExtra(GuardianBlockOverlayActivity.EXTRA_REASON, reason.name)
                    putExtra(GuardianBlockOverlayActivity.EXTRA_DETAIL, detail)
                }
            )
        }
    }

    private fun logEvent(pkg: String, reason: BlockReason, detail: String) {
        ioScope.launch {
            runCatching { repo.insertBlockEvent(pkg, reason.name, detail.ifBlank { null }) }
        }
    }
}
