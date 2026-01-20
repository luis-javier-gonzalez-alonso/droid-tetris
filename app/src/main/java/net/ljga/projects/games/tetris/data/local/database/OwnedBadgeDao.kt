package net.ljga.projects.games.tetris.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OwnedBadgeDao {
    @Query("SELECT * FROM owned_badges")
    fun getOwnedBadges(): Flow<List<OwnedBadge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOwnedBadge(ownedBadge: OwnedBadge)
}
