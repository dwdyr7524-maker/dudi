package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object MessageStatus {
    const val PREPARED = "PREPARED"
    const val SENT = "SENT"
    const val RECEIVED = "RECEIVED"
    const val FAILED = "FAILED"
}

object ConnectionMode {
    const val FREE_ACCOUNT = "FREE_ACCOUNT"
    const val PAID_API = "PAID_API"
}

object ResponseLanguage {
    const val AUTO = "AUTO"
    const val HEBREW = "HEBREW"
    const val ENGLISH = "ENGLISH"

    fun hasHebrewCharacters(text: String): Boolean {
        return text.any { ch -> ch in '\u0590'..'\u05FF' }
    }

    fun resolveLabel(code: String): String = when (code) {
        HEBREW -> "עברית"
        ENGLISH -> "אנגלית"
        else -> "זיהוי אוטומטי"
    }
}

object NotificationType {
    const val NEW_MESSAGE = "NEW_MESSAGE"
    const val SEND_REMINDER = "SEND_REMINDER"
    const val CONNECTION_ERROR = "CONNECTION_ERROR"
    const val TASK_DUE = "TASK_DUE"
    const val ACTION_SUCCESS = "ACTION_SUCCESS"
    const val ACTION_FAILURE = "ACTION_FAILURE"
}

object ActionType {
    const val PREPARED = "PREPARED"
    const val SENT = "SENT"
    const val REPLY_RECEIVED = "REPLY_RECEIVED"
    const val FAILED = "FAILED"
    const val REMINDER_FIRED = "REMINDER_FIRED"
    const val TASK_COMPLETED = "TASK_COMPLETED"
}

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val model: String,
    val isScheduledThread: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversation_id"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "conversation_id")
    val conversationId: Long,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val isScheduled: Boolean = false,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val status: String = MessageStatus.SENT
)

data class ConversationWithSummary(
    val id: Long,
    val title: String,
    val model: String,
    val isScheduledThread: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int,
    val lastMessagePreview: String?
)

@Entity(tableName = "scheduled_prompts")
data class ScheduledPromptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val promptText: String,
    val intervalHours: Int = 5,
    val activeStartHour: Int = 8,
    val activeEndHour: Int = 23,
    val isEnabled: Boolean = false,
    val isPaused: Boolean = false,
    val targetConversationId: Long = -1L,
    val lastRunAt: Long = 0L,
    val nextRunAt: Long = 0L,
    val lastStatus: String = "",
    val lastExecutionKey: String = ""
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val promptText: String,
    val dueAt: Long,
    val remindEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_items")
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val relatedConversationId: Long = -1L,
    val relatedTaskId: Long = -1L
)

@Entity(tableName = "action_logs")
data class ActionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val actionType: String,
    val mode: String,
    val summary: String,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
