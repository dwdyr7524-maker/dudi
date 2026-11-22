package com.example.data.repository

import com.example.data.backup.BackupSerializer
import com.example.data.local.ActionLogEntity
import com.example.data.local.ActionType
import com.example.data.local.AppNotificationEntity
import com.example.data.local.ChatDao
import com.example.data.local.ConnectionMode
import com.example.data.local.ControlCenterDao
import com.example.data.local.ConversationEntity
import com.example.data.local.ConversationWithSummary
import com.example.data.local.MessageEntity
import com.example.data.local.MessageStatus
import com.example.data.local.ResponseLanguage
import com.example.data.local.ScheduledPromptEntity
import com.example.data.local.TaskEntity
import com.example.data.preferences.AppSettings
import com.example.data.preferences.SettingsDataStore
import com.example.data.remote.AnthropicClientFactory
import com.example.data.remote.AnthropicErrorMapper
import com.example.data.remote.AnthropicMessageParam
import com.example.data.remote.AnthropicMessageRequest
import com.example.data.remote.StreamingOutcome
import com.example.data.security.ApiKeyStore
import com.example.notifications.ClaudeNotificationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.system.measureTimeMillis

sealed interface SendMessageResult {
    data class Success(
        val conversationId: Long,
        val replyText: String,
        val inputTokens: Int,
        val outputTokens: Int
    ) : SendMessageResult

    data class Failure(
        val conversationId: Long?,
        val errorMessage: String,
        val isTransient: Boolean = false
    ) : SendMessageResult
}

sealed interface ConnectionTestResult {
    data class Success(val modelUsed: String, val latencyMs: Long) : ConnectionTestResult
    data class Failure(val errorMessage: String) : ConnectionTestResult
}

