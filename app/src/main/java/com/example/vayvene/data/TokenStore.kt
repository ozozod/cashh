package com.example.vayvene.data

import android.content.Context
import android.content.SharedPreferences

object TokenStore {
    private const val PREFS = "auth_prefs"
    private const val KEY = "jwt_token"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getToken(ctx: Context): String? =
        prefs(ctx).getString(KEY, null)

    fun saveToken(ctx: Context, token: String) {
        prefs(ctx).edit().putString(KEY, token).apply()
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(KEY).apply()
    }
}
