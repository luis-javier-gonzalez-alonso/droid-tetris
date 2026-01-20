
package net.ljga.projects.games.tetris.ui.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import net.ljga.projects.games.tetris.R
import net.ljga.projects.games.tetris.data.GameplayDataRepository
import net.ljga.projects.games.tetris.domain.game.Artifact
import net.ljga.projects.games.tetris.domain.game.BoardShrinkerArtifact
import net.ljga.projects.games.tetris.domain.game.BoardWipeArtifact
import net.ljga.projects.games.tetris.domain.game.ChaosOrbArtifact
import net.ljga.projects.games.tetris.domain.game.ClairvoyanceMutation
import net.ljga.projects.games.tetris.domain.game.ColorblindMutation
import net.ljga.projects.games.tetris.domain.game.FairPlayMutation
import net.ljga.projects.games.tetris.domain.game.FallingFragmentsArtifact
import net.ljga.projects.games.tetris.domain.game.FeatherFallMutation
import net.ljga.projects.games.tetris.domain.game.GameMechanic
import net.ljga.projects.games.tetris.domain.game.GarbageCollectorMutation
import net.ljga.projects.games.tetris.domain.game.IBeforeLineClearHook
import net.ljga.projects.games.tetris.domain.game.ILineClearStrategy
import net.ljga.projects.games.tetris.domain.game.IOnLevelUpHook
import net.ljga.projects.games.tetris.domain.game.IOnLineClearHook
import net.ljga.projects.games.tetris.domain.game.IOnNewGameHook
import net.ljga.projects.games.tetris.domain.game.IOnPieceSpawnHook
import net.ljga.projects.games.tetris.domain.game.IPositionValidator
import net.ljga.projects.games.tetris.domain.game.IPostRotationPlacementModifier
import net.ljga.projects.games.tetris.domain.game.IRequiresGhostPiece
import net.ljga.projects.games.tetris.domain.game.IRotationDirectionModifier
import net.ljga.projects.games.tetris.domain.game.IRotationOverride
import net.ljga.projects.games.tetris.domain.game.IScoreModifier
import net.ljga.projects.games.tetris.domain.game.ITickDelayModifier
import net.ljga.projects.games.tetris.domain.game.InvertedRotationArtifact
import net.ljga.projects.games.tetris.domain.game.LeadFallMutation
import net.ljga.projects.games.tetris.domain.game.LineClearerArtifact
import net.ljga.projects.games.tetris.domain.game.MoreIsMutation
import net.ljga.projects.games.tetris.domain.game.Mutation
import net.ljga.projects.games.tetris.domain.game.PhantomPieceMutation
import net.ljga.projects.games.tetris.domain.game.PieceSwapperArtifact
import net.ljga.projects.games.tetris.domain.game.ScoreMultiplierArtifact
import net.ljga.projects.games.tetris.domain.game.SpringLoadedRotatorArtifact
import net.ljga.projects.games.tetris.domain.game.SwiftnessCharmArtifact
import net.ljga.projects.games.tetris.domain.game.TimeWarpMutation
import net.ljga.projects.games.tetris.domain.game.UnyieldingMutation
import net.ljga.projects.games.tetris.domain.game.applyStartingMutations
import net.ljga.projects.games.tetris.domain.game.createEmptyBoard
import net.ljga.projects.games.tetris.domain.game.movePiece
import net.ljga.projects.games.tetris.domain.game.runGame
import net.ljga.projects.games.tetris.domain.game.updateGhostPiece
import java.util.Locale
import javax.inject.Inject

private const val TAG = "GameViewModel"

