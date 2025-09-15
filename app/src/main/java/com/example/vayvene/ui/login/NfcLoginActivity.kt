package com.example.vayvene.ui.login

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.BuildConfig
import com.example.vayvene.R
import com.example.vayvene.data.Session
import com.example.vayvene.ui.main.AdminMenuActivity
import com.example.vayvene.ui.main.CashierMenuActivity
import com.example.vayvene.ui.main.SellerMenuActivity
import com.example.vayvene.ui.nfc.NfcCaptureActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NfcLoginActivity : AppCompatActivity() {

    private val http by lazy { OkHttpClient() }
    // <-- ahora son nullables para evitar NPE si el layout no trae los IDs
    private var tvTitle: TextView? = null
    private var tvSub: TextView? = null

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        if (r.resultCode == RESULT_OK) {
            val uid = r.data?.getStringExtra(NfcCaptureActivity.EXTRA_UID)?.uppercase()
            if (uid.isNullOrBlank()) {
                toast("UID inválido")
                return@registerForActivityResult
            }
            tvSub?.text = "UID: $uid\nAhora llamá al backend para hacer login"
            doLogin(uid)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_login)

        // Si el layout no tiene estos IDs, no crashea:
        tvTitle = findViewById(R.id.tvTitle)
        tvSub   = findViewById(R.id.tvSub)

        // Permisos de red para RSSI
        permLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
        ))

        // Arranca a escanear inmediatamente
        val i = Intent(this, NfcCaptureActivity::class.java)
        i.putExtra(NfcCaptureActivity.EXTRA_PROMPT, "Acercá tu tarjeta para iniciar sesión")
        scanLauncher.launch(i)
    }

    private fun doLogin(cardUid: String) {
        val url = "${BuildConfig.BASE_URL.trimEnd('/')}/api/mobile/login"

        // Telemetría
        val battery = getBatteryPercent(this)
        val signal = getSignalDbm(this)
        val device = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        val payload = JSONObject().apply {
            put("cardUid", cardUid)
            put("battery", battery)     // %
            put("signal", signal)       // dBm aprox (negativo si hay)
            put("device", device)
        }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val req = Request.Builder()
            .url(url)
            .post(payload)
            .build()

        Thread {
            try {
                http.newCall(req).execute().use { res ->
                    val body = res.body?.string().orEmpty()
                    runOnUiThread {
                        if (!res.isSuccessful) {
                            tvSub?.text = "Login por NFC\nVolvé a intentar."
                            toast("Error de login: $body")
                            return@runOnUiThread
                        }
                        val obj = JSONObject(body)

                        val jwt = obj.optString("token", obj.optString("jwt"))
                        val eventId = obj.optString("eventId")
                        val role = obj.optString("role",
                            obj.optJSONObject("staff")?.optString("role") ?: "")
                        val staffCard = obj.optString("cardUid",
                            obj.optJSONObject("staff")?.optString("cardUid") ?: cardUid)

                        if (jwt.isBlank()) {
                            toast("Login inválido: sin token")
                            return@runOnUiThread
                        }

                        Session.setLogin(this, jwt, eventId, role, staffCard)

                        // Router por rol
                        when (role.uppercase()) {
                            "ADMIN", "ADMINISTRADOR" -> {
                                startActivity(Intent(this, AdminMenuActivity::class.java))
                            }
                            "CAJERO" -> {
                                startActivity(Intent(this, CashierMenuActivity::class.java))
                            }
                            "ENCARGADO", "MANAGER" -> {
                                startActivity(Intent(this, SellerMenuActivity::class.java))
                            }
                            "VENDEDOR" -> {
                                startActivity(Intent(this, SellerMenuActivity::class.java))
                            }
                            else -> {
                                // por defecto vendedor
                                startActivity(Intent(this, SellerMenuActivity::class.java))
                            }
                        }
                        finish()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { toast("Red: ${e.message}") }
            }
        }.start()
    }

    private fun getBatteryPercent(ctx: Context): Int {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else -1
    }

    private fun getSignalDbm(ctx: Context): Int {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork ?: return -1
            val caps = cm.getNetworkCapabilities(net) ?: return -1

            // Solo medimos si es Wi-Fi; para celular devolvemos -1 por simplicidad
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION") // compat
                val rssi = wm.connectionInfo?.rssi
                rssi ?: -1
            } else {
                -1
            }
        } catch (_: Exception) {
            -1
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
