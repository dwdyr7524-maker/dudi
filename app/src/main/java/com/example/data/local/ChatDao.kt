package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query(
        """
        SELECT c.id, c.title, c.model, c.isScheduledThread, c.createdAt, c.updatedAt,
               COUNT(m.id) AS messageCount,
               (SELECT content FROM messages WHERE conversation_id = c.id ORDER BY timestamp DESC, id DESC LIMIT 1) AS lastMessagePreview
        FROM conversations c
        LEFT JOIN messages m ON c.id = m.conversation_id
        GROUP BY c.id
        ORDER BY c.updatedAt DESC
        """
    )
    fun observeConversations(): Flow<List<ConversationWithSummary>>

    @Query(
        """
        SELECT DISTINCT c.id
        FROM conversations c
        LEFT JOIN messages m ON c.id = m.conversation_id
        WHERE c.title LIKE '%' || :query || '%'
           OR m.content LIKE '%' || :query || '%'
        """
    )
    fun observeMatchingConversationIds(query: String): Flow<List<Long>>

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getAllConversationsSync(): List<ConversationEntity>

    @Query("SELECT * FROM messages ORDER BY timestamp ASC, id ASC")
    suspend fun getAllMessagesSync(): List<MessageEntity>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE isScheduledThread = 1 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getScheduledConversation(): ConversationEntity?

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY timestamp ASC, id ASC")
    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId AND isError = 0 AND status != 'PREPARED' ORDER BY timestamp ASC, id ASC")
    suspend fun getValidMessagesForApi(conversationId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Long): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: Long, status: String)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun renameConversation(conversationId: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET updatedAt = :updatedAt, model = :model WHERE id = :conversationId")
    suspend fun touchConversation(conversationId: Long, model: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: Long)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}

@Dao
interface ControlCenterDao {

    // Scheduled Prompts
    @Query("SELECT * FROM scheduled_prompts ORDER BY id ASC")
    fun observeScheduledPrompts(): Flow<List<ScheduledPromptEntity>>

    @Query("SELECT * FROM scheduled_prompts ORDER BY id ASC")
    suspend fun getScheduledPromptsSync(): List<ScheduledPromptEntity>

    @Query("SELECT * FROM scheduled_prompts WHERE id = :id LIMIT 1")
    suspend fun getScheduledPromptById(id: Long): ScheduledPromptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledPrompt(prompt: ScheduledPromptEntity): Long

    @Update
    suspend fun updateScheduledPrompt(prompt: ScheduledPromptEntity)

    @Query("DELETE FROM scheduled_prompts WHERE id = :id")
    suspend fun deleteScheduledPrompt(id: Long)

    @Query("UPDATE scheduled_prompts SET isPaused = 1 WHERE isEnabled = 1")
    suspend fun pauseAllSchedules()

    @Query("DELETE FROM scheduled_prompts")
    suspend fun deleteAllScheduledPrompts()

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueAt ASC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueAt ASC")
    suspend fun getTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    // Notification Center
    @Query("SELECT * FROM notification_items ORDER BY timestamp DESC")
    fun observeNotifications(): Flow<List<AppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM notification_items WHERE isRead = 0")
    fun observeUnreadNotificationCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(item: AppNotificationEntity): Long

    @Query("UPDATE notification_items SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: Long)

    @Query("UPDATE notification_items SET isRead = 1")
    suspend fun markAllNotificationsRead()

    @Query("DELETE FROM notification_items WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    @Query("DELETE FROM notification_items")
    suspend fun deleteAllNotifications()

    // Action Logs & Real Statistics
    @Query("SELECT * FROM action_logs ORDER BY timestamp DESC")
    fun observeActionLogs(): Flow<List<ActionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionLog(log: ActionLogEntity): Long

    @Query("DELETE FROM action_logs")
    suspend fun deleteAllActionLogs()
}
