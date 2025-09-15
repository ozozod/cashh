package com.example.vayvene.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.data.Session
import com.example.vayvene.ui.login.NfcLoginActivity
import com.example.vayvene.ui.nfc.NfcCaptureActivity
import com.example.vayvene.ui.seller.SellerCancelLastSaleActivity
import com.example.vayvene.ui.seller.SellerPosActivity

class SellerMenuActivity : AppCompatActivity() {

    private val scanMgrLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        if (r.resultCode == RESULT_OK) {
            val managerUid = r.data?.getStringExtra(NfcCaptureActivity.EXTRA_UID)?.uppercase()
            if (managerUid.isNullOrBlank()) {
                Toast.makeText(this, "Tarjeta inválida. Probá de nuevo.", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            val i = Intent(this, SellerCancelLastSaleActivity::class.java)
            i.putExtra(SellerCancelLastSaleActivity.EXTRA_MANAGER_UID, managerUid)
            startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_menu)

        val btnNuevaVenta: Button = findViewById(R.id.btnNuevaVenta)
        val btnConsultarSaldo: Button = findViewById(R.id.btnConsultarSaldo)
        val btnResumen: Button = findViewById(R.id.btnResumen)
        val btnAnularVenta: Button = findViewById(R.id.btnAnularVenta)
        val btnLogout: Button = findViewById(R.id.btnLogout)

        btnNuevaVenta.setOnClickListener {
            startActivity(Intent(this, SellerPosActivity::class.java))
        }

        btnConsultarSaldo.setOnClickListener {
            Toast.makeText(this, "Pendiente de UI (Consultar saldo)", Toast.LENGTH_SHORT).show()
        }
        btnResumen.setOnClickListener {
            Toast.makeText(this, "Pendiente de UI (Resumen)", Toast.LENGTH_SHORT).show()
        }

        btnAnularVenta.setOnClickListener {
            // 👉 Si es ENCARGADO/ADMIN, no pedimos tarjeta
            if (Session.isManagerOrAdmin(this)) {
                val selfCard = Session.staffCardUid(this)
                val i = Intent(this, SellerCancelLastSaleActivity::class.java)
                i.putExtra(SellerCancelLastSaleActivity.EXTRA_MANAGER_UID, selfCard)
                startActivity(i)
            } else {
                // Vendedor: pedimos tarjeta de ENCARGADO/ADMIN
                val i = Intent(this, NfcCaptureActivity::class.java)
                i.putExtra(
                    NfcCaptureActivity.EXTRA_PROMPT,
                    "Acercá tarjeta de ENCARGADO/ADMIN para autorizar"
                )
                scanMgrLauncher.launch(i)
            }
        }

        btnLogout.setOnClickListener {
            Session.clear(this)
            val i = Intent(this, NfcLoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }
    }
}
