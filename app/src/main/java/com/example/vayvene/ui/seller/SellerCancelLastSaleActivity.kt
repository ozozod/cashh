package com.example.vayvene.ui.seller

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.BuildConfig
import com.example.vayvene.R
import com.example.vayvene.data.Session
import com.example.vayvene.ui.nfc.NfcCaptureActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class SellerCancelLastSaleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MANAGER_UID = "extra_manager_uid"
    }

    private val http by lazy { OkHttpClient() }

    private lateinit var tvInfo: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnAuth: Button     // fallback por si entran sin autorización previa
    private lateinit var btnCancel: Button
    private lateinit var progress: ProgressBar

    private var saleId: String? = null
    private var customerUid: String? = null
    private var managerUid: String? = null
    private var busy = false

    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

    // Fallback: escaneo de ENCARGADO si el Activity llegó sin autorización previa
    private val scanMgrLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { r ->
        if (r.resultCode == RESULT_OK) {
            managerUid = r.data?.getStringExtra(NfcCaptureActivity.EXTRA_UID)?.uppercase()
            if (managerUid.isNullOrBlank()) {
                toast("Tarjeta de encargado inválida. Probá de nuevo.")
                return@registerForActivityResult
            }
            tvStatus.text = "Autorización OK. Podés anular."
            updateButtons()
        }
    }

    // Escaneo del CLIENTE y anulación
    private val scanCustomerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { r ->
        if (r.resultCode == RESULT_OK) {
            val cust = r.data?.getStringExtra(NfcCaptureActivity.EXTRA_UID)?.uppercase()
            if (cust.isNullOrBlank()) {
                toast("Tarjeta cliente inválida.")
                return@registerForActivityResult
            }
            doCancel(cust)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_cancel_last_sale)

        tvInfo = findViewById(R.id.tvInfo)
        tvStatus = findViewById(R.id.tvStatus)
        btnAuth = findViewById(R.id.btnAuth)
        btnCancel = findViewById(R.id.btnCancel)
        progress = findViewById(R.id.progress)

        // Trae el managerUid desde el menú (pre-escan de encargado)
        managerUid = intent.getStringExtra(EXTRA_MANAGER_UID)?.uppercase()

        // Si ya vino autorizado, ocultamos "Autorizar"
        if (!managerUid.isNullOrBlank()) {
            btnAuth.visibility = View.GONE
            tvStatus.text = "Autorización OK. Podés anular."
        } else {
            btnAuth.visibility = View.VISIBLE
            tvStatus.text = "Pedí autorización de ENCARGADO/ADMIN"
            btnAuth.setOnClickListener {
                val i = Intent(this, NfcCaptureActivity::class.java)
                i.putExtra(
                    NfcCaptureActivity.EXTRA_PROMPT,
                    "Acercá tarjeta de ENCARGADO/ADMIN para autorizar"
                )
                scanMgrLauncher.launch(i)
            }
        }

        btnCancel.setOnClickListener {
            if (saleId == null) { toast("No hay venta para anular"); return@setOnClickListener }
            if (managerUid.isNullOrBlank()) { toast("Falta autorización"); return@setOnClickListener }
            val i = Intent(this, NfcCaptureActivity::class.java)
            i.putExtra(
                NfcCaptureActivity.EXTRA_PROMPT,
                "Acercá la tarjeta del CLIENTE para anular y devolver saldo"
            )
            scanCustomerLauncher.launch(i)
        }

        updateButtons()
        loadLastSale()
    }

    private fun loadLastSale() {
        val token = Session.jwt(this)
        if (token.isNullOrBlank()) {
            toast("Sesión expirada")
            finish(); return
        }

        val url = "${BuildConfig.BASE_URL.trimEnd('/')}/api/mobile/last-sale"
        val req = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()

        setBusy(true)
        Thread {
            try {
                http.newCall(req).execute().use { res ->
                    val body = res.body?.string().orEmpty()
                    runOnUiThread {
                        if (!res.isSuccessful) {
                            setBusy(false)
                            toast("Error al cargar (${res.code})")
                            finish()
                            return@runOnUiThread
                        }

                        val sale = JSONObject(body).optJSONObject("sale")
                        if (sale == null || sale.isNull("id")) {
                            tvInfo.text = "No hay ventas para anular."
                            tvStatus.text = ""
                            saleId = null
                            setBusy(false)
                            updateButtons()
                            return@runOnUiThread
                        }

                        saleId = sale.optString("id")
                        customerUid = sale.optString("customerUid")
                        val amount = sale.optDouble("amount", 0.0)
                        val det = sale.optString("details", "")
                        tvInfo.text = "Última venta\nTotal: ${moneyFmt.format(Math.abs(amount))}\nCliente: ${customerUid}\nDetalles: ${det}"

                        setBusy(false)
                        updateButtons()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setBusy(false)
                    toast("Red: ${e.message}")
                    finish()
                }
            }
        }.start()
    }

    private fun doCancel(scannedCustomerUid: String) {
        val token = Session.jwt(this)
        if (token.isNullOrBlank()) {
            toast("Sesión expirada")
            finish(); return
        }
        val sid = saleId
        if (sid == null) {
            toast("Sin venta")
            finish(); return
        }
        val mgr = managerUid
        if (mgr.isNullOrBlank()) {
            toast("Falta autorización")
            return
        }

        val url = "${BuildConfig.BASE_URL.trimEnd('/')}/api/mobile/cancel-last-sale"
        val payload = JSONObject().apply {
            put("saleId", sid)
            put("managerCardUid", mgr)
            put("customerUid", scannedCustomerUid.uppercase())
        }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val req = Request.Builder()
            .url(url)
            .post(payload)
            .addHeader("Authorization", "Bearer $token")
            .build()

        setBusy(true)
        Thread {
            try {
                http.newCall(req).execute().use { res ->
                    val body = res.body?.string().orEmpty()
                    runOnUiThread {
                        setBusy(false)
                        if (res.code == 401) { toast("Sesión expirada"); finish(); return@runOnUiThread }
                        if (res.isSuccessful) {
                            val newBal = try { JSONObject(body).optDouble("newBalance") } catch (_: Exception) { null }
                            val msg = if (newBal != null)
                                "Anulado ✔  Nuevo saldo: ${moneyFmt.format(newBal)}"
                            else "Anulado ✔"
                            tvStatus.text = msg
                            toast("Venta anulada")
                            btnCancel.isEnabled = false
                        } else {
                            toast("Error (${res.code}): $body")
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { setBusy(false); toast("Red: ${e.message}") }
            }
        }.start()
    }

    private fun setBusy(b: Boolean) {
        busy = b
        progress.visibility = if (b) View.VISIBLE else View.GONE
        updateButtons()
    }

    private fun updateButtons() {
        // Anular habilitado solo si hay venta cargada, hay autorización y no está ocupado
        btnCancel.isEnabled = !busy && saleId != null && !managerUid.isNullOrBlank()
        // Botón Autorizar visible solo si no tenemos managerUid
        btnAuth.isEnabled = !busy && managerUid.isNullOrBlank()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
