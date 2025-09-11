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
import com.example.vayvene.R

class NfcLoginActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var tvStatus: TextView

    // Anti-rebote
    @Volatile private var isHandlingTap = false
    private var lastUid: String? = null
    private var lastTs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ⬇️ Ahora SÍ hay UI (antes, por eso veías pantalla gris)
        setContentView(R.layout.activity_nfc_login)
        tvStatus = findViewById(R.id.tvStatus)

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

        // Si te llegan intents por dispatch clásico, igual los manejamos
        intent?.let { maybeHandleIntent(it) }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { maybeHandleIntent(it) }
    }

    override fun onResume() {
        super.onResume()
        // Reader Mode one-shot (mientras esta pantalla está visible)
        nfcAdapter?.enableReaderMode(
            this,
            this, // ReaderCallback
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

    /** ReaderCallback: llega cuando tocás una tarjeta */
    override fun onTagDiscovered(tag: Tag) {
        val uid = tag.id.toHex()
        val now = SystemClock.elapsedRealtime()
        if (isHandlingTap || (uid == lastUid && now - lastTs < 1500)) return
        isHandlingTap = true; lastUid = uid; lastTs = now

        runOnUiThread {
            tvStatus.text = "Leyendo...\nUID: $uid"
        }

        // TODO: acá llamá a tu backend para login con UID.
        // Ejemplo (si ya tenés Repository/ApiService):
        // lifecycleScope.launch {
        //     try {
        //         val ok = withContext(Dispatchers.IO) { repository.loginWithUid(uid) }
        //         if (ok) { startActivity(Intent(...)); finish() }
        //         else { tvStatus.text = "Error de login"; }
        //     } finally { isHandlingTap = false }
        // }

        // Por ahora, solo demostramos lectura:
        runOnUiThread {
            Toast.makeText(this, "UID detectado: $uid", Toast.LENGTH_SHORT).show()
            tvStatus.text = "UID: $uid\nAhora llamá al backend para hacer login"
            isHandlingTap = false
        }
    }

    private fun maybeHandleIntent(intent: Intent) {
        // Soporte para ACTION_TAG_DISCOVERED si llegara por intent-filter
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
