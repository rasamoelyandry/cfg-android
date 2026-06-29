package com.cfg.android.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cfg_prefs")

@Singleton
class TokenManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val RESTAURANT_ID = stringPreferencesKey("restaurant_id")
        val USER_ROLE = stringPreferencesKey("user_role")
    }

    suspend fun getAccessToken(): String? =
        context.dataStore.data.map { it[ACCESS_TOKEN] }.firstOrNull()

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit {
            it[ACCESS_TOKEN] = accessToken
            it[REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun saveUserInfo(userId: String, restaurantId: String?, role: String) {
        context.dataStore.edit {
            it[USER_ID] = userId
            if (restaurantId != null) it[RESTAURANT_ID] = restaurantId
            it[USER_ROLE] = role
        }
    }

    suspend fun getRestaurantId(): String? =
        context.dataStore.data.map { it[RESTAURANT_ID] }.firstOrNull()

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun isLoggedIn(): Boolean = getAccessToken() != null
}
