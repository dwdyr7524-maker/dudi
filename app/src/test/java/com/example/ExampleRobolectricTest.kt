package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.SettingsDataStore
import com.example.data.remote.AnthropicErrorMapper
import com.example.data.security.SecureApiKeyStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Claude Connect`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Claude Connect", appName)
    }

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
}
