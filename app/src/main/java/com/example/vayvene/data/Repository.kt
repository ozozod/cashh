package com.example.vayvene.data

import com.example.vayvene.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LoginResult(
    val ok: Boolean,
    val message: String? = null,
    val token: String? = null,
    val role: String? = null,
    val eventId: String? = null,
    val name: String? = null,
    val cardUid: String? = null,
)

object Repository {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun mobileLogin(cardUid: String, telemetry: Map<String, Any?>): LoginResult {
        val url = BuildConfig.BASE_URL.trimEnd('/') + "/api/mobile/login"
        val media = "application/json; charset=utf-8".toMediaType()

        val bodyJson = JSONObject()
            .put("cardUid", cardUid.uppercase())
            .put("telemetry", JSONObject(telemetry))
            .toString()
            .toRequestBody(media)

        val req = Request.Builder()
            .url(url)
            .post(bodyJson)
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val err = resp.body?.string()
                return LoginResult(false, err ?: "HTTP ${resp.code}")
            }
            val txt = resp.body?.string().orEmpty()
            val j = JSONObject(txt)

            return LoginResult(
                ok = true,
                token = j.optString("token", null),
                role = j.optString("role", null),
                eventId = j.optString("eventId", null),
                name = j.optString("name", null),
                cardUid = j.optString("cardUid", null),
            )
        }
    }
}
