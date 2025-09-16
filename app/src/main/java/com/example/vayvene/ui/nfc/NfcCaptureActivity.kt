package com.example.vayvene.ui.nfc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.ui.common.EXTRA_PROMPT
import com.example.vayvene.ui.common.EXTRA_UID

class NfcCaptureActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var handled = false

    private lateinit var tvHint: TextView
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_prompt)

        tvHint = findViewById(R.id.tvHint)
        tvStatus = findViewById(R.id.tvStatus)

        val hint = intent.getStringExtra(EXTRA_PROMPT)
            ?: getString(R.string.nfc_ready)
        tvHint.text = hint
        tvStatus.text = getString(R.string.nfc_ready)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, getString(R.string.nfc_not_supported), Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // PendingIntent para foreground dispatch
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            if (Build.VERSION.SDK_INT >= 31)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onResume() {
        super.onResume()
        // Si el NFC está apagado, sugerimos activarlo
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) {
            Toast.makeText(this, getString(R.string.nfc_not_supported), Toast.LENGTH_LONG).show()
            try {
                startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            } catch (_: Exception) { /* no-op */ }
        }
        adapter.enableForegroundDispatch(this, pendingIntent, null, null)
        handled = false
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (handled) return
        handled = true

        val tag: Tag? = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG) as? Tag
        }

        if (tag == null) {
            tvStatus.text = getString(R.string.nfc_read_fail)
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val uid = tag.id?.toHexStringUpper() ?: ""
        if (uid.isBlank()) {
            tvStatus.text = getString(R.string.nfc_read_fail)
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        tvStatus.text = getString(R.string.nfc_read_ok)

        // Devolvemos el UID (formato HEX mayúsculas, sin espacios)
        val data = Intent().putExtra(EXTRA_UID, uid)
        setResult(RESULT_OK, data)
        finish()
    }

    // ========= Helpers =========

    private fun ByteArray.toHexStringUpper(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }

    companion object {
        /**
         * Método de fábrica para lanzar el prompt NFC desde otras pantallas.
         */
        fun newIntent(context: Context, prompt: String? = null): Intent {
            return Intent(context, NfcCaptureActivity::class.java)
                .putExtra(EXTRA_PROMPT, prompt)
        }
    }
}
