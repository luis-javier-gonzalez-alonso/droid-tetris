package net.ljga.projects.games.tetris.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.util.Log
import java.util.Random

private const val TAG = "GameViewModel"

class GameViewModel(private val preferenceDataStore: PreferenceDataStore) : ViewModel() {

    val boardWidth = 10
    val boardHeight = 20

    private val _gameState = MutableStateFlow(GameState(createEmptyBoard(), null, null, null, 0, 0, emptyList(), 0, 1, 5, false, emptyList(), emptyList(), null))
    val gameState: StateFlow<GameState> = _gameState

    private var _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore

    private var gameJob: Job? = null

    init {
        viewModelScope.launch {
            _highScore.value = preferenceDataStore.highScore.first()
            val unlockedMutationNames = preferenceDataStore.unlockedMutations.first()
            val unlockedMutations = allMutations.filter { unlockedMutationNames.contains(it.name) }

            preferenceDataStore.gameState.first()?.let {
                _gameState.value = it.copy(selectedMutations = unlockedMutations)
            } ?: run {
                _gameState.value = _gameState.value.copy(selectedMutations = unlockedMutations)
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
        val currentBoss: Boss?
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

    data class Artifact(val name: String, val description: String)

    data class Mutation(val name: String, val description: String)

    data class Boss(val name: String, var requiredLines: Int)

    val allMutations = listOf(
        Mutation("Unyielding", "Start with one line of garbage blocks"),
        Mutation("Feather Fall", "Pieces fall 20% slower"),
        Mutation("Clairvoyance", "See the next two pieces instead of one")
    )

    private val allArtifacts = listOf(
        Artifact("Swiftness Charm", "Increases piece drop speed by 10%"),
        Artifact("Line Clearer", "Clears an extra line randomly"),
        Artifact("Score Multiplier", "Multiplies score from line clears by 1.5x")
    )

    private val bosses = listOf(
        Boss("The Wall", 10),
        Boss("The Sprinter", 15)
    )

    private val pieces = listOf(
        Piece(arrayOf(intArrayOf(1, 1, 1, 1)), 1), // I
        Piece(arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)), 2), // O
        Piece(arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1)), 3), // T
        Piece(arrayOf(intArrayOf(0, 0, 1), intArrayOf(1, 1, 1)), 4), // L
        Piece(arrayOf(intArrayOf(1, 0, 0), intArrayOf(1, 1, 1)), 5), // J
        Piece(arrayOf(intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)), 6), // S
        Piece(arrayOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)), 7)  // Z
    )

    fun newGame() {
        gameJob?.cancel()
        gameJob = null
        Log.d(TAG, "Starting new game...")
        viewModelScope.launch {
            val unlockedMutationNames = preferenceDataStore.unlockedMutations.first()
            val unlockedMutations = allMutations.filter { unlockedMutationNames.contains(it.name) }

            val startingMutation = if (unlockedMutations.isNotEmpty()) listOf(unlockedMutations.random()) else emptyList()

            _gameState.value = GameState(createEmptyBoard(), null, pieces.random(), pieces.random(), 0, 0, emptyList(), 0, 1, 5, false, emptyList(), startingMutation, null)
            applyStartingMutations()

            preferenceDataStore.clearGameState()
            runGame()
        }
    }

    private fun applyStartingMutations() {
        val state = _gameState.value
        if (state.selectedMutations.any { it.name == "Unyielding" }) {
            val newBoard = state.board.clone()
            for (i in 0 until boardWidth) {
                newBoard[boardHeight - 1][i] = 8 // Garbage block color
            }
            val random = Random()
            val indicesToRemove = (0 until boardWidth).shuffled(random).take(3)
            for (i in indicesToRemove) {
                newBoard[boardHeight - 1][i] = 0
            }
            _gameState.value = state.copy(board = newBoard)
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
        if (gameJob?.isActive != true && _gameState.value.piece != null) {
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
                if (_gameState.value.artifacts.any { it.name == "Swiftness Charm" }) {
                    delayMs = (delayMs * 0.9).toLong()
                }
                if (_gameState.value.selectedMutations.any { it.name == "Feather Fall" }) {
                    delayMs = (delayMs * 1.2).toLong()
                }
                if (_gameState.value.currentBoss?.name == "The Sprinter") {
                    delayMs = (delayMs * 0.7).toLong()
                }
                delay(delayMs)
                if (!movePiece(0, 1)) {
                    Log.d(TAG, "Piece could not move down. Locking piece.")
                    lockPiece()
                    val linesCleared = clearLines()
                    updateScoreAndLevel(linesCleared)
                    if (linesCleared > 0) {
                        delay(300)
                        _gameState.value = _gameState.value.copy(clearingLines = emptyList())
                    }
                    if (!spawnNewPiece()) {
                        Log.d(TAG, "Game Over.")
                        endGame()
                        break
                    }
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
        _gameState.value = _gameState.value.copy(isGameOver = true)
        gameJob?.cancel()
        gameJob = null
        viewModelScope.launch { preferenceDataStore.clearGameState() }
    }

    fun moveLeft() {
        if (gameJob?.isActive != true) return
        movePiece(-1, 0)
    }

    fun moveRight() {
        if (gameJob?.isActive != true) return
        movePiece(1, 0)
    }

    fun moveDown() {
        if (gameJob?.isActive != true) return
        movePiece(0, 1)
    }

    fun rotate() {
        if (gameJob?.isActive != true) return
        val currentPiece = _gameState.value.piece ?: return
        val rotatedShape = Array(currentPiece.shape[0].size) { IntArray(currentPiece.shape.size) }
        for (y in currentPiece.shape.indices) {
            for (x in currentPiece.shape[y].indices) {
                rotatedShape[x][currentPiece.shape.size - 1 - y] = currentPiece.shape[y][x]
            }
        }
        val newPiece = Piece(rotatedShape, currentPiece.color)
        if (isValidPosition(_gameState.value.pieceX, _gameState.value.pieceY, newPiece)) {
            _gameState.value = _gameState.value.copy(piece = newPiece)
        }
    }

    private fun movePiece(dx: Int, dy: Int): Boolean {
        val currentPiece = _gameState.value.piece ?: return false
        val newX = _gameState.value.pieceX + dx
        val newY = _gameState.value.pieceY + dy

        if (isValidPosition(newX, newY, currentPiece)) {
            _gameState.value = _gameState.value.copy(pieceX = newX, pieceY = newY)
            return true
        }
        return false
    }

    private fun isValidPosition(x: Int, y: Int, piece: Piece): Boolean {
        for (py in piece.shape.indices) {
            for (px in piece.shape[py].indices) {
                if (piece.shape[py][px] != 0) {
                    val boardX = x + px
                    val boardY = y + py
                    if (boardX < 0 || boardX >= boardWidth || boardY < 0 || boardY >= boardHeight || _gameState.value.board[boardY][boardX] != 0) {
                        return false
                    }
                }
            }
        }
        return true
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
                        newBoard[boardY][boardX] = currentPiece.color
                    }
                }
            }
        }
        _gameState.value = _gameState.value.copy(board = newBoard, piece = null)
    }

    private suspend fun clearLines(): Int {
        val board = _gameState.value.board
        var linesToClear = board.indices.filter { y -> board[y].all { it != 0 } }

        if (_gameState.value.artifacts.any { it.name == "Line Clearer" } && linesToClear.isNotEmpty()) {
            val randomLine = (0 until boardHeight).random()
            if (!linesToClear.contains(randomLine)) {
                linesToClear = linesToClear + randomLine
            }
        }

        if (linesToClear.isNotEmpty()) {
            _gameState.value = _gameState.value.copy(clearingLines = linesToClear)
            delay(300)

            val newBoard = board.toMutableList()
            linesToClear.sorted().reversed().forEach { newBoard.removeAt(it) }
            repeat(linesToClear.size) { newBoard.add(0, IntArray(boardWidth)) }
            _gameState.value = _gameState.value.copy(board = newBoard.toTypedArray())
        }
        return linesToClear.size
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

            if (_gameState.value.artifacts.any { it.name == "Score Multiplier" }) {
                points = (points * 1.5).toInt()
            }

            val newScore = _gameState.value.currentScore + points

            var currentBoss = _gameState.value.currentBoss
            if (currentBoss != null) {
                currentBoss.requiredLines -= linesCleared
                if (currentBoss.requiredLines <= 0) {
                    _gameState.value = _gameState.value.copy(currentScore = _gameState.value.currentScore + 5000 * _gameState.value.level)
                    unlockNextMutation()
                    addRandomMutationToRun()
                    currentBoss = null
                }
            }

            var newLinesUntilNextLevel = _gameState.value.linesUntilNextLevel - linesCleared
            var newLevel = _gameState.value.level
            var newArtifacts = _gameState.value.artifacts
            if (newLinesUntilNextLevel <= 0) {
                newLevel++
                newLinesUntilNextLevel += newLevel * 5
                if (allArtifacts.size > newArtifacts.size) {
                    val availableArtifacts = allArtifacts.filterNot { newArtifacts.contains(it) }
                    newArtifacts = newArtifacts + availableArtifacts.random()
                }
            }

            if (newLevel % 3 == 0 && currentBoss == null && _gameState.value.level % 3 != 0) {
                currentBoss = spawnBoss(newLevel)
            }

            _gameState.value = _gameState.value.copy(
                currentScore = newScore,
                level = newLevel,
                linesUntilNextLevel = newLinesUntilNextLevel,
                artifacts = newArtifacts,
                currentBoss = currentBoss
            )
        }
    }

    private fun spawnBoss(level: Int): Boss {
        val boss = bosses.random().copy()
        boss.requiredLines = level * 2
        return boss
    }

    private suspend fun unlockNextMutation() {
        val unlockedNames = preferenceDataStore.unlockedMutations.first()
        val nextMutation = allMutations.firstOrNull { !unlockedNames.contains(it.name) }
        if (nextMutation != null) {
            preferenceDataStore.unlockMutation(nextMutation.name)
        }
    }

    private suspend fun addRandomMutationToRun() {
        val unlockedMutationNames = preferenceDataStore.unlockedMutations.first()
        val availableMutations = allMutations.filter { unlockedMutationNames.contains(it.name) && !_gameState.value.selectedMutations.contains(it) }
        if (availableMutations.isNotEmpty()) {
            val newMutation = availableMutations.random()
            _gameState.value = _gameState.value.copy(selectedMutations = _gameState.value.selectedMutations + newMutation)
        }
    }

    private fun spawnNewPiece(): Boolean {
        val state = _gameState.value
        val pieceToSpawn = state.nextPiece ?: pieces.random()
        val nextPiece = state.secondNextPiece ?: pieces.random()
        val secondNextPiece = pieces.random()

        val startX = boardWidth / 2 - pieceToSpawn.shape[0].size / 2
        val startY = 0

        if (!isValidPosition(startX, startY, pieceToSpawn)) {
            return false // Game Over
        }

        _gameState.value = state.copy(
            piece = pieceToSpawn,
            nextPiece = nextPiece,
            secondNextPiece = secondNextPiece,
            pieceX = startX,
            pieceY = startY
        )
        return true
    }

    private fun createEmptyBoard(): Array<IntArray> = Array(boardHeight) { IntArray(boardWidth) }
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
