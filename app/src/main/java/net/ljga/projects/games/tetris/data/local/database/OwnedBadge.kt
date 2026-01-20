package net.ljga.projects.games.tetris.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "owned_badges")
data class OwnedBadge(
    @PrimaryKey val name: String
)
