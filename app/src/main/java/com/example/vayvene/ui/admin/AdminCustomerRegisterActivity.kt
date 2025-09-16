package com.example.vayvene.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.BuildConfig
import com.example.vayvene.data.Session
import com.example.vayvene.ui.common.EXTRA_PROMPT
import com.example.vayvene.ui.common.EXTRA_UID
import com.example.vayvene.ui.nfc.NfcCaptureActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AdminCustomerRegisterActivity : AppCompatActivity() {

    private val client by lazy { OkHttpClient() }

    private val nfcLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val data = res.data
        val uid = data?.getStringExtra(EXTRA_UID)
        if (uid.isNullOrBlank()) {
            Toast.makeText(this, "No se leyó ninguna tarjeta", Toast.LENGTH_SHORT).show()
            finish()
            return@registerForActivityResult
        }
        registrarComprador(uid)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sin UI específica: lanzamos directo el prompt NFC
        val intent = NfcCaptureActivity.newIntent(
            context = this,
            prompt = "Acerque tarjeta para registrar comprador"
        )
        nfcLauncher.launch(intent)
    }

    private fun registrarComprador(cardUid: String) {
        val token = Session.jwt(this)
        val eventId = Session.eventId(this)

        if (eventId.isNullOrBlank()) {
            Toast.makeText(this, "Falta eventId en sesión", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val url = BuildConfig.BASE_URL.trimEnd('/') + "/mobile/customer/register"

        val telemetry = JSONObject().apply {
            put("batteryPct", 100)
            put("signal", -50)
            put("networkType", "wifi")
            put("device", "android")
            put("appVersion", BuildConfig.VERSION_NAME)
            put("online", true)
            put("staffName", Session.userName(this@AdminCustomerRegisterActivity) ?: "")
            put("staffRole", Session.userRole(this@AdminCustomerRegisterActivity) ?: "")
            put("staffCardUidReversed", Session.staffCardUid(this@AdminCustomerRegisterActivity) ?: "")
        }

        val payload = JSONObject().apply {
            put("eventId", eventId)
            put("cardUid", cardUid)
            put("telemetry", telemetry)
        }

        val body = payload.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val req = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
            .post(body)
            .build()

        client.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    Toast.makeText(this@AdminCustomerRegisterActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val code = response.code
                val respText = response.body?.string().orEmpty()
                runOnUiThread {
                    when (code) {
                        200, 201 -> {
                            Toast.makeText(this@AdminCustomerRegisterActivity, "Comprador registrado", Toast.LENGTH_LONG).show()
                        }
                        409 -> {
                            Toast.makeText(this@AdminCustomerRegisterActivity, "Esa tarjeta ya está registrada en este evento", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(this@AdminCustomerRegisterActivity, "Error ($code): $respText", Toast.LENGTH_LONG).show()
                        }
                    }
                    finish()
                }
            }
        })
    }
}
