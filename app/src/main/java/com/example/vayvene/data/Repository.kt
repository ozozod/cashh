// Reemplazá el contenido de Repository.kt por algo como esto si hoy depende de ApiBase.
// Si preferís mantener tu lógica actual, al menos quitá la herencia/uso de ApiBase.

package com.example.vayvene.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.vayvene.util.TelemetryUtil
import com.example.vayvene.util.putSafe
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Repository {

    /**
     * Login móvil por UID de tarjeta (reversed).
     */
    fun login(
        ctx: Context,
        uidReversed: String,
        onSuccess: (JSONObject) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        Thread {
            try {
                val base = com.example.vayvene.BuildConfig.BASE_URL.trimEnd('/')
                val endpoint = "$base/mobile/login"
                val payload = JSONObject()
                    .put("cardUidReversed", uidReversed)
                    .put("telemetry", TelemetryUtil.snapshot(ctx))

                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 15000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream.bufferedReader().use { it.readText() }
                val json = try { JSONObject(text) } catch (_: Throwable) { JSONObject().put("raw", text) }

                Handler(Looper.getMainLooper()).post {
                    if (code in 200..299) onSuccess(json) else onError(Exception("HTTP $code: $text"))
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { onError(e) }
            }
        }.start()
    }
}
