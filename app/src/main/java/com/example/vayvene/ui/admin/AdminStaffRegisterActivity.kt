package com.example.vayvene.ui.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.BuildConfig
import com.example.vayvene.R
import com.example.vayvene.ui.nfc.NfcCaptureActivity
import com.example.vayvene.util.Extras
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AdminStaffRegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmployee: EditText
    private lateinit var spRole: Spinner
    private lateinit var tvCardUid: TextView
    private lateinit var btnScan: Button
    private lateinit var btnSave: Button

    private val client = OkHttpClient()
    private var eventId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_staff_register)

        eventId = intent.getStringExtra(Extras.EXTRA_EVENT_ID)

        etName = findViewById(R.id.etName)
        etEmployee = findViewById(R.id.etEmployee)
        spRole = findViewById(R.id.spRole)
        tvCardUid = findViewById(R.id.tvCardUid)
        btnScan = findViewById(R.id.btnScan)
        btnSave = findViewById(R.id.btnSave)

        ArrayAdapter.createFromResource(
            this,
            R.array.roles_staff,
            android.R.layout.simple_spinner_item
        ).also { a ->
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spRole.adapter = a
        }

        btnScan.setOnClickListener {
            val i = Intent(this, NfcCaptureActivity::class.java)
                .putExtra(Extras.EXTRA_PROMPT, getString(R.string.nfc_prompt_register_staff))
            startActivityForResult(i, Extras.REQ_NFC_CAPTURE)
        }

        btnSave.setOnClickListener { saveStaff() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Extras.REQ_NFC_CAPTURE && resultCode == Activity.RESULT_OK) {
            val uid = data?.getStringExtra(Extras.EXTRA_UID)?.uppercase()
            if (uid.isNullOrBlank()) {
                Toast.makeText(this, getString(R.string.nfc_read_fail), Toast.LENGTH_SHORT).show()
            } else {
                tvCardUid.text = uid
            }
        }
    }

    private fun saveStaff() {
        val eid = eventId
        if (eid.isNullOrBlank()) {
            Toast.makeText(this, "Falta eventId (login). Volvé a loguearte.", Toast.LENGTH_LONG).show()
            return
        }

        val name = etName.text.toString().trim()
        val empNum = etEmployee.text.toString().trim()
        val role = spRole.selectedItem?.toString()?.trim().orEmpty()
        val cardUid = tvCardUid.text.toString().trim().uppercase()

        if (name.isEmpty()) { etName.error = "Requerido"; return }
        if (cardUid.isEmpty()) {
            Toast.makeText(this, "Escaneá una tarjeta", Toast.LENGTH_LONG).show(); return
        }

        val url = BuildConfig.BASE_URL.trimEnd('/') + "/api/events/$eid/users"
        val payload = JSONObject().apply {
            put("name", name)
            put("role", role)
            put("cardUid", cardUid)
            if (empNum.isNotEmpty()) put("employeeNumber", empNum)
        }

        val body = payload.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder().url(url).post(body).build()

        btnSave.isEnabled = false
        Thread {
            try {
                client.newCall(req).execute().use { resp ->
                    val code = resp.code
                    val text = resp.body?.string().orEmpty()
                    runOnUiThread {
                        btnSave.isEnabled = true
                        when {
                            code in 200..299 -> {
                                Toast.makeText(this, "Staff guardado ✅", Toast.LENGTH_LONG).show()
                                etName.text?.clear()
                                etEmployee.text?.clear()
                                tvCardUid.text = ""
                            }
                            code == 409 -> {
                                Toast.makeText(this, "Esa tarjeta ya está asignada en este evento.", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this, "Error ($code): $text", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
