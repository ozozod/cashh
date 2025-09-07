package com.example.vayvene.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vayvene.R

class PosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pos)
        title = "Nueva venta"
    }
}
