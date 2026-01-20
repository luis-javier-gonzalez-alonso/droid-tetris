package net.ljga.projects.games.tetris.data.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.ljga.projects.games.tetris.data.GameplayDataRepository
import net.ljga.projects.games.tetris.data.local.database.EnabledMutationDao
import net.ljga.projects.games.tetris.data.local.database.GameProgressDao
import net.ljga.projects.games.tetris.data.local.database.OwnedBadgeDao
import net.ljga.projects.games.tetris.data.local.database.UnlockedMutationDao
import net.ljga.projects.games.tetris.ui.game.Artifact
import net.ljga.projects.games.tetris.ui.game.BoardShrinkerArtifact
import net.ljga.projects.games.tetris.ui.game.BoardWipeArtifact
import net.ljga.projects.games.tetris.ui.game.ChaosOrbArtifact
import net.ljga.projects.games.tetris.ui.game.ClairvoyanceMutation
import net.ljga.projects.games.tetris.ui.game.ColorblindMutation
import net.ljga.projects.games.tetris.ui.game.FairPlayMutation
import net.ljga.projects.games.tetris.ui.game.FallingFragmentsArtifact
import net.ljga.projects.games.tetris.ui.game.FeatherFallMutation
import net.ljga.projects.games.tetris.ui.game.GarbageCollectorMutation
import net.ljga.projects.games.tetris.ui.game.InvertedRotationArtifact
import net.ljga.projects.games.tetris.ui.game.LeadFallMutation
import net.ljga.projects.games.tetris.ui.game.LineClearerArtifact
import net.ljga.projects.games.tetris.ui.game.MoreIsMutation
import net.ljga.projects.games.tetris.ui.game.Mutation
import net.ljga.projects.games.tetris.ui.game.PhantomPieceMutation
import net.ljga.projects.games.tetris.ui.game.PieceSwapperArtifact
import net.ljga.projects.games.tetris.ui.game.RuntimeTypeAdapterFactory
import net.ljga.projects.games.tetris.ui.game.ScoreMultiplierArtifact
import net.ljga.projects.games.tetris.ui.game.SpringLoadedRotatorArtifact
import net.ljga.projects.games.tetris.ui.game.SwiftnessCharmArtifact
import net.ljga.projects.games.tetris.ui.game.TimeWarpMutation
import net.ljga.projects.games.tetris.ui.game.UnyieldingMutation
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DataModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapterFactory(
                RuntimeTypeAdapterFactory(Artifact::class)
                    .registerSubtype(SwiftnessCharmArtifact::class)
                    .registerSubtype(LineClearerArtifact::class)
                    .registerSubtype(ScoreMultiplierArtifact::class)
                    .registerSubtype(SpringLoadedRotatorArtifact::class)
                    .registerSubtype(ChaosOrbArtifact::class)
                    .registerSubtype(FallingFragmentsArtifact::class)
                    .registerSubtype(BoardWipeArtifact::class)
                    .registerSubtype(InvertedRotationArtifact::class)
                    .registerSubtype(PieceSwapperArtifact::class)
                    .registerSubtype(BoardShrinkerArtifact::class)
            )
            .registerTypeAdapterFactory(
                RuntimeTypeAdapterFactory(Mutation::class)
                    .registerSubtype(UnyieldingMutation::class)
                    .registerSubtype(FeatherFallMutation::class)
                    .registerSubtype(LeadFallMutation::class)
                    .registerSubtype(ClairvoyanceMutation::class)
                    .registerSubtype(ColorblindMutation::class)
                    .registerSubtype(MoreIsMutation::class)
                    .registerSubtype(GarbageCollectorMutation::class)
                    .registerSubtype(TimeWarpMutation::class)
                    .registerSubtype(FairPlayMutation::class)
                    .registerSubtype(PhantomPieceMutation::class)
            )
            .create()
    }

    @Provides
    @Singleton
    fun provideGameplayDataRepository(
        gameProgressDao: GameProgressDao,
        unlockedMutationDao: UnlockedMutationDao,
        enabledMutationDao: EnabledMutationDao,
        ownedBadgeDao: OwnedBadgeDao,
        gson: Gson
    ): GameplayDataRepository {
        return GameplayDataRepository(
            gameProgressDao,
            unlockedMutationDao,
            enabledMutationDao,
            ownedBadgeDao,
            gson
        )
    }
}
