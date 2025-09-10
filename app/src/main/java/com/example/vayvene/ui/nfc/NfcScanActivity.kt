package com.example.vayvene.ui.nfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R

class NfcScanActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_scan)

        findViewById<TextView>(R.id.tvTitle).text =
            intent.getStringExtra(EXTRA_TITLE) ?: "Acercá la tarjeta"

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            toast("Este dispositivo no tiene NFC")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        if (!nfcAdapter!!.isEnabled) {
            toast("Activá el NFC")
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }

        // Fallback (algunos OEM requieren esto)
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        )
    }

    override fun onResume() {
        super.onResume()
        // Activa lectura SOLO mientras esta pantalla está visible
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NFC_BARCODE or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
        try { nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null) } catch (_: Throwable) {}
    }

    override fun onPause() {
        super.onPause()
        // Se desactiva al salir → no queda escuchando siempre
        try { nfcAdapter?.disableReaderMode(this) } catch (_: Throwable) {}
        try { nfcAdapter?.disableForegroundDispatch(this) } catch (_: Throwable) {}
    }

    // Reader mode
    override fun onTagDiscovered(tag: Tag?) {
        val uid = tag?.id?.toHexUpper() ?: return
        returnUid(uid)
    }

    // Fallback por intent (por si algún equipo lo usa)
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val tag: Tag? = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION") intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        val uid = tag?.id?.toHexUpper() ?: return
        returnUid(uid)
    }

    private fun returnUid(uid: String) {
        runOnUiThread { toast("Tarjeta leída") }
        val data = Intent().putExtra(RESULT_UID, uid)
        setResult(RESULT_OK, data)
        finish() // se cierra y listo: NFC apagado
    }

    private fun ByteArray.toHexUpper(): String =
        joinToString("") { b -> "%02X".format(b) }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val RESULT_UID = "uid"
        const val EXTRA_TITLE = "title"
    }
}
