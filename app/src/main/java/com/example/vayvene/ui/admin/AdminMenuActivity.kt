package com.example.vayvene.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.data.ApiClient
import com.example.vayvene.ui.login.NfcLoginActivity

class AdminMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu)

        // TARJETAS
        findViewById<Button>(R.id.btnCards).setOnClickListener {
            startActivity(Intent(this, AdminCardsMenuActivity::class.java))
        }
        // CAJERO
        findViewById<Button>(R.id.btnCashier).setOnClickListener {
            startActivity(Intent(this, com.example.vayvene.ui.cashier.CashierQuickOpsActivity::class.java))
        }
        // VENDEDOR
        findViewById<Button>(R.id.btnSeller).setOnClickListener {
            startActivity(Intent(this, AdminSellerMenuActivity::class.java))
        }
        // CERRAR SESIÓN
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            // limpia prefs y token en memoria
            getSharedPreferences("session", MODE_PRIVATE).edit().remove("jwt").apply()
            try { ApiClient.setToken(null) } catch (_: Throwable) { }

            val i = Intent(this, NfcLoginActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
            finish()
        }
    }
}
