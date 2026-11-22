package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface AnthropicApiService {
    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") anthropicVersion: String,
        @Body request: AnthropicMessageRequest
    ): Response<AnthropicMessageResponse>
}
