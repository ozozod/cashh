package com.example.vayvene.ui.login

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vayvene.data.* // ApiClient, ApiService, LoginRequest, TokenStore, etc.
import com.example.vayvene.ui.admin.AdminMenuActivity
import com.example.vayvene.ui.main.SellerMenuActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class NfcLoginActivity : AppCompatActivity() {

    private lateinit var api: ApiService
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos el cliente de API (usa BASE_URL del BuildConfig)
        api = ApiClient.create(ApiService::class.java)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Si abriste por PendingIntent de NFC, manejarlo:
        intent?.let { handleNfcIntent(it) }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleNfcIntent(it) }
    }

    private fun handleNfcIntent(intent: Intent) {
        if (intent.action != NfcAdapter.ACTION_TAG_DISCOVERED &&
            intent.action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            intent.action != NfcAdapter.ACTION_NDEF_DISCOVERED
        ) return

        val tag: Tag? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        val uid = tag?.id?.toHexString()?.uppercase(Locale.US)
        if (uid.isNullOrBlank()) {
            Toast.makeText(this, "No se pudo leer la tarjeta", Toast.LENGTH_SHORT).show()
            return
        }

        // Llamamos al backend: POST /auth/nfc-login { uid } (ajustá según tu ApiService)
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    api.login(LoginRequest(cardUid = uid))
                }

                if (!resp.isSuccessful) {
                    Toast.makeText(
                        this@NfcLoginActivity,
                        "Login HTTP ${resp.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val body = resp.body()
                val token = body?.token
                if (token.isNullOrBlank()) {
                    Toast.makeText(
                        this@NfcLoginActivity,
                        "Respuesta sin token",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Guardar token persistente y en el interceptor de ApiClient
                TokenStore.saveToken(this@NfcLoginActivity, token) // si ya lo tenés
                ApiClient.setToken(token) // para que el interceptor agregue Authorization

                // Consultar /me para decidir a dónde ir (ajustá firma según tu ApiService)
                val me = withContext(Dispatchers.IO) {
                    api.me("Bearer $token")
                }
                val role = me.body()?.user?.role?.trim()?.uppercase(Locale.US) ?: ""

                // Ruteo por rol: Admin/Encargado => AdminMenu; resto => SellerMenu
                val next = if (role == "ADMINISTRADOR" || role == "ENCARGADO") {
                    Intent(this@NfcLoginActivity, AdminMenuActivity::class.java)
                } else {
                    Intent(this@NfcLoginActivity, SellerMenuActivity::class.java)
                }
                next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(next)
                setResult(Activity.RESULT_OK)
                finish()

            } catch (e: Exception) {
                Toast.makeText(this@NfcLoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ByteArray.toHexString(): String {
        val result = StringBuilder()
        for (b in this) {
            result.append(String.format("%02X", b))
        }
        return result.toString()
    }
}