class ClaudeRepository(
    private val chatDao: ChatDao,
    private val controlCenterDao: ControlCenterDao,
    private val apiKeyStore: ApiKeyStore,
    private val settingsDataStore: SettingsDataStore,
    private val clientFactory: AnthropicClientFactory,
    private val notificationManager: ClaudeNotificationManager
) {
    val conversationsFlow: Flow<List<ConversationWithSummary>> = chatDao.observeConversations()
    val settingsFlow: Flow<AppSettings> = settingsDataStore.settingsFlow
    val hasApiKeyFlow: StateFlow<Boolean> = apiKeyStore.hasApiKeyFlow
    val maskedApiKeyFlow: StateFlow<String> = apiKeyStore.maskedApiKeyFlow

    val scheduledPromptsFlow: Flow<List<ScheduledPromptEntity>> = controlCenterDao.observeScheduledPrompts()
    val tasksFlow: Flow<List<TaskEntity>> = controlCenterDao.observeTasks()
    val notificationsFlow: Flow<List<AppNotificationEntity>> = controlCenterDao.observeNotifications()
    val unreadNotificationCountFlow: Flow<Int> = controlCenterDao.observeUnreadNotificationCount()
    val actionLogsFlow: Flow<List<ActionLogEntity>> = controlCenterDao.observeActionLogs()

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        chatDao.observeMessages(conversationId)

    fun observeMatchingConversationIds(query: String): Flow<List<Long>> =
        chatDao.observeMatchingConversationIds(query.trim())

    suspend fun getLatestConversationId(): Long? =
        chatDao.getAllConversationsSync().firstOrNull()?.id

    suspend fun createNewConversation(title: String = "שיחה חדשה"): Long {
        val settings = settingsDataStore.getCurrentSettings()
        val now = System.currentTimeMillis()
        return chatDao.insertConversation(
            ConversationEntity(
                title = title.trim().ifEmpty { "שיחה חדשה" },
                model = if (settings.isPaidApiReady) settings.selectedModel else "חשבון Claude חינמי",
                isScheduledThread = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun renameConversation(conversationId: Long, newTitle: String) {
        val cleaned = newTitle.trim()
        if (cleaned.isNotEmpty()) {
            chatDao.renameConversation(conversationId, cleaned)
        }
    }

    suspend fun deleteConversation(conversationId: Long) {
        chatDao.deleteConversation(conversationId)
    }

    suspend fun prepareMessageForFreeClaude(
        conversationId: Long?,
        promptText: String,
        isScheduled: Boolean = false
    ): Pair<Long, Long> {
        val cleaned = promptText.trim()
        val activeId = resolveConversationId(
            requestedId = conversationId,
            promptText = cleaned,
            model = "חשבון Claude חינמי",
            isScheduled = isScheduled
        )
        val now = System.currentTimeMillis()
        val messageId = chatDao.insertMessage(
            MessageEntity(
                conversationId = activeId,
                role = "user",
                content = cleaned,
                timestamp = now,
                isError = false,
                isScheduled = isScheduled,
                status = MessageStatus.PREPARED
            )
        )
        chatDao.touchConversation(activeId, "חשבון Claude חינמי", now)
        controlCenterDao.insertActionLog(
            ActionLogEntity(
                actionType = ActionType.PREPARED,
                mode = ConnectionMode.FREE_ACCOUNT,
                summary = "הוכנה הודעה: ${cleaned.take(60)}",
                timestamp = now
            )
        )
        return activeId to messageId
    }

    suspend fun markPreparedMessageSent(messageId: Long) {
        val msg = chatDao.getMessageById(messageId) ?: return
        if (msg.status == MessageStatus.SENT) return
        chatDao.updateMessageStatus(messageId, MessageStatus.SENT)
        val now = System.currentTimeMillis()
        controlCenterDao.insertActionLog(
            ActionLogEntity(
                actionType = ActionType.SENT,
                mode = ConnectionMode.FREE_ACCOUNT,
                summary = "אושרה שליחה בחשבון החינמי: ${msg.content.take(60)}",
                timestamp = now
            )
        )
        notificationManager.notifyActionSuccess(
            title = "ההודעה סומנה כנשלחה",
            message = msg.content.take(100),
            conversationId = msg.conversationId,
            showSystemNotification = false
        )
    }

    suspend fun saveManualClaudeReply(conversationId: Long, replyText: String) {
        val cleaned = replyText.trim()
        if (cleaned.isEmpty()) return
        val now = System.currentTimeMillis()
        chatDao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = "assistant",
                content = cleaned,
                timestamp = now,
                isError = false,
                isScheduled = false,
                status = MessageStatus.RECEIVED
            )
        )
        chatDao.touchConversation(conversationId, "חשבון Claude חינמי", now)
        controlCenterDao.insertActionLog(
            ActionLogEntity(
                actionType = ActionType.REPLY_RECEIVED,
                mode = ConnectionMode.FREE_ACCOUNT,
                summary = "נשמרה תשובה מ-Claude: ${cleaned.take(60)}",
                timestamp = now
            )
        )
    }

    suspend fun updateConnectionMode(mode: String, paidApproved: Boolean) {
        settingsDataStore.updateConnectionMode(mode, paidApproved)
    }

    suspend fun setResponseLanguage(language: String) {
        settingsDataStore.updateResponseLanguage(language)
    }

    fun saveApiKey(rawKey: String) = apiKeyStore.saveApiKey(rawKey)

    fun clearApiKey() = apiKeyStore.clearApiKey()

    fun getRevealedApiKey(): String? = apiKeyStore.getApiKey()

    suspend fun saveConnectionSettings(
        baseUrl: String,
        anthropicVersion: String,
        selectedModel: String,
        systemPrompt: String,
        maxTokens: Int,
        temperature: Float
    ) {
        settingsDataStore.updateConnectionSettings(
            baseUrl = baseUrl,
            anthropicVersion = anthropicVersion,
            selectedModel = selectedModel,
            systemPrompt = systemPrompt,
            maxTokens = maxTokens,
            temperature = temperature
        )
    }

    suspend fun updateAutoSendConfig(
        enabled: Boolean,
        paused: Boolean,
        intervalHours: Int,
        activeStartHour: Int,
        activeEndHour: Int,
        prompt: String,
        targetConversationId: Long,
        nextRunTimestamp: Long = 0L
    ) {
        settingsDataStore.updateAutoSendConfig(
            enabled = enabled,
            paused = paused,
            intervalHours = intervalHours,
            activeStartHour = activeStartHour,
            activeEndHour = activeEndHour,
            prompt = prompt,
            targetConversationId = targetConversationId,
            nextRunTimestamp = nextRunTimestamp
        )
    }

    suspend fun updateNotificationPreferences(
        notifyOnReplies: Boolean,
        notifyOnErrors: Boolean,
        notifyTaskReminders: Boolean
    ) {
        settingsDataStore.updateNotificationPreferences(
            notifyOnReplies = notifyOnReplies,
            notifyOnErrors = notifyOnErrors,
            notifyTaskReminders = notifyTaskReminders
        )
    }

    suspend fun setMonthlyBudgetUsd(budgetUsd: Float) {
        settingsDataStore.updateMonthlyBudgetUsd(budgetUsd)
    }

    suspend fun recordAutoSendRun(timestamp: Long, nextRunTimestamp: Long, statusMessage: String) {
        settingsDataStore.recordAutoSendRun(timestamp, nextRunTimestamp, statusMessage)
    }

    // Scheduled Prompts CRUD
    suspend fun saveScheduledPrompt(prompt: ScheduledPromptEntity): Long {
        return controlCenterDao.insertScheduledPrompt(prompt)
    }

    suspend fun updateScheduledPrompt(prompt: ScheduledPromptEntity) {
        controlCenterDao.updateScheduledPrompt(prompt)
    }

    suspend fun deleteScheduledPrompt(id: Long) {
        controlCenterDao.deleteScheduledPrompt(id)
    }

    suspend fun getScheduledPromptById(id: Long): ScheduledPromptEntity? {
        return controlCenterDao.getScheduledPromptById(id)
    }

    suspend fun getScheduledPromptsSync(): List<ScheduledPromptEntity> {
        return controlCenterDao.getScheduledPromptsSync()
    }

    // Tasks CRUD
    suspend fun saveTask(task: TaskEntity): Long {
        return controlCenterDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        controlCenterDao.updateTask(task)
    }

    suspend fun deleteTask(id: Long) {
        controlCenterDao.deleteTask(id)
    }

    suspend fun getTaskById(id: Long): TaskEntity? = controlCenterDao.getTaskById(id)

    suspend fun getTasksSync(): List<TaskEntity> = controlCenterDao.getTasksSync()

    suspend fun logTaskCompleted(task: TaskEntity) {
        controlCenterDao.insertActionLog(
            ActionLogEntity(
                actionType = ActionType.TASK_COMPLETED,
                mode = ConnectionMode.FREE_ACCOUNT,
                summary = "משימה הושלמה: ${task.title}",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // Notifications CRUD
    suspend fun markNotificationRead(id: Long) = controlCenterDao.markNotificationRead(id)

    suspend fun markAllNotificationsRead() = controlCenterDao.markAllNotificationsRead()

    suspend fun deleteNotification(id: Long) = controlCenterDao.deleteNotification(id)

    suspend fun clearAllNotifications() = controlCenterDao.deleteAllNotifications()

    suspend fun logAction(
        actionType: String,
        mode: String,
        summary: String,
        inputTokens: Int = 0,
        outputTokens: Int = 0
    ) {
        controlCenterDao.insertActionLog(
            ActionLogEntity(
                actionType = actionType,
                mode = mode,
                summary = summary,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun sendMessage(
        conversationId: Long?,
        promptText: String,
        isScheduled: Boolean = false,
        notifyEvenIfForeground: Boolean = false
    ): SendMessageResult {
        val cleanedPrompt = promptText.trim()
        if (cleanedPrompt.isEmpty()) {
            return SendMessageResult.Failure(conversationId, "לא ניתן לשלוח הודעה ריקה.")
        }

        val settings = settingsDataStore.getCurrentSettings()
        if (!settings.isPaidApiReady) {
            val errorMsg = "חיבור API בתשלום אינו מאושר. במצב חינמי יש להכין ולשלוח ידנית דרך Claude."
            return SendMessageResult.Failure(conversationId, errorMsg)
        }

        val apiKey = apiKeyStore.getApiKey()
        if (apiKey.isNullOrBlank()) {
            val errorMsg = "מפתח Anthropic API חסר. יש להגדיר מפתח מאובטח במסך ההגדרות."
            if (isScheduled && settings.notifyOnErrors) {
                notificationManager.notifyError(conversationId ?: -1L, errorMsg)
            }
            return SendMessageResult.Failure(conversationId, errorMsg)
        }

        val activeId = resolveConversationId(conversationId, cleanedPrompt, settings.selectedModel, isScheduled)
        val now = System.currentTimeMillis()

        chatDao.insertMessage(
            MessageEntity(
                conversationId = activeId,
                role = "user",
                content = cleanedPrompt,
                timestamp = now,
                isError = false,
                isScheduled = isScheduled,
                status = MessageStatus.SENT
            )
        )
        chatDao.touchConversation(activeId, settings.selectedModel, now)
        logAction(ActionType.SENT, ConnectionMode.PAID_API, "נשלחה הודעה ב-API: ${cleanedPrompt.take(55)}")

        val apiMessages = buildNormalizedApiMessages(chatDao.getValidMessagesForApi(activeId))
        val effectiveSystem = buildEffectiveSystemPrompt(
            basePrompt = settings.systemPrompt,
            responseLanguage = settings.responseLanguage,
            latestUserPrompt = cleanedPrompt
        )
        val request = AnthropicMessageRequest(
            model = settings.selectedModel,
            maxTokens = settings.maxTokens,
            messages = apiMessages,
            system = effectiveSystem.takeIf { it.isNotBlank() },
            temperature = settings.temperature
        )

        return try {
            val response = clientFactory.getService(settings.baseUrl).createMessage(
                apiKey = apiKey,
                anthropicVersion = settings.anthropicVersion,
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                val replyText = body?.content
                    ?.filter { it.type == "text" && !it.text.isNullOrBlank() }
                    ?.joinToString("\n\n") { it.text.orEmpty() }
                    ?.trim()
                    .orEmpty()

                if (replyText.isEmpty()) {
                    val emptyMsg = "התקבלה תשובה ריקה ממודל קלוד."
                    saveErrorBubble(activeId, emptyMsg, isScheduled)
                    logAction(ActionType.FAILED, ConnectionMode.PAID_API, emptyMsg)
                    if (settings.notifyOnErrors) {
                        notificationManager.notifyError(activeId, emptyMsg)
                    }
                    SendMessageResult.Failure(activeId, emptyMsg, isTransient = false)
                } else {
                    val inputTokens = body?.usage?.inputTokens ?: 0
                    val outputTokens = body?.usage?.outputTokens ?: 0
                    val replyTime = System.currentTimeMillis()
                    chatDao.insertMessage(
                        MessageEntity(
                            conversationId = activeId,
                            role = "assistant",
                            content = replyText,
                            timestamp = replyTime,
                            isError = false,
                            isScheduled = isScheduled,
                            inputTokens = inputTokens,
                            outputTokens = outputTokens,
                            status = MessageStatus.RECEIVED
                        )
                    )
                    chatDao.touchConversation(activeId, settings.selectedModel, replyTime)
                    logAction(
                        actionType = ActionType.REPLY_RECEIVED,
                        mode = ConnectionMode.PAID_API,
                        summary = "התקבלה תשובה (${outputTokens} אסימונים)",
                        inputTokens = inputTokens,
                        outputTokens = outputTokens
                    )

                    if (settings.notifyOnReplies && (isScheduled || notifyEvenIfForeground)) {
                        notificationManager.notifyReply(
                            conversationId = activeId,
                            replyPreview = replyText,
                            isScheduled = isScheduled
                        )
                    }

                    SendMessageResult.Success(
                        conversationId = activeId,
                        replyText = replyText,
                        inputTokens = inputTokens,
                        outputTokens = outputTokens
                    )
                }
            } else {
                val rawErr = response.errorBody()?.string()
                val code = response.code()
                val mapped = AnthropicErrorMapper.mapHttpError(
                    code = code,
                    rawErrorBody = rawErr,
                    moshi = clientFactory.moshi
                )
                saveErrorBubble(activeId, mapped, isScheduled)
                logAction(ActionType.FAILED, ConnectionMode.PAID_API, mapped)
                if (settings.notifyOnErrors) {
                    notificationManager.notifyError(activeId, mapped)
                }
                val transient = code in listOf(429, 500, 502, 503, 529)
                SendMessageResult.Failure(activeId, mapped, isTransient = transient)
            }
        } catch (t: Throwable) {
            val mapped = AnthropicErrorMapper.mapException(t)
            saveErrorBubble(activeId, mapped, isScheduled)
            logAction(ActionType.FAILED, ConnectionMode.PAID_API, mapped)
            if (settings.notifyOnErrors) {
                notificationManager.notifyError(activeId, mapped)
            }
            SendMessageResult.Failure(activeId, mapped, isTransient = true)
        }
    }

    suspend fun sendMessageStreaming(
        conversationId: Long?,
        promptText: String,
        isScheduled: Boolean = false,
        notifyEvenIfForeground: Boolean = false,
        onPartialText: suspend (String) -> Unit
    ): SendMessageResult {
        val cleanedPrompt = promptText.trim()
        if (cleanedPrompt.isEmpty()) {
            return SendMessageResult.Failure(conversationId, "לא ניתן לשלוח הודעה ריקה.")
        }

        val settings = settingsDataStore.getCurrentSettings()
        if (!settings.isPaidApiReady) {
            val errorMsg = "חיבור API בתשלום אינו מאושר. במצב חינמי יש להכין ולשלוח ידנית דרך Claude."
            return SendMessageResult.Failure(conversationId, errorMsg)
        }

        val apiKey = apiKeyStore.getApiKey()
        if (apiKey.isNullOrBlank()) {
            val errorMsg = "מפתח Anthropic API חסר. יש להגדיר מפתח מאובטח במסך ההגדרות."
            if (isScheduled && settings.notifyOnErrors) {
                notificationManager.notifyError(conversationId ?: -1L, errorMsg)
            }
            return SendMessageResult.Failure(conversationId, errorMsg)
        }

        val activeId = resolveConversationId(conversationId, cleanedPrompt, settings.selectedModel, isScheduled)
        val now = System.currentTimeMillis()

        chatDao.insertMessage(
            MessageEntity(
                conversationId = activeId,
                role = "user",
                content = cleanedPrompt,
                timestamp = now,
                isError = false,
                isScheduled = isScheduled,
                status = MessageStatus.SENT
            )
        )
        chatDao.touchConversation(activeId, settings.selectedModel, now)
        logAction(ActionType.SENT, ConnectionMode.PAID_API, "נשלחה הודעה ב-API: ${cleanedPrompt.take(55)}")

        val apiMessages = buildNormalizedApiMessages(chatDao.getValidMessagesForApi(activeId))
        val effectiveSystem = buildEffectiveSystemPrompt(
            basePrompt = settings.systemPrompt,
            responseLanguage = settings.responseLanguage,
            latestUserPrompt = cleanedPrompt
        )
        val request = AnthropicMessageRequest(
            model = settings.selectedModel,
            maxTokens = settings.maxTokens,
            messages = apiMessages,
            system = effectiveSystem.takeIf { it.isNotBlank() },
            temperature = settings.temperature,
            stream = true
        )

        return try {
            when (
                val outcome = clientFactory.streamMessage(
                    baseUrl = settings.baseUrl,
                    apiKey = apiKey,
                    anthropicVersion = settings.anthropicVersion,
                    request = request,
                    onPartialText = onPartialText
                )
            ) {
                is StreamingOutcome.Success -> {
                    val replyTime = System.currentTimeMillis()
                    chatDao.insertMessage(
                        MessageEntity(
                            conversationId = activeId,
                            role = "assistant",
                            content = outcome.replyText,
                            timestamp = replyTime,
                            isError = false,
                            isScheduled = isScheduled,
                            inputTokens = outcome.inputTokens,
                            outputTokens = outcome.outputTokens,
                            status = MessageStatus.RECEIVED
                        )
                    )
                    chatDao.touchConversation(activeId, settings.selectedModel, replyTime)
                    logAction(
                        actionType = ActionType.REPLY_RECEIVED,
                        mode = ConnectionMode.PAID_API,
                        summary = "התקבלה תשובה בזמן אמת (${outcome.outputTokens} אסימונים)",
                        inputTokens = outcome.inputTokens,
                        outputTokens = outcome.outputTokens
                    )
                    if (settings.notifyOnReplies && (isScheduled || notifyEvenIfForeground)) {
                        notificationManager.notifyReply(
                            conversationId = activeId,
                            replyPreview = outcome.replyText,
                            isScheduled = isScheduled
                        )
                    }
                    SendMessageResult.Success(
                        conversationId = activeId,
                        replyText = outcome.replyText,
                        inputTokens = outcome.inputTokens,
                        outputTokens = outcome.outputTokens
                    )
                }

                is StreamingOutcome.HttpFailure -> {
                    val mapped = AnthropicErrorMapper.mapHttpError(
                        code = outcome.code,
                        rawErrorBody = outcome.rawErrorBody,
                        moshi = clientFactory.moshi
                    )
                    saveErrorBubble(activeId, mapped, isScheduled)
                    logAction(ActionType.FAILED, ConnectionMode.PAID_API, mapped)
                    if (settings.notifyOnErrors) {
                        notificationManager.notifyError(activeId, mapped)
                    }
                    val transient = outcome.code in listOf(429, 500, 502, 503, 529)
                    SendMessageResult.Failure(activeId, mapped, isTransient = transient)
                }

                is StreamingOutcome.Error -> {
                    saveErrorBubble(activeId, outcome.message, isScheduled)
                    logAction(ActionType.FAILED, ConnectionMode.PAID_API, outcome.message)
                    if (settings.notifyOnErrors) {
                        notificationManager.notifyError(activeId, outcome.message)
                    }
                    SendMessageResult.Failure(activeId, outcome.message, isTransient = outcome.isTransient)
                }
            }
        } catch (t: Throwable) {
            val mapped = AnthropicErrorMapper.mapException(t)
            saveErrorBubble(activeId, mapped, isScheduled)
            logAction(ActionType.FAILED, ConnectionMode.PAID_API, mapped)
            if (settings.notifyOnErrors) {
                notificationManager.notifyError(activeId, mapped)
            }
            SendMessageResult.Failure(activeId, mapped, isTransient = true)
        }
    }

    suspend fun testConnection(
        baseUrlOverride: String? = null,
        anthropicVersionOverride: String? = null,
        modelOverride: String? = null
    ): ConnectionTestResult {
        val settings = settingsDataStore.getCurrentSettings()
        if (!settings.isPaidApiReady) {
            return ConnectionTestResult.Failure("יש לאשר שימוש ב-API בתשלום לפני בדיקת החיבור.")
        }
        val apiKey = apiKeyStore.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return ConnectionTestResult.Failure("לא שמור מפתח Anthropic API במכשיר.")
        }

        val targetBaseUrl = SettingsDataStore.normalizeBaseUrl(baseUrlOverride ?: settings.baseUrl)
        val targetVersion = (anthropicVersionOverride ?: settings.anthropicVersion)
            .trim()
            .ifEmpty { AppSettings.DEFAULT_ANTHROPIC_VERSION }
        val targetModel = (modelOverride ?: settings.selectedModel)
            .trim()
            .ifEmpty { AppSettings.DEFAULT_MODEL }

        return try {
            var responseResult: retrofit2.Response<com.example.data.remote.AnthropicMessageResponse>? = null
            val latencyMs = measureTimeMillis {
                responseResult = clientFactory.getService(targetBaseUrl).createMessage(
                    apiKey = apiKey,
                    anthropicVersion = targetVersion,
                    request = AnthropicMessageRequest(
                        model = targetModel,
                        maxTokens = 16,
                        messages = listOf(AnthropicMessageParam(role = "user", content = "שלום"))
                    )
                )
            }

            val response = responseResult
            if (response != null && response.isSuccessful) {
                ConnectionTestResult.Success(
                    modelUsed = response.body()?.model ?: targetModel,
                    latencyMs = latencyMs
                )
            } else {
                val errBody = response?.errorBody()?.string()
                val mapped = AnthropicErrorMapper.mapHttpError(
                    code = response?.code() ?: 0,
                    rawErrorBody = errBody,
                    moshi = clientFactory.moshi
                )
                if (settings.notifyOnErrors) {
                    notificationManager.notifyError(-1L, mapped)
                }
                ConnectionTestResult.Failure(mapped)
            }
        } catch (t: Throwable) {
            val mapped = AnthropicErrorMapper.mapException(t)
            if (settings.notifyOnErrors) {
                notificationManager.notifyError(-1L, mapped)
            }
            ConnectionTestResult.Failure(mapped)
        }
    }

    suspend fun exportBackupJson(): String {
        val conversations = chatDao.getAllConversationsSync()
        val messages = chatDao.getAllMessagesSync()
        val tasks = controlCenterDao.getTasksSync()
        val schedules = controlCenterDao.getScheduledPromptsSync()
        return BackupSerializer.encode(conversations, messages, tasks, schedules)
    }

    suspend fun restoreBackupJson(rawJson: String): Int {
        val payload = BackupSerializer.decode(rawJson)
        val oldToNewConvoId = mutableMapOf<Long, Long>()
        for (c in payload.conversations) {
            val newId = chatDao.insertConversation(
                ConversationEntity(
                    title = c.title,
                    model = c.model,
                    isScheduledThread = c.isScheduledThread,
                    createdAt = c.createdAt,
                    updatedAt = c.updatedAt
                )
            )
            oldToNewConvoId[c.id] = newId
        }
        for (m in payload.messages) {
            val mappedId = oldToNewConvoId[m.conversationId] ?: continue
            chatDao.insertMessage(
                MessageEntity(
                    conversationId = mappedId,
                    role = m.role,
                    content = m.content,
                    timestamp = m.timestamp,
                    isError = m.isError,
                    isScheduled = m.isScheduled,
                    inputTokens = m.inputTokens,
                    outputTokens = m.outputTokens,
                    status = m.status
                )
            )
        }
        for (t in payload.tasks) {
            controlCenterDao.insertTask(
                TaskEntity(
                    title = t.title,
                    promptText = t.promptText,
                    dueAt = t.dueAt,
                    remindEnabled = t.remindEnabled,
                    isCompleted = t.isCompleted,
                    createdAt = t.createdAt,
                    updatedAt = t.updatedAt
                )
            )
        }
        for (s in payload.schedules) {
            controlCenterDao.insertScheduledPrompt(
                ScheduledPromptEntity(
                    title = s.title,
                    promptText = s.promptText,
                    intervalHours = s.intervalHours,
                    activeStartHour = s.activeStartHour,
                    activeEndHour = s.activeEndHour,
                    isEnabled = false,
                    isPaused = true
                )
            )
        }
        return payload.conversations.size + payload.tasks.size + payload.schedules.size
    }

    suspend fun stopAllActivity() {
        val current = settingsDataStore.getCurrentSettings()
        settingsDataStore.updateAutoSendConfig(
            enabled = false,
            paused = true,
            intervalHours = current.autoSendIntervalHours,
            activeStartHour = current.activeStartHour,
            activeEndHour = current.activeEndHour,
            prompt = current.autoSendPrompt,
            targetConversationId = current.autoSendConversationId,
            nextRunTimestamp = 0L
        )
        settingsDataStore.setFloatingBubbleEnabled(false)
        controlCenterDao.pauseAllSchedules()
    }

    suspend fun clearAllLocalData() {
        stopAllActivity()
        apiKeyStore.clearApiKey()
        chatDao.deleteAllConversations()
        controlCenterDao.deleteAllTasks()
        controlCenterDao.deleteAllScheduledPrompts()
        controlCenterDao.deleteAllNotifications()
        controlCenterDao.deleteAllActionLogs()
        settingsDataStore.resetAllPreferences()
    }

    private suspend fun resolveConversationId(
        requestedId: Long?,
        promptText: String,
        model: String,
        isScheduled: Boolean
    ): Long {
        if (requestedId != null && requestedId > 0L) {
            val existing = chatDao.getConversationById(requestedId)
            if (existing != null) return existing.id
        }

        if (isScheduled) {
            chatDao.getScheduledConversation()?.let { return it.id }
            val now = System.currentTimeMillis()
            return chatDao.insertConversation(
                ConversationEntity(
                    title = "שליחות אוטומטיות · Claude",
                    model = model,
                    isScheduledThread = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        val generatedTitle = promptText
            .replace('\n', ' ')
            .trim()
            .take(36)
            .ifEmpty { "שיחה חדשה" }
        val now = System.currentTimeMillis()
        return chatDao.insertConversation(
            ConversationEntity(
                title = generatedTitle,
                model = model,
                isScheduledThread = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private suspend fun saveErrorBubble(
        conversationId: Long,
        errorMessage: String,
        isScheduled: Boolean
    ) {
        val now = System.currentTimeMillis()
        chatDao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = "assistant",
                content = errorMessage,
                timestamp = now,
                isError = true,
                isScheduled = isScheduled,
                status = MessageStatus.FAILED
            )
        )
    }

    private fun buildEffectiveSystemPrompt(
        basePrompt: String,
        responseLanguage: String,
        latestUserPrompt: String
    ): String = Companion.buildEffectiveSystemPrompt(basePrompt, responseLanguage, latestUserPrompt)

    private fun buildNormalizedApiMessages(history: List<MessageEntity>): List<AnthropicMessageParam> {
        val merged = mutableListOf<AnthropicMessageParam>()
        for (item in history) {
            val role = if (item.role == "assistant") "assistant" else "user"
            if (merged.isEmpty()) {
                if (role == "user") {
                    merged.add(AnthropicMessageParam(role = "user", content = item.content))
                }
            } else {
                val last = merged.last()
                if (last.role == role) {
                    merged[merged.lastIndex] = last.copy(content = "${last.content}\n\n${item.content}")
                } else {
                    merged.add(AnthropicMessageParam(role = role, content = item.content))
                }
            }
        }
        return merged
    }

    companion object {
        fun buildEffectiveSystemPrompt(
            basePrompt: String,
            responseLanguage: String,
            latestUserPrompt: String
        ): String {
            val directive = when (responseLanguage) {
                ResponseLanguage.HEBREW ->
                    "חשוב: ענה תמיד בעברית תקינה, רהוטה ומאורגנת היטב (ניתן לשלב מונחי קוד באנגלית לפי צורך)."
                ResponseLanguage.ENGLISH ->
                    "Important: Reply in clear, well-structured English."
                else -> if (ResponseLanguage.hasHebrewCharacters(latestUserPrompt)) {
                    "המשתמש כתב בעברית. ענה בעברית טבעית, מדויקת ורהוטה, תוך שמירה על מונחים טכניים או קוד באנגלית בעת הצורך."
                } else {
                    "Reply naturally in the same language as the user's message."
                }
            }
            return listOf(basePrompt.trim(), directive)
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
        }
    }
}
