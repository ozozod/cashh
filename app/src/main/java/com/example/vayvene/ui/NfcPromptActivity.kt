package com.example.vayvene.ui.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R

class NfcCaptureActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var tv: TextView
    @Volatile private var isHandlingTap = false
    private var lastUid: String? = null
    private var lastTs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_capture)
        tv = findViewById(R.id.tvPrompt)
        val prompt = intent.getStringExtra(EXTRA_PROMPT).orEmpty()
        tv.text = if (prompt.isNotBlank()) prompt else "Acercá la tarjeta"
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this, this,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null
        )
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

        val data = intent.apply { putExtra(EXTRA_UID, uid) }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(); for (b in this) sb.append(String.format("%02X", b)); return sb.toString()
    }

    companion object {
        const val EXTRA_UID = "extra_uid"
        const val EXTRA_PROMPT = "extra_prompt"
    }
}
