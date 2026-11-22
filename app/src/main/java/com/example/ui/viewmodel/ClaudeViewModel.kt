package com.example.ui.viewmodel

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.data.launcher.ClaudeLauncher
import com.example.data.local.ActionLogEntity
import com.example.data.local.ActionType
import com.example.data.local.AppNotificationEntity
import com.example.data.local.ConnectionMode
import com.example.data.local.ConversationWithSummary
import com.example.data.local.MessageEntity
import com.example.data.local.ResponseLanguage
import com.example.data.local.ScheduledPromptEntity
import com.example.data.local.TaskEntity
import com.example.data.network.NetworkMonitor
import com.example.data.preferences.AppSettings
import com.example.data.preferences.SettingsDataStore
import com.example.data.remote.AnthropicCostEstimator
import com.example.data.repository.ClaudeRepository
import com.example.data.repository.ConnectionTestResult
import com.example.data.repository.SendMessageResult
import com.example.overlay.FloatingBubbleService
import com.example.worker.AutoSendScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class InAppAlert(
    val title: String,
    val message: String,
    val isError: Boolean
)

data class DashboardStats(
    val preparedCount: Int = 0,
    val sentCount: Int = 0,
    val repliesCount: Int = 0,
    val failedCount: Int = 0,
    val activeTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val unreadNotificationsCount: Int = 0,
    val totalInputTokens: Int = 0,
    val totalOutputTokens: Int = 0,
    val monthlyInputTokens: Int = 0,
    val monthlyOutputTokens: Int = 0,
    val estimatedMonthlyCostUsd: Double = 0.0,
    val lastActionTimestamp: Long = 0L,
    val nextScheduledTimestamp: Long = 0L
)

