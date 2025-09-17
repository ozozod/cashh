package com.example.vayvene.utils

import android.nfc.NfcAdapter
import android.nfc.Tag

object NfcUidParser {
    fun getUid(tag: Tag): String? = tag.id?.joinToString("") { "%02X".format(it) }
}

class ReaderCallbackImpl(
    private val onUid: (String) -> Unit,
    private val onErr: (Throwable) -> Unit
) : NfcAdapter.ReaderCallback {
    override fun onTagDiscovered(tag: Tag) {
        try {
            NfcUidParser.getUid(tag)?.let(onUid) ?: onErr(IllegalStateException("UID vacío"))
        } catch (t: Throwable) {
            onErr(t)
        }
    }
}
