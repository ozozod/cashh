package com.example.vayvene.ui.login

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vayvene.R
import com.example.vayvene.data.Repository
import com.example.vayvene.data.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NfcLoginActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_login)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "Tu dispositivo no tiene NFC.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag == null) {
            Toast.makeText(this, "Acercá una tarjeta para iniciar sesión.", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = tag.id?.toHexString() ?: run {
            Toast.makeText(this, "No pude leer el UID.", Toast.LENGTH_SHORT).show()
            return
        }
        doLogin(uid)
    }

    private fun doLogin(cardUid: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val repo = Repository(this@NfcLoginActivity)
            try {
                // ⬇️ Usa el método que tengas disponible en Repository (ver sección 2)
                val resp = repo.mobileLogin(cardUid)

                val token = resp.token ?: ""
                val eventId = resp.eventId ?: ""
                val role = resp.staffRole ?: ""        // "ADMINISTRADOR" | "ENCARGADO" | "VENDEDOR" | ""
                val isStaff = resp.isStaff == true

                // Guardar sesión centralizada
                Session.saveToken(this@NfcLoginActivity, token)
                Session.saveEventId(this@NfcLoginActivity, eventId)
                Session.saveStaffRole(this@NfcLoginActivity, role)
                Session.saveIsStaff(this@NfcLoginActivity, isStaff)

                withContext(Dispatchers.Main) {
                    when {
                        isStaff && (role.equals("ADMINISTRADOR", true) || role.equals("ENCARGADO", true)) -> {
                            goToAdminMenu()
                        }
                        isStaff && role.equals("VENDEDOR", true) -> {
                            goToSellerMenu()
                        }
                        else -> {
                            Toast.makeText(
                                this@NfcLoginActivity,
                                "La tarjeta no tiene rol de STAFF en este evento.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@NfcLoginActivity,
                        "Error de login: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Navegación robusta por reflection: evita imports que fallan si moviste paquetes.
    private fun goToAdminMenu() {
        val tried = arrayOf(
            "com.example.vayvene.ui.admin.AdminMenuActivity",
            "com.example.vayvene.ui.main.AdminMenuActivity"
        )
        startByClassNames(tried)
    }

    private fun goToSellerMenu() {
        val tried = arrayOf(
            "com.example.vayvene.ui.seller.SellerMenuActivity",
            "com.example.vayvene.ui.main.SellerMenuActivity"
        )
        startByClassNames(tried)
    }

    private fun startByClassNames(classNames: Array<String>) {
        for (name in classNames) {
            try {
                val clazz = Class.forName(name)
                startActivity(Intent(this, clazz))
                finish()
                return
            } catch (_: ClassNotFoundException) {
                // probar el siguiente
            }
        }
        Toast.makeText(this, "No encontré la pantalla destino.", Toast.LENGTH_LONG).show()
    }

    private fun ByteArray.toHexString(): String {
        val sb = StringBuilder()
        for (b in this) sb.append(String.format("%02X", b))
        return sb.toString()
    }
}
