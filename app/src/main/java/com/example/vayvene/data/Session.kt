package com.example.vayvene.data

import android.content.Context
import android.content.SharedPreferences

object Session {
    private const val PREFS = "session"
    private const val K_JWT = "jwt"
    private const val K_EVENT = "eventId"
    private const val K_ROLE = "staffRole"
    private const val K_NAME = "staffName"
    private const val K_STAFF_UID = "staffCardUid"
    private const val K_IS_STAFF = "isStaff"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(
        ctx: Context,
        token: String? = null,
        eventId: String? = null,
        staffRole: String? = null,
        staffName: String? = null,
        staffCardUid: String? = null,
        isStaff: Boolean? = null
    ) {
        prefs(ctx).edit().apply {
            if (token != null) putString(K_JWT, token) else remove(K_JWT)
            if (eventId != null) putString(K_EVENT, eventId) else remove(K_EVENT)
            if (staffRole != null) putString(K_ROLE, staffRole) else remove(K_ROLE)
            if (staffName != null) putString(K_NAME, staffName) else remove(K_NAME)
            if (staffCardUid != null) putString(K_STAFF_UID, staffCardUid) else remove(K_STAFF_UID)
            if (isStaff != null) putBoolean(K_IS_STAFF, isStaff) else remove(K_IS_STAFF)
        }.apply()
    }

    fun clear(ctx: Context) { prefs(ctx).edit().clear().apply() }

    fun token(ctx: Context): String? = prefs(ctx).getString(K_JWT, null)
    fun jwt(ctx: Context): String? = token(ctx) // alias
    fun eventId(ctx: Context): String? = prefs(ctx).getString(K_EVENT, null)
    fun staffRole(ctx: Context): String? = prefs(ctx).getString(K_ROLE, null)
    fun staffName(ctx: Context): String? = prefs(ctx).getString(K_NAME, null)
    fun staffCardUid(ctx: Context): String? = prefs(ctx).getString(K_STAFF_UID, null)
    fun isStaff(ctx: Context): Boolean = prefs(ctx).getBoolean(K_IS_STAFF, false)

    // Legacy aliases for compatibility
    fun userRole(ctx: Context): String? = staffRole(ctx)
    fun userName(ctx: Context): String? = staffName(ctx)

    fun isManagerOrAdmin(ctx: Context): Boolean {
        return when (staffRole(ctx)?.lowercase()) {
            "admin", "administrador", "manager", "encargado" -> true
            else -> false
        }
    }

    data class Snapshot(
        val token: String?,
        val eventId: String?,
        val staffName: String?,
        val staffRole: String?,
        val staffCardUid: String?,
        val isStaff: Boolean
    )

    fun snapshot(ctx: Context) = Snapshot(
        token(ctx),
        eventId(ctx),
        staffName(ctx),
        staffRole(ctx),
        staffCardUid(ctx),
        isStaff(ctx)
    )
}
