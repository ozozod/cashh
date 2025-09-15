package com.example.vayvene.data

import android.content.Context
import android.content.SharedPreferences

object Session {
    private const val PREFS = "session"
    private const val K_TOKEN = "token"
    private const val K_EVENT_ID = "event_id"
    private const val K_ROLE = "role"
    private const val K_IS_STAFF = "is_staff"
    private const val K_STAFF_NAME = "staff_name"
    private const val K_STAFF_CARD_UID = "staff_card_uid"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // getters
    fun jwt(ctx: Context): String? = prefs(ctx).getString(K_TOKEN, null)
    fun eventId(ctx: Context): String? = prefs(ctx).getString(K_EVENT_ID, null)
    fun role(ctx: Context): String? = prefs(ctx).getString(K_ROLE, null)
    fun isStaff(ctx: Context): Boolean = prefs(ctx).getBoolean(K_IS_STAFF, false)
    fun staffName(ctx: Context): String? = prefs(ctx).getString(K_STAFF_NAME, null)
    fun staffCardUid(ctx: Context): String? = prefs(ctx).getString(K_STAFF_CARD_UID, null)
    fun isManagerOrAdmin(ctx: Context): Boolean {
        val r = role(ctx)?.lowercase()
        return r == "admin" || r == "manager" || r == "encargado"
    }

    // setters (compat)
    fun saveToken(ctx: Context, token: String) =
        prefs(ctx).edit().putString(K_TOKEN, token).apply()
    fun saveEventId(ctx: Context, eventId: String) =
        prefs(ctx).edit().putString(K_EVENT_ID, eventId).apply()
    fun saveStaffRole(ctx: Context, role: String) =
        prefs(ctx).edit().putString(K_ROLE, role).apply()
    fun saveIsStaff(ctx: Context, value: Boolean) =
        prefs(ctx).edit().putBoolean(K_IS_STAFF, value).apply()
    fun saveStaffName(ctx: Context, name: String) =
        prefs(ctx).edit().putString(K_STAFF_NAME, name).apply()
    fun saveStaffCardUid(ctx: Context, uid: String) =
        prefs(ctx).edit().putString(K_STAFF_CARD_UID, uid).apply()

    // setLogin
    fun setLogin(
        ctx: Context,
        token: String,
        eventId: String,
        role: String,
        isStaff: Boolean,
        staffName: String? = null,
        staffCardUid: String? = null
    ) {
        prefs(ctx).edit()
            .putString(K_TOKEN, token)
            .putString(K_EVENT_ID, eventId)
            .putString(K_ROLE, role)
            .putBoolean(K_IS_STAFF, isStaff)
            .apply()
        staffName?.let { saveStaffName(ctx, it) }
        staffCardUid?.let { saveStaffCardUid(ctx, it) }
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }
}
