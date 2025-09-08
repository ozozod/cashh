package com.example.vayvene.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.data.TokenHolder
import com.example.vayvene.ui.login.NfcLoginActivity

/**
 * Menú del Vendedor con 5 botones.
 * Si tu layout aún no tiene esos IDs, no crashea (solo avisa).
 */
class SellerMenuActivity : AppCompatActivity() {

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usamos el layout que mencionaste para el menú de roles del vendedor.
        setContentView(R.layout.activity_role_menu)

        val btn1: Button? = findViewById(R.id.btnAction1)   // Nueva venta
        val btn2: Button? = findViewById(R.id.btnAction2)   // Consultar saldo
        val btn3: Button? = findViewById(R.id.btnAction3)   // Resumen
        val btn4: Button? = findViewById(R.id.btnAction4)   // Anular venta
        val btn5: Button? = findViewById(R.id.btnAction5)   // Cerrar sesión

        btn1?.setOnClickListener { toast("Nueva venta (pendiente de UI)") }
        btn2?.setOnClickListener { toast("Consultar saldo (pendiente de UI)") }
        btn3?.setOnClickListener {
            startActivity(Intent(this, SellerSummaryActivity::class.java))
        }
        btn4?.setOnClickListener { toast("Anular venta (pendiente de UI)") }
        btn5?.setOnClickListener {
            // Cerrar sesión
            TokenHolder.clear(this)
            val i = Intent(this, NfcLoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }

        if (listOf(btn1, btn2, btn3, btn4, btn5).all { it == null }) {
            toast("Faltan los 5 botones en el layout del vendedor")
        }
    }
}
