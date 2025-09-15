package com.example.vayvene.data

import android.content.Context
import android.content.SharedPreferences

object Session {
    private const val PREFS = "session_prefs"

    private const val K_JWT = "jwt"
    private const val K_EVENT = "eventId"
    private const val K_ROLE = "role"
    private const val K_CARD = "staffCardUid"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setLogin(ctx: Context, jwt: String, eventId: String?, role: String?, staffCardUid: String?) {
        sp(ctx).edit()
            .putString(K_JWT, jwt)
            .putString(K_EVENT, eventId ?: "")
            .putString(K_ROLE, role ?: "")
            .putString(K_CARD, staffCardUid ?: "")
            .apply()
    }

    fun jwt(ctx: Context): String? = sp(ctx).getString(K_JWT, null)

    fun eventId(ctx: Context): String? = sp(ctx).getString(K_EVENT, null)

    fun role(ctx: Context): String? = sp(ctx).getString(K_ROLE, null)

    fun staffCardUid(ctx: Context): String? = sp(ctx).getString(K_CARD, null)

    fun isManagerOrAdmin(ctx: Context): Boolean {
        val r = role(ctx)?.uppercase() ?: ""
        return r == "ENCARGADO" || r == "MANAGER" || r == "ADMIN" || r == "ADMINISTRADOR"
    }

    fun clear(ctx: Context) {
        sp(ctx).edit().clear().apply()
    }
}
