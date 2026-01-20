package net.ljga.projects.games.tetris.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockedMutationDao {
    @Query("SELECT * FROM unlocked_mutations")
    fun getUnlockedMutations(): Flow<List<UnlockedMutation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addUnlockedMutation(unlockedMutation: UnlockedMutation)
}
