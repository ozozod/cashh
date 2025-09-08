package com.example.vayvene.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.ui.admin.RegisterCardActivity

/**
 * Pantalla de Admin con dos botones (según tu layout).
 * Quité referencias a 'token' y 'role' (no existen como globales).
 */
class AdminMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu)

        val btnStaff = findViewById<Button>(R.id.btnRegistrarStaff)
        val btnComprador = findViewById<Button>(R.id.btnRegistrarComprador)

        btnStaff.setOnClickListener {
            // Abre registro de tarjeta para staff
            val i = Intent(this, RegisterCardActivity::class.java)
            i.putExtra("mode", "staff")
            startActivity(i)
        }

        btnComprador.setOnClickListener {
            // Abre registro de tarjeta para comprador
            val i = Intent(this, RegisterCardActivity::class.java)
            i.putExtra("mode", "buyer")
            startActivity(i)
        }

        // Si querés cerrar sesión acá, agregamos un 3er botón en el layout.
        // Por ahora dejo un hint:
        Toast.makeText(this, "Admin listo. Usa Atrás para volver.", Toast.LENGTH_SHORT).show()
    }
}
