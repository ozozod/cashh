package com.example.vayvene.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.data.ApiClient
import com.example.vayvene.data.TokenStore
import com.example.vayvene.ui.login.NfcLoginActivity
import com.example.vayvene.ui.main.CashierMenuActivity
import com.example.vayvene.ui.main.SellerMenuActivity

class AdminMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu)

        // ===== Navegación a pantallas ya existentes =====
        // Registrar tarjeta (ya está en tu proyecto)
        findViewById<Button>(R.id.btnRegisterCard).setOnClickListener {
            startActivity(Intent(this, RegisterCardActivity::class.java))
        }

        // Operaciones de Vendedor (reuso de flujo vendedor: POS / Consultar / Resumen / Anular)
        findViewById<Button>(R.id.btnSellerOps).setOnClickListener {
            startActivity(Intent(this, SellerMenuActivity::class.java))
        }

        // Operaciones de Cajero (Cargar / Retirar / Consultar)
        findViewById<Button>(R.id.btnCashierOps).setOnClickListener {
            startActivity(Intent(this, CashierMenuActivity::class.java))
        }

        // ===== Botones “stub” (a completar) =====
        findViewById<Button>(R.id.btnStaff).setOnClickListener {
            Toast.makeText(this, "Gestión de staff: en construcción", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnSelectEvent).setOnClickListener {
            Toast.makeText(this, "Seleccionar evento: en construcción", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnReports).setOnClickListener {
            Toast.makeText(this, "Reportes/Export: en construcción", Toast.LENGTH_SHORT).show()
        }

        // ===== Logout =====
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            try {
                // Limpio token del interceptor y (si existe) del almacenamiento local
                ApiClient.setToken(null)
                try { TokenStore.clear(this) } catch (_: Throwable) { /* si no existe TokenStore, ignora */ }

                val i = Intent(this, NfcLoginActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Error al cerrar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
