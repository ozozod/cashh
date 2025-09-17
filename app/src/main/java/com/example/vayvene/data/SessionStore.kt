package com.example.vayvene.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

class SessionStore(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val ROLE = stringPreferencesKey("role")
        val EVENT = longPreferencesKey("eventId")
        val IS_STAFF = booleanPreferencesKey("isStaff")
    }

    val token: Flow<String?> = context.dataStore.data.map { prefs -> prefs[Keys.TOKEN] }
    val role: Flow<String?> = context.dataStore.data.map { prefs -> prefs[Keys.ROLE] }
    val eventId: Flow<Long?> = context.dataStore.data.map { prefs -> prefs[Keys.EVENT] }
    val isStaff: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[Keys.IS_STAFF] ?: false }

    suspend fun saveToken(value: String) {
        context.dataStore.edit { it[Keys.TOKEN] = value }
    }
    suspend fun saveStaffRole(value: String) {
        context.dataStore.edit { it[Keys.ROLE] = value }
    }
    suspend fun saveEventId(value: Long) {
        context.dataStore.edit { it[Keys.EVENT] = value }
    }
    suspend fun saveIsStaff(value: Boolean) {
        context.dataStore.edit { it[Keys.IS_STAFF] = value }
    }
    suspend fun clear() { context.dataStore.edit { it.clear() } }
}
