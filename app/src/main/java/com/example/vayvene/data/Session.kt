package com.example.vayvene.data

import android.content.Context

object Session {
    private const val PREFS = "session"
    private const val K_JWT = "jwt"
    private const val K_ROLE = "role"
    private const val K_USER_ID = "userId"
    private const val K_EVENT_ID = "eventId"
    private const val K_USER_NAME = "userName"

    fun save(
        ctx: Context,
        jwt: String,
        role: String?,
        userId: String?,
        eventId: String?,
        userName: String?
    ) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(K_JWT, jwt)
            .putString(K_ROLE, role)
            .putString(K_USER_ID, userId)
            .putString(K_EVENT_ID, eventId)
            .putString(K_USER_NAME, userName)
            .apply()
    }

    fun jwt(ctx: Context): String? {
        // Clave principal
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(K_JWT, null)?.let { if (it.isNotBlank()) return it }

        // Fallbacks por si quedó de código anterior
        val legacyKeys = arrayOf("token", "auth_token", "bearer", "access_token")
        for (k in legacyKeys) {
            prefs.getString(k, null)?.let { if (it.isNotBlank()) return it }
        }
        return null
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun role(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(K_ROLE, null)
}