sealed interface ConnectionTestUiState {
    data object Idle : ConnectionTestUiState
    data object Testing : ConnectionTestUiState
    data class Success(val model: String, val latencyMs: Long) : ConnectionTestUiState
    data class Error(val message: String) : ConnectionTestUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class ClaudeViewModel(
    private val repository: ClaudeRepository,
    private val autoSendScheduler: AutoSendScheduler,
    private val networkMonitor: NetworkMonitor,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    val conversations: StateFlow<List<ConversationWithSummary>> = repository.conversationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMessages: StateFlow<List<MessageEntity>> = _activeConversationId
        .flatMapLatest { id ->
            if (id == null || id <= 0L) flowOf(emptyList()) else repository.observeMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val hasApiKey: StateFlow<Boolean> = repository.hasApiKeyFlow
    val maskedApiKey: StateFlow<String> = repository.maskedApiKeyFlow

    val scheduledPrompts: StateFlow<List<ScheduledPromptEntity>> = repository.scheduledPromptsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.tasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<AppNotificationEntity>> = repository.notificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = repository.unreadNotificationCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val actionLogs: StateFlow<List<ActionLogEntity>> = repository.actionLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats> = combine(
        actionLogs,
        tasks,
        unreadNotificationCount,
        scheduledPrompts,
        settings
    ) { logs, taskList, unreadCount, schedules, currentSettings ->
        val nextFromSchedules = schedules
            .filter { it.isEnabled && !it.isPaused && it.nextRunAt > 0L }
            .minOfOrNull { it.nextRunAt } ?: 0L
        val nextGlobal = if (currentSettings.autoSendEnabled && !currentSettings.autoSendPaused) {
            currentSettings.nextAutoSendTimestamp
        } else {
            0L
        }
        val nextScheduled = listOf(nextFromSchedules, nextGlobal)
            .filter { it > 0L }
            .minOrNull() ?: 0L

        val lastAction = logs.maxOfOrNull { it.timestamp } ?: currentSettings.lastAutoSendTimestamp

        val nowCal = Calendar.getInstance()
        val nowYear = nowCal.get(Calendar.YEAR)
        val nowMonth = nowCal.get(Calendar.MONTH)
        val cal = Calendar.getInstance()
        val monthLogs = logs.filter { log ->
            cal.timeInMillis = log.timestamp
            cal.get(Calendar.YEAR) == nowYear && cal.get(Calendar.MONTH) == nowMonth
        }
        val monthlyIn = monthLogs.sumOf { it.inputTokens }
        val monthlyOut = monthLogs.sumOf { it.outputTokens }
        val monthlyCostUsd = AnthropicCostEstimator.estimateCostUsd(
            model = currentSettings.selectedModel,
            inputTokens = monthlyIn,
            outputTokens = monthlyOut
        )

        DashboardStats(
            preparedCount = logs.count { it.actionType == ActionType.PREPARED },
            sentCount = logs.count { it.actionType == ActionType.SENT },
            repliesCount = logs.count { it.actionType == ActionType.REPLY_RECEIVED },
            failedCount = logs.count { it.actionType == ActionType.FAILED },
            activeTasksCount = taskList.count { !it.isCompleted },
            completedTasksCount = taskList.count { it.isCompleted },
            unreadNotificationsCount = unreadCount,
            totalInputTokens = logs.sumOf { it.inputTokens },
            totalOutputTokens = logs.sumOf { it.outputTokens },
            monthlyInputTokens = monthlyIn,
            monthlyOutputTokens = monthlyOut,
            estimatedMonthlyCostUsd = monthlyCostUsd,
            lastActionTimestamp = lastAction,
            nextScheduledTimestamp = nextScheduled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    val scheduleWorkState: StateFlow<WorkInfo.State?> = autoSendScheduler.observeScheduleState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _historySearchQuery = MutableStateFlow("")
    val historyMatchingIds: StateFlow<Set<Long>?> = _historySearchQuery
        .flatMapLatest { raw ->
            val q = raw.trim()
            if (q.isEmpty()) {
                flowOf(null)
            } else {
                repository.observeMatchingConversationIds(q).map { it.toSet() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _streamingReplyText = MutableStateFlow<String?>(null)
    val streamingReplyText: StateFlow<String?> = _streamingReplyText.asStateFlow()

    private val _lastArrivedMessageId = MutableStateFlow<Long?>(null)
    val lastArrivedMessageId: StateFlow<Long?> = _lastArrivedMessageId.asStateFlow()

    private val _lastOpenedPromptForClipboard = MutableStateFlow<String?>(null)
    val lastOpenedPromptForClipboard: StateFlow<String?> = _lastOpenedPromptForClipboard.asStateFlow()

    private val _connectionTestState = MutableStateFlow<ConnectionTestUiState>(ConnectionTestUiState.Idle)
    val connectionTestState: StateFlow<ConnectionTestUiState> = _connectionTestState.asStateFlow()

    private val _inAppAlert = MutableStateFlow<InAppAlert?>(null)
    val inAppAlert: StateFlow<InAppAlert?> = _inAppAlert.asStateFlow()

    fun updateHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun selectConversation(conversationId: Long?) {
        _activeConversationId.value = conversationId
        _lastArrivedMessageId.value = null
    }

    fun startNewConversation() {
        _activeConversationId.value = null
        _lastArrivedMessageId.value = null
    }

    fun prepareDraftOnly(promptText: String) {
        val cleaned = promptText.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            val (convoId, _) = repository.prepareMessageForFreeClaude(
                conversationId = _activeConversationId.value,
                promptText = cleaned,
                isScheduled = false
            )
            _activeConversationId.value = convoId
            _inAppAlert.value = InAppAlert(
                title = "ההודעה הוכנה לשליחה",
                message = "תוכל לשלוח אותה בלחיצה אחת דרך החשבון החינמי של Claude.",
                isError = false
            )
        }
    }

    fun prepareAndOpenFreeClaude(
        context: Context,
        promptText: String,
        preferOfficialApp: Boolean = false
    ) {
        val cleaned = promptText.trim()
        if (cleaned.isEmpty()) return
        _lastOpenedPromptForClipboard.value = cleaned
        viewModelScope.launch {
            val (convoId, _) = repository.prepareMessageForFreeClaude(
                conversationId = _activeConversationId.value,
                promptText = cleaned,
                isScheduled = false
            )
            _activeConversationId.value = convoId
            val outcome = if (preferOfficialApp) {
                ClaudeLauncher.shareToClaudeAppOrChooser(context, cleaned)
            } else {
                ClaudeLauncher.openClaudeWebWithPrompt(context, cleaned)
            }
            _inAppAlert.value = InAppAlert(
                title = if (outcome.launchedSuccessfully) "Claude נפתח עם ההודעה המוכנה" else "ההודעה הועתקה ללוח",
                message = outcome.message,
                isError = !outcome.launchedSuccessfully
            )
        }
    }

    fun openExistingPreparedMessageInClaude(
        context: Context,
        messageContent: String,
        preferOfficialApp: Boolean = false
    ) {
        _lastOpenedPromptForClipboard.value = messageContent.trim()
        val outcome = if (preferOfficialApp) {
            ClaudeLauncher.shareToClaudeAppOrChooser(context, messageContent)
        } else {
            ClaudeLauncher.openClaudeWebWithPrompt(context, messageContent)
        }
        _inAppAlert.value = InAppAlert(
            title = if (outcome.launchedSuccessfully) "Claude נפתח לשליחה" else "הודעה הועתקה",
            message = outcome.message,
            isError = !outcome.launchedSuccessfully
        )
    }

    fun markMessageSent(messageId: Long) {
        viewModelScope.launch {
            repository.markPreparedMessageSent(messageId)
            _inAppAlert.value = InAppAlert(
                title = "ההודעה סומנה כנשלחה",
                message = "הסטטוס עודכן ביומן הפעולות ובהיסטוריה.",
                isError = false
            )
        }
    }

    fun saveManualReply(replyText: String) {
        val convoId = _activeConversationId.value ?: return
        val cleaned = replyText.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            repository.saveManualClaudeReply(convoId, cleaned)
            _inAppAlert.value = InAppAlert(
                title = "התשובה נשמרה בשיחה",
                message = "התשובה מ-Claude נוספה להיסטוריה המקומית.",
                isError = false
            )
        }
    }

    fun sendMessageViaApi(promptText: String) {
        if (_isSending.value) return
        val cleaned = promptText.trim()
        if (cleaned.isEmpty()) return

        if (!settings.value.isPaidApiReady) {
            _inAppAlert.value = InAppAlert(
                title = "מצב חינמי פעיל",
                message = "שליחה ישירה ב-API דורשת אישור עלויות והגדרת מפתח בהגדרות.",
                isError = true
            )
            return
        }

        if (!networkMonitor.checkCurrentConnectivity()) {
            _inAppAlert.value = InAppAlert(
                title = "אין חיבור לאינטרנט",
                message = "בדוק את חיבור הרשת במכשיר ונסה שוב.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            _isSending.value = true
            _streamingReplyText.value = null
            val beforeCount = activeMessages.value.size
            try {
                val result = repository.sendMessageStreaming(
                    conversationId = _activeConversationId.value,
                    promptText = cleaned,
                    isScheduled = false,
                    onPartialText = { partial ->
                        _streamingReplyText.value = partial
                    }
                )
                _streamingReplyText.value = null
                when (result) {
                    is SendMessageResult.Success -> {
                        _activeConversationId.value = result.conversationId
                        val updatedList = activeMessages.value
                        if (updatedList.size > beforeCount) {
                            _lastArrivedMessageId.value = updatedList.lastOrNull()?.id
                        }
                        if (settings.value.notifyOnReplies) {
                            _inAppAlert.value = InAppAlert(
                                title = "התקבלה תשובה מ-Claude",
                                message = "${result.outputTokens} אסימוני פלט",
                                isError = false
                            )
                        }
                    }
                    is SendMessageResult.Failure -> {
                        if (result.conversationId != null) {
                            _activeConversationId.value = result.conversationId
                        }
                        _inAppAlert.value = InAppAlert(
                            title = "שגיאה בשליחה ל-Claude",
                            message = result.errorMessage,
                            isError = true
                        )
                    }
                }
            } finally {
                _streamingReplyText.value = null
                _isSending.value = false
            }
        }
    }

    fun markLatestMessageAnimated(messageId: Long) {
        if (_lastArrivedMessageId.value == messageId) {
            _lastArrivedMessageId.value = null
        }
    }

    fun renameConversation(conversationId: Long, newTitle: String) {
        viewModelScope.launch { repository.renameConversation(conversationId, newTitle) }
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
            }
        }
    }

    fun setResponseLanguage(language: String) {
        viewModelScope.launch {
            repository.setResponseLanguage(language)
            _inAppAlert.value = InAppAlert(
                title = "שפת השיחה עודכנה",
                message = "שפה נבחרת: ${ResponseLanguage.resolveLabel(language)}",
                isError = false
            )
        }
    }

    // Connection Mode & API Security
    fun selectFreeConnectionMode() {
        viewModelScope.launch {
            repository.updateConnectionMode(ConnectionMode.FREE_ACCOUNT, paidApproved = false)
            _inAppAlert.value = InAppAlert(
                title = "עברת למצב חשבון Claude חינמי",
                message = "לא יתבצעו קריאות API בתשלום. הודעות יוכנו וייפתחו ב-Claude בחינם.",
                isError = false
            )
        }
    }

    fun approvePaidApiMode() {
        viewModelScope.launch {
            repository.updateConnectionMode(ConnectionMode.PAID_API, paidApproved = true)
            _inAppAlert.value = InAppAlert(
                title = "חיבור API אושר והופעל",
                message = "חיובי טוקנים חלים לפי תעריפי Anthropic. תוכל לחזור למצב חינמי בכל עת.",
                isError = false
            )
        }
    }

    fun saveApiKey(rawKey: String) {
        repository.saveApiKey(rawKey)
        _connectionTestState.value = ConnectionTestUiState.Idle
        _inAppAlert.value = InAppAlert(
            title = "מפתח API נשמר בהצפנה",
            message = "המפתח מוצפן מקומית באמצעות Android Keystore.",
            isError = false
        )
    }

    fun clearApiKey() {
        repository.clearApiKey()
        _connectionTestState.value = ConnectionTestUiState.Idle
        _inAppAlert.value = InAppAlert(
            title = "מפתח ה-API הוסר",
            message = "המפתח נמחק מהאחסון המאובטח במכשיר.",
            isError = false
        )
    }

    fun revealApiKeyForEditing(): String = repository.getRevealedApiKey().orEmpty()

    fun saveConnectionSettings(
        baseUrl: String,
        anthropicVersion: String,
        selectedModel: String,
        systemPrompt: String,
        maxTokens: Int,
        temperature: Float
    ) {
        viewModelScope.launch {
            repository.saveConnectionSettings(
                baseUrl, anthropicVersion, selectedModel, systemPrompt, maxTokens, temperature
            )
            _inAppAlert.value = InAppAlert(
                title = "הגדרות החיבור נשמרו",
                message = "מודל מוגדר: $selectedModel",
                isError = false
            )
        }
    }

    fun testConnection(baseUrlOverride: String, anthropicVersionOverride: String, modelOverride: String) {
        if (_connectionTestState.value is ConnectionTestUiState.Testing) return
        if (!networkMonitor.checkCurrentConnectivity()) {
            _connectionTestState.value = ConnectionTestUiState.Error(
                "אין חיבור לאינטרנט במכשיר. חבר את הרשת ונסה שוב."
            )
            return
        }
        viewModelScope.launch {
            _connectionTestState.value = ConnectionTestUiState.Testing
            when (val res = repository.testConnection(baseUrlOverride, anthropicVersionOverride, modelOverride)) {
                is ConnectionTestResult.Success -> {
                    _connectionTestState.value = ConnectionTestUiState.Success(res.modelUsed, res.latencyMs)
                }
                is ConnectionTestResult.Failure -> {
                    _connectionTestState.value = ConnectionTestUiState.Error(res.errorMessage)
                }
            }
        }
    }

    // Primary Automation & Multi-Schedules
    fun enableAutoSend(
        prompt: String,
        intervalHours: Int,
        activeStartHour: Int,
        activeEndHour: Int,
        targetConversationId: Long
    ) {
        viewModelScope.launch {
            val nextRun = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(intervalHours.toLong())
            repository.updateAutoSendConfig(
                enabled = true,
                paused = false,
                intervalHours = intervalHours,
                activeStartHour = activeStartHour,
                activeEndHour = activeEndHour,
                prompt = prompt,
                targetConversationId = targetConversationId,
                nextRunTimestamp = nextRun
            )
            autoSendScheduler.scheduleEveryFiveHours(intervalHours)
            _inAppAlert.value = InAppAlert(
                title = "התזמון הופעל (כל $intervalHours שעות)",
                message = if (settings.value.isPaidApiReady) {
                    "שליחה אוטומטית דרך API פעילה בחלון השעות שנקבע."
                } else {
                    "תזכורת והכנת הודעה אוטומטית כל $intervalHours שעות בחשבון החינמי."
                },
                isError = false
            )
        }
    }

    fun pauseAutoSend(
        prompt: String,
        intervalHours: Int,
        activeStartHour: Int,
        activeEndHour: Int,
        targetConversationId: Long
    ) {
        viewModelScope.launch {
            repository.updateAutoSendConfig(
                enabled = true,
                paused = true,
                intervalHours = intervalHours,
                activeStartHour = activeStartHour,
                activeEndHour = activeEndHour,
                prompt = prompt,
                targetConversationId = targetConversationId,
                nextRunTimestamp = 0L
            )
            autoSendScheduler.cancelSchedule()
            _inAppAlert.value = InAppAlert(
                title = "האוטומציה הושהתה",
                message = "ניתן לחדש בכל רגע מבלי לאבד את ההגדרות.",
                isError = false
            )
        }
    }

    fun resumeAutoSend(
        prompt: String,
        intervalHours: Int,
        activeStartHour: Int,
        activeEndHour: Int,
        targetConversationId: Long
    ) {
        enableAutoSend(prompt, intervalHours, activeStartHour, activeEndHour, targetConversationId)
    }

    fun disableAutoSend(
        prompt: String,
        intervalHours: Int,
        activeStartHour: Int,
        activeEndHour: Int,
        targetConversationId: Long
    ) {
        viewModelScope.launch {
            repository.updateAutoSendConfig(
                enabled = false,
                paused = false,
                intervalHours = intervalHours,
                activeStartHour = activeStartHour,
                activeEndHour = activeEndHour,
                prompt = prompt,
                targetConversationId = targetConversationId,
                nextRunTimestamp = 0L
            )
            autoSendScheduler.cancelSchedule()
            _inAppAlert.value = InAppAlert(
                title = "האוטומציה כובתה",
                message = "כל המשימות המחזוריות הראשיות הופסקו.",
                isError = false
            )
        }
    }

    fun saveAutoSendPromptOnly(
        prompt: String,
        intervalHours: Int,
        activeStartHour: Int,
        activeEndHour: Int,
        targetConversationId: Long
    ) {
        viewModelScope.launch {
            val current = settings.value
            val nextRun = if (current.autoSendEnabled && !current.autoSendPaused) {
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(intervalHours.toLong())
            } else 0L
            repository.updateAutoSendConfig(
                enabled = current.autoSendEnabled,
                paused = current.autoSendPaused,
                intervalHours = intervalHours,
                activeStartHour = activeStartHour,
                activeEndHour = activeEndHour,
                prompt = prompt,
                targetConversationId = targetConversationId,
                nextRunTimestamp = nextRun
            )
            if (current.autoSendEnabled && !current.autoSendPaused) {
                autoSendScheduler.scheduleEveryFiveHours(intervalHours)
            }
            _inAppAlert.value = InAppAlert(
                title = "הגדרות התזמון נשמרו",
                message = "מרווח: כל $intervalHours שעות · חלון: $activeStartHour:00–$activeEndHour:00",
                isError = false
            )
        }
    }

    fun runAutoSendNow(
        prompt: String,
        intervalHours: Int,
        activeStartHour: Int,
        activeEndHour: Int,
        targetConversationId: Long
    ) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateAutoSendConfig(
                enabled = current.autoSendEnabled,
                paused = current.autoSendPaused,
                intervalHours = intervalHours,
                activeStartHour = activeStartHour,
                activeEndHour = activeEndHour,
                prompt = prompt,
                targetConversationId = targetConversationId,
                nextRunTimestamp = current.nextAutoSendTimestamp
            )
            autoSendScheduler.runImmediateVerification(-1L)
            _inAppAlert.value = InAppAlert(
                title = "הרצת בדיקה נשלחה לרקע",
                message = "תקבל התראה במרכז ההתראות מיד עם סיום הפעולה.",
                isError = false
            )
        }
    }

    // Additional Scheduled Prompts
    fun saveCustomSchedule(
        existingId: Long = 0L,
        title: String,
        promptText: String,
        intervalHours: Int,
        activeStartHour: Int,
        activeEndHour: Int,
        isEnabled: Boolean,
        targetConversationId: Long = -1L
    ) {
        if (title.isBlank() || promptText.isBlank()) return
        viewModelScope.launch {
            val nextRun = if (isEnabled) {
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(intervalHours.toLong())
            } else 0L
            val savedId = repository.saveScheduledPrompt(
                ScheduledPromptEntity(
                    id = existingId,
                    title = title.trim(),
                    promptText = promptText.trim(),
                    intervalHours = intervalHours.coerceIn(1, 24),
                    activeStartHour = activeStartHour.coerceIn(0, 23),
                    activeEndHour = activeEndHour.coerceIn(0, 23),
                    isEnabled = isEnabled,
                    isPaused = !isEnabled,
                    targetConversationId = targetConversationId,
                    nextRunAt = nextRun
                )
            )
            if (isEnabled) {
                autoSendScheduler.schedulePrompt(savedId, intervalHours)
            } else {
                autoSendScheduler.cancelPromptSchedule(savedId)
            }
            _inAppAlert.value = InAppAlert(
                title = "תזמון נוסף נשמר",
                message = title.trim(),
                isError = false
            )
        }
    }

    fun toggleCustomSchedule(schedule: ScheduledPromptEntity) {
        viewModelScope.launch {
            val nowEnabled = !schedule.isEnabled || schedule.isPaused
            val nextRun = if (nowEnabled) {
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(schedule.intervalHours.toLong())
            } else 0L
            val updated = schedule.copy(
                isEnabled = nowEnabled,
                isPaused = !nowEnabled,
                nextRunAt = nextRun
            )
            repository.updateScheduledPrompt(updated)
            if (nowEnabled) {
                autoSendScheduler.schedulePrompt(updated.id, updated.intervalHours)
            } else {
                autoSendScheduler.cancelPromptSchedule(updated.id)
            }
        }
    }

    fun deleteCustomSchedule(scheduleId: Long) {
        viewModelScope.launch {
            autoSendScheduler.cancelPromptSchedule(scheduleId)
            repository.deleteScheduledPrompt(scheduleId)
        }
    }

    fun runCustomScheduleNow(scheduleId: Long) {
        autoSendScheduler.runImmediateVerification(scheduleId)
        _inAppAlert.value = InAppAlert(
            title = "התזמון מופעל כעת",
            message = "בדוק את מרכז ההתראות בעוד מספר שניות.",
            isError = false
        )
    }

    // Tasks Management
    fun saveTask(
        existingId: Long = 0L,
        title: String,
        promptText: String,
        dueAt: Long,
        remindEnabled: Boolean
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val savedId = repository.saveTask(
                TaskEntity(
                    id = existingId,
                    title = title.trim(),
                    promptText = promptText.trim(),
                    dueAt = dueAt,
                    remindEnabled = remindEnabled,
                    isCompleted = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
            if (remindEnabled && dueAt > now) {
                autoSendScheduler.scheduleTaskReminder(savedId, dueAt)
            } else {
                autoSendScheduler.cancelTaskReminder(savedId)
            }
            _inAppAlert.value = InAppAlert(
                title = if (existingId > 0L) "המשימה עודכנה" else "משימה חדשה נוצרה",
                message = title.trim(),
                isError = false
            )
        }
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            val completed = !task.isCompleted
            val updated = task.copy(isCompleted = completed, updatedAt = System.currentTimeMillis())
            repository.updateTask(updated)
            if (completed) {
                autoSendScheduler.cancelTaskReminder(task.id)
                repository.logTaskCompleted(updated)
            } else if (updated.remindEnabled && updated.dueAt > System.currentTimeMillis()) {
                autoSendScheduler.scheduleTaskReminder(updated.id, updated.dueAt)
            }
        }
    }

    fun duplicateTask(task: TaskEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val nextDue = (task.dueAt.coerceAtLeast(now)) + TimeUnit.HOURS.toMillis(5)
            val newId = repository.saveTask(
                TaskEntity(
                    title = "${task.title} (עותק)",
                    promptText = task.promptText,
                    dueAt = nextDue,
                    remindEnabled = task.remindEnabled,
                    isCompleted = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
            if (task.remindEnabled) {
                autoSendScheduler.scheduleTaskReminder(newId, nextDue)
            }
            _inAppAlert.value = InAppAlert(
                title = "המשימה שוכפלה",
                message = "${task.title} (עותק)",
                isError = false
            )
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            autoSendScheduler.cancelTaskReminder(taskId)
            repository.deleteTask(taskId)
        }
    }

    // Notification Center
    fun markNotificationRead(id: Long) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { repository.markAllNotificationsRead() }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch { repository.deleteNotification(id) }
    }

    fun clearAllNotifications() {
        viewModelScope.launch { repository.clearAllNotifications() }
    }

    fun updateNotificationPreferences(
        notifyOnReplies: Boolean,
        notifyOnErrors: Boolean,
        notifyTaskReminders: Boolean
    ) {
        viewModelScope.launch {
            repository.updateNotificationPreferences(notifyOnReplies, notifyOnErrors, notifyTaskReminders)
        }
    }

    fun setMonthlyBudgetUsd(budgetUsd: Float) {
        viewModelScope.launch {
            repository.setMonthlyBudgetUsd(budgetUsd)
        }
    }

    // Floating Bubble Controls
    fun toggleFloatingBubble(context: Context, enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                if (!Settings.canDrawOverlays(context)) {
                    _inAppAlert.value = InAppAlert(
                        title = "נדרשת הרשאת הצגה מעל אפליקציות",
                        message = "אשר את ההרשאה בהגדרות המערכת והפעל מחדש את הכפתור הצף.",
                        isError = true
                    )
                    settingsDataStore.setFloatingBubbleEnabled(false)
                } else {
                    settingsDataStore.setFloatingBubbleEnabled(true)
                    FloatingBubbleService.start(context)
                    _inAppAlert.value = InAppAlert(
                        title = "הכפתור הצף הופעל",
                        message = "תוכל לגרור אותו לכל מקום במסך או ללחוץ עליו לגישה מהירה.",
                        isError = false
                    )
                }
            } else {
                settingsDataStore.setFloatingBubbleEnabled(false)
                FloatingBubbleService.stop(context)
                _inAppAlert.value = InAppAlert(
                    title = "הכפתור הצף הוסתר",
                    message = "ניתן להפעיל אותו שוב בכל עת מהגדרות הכפתור הצף.",
                    isError = false
                )
            }
        }
    }

