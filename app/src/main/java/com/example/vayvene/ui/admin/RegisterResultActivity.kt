package com.example.vayvene.ui.admin

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R

class RegisterResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_result)

        val ok = intent.getBooleanExtra("ok", false)
        val mode = intent.getStringExtra("mode") ?: "buyer"
        val uid = intent.getStringExtra("uid") ?: ""
        val nombre = intent.getStringExtra("nombre")

        val iv = findViewById<ImageView>(R.id.ivIcon)
        val tv = findViewById<TextView>(R.id.tvResultado)
        val btn = findViewById<Button>(R.id.btnVolver)

        if (ok) {
            iv.setImageResource(R.drawable.ic_ok)
            val quien = if (mode == RegisterCardActivity.MODE_STAFF) {
                "STAFF" + (if (!nombre.isNullOrBlank()) " ($nombre)" else "")
            } else "COMPRADOR"
            tv.text = "Registro exitoso de $quien\nUID: $uid"
        } else {
            iv.setImageResource(R.drawable.ic_error)
            tv.text = "No se pudo registrar.\nUID: $uid"
        }

        btn.setOnClickListener { finish() }
    }
}
