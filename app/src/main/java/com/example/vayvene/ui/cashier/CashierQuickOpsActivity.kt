package com.example.vayvene.ui.cashier

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.BuildConfig
import com.example.vayvene.R
import com.example.vayvene.ui.login.NfcLoginActivity
import com.example.vayvene.ui.nfc.NfcCaptureActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

class CashierQuickOpsActivity : AppCompatActivity() {

    private lateinit var rgAction: RadioGroup
    private lateinit var rbRecharge: RadioButton
    private lateinit var rbRefund: RadioButton
    private lateinit var rbBalance: RadioButton
    private lateinit var etAmount: EditText
    private lateinit var btnScanAndGo: Button
    private lateinit var tvInfo: TextView

    private val http by lazy { OkHttpClient() }

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
            val uid = res.data?.getStringExtra(NfcCaptureActivity.EXTRA_UID).orEmpty().uppercase()
            if (uid.isNotBlank()) {
                executeChosenAction(uid)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cashier_quick_ops)

        rgAction = findViewById(R.id.rgAction)
        rbRecharge = findViewById(R.id.rbRecharge)
        rbRefund = findViewById(R.id.rbRefund)
        rbBalance = findViewById(R.id.rbBalance)
        etAmount = findViewById(R.id.etAmount)
        btnScanAndGo = findViewById(R.id.btnScanAndGo)
        tvInfo = findViewById(R.id.tvInfo)

        // Habilitar/deshabilitar monto según acción
        rgAction.setOnCheckedChangeListener { _, _ ->
            etAmount.isEnabled = rbRecharge.isChecked || rbRefund.isChecked
            etAmount.visibility = if (etAmount.isEnabled) View.VISIBLE else View.GONE
        }
        rgAction.check(R.id.rbRecharge) // default

