package com.example.vayvene.ui.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.ui.common.Extras
import com.example.vayvene.ui.nfc.NfcCaptureActivity

class AdminStaffRegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var spRole: Spinner
    private lateinit var tvCardUid: TextView
    private lateinit var etEmployee: EditText
    private lateinit var btnScan: Button
    private lateinit var btnSave: Button

    private lateinit var scanLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asegurate de que este sea tu layout real o usa el que te dejo abajo
        setContentView(R.layout.activity_admin_staff_register)

        // Referencias del layout (ids deben existir en el XML)
        etName = findViewById(R.id.etName)
        spRole = findViewById(R.id.spRole)
        tvCardUid = findViewById(R.id.tvCardUid)
        etEmployee = findViewById(R.id.etEmployee)
        btnScan = findViewById(R.id.btnScan)
        btnSave = findViewById(R.id.btnSave)

        // Spinner simple para roles (ajusta si ya tenés un adapter propio)
        if (spRole.adapter == null) {
            val roles = listOf("ADMINISTRADOR", "CAJERO", "VENDEDOR")
            spRole.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        }

        // Registrar launcher AQUÍ (ya existen las views)
        scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                val uid = res.data?.getStringExtra(Extras.EXTRA_UID).orEmpty()
                tvCardUid.text = uid
            } else {
                Toast.makeText(this, getString(R.string.nfc_read_fail), Toast.LENGTH_SHORT).show()
            }
        }

        btnScan.setOnClickListener {
            val i = Intent(this, NfcCaptureActivity::class.java)
            // Opcional: mensaje de la pantalla de NFC si lo usás
            i.putExtra(Extras.EXTRA_PROMPT, getString(R.string.nfc_prompt_register_staff))
            scanLauncher.launch(i)
        }

        btnSave.setOnClickListener {
            // TODO: acá iría el POST a tu API /api/mobile/staff/register
            // usando etName.text, spRole.selectedItem, tvCardUid.text, etc.
            Toast.makeText(this, "Guardar staff (TODO)", Toast.LENGTH_SHORT).show()
        }
    }
}
