package com.example.data.backup

import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.ScheduledPromptEntity
import com.example.data.local.TaskEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val conversations: List<BackupConversation> = emptyList(),
    val messages: List<BackupMessage> = emptyList(),
    val tasks: List<BackupTask> = emptyList(),
    val schedules: List<BackupSchedule> = emptyList()
)

@Serializable
data class BackupConversation(
    val id: Long,
    val title: String,
    val model: String,
    val isScheduledThread: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupMessage(
    val conversationId: Long,
    val role: String,
    val content: String,
    val timestamp: Long,
    val isError: Boolean,
    val isScheduled: Boolean,
    val inputTokens: Int,
    val outputTokens: Int,
    val status: String
)

@Serializable
data class BackupTask(
    val title: String,
    val promptText: String,
    val dueAt: Long,
    val remindEnabled: Boolean,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupSchedule(
    val title: String,
    val promptText: String,
    val intervalHours: Int,
    val activeStartHour: Int,
    val activeEndHour: Int,
    val isEnabled: Boolean,
    val isPaused: Boolean
)

object BackupSerializer {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun encode(
        conversations: List<ConversationEntity>,
        messages: List<MessageEntity>,
        tasks: List<TaskEntity>,
        schedules: List<ScheduledPromptEntity>
    ): String {
        val payload = BackupPayload(
            conversations = conversations.map {
                BackupConversation(
                    id = it.id,
                    title = it.title,
                    model = it.model,
                    isScheduledThread = it.isScheduledThread,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            messages = messages.map {
                BackupMessage(
                    conversationId = it.conversationId,
                    role = it.role,
                    content = it.content,
                    timestamp = it.timestamp,
                    isError = it.isError,
                    isScheduled = it.isScheduled,
                    inputTokens = it.inputTokens,
                    outputTokens = it.outputTokens,
                    status = it.status
                )
            },
            tasks = tasks.map {
                BackupTask(
                    title = it.title,
                    promptText = it.promptText,
                    dueAt = it.dueAt,
                    remindEnabled = it.remindEnabled,
                    isCompleted = it.isCompleted,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            schedules = schedules.map {
                BackupSchedule(
                    title = it.title,
                    promptText = it.promptText,
                    intervalHours = it.intervalHours,
                    activeStartHour = it.activeStartHour,
                    activeEndHour = it.activeEndHour,
                    isEnabled = it.isEnabled,
                    isPaused = it.isPaused
                )
            }
        )
        return json.encodeToString(payload)
    }

    fun decode(rawJson: String): BackupPayload {
        return json.decodeFromString(rawJson)
    }
}
