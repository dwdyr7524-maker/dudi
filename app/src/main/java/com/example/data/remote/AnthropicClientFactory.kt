package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

sealed interface StreamingOutcome {
    data class Success(
        val replyText: String,
        val inputTokens: Int,
        val outputTokens: Int
    ) : StreamingOutcome

    data class HttpFailure(
        val code: Int,
        val rawErrorBody: String?
    ) : StreamingOutcome

    data class Error(
        val message: String,
        val isTransient: Boolean
    ) : StreamingOutcome
}

sealed interface ParsedSseEvent {
    data class TextDelta(val chunk: String) : ParsedSseEvent
    data class UsageUpdate(val inputTokens: Int?, val outputTokens: Int?) : ParsedSseEvent
    data class StreamError(val message: String) : ParsedSseEvent
    data object Done : ParsedSseEvent
    data object Ignore : ParsedSseEvent
}

class AnthropicClientFactory {

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(AnthropicMessageRequest::class.java)
    private val responseAdapter = moshi.adapter(AnthropicMessageResponse::class.java)
    private val streamEventAdapter = moshi.adapter(AnthropicStreamEventEnvelope::class.java)

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
            redactHeader("x-api-key")
            redactHeader("Authorization")
        }
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var cachedService: AnthropicApiService? = null

    fun getService(baseUrl: String): AnthropicApiService {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val existing = cachedService
        if (existing != null && cachedBaseUrl == normalized) {
            return existing
        }
        return synchronized(this) {
            val recheck = cachedService
            if (recheck != null && cachedBaseUrl == normalized) {
                recheck
            } else {
                Retrofit.Builder()
                    .baseUrl(normalized)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(AnthropicApiService::class.java)
                    .also {
                        cachedBaseUrl = normalized
                        cachedService = it
                    }
            }
        }
    }

    suspend fun streamMessage(
        baseUrl: String,
        apiKey: String,
        anthropicVersion: String,
        request: AnthropicMessageRequest,
        onPartialText: suspend (String) -> Unit
    ): StreamingOutcome = withContext(Dispatchers.IO) {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val streamPayload = requestAdapter.toJson(request.copy(stream = true))
        val httpRequest = Request.Builder()
            .url("${normalized}v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", anthropicVersion)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .addHeader("Accept", "text/event-stream")
            .post(streamPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        okHttpClient.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext StreamingOutcome.HttpFailure(
                    code = response.code,
                    rawErrorBody = response.body?.string()
                )
            }

            val body = response.body ?: return@withContext StreamingOutcome.Error(
                message = "התקבלה תשובה ריקה ממודל קלוד.",
                isTransient = false
            )

            val contentType = response.header("Content-Type").orEmpty()
            // Fallback gracefully if a custom proxy returns standard JSON instead of SSE
            if (contentType.contains("application/json", ignoreCase = true)) {
                val raw = body.string()
                val parsed = runCatching { responseAdapter.fromJson(raw) }.getOrNull()
                val fullText = parsed?.content
                    ?.filter { it.type == "text" && !it.text.isNullOrBlank() }
                    ?.joinToString("\n\n") { it.text.orEmpty() }
                    ?.trim()
                    .orEmpty()
                if (fullText.isEmpty()) {
                    return@withContext StreamingOutcome.Error(
                        message = "התקבלה תשובה ריקה ממודל קלוד.",
                        isTransient = false
                    )
                }
                withContext(Dispatchers.Main.immediate) {
                    onPartialText(fullText)
                }
                return@withContext StreamingOutcome.Success(
                    replyText = fullText,
                    inputTokens = parsed?.usage?.inputTokens ?: 0,
                    outputTokens = parsed?.usage?.outputTokens ?: 0
                )
            }

            val accumulated = StringBuilder()
            var inputTokens = 0
            var outputTokens = 0
            val source = body.source()

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val jsonPart = line.removePrefix("data:").trim()
                when (val event = parseSseDataPayload(jsonPart, streamEventAdapter)) {
                    is ParsedSseEvent.TextDelta -> {
                        accumulated.append(event.chunk)
                        val snapshot = accumulated.toString()
                        withContext(Dispatchers.Main.immediate) {
                            onPartialText(snapshot)
                        }
                    }
                    is ParsedSseEvent.UsageUpdate -> {
                        event.inputTokens?.let { if (it > 0) inputTokens = it }
                        event.outputTokens?.let { if (it > 0) outputTokens = it }
                    }
                    is ParsedSseEvent.StreamError -> {
                        return@withContext StreamingOutcome.Error(
                            message = event.message,
                            isTransient = true
                        )
                    }
                    ParsedSseEvent.Done -> break
                    ParsedSseEvent.Ignore -> Unit
                }
            }

            val finalText = accumulated.toString().trim()
            if (finalText.isEmpty()) {
                StreamingOutcome.Error(
                    message = "התקבלה תשובה ריקה ממודל קלוד.",
                    isTransient = false
                )
            } else {
                StreamingOutcome.Success(
                    replyText = finalText,
                    inputTokens = inputTokens,
                    outputTokens = outputTokens
                )
            }
        }
    }

    companion object {
        fun parseSseDataPayload(
            payload: String,
            adapter: com.squareup.moshi.JsonAdapter<AnthropicStreamEventEnvelope>
        ): ParsedSseEvent {
            if (payload.isBlank() || payload == "[DONE]") return ParsedSseEvent.Done
            val event = runCatching { adapter.fromJson(payload) }.getOrNull() ?: return ParsedSseEvent.Ignore
            return when (event.type) {
                "content_block_delta" -> {
                    val textChunk = event.delta?.text
                    if (event.delta?.type == "text_delta" && !textChunk.isNullOrEmpty()) {
                        ParsedSseEvent.TextDelta(textChunk)
                    } else {
                        ParsedSseEvent.Ignore
                    }
                }
                "message_start" -> {
                    val usage = event.message?.usage
                    ParsedSseEvent.UsageUpdate(
                        inputTokens = usage?.inputTokens,
                        outputTokens = usage?.outputTokens
                    )
                }
                "message_delta" -> {
                    val usage = event.usage
                    ParsedSseEvent.UsageUpdate(
                        inputTokens = usage?.inputTokens,
                        outputTokens = usage?.outputTokens
                    )
                }
                "message_stop" -> ParsedSseEvent.Done
                "error" -> ParsedSseEvent.StreamError(
                    event.error?.message?.takeIf { it.isNotBlank() } ?: "שגיאה בקבלת הזרם מ-Claude."
                )
                else -> ParsedSseEvent.Ignore
            }
        }
    }
}
