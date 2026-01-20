package net.ljga.projects.games.tetris.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_progress")
data class GameProgress(
    @PrimaryKey val id: Int = 1, // Single entry for game progress
    val highScore: Int,
    val coins: Int,
    val gameState: String?
)
