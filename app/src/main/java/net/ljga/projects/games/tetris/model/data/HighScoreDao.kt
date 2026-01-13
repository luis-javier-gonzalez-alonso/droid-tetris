package net.ljga.projects.games.tetris.model.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HighScoreDao {
    @Query("SELECT * FROM high_scores WHERE id = 1")
    fun getHighScore(): Flow<HighScore?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateHighScore(highScore: HighScore)
}
