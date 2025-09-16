package com.example.vayvene.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import org.json.JSONObject

object TelemetryUtil {

    fun buildBasicTelemetry(ctx: Context): JSONObject {
        return JSONObject().apply {
            put("batteryPct", getBatteryPct(ctx))
            val (networkType, signal) = getNetworkInfo(ctx)
            put("networkType", networkType)
            put("signal", signal)
            put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("appVersion", getAppVersion(ctx))
            put("online", true)
        }
    }

    private fun getBatteryPct(ctx: Context): Int {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return pct.coerceIn(0, 100)
    }

    private fun getNetworkInfo(ctx: Context): Pair<String, Int> {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return "offline" to -1
        val caps = cm.getNetworkCapabilities(net) ?: return "offline" to -1
        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
            else -> "other"
        }
        // Señal: no siempre disponible. Dejamos -1 si no hay forma simple
        return type to -1
    }

    private fun getAppVersion(ctx: Context): String {
        return try {
            val p = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            if (Build.VERSION.SDK_INT >= 28)
                "${p.versionName} (${p.longVersionCode})"
            else
                "${p.versionName} (${p.versionCode})"
        } catch (_: Exception) {
            "unknown"
        }
    }
}
