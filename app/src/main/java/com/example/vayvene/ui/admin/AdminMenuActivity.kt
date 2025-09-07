package com.example.vayvene.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R

class AdminMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu)

        val btnStaff = findViewById<Button>(R.id.btnRegistrarStaff)
        val btnBuyer = findViewById<Button>(R.id.btnRegistrarComprador)

        btnStaff.setOnClickListener {
            startActivity(
                Intent(this, RegisterCardActivity::class.java)
                    .putExtra(RegisterCardActivity.EXTRA_MODE, RegisterCardActivity.MODE_STAFF)
            )
        }

        btnBuyer.setOnClickListener {
            startActivity(
                Intent(this, RegisterCardActivity::class.java)
                    .putExtra(RegisterCardActivity.EXTRA_MODE, RegisterCardActivity.MODE_BUYER)
            )
        }
    }
}
