package com.example.vayvene.ui.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.ui.common.EXTRA_UID

class NfcCaptureActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var tvHint: TextView
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_prompt)

        tvHint = findViewById(R.id.tvHint)
        tvStatus = findViewById(R.id.tvStatus)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            tvStatus.text = getString(R.string.nfc_not_supported)
        } else {
            tvStatus.text = getString(R.string.nfc_ready)
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            { tag ->
                val uid = tag.id?.joinToString("") { b -> "%02X".format(b) } ?: return@enableReaderMode
                runOnUiThread { tvStatus.text = getString(R.string.nfc_read_ok) }
                val data = Intent().putExtra(EXTRA_UID, uid)
                setResult(Activity.RESULT_OK, data)
                finish()
            },
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }
}
