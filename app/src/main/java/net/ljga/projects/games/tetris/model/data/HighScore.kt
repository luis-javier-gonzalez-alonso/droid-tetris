package net.ljga.projects.games.tetris.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "high_scores")
data class HighScore(
    @PrimaryKey val id: Int = 1, // Single entry for the high score
    val score: Int
)