    fun resetBubblePosition(context: Context) {
        viewModelScope.launch {
            settingsDataStore.saveBubblePosition(40, 260)
            if (settings.value.floatingBubbleEnabled && Settings.canDrawOverlays(context)) {
                FloatingBubbleService.stop(context)
                FloatingBubbleService.start(context)
            }
            _inAppAlert.value = InAppAlert(
                title = "מיקום הכפתור הצף אופס",
                message = "הכפתור הוחזר למיקום ההתחלתי.",
                isError = false
            )
        }
    }

    // Backup, Restore, Stop All & Privacy Wipe
    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun restoreBackupJson(rawJson: String) {
        viewModelScope.launch {
            try {
                val restoredCount = repository.restoreBackupJson(rawJson)
                _inAppAlert.value = InAppAlert(
                    title = "השחזור הושלם בהצלחה",
                    message = "שוחזרו $restoredCount פריטים (שיחות, משימות ותזמונים).",
                    isError = false
                )
            } catch (_: Exception) {
                _inAppAlert.value = InAppAlert(
                    title = "שגיאה בקובץ הגיבוי",
                    message = "קובץ ה-JSON אינו תקין או פגום.",
                    isError = true
                )
            }
        }
    }

    fun stopAllActivity(context: Context) {
        viewModelScope.launch {
            autoSendScheduler.cancelAllWork()
            FloatingBubbleService.stop(context)
            repository.stopAllActivity()
            _inAppAlert.value = InAppAlert(
                title = "כל הפעילות נעצרה",
                message = "כל התזמונים, התזכורות והכפתור הצף הושבתו.",
                isError = false
            )
        }
    }

    fun clearAllLocalData(context: Context) {
        viewModelScope.launch {
            autoSendScheduler.cancelAllWork()
            FloatingBubbleService.stop(context)
            repository.clearAllLocalData()
            _activeConversationId.value = null
            _inAppAlert.value = InAppAlert(
                title = "כל המידע המקומי נמחק",
                message = "שיחות, משימות, מפתחות והגדרות נוקו מהמכשיר.",
                isError = false
            )
        }
    }

    fun dismissAlert() {
        _inAppAlert.value = null
    }

    class Factory(
        private val repository: ClaudeRepository,
        private val autoSendScheduler: AutoSendScheduler,
        private val networkMonitor: NetworkMonitor,
        private val settingsDataStore: SettingsDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClaudeViewModel(repository, autoSendScheduler, networkMonitor, settingsDataStore) as T
        }
    }
}
