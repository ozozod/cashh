package com.example.vayvene.ui.cashier

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.ui.login.NfcLoginActivity

/**
 * Menú de Cajero.
 * Arregla "Cerrar sesión": limpia prefs y vuelve a NfcLoginActivity.
 */
class CashierMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cashier_menu)

        findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
            // limpiar sesión
            getSharedPreferences("session", MODE_PRIVATE).edit().clear().apply()
            // volver a login
            val i = Intent(this, NfcLoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }

        // Dejé el resto de botones como estaban en tu layout original
        // R.id.btnRegisterCustomer, R.id.btnTopup, R.id.btnBalance, R.id.btnRefund, etc.
    }
}
