package net.ljga.projects.games.tetris.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GameProgress::class, UnlockedMutation::class, EnabledMutation::class, OwnedBadge::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameProgressDao(): GameProgressDao
    abstract fun unlockedMutationDao(): UnlockedMutationDao
    abstract fun enabledMutationDao(): EnabledMutationDao
    abstract fun ownedBadgeDao(): OwnedBadgeDao
}