        btnScanAndGo.setOnClickListener {
            val prompt = when {
                rbRecharge.isChecked -> "Acercá la tarjeta del COMPRADOR para RECARGAR"
                rbRefund.isChecked   -> "Acercá la tarjeta del COMPRADOR para DEVOLVER saldo"
                else                 -> "Acercá la tarjeta del COMPRADOR para CONSULTAR saldo"
            }
            val i = Intent(this, NfcCaptureActivity::class.java)
            i.putExtra(NfcCaptureActivity.EXTRA_PROMPT, prompt)
            scanLauncher.launch(i)
        }
    }

    override fun onResume() {
        super.onResume()
        validateSessionOrLogin()
    }

    // ----------------- Lógica principal -----------------
    private fun executeChosenAction(customerUid: String) {
        val token = getToken() ?: run { goToLogin(); return }

        when {
            rbRecharge.isChecked -> {
                val amount = etAmount.text.toString().trim().toBigDecimalOrNull()
                if (amount == null || amount <= BigDecimal.ZERO) { toast("Monto inválido"); return }
                recharge(customerUid, amount, token)
            }
            rbRefund.isChecked -> {
                val amount = etAmount.text.toString().trim().toBigDecimalOrNull()
                if (amount == null || amount <= BigDecimal.ZERO) { toast("Monto inválido"); return }
                refund(customerUid, amount, token)
            }
            else -> balance(customerUid, token)
        }
    }

    // ----------------- API calls -----------------

    // POST /api/mobile/recharge  { customerUid, amount } (Bearer)
    private fun recharge(customerUid: String, amount: BigDecimal, token: String) {
        val url = BuildConfig.BASE_URL.trimEnd('/') + "/api/mobile/recharge"
        val payload = JSONObject().apply {
            put("customerUid", customerUid)
            put("amount", amount)
        }
        doPostAuth(url, payload, token) { code, body ->
            if (code == 401) { goToLogin(); return@doPostAuth }
            if (code in 200..299) {
                toast("Recarga OK ✔")
            } else {
                toast("Error recarga ($code): $body")
            }
        }
    }

    // POST /api/mobile/withdraw  { customerUid, amount } (Bearer)
    private fun refund(customerUid: String, amount: BigDecimal, token: String) {
        val url = BuildConfig.BASE_URL.trimEnd('/') + "/api/mobile/withdraw"
        val payload = JSONObject().apply {
            put("customerUid", customerUid)
            put("amount", amount)
        }
        doPostAuth(url, payload, token) { code, body ->
            if (code == 401) { goToLogin(); return@doPostAuth }
            if (code in 200..299) {
                toast("Devolución OK ✔")
            } else {
                toast("Error devolución ($code): $body")
            }
        }
    }

    // POST /api/mobile/balance { cardUid } (Bearer)
    private fun balance(cardUid: String, token: String) {
        val url = BuildConfig.BASE_URL.trimEnd('/') + "/api/mobile/balance"
        val payload = JSONObject().apply { put("cardUid", cardUid) }
        doPostAuth(url, payload, token) { code, body ->
            if (code == 401) { goToLogin(); return@doPostAuth }
            if (code in 200..299) {
                val b = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                val bal = b.opt("balance")?.toString() ?: "0"
                tvInfo.text = "Saldo: $bal"
                toast("Consulta OK ✔")
            } else {
                toast("Error consulta ($code): $body")
            }
        }
    }

    // ----------------- HTTP helpers -----------------
    private fun doPostAuth(url: String, json: JSONObject, token: String, cb: (Int, String) -> Unit) {
        val req = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addHeader("Authorization", "Bearer $token")
            .build()
        Thread {
            try {
                http.newCall(req).execute().use { res ->
                    val code = res.code
                    val body = res.body?.string().orEmpty()
                    runOnUiThread { cb(code, body) } // <-- volvemos al hilo UI
                }
            } catch (e: Exception) {
                runOnUiThread { toast("Red: ${e.message}") }
            }
        }.start()
    }

    private fun doPost(url: String, json: JSONObject, cb: (Int, String) -> Unit) {
        val req = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        Thread {
            try {
                http.newCall(req).execute().use { res ->
                    val code = res.code
                    val body = res.body?.string().orEmpty()
                    runOnUiThread { cb(code, body) } // <-- volvemos al hilo UI
                }
            } catch (e: Exception) {
                runOnUiThread { toast("Red: ${e.message}") }
            }
        }.start()
    }

    // ----------------- Session helpers -----------------
    private fun getToken(): String? =
        getSharedPreferences("session", Context.MODE_PRIVATE).getString("jwt", null)

    private fun goToLogin() {
        Toast.makeText(this, "Sesión expirada. Volvé a loguear.", Toast.LENGTH_SHORT).show()
        val i = Intent(this, NfcLoginActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
    }

    private fun toast(s: String) = runOnUiThread {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }

    // ---------- (Opcional) resolutores por si necesitás leer claims / eventId ----------
    private fun resolveEventIdBlocking(token: String): String? {
        val prefs = getSharedPreferences("session", Context.MODE_PRIVATE)
        prefs.getString("eventId", null)?.let { if (it.isNotBlank()) return it }

        listOf("eventId", "event_id").forEach { k ->
            getClaim(token, k)?.let { v ->
                if (v.isNotBlank()) {
                    prefs.edit().putString("eventId", v).apply()
                    return v
                }
            }
        }

        val base = BuildConfig.BASE_URL.trimEnd('/')
        val urls = listOf(
            "$base/api/mobile/me",
            "$base/api/events/active",
            "$base/api/events?status=activo",
            "$base/api/events?status=active",
            "$base/api/events",
            "$base/mobile/me",
            "$base/events/active",
            "$base/events?status=activo",
            "$base/events?status=active",
            "$base/events"
        )
        for (u in urls) {
            try {
                val req = Request.Builder().url(u).get()
                    .addHeader("Authorization", "Bearer $token").build()
                http.newCall(req).execute().use { res ->
                    val body = res.body?.string().orEmpty()
                    if (!res.isSuccessful) return@use
                    parseEventIdFrom(body)?.let { id ->
                        prefs.edit().putString("eventId", id).apply()
                        return id
                    }
                }
            } catch (_: Exception) { }
        }
        return null
    }

    private fun parseEventIdFrom(body: String): String? {
        val t = body.trim()
        try {
            if (t.startsWith("[")) {
                val arr = JSONArray(t)
                var firstId: String? = null
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("id")
                    if (firstId == null && id.isNotBlank()) firstId = id
                    val status = o.optString("status")
                    if (status.equals("activo", true) || status.equals("active", true)) {
                        if (id.isNotBlank()) return id
                    }
                }
                return firstId
            } else if (t.startsWith("{")) {
                val obj = JSONObject(t)
                obj.opt("eventId")?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
                obj.opt("event_id")?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
                obj.optJSONObject("event")?.opt("id")?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
                obj.optJSONObject("user")?.opt("eventId")?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
                obj.optJSONObject("user")?.opt("event_id")?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
                obj.opt("id")?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            }
        } catch (_: Exception) { }
        return null
    }

    private fun getClaim(jwt: String, key: String): String? {
        return try {
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val b64 = parts[1].replace('-', '+').replace('_', '/')
            val pad = (4 - b64.length % 4) % 4
            val payload = String(Base64.decode(b64 + "=".repeat(pad), Base64.DEFAULT))
            val json = JSONObject(payload)
            (json.opt(key) ?: json.optJSONObject("user")?.opt(key))?.toString()?.takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
    }

    private fun validateSessionOrLogin() {
        val token = getToken()
        if (token.isNullOrBlank()) { goToLogin(); return }

        val base = BuildConfig.BASE_URL.trimEnd('/')
        val req = Request.Builder()
            .url("$base/api/mobile/me")
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()

        Thread {
            try {
                http.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) {
                        runOnUiThread { goToLogin() }
                    }
                }
            } catch (_: Exception) {
                // podés mostrar un toast de red si querés
            }
        }.start()
    }
}
