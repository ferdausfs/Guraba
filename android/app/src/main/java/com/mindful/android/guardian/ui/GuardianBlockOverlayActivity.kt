package com.mindful.android.guardian.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mindful.android.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Full-screen overlay shown when an app is blocked by Guardian.
 * Uses a simple XML layout embedded programmatically so it works
 * without adding new layout files (the activity draws its own view).
 */
@AndroidEntryPoint
class GuardianBlockOverlayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pkg    = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        val reason = intent.getStringExtra(EXTRA_REASON) ?: "BLOCKED"
        val detail = intent.getStringExtra(EXTRA_DETAIL) ?: ""

        // Build a simple layout programmatically
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
            setPadding(64, 64, 64, 64)
        }

        val icon = TextView(this).apply {
            text = "🛡️"
            textSize = 64f
            gravity = android.view.Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "Blocked by Guardian"
            textSize = 22f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 32, 0, 8)
        }

        val reasonTv = TextView(this).apply {
            text = formatReason(reason, pkg)
            textSize = 14f
            setTextColor(android.graphics.Color.LTGRAY)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }

        val detailTv = TextView(this).apply {
            text = if (detail.isNotBlank()) detail else ""
            textSize = 12f
            setTextColor(android.graphics.Color.GRAY)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val goHomeBtn = Button(this).apply {
            text = "Go Home"
            setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        }

        root.addView(icon)
        root.addView(title)
        root.addView(reasonTv)
        if (detail.isNotBlank()) root.addView(detailTv)
        root.addView(goHomeBtn)

        setContentView(root)
    }

    private fun formatReason(reason: String, pkg: String): String = when (reason) {
        "APP_BLOCKED"      -> "This app is blocked"
        "KEYWORD_MATCH"    -> "Blocked: inappropriate keyword detected"
        "AI_DETECTION"     -> "Blocked: inappropriate content detected"
        "SCHEDULE_BLOCKED" -> "Blocked: outside allowed schedule"
        else               -> "Content blocked"
    }

    override fun onBackPressed() {
        // Prevent back press from bypassing the block
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_REASON  = "extra_reason"
        const val EXTRA_DETAIL  = "extra_detail"
    }
}
