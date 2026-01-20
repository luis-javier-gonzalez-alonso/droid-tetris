package net.ljga.projects.games.tetris.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EnabledMutationDao {
    @Query("SELECT * FROM enabled_mutations")
    fun getEnabledMutations(): Flow<List<EnabledMutation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addEnabledMutation(enabledMutation: EnabledMutation)

    @Query("DELETE FROM enabled_mutations WHERE name = :name")
    suspend fun removeEnabledMutation(name: String)
}
