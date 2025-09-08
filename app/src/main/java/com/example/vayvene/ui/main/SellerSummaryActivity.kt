package com.example.vayvene.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R

/**
 * Resumen del vendedor. Por ahora solo deja la pantalla en pie
 * para que compile y puedas entrar. Luego armamos el recycler con datos reales.
 */
class SellerSummaryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usamos un layout existente para no romper. Luego creamos el propio.
        setContentView(R.layout.activity_main)
    }
}
