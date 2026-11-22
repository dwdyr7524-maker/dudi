package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.ConnectionMode
import com.example.data.local.ResponseLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "claude_classic_settings"
)

data class AppSettings(
    val connectionMode: String = ConnectionMode.FREE_ACCOUNT,
    val paidApiApproved: Boolean = false,
    val baseUrl: String = DEFAULT_BASE_URL,
    val anthropicVersion: String = DEFAULT_ANTHROPIC_VERSION,
    val selectedModel: String = DEFAULT_MODEL,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val temperature: Float = DEFAULT_TEMPERATURE,
    val autoSendEnabled: Boolean = false,
    val autoSendPaused: Boolean = false,
    val autoSendIntervalHours: Int = 5,
    val activeStartHour: Int = 8,
    val activeEndHour: Int = 23,
    val autoSendPrompt: String = DEFAULT_AUTO_SEND_PROMPT,
    val autoSendConversationId: Long = -1L,
    val lastAutoSendTimestamp: Long = 0L,
    val nextAutoSendTimestamp: Long = 0L,
    val lastAutoSendStatus: String = "",
    val notifyOnReplies: Boolean = true,
    val notifyOnErrors: Boolean = true,
    val notifyTaskReminders: Boolean = true,
    val floatingBubbleEnabled: Boolean = false,
    val bubbleX: Int = 40,
    val bubbleY: Int = 260,
    val responseLanguage: String = ResponseLanguage.AUTO,
    val monthlyBudgetUsd: Float = 0f
) {
    val isPaidApiReady: Boolean
        get() = connectionMode == ConnectionMode.PAID_API && paidApiApproved

    companion object {
        const val DEFAULT_BASE_URL = "https://api.anthropic.com/"
        const val DEFAULT_ANTHROPIC_VERSION = "2023-06-01"
        const val DEFAULT_MODEL = "claude-sonnet-4-5-20250929"
        const val DEFAULT_SYSTEM_PROMPT =
            "אתה Claude, עוזר חכם, מדויק ואדיב. ענה בעברית ברורה, רהוטה ומאורגנת היטב."
        const val DEFAULT_MAX_TOKENS = 2048
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_AUTO_SEND_PROMPT =
            "תן לי סיכום קצר וממוקד של רעיון אחד מועיל לשעות הקרובות בעברית."

        val AVAILABLE_MODELS = listOf(
            "claude-sonnet-4-5-20250929" to "Claude Sonnet 4.5 (מאוזן · כ־3$ למיליון קלט)",
            "claude-opus-4-1-20250805" to "Claude Opus 4.1 (מעמיק · כ־15$ למיליון קלט)",
            "claude-3-7-sonnet-20250219" to "Claude 3.7 Sonnet (חשיבה · כ־3$ למיליון קלט)",
            "claude-3-5-haiku-20241022" to "Claude 3.5 Haiku (חסכוני · כ־0.80$ למיליון קלט)"
        )
    }
}

class SettingsDataStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            connectionMode = prefs[KEY_CONNECTION_MODE] ?: ConnectionMode.FREE_ACCOUNT,
            paidApiApproved = prefs[KEY_PAID_API_APPROVED] ?: false,
            baseUrl = prefs[KEY_BASE_URL] ?: AppSettings.DEFAULT_BASE_URL,
            anthropicVersion = prefs[KEY_ANTHROPIC_VERSION] ?: AppSettings.DEFAULT_ANTHROPIC_VERSION,
            selectedModel = prefs[KEY_SELECTED_MODEL] ?: AppSettings.DEFAULT_MODEL,
            systemPrompt = prefs[KEY_SYSTEM_PROMPT] ?: AppSettings.DEFAULT_SYSTEM_PROMPT,
            maxTokens = prefs[KEY_MAX_TOKENS] ?: AppSettings.DEFAULT_MAX_TOKENS,
            temperature = prefs[KEY_TEMPERATURE] ?: AppSettings.DEFAULT_TEMPERATURE,
            autoSendEnabled = prefs[KEY_AUTO_SEND_ENABLED] ?: false,
            autoSendPaused = prefs[KEY_AUTO_SEND_PAUSED] ?: false,
            autoSendIntervalHours = prefs[KEY_AUTO_SEND_INTERVAL_HOURS] ?: 5,
            activeStartHour = prefs[KEY_ACTIVE_START_HOUR] ?: 8,
            activeEndHour = prefs[KEY_ACTIVE_END_HOUR] ?: 23,
            autoSendPrompt = prefs[KEY_AUTO_SEND_PROMPT] ?: AppSettings.DEFAULT_AUTO_SEND_PROMPT,
            autoSendConversationId = prefs[KEY_AUTO_SEND_CONVERSATION_ID] ?: -1L,
            lastAutoSendTimestamp = prefs[KEY_LAST_AUTO_SEND_TS] ?: 0L,
            nextAutoSendTimestamp = prefs[KEY_NEXT_AUTO_SEND_TS] ?: 0L,
            lastAutoSendStatus = prefs[KEY_LAST_AUTO_SEND_STATUS] ?: "",
            notifyOnReplies = prefs[KEY_NOTIFY_REPLIES] ?: true,
            notifyOnErrors = prefs[KEY_NOTIFY_ERRORS] ?: true,
            notifyTaskReminders = prefs[KEY_NOTIFY_TASKS] ?: true,
            floatingBubbleEnabled = prefs[KEY_FLOATING_BUBBLE_ENABLED] ?: false,
            bubbleX = prefs[KEY_BUBBLE_X] ?: 40,
            bubbleY = prefs[KEY_BUBBLE_Y] ?: 260,
            responseLanguage = prefs[KEY_RESPONSE_LANGUAGE] ?: ResponseLanguage.AUTO,
            monthlyBudgetUsd = prefs[KEY_MONTHLY_BUDGET_USD] ?: 0f
        )
    }

    suspend fun getCurrentSettings(): AppSettings = settingsFlow.first()

    suspend fun updateResponseLanguage(language: String) {
        dataStore.edit { prefs ->
            prefs[KEY_RESPONSE_LANGUAGE] = language
        }
    }

    suspend fun updateMonthlyBudgetUsd(budgetUsd: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_MONTHLY_BUDGET_USD] = budgetUsd.coerceIn(0f, 500f)
        }
    }

    suspend fun updateConnectionMode(mode: String, paidApproved: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_CONNECTION_MODE] = mode
            prefs[KEY_PAID_API_APPROVED] = paidApproved
        }
    }

    suspend fun updateConnectionSettings(
        baseUrl: String,
        anthropicVersion: String,
        selectedModel: String,
        systemPrompt: String,
        maxTokens: Int,
        temperature: Float
    ) {
        val normalizedUrl = normalizeBaseUrl(baseUrl)
        dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = normalizedUrl
            prefs[KEY_ANTHROPIC_VERSION] = anthropicVersion.trim().ifEmpty { AppSettings.DEFAULT_ANTHROPIC_VERSION }
            prefs[KEY_SELECTED_MODEL] = selectedModel.trim().ifEmpty { AppSettings.DEFAULT_MODEL }
            prefs[KEY_SYSTEM_PROMPT] = systemPrompt.trim()
            prefs[KEY_MAX_TOKENS] = maxTokens.coerceIn(128, 8192)
            prefs[KEY_TEMPERATURE] = temperature.coerceIn(0.0f, 1.0f)
        }
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
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_SEND_ENABLED] = enabled
            prefs[KEY_AUTO_SEND_PAUSED] = paused
            prefs[KEY_AUTO_SEND_INTERVAL_HOURS] = intervalHours.coerceIn(1, 24)
            prefs[KEY_ACTIVE_START_HOUR] = activeStartHour.coerceIn(0, 23)
            prefs[KEY_ACTIVE_END_HOUR] = activeEndHour.coerceIn(0, 23)
            prefs[KEY_AUTO_SEND_PROMPT] = prompt.trim().ifEmpty { AppSettings.DEFAULT_AUTO_SEND_PROMPT }
            prefs[KEY_AUTO_SEND_CONVERSATION_ID] = targetConversationId
            prefs[KEY_NEXT_AUTO_SEND_TS] = nextRunTimestamp
        }
    }

    suspend fun updateNotificationPreferences(
        notifyOnReplies: Boolean,
        notifyOnErrors: Boolean,
        notifyTaskReminders: Boolean
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_NOTIFY_REPLIES] = notifyOnReplies
            prefs[KEY_NOTIFY_ERRORS] = notifyOnErrors
            prefs[KEY_NOTIFY_TASKS] = notifyTaskReminders
        }
    }

    suspend fun recordAutoSendRun(timestamp: Long, nextRunTimestamp: Long, statusMessage: String) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_AUTO_SEND_TS] = timestamp
            prefs[KEY_NEXT_AUTO_SEND_TS] = nextRunTimestamp
            prefs[KEY_LAST_AUTO_SEND_STATUS] = statusMessage
        }
    }

    suspend fun setFloatingBubbleEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_FLOATING_BUBBLE_ENABLED] = enabled
        }
    }

    suspend fun saveBubblePosition(x: Int, y: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_BUBBLE_X] = x
            prefs[KEY_BUBBLE_Y] = y
        }
    }

    suspend fun resetAllPreferences() {
        dataStore.edit { it.clear() }
    }

    companion object {
        private val KEY_CONNECTION_MODE = stringPreferencesKey("connection_mode")
        private val KEY_PAID_API_APPROVED = booleanPreferencesKey("paid_api_approved")
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_ANTHROPIC_VERSION = stringPreferencesKey("anthropic_version")
        private val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
        private val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        private val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        private val KEY_AUTO_SEND_ENABLED = booleanPreferencesKey("auto_send_enabled")
        private val KEY_AUTO_SEND_PAUSED = booleanPreferencesKey("auto_send_paused")
        private val KEY_AUTO_SEND_INTERVAL_HOURS = intPreferencesKey("auto_send_interval_hours")
        private val KEY_ACTIVE_START_HOUR = intPreferencesKey("active_start_hour")
        private val KEY_ACTIVE_END_HOUR = intPreferencesKey("active_end_hour")
        private val KEY_AUTO_SEND_PROMPT = stringPreferencesKey("auto_send_prompt")
        private val KEY_AUTO_SEND_CONVERSATION_ID = longPreferencesKey("auto_send_conversation_id")
        private val KEY_LAST_AUTO_SEND_TS = longPreferencesKey("last_auto_send_ts")
        private val KEY_NEXT_AUTO_SEND_TS = longPreferencesKey("next_auto_send_ts")
        private val KEY_LAST_AUTO_SEND_STATUS = stringPreferencesKey("last_auto_send_status")
        private val KEY_NOTIFY_REPLIES = booleanPreferencesKey("notify_replies")
        private val KEY_NOTIFY_ERRORS = booleanPreferencesKey("notify_errors")
        private val KEY_NOTIFY_TASKS = booleanPreferencesKey("notify_tasks")
        private val KEY_FLOATING_BUBBLE_ENABLED = booleanPreferencesKey("floating_bubble_enabled")
        private val KEY_BUBBLE_X = intPreferencesKey("bubble_x")
        private val KEY_BUBBLE_Y = intPreferencesKey("bubble_y")
        private val KEY_RESPONSE_LANGUAGE = stringPreferencesKey("response_language")
        private val KEY_MONTHLY_BUDGET_USD = floatPreferencesKey("monthly_budget_usd")

        fun normalizeBaseUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim().ifEmpty { AppSettings.DEFAULT_BASE_URL }
            val withScheme = if (trimmed.startsWith("https://", ignoreCase = true)) {
                trimmed
            } else {
                "https://" + trimmed.removePrefix("http://").removePrefix("https://")
            }
            return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
        }
    }
}
