package net.ljga.projects.games.tetris.ui.game

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    private val languageCodeKey = stringPreferencesKey("language_code")
    private val isClassicModeKey = booleanPreferencesKey("is_classic_mode")
    private val touchSensitivityKey = floatPreferencesKey("touch_sensitivity")
    private val lastSeedKey = longPreferencesKey("last_seed")

    val languageCode: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[languageCodeKey] }

    val isClassicMode: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[isClassicModeKey] ?: false }

    val touchSensitivity: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[touchSensitivityKey] ?: 2.0f }

    val lastSeed: Flow<Long?> = context.dataStore.data
        .map { preferences -> preferences[lastSeedKey] }

    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { it[languageCodeKey] = code }
    }

    suspend fun setClassicMode(enabled: Boolean) {
        context.dataStore.edit { it[isClassicModeKey] = enabled }
    }

    suspend fun setTouchSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[touchSensitivityKey] = sensitivity }
    }

    suspend fun updateLastSeed(seed: Long) {
        context.dataStore.edit { it[lastSeedKey] = seed }
    }
}
