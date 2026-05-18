package com.guraba.app.guardian.blocker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.guraba.app.R

class GuardianBlockOverlayActivity : Activity() {

    companion object {
        const val EXTRA_PACKAGE = "pkg"
        const val EXTRA_REASON = "reason"
        const val EXTRA_DETAIL = "detail"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_block_overlay)

        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        val reason = intent.getStringExtra(EXTRA_REASON) ?: ""
        val detail = intent.getStringExtra(EXTRA_DETAIL) ?: ""

        findViewById<TextView>(R.id.txtPackage).text = getAppName(pkg)
        findViewById<TextView>(R.id.txtReason).text = buildReasonText(reason, detail)

        findViewById<Button>(R.id.btnHome).setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            finish()
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun getAppName(pkg: String): String {
        return runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        }.getOrDefault(pkg)
    }

    private fun buildReasonText(reason: String, detail: String): String {
        return when (reason) {
            "AI_DETECTION" -> if (detail.startsWith("temp_block:"))
                "⛔ AI detected unsafe content\n🔒 App blocked for ${detail.removePrefix("temp_block:")}"
            else "⚠️ AI detected unsafe content"
            "KEYWORD_MATCH" -> "🔤 Keyword matched: $detail"
            "APP_BLOCKED" -> "🚫 This app is blocked"
            "SCHEDULE_BLOCKED" -> "⏰ Outside allowed schedule"
            else -> "🛡️ Access blocked by Guraba"
        }
    }
}
