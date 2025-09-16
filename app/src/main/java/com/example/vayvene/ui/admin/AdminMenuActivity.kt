package com.example.vayvene.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.ui.cashier.CashierMenuActivity
import com.example.vayvene.ui.login.NfcLoginActivity
import com.example.vayvene.ui.seller.SellerMenuActivity

class AdminMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu)

        findViewById<Button>(R.id.btnCards).setOnClickListener {
            startActivity(Intent(this, AdminCardsMenuActivity::class.java))
        }

        findViewById<Button>(R.id.btnCashier).setOnClickListener {
            startActivity(Intent(this, CashierMenuActivity::class.java))
        }

        findViewById<Button>(R.id.btnSeller).setOnClickListener {
            startActivity(Intent(this, SellerMenuActivity::class.java))
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            // (Opcional) POST /api/mobile/logout con online=false
            getSharedPreferences("session", MODE_PRIVATE).edit().clear().apply()
            val i = Intent(this, NfcLoginActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
            finish()
        }
    }
}
