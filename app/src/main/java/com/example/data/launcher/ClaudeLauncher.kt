package com.example.data.launcher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ClaudeLaunchOutcome(
    val launchedSuccessfully: Boolean,
    val usedOfficialApp: Boolean,
    val copiedToClipboard: Boolean,
    val message: String
)

/**
 * Official zero-cost Claude launcher.
 * Copies the prepared prompt to clipboard and opens either the official Claude web prompt URL
 * (`https://claude.ai/new?q=...`) or shares directly to the installed Claude Android app.
 * Never performs fake UI click automation.
 */
object ClaudeLauncher {

    private const val CLAUDE_PACKAGE = "com.anthropic.claude"
    private const val CLAUDE_NEW_CHAT_URL = "https://claude.ai/new"

    fun isClaudeAppInstalled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    CLAUDE_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(CLAUDE_PACKAGE, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun copyToClipboard(context: Context, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Claude Prompt", text))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openClaudeWebWithPrompt(context: Context, prompt: String): ClaudeLaunchOutcome {
        val copied = copyToClipboard(context, prompt)
        val encoded = URLEncoder.encode(prompt.take(1800), StandardCharsets.UTF_8.name())
        val uri = Uri.parse("$CLAUDE_NEW_CHAT_URL?q=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ClaudeLaunchOutcome(
                launchedSuccessfully = true,
                usedOfficialApp = false,
                copiedToClipboard = copied,
                message = "Claude נפתח עם ההודעה המוכנה (וגם הועתקה ללוח). לחץ שליחה בחלון שנפתח."
            )
        } catch (_: Exception) {
            ClaudeLaunchOutcome(
                launchedSuccessfully = false,
                usedOfficialApp = false,
                copiedToClipboard = copied,
                message = if (copied) {
                    "לא נמצא דפדפן לפתיחת הקישור, אך ההודעה הועתקה ללוח."
                } else {
                    "לא ניתן היה לפתוח את Claude במכשיר זה."
                }
            )
        }
    }

    fun shareToClaudeAppOrChooser(context: Context, prompt: String): ClaudeLaunchOutcome {
        val copied = copyToClipboard(context, prompt)
        val appInstalled = isClaudeAppInstalled(context)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, prompt)
            if (appInstalled) {
                setPackage(CLAUDE_PACKAGE)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            val targetIntent = if (appInstalled) {
                sendIntent
            } else {
                Intent.createChooser(sendIntent, "שלח אל Claude").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(targetIntent)
            ClaudeLaunchOutcome(
                launchedSuccessfully = true,
                usedOfficialApp = appInstalled,
                copiedToClipboard = copied,
                message = if (appInstalled) {
                    "נפתחה אפליקציית Claude הרשמית עם ההודעה המוכנה."
                } else {
                    "נפתח תפריט השיתוף עם ההודעה המוכנה (והועתקה ללוח)."
                }
            )
        } catch (_: Exception) {
            openClaudeWebWithPrompt(context, prompt)
        }
    }

    fun shareText(context: Context, subject: String, text: String): Boolean {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, subject).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun buildConversationMarkdown(
        title: String,
        messages: List<com.example.data.local.MessageEntity>
    ): String {
        val timeFmt = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("he", "IL"))
        val sb = StringBuilder()
        sb.appendLine("# $title")
        sb.appendLine()
        messages.forEach { msg ->
            val roleLabel = if (msg.role == "user") "אתה" else "Claude"
            val timeStr = timeFmt.format(java.util.Date(msg.timestamp))
            sb.appendLine("### $roleLabel ($timeStr)")
            sb.appendLine(msg.content.trim())
            sb.appendLine()
        }
        return sb.toString().trim()
    }
}
