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

        // Botón: registrar STAFF
        findViewById<Button>(R.id.btnRegistrarStaff).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    com.example.vayvene.ui.admin.RegisterCardActivity::class.java
                ).apply {
                    // el RegisterCardActivity usa este extra para saber el rol
                    putExtra("role", "STAFF")
                }
            )
        }

        // Botón: registrar COMPRADOR
        findViewById<Button>(R.id.btnRegistrarComprador).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    com.example.vayvene.ui.admin.RegisterCardActivity::class.java
                ).apply {
                    putExtra("role", "BUYER")
                }
            )
        }
    }
}
