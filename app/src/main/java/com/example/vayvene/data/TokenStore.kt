package com.example.vayvene.data

import android.content.Context

object TokenStore {
    private const val PREF = "auth"
    private const val KEY = "token"

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, token).apply()
    }

    fun getToken(context: Context): String? =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, null)

    fun clearToken(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(KEY).apply()
    }
}
