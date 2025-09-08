package com.example.vayvene.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R

class CancelSaleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // usá un layout simple o uno existente
        setContentView(R.layout.activity_nfc_prompt) // placeholder
        // TODO: pedir tarjeta ENCARGADO, listar ventas del vendedor, elegir una, validar tarjeta del comprador y devolver saldo.
    }
}