@HiltViewModel
class GameViewModel @Inject constructor(
    val gameplayDataRepository: GameplayDataRepository,
    val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val boardWidth = 10
    val boardHeight = 20

    val _gameState = MutableStateFlow(GameState(
        createEmptyBoard(), null, null, null, 0, 0, emptyList(), 0, 1, 5, false,
        emptyList(), emptyList(), null, listOf(), 0, emptyList(), 0, false, emptyList(),
        seed = 0L, rngCount = 0
    ))
    val gameState: StateFlow<GameState> = _gameState

    var _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore

    private var _mutations = MutableStateFlow<List<Mutation>>(emptyList())
    val mutations: StateFlow<List<Mutation>> = _mutations

    val coins: StateFlow<Int> = gameplayDataRepository.gameProgress
        .map { it?.coins ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val ownedBadges: StateFlow<Set<String>> = gameplayDataRepository.ownedBadges
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val unlockedMutations: StateFlow<Set<String>> = gameplayDataRepository.unlockedMutations
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val enabledMutations: StateFlow<Set<String>> = gameplayDataRepository.enabledMutations
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val lastSeed: StateFlow<Long?> = settingsDataStore.lastSeed
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var debugMutations: List<Mutation> = emptyList()
    var debugArtifacts: List<Artifact> = emptyList()

    var gameJob: Job? = null

    init {
        viewModelScope.launch {
            _highScore.value = gameplayDataRepository.gameProgress.first()?.highScore ?: 0
            val activeMutationNames = gameplayDataRepository.enabledMutations.first()
            _mutations.value = allMutations.filter { activeMutationNames.contains(it.name) }

            gameplayDataRepository.gameState.first()?.let {
                _gameState.value = it
                // Restore RNG state
                gameRandom = GameRandom(it.seed)
                // Fast forward
                repeat(it.rngCount) { gameRandom.nextBits(32) }
            } ?: run {
                val startingMutation = if (_mutations.value.isNotEmpty()) listOf(_mutations.value.random(kotlin.random.Random)) else emptyList()
                // Initial empty state, will be overwritten by newGame or saved state
                // seed=0 is temporary
                _gameState.value = _gameState.value.copy(
                    selectedMutations = startingMutation,
                    pendingMutationPopup = startingMutation.firstOrNull(),
                    seed = 0L,
                    rngCount = 0
                )
            }
        }
    }

    private var gameRandom: GameRandom = GameRandom(0)
    val rng: kotlin.random.Random get() = gameRandom

    class GameRandom(val seed: Long) : kotlin.random.Random() {
        private val impl = kotlin.random.Random(seed)
        var count = 0
            private set

        override fun nextBits(bitCount: Int): Int {
            count++
            return impl.nextBits(bitCount)
        }
    }

    data class GameState(
        val board: Array<IntArray>,
        val piece: Piece?,
        val nextPiece: Piece?,
        val secondNextPiece: Piece?,
        val pieceX: Int,
        val pieceY: Int,
        val clearingLines: List<Int>,
        val currentScore: Int,
        val level: Int,
        val linesUntilNextLevel: Int,
        val isGameOver: Boolean,
        val artifacts: List<Artifact>,
        val selectedMutations: List<Mutation>,
        val currentBoss: Boss?,
        val pieceQueue: List<Piece>,
        val ghostPieceY: Int,
        val artifactChoices: List<Artifact>,
        var rotationCount: Int,
        val isDebugMode: Boolean,
        val fallingFragments: List<Pair<Int, Int>>,
        val pendingMutationPopup: GameMechanic? = null, // For showing acquired artifacts/mutations
        val seed: Long,
        val rngCount: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as GameState

            if (!board.contentDeepEquals(other.board)) return false
            if (piece != other.piece) return false
            if (nextPiece != other.nextPiece) return false
            if (secondNextPiece != other.secondNextPiece) return false
            if (pieceX != other.pieceX) return false
            if (pieceY != other.pieceY) return false
            if (clearingLines != other.clearingLines) return false
            if (currentScore != other.currentScore) return false
            if (level != other.level) return false
            if (linesUntilNextLevel != other.linesUntilNextLevel) return false
            if (isGameOver != other.isGameOver) return false
            if (artifacts != other.artifacts) return false
            if (selectedMutations != other.selectedMutations) return false
            if (currentBoss != other.currentBoss) return false
            if (pieceQueue != other.pieceQueue) return false
            if (ghostPieceY != other.ghostPieceY) return false
            if (artifactChoices != other.artifactChoices) return false
            if (rotationCount != other.rotationCount) return false
            if (isDebugMode != other.isDebugMode) return false
            if (fallingFragments != other.fallingFragments) return false
            if (pendingMutationPopup != other.pendingMutationPopup) return false
            if (seed != other.seed) return false
            if (rngCount != other.rngCount) return false

            return true
        }

        override fun hashCode(): Int {
            var result = board.contentDeepHashCode()
            result = 31 * result + (piece?.hashCode() ?: 0)
            result = 31 * result + (nextPiece?.hashCode() ?: 0)
            result = 31 * result + (secondNextPiece?.hashCode() ?: 0)
            result = 31 * result + pieceX
            result = 31 * result + pieceY
            result = 31 * result + clearingLines.hashCode()
            result = 31 * result + currentScore
            result = 31 * result + level
            result = 31 * result + linesUntilNextLevel
            result = 31 * result + isGameOver.hashCode()
            result = 31 * result + artifacts.hashCode()
            result = 31 * result + selectedMutations.hashCode()
            result = 31 * result + (currentBoss?.hashCode() ?: 0)
            result = 31 * result + pieceQueue.hashCode()
            result = 31 * result + ghostPieceY
            result = 31 * result + artifactChoices.hashCode()
            result = 31 * result + rotationCount
            result = 31 * result + isDebugMode.hashCode()
            result = 31 * result + fallingFragments.hashCode()
            result = 31 * result + (pendingMutationPopup?.hashCode() ?: 0)
            result = 31 * result + seed.hashCode()
            result = 31 * result + rngCount
            return result
        }
    }

    data class Piece(val shape: Array<IntArray>, val color: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Piece

            if (!shape.contentDeepEquals(other.shape)) return false
            if (color != other.color) return false

            return true
        }

        override fun hashCode(): Int {
            var result = shape.contentDeepHashCode()
            result = 31 * result + color
            return result
        }
    }

    data class Boss(val name: String, val nameResId: Int, var requiredLines: Int)

    data class Badge(val id: String, val name: String, val nameResId: Int, val iconResId: Int, val cost: Int)

    companion object {
        val pieces = listOf(
            Piece(arrayOf(intArrayOf(1, 1, 1, 1)), 1), // I
            Piece(arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)), 2), // O
            Piece(arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1)), 3), // T
            Piece(arrayOf(intArrayOf(0, 0, 1), intArrayOf(1, 1, 1)), 4), // L
            Piece(arrayOf(intArrayOf(1, 0, 0), intArrayOf(1, 1, 1)), 5), // J
            Piece(arrayOf(intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)), 6), // S
            Piece(arrayOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)), 7)  // Z
        )
    }

    val allMutations = listOf(
        UnyieldingMutation(),
        FeatherFallMutation(),
        LeadFallMutation(),
        ClairvoyanceMutation(),
        ColorblindMutation(),
        MoreIsMutation(),
        GarbageCollectorMutation(),
        TimeWarpMutation(),
        FairPlayMutation(),
        PhantomPieceMutation()
    )

    val allArtifacts = listOf(
        SwiftnessCharmArtifact(),
        LineClearerArtifact(),
        ScoreMultiplierArtifact(),
        SpringLoadedRotatorArtifact(),
        ChaosOrbArtifact(),
        FallingFragmentsArtifact(),
        BoardWipeArtifact(),
        InvertedRotationArtifact(),
        PieceSwapperArtifact(),
        BoardShrinkerArtifact()
    )

    val allBadges = listOf(
        Badge("axe", "Axe", R.string.badge_axe, R.drawable.ic_badge_axe, 300),
        Badge("bomb", "Bomb", R.string.badge_bomb, R.drawable.ic_badge_bomb, 300),
        Badge("book", "Book", R.string.badge_book, R.drawable.ic_badge_book, 300),
        Badge("boots", "Boots", R.string.badge_boots, R.drawable.ic_badge_boots, 300),
        Badge("bulb", "Bulb", R.string.badge_bulb, R.drawable.ic_badge_bulb, 300),
        Badge("cat", "Cat", R.string.badge_cat, R.drawable.ic_badge_cat, 300),
        Badge("chest", "Chest", R.string.badge_chest, R.drawable.ic_badge_chest, 500),
        Badge("coin", "Coin", R.string.badge_coin, R.drawable.ic_badge_coin, 300),
        Badge("dog", "Dog", R.string.badge_dog, R.drawable.ic_badge_dog, 300),
        Badge("feather", "Feather", R.string.badge_feather, R.drawable.ic_badge_feather, 300),
        Badge("fire", "Fire", R.string.badge_fire, R.drawable.ic_badge_fire, 300),
        Badge("gold", "Gold", R.string.badge_gold, R.drawable.ic_badge_gold, 500),
        Badge("hat", "Hat", R.string.badge_hat, R.drawable.ic_badge_hat, 300),
        Badge("heart", "Heart", R.string.badge_heart, R.drawable.ic_badge_heart, 300),
        Badge("key", "Key", R.string.badge_key, R.drawable.ic_badge_key, 300),
        Badge("map", "Map", R.string.badge_map, R.drawable.ic_badge_map, 300),
        Badge("plant", "Plant", R.string.badge_plant, R.drawable.ic_badge_plant, 300),
        Badge("potion", "Potion", R.string.badge_potion, R.drawable.ic_badge_potion, 300),
        Badge("potion2", "Elixir", R.string.badge_elixir, R.drawable.ic_badge_potion2, 300),
        Badge("ring", "Ring", R.string.badge_ring, R.drawable.ic_badge_ring, 300),
        Badge("shield", "Shield", R.string.badge_shield, R.drawable.ic_badge_shield, 300),
        Badge("sunmoon", "Sun & Moon", R.string.badge_sun_moon, R.drawable.ic_badge_sunmoon, 500),
        Badge("sword", "Sword", R.string.badge_sword, R.drawable.ic_badge_sword, 300),
        Badge("wand", "Wand", R.string.badge_wand, R.drawable.ic_badge_wand, 300)
    )

    // Helper functions to reduce hook invocation duplication
    internal inline fun <reified T> GameState.applyHook(transform: (T, GameState) -> GameState): GameState {
        var state = this
        selectedMutations.filterIsInstance<T>().forEach { state = transform(it, state) }
        artifacts.filterIsInstance<T>().forEach { state = transform(it, state) }
        return state
    }

    internal inline fun <reified T> GameState.applyModifier(
        initial: Long,
        transform: (T, Long) -> Long
    ): Long {
        var result = initial
        selectedMutations.filterIsInstance<T>().forEach { result = transform(it, result) }
        artifacts.filterIsInstance<T>().forEach { result = transform(it, result) }
        return result
    }

    val bosses = listOf(
        Boss("The Wall", R.string.boss_wall, 10),
        Boss("The Sprinter", R.string.boss_sprinter, 15)
    )

    fun newGame(seed: Long? = null) {
        newGame(emptyList(), emptyList(), seed)
    }

    fun newGame(mutations: List<Mutation>, artifacts: List<Artifact>, seed: Long? = null) {
        gameJob?.cancel()
        gameJob = null
        Log.d(TAG, "Starting new game...")
        viewModelScope.launch {
            val actualSeed = seed ?: kotlin.random.Random.nextLong()
            gameRandom = GameRandom(actualSeed)
            settingsDataStore.updateLastSeed(actualSeed)

            val isClassicMode = settingsDataStore.isClassicMode.first()
            val isDebugMode = mutations.isNotEmpty() || artifacts.isNotEmpty()
            val startingMutations = if (isDebugMode) {
                mutations
            } else if (isClassicMode) {
                emptyList()
            } else {
                val activeMutationNames = gameplayDataRepository.enabledMutations.first()
                val activeMutations = allMutations.filter { activeMutationNames.contains(it.name) }
                if (activeMutations.isNotEmpty()) listOf(activeMutations.random(gameRandom)) else emptyList()
            }

            _gameState.value = GameState(
                createEmptyBoard(), null, 
                Companion.pieces.random(gameRandom), 
                Companion.pieces.random(gameRandom), 
                0, 0, emptyList(), 0, 1, 5, false, artifacts, startingMutations, null, 
                Companion.pieces.shuffled(gameRandom), 
                0, emptyList(), 0, isDebugMode, emptyList(), 
                pendingMutationPopup = startingMutations.firstOrNull(),
                seed = actualSeed,
                rngCount = gameRandom.count
            )
            applyStartingMutations()

            gameplayDataRepository.clearGameState()
            
            // Only run game immediately if there is NO popup pending
            // If there is a popup, we wait for dismissal
            if (startingMutations.isEmpty()) {
                runGame()
            }
        }
    }

    val languageCode: StateFlow<String> = settingsDataStore.languageCode
        .map { code ->
            if (code.isNullOrEmpty()) {
                val systemLang = Locale.getDefault().language
                if (systemLang == "es") "es" else "en"
            } else {
                code
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    fun pauseGame() {
        gameJob?.cancel()
        gameJob = null
        if (_gameState.value.piece != null) {
            viewModelScope.launch {
                // Update RNG count in state before saving
                _gameState.value = _gameState.value.copy(rngCount = gameRandom.count)
                gameplayDataRepository.saveGameState(_gameState.value)
            }
        }
        Log.d(TAG, "Game paused.")
    }

    fun continueGame() {
        if (gameJob?.isActive != true) {
            Log.d(TAG, "Continuing game...")
            runGame()
        }
    }

    fun moveLeft() {
        if (gameJob?.isActive != true) return
        movePiece(-1, 0)
        _gameState.value = _gameState.value.copy(rotationCount = 0)
    }

    fun moveRight() {
        if (gameJob?.isActive != true) return
        movePiece(1, 0)
        _gameState.value = _gameState.value.copy(rotationCount = 0)
    }

    fun moveDown() {
        if (gameJob?.isActive != true) return
        movePiece(0, 1)
        _gameState.value = _gameState.value.copy(rotationCount = 0)
    }

    fun rotate() {
        if (gameJob?.isActive != true) return

        _gameState.value.rotationCount++

        for (artifact in _gameState.value.artifacts) {
            if (artifact is IRotationOverride) {
                val newState = artifact.onRotate(_gameState.value, this)
                if (newState != null) {
                    _gameState.value = newState
                    updateGhostPiece()
                    return
                }
            }
        }

        val currentPiece = _gameState.value.piece ?: return

        var isInverted = false
        for (artifact in _gameState.value.artifacts) {
            if (artifact is IRotationDirectionModifier) {
                isInverted = artifact.isInverted()
                break
            }
        }

        val rotatedShape = Array(currentPiece.shape[0].size) { IntArray(currentPiece.shape.size) }
        if (isInverted) {
            for (y in currentPiece.shape.indices) {
                for (x in currentPiece.shape[y].indices) {
                    rotatedShape[rotatedShape.size - 1 - x][y] = currentPiece.shape[y][x]
                }
            }
        } else {
            for (y in currentPiece.shape.indices) {
                for (x in currentPiece.shape[y].indices) {
                    rotatedShape[x][currentPiece.shape.size - 1 - y] = currentPiece.shape[y][x]
                }
            }
        }

        val newPiece = Piece(rotatedShape, currentPiece.color)
        var newX = _gameState.value.pieceX
        var newY = _gameState.value.pieceY

        for (artifact in _gameState.value.artifacts) {
            if (artifact is IPostRotationPlacementModifier) {
                val (x, y) = artifact.modifyPlacement(newX, newY, newPiece, _gameState.value)
                newX = x
                newY = y
            }
        }

        if (isValidPosition(newX, newY, newPiece, _gameState.value.board)) {
            _gameState.value = _gameState.value.copy(piece = newPiece, pieceX = newX, pieceY = newY)
            updateGhostPiece()
        }
    }
    
    fun isValidPosition(x: Int, y: Int, piece: Piece, board: Array<IntArray>): Boolean {
        var defaultResult = true
        for (py in piece.shape.indices) {
            for (px in piece.shape[py].indices) {
                if (piece.shape[py][px] != 0) {
                    val boardX = x + px
                    val boardY = y + py

                    if (boardX < 0 || boardX >= boardWidth || boardY < 0 || boardY >= boardHeight || board[boardY][boardX] != 0) {
                        defaultResult = false
                        break
                    }
                }
            }
            if (!defaultResult) break
        }

        for (artifact in _gameState.value.artifacts) {
            if (artifact is IPositionValidator) {
                if (!artifact.isValidPosition(x, y, piece, _gameState.value, defaultResult)) {
                    return false
                }
            }
        }
        return defaultResult
    }

    fun selectArtifact(newArtifact: Artifact) {
        viewModelScope.launch {
            val currentArtifacts = _gameState.value.artifacts.toMutableList()

            // 1. Enforce exclusivity for standard GameMechanic hooks
            // (Newest artifact overrides older ones implementing the same hook)
            val hooksToCheck = listOf<Class<*>>(
                IOnNewGameHook::class.java,
                IOnLevelUpHook::class.java,
                IOnPieceSpawnHook::class.java,
                IOnLineClearHook::class.java,
                ITickDelayModifier::class.java,
                IScoreModifier::class.java,
                IRotationOverride::class.java,
                IRotationDirectionModifier::class.java,
                IPostRotationPlacementModifier::class.java,
                IPositionValidator::class.java,
                IBeforeLineClearHook::class.java,
                IRequiresGhostPiece::class.java
            )

            hooksToCheck.forEach { hookClass ->
                if (hookClass.isAssignableFrom(newArtifact.javaClass)) {
                    currentArtifacts.removeAll { hookClass.isAssignableFrom(it.javaClass) }
                }
            }

            // 2. Special exclusivity for ILineClearStrategy (Overlap based)
            if (newArtifact is ILineClearStrategy) {
                val newCounts = newArtifact.supportedLineCounts
                currentArtifacts.removeAll { existing ->
                    existing is ILineClearStrategy && existing.supportedLineCounts.any { newCounts.contains(it) }
                }
            }

            currentArtifacts.add(newArtifact)

            _gameState.value = _gameState.value.copy(
                artifacts = currentArtifacts,
                artifactChoices = emptyList(),
                pendingMutationPopup = newArtifact
            )
            // Game remains paused until popup is dismissed
        }
    }

    fun dismissMutationPopup() {
        _gameState.value = _gameState.value.copy(pendingMutationPopup = null)
        continueGame()
    }

    fun purchaseBadge(badgeName: String, cost: Int) {
        viewModelScope.launch {
            gameplayDataRepository.purchaseBadge(badgeName, cost)
        }
    }

    fun purchaseMutation(mutationName: String, cost: Int) {
        viewModelScope.launch {
            if (gameplayDataRepository.purchaseMutation(mutationName, cost)) {
                // Update local list if needed, though flows handle it
                val enabled = gameplayDataRepository.enabledMutations.first()
                _mutations.value = allMutations.filter { enabled.contains(it.name) }
            }
        }
    }

    fun toggleMutation(mutationName: String, enabled: Boolean) {
        viewModelScope.launch {
            gameplayDataRepository.setMutationEnabled(mutationName, enabled)
            val updatedEnabled = gameplayDataRepository.enabledMutations.first()
            _mutations.value = allMutations.filter { updatedEnabled.contains(it.name) }
        }
    }
}
