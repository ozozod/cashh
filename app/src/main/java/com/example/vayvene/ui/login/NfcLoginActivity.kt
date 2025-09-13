package com.example.vayvene.ui.login

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.BuildConfig
import com.example.vayvene.R
import com.example.vayvene.data.Session
import com.example.vayvene.ui.admin.AdminMenuActivity
import com.example.vayvene.ui.main.SellerMenuActivity
import com.example.vayvene.ui.main.CashierMenuActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NfcLoginActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private val http by lazy { OkHttpClient() }
    private lateinit var tvStatus: TextView
    private var nfc: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_login)

        tvStatus = findViewById(R.id.tvStatus)
        nfc = NfcAdapter.getDefaultAdapter(this)

        tvStatus.text = "Login por NFC\nAcercá una tarjeta…"
    }

    override fun onResume() {
        super.onResume()
        nfc?.enableReaderMode(
            this, this,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null
        )
    }

    override fun onPause() {
        super.onPause()
        nfc?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag) {
        val uid = tag.id.joinToString("") { b -> "%02X".format(b) }
        runOnUiThread { tvStatus.text = "UID: $uid\nLogueando…" }
        doLogin(uid)
    }

    private fun doLogin(cardUid: String) {
        val url = BuildConfig.BASE_URL.trimEnd('/') + "/api/mobile/login"
        val json = JSONObject().put("cardUid", cardUid)
        val req = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        Thread {
            try {
                http.newCall(req).execute().use { res ->
                    val body = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this, "Error de login: $body", Toast.LENGTH_LONG).show()
                            tvStatus.text = "Login por NFC\nVolvé a intentar."
                        }
                        return@use
                    }

                    val obj = JSONObject(body)
                    val token = obj.optString("token")
                    val user = obj.optJSONObject("user")
                    val role = user?.optString("role")
                    val userId = user?.opt("id")?.toString()
                    val userName = user?.optString("name")
                    // eventId puede venir en la sección "user.eventId" o "event.id"
                    val eventId = user?.opt("eventId")?.toString()
                        ?: obj.optJSONObject("event")?.opt("id")?.toString()

                    // Guarda TODO SIEMPRE con las MISMAS CLAVES
                    Session.save(
                        ctx = this,
                        jwt = token,
                        role = role,
                        userId = userId,
                        eventId = eventId,
                        userName = userName
                    )

                    runOnUiThread {
                        Toast.makeText(this, "Login OK (${role ?: "?"})", Toast.LENGTH_SHORT).show()
                        goToMenuByRole(role)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Red: ${e.message}", Toast.LENGTH_LONG).show()
                    tvStatus.text = "Login por NFC\nError de red."
                }
            }
        }.start()
    }

    private fun goToMenuByRole(role: String?) {
        val r = (role ?: "").uppercase()
        val intent = when {
            r == "ADMINISTRADOR" -> Intent(this, AdminMenuActivity::class.java)
            r == "CAJERO" -> Intent(this, CashierMenuActivity::class.java)
            else -> Intent(this, SellerMenuActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
