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
import net.ljga.projects.games.tetris.domain.game.Artifact
import net.ljga.projects.games.tetris.domain.game.BoardShrinkerArtifact
import net.ljga.projects.games.tetris.domain.game.BoardWipeArtifact
import net.ljga.projects.games.tetris.domain.game.ChaosOrbArtifact
import net.ljga.projects.games.tetris.domain.game.ClairvoyanceMutation
import net.ljga.projects.games.tetris.domain.game.ColorblindMutation
import net.ljga.projects.games.tetris.domain.game.FairPlayMutation
import net.ljga.projects.games.tetris.domain.game.FallingFragmentsArtifact
import net.ljga.projects.games.tetris.domain.game.FeatherFallMutation
import net.ljga.projects.games.tetris.domain.game.GarbageCollectorMutation
import net.ljga.projects.games.tetris.domain.game.InvertedRotationArtifact
import net.ljga.projects.games.tetris.domain.game.LeadFallMutation
import net.ljga.projects.games.tetris.domain.game.LineClearerArtifact
import net.ljga.projects.games.tetris.domain.game.MoreIsMutation
import net.ljga.projects.games.tetris.domain.game.Mutation
import net.ljga.projects.games.tetris.domain.game.PhantomPieceMutation
import net.ljga.projects.games.tetris.domain.game.PieceSwapperArtifact
import net.ljga.projects.games.tetris.ui.game.RuntimeTypeAdapterFactory
import net.ljga.projects.games.tetris.domain.game.ScoreMultiplierArtifact
import net.ljga.projects.games.tetris.domain.game.SpringLoadedRotatorArtifact
import net.ljga.projects.games.tetris.domain.game.SwiftnessCharmArtifact
import net.ljga.projects.games.tetris.domain.game.TimeWarpMutation
import net.ljga.projects.games.tetris.domain.game.UnyieldingMutation
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
