package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "smart_farm_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

data class UserPreferencesState(
    val currentUserId: Long? = null,
    val activeFarmId: Long? = null, // null = All Farms
    val languageCode: String = "en",
    val currencyCode: String = "BDT",
    val darkModeOverride: Boolean? = null // null = follow system
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val CURRENT_USER_ID = longPreferencesKey("current_user_id")
        val ACTIVE_FARM_ID = longPreferencesKey("active_farm_id")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val DARK_MODE_OVERRIDE = stringPreferencesKey("dark_mode_override") // "system", "dark", "light"
    }

    val preferences: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        val uid = prefs[Keys.CURRENT_USER_ID]?.takeIf { it > 0L }
        val farmId = prefs[Keys.ACTIVE_FARM_ID]?.takeIf { it > 0L }
        val lang = prefs[Keys.LANGUAGE_CODE] ?: "en"
        val curr = prefs[Keys.CURRENCY_CODE] ?: "BDT"
        val darkOverride = when (prefs[Keys.DARK_MODE_OVERRIDE]) {
            "dark" -> true
            "light" -> false
            else -> null
        }
        UserPreferencesState(
            currentUserId = uid,
            activeFarmId = farmId,
            languageCode = lang,
            currencyCode = curr,
            darkModeOverride = darkOverride
        )
    }

    suspend fun setCurrentUserId(userId: Long?) {
        context.dataStore.edit { prefs ->
            if (userId == null) {
                prefs.remove(Keys.CURRENT_USER_ID)
                prefs.remove(Keys.ACTIVE_FARM_ID)
            } else {
                prefs[Keys.CURRENT_USER_ID] = userId
            }
        }
    }

    suspend fun setActiveFarmId(farmId: Long?) {
        context.dataStore.edit { prefs ->
            if (farmId == null) prefs.remove(Keys.ACTIVE_FARM_ID)
            else prefs[Keys.ACTIVE_FARM_ID] = farmId
        }
    }

    suspend fun setLanguage(code: String) {
        context.dataStore.edit { it[Keys.LANGUAGE_CODE] = code }
    }

    suspend fun setCurrency(code: String) {
        context.dataStore.edit { it[Keys.CURRENCY_CODE] = code }
    }

    suspend fun setDarkMode(dark: Boolean?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE_OVERRIDE] = when (dark) {
                true -> "dark"
                false -> "light"
                null -> "system"
            }
        }
    }
}
