package com.example.vayvene.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.ui.main.BalanceActivity
import com.example.vayvene.ui.main.CashierMenuActivity

class AdminCashierMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_cashier_menu)

        // Registrar COMPRADOR (primera vez)
        findViewById<Button>(R.id.btnRegisterCustomer).setOnClickListener {
            // Reutiliza la pantalla de register (comprador)
            val i = Intent(this, RegisterCardActivity::class.java)
            // i.putExtra("MODE", "CUSTOMER")
            startActivity(i)
        }

        // Cargar saldo (podés abrir tu menú de cajero si ahí elegís la acción)
        findViewById<Button>(R.id.btnRecharge).setOnClickListener {
            startActivity(Intent(this, CashierMenuActivity::class.java))
        }

        // Consultar saldo directo
        findViewById<Button>(R.id.btnCheckBalance).setOnClickListener {
            startActivity(Intent(this, BalanceActivity::class.java))
        }

        // Volver
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }
}
