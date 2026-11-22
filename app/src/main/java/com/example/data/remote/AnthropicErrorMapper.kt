package com.example.data.remote

import com.squareup.moshi.Moshi
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object AnthropicErrorMapper {

    fun mapHttpError(code: Int, rawErrorBody: String?, moshi: Moshi): String {
        val parsedDetail = parseApiMessage(rawErrorBody, moshi)
        val prefix = when (code) {
            400 -> "בקשה לא תקינה (400)"
            401 -> "מפתח ה-API שגוי או לא מורשה (401)"
            403 -> "אין הרשאה לגשת למשאב המבוקש (403)"
            404 -> "המודל או כתובת ה-API לא נמצאו (404)"
            413 -> "השיחה ארוכה מדי וחורגת ממגבלת הבקשה (413)"
            429 -> "חרגת ממכסת הקריאות או האשראי ב-Anthropic (429)"
            500, 502, 503 -> "שגיאה זמנית בשרתי Anthropic ($code)"
            529 -> "שרתי Anthropic עמוסים כרגע, נסה שוב בעוד מספר שניות (529)"
            else -> "שגיאת תקשורת מול השרת ($code)"
        }
        return if (!parsedDetail.isNullOrBlank()) {
            "$prefix: $parsedDetail"
        } else {
            prefix
        }
    }

    fun mapException(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> "אין חיבור לאינטרנט או שכתובת השרת אינה זמינה."
            is SocketTimeoutException -> "הזמן הקצוב לתשובה מקלוד הסתיים. נסה שוב."
            is SSLException -> "שגיאת אבטחת SSL בחיבור לשרת. ודא כתובת HTTPS תקינה."
            is IOException -> "תקלת רשת בתקשורת עם Anthropic: ${throwable.localizedMessage ?: "שגיאה לא ידועה"}"
            is IllegalArgumentException -> "כתובת שרת או פרמטר חיבור אינם תקינים."
            else -> throwable.localizedMessage ?: "אירעה שגיאה בלתי צפויה בעת הפנייה לקלוד."
        }
    }

    private fun parseApiMessage(rawErrorBody: String?, moshi: Moshi): String? {
        if (rawErrorBody.isNullOrBlank()) return null
        return try {
            val adapter = moshi.adapter(AnthropicErrorEnvelope::class.java)
            adapter.fromJson(rawErrorBody)?.error?.message
        } catch (_: Exception) {
            null
        }
    }
}
