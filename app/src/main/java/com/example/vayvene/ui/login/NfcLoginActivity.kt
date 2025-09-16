package com.example.vayvene.ui.login

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.BuildConfig
import com.example.vayvene.data.Session
import com.example.vayvene.ui.cashier.CashierMenuActivity
import com.example.vayvene.ui.seller.SellerMenuActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import android.content.Intent
import com.example.vayvene.ui.admin.AdminMenuActivity

class NfcLoginActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private val client by lazy { OkHttpClient() }
    private val main by lazy { Handler(Looper.getMainLooper()) }
    private var nfcAdapter: NfcAdapter? = null
    private var isLoggingIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Puede no tener layout; si tenés uno, dejalo:
        // setContentView(R.layout.activity_nfc_login)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "El dispositivo no soporta NFC", Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        val flags = (NfcAdapter.FLAG_READER_NFC_A
                or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK)
        nfcAdapter?.enableReaderMode(this, this, flags, null)
        isLoggingIn = false
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null || isLoggingIn) return
        isLoggingIn = true

        val uidBytes = tag.id ?: return
        val uid = uidBytes.joinToString("") { String.format(Locale.US, "%02X", it) }

        main.post {
            Toast.makeText(this, "Tarjeta detectada: $uid", Toast.LENGTH_SHORT).show()
        }

        doLogin(uid)
    }

    private fun doLogin(cardUid: String) {
        val url = BuildConfig.BASE_URL.trimEnd('/') + "/mobile/login"

        val telemetry = JSONObject().apply {
            put("batteryPct", 100)
            put("signal", -50)
            put("networkType", "wifi")
            put("device", "android")
            put("appVersion", BuildConfig.VERSION_NAME)
            put("online", true)
        }

        val payload = JSONObject().apply {
            put("cardUid", cardUid)
            put("telemetry", telemetry)
        }

        val body = payload.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val req = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                main.post {
                    Toast.makeText(this@NfcLoginActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                    isLoggingIn = false
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val code = response.code
                val text = response.body?.string().orEmpty()
                if (code != 200) {
                    main.post {
                        Toast.makeText(this@NfcLoginActivity, "Login falló ($code): $text", Toast.LENGTH_LONG).show()
                        isLoggingIn = false
                    }
                    return
                }

                try {
                    val json = JSONObject(text)
                    val token = json.optString("token", null)
                    val eventId = json.optString("eventId", null)
                    // El backend puede devolver "role" o "staffRole"
                    val role = when {
                        json.has("staffRole") -> json.optString("staffRole", null)
                        else -> json.optString("role", null)
                    }
                    val name = json.optString("staffName", null)
                    val staffUid = json.optString("staffCardUid", cardUid)

                    Session.save(
                        ctx = this@NfcLoginActivity,
                        jwt = token,
                        eventId = eventId,
                        role = role,
                        name = name,
                        staffUid = staffUid
                    )

                    main.post {
                        routeByRole(role)
                    }
                } catch (ex: Exception) {
                    main.post {
                        Toast.makeText(this@NfcLoginActivity, "Respuesta inválida: ${ex.message}", Toast.LENGTH_LONG).show()
                        isLoggingIn = false
                    }
                }
            }
        })
    }

    private fun routeByRole(roleRaw: String?) {
        val role = roleRaw?.lowercase().orEmpty()
        val intent: Intent? = when (role) {
            "admin", "administrador" -> Intent(this, AdminMenuActivity::class.java)
            "cajero" -> Intent(this, CashierMenuActivity::class.java)
            "vendedor", "encargado" -> Intent(this, SellerMenuActivity::class.java)
            else -> null
        }

        if (intent == null) {
            Toast.makeText(this, "Tarjeta no registrada para staff", Toast.LENGTH_LONG).show()
            isLoggingIn = false
            return
        }

        startActivity(intent)
        finish()
    }
}
