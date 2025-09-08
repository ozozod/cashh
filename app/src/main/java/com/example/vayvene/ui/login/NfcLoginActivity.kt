package com.example.vayvene.ui.login

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.data.ApiClient
import com.example.vayvene.data.Repository
import com.example.vayvene.ui.main.RoleMenuActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NfcLoginActivity : AppCompatActivity() {

    private val api by lazy { ApiClient.create(this) }
    private val repo by lazy { Repository(api, this) }

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            if (Build.VERSION.SDK_INT >= 31)
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Si la activity ya fue lanzada por NFC:
        intent?.let { handleNfcIntent(it) }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        if (intent.action != NfcAdapter.ACTION_TAG_DISCOVERED &&
            intent.action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            intent.action != NfcAdapter.ACTION_TECH_DISCOVERED
        ) return

        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        val idBytes = tag?.id ?: return

        val cardUid = bytesToHex(idBytes) // UID seguro (sin índices locos)

        CoroutineScope(Dispatchers.Main).launch {
            val result = repo.loginWithCard(cardUid)
            result.onSuccess { login ->
                Toast.makeText(this@NfcLoginActivity, "Login OK", Toast.LENGTH_SHORT).show()
                // Después del login, vamos al menú de roles (ahí decidís Admin/Vendedor)
                startActivity(Intent(this@NfcLoginActivity, RoleMenuActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
            }.onFailure { err ->
                Toast.makeText(
                    this@NfcLoginActivity,
                    "Error login: ${err.message ?: "desconocido"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** Conversión segura de ByteArray (UID de la tarjeta) a HEX mayúsculas */
    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { b -> "%02X".format(b) }
}
