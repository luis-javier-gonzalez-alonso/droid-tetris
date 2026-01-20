package net.ljga.projects.games.tetris.data

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.ljga.projects.games.tetris.data.local.database.EnabledMutation
import net.ljga.projects.games.tetris.data.local.database.EnabledMutationDao
import net.ljga.projects.games.tetris.data.local.database.GameProgressDao
import net.ljga.projects.games.tetris.data.local.database.OwnedBadge
import net.ljga.projects.games.tetris.data.local.database.OwnedBadgeDao
import net.ljga.projects.games.tetris.data.local.database.UnlockedMutation
import net.ljga.projects.games.tetris.data.local.database.UnlockedMutationDao
import net.ljga.projects.games.tetris.ui.game.GameViewModel
import javax.inject.Inject

class GameplayDataRepository @Inject constructor(
    private val gameProgressDao: GameProgressDao,
    private val unlockedMutationDao: UnlockedMutationDao,
    private val enabledMutationDao: EnabledMutationDao,
    private val ownedBadgeDao: OwnedBadgeDao,
    private val gson: Gson
) {

    val gameProgress = gameProgressDao.getGameProgress()

    val unlockedMutations: Flow<Set<String>> = unlockedMutationDao.getUnlockedMutations()
        .map { list -> list.map { it.name }.toSet() }

    val enabledMutations: Flow<Set<String>> = enabledMutationDao.getEnabledMutations()
        .map { list -> list.map { it.name }.toSet() }

    val ownedBadges: Flow<Set<String>> = ownedBadgeDao.getOwnedBadges()
        .map { list -> list.map { it.name }.toSet() }

    val gameState: Flow<GameViewModel.GameState?> = gameProgress.map { progress ->
        progress?.gameState?.let {
            try {
                gson.fromJson(it, GameViewModel.GameState::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun updateHighScore(score: Int) {
        val currentProgress = gameProgress.first()?.copy(highScore = score) ?: return
        gameProgressDao.updateGameProgress(currentProgress)
    }

    suspend fun saveGameState(gameState: GameViewModel.GameState) {
        val json = gson.toJson(gameState)
        val currentProgress = gameProgress.first()?.copy(gameState = json) ?: return
        gameProgressDao.updateGameProgress(currentProgress)
    }

    suspend fun clearGameState() {
        val currentProgress = gameProgress.first()?.copy(gameState = null) ?: return
        gameProgressDao.updateGameProgress(currentProgress)
    }

    suspend fun setMutationEnabled(mutationName: String, enabled: Boolean) {
        if (enabled) {
            enabledMutationDao.addEnabledMutation(EnabledMutation(mutationName))
        } else {
            enabledMutationDao.removeEnabledMutation(mutationName)
        }
    }

    suspend fun addCoins(amount: Int) {
        val currentProgress = gameProgress.first() ?: return
        val newCoins = currentProgress.coins + amount
        gameProgressDao.updateCoins(newCoins)
    }

    suspend fun purchaseBadge(badgeName: String, cost: Int): Boolean {
        val currentProgress = gameProgress.first() ?: return false
        val currentBadges = ownedBadges.first()

        return if (currentProgress.coins >= cost && !currentBadges.contains(badgeName)) {
            val newCoins = currentProgress.coins - cost
            gameProgressDao.updateCoins(newCoins)
            ownedBadgeDao.addOwnedBadge(OwnedBadge(badgeName))
            true
        } else {
            false
        }
    }

    suspend fun purchaseMutation(mutationName: String, cost: Int): Boolean {
        val currentProgress = gameProgress.first() ?: return false
        val unlocked = unlockedMutations.first()

        return if (currentProgress.coins >= cost && !unlocked.contains(mutationName)) {
            val newCoins = currentProgress.coins - cost
            gameProgressDao.updateCoins(newCoins)
            unlockedMutationDao.addUnlockedMutation(UnlockedMutation(mutationName))
            enabledMutationDao.addEnabledMutation(EnabledMutation(mutationName))
            true
        } else {
            false
        }
    }
}
