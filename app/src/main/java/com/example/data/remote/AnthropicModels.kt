package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AnthropicMessageRequest(
    val model: String,
    @Json(name = "max_tokens")
    val maxTokens: Int,
    val messages: List<AnthropicMessageParam>,
    val system: String? = null,
    val temperature: Float? = null,
    val stream: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicMessageParam(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class AnthropicMessageResponse(
    val id: String? = null,
    val type: String? = null,
    val role: String? = null,
    val model: String? = null,
    val content: List<AnthropicContentBlock> = emptyList(),
    @Json(name = "stop_reason")
    val stopReason: String? = null,
    val usage: AnthropicUsage? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicContentBlock(
    val type: String,
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicUsage(
    @Json(name = "input_tokens")
    val inputTokens: Int = 0,
    @Json(name = "output_tokens")
    val outputTokens: Int = 0
)

@JsonClass(generateAdapter = true)
data class AnthropicErrorEnvelope(
    val type: String? = null,
    val error: AnthropicErrorDetail? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicErrorDetail(
    val type: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicStreamEventEnvelope(
    val type: String? = null,
    val message: AnthropicStreamMessageMeta? = null,
    val delta: AnthropicStreamDelta? = null,
    val usage: AnthropicUsage? = null,
    val error: AnthropicErrorDetail? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicStreamMessageMeta(
    val usage: AnthropicUsage? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicStreamDelta(
    val type: String? = null,
    val text: String? = null
)
