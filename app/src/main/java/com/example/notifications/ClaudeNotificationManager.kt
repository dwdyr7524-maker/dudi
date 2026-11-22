package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppNotificationEntity
import com.example.data.local.ControlCenterDao
import com.example.data.local.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ClaudeNotificationManager(
    private val context: Context,
    private val controlCenterDao: ControlCenterDao? = null
) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val repliesChannel = NotificationChannel(
                CHANNEL_REPLIES,
                appContext.getString(R.string.channel_replies_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = appContext.getString(R.string.channel_replies_desc)
            }

            val errorsChannel = NotificationChannel(
                CHANNEL_ERRORS,
                appContext.getString(R.string.channel_errors_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = appContext.getString(R.string.channel_errors_desc)
            }

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "תזכורות ומשימות",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "תזכורות שליחה תקופתיות והתראות על משימות שהגיע זמנן"
            }

            val overlayChannel = NotificationChannel(
                CHANNEL_OVERLAY,
                "כפתור צף חכם",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "התראת שירות פעילה עבור הכפתור הצף של Claude"
            }

            manager.createNotificationChannel(repliesChannel)
            manager.createNotificationChannel(errorsChannel)
            manager.createNotificationChannel(remindersChannel)
            manager.createNotificationChannel(overlayChannel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        }
    }

    fun notifyReply(
        conversationId: Long,
        replyPreview: String,
        isScheduled: Boolean
    ) {
        val title = if (isScheduled) {
            appContext.getString(R.string.notif_auto_reply_title)
        } else {
            appContext.getString(R.string.notif_reply_title)
        }
        val cleanText = replyPreview.trim().ifEmpty { "התקבלה תשובה חדשה מ-Claude." }

        recordInCenter(
            type = NotificationType.NEW_MESSAGE,
            title = title,
            message = cleanText.take(280),
            conversationId = conversationId
        )

        postSystemNotification(
            channelId = CHANNEL_REPLIES,
            notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            title = title,
            body = cleanText,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            conversationId = conversationId
        )
    }

    fun notifySendReminder(
        conversationId: Long,
        promptTitle: String,
        promptPreview: String
    ) {
        val title = "תזכורת לשליחה · $promptTitle"
        val body = "הגיע הזמן לשלוח ל-Claude: ${promptPreview.take(160)}"

        recordInCenter(
            type = NotificationType.SEND_REMINDER,
            title = title,
            message = body,
            conversationId = conversationId
        )

        postSystemNotification(
            channelId = CHANNEL_REMINDERS,
            notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_HIGH,
            conversationId = conversationId
        )
    }

    fun notifyTaskDue(
        taskId: Long,
        taskTitle: String,
        promptPreview: String
    ) {
        val title = "משימה הגיעה לזמנה: $taskTitle"
        val body = if (promptPreview.isNotBlank()) {
            "הודעה מוכנה ל-Claude: ${promptPreview.take(140)}"
        } else {
            "פתח את מרכז המשימות לטיפול במשימה."
        }

        recordInCenter(
            type = NotificationType.TASK_DUE,
            title = title,
            message = body,
            taskId = taskId
        )

        postSystemNotification(
            channelId = CHANNEL_REMINDERS,
            notificationId = (taskId.toInt() + 5000),
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_HIGH,
            taskId = taskId
        )
    }

    fun notifyActionSuccess(
        title: String,
        message: String,
        conversationId: Long = -1L,
        showSystemNotification: Boolean = false
    ) {
        recordInCenter(
            type = NotificationType.ACTION_SUCCESS,
            title = title,
            message = message,
            conversationId = conversationId
        )
        if (showSystemNotification) {
            postSystemNotification(
                channelId = CHANNEL_REPLIES,
                notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                title = title,
                body = message,
                priority = NotificationCompat.PRIORITY_DEFAULT,
                conversationId = conversationId
            )
        }
    }

    fun notifyError(
        conversationId: Long,
        errorMessage: String,
        isConnectionError: Boolean = true
    ) {
        val cleanError = errorMessage.trim().ifEmpty { "שגיאה בתקשורת עם שרת Anthropic." }
        val title = appContext.getString(R.string.notif_error_title)

        recordInCenter(
            type = if (isConnectionError) NotificationType.CONNECTION_ERROR else NotificationType.ACTION_FAILURE,
            title = title,
            message = cleanError,
            conversationId = conversationId
        )

        postSystemNotification(
            channelId = CHANNEL_ERRORS,
            notificationId = ERROR_NOTIFICATION_ID,
            title = title,
            body = cleanError,
            priority = NotificationCompat.PRIORITY_HIGH,
            conversationId = conversationId
        )
    }

    private fun recordInCenter(
        type: String,
        title: String,
        message: String,
        conversationId: Long = -1L,
        taskId: Long = -1L
    ) {
        val dao = controlCenterDao ?: return
        scope.launch {
            dao.insertNotification(
                AppNotificationEntity(
                    type = type,
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    relatedConversationId = conversationId,
                    relatedTaskId = taskId
                )
            )
        }
    }

    private fun postSystemNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        priority: Int,
        conversationId: Long = -1L,
        taskId: Long = -1L
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        } else if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
            return
        }

        ensureChannels()

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_stat_claude)
            .setContentTitle(title)
            .setContentText(body.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.take(600)))
            .setAutoCancel(true)
            .setPriority(priority)
            .setContentIntent(createOpenIntent(conversationId, taskId))
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently; keep notification recorded in in-app center.
        }
    }

    private fun createOpenIntent(conversationId: Long, taskId: Long): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (conversationId > 0L) {
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
            }
            if (taskId > 0L) {
                putExtra(EXTRA_TASK_ID, taskId)
            }
        }
        return PendingIntent.getActivity(
            appContext,
            (conversationId * 31 + taskId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_REPLIES = "claude_replies_channel"
        const val CHANNEL_ERRORS = "claude_errors_channel"
        const val CHANNEL_REMINDERS = "claude_reminders_channel"
        const val CHANNEL_OVERLAY = "claude_overlay_channel"
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_TASK_ID = "extra_task_id"
        private const val ERROR_NOTIFICATION_ID = 9001
    }
}
