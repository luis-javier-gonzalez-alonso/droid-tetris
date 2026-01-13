package net.ljga.projects.games.tetris.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ljga.projects.games.tetris.model.data.AppDatabase
import net.ljga.projects.games.tetris.model.data.HighScoreDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tetris-database"
        ).build()
    }

    @Provides
    fun provideHighScoreDao(appDatabase: AppDatabase): HighScoreDao {
        return appDatabase.highScoreDao()
    }
}
