package com.example.vayvene.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.data.Session
import com.example.vayvene.ui.cashier.CashierQuickOpsActivity
import com.example.vayvene.ui.login.NfcLoginActivity
import com.example.vayvene.ui.seller.SellerMenuActivity

class AdminMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu)

        val btnTarjetas: Button = findViewById(R.id.btnTarjetas)
        val btnCajero: Button = findViewById(R.id.btnCajero)
        val btnVendedor: Button = findViewById(R.id.btnVendedor)
        val btnLogout: Button = findViewById(R.id.btnLogout)

        btnTarjetas.setOnClickListener {
            startActivity(Intent(this, AdminCardsMenuActivity::class.java))
        }

        btnCajero.setOnClickListener {
            startActivity(Intent(this, CashierQuickOpsActivity::class.java))
        }

        // SellerMenuActivity está en ui.main (¡ojo con el import!)
        btnVendedor.setOnClickListener {
            startActivity(Intent(this, SellerMenuActivity::class.java))
        }

        btnLogout.setOnClickListener {
            Session.clear(this)
            val i = Intent(this, NfcLoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }
    }
}