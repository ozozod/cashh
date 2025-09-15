package com.example.vayvene.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.data.Repository
import com.example.vayvene.data.Session
import com.example.vayvene.ui.admin.AdminMenuActivity
import com.example.vayvene.ui.seller.SellerMenuActivity
import com.example.vayvene.ui.cashier.CashierMenuActivity
import com.example.vayvene.ui.common.EXTRA_UID
import org.json.JSONObject

class NfcLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_login) // Asegurate que exista y tenga tus views

        // UID de la tarjeta (reversed) que te llega del capturador NFC:
        val uid = intent.getStringExtra(EXTRA_UID)
        if (uid != null) {
            doLogin(uid)
        } else {
            // si tu flujo primero captura, navega a tu capturador aquí
            // startActivity(Intent(this, NfcPromptActivity::class.java))
        }
    }

    private fun doLogin(cardUidReversed: String) {
        Repository.login(
            this, cardUidReversed,
            { json -> handleLoginOk(json) },
            { e -> Toast.makeText(this, "Error de login: ${e.message}", Toast.LENGTH_LONG).show() }
        )

    }

    private fun handleLoginOk(json: JSONObject) {
        val token        = json.optString("token")
        val eventId      = json.optString("eventId")
        val staffRole    = json.optString("staffRole")
        val staffName    = json.optString("staffName")
        val staffCardUid = json.optString("staffCardUid")
        val isStaff      = json.optBoolean("isStaff", false)

        if (token.isBlank() || eventId.isBlank()) {
            Toast.makeText(this, "Respuesta de login inválida", Toast.LENGTH_LONG).show()
            return
        }

        Session.setLogin(
            ctx = this,
            token = token,
            eventId = eventId,
            role = staffRole,
            isStaff = isStaff,
            staffName = staffName,
            staffCardUid = staffCardUid
        )

        when (staffRole.lowercase()) {
            "admin"    -> startActivity(Intent(this, com.example.vayvene.ui.admin.AdminMenuActivity::class.java))
            "cajero"   -> startActivity(Intent(this, com.example.vayvene.ui.cashier.CashierMenuActivity::class.java))
            "vendedor" -> startActivity(Intent(this, com.example.vayvene.ui.seller.SellerMenuActivity::class.java))
            else -> {
                Toast.makeText(this, "Tarjeta no registrada para staff", Toast.LENGTH_LONG).show()
                // se queda en login
            }
        }
        finish()
    }
}
