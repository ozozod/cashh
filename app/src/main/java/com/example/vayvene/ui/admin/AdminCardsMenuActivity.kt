package com.example.vayvene.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R

class AdminCardsMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_cards_menu)

        findViewById<Button>(R.id.btnRegisterStaff).setOnClickListener {
            startActivity(Intent(this, AdminStaffRegisterActivity::class.java))
        }

        findViewById<Button>(R.id.btnRegisterCustomer).setOnClickListener {
            startActivity(Intent(this, AdminCustomerRegisterActivity::class.java))
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}
