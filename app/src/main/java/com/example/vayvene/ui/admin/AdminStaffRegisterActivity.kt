package com.example.vayvene.ui.admin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
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
import java.util.Locale

class AdminStaffRegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmployeeNumber: EditText
    private lateinit var spRole: Spinner
    private lateinit var tvUid: TextView
    private lateinit var btnScan: Button
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button

    private val http by lazy { OkHttpClient() }
    private var pendingSaveAfterScan = false

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
            val uid = res.data?.getStringExtra(NfcCaptureActivity.EXTRA_UID).orEmpty()
            if (uid.isNotBlank()) {
                tvUid.text = uid
                if (pendingSaveAfterScan) {
                    pendingSaveAfterScan = false
                    saveStaff()
                }
            }
        } else {
            pendingSaveAfterScan = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_staff_register)

        etName = findViewById(R.id.etName)
        etEmployeeNumber = findViewById(R.id.etEmployeeNumber)
        spRole = findViewById(R.id.spRole)
        tvUid = findViewById(R.id.tvUid)
        btnScan = findViewById(R.id.btnScan)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        spRole.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("ADMINISTRADOR", "ENCARGADO", "VENDEDOR", "CAJERO")
        )

        btnScan.setOnClickListener { openScan("Acercá la tarjeta de STAFF") }
        btnSave.setOnClickListener { saveStaff() }
        btnBack.setOnClickListener { finish() }
    }

    private fun saveStaff() {
        val name = etName.text.toString().trim()
        val employee = etEmployeeNumber.text.toString().trim().ifBlank { null }
        val role = (spRole.selectedItem?.toString() ?: "VENDEDOR").uppercase(Locale.ROOT)
        val cardUid = tvUid.text.toString().trim().uppercase(Locale.ROOT)

        if (name.isEmpty()) { toast("Falta nombre"); return }
        if (cardUid.isEmpty()) { pendingSaveAfterScan = true; openScan("Acercá la tarjeta de STAFF para asignarla"); return }

        Thread {
            val token = getToken() ?: run { runOnUiThread { goToLogin() }; return@Thread }

            val eventId = resolveEventIdBlocking(token)
            if (eventId.isNullOrBlank()) {
                runOnUiThread { toast("No pude determinar el evento.") }
                return@Thread
            }

            val url = BuildConfig.BASE_URL.trimEnd('/') + "/api/events/$eventId/users"
            val payload = JSONObject().apply {
                put("name", name)
                put("role", role)
                put("cardUid", cardUid)
                if (employee != null) put("employeeNumber", employee)
            }

            val req = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Authorization", "Bearer $token")
                .build()

            try {
                val res = http.newCall(req).execute()
                val body = res.body?.string().orEmpty()
                runOnUiThread {
                    if (!res.isSuccessful) toast("Error ${res.code}: $body")
                    else { toast("Staff guardado ✔"); finish() }
                }
            } catch (e: Exception) {
                runOnUiThread { toast("Error: ${e.message}") }
            }
        }.start()
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

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun resolveEventIdBlocking(token: String): String? {
        val prefs = getSharedPreferences("session", Context.MODE_PRIVATE)
        // cache
        prefs.getString("eventId", null)?.let { if (it.isNotBlank()) return it }

        // JWT
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
                val req = Request.Builder().url(u).get().addHeader("Authorization", "Bearer $token").build()
                http.newCall(req).execute().use { res ->
                    val body = res.body?.string().orEmpty()
                    if (!res.isSuccessful) return@use
                    val id = parseEventIdFrom(body)
                    if (!id.isNullOrBlank()) {
                        prefs.edit().putString("eventId", id).apply()
                        return id
                    }
                }
            } catch (_: Exception) { /* next */ }
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
}
