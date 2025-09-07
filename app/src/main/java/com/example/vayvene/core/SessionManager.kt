package com.example.vayvene.core

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun saveLogin(staffId: String, role: String, eventId: String, name: String) {
        sp.edit()
            .putString("staffId", staffId)
            .putString("role", role)
            .putString("eventId", eventId)
            .putString("name", name)
            .apply()
    }

    fun clear() { sp.edit().clear().apply() }

    val staffId: String? get() = sp.getString("staffId", null)
    val role: String? get() = sp.getString("role", null)
    val eventId: String? get() = sp.getString("eventId", null)
    val name: String? get() = sp.getString("name", null)
}