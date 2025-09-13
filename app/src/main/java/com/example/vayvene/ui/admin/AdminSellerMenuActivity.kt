package com.example.vayvene.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R
import com.example.vayvene.ui.main.BalanceActivity
import com.example.vayvene.ui.main.CancelSaleActivity
import com.example.vayvene.ui.main.PosActivity

class AdminSellerMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_seller_menu)

        // Ventas (POS)
        findViewById<Button>(R.id.btnPos).setOnClickListener {
            startActivity(Intent(this, PosActivity::class.java))
        }
        // Consultar saldo
        findViewById<Button>(R.id.btnCheckBalance).setOnClickListener {
            startActivity(Intent(this, BalanceActivity::class.java))
        }
        // Anular última venta
        findViewById<Button>(R.id.btnCancelSale).setOnClickListener {
            startActivity(Intent(this, CancelSaleActivity::class.java))
        }
        // Volver
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }
}
