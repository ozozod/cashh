package com.example.vayvene.data

import android.content.Context
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.getSystemService
import com.example.vayvene.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

class Repository(private val context: Context) {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /** POST {BASE_URL}/api/mobile/login  { cardUid }  -> { token, user:{ role,... } } */
    suspend fun loginWithUid(uid: String): LoginResult {
        val url = BuildConfig.BASE_URL.trimEnd('/') + "/api/mobile/login"

        val payload = JSONObject().apply {
            put("cardUid", uid.uppercase(Locale.ROOT))    // lo que espera tu backend
            // opcional: telemetría tolerante (si falla, se ignora)
            put("device", JSONObject().apply {
                put("nombre", Build.MODEL)
                put("plataforma", "android")
            })
            readBattery()?.let { put("bateria", it) }
            readRssi()?.let { put("senal", it) }
        }

        val req = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code} en /api/mobile/login")

        val body = resp.body?.string().orEmpty()
        val json = JSONObject(body)

        // Token: tu router móvil devuelve "token"
        // Token: tu API devuelve "token"
        val token = json.optString("token", "")
        if (token.isBlank()) throw IllegalStateException("Login sin token")


        // Guardar token (memoria + prefs)
        ApiClient.setToken(token)
        context.getSharedPreferences("session", Context.MODE_PRIVATE)
            .edit().putString("jwt", token).apply()

        // Rol viene en user.role
        val roleRaw = json.optJSONObject("user")?.optString("role", "") ?: ""
        val role = normalizeRole(roleRaw)

        return LoginResult(token = token, role = role)
    }

    private fun normalizeRole(raw: String?): String {
        val r = (raw ?: "").trim().lowercase(Locale.ROOT)
        return when (r) {
            "admin", "administrador" -> "admin"
            "encargado", "manager"   -> "encargado"
            "vendedor", "seller", "barman", "bartender" -> "vendedor"
            "cajero", "cashier"      -> "cajero"
            else -> r
        }
    }

    private fun readBattery(): Int? {
        val bm = context.getSystemService<BatteryManager>() ?: return null
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 1..100 }
    }

    @Suppress("DEPRECATION")
    private fun readRssi(): Int? = try {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wifi?.connectionInfo?.rssi
    } catch (_: SecurityException) {
        null // si falta permiso, no rompemos el login
    } catch (_: Throwable) {
        null
    }
}

data class LoginResult(val token: String, val role: String)
