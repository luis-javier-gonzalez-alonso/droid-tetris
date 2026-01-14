package net.ljga.projects.games.tetris.ui.game

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceDataStore(private val context: Context) {

    private val gson = Gson()
    private val highScoreKey = intPreferencesKey("high_score")
    private val gameStateKey = stringPreferencesKey("game_state")

    val highScore: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[highScoreKey] ?: 0
        }

    val gameState: Flow<GameViewModel.GameState?> = context.dataStore.data
        .map { preferences ->
            val json = preferences[gameStateKey]
            if (json == null) {
                null
            } else {
                gson.fromJson(json, GameViewModel.GameState::class.java)
            }
        }

    suspend fun updateHighScore(score: Int) {
        context.dataStore.edit {
            it[highScoreKey] = score
        }
    }

    suspend fun saveGameState(gameState: GameViewModel.GameState) {
        context.dataStore.edit {
            val json = gson.toJson(gameState)
            it[gameStateKey] = json
        }
    }

    suspend fun clearGameState() {
        context.dataStore.edit {
            it.remove(gameStateKey)
        }
    }
}
