package net.ljga.projects.games.tetris.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import java.util.Random

private const val TAG = "GameViewModel"

class GameViewModel(private val preferenceDataStore: PreferenceDataStore) : ViewModel() {

    val boardWidth = 10
    val boardHeight = 20

    private val _gameState = MutableStateFlow(GameState(createEmptyBoard(), null, null, null, 0, 0, emptyList(), 0, 1, 5, false, emptyList(), emptyList(), null, listOf(), 0, emptyList(), 0, false, emptyList()))
    val gameState: StateFlow<GameState> = _gameState

    private var _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore

    private var _mutations = MutableStateFlow<List<Mutation>>(emptyList())
    val mutations: StateFlow<List<Mutation>> = _mutations

    val coins: StateFlow<Int> = preferenceDataStore.coins
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    
    val ownedBadges: StateFlow<Set<String>> = preferenceDataStore.ownedBadges
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val unlockedMutations: StateFlow<Set<String>> = preferenceDataStore.unlockedMutations
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val enabledMutations: StateFlow<Set<String>> = preferenceDataStore.enabledMutations
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    var debugMutations: List<Mutation> = emptyList()
    var debugArtifacts: List<Artifact> = emptyList()

    private var gameJob: Job? = null

    init {
        viewModelScope.launch {
            _highScore.value = preferenceDataStore.highScore.first()
            _highScore.value = preferenceDataStore.highScore.first()
            val activeMutationNames = preferenceDataStore.enabledMutations.first()
            _mutations.value = allMutations.filter { activeMutationNames.contains(it.name) }

            preferenceDataStore.gameState.first()?.let {
                _gameState.value = it
            } ?: run {
                val startingMutation = if (_mutations.value.isNotEmpty()) listOf(_mutations.value.random()) else emptyList()
                _gameState.value = _gameState.value.copy(selectedMutations = startingMutation)
            }
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
        val pendingMutationPopup: GameMechanic? = null // For showing acquired artifacts/mutations
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

    data class Boss(val name: String, var requiredLines: Int)

    data class Badge(val id: String, val name: String, val iconResId: Int, val cost: Int)

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

        fun isValidPosition(x: Int, y: Int, piece: Piece, board: Array<IntArray>): Boolean {
            for (py in piece.shape.indices) {
                for (px in piece.shape[py].indices) {
                    if (piece.shape[py][px] != 0) {
                        val boardX = x + px
                        val boardY = y + py
                        if (boardX < 0 || boardX >= board[0].size || boardY < 0 || boardY >= board.size || board[boardY][boardX] != 0) {
                            return false
                        }
                    }
                }
            }
            return true
        }
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
        Badge("axe", "Axe", net.ljga.projects.games.tetris.R.drawable.ic_badge_axe, 300),
        Badge("bomb", "Bomb", net.ljga.projects.games.tetris.R.drawable.ic_badge_bomb, 300),
        Badge("book", "Book", net.ljga.projects.games.tetris.R.drawable.ic_badge_book, 300),
        Badge("boots", "Boots", net.ljga.projects.games.tetris.R.drawable.ic_badge_boots, 300),
        Badge("bulb", "Bulb", net.ljga.projects.games.tetris.R.drawable.ic_badge_bulb, 300),
        Badge("cat", "Cat", net.ljga.projects.games.tetris.R.drawable.ic_badge_cat, 300),
        Badge("chest", "Chest", net.ljga.projects.games.tetris.R.drawable.ic_badge_chest, 500),
        Badge("coin", "Coin", net.ljga.projects.games.tetris.R.drawable.ic_badge_coin, 300),
        Badge("dog", "Dog", net.ljga.projects.games.tetris.R.drawable.ic_badge_dog, 300),
        Badge("feather", "Feather", net.ljga.projects.games.tetris.R.drawable.ic_badge_feather, 300),
        Badge("fire", "Fire", net.ljga.projects.games.tetris.R.drawable.ic_badge_fire, 300),
        Badge("gold", "Gold", net.ljga.projects.games.tetris.R.drawable.ic_badge_gold, 500),
        Badge("hat", "Hat", net.ljga.projects.games.tetris.R.drawable.ic_badge_hat, 300),
        Badge("heart", "Heart", net.ljga.projects.games.tetris.R.drawable.ic_badge_heart, 300),
        Badge("key", "Key", net.ljga.projects.games.tetris.R.drawable.ic_badge_key, 300),
        Badge("map", "Map", net.ljga.projects.games.tetris.R.drawable.ic_badge_map, 300),
        Badge("plant", "Plant", net.ljga.projects.games.tetris.R.drawable.ic_badge_plant, 300),
        Badge("potion", "Potion", net.ljga.projects.games.tetris.R.drawable.ic_badge_potion, 300),
        Badge("potion2", "Elixir", net.ljga.projects.games.tetris.R.drawable.ic_badge_potion2, 300),
        Badge("ring", "Ring", net.ljga.projects.games.tetris.R.drawable.ic_badge_ring, 300),
        Badge("shield", "Shield", net.ljga.projects.games.tetris.R.drawable.ic_badge_shield, 300),
        Badge("sunmoon", "Sun & Moon", net.ljga.projects.games.tetris.R.drawable.ic_badge_sunmoon, 500),
        Badge("sword", "Sword", net.ljga.projects.games.tetris.R.drawable.ic_badge_sword, 300),
        Badge("wand", "Wand", net.ljga.projects.games.tetris.R.drawable.ic_badge_wand, 300)
    )

    // Helper functions to reduce hook invocation duplication
    private inline fun <reified T> GameState.applyHook(transform: (T, GameState) -> GameState): GameState {
        var state = this
        selectedMutations.filterIsInstance<T>().forEach { state = transform(it, state) }
        artifacts.filterIsInstance<T>().forEach { state = transform(it, state) }
        return state
    }

    private inline fun <reified T> GameState.applyModifier(
        initial: Long,
        transform: (T, Long) -> Long
    ): Long {
        var result = initial
        selectedMutations.filterIsInstance<T>().forEach { result = transform(it, result) }
        artifacts.filterIsInstance<T>().forEach { result = transform(it, result) }
        return result
    }

    private val bosses = listOf(
        Boss("The Wall", 10),
        Boss("The Sprinter", 15)
    )

    fun newGame() {
        newGame(emptyList(), emptyList())
    }

    fun newGame(mutations: List<Mutation>, artifacts: List<Artifact>) {
        gameJob?.cancel()
        gameJob = null
        Log.d(TAG, "Starting new game...")
        viewModelScope.launch {
            val isDebugMode = mutations.isNotEmpty() || artifacts.isNotEmpty()
            val startingMutations = if (isDebugMode) mutations else {
            val startingMutations = if (isDebugMode) mutations else {
                val activeMutationNames = preferenceDataStore.enabledMutations.first()
                val activeMutations = allMutations.filter { activeMutationNames.contains(it.name) }
                if (activeMutations.isNotEmpty()) listOf(activeMutations.random()) else emptyList()
            }

            _gameState.value = GameState(createEmptyBoard(), null, pieces.random(), pieces.random(), 0, 0, emptyList(), 0, 1, 5, false, artifacts, startingMutations, null, pieces.shuffled(), 0, emptyList(), 0, isDebugMode, emptyList())
            applyStartingMutations()

            preferenceDataStore.clearSavedGame()
            runGame()
        }
    }

    private fun applyStartingMutations() {
        _gameState.value = _gameState.value.applyHook<IOnNewGameHook> { hook, state -> 
            hook.onNewGame(state) 
        }
    }

    fun pauseGame() {
        gameJob?.cancel()
        gameJob = null
        if (_gameState.value.piece != null) {
            viewModelScope.launch {
                preferenceDataStore.saveGameState(_gameState.value)
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

    private fun runGame() {
        if (gameJob?.isActive == true) return
        gameJob = viewModelScope.launch {
            if (_gameState.value.piece == null) {
                spawnNewPiece()
            }
            while (true) {
                val currentLevel = _gameState.value.level
                var delayMs = (500 - (currentLevel - 1) * 50).coerceAtLeast(100).toLong()
                delayMs = _gameState.value.applyModifier<ITickDelayModifier>(delayMs) { mod, delay ->
                    mod.modifyTickDelay(delay)
                }


                if (_gameState.value.currentBoss?.name == "The Sprinter") {
                    delayMs = (delayMs * 0.7).toLong()
                }
                delay(delayMs)
                if (!movePiece(0, 1)) {
                    Log.d(TAG, "Piece could not move down. Locking piece.")
                    lockPiece()
                    val linesCleared = clearLines()
                    viewModelScope.launch {
                        var state = _gameState.value
                        _gameState.value.selectedMutations.forEach { mutation ->
                            if (mutation is IOnLineClearHook) {
                                state = mutation.onLineClear(state, linesCleared)
                            }
                        }
                        _gameState.value.artifacts.forEach { artifact ->
                            if (artifact is IOnLineClearHook) {
                                state = artifact.onLineClear(state, linesCleared)
                            }
                        }
                        _gameState.value = state

                        // Animate falling fragments if any were added by artifacts
                        if (_gameState.value.fallingFragments.isNotEmpty()) {
                            animateFallingFragments()
                        }

                        updateScoreAndLevel(linesCleared)
                        if (linesCleared > 0) {
                            delay(300)
                            _gameState.value = _gameState.value.copy(clearingLines = emptyList())
                        }
                        if (!spawnNewPiece()) {
                            Log.d(TAG, "Game Over.")
                            endGame()
                        }
                    }.join()
                }
            }
        }
    }

    private fun endGame() {
        if (_gameState.value.currentScore > _highScore.value) {
            _highScore.value = _gameState.value.currentScore
            viewModelScope.launch {
                preferenceDataStore.updateHighScore(_gameState.value.currentScore)
            }
        }
        
        // Award coins based on score (1 coin per 100 points)
        val coinsEarned = _gameState.value.currentScore / 100
        if (coinsEarned > 0) {
            viewModelScope.launch {
                preferenceDataStore.addCoins(coinsEarned)
            }
        }

        _gameState.value = _gameState.value.copy(isGameOver = true)
        gameJob?.cancel()
        gameJob = null
        viewModelScope.launch { preferenceDataStore.clearSavedGame() }
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
                val newState = artifact.onRotate(_gameState.value)
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

        if (isValidPosition(newX, newY, newPiece)) {
            _gameState.value = _gameState.value.copy(piece = newPiece, pieceX = newX, pieceY = newY)
            updateGhostPiece()
        }
    }

    private fun movePiece(dx: Int, dy: Int): Boolean {
        val currentPiece = _gameState.value.piece ?: return false
        val newX = _gameState.value.pieceX + dx
        val newY = _gameState.value.pieceY + dy

        if (isValidPosition(newX, newY, currentPiece)) {
            _gameState.value = _gameState.value.copy(pieceX = newX, pieceY = newY)
            updateGhostPiece()
            return true
        }
        return false
    }

    private fun isValidPosition(x: Int, y: Int, piece: Piece): Boolean {
        var defaultResult = true
        for (py in piece.shape.indices) {
            for (px in piece.shape[py].indices) {
                if (piece.shape[py][px] != 0) {
                    val boardX = x + px
                    val boardY = y + py

                    if (boardX < 0 || boardX >= boardWidth || boardY < 0 || boardY >= boardHeight || _gameState.value.board[boardY][boardX] != 0) {
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

    private fun lockPiece() {
        val currentPiece = _gameState.value.piece ?: return
        val newBoard = _gameState.value.board.map { it.clone() }.toTypedArray()
        for (py in currentPiece.shape.indices) {
            for (px in currentPiece.shape[py].indices) {
                if (currentPiece.shape[py][px] != 0) {
                    val boardX = _gameState.value.pieceX + px
                    val boardY = _gameState.value.pieceY + py
                    if (boardY >= 0 && boardY < boardHeight && boardX >= 0 && boardX < boardWidth) {
                        newBoard[boardY][boardX] = if (_gameState.value.selectedMutations.any { it is ColorblindMutation }) 8 else currentPiece.color
                    }
                }
            }
        }
        _gameState.value = _gameState.value.copy(board = newBoard, piece = null, rotationCount = 0)
    }

    private suspend fun clearLines(): Int {
        val board = _gameState.value.board
        var linesToClear = board.indices.filter { y ->
            board[y].all { it != 0 }
        }.toMutableList()

        for (artifact in _gameState.value.artifacts) {
            if (artifact is IBeforeLineClearHook) {
                linesToClear = artifact.beforeLineClear(linesToClear, _gameState.value).toMutableList()
            }
        }

        val linesToClearList = linesToClear.toList()

        if (linesToClearList.isNotEmpty()) {
            _gameState.value = _gameState.value.copy(clearingLines = linesToClearList)
            delay(300)

            val newBoard = board.toMutableList()
            linesToClearList.sorted().reversed().forEach { newBoard.removeAt(it) }
            repeat(linesToClearList.size) { newBoard.add(0, IntArray(boardWidth)) }
            val finalBoard = newBoard.toTypedArray()

            _gameState.value = _gameState.value.copy(board = finalBoard)
        }
        return linesToClearList.size
    }

    private suspend fun updateScoreAndLevel(linesCleared: Int) {
        if (linesCleared > 0) {
            var points = when (linesCleared) {
                1 -> 100
                2 -> 300
                3 -> 600
                4 -> 1000
                else -> 0
            } * _gameState.value.level

            // Apply Mutation Modifiers
            val multiplier = _gameState.value.selectedMutations.map { it.scoreMultiplier }.fold(1f) { acc, m -> acc * m }
            points = (points * multiplier).toInt()

            for (artifact in _gameState.value.artifacts) {
                if (artifact is IScoreModifier) {
                    points = artifact.modifyScore(points, linesCleared, _gameState.value.level)
                }
            }

            val newScore = _gameState.value.currentScore + points

            var currentBoss = _gameState.value.currentBoss
            if (currentBoss != null && !_gameState.value.isDebugMode) {
                currentBoss.requiredLines -= linesCleared
                if (currentBoss.requiredLines <= 0) {
                    _gameState.value = _gameState.value.copy(currentScore = _gameState.value.currentScore + 5000 * _gameState.value.level)
                    // unlockNextMutation() - REMOVED for Badge Shop system
                    addRandomMutationToRun()
                    currentBoss = null
                }
            }

            var newLinesUntilNextLevel = _gameState.value.linesUntilNextLevel - linesCleared
            var newLevel = _gameState.value.level

            if (newLinesUntilNextLevel <= 0 && !_gameState.value.isDebugMode) {
                newLevel++
                newLinesUntilNextLevel += newLevel * 5
                var state = _gameState.value
                _gameState.value.selectedMutations.forEach { mutation ->
                    if (mutation is IOnLevelUpHook) {
                        state = mutation.onLevelUp(state)
                    }
                }
                _gameState.value.artifacts.forEach { artifact ->
                    if (artifact is IOnLevelUpHook) {
                        state = artifact.onLevelUp(state)
                    }
                }
                _gameState.value = state

                if (allArtifacts.size > _gameState.value.artifacts.size) {
                    val availableArtifacts = allArtifacts.filterNot { _gameState.value.artifacts.contains(it) }
                    if (availableArtifacts.size >= 2) {
                        val choices = availableArtifacts.shuffled().take(2)
                        _gameState.value = _gameState.value.copy(artifactChoices = choices)
                        pauseGame()
                    }
                }
            }

            if (newLevel % 3 == 0 && currentBoss == null && !_gameState.value.isDebugMode) {
                currentBoss = spawnBoss(newLevel)
            }

            _gameState.value = _gameState.value.copy(
                currentScore = newScore,
                level = newLevel,
                linesUntilNextLevel = newLinesUntilNextLevel,
                currentBoss = currentBoss
            )
        }
    }

    fun selectArtifact(artifact: Artifact) {
        viewModelScope.launch {
            _gameState.value = _gameState.value.copy(
                artifacts = _gameState.value.artifacts + artifact,
                artifactChoices = emptyList(),
                pendingMutationPopup = artifact
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
            preferenceDataStore.purchaseBadge(badgeName, cost)
        }
    }

    fun purchaseMutation(mutationName: String, cost: Int) {
        viewModelScope.launch {
            if (preferenceDataStore.purchaseMutation(mutationName, cost)) {
                // Update local list if needed, though flows handle it
                val enabled = preferenceDataStore.enabledMutations.first()
                _mutations.value = allMutations.filter { enabled.contains(it.name) }
            }
        }
    }

    fun toggleMutation(mutationName: String, enabled: Boolean) {
        viewModelScope.launch {
            preferenceDataStore.setMutationEnabled(mutationName, enabled)
            val updatedEnabled = preferenceDataStore.enabledMutations.first()
            _mutations.value = allMutations.filter { updatedEnabled.contains(it.name) }
        }
    }

    private fun spawnBoss(level: Int): Boss {
        val boss = bosses.random().copy()
        boss.requiredLines = level * 2
        return boss
    }

    // private suspend fun unlockNextMutation() - REMOVED

    private suspend fun addRandomMutationToRun() {
        val activeMutationNames = preferenceDataStore.enabledMutations.first()
        val availableMutations = allMutations.filter { activeMutationNames.contains(it.name) && !_gameState.value.selectedMutations.contains(it) }
        if (availableMutations.isNotEmpty()) {
            val newMutation = availableMutations.random()
            _gameState.value = _gameState.value.copy(selectedMutations = _gameState.value.selectedMutations + newMutation)
        }
    }

    private suspend fun spawnNewPiece(): Boolean {
        var state = _gameState.value

        val pieceToSpawn = state.nextPiece ?: pieces.random()
        val nextPiece = state.secondNextPiece ?: pieces.random()
        val secondNextPiece = pieces.random()

        val startX = boardWidth / 2 - pieceToSpawn.shape[0].size / 2
        val startY = 0

        state = state.copy(
            piece = pieceToSpawn,
            nextPiece = nextPiece,
            secondNextPiece = secondNextPiece,
            pieceX = startX,
            pieceY = startY
        )

        _gameState.value.selectedMutations.forEach { mutation ->
            if (mutation is IOnPieceSpawnHook) {
                state = mutation.onPieceSpawn(state)
            }
        }
        _gameState.value.artifacts.forEach { artifact ->
            if (artifact is IOnPieceSpawnHook) {
                state = artifact.onPieceSpawn(state)
            }
        }

        if (!isValidPosition(state.pieceX, state.pieceY, state.piece!!)) {
            return false // Game Over
        }

        _gameState.value = state
        updateGhostPiece()
        return true
    }

    private fun updateGhostPiece() {
        val state = _gameState.value
        var ghostY = state.pieceY

        if (_gameState.value.selectedMutations.any { it is IRequiresGhostPiece } || _gameState.value.artifacts.any { it is IRequiresGhostPiece }) {
            state.piece?.let {
                while (isValidPosition(state.pieceX, ghostY + 1, it)) {
                    ghostY++
                }
            }
        }
        _gameState.value = state.copy(ghostPieceY = ghostY)
    }

    private fun createEmptyBoard(): Array<IntArray> = Array(boardHeight) { IntArray(boardWidth) }

    private suspend fun animateFallingFragments() {
        val fragmentAnimationJob = viewModelScope.launch {
            var currentFragments = _gameState.value.fallingFragments.toMutableList()
            if (currentFragments.isEmpty()) return@launch

            for (i in 0 until boardHeight) {
                delay(50)
                val nextFragments = mutableListOf<Pair<Int, Int>>()
                var fragmentsLanded = false
                currentFragments.forEach { (x, y) ->
                    val nextY = y + 1
                    if (nextY < boardHeight && _gameState.value.board[nextY][x] == 0) {
                        nextFragments.add(Pair(x, nextY))
                    } else {
                        nextFragments.add(Pair(x, y))
                        fragmentsLanded = true
                    }
                }
                currentFragments = nextFragments.toMutableList()
                _gameState.value = _gameState.value.copy(fallingFragments = currentFragments)
                if (fragmentsLanded) {
                    val finalBoard = _gameState.value.board.map { it.clone() }.toTypedArray()
                    currentFragments.forEach { (x, y) ->
                        if (y >= 0 && y < boardHeight && x >= 0 && x < boardWidth) {
                            finalBoard[y][x] = 8 // Color for falling fragments
                        }
                    }
                    _gameState.value = _gameState.value.copy(board = finalBoard, fallingFragments = emptyList())
                    return@launch
                }
            }
        }
        fragmentAnimationJob.join()
    }
}

class GameViewModelFactory(private val preferenceDataStore: PreferenceDataStore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(preferenceDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
