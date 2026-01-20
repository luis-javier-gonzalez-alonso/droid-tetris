package net.ljga.projects.games.tetris.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProgressDao {
    @Query("SELECT * FROM game_progress WHERE id = 1")
    fun getGameProgress(): Flow<GameProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateGameProgress(gameProgress: GameProgress)

    @Query("UPDATE game_progress SET coins = :coins WHERE id = 1")
    suspend fun updateCoins(coins: Int)

    @Query("UPDATE game_progress SET gameState = :gameState WHERE id = 1")
    suspend fun updateGameState(gameState: String?)
}
