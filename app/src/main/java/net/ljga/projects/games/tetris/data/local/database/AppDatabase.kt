package net.ljga.projects.games.tetris.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HighScore::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun highScoreDao(): HighScoreDao
}
