package com.example.vayvene.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R

class CancelSaleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Reutilizamos un layout simple. Luego armamos la UI de historial y anulación.
        setContentView(R.layout.activity_main)
        title = "Anular venta (placeholder)"
    }
}
