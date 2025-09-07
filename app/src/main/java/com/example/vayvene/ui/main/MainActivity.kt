package com.example.vayvene.ui.main

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vayvene.data.ApiClient
import com.example.vayvene.data.Repository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val repo by lazy { Repository(ApiClient.api) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usá tu layout si lo tenés:
        // setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        // Enviar heartbeat cada 30s
        lifecycleScope.launch {
            while (isActive) {
                repo.heartbeat(deviceId(), null, null)
                delay(30_000)
            }
        }
    }

    private fun deviceId(): String {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val model = Build.MODEL ?: "Android"
        return "$model-$androidId"
    }
}
