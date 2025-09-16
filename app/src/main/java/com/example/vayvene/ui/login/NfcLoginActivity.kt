package com.example.vayvene.ui.login

import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.BuildConfig
import com.example.vayvene.data.Session
import com.example.vayvene.ui.admin.AdminCustomerRegisterActivity
import com.example.vayvene.ui.cashier.CashierQuickOpsActivity
import com.example.vayvene.ui.seller.SellerMenuActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NfcLoginActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var tvStatus: TextView
    private val client by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_login)

        tvStatus = findViewById(R.id.tvStatus)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            tvStatus.text = getString(R.string.nfc_not_supported)
            return
        }
        tvStatus.text = getString(R.string.nfc_ready)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            { tag ->
                val uid = tag.id?.joinToString("") { b -> "%02X".format(b) } ?: return@enableReaderMode
                runOnUiThread { tvStatus.text = getString(R.string.login_logging_in) }
                doLogin(uid)
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

    private fun doLogin(cardUid: String) {
        Thread {
            try {
                val url = BuildConfig.BASE_URL.trimEnd('/') + "/api/mobile/login"

                val json = JSONObject().put("cardUid", cardUid).toString()
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

                val req = Request.Builder().url(url).post(body).build()
                val resp = client.newCall(req).execute()
                val code = resp.code
                val bodyStr = resp.body?.string().orEmpty()

                if (code in 200..299) {
                    val obj = JSONObject(bodyStr)
                    val user = obj.getJSONObject("user")
                    val role = user.optString("role", "").uppercase()

                    Session.currentUserName = user.optString("name", "")
                    Session.currentUserRole = role

                    runOnUiThread {
                        tvStatus.text = getString(R.string.nfc_read_ok)
                        when (role) {
                            "VENDEDOR" -> startActivity(android.content.Intent(this, SellerMenuActivity::class.java))
                            "CAJERO" -> startActivity(android.content.Intent(this, CashierQuickOpsActivity::class.java))
                            "ADMINISTRADOR" -> startActivity(android.content.Intent(this, AdminCustomerRegisterActivity::class.java))
                            else -> tvStatus.text = getString(R.string.nfc_read_fail)
                        }
                    }
                } else {
                    runOnUiThread { tvStatus.text = getString(R.string.nfc_read_fail) }
                }
            } catch (_: Exception) {
                runOnUiThread { tvStatus.text = getString(R.string.nfc_read_fail) }
            }
        }.start()
    }
}
