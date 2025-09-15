package com.example.vayvene.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

object TelemetryUtil {

    fun batteryPercent(ctx: Context): Int {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val st: Intent? = ctx.registerReceiver(null, ifilter)
            val level = st?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = st?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100f / scale).toInt() else -1
        } catch (_: Exception) { -1 }
    }

    /** -1 (off/desconocido) o 0..4 */
    fun netQuality(ctx: Context): Int {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork ?: return -1
            val caps = cm.getNetworkCapabilities(net) ?: return -1

            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val rssi: Int = run {
                        @Suppress("DEPRECATION")
                        val info = wm.connectionInfo
                        info?.rssi ?: return -1
                    }
                    when {
                        rssi <= -100 -> 0
                        rssi >= -55 -> 4
                        else -> {
                            val level = ((rssi + 100) * 4) / 45
                            min(4, max(0, level))
                        }
                    }
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 3
                else -> -1
            }
        } catch (_: Exception) { -1 }
    }

    fun deviceName(): String =
        listOf(Build.MANUFACTURER, Build.MODEL).joinToString(" ").trim()

    fun snapshot(ctx: Context): JSONObject =
        JSONObject()
            .putSafe("batteryPct", batteryPercent(ctx))
            .putSafe("netQuality", netQuality(ctx))
            .putSafe("device", deviceName())
            .putSafe("appVersion", try { com.example.vayvene.BuildConfig.VERSION_NAME } catch (_: Throwable) { null })
            .putSafe("online", true)
}
