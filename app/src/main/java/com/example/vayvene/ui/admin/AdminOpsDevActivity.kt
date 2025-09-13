package com.example.vayvene.ui.admin

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vayvene.BuildConfig
import com.example.vayvene.R
import com.example.vayvene.ui.login.NfcLoginActivity
import com.example.vayvene.ui.nfc.NfcCaptureActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

class AdminOpsDevActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var tvScanStatus: TextView
    private lateinit var etCustomerUid: EditText
    private lateinit var etAmount: EditText
    private lateinit var tvBalance: TextView
    private lateinit var btnScan: Button
    private lateinit var btnSeeBalance: Button
    private lateinit var btnRecharge: Button
    private lateinit var btnWithdraw: Button
    private lateinit var btnGotoRegisterStaff: Button

    private val http by lazy { OkHttpClient() }

    // anti-rebote (solo para completar el UID en esta pantalla)
    @Volatile private var isHandlingTap = false
    private var lastUid: String? = null
    private var lastTs = 0L

    // launcher del escaneo en pantalla separada
    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
            val uid = res.data?.getStringExtra(NfcCaptureActivity.EXTRA_UID).orEmpty()
            if (uid.isNotBlank()) {
                etCustomerUid.setText(uid)
                tvScanStatus.text = "UID: $uid"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_ops_dev)

        tvScanStatus = findViewById(R.id.tvScanStatus)
        etCustomerUid = findViewById(R.id.etCustomerUid)
        etAmount = findViewById(R.id.etAmount)
        tvBalance = findViewById(R.id.tvBalance)
        btnScan = findViewById(R.id.btnScan)
        btnSeeBalance = findViewById(R.id.btnSeeBalance)
        btnRecharge = findViewById(R.id.btnRecharge)
        btnWithdraw = findViewById(R.id.btnWithdraw)
        btnGotoRegisterStaff = findViewById(R.id.btnGotoRegisterStaff)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        tvScanStatus.text = when {
            nfcAdapter == null -> "Este dispositivo no tiene NFC"
            nfcAdapter?.isEnabled == true -> "Tocá una tarjeta para llenar el UID"
            else -> "Activá el NFC en Ajustes"
        }

        // Ir a tu flujo ya existente de STAFF
        btnGotoRegisterStaff.setOnClickListener {
            startActivity(Intent(this, RegisterCardActivity::class.java))
        }

        // Abrir pantalla dedicada de escaneo
        btnScan.setOnClickListener {
            openScan("Acercá la tarjeta del cliente para identificarla")
        }

        btnSeeBalance.setOnClickListener {
            val uid = etCustomerUid.text.toString().trim().uppercase(Locale.ROOT)
            if (uid.isEmpty()) { openScan("Acercá la tarjeta para ver el saldo"); return@setOnClickListener }
            consultarSaldo(uid)
        }

        btnRecharge.setOnClickListener {
            val uid = etCustomerUid.text.toString().trim().uppercase(Locale.ROOT)
            val amount = etAmount.text.toString().trim().toBigDecimalOrNull()
            if (uid.isEmpty()) { openScan("Acercá la tarjeta para cargar saldo"); return@setOnClickListener }
            if (amount == null || amount <= java.math.BigDecimal.ZERO) { toast("Monto inválido"); return@setOnClickListener }
            recargar(uid, amount)
        }

        btnWithdraw.setOnClickListener {
            val uid = etCustomerUid.text.toString().trim().uppercase(Locale.ROOT)
            val amount = etAmount.text.toString().trim().toBigDecimalOrNull()
            if (uid.isEmpty()) { openScan("Acercá la tarjeta para devolver saldo"); return@setOnClickListener }
            if (amount == null || amount <= java.math.BigDecimal.ZERO) { toast("Monto inválido"); return@setOnClickListener }
            devolver(uid, amount)
        }

        etAmount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { btnRecharge.performClick(); true } else false
        }
    }

    override fun onResume() {
        super.onResume()
        // También dejamos ReaderMode acá para autocompletar el UID si el usuario prefiere
        nfcAdapter?.enableReaderMode(
            this, this,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null
        )
    }

    override fun onPause() { super.onPause(); nfcAdapter?.disableReaderMode(this) }

    override fun onTagDiscovered(tag: Tag) {
        val uid = tag.id.toHex()
        val now = SystemClock.elapsedRealtime()
        if (isHandlingTap || (uid == lastUid && now - lastTs < 1500)) return
        isHandlingTap = true; lastUid = uid; lastTs = now
        runOnUiThread {
            etCustomerUid.setText(uid)
            tvScanStatus.text = "UID detectado: $uid"
            isHandlingTap = false
        }
    }

    // ---------- llamadas ----------
    private fun consultarSaldo(uid: String) {
        val token = getToken() ?: run { goToLogin(); return }   // <- FIX: no devolvemos String
        lifecycleScope.launch {
            try {
                val url = BuildConfig.BASE_URL.trimEnd('/') + "/api/mobile/customer/$uid"
                val req = Request.Builder().url(url).get()
                    .addHeader("Authorization", "Bearer $token").build()
                val res = withContext(Dispatchers.IO) { http.newCall(req).execute() }
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) throw IllegalStateException("HTTP ${res.code}: $body")
                val json = JSONObject(body)
                val balance = json.optDouble("balance", Double.NaN)
                val num = json.optString("customerNumber", "")
                tvBalance.text = "Saldo: $balance  (N° $num)"
            } catch (e: Exception) { tvBalance.text = "Error: ${e.message}" }
        }
    }

    private fun recargar(uid: String, amount: java.math.BigDecimal) {
        val token = getToken() ?: run { goToLogin(); return }   // <- FIX
        lifecycleScope.launch {
            try {
                val url = BuildConfig.BASE_URL.trimEnd('/') + "/api/mobile/recharge"
                val payload = JSONObject().apply { put("customerUid", uid); put("amount", amount) }
                val req = Request.Builder().url(url)
                    .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .addHeader("Authorization", "Bearer $token").build()
                val res = withContext(Dispatchers.IO) { http.newCall(req).execute() }
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) throw IllegalStateException("HTTP ${res.code}: $body")
                val json = JSONObject(body)
                val newBal = json.optDouble("newBalance", Double.NaN)
                tvBalance.text = "Nuevo saldo: $newBal"; toast("Recarga ok")
            } catch (e: Exception) { toast("Error recarga: ${e.message}") }
        }
    }

    private fun devolver(uid: String, amount: java.math.BigDecimal) {
        val token = getToken() ?: run { goToLogin(); return }   // <- FIX
        lifecycleScope.launch {
            try {
                val url = BuildConfig.BASE_URL.trimEnd('/') + "/api/mobile/withdraw"
                val payload = JSONObject().apply { put("customerUid", uid); put("amount", amount) }
                val req = Request.Builder().url(url)
                    .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .addHeader("Authorization", "Bearer $token").build()
                val res = withContext(Dispatchers.IO) { http.newCall(req).execute() }
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) throw IllegalStateException("HTTP ${res.code}: $body")
                val json = JSONObject(body)
                val newBal = json.optDouble("newBalance", Double.NaN)
                tvBalance.text = "Nuevo saldo: $newBal"; toast("Devolución ok")
            } catch (e: Exception) { toast("Error devolución: ${e.message}") }
        }
    }

    // ---------- helpers ----------
    private fun openScan(prompt: String) {
        val i = Intent(this, NfcCaptureActivity::class.java)
        i.putExtra(NfcCaptureActivity.EXTRA_PROMPT, prompt)
        scanLauncher.launch(i)
    }

    private fun getToken(): String? =
        getSharedPreferences("session", Context.MODE_PRIVATE).getString("jwt", null)

    private fun goToLogin() {
        Toast.makeText(this, "Sesión expirada. Volvé a loguear.", Toast.LENGTH_SHORT).show()
        val i = Intent(this, NfcLoginActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(); for (b in this) sb.append(String.format("%02X", b)); return sb.toString()
    }
}
