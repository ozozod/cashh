package com.example.vayvene.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R

class SellerMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Este layout debe tener los 5 botones con ids abajo
        setContentView(R.layout.activity_role_menu)

        val btn1 = findViewById<Button>(R.id.btnAction1) // Nueva venta
        val btn2 = findViewById<Button>(R.id.btnAction2) // Consultar saldo
        val btn3 = findViewById<Button>(R.id.btnAction3) // Resumen
        val btn4 = findViewById<Button>(R.id.btnAction4) // Anular venta
        val btn5 = findViewById<Button>(R.id.btnAction5) // Cerrar sesión

        btn1.setOnClickListener { Toast.makeText(this, "TODO: Nueva venta", Toast.LENGTH_SHORT).show() }
        btn2.setOnClickListener { Toast.makeText(this, "TODO: Consultar saldo", Toast.LENGTH_SHORT).show() }
        btn3.setOnClickListener { startActivity(Intent(this, SellerSummaryActivity::class.java)) }
        btn4.setOnClickListener { Toast.makeText(this, "TODO: Anular venta (requiere Encargado)", Toast.LENGTH_SHORT).show() }
        btn5.setOnClickListener {
            // TODO: limpiar token si usás persistencia
            finish()
        }
    }
}
