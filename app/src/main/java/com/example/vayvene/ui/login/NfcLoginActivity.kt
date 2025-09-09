package com.example.vayvene.ui.login

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vayvene.R
import com.example.vayvene.data.ApiClient
import com.example.vayvene.data.Repository
import com.example.vayvene.data.LoginRequest
import com.example.vayvene.data.JwtUtils
import com.example.vayvene.ui.admin.AdminMenuActivity
import com.example.vayvene.ui.main.SellerMenuActivity
import kotlinx.coroutines.launch

class NfcLoginActivity : AppCompatActivity() {

    private val api by lazy { ApiClient.create(this) }
    private val repo by lazy { Repository(api) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_login)
        handleNfcIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent?.action != NfcAdapter.ACTION_TAG_DISCOVERED) return

        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        val uid = tag?.id?.toHexString() ?: run {
            Toast.makeText(this, "No se pudo leer la tarjeta", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Hacemos login con el UID de la tarjeta
                val login = repo.loginWithCard(LoginRequest(cardId = uid))
                // Guardar token en SharedPreferences (ya no usamos saveToken)
                val token = login.token
                getSharedPreferences("app", MODE_PRIVATE)
                    .edit()
                    .putString("jwt", token)
                    .apply()

                // Sacar rol del JWT y navegar
                val role = JwtUtils.getRole(token).orEmpty()
                when (role.lowercase()) {
                    "admin" -> {
                        startActivity(Intent(this@NfcLoginActivity, AdminMenuActivity::class.java))
                        finish()
                    }
                    "seller", "vendedor" -> {
                        startActivity(Intent(this@NfcLoginActivity, SellerMenuActivity::class.java))
                        finish()
                    }
                    else -> {
                        Toast.makeText(this@NfcLoginActivity, "Rol desconocido: $role", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@NfcLoginActivity, "Error login: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- utils ---

    private fun ByteArray.toHexString(): String {
        val hexChars = CharArray(size * 2)
        val hexArray = "0123456789ABCDEF".toCharArray()
        for (j in indices) {
            val v = this[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
