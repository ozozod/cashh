package com.example.vayvene.ui.login

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vayvene.R
import com.example.vayvene.data.Repository
import com.example.vayvene.ui.admin.AdminMenuActivity
import com.example.vayvene.ui.main.CashierMenuActivity
import com.example.vayvene.ui.main.SellerMenuActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class NfcLoginActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var tvStatus: TextView
    private lateinit var repository: Repository

    // Anti-rebote
    @Volatile private var isHandlingTap = false
    private var lastUid: String? = null
    private var lastTs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_login)
        tvStatus = findViewById(R.id.tvStatus)

        repository = Repository(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            tvStatus.text = "Este dispositivo no tiene NFC"
            Toast.makeText(this, "NFC no disponible", Toast.LENGTH_LONG).show()
            return
        }
        if (nfcAdapter?.isEnabled != true) {
            tvStatus.text = "Activá el NFC en Ajustes"
        } else {
            tvStatus.text = "Acercá la tarjeta para iniciar sesión"
        }

        intent?.let { maybeHandleIntent(it) }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { maybeHandleIntent(it) }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
        if (nfcAdapter?.isEnabled == true) {
            tvStatus.text = "Acercá la tarjeta para iniciar sesión"
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag) {
        val uid = tag.id.toHex()
        val now = SystemClock.elapsedRealtime()
        if (isHandlingTap || (uid == lastUid && now - lastTs < 1500)) return
        isHandlingTap = true; lastUid = uid; lastTs = now

        runOnUiThread { tvStatus.text = "Leyendo...\nUID: $uid" }

        lifecycleScope.launch {
            try {
                val result = withContext(kotlinx.coroutines.Dispatchers.IO) { repository.loginWithUid(uid) }
                val next = when (result.role) {
                    "admin", "encargado" -> Intent(this@NfcLoginActivity, com.example.vayvene.ui.admin.AdminMenuActivity::class.java)
                    "cajero" -> Intent(this@NfcLoginActivity, com.example.vayvene.ui.main.CashierMenuActivity::class.java)
                    else -> Intent(this@NfcLoginActivity, com.example.vayvene.ui.main.SellerMenuActivity::class.java)
                }
                startActivity(next); finish()

                next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(next)
                finish()

            } catch (e: Exception) {
                runOnUiThread {
                    tvStatus.text = "Error de login: ${e.message}"
                    Toast.makeText(this@NfcLoginActivity, "Login fallido: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isHandlingTap = false
            }
        }
    }

    private fun maybeHandleIntent(intent: Intent) {
        val action = intent.action ?: return
        if (action != NfcAdapter.ACTION_TAG_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_NDEF_DISCOVERED) return

        val tag: Tag? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        tag?.let { onTagDiscovered(it) }
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder()
        for (b in this) sb.append(String.format("%02X", b))
        return sb.toString()
    }
}
