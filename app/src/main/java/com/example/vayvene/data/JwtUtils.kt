package com.example.vayvene.data

import android.util.Base64
import org.json.JSONObject

object JwtUtils {

    private fun decodePayload(token: String): JSONObject? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payloadB64 = parts[1]
            val bytes = Base64.decode(
                payloadB64,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            val json = String(bytes, Charsets.UTF_8)
            JSONObject(json)
        } catch (_: Throwable) {
            null
        }
    }

    fun getRole(token: String): String? =
        decodePayload(token)?.optString("role")?.takeIf { it.isNotEmpty() }

    fun getUserId(token: String): String? =
        decodePayload(token)?.optString("userId")?.takeIf { it.isNotEmpty() }

    fun getEventId(token: String): String? =
        decodePayload(token)?.optString("eventId")?.takeIf { it.isNotEmpty() }
}
