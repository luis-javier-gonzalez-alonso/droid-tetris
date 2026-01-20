package net.ljga.projects.games.tetris.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enabled_mutations")
data class EnabledMutation(
    @PrimaryKey val name: String
)
