package com.example.vayvene.ui.seller

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.BuildConfig
import com.example.vayvene.R
import com.example.vayvene.data.Session
import com.example.vayvene.ui.seller.SellerMenuActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class SellerSaleResultActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TOTAL = "totalCharged"
        const val EXTRA_UID = "customerUid"
    }

    private val http by lazy { OkHttpClient() }
    private lateinit var tvTotal: TextView
    private lateinit var tvBalance: TextView
    private lateinit var btnNew: Button
    private lateinit var btnMenu: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_sale_result)

        tvTotal = findViewById(R.id.tvTotal)
        tvBalance = findViewById(R.id.tvBalance)
        btnNew = findViewById(R.id.btnNewSale)
        btnMenu = findViewById(R.id.btnBackMenu)

        val total = intent.getDoubleExtra(EXTRA_TOTAL, 0.0)
        val uid = intent.getStringExtra(EXTRA_UID).orEmpty()

        tvTotal.text = "Total cobrado: ${formatMoney(total)}"

        if (uid.isNotBlank()) fetchBalance(uid) else tvBalance.text = "Balance: (UID no disponible)"

        btnNew.setOnClickListener {
            // Volvemos al POS para una nueva venta
            finish()
        }
        btnMenu.setOnClickListener {
            startActivity(Intent(this, SellerMenuActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            finish()
        }
    }

    private fun formatMoney(v: Double): String =
        NumberFormat.getCurrencyInstance(Locale("es", "AR")).format(v)

    private fun fetchBalance(uid: String) {
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val url = "$base/api/mobile/balance"
        val token = Session.jwt(this)
        if (token.isNullOrBlank()) {
            tvBalance.text = "Balance: sesión expirada"
            return
        }
        val body = JSONObject().apply { put("cardUid", uid) }
            .toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val req = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer $token")
            .build()

        Thread {
            try {
                http.newCall(req).execute().use { res ->
                    val txt = res.body?.string().orEmpty()
                    runOnUiThread {
                        if (!res.isSuccessful) {
                            tvBalance.text = "Balance (${res.code}): $txt"
                            return@runOnUiThread
                        }
                        val o = JSONObject(txt)
                        // tolero distintos nombres que pueda devolver tu API
                        val balance = when {
                            o.has("balance") -> o.optDouble("balance", 0.0)
                            o.has("saldo") -> o.optDouble("saldo", 0.0)
                            else -> 0.0
                        }
                        tvBalance.text = "Saldo restante: ${formatMoney(balance)}"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvBalance.text = "Balance red: ${e.message}"
                }
            }
        }.start()
    }
}
