package net.ljga.projects.games.tetris.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlocked_mutations")
data class UnlockedMutation(
    @PrimaryKey val name: String
)
