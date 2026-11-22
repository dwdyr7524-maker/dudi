package com.example

import com.example.data.backup.BackupSerializer
import com.example.data.local.ConnectionMode
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.MessageStatus
import com.example.data.local.ResponseLanguage
import com.example.data.local.ScheduledPromptEntity
import com.example.data.local.TaskEntity
import com.example.data.launcher.ClaudeLauncher
import com.example.data.preferences.AppSettings
import com.example.data.preferences.SettingsDataStore
import com.example.data.remote.AnthropicClientFactory
import com.example.data.remote.AnthropicCostEstimator
import com.example.data.remote.AnthropicErrorMapper
import com.example.data.remote.AnthropicStreamEventEnvelope
import com.example.data.remote.ParsedSseEvent
import com.example.data.repository.ClaudeRepository
import com.example.data.security.SecureApiKeyStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class ExampleUnitTest {

    @Test
    fun `api key masking hides middle characters`() {
        val masked = SecureApiKeyStore.maskKey("sk-ant-api03-1234567890abcd")
        assertEquals("sk-ant-••••abcd", masked)
    }

    @Test
    fun `base url normalization enforces https and trailing slash`() {
        assertEquals(
            "https://api.anthropic.com/",
            SettingsDataStore.normalizeBaseUrl("api.anthropic.com")
        )
        assertEquals(
            "https://custom.proxy.dev/",
            SettingsDataStore.normalizeBaseUrl("http://custom.proxy.dev")
        )
    }

    @Test
    fun `error mapper returns Hebrew messages for offline and 401`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val authErr = AnthropicErrorMapper.mapHttpError(
            code = 401,
            rawErrorBody = """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}""",
            moshi = moshi
        )
        assertTrue(authErr.contains("401"))
        assertTrue(authErr.contains("invalid x-api-key"))

        val offlineErr = AnthropicErrorMapper.mapException(UnknownHostException("api.anthropic.com"))
        assertTrue(offlineErr.contains("אינטרנט"))
    }

    @Test
    fun `default settings stay in free account mode without paid approval`() {
        val defaults = AppSettings()
        assertEquals(ConnectionMode.FREE_ACCOUNT, defaults.connectionMode)
        assertFalse(defaults.paidApiApproved)
        assertFalse(defaults.isPaidApiReady)
        assertEquals(5, defaults.autoSendIntervalHours)
        assertEquals(ResponseLanguage.AUTO, defaults.responseLanguage)
    }

    @Test
    fun `response language detection and labels support Hebrew English and mixed text`() {
        assertTrue(ResponseLanguage.hasHebrewCharacters("שלום Claude, מה נשמע?"))
        assertFalse(ResponseLanguage.hasHebrewCharacters("Hello Claude 2025!"))
        assertEquals("זיהוי אוטומטי", ResponseLanguage.resolveLabel(ResponseLanguage.AUTO))
        assertEquals("עברית", ResponseLanguage.resolveLabel(ResponseLanguage.HEBREW))
        assertEquals("אנגלית", ResponseLanguage.resolveLabel(ResponseLanguage.ENGLISH))

        val autoHebrewPrompt = ClaudeRepository.buildEffectiveSystemPrompt(
            basePrompt = "בסיס",
            responseLanguage = ResponseLanguage.AUTO,
            latestUserPrompt = "איך משלבים Kotlin ו-Compose?"
        )
        assertTrue(autoHebrewPrompt.contains("ענה בעברית"))

        val explicitEnglishPrompt = ClaudeRepository.buildEffectiveSystemPrompt(
            basePrompt = "בסיס",
            responseLanguage = ResponseLanguage.ENGLISH,
            latestUserPrompt = "כתוב לי באנגלית"
        )
        assertTrue(explicitEnglishPrompt.contains("English"))
    }

    @Test
    fun `backup serializer round trip preserves conversations messages tasks and schedules`() {
        val conversations = listOf(
            ConversationEntity(
                id = 10L,
                title = "בדיקת גיבוי",
                model = AppSettings.DEFAULT_MODEL,
                isScheduledThread = false,
                createdAt = 1000L,
                updatedAt = 2000L
            )
        )
        val messages = listOf(
            MessageEntity(
                id = 1L,
                conversationId = 10L,
                role = "user",
                content = "שלום קלוד Hello Claude 123",
                timestamp = 1500L,
                status = MessageStatus.PREPARED
            )
        )
        val tasks = listOf(
            TaskEntity(
                id = 2L,
                title = "משימת בדיקה",
                promptText = "סכם את היום",
                dueAt = 3000L,
                remindEnabled = true,
                isCompleted = false
            )
        )
        val schedules = listOf(
            ScheduledPromptEntity(
                id = 3L,
                title = "תזמון בדיקה",
                promptText = "בוקר טוב",
                intervalHours = 5,
                activeStartHour = 8,
                activeEndHour = 22,
                isEnabled = true
            )
        )

        val encoded = BackupSerializer.encode(conversations, messages, tasks, schedules)
        val decoded = BackupSerializer.decode(encoded)

        assertEquals(1, decoded.conversations.size)
        assertEquals("בדיקת גיבוי", decoded.conversations.first().title)
        assertEquals(1, decoded.messages.size)
        assertEquals("שלום קלוד Hello Claude 123", decoded.messages.first().content)
        assertEquals(MessageStatus.PREPARED, decoded.messages.first().status)
        assertEquals(1, decoded.tasks.size)
        assertEquals("משימת בדיקה", decoded.tasks.first().title)
        assertEquals(1, decoded.schedules.size)
        assertEquals(5, decoded.schedules.first().intervalHours)
    }

    @Test
    fun `cost estimator calculates model rates and formats usd`() {
        val sonnetCost = AnthropicCostEstimator.estimateCostUsd(
            model = "claude-3-5-sonnet-latest",
            inputTokens = 1_000_000,
            outputTokens = 1_000_000
        )
        assertEquals(18.0, sonnetCost, 0.001)

        val haikuCost = AnthropicCostEstimator.estimateCostUsd(
            model = "claude-3-5-haiku-latest",
            inputTokens = 500_000,
            outputTokens = 500_000
        )
        assertEquals(2.4, haikuCost, 0.001)
        assertEquals("$0.00", AnthropicCostEstimator.formatUsd(0.0))
        assertEquals("$0.0042", AnthropicCostEstimator.formatUsd(0.0042))
        assertEquals("$1.25", AnthropicCostEstimator.formatUsd(1.25))
    }

    @Test
    fun `sse parser extracts deltas usage errors and done`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(AnthropicStreamEventEnvelope::class.java)

        val delta = AnthropicClientFactory.parseSseDataPayload(
            """{"type":"content_block_delta","delta":{"type":"text_delta","text":"שלום "}}""",
            adapter
        )
        assertEquals(ParsedSseEvent.TextDelta("שלום "), delta)

        val start = AnthropicClientFactory.parseSseDataPayload(
            """{"type":"message_start","message":{"usage":{"input_tokens":42,"output_tokens":0}}}""",
            adapter
        )
        assertEquals(ParsedSseEvent.UsageUpdate(42, 0), start)

        val usageEnd = AnthropicClientFactory.parseSseDataPayload(
            """{"type":"message_delta","usage":{"output_tokens":128}}""",
            adapter
        )
        assertEquals(ParsedSseEvent.UsageUpdate(0, 128), usageEnd)

        val err = AnthropicClientFactory.parseSseDataPayload(
            """{"type":"error","error":{"type":"overloaded_error","message":"Overloaded"}}""",
            adapter
        )
        assertEquals(ParsedSseEvent.StreamError("Overloaded"), err)

        assertEquals(
            ParsedSseEvent.Done,
            AnthropicClientFactory.parseSseDataPayload("""{"type":"message_stop"}""", adapter)
        )
        assertEquals(
            ParsedSseEvent.Ignore,
            AnthropicClientFactory.parseSseDataPayload("""not-valid-json""", adapter)
        )
    }

    @Test
    fun `conversation markdown export preserves title and bilingual roles`() {
        val md = ClaudeLauncher.buildConversationMarkdown(
            title = "סיכום פגישה",
            messages = listOf(
                MessageEntity(conversationId = 1L, role = "user", content = "סכם בקצרה Kotlin", timestamp = 1000L),
                MessageEntity(conversationId = 1L, role = "assistant", content = "הנה הסיכום.", timestamp = 2000L)
            )
        )
        assertTrue(md.startsWith("# סיכום פגישה"))
        assertTrue(md.contains("### אתה"))
        assertTrue(md.contains("סכם בקצרה Kotlin"))
        assertTrue(md.contains("### Claude"))
        assertTrue(md.contains("הנה הסיכום."))
    }
}

