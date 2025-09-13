package com.example.vayvene.ui.cashier

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.BuildConfig
import com.example.vayvene.R
import com.example.vayvene.data.Session
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
            if (uid.isNotBlank()) executeChosenAction(uid)
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

        rgAction.setOnCheckedChangeListener { _, _ ->
            val needsAmount = rbRecharge.isChecked || rbRefund.isChecked
            etAmount.isEnabled = needsAmount
            etAmount.visibility = if (needsAmount) View.VISIBLE else View.GONE
        }
        rgAction.check(R.id.rbRecharge)

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
        val token = getToken() ?: run { goToLogin("No hay token guardado"); return }

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
            if (code == 401) { goToLogin("Sesión expirada (401)"); return@doPostAuth }
            if (code in 200..299) {
                toast("Recarga OK ✔")
                tvInfo.text = "Recargado $amount a $customerUid"
            } else {
                toast("Error recarga ($code)")
                tvInfo.text = body.take(200)
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
            if (code == 401) { goToLogin("Sesión expirada (401)"); return@doPostAuth }
            if (code in 200..299) {
                toast("Devolución OK ✔")
                tvInfo.text = "Devuelto $amount a $customerUid"
            } else {
                toast("Error devolución ($code)")
                tvInfo.text = body.take(200)
            }
        }
    }

    // POST /api/mobile/balance { cardUid } (Bearer)
    private fun balance(cardUid: String, token: String) {
        val url = BuildConfig.BASE_URL.trimEnd('/') + "/api/mobile/balance"
        val payload = JSONObject().apply { put("cardUid", cardUid) }
        doPostAuth(url, payload, token) { code, body ->
            if (code == 401) { goToLogin("Sesión expirada (401)"); return@doPostAuth }
            if (code in 200..299) {
                val b = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                val bal = b.opt("balance")?.toString() ?: "0"
                tvInfo.text = "Saldo: $bal"
                toast("Consulta OK ✔")
            } else {
                toast("Error consulta ($code)")
                tvInfo.text = body.take(200)
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
                    runOnUiThread { cb(code, body) }
                }
            } catch (e: Exception) {
                runOnUiThread { toast("Red: ${e.message}") }
            }
        }.start()
    }

    // ----------------- Session helpers -----------------
    private fun getToken(): String? = Session.jwt(this)

    private fun goToLogin(msg: String? = null) {
        msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        val i = Intent(this, NfcLoginActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    // Verifica /api/mobile/me pero SOLO redirige si es 401
    private fun validateSessionOrLogin() {
        val token = getToken()
        if (token.isNullOrBlank()) { goToLogin("Sin token en sesión"); return }

        val base = BuildConfig.BASE_URL.trimEnd('/')
        val req = Request.Builder()
            .url("$base/api/mobile/me")
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()

        Thread {
            try {
                http.newCall(req).execute().use { res ->
                    val code = res.code
                    if (code == 401) {
                        runOnUiThread { goToLogin("Sesión expirada (401)") }
                    } else if (!res.isSuccessful) {
                        // No te saco del cajero por 404/500; sólo informo
                        val body = res.body?.string().orEmpty()
                        runOnUiThread {
                            toast("ME $code")
                            tvInfo.text = body.take(200)
                        }
                    } // 2xx -> OK, no hago nada
                }
            } catch (e: Exception) {
                runOnUiThread { toast("Red ME: ${e.message}") }
            }
        }.start()
    }
}
