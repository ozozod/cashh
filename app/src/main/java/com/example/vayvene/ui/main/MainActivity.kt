package com.example.vayvene.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.data.TokenHolder
import com.example.vayvene.ui.login.NfcLoginActivity

/**
 * Pantalla puente:
 * - Si hay token guardado -> va al menú de roles
 * - Si no -> va a la pantalla de login por NFC
 *
 * No crea Repository (ya no hace falta aquí).
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val token = TokenHolder.getToken(this)
        val next = if (!token.isNullOrBlank()) {
            Intent(this, RoleMenuActivity::class.java)
        } else {
            Intent(this, NfcLoginActivity::class.java)
        }

        next.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(next)
    }
}
