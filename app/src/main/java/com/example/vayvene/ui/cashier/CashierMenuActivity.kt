package com.example.vayvene.ui.cashier

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.data.Session
import com.example.vayvene.ui.login.NfcLoginActivity

class CashierMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cashier_menu) // asegurate de tener este layout

        // Si no hay token, volvé a login
        if (Session.jwt(this).isNullOrBlank()) {
            startActivity(Intent(this, NfcLoginActivity::class.java))
            finish()
            return
        }

        // TODO: conectar botones: registrar comprador, cargar saldo, consultar, devolver, cerrar sesión…
    }
}
