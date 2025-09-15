package com.example.vayvene.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.ui.common.EXTRA_PROMPT
import com.example.vayvene.ui.common.EXTRA_UID
import com.example.vayvene.ui.nfc.NfcCaptureActivity

class AdminStaffRegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmployeeNumber: EditText
    private lateinit var spRole: Spinner
    private lateinit var btnScan: Button
    private lateinit var tvUid: TextView
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uid = result.data?.getStringExtra(EXTRA_UID)
            tvUid.text = uid ?: ""
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_staff_register)

        etName = findViewById(R.id.etName)
        etEmployeeNumber = findViewById(R.id.etEmployeeNumber)
        spRole = findViewById(R.id.spRole)
        btnScan = findViewById(R.id.btnScan)
        tvUid = findViewById(R.id.tvUid)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        // Importante: usamos TU layout local del spinner (no android.R)
        spRole.adapter = ArrayAdapter(
            this,
            R.layout.simple_spinner_dropdown_item,
            listOf("ADMIN", "CAJERO", "VENDEDOR")
        )

        btnScan.setOnClickListener { openScan("Acerque tarjeta para registrar staff") }
        btnSave.setOnClickListener {
            // TODO: POST /mobile/staff/register con (eventId, uid, role, name, telemetría)
            Toast.makeText(this, "Guardar staff (TODO)", Toast.LENGTH_SHORT).show()
        }
        btnBack.setOnClickListener { finish() }
    }

    private fun openScan(prompt: String) {
        val i = Intent(this, NfcCaptureActivity::class.java)
        i.putExtra(EXTRA_PROMPT, prompt)
        scanLauncher.launch(i)
    }
}
