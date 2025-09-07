package com.example.vayvene.ui.common

import android.app.Dialog
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.vayvene.R

class NfcOneShotDialog : DialogFragment(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var handled = false
    private val timeoutMs = 10_000L
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.dialog_nfc_oneshot, container, false)

        v.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            sendError("Cancelado por el usuario.")
            dismissAllowingStateLoss()
        }

        return v
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext())
        if (nfcAdapter == null) {
            sendError("Este dispositivo no tiene NFC.")
            dismissAllowingStateLoss()
            return
        }
        if (!(nfcAdapter?.isEnabled ?: false)) {
            sendError("El NFC está desactivado. Actívalo e intenta nuevamente.")
            dismissAllowingStateLoss()
            return
        }

        val flags = (NfcAdapter.FLAG_READER_NFC_A
                or NfcAdapter.FLAG_READER_NFC_B
                or NfcAdapter.FLAG_READER_NFC_F
                or NfcAdapter.FLAG_READER_NFC_V
                or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK)

        nfcAdapter?.enableReaderMode(
            requireActivity(),
            this,
            flags,
            Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 50) }
        )

        handler.postDelayed({
            if (!handled) {
                disable()
                sendError("Tiempo agotado. Acerca la tarjeta e inténtalo de nuevo.")
                dismissAllowingStateLoss()
            }
        }, timeoutMs)
    }

    override fun onPause() {
        super.onPause()
        disable()
    }

    override fun onTagDiscovered(tag: Tag?) {
        val id = tag?.id ?: return
        val uid = id.joinToString("") { b -> "%02X".format(b) }

        if (!handled) {
            handled = true
            handler.post {
                disable()
                parentFragmentManager.setFragmentResult(
                    RESULT_OK,
                    bundleOf(KEY_UID to uid)
                )
                dismissAllowingStateLoss()
            }
        }
    }

    private fun disable() {
        try { nfcAdapter?.disableReaderMode(requireActivity()) } catch (_: Exception) { }
    }

    private fun sendError(msg: String) {
        parentFragmentManager.setFragmentResult(
            RESULT_ERROR,
            bundleOf(KEY_ERROR to msg)
        )
    }

    companion object {
        const val TAG = "NfcOneShotDialog"
        const val RESULT_OK = "nfc_scan_result_ok"
        const val RESULT_ERROR = "nfc_scan_result_error"
        const val KEY_UID = "uid"
        const val KEY_ERROR = "message"

        fun show(fm: FragmentManager) {
            NfcOneShotDialog().show(fm, TAG)
        }
    }
}
