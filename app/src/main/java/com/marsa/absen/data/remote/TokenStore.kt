package com.marsa.absen.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "marsa_prefs")

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tokenKey = stringPreferencesKey("api_token")
    private val userKey = stringPreferencesKey("user_data")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val user: Flow<String?> = context.dataStore.data.map { it[userKey] }

    suspend fun save(token: String, userJson: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = token
            if (userJson != null) prefs[userKey] = userJson
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
