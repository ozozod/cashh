package com.example.vayvene.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.data.JwtUtils
import com.example.vayvene.ui.login.NfcLoginActivity

class RoleMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val token = prefs.getString("jwt", null)

        if (token.isNullOrBlank()) {
            startActivity(Intent(this, NfcLoginActivity::class.java))
            finish()
            return
        }

        val role = (JwtUtils.getRole(token) ?: "").uppercase()

        when (role) {
            "ADMINISTRADOR", "ADMIN", "ENCARGADO" -> {
                // OJO: el AdminMenuActivity está en el paquete ui.admin (no ui.main)
                startActivity(Intent(this, com.example.vayvene.ui.admin.AdminMenuActivity::class.java))
            }
            "VENDEDOR" -> {
                startActivity(Intent(this, com.example.vayvene.ui.main.SellerMenuActivity::class.java))
            }
            "CAJERO" -> {
                // podés cambiar esto a una pantalla de caja cuando la tengas
                startActivity(Intent(this, com.example.vayvene.ui.main.MainActivity::class.java))
            }
            else -> {
                Toast.makeText(this, "Rol no soportado: $role", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, com.example.vayvene.ui.main.MainActivity::class.java))
            }
        }
        finish()
    }
}
