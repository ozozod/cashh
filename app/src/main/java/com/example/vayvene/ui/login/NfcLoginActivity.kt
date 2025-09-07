package com.example.vayvene.ui.login

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vayvene.data.*
import com.example.vayvene.ui.main.MainActivity
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.vayvene.data.TokenHolder

class NfcLoginActivity : AppCompatActivity() {

    private val api by lazy { ApiClient.api }
    private val repo by lazy { Repository(api) }
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        intent?.let { handleIntent(it) }
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
        if (intent != null) handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag == null) return

        val uidBytes = tag.id ?: return
        val normal = uidBytes.joinToString("") { String.format(Locale.US, "%02X", it) }
        val reversed = uidBytes.reversedArray().joinToString("") { String.format(Locale.US, "%02X", it) }

        lifecycleScope.launch {
            tryLogin(normal) || tryLogin(reversed)
        }
    }

    private suspend fun tryLogin(uid: String): Boolean {
        return try {
            val r = repo.login(uid)
            val token = r.getOrNull()?.token
            TokenHolder.token = token
            if (token != null) {
                TokenHolder.token = token
                val me = repo.me().getOrNull()?.user
                Toast.makeText(this, "Login OK (${me?.role ?: "?"})", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            } else false
        } catch (_: Exception) {
            Toast.makeText(this, "Login falló para UID $uid", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
