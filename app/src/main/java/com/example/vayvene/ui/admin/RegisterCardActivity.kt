package com.example.vayvene.ui.admin

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import org.json.JSONObject
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL

class RegisterCardActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_STAFF = "staff"
        const val MODE_BUYER = "buyer"
    }

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var readingEnabled = false
    private var mode: String = MODE_BUYER
    private var inFlight = false

    // UI (form STAFF)
    private lateinit var groupForm: View
    private lateinit var etNombre: EditText
    private lateinit var etNumero: EditText
    private lateinit var spRol: Spinner
    private lateinit var btnOkForm: Button

    // UI (NFC)
    private lateinit var groupNfc: View
    private lateinit var tvNfcMsg: TextView
    private lateinit var progress: ProgressBar

    // datos de formulario
    private var nombreIngresado: String? = null
    private var numeroIngresado: String? = null
    private var rolSeleccionado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_card)

        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_BUYER

        groupForm = findViewById(R.id.groupForm)
        etNombre = findViewById(R.id.etNombre)
        etNumero = findViewById(R.id.etNumero)
        spRol = findViewById(R.id.spRol)
        btnOkForm = findViewById(R.id.btnOkForm)

        groupNfc = findViewById(R.id.groupNfc)
        tvNfcMsg = findViewById(R.id.tvNfcMsg)
        progress = findViewById(R.id.progress)

        // NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "Este dispositivo no tiene NFC", Toast.LENGTH_LONG).show()
            finish(); return
        }
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if (mode == MODE_STAFF) {
            title = "Registrar STAFF"
            val adapter = ArrayAdapter.createFromResource(
                this, R.array.staff_roles, android.R.layout.simple_spinner_item
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spRol.adapter = adapter
            showForm()
        } else {
            title = "Registrar COMPRADOR"
            showNfcStep()
        }

        btnOkForm.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val numero = etNumero.text.toString().trim()
            if (nombre.isEmpty()) { etNombre.error = "Requerido"; return@setOnClickListener }
            nombreIngresado = nombre
            numeroIngresado = if (numero.isEmpty()) null else numero
            rolSeleccionado = spRol.selectedItem?.toString()
            showNfcStep()
        }

        intent?.let { handleIntent(it) }
    }

    override fun onResume() {
        super.onResume()
        if (readingEnabled) nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) handleIntent(intent)
    }

    private fun showForm() {
        readingEnabled = false
        nfcAdapter?.disableForegroundDispatch(this)
        groupForm.visibility = View.VISIBLE
        groupNfc.visibility = View.GONE
    }

    private fun showNfcStep() {
        groupForm.visibility = View.GONE
        groupNfc.visibility = View.VISIBLE
        tvNfcMsg.text = "Acercá la tarjeta…"
        progress.visibility = View.VISIBLE
        readingEnabled = true
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    private fun handleIntent(intent: Intent) {
        if (!readingEnabled || inFlight) return
        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        if (tag != null) {
            readingEnabled = false
            nfcAdapter?.disableForegroundDispatch(this)
            val uid = tag.id?.toHexString() ?: ""
            enviarRegistro(uid)
        }
    }

    private fun enviarRegistro(uid: String) {
        inFlight = true
        val baseUrl = getString(R.string.base_url)
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("auth_token", null)
        val eventId = getSharedPreferences("app", MODE_PRIVATE).getString("event_id", null)

        if (token.isNullOrBlank() || eventId.isNullOrBlank()) {
            Toast.makeText(this, "Falta sesión o evento", Toast.LENGTH_LONG).show()
            finish(); return
        }

        Thread {
            try {
                val json = JSONObject().apply {
                    put("event_id", eventId)
                    put("uid", uid)
                    put("cardType", if (mode == MODE_STAFF) "STAFF" else "BUYER")
                    if (mode == MODE_STAFF) {
                        put("name", nombreIngresado)
                        if (!numeroIngresado.isNullOrEmpty()) put("number", numeroIngresado)
                        put("role", rolSeleccionado)
                    }
                }
                val (ok, resp) = postJson("$baseUrl/mobile/register-card", json.toString(), token)

                runOnUiThread {
                    inFlight = false
                    val success = ok && resp?.optBoolean("ok") == true
                    val msg = resp?.optString("message") ?: if (success) "Registrado" else "Error"
                    val i = Intent(this, RegisterResultActivity::class.java)
                        .putExtra("ok", success)
                        .putExtra("message", msg)
                        .putExtra("mode", mode)
                        .putExtra("uid", uid)
                    startActivity(i)
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    inFlight = false
                    val i = Intent(this, RegisterResultActivity::class.java)
                        .putExtra("ok", false)
                        .putExtra("message", "Error interno")
                        .putExtra("mode", mode)
                        .putExtra("uid", uid)
                    startActivity(i); finish()
                }
            }
        }.start()
    }

    private fun postJson(url: String, jsonBody: String, bearer: String): Pair<Boolean, JSONObject?> {
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Authorization", "Bearer $bearer")
                doOutput = true
                connectTimeout = 10000; readTimeout = 15000
            }
            conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                .bufferedReader().use { it.readText() }
            Pair(code in 200..299, JSONObject(text))
        } catch (_: Exception) {
            Pair(false, null)
        }
    }
}

private fun ByteArray.toHexString(): String =
    joinToString("") { String.format(Locale.US, "%02X", it) }
