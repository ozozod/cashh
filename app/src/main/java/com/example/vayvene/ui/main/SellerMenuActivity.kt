package com.example.vayvene.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.ui.login.NfcLoginActivity
import org.json.JSONObject
import java.util.Locale

class SellerMenuActivity : AppCompatActivity() {

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_menu)

        val token = getTokenFromPrefs(this)
        val role = token?.let { getClaim(it, "role") }?.uppercase(Locale.ROOT)
        val isSeller = role == "SELLER" || role == "VENDEDOR" || role == "VENDOR"

        if (token.isNullOrBlank() || !isSeller) {
            Toast.makeText(this, "Sesión inválida. Iniciá de nuevo.", Toast.LENGTH_SHORT).show()
            val i = Intent(this, NfcLoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
            return
        }

        val btn1: Button? = findViewById(R.id.btnAction1)   // Nueva venta
        val btn2: Button? = findViewById(R.id.btnAction2)   // Consultar saldo
        val btn3: Button? = findViewById(R.id.btnAction3)   // Resumen
        val btn4: Button? = findViewById(R.id.btnAction4)   // Anular venta
        val btn5: Button? = findViewById(R.id.btnAction5)   // Cerrar sesión

        btn1?.setOnClickListener { toast("Nueva venta (pendiente de UI)") }
        btn2?.setOnClickListener { toast("Consultar saldo (pendiente de UI)") }
        btn3?.setOnClickListener { startActivity(Intent(this, SellerSummaryActivity::class.java)) }
        btn4?.setOnClickListener { toast("Anular venta (pendiente de UI)") }
        btn5?.setOnClickListener {
            clearTokenInPrefs(this)
            val i = Intent(this, NfcLoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }

        if (listOf(btn1, btn2, btn3, btn4, btn5).all { it == null }) {
            toast("Faltan los 5 botones en el layout del vendedor")
        }
    }

    // -------- helpers --------
    private fun getTokenFromPrefs(ctx: Context): String? {
        val prefNames = arrayOf("auth", "session", "app_prefs")
        val keys = arrayOf("token", "jwt", "Authorization")
        for (p in prefNames) {
            val prefs = ctx.getSharedPreferences(p, Context.MODE_PRIVATE)
            for (k in keys) {
                prefs.getString(k, null)?.let { if (it.isNotBlank()) return it }
            }
        }
        return null
    }

    private fun clearTokenInPrefs(ctx: Context) {
        val prefNames = arrayOf("auth", "session", "app_prefs")
        val keys = arrayOf("token", "jwt", "Authorization")
        for (p in prefNames) {
            val prefs = ctx.getSharedPreferences(p, Context.MODE_PRIVATE)
            val e = prefs.edit()
            for (k in keys) e.remove(k)
            e.apply()
        }
    }

    /** Devuelve el claim `key` del JWT o, si viene anidado, lo busca en `user[key]`. */
    private fun getClaim(jwt: String, key: String): String? {
        return try {
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val payloadB64 = parts[1].replace('-', '+').replace('_', '/')
            val pad = (4 - payloadB64.length % 4) % 4
            val padded = payloadB64 + "=".repeat(pad)
            val json = JSONObject(String(Base64.decode(padded, Base64.DEFAULT)))

            // optString no acepta null como default → usamos "" y validamos
            val direct = json.optString(key, "")
            if (direct.isNotBlank()) return direct

            val nested = json.optJSONObject("user")?.optString(key, "")
            nested?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

}
