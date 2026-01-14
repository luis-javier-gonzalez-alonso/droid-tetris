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

    private val _gameState = MutableStateFlow(GameState(createEmptyBoard(), null, null, null, 0, 0, emptyList(), 0, 1, 5, false, emptyList(), emptyList(), null, listOf(), 0, emptyList()))
    val gameState: StateFlow<GameState> = _gameState

    private var _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore

    private var _mutations = MutableStateFlow<List<Mutation>>(emptyList())
    val mutations: StateFlow<List<Mutation>> = _mutations

    private var gameJob: Job? = null

    init {
        viewModelScope.launch {
            _highScore.value = preferenceDataStore.highScore.first()
            val unlockedMutationNames = preferenceDataStore.unlockedMutations.first()
            _mutations.value = allMutations.filter { unlockedMutationNames.contains(it.name) }

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
        val artifactChoices: List<Artifact>
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
        Mutation("Unyielding", "Start with one line of garbage blocks, and a new one for each level"),
        Mutation("Feather Fall", "Pieces fall 20% slower"),
        Mutation("Clairvoyance", "See the next two pieces instead of one"),
        Mutation("Lead Fall", "Pieces fall 25% faster"),
        Mutation("Colorblind", "All pieces are the same color"),
        Mutation("More 'I's", "Increases the frequency of 'I' pieces"),
        Mutation("Garbage Collector", "Spawning a piece has a chance to add a garbage block"),
        Mutation("Time Warp", "Clearing a line has a 10% chance to freeze the game for 2 seconds"),
        Mutation("Fair Play", "All 7 unique pieces will spawn before any are repeated"),
        Mutation("Phantom Piece", "Shows a ghost of where the current piece will land")
    )

    private val allArtifacts = listOf(
        Artifact("Swiftness Charm", "Increases piece drop speed by 10%"),
        Artifact("Line Clearer", "Clears an extra line randomly"),
        Artifact("Score Multiplier", "Multiplies score from line clears by 1.5x"),
        Artifact("Spring-loaded Rotator", "Rotating moves the piece up one space"),
        Artifact("Chaos Orb", "Rotating changes the piece type"),
        Artifact("Falling Fragments", "When completing 2 or 4 lines, 2 single-square pieces drop from the top in random positions."),
        Artifact("Repulsor", "Pieces are repelled from the walls"),
        Artifact("Inverted Rotation", "Inverts the rotation direction of pieces"),
        Artifact("Piece Swapper", "Swap the current piece with the next piece"),
        Artifact("Board Shrinker", "Reduces the board width by 2 columns")
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

            _gameState.value = GameState(createEmptyBoard(), null, pieces.random(), pieces.random(), 0, 0, emptyList(), 0, 1, 5, false, emptyList(), startingMutation, null, pieces.shuffled(), 0, emptyList())
            applyStartingMutations()

            preferenceDataStore.clearSavedGame()
            runGame()
        }
    }

    private fun applyStartingMutations() {
        val state = _gameState.value
        if (state.selectedMutations.any { it.name == "Unyielding" }) {
            addGarbageLine()
        }
    }

    private fun addGarbageLine() {
        val state = _gameState.value
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
                if (_gameState.value.artifacts.any { it.name == "Swiftness Charm" }) {
                    delayMs = (delayMs * 0.9).toLong()
                }
                if (_gameState.value.selectedMutations.any { it.name == "Feather Fall" }) {
                    delayMs = (delayMs * 1.2).toLong()
                }
                if (_gameState.value.selectedMutations.any { it.name == "Lead Fall" }) {
                    delayMs = (delayMs * 0.75).toLong()
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
                        if (linesCleared > 0 && _gameState.value.selectedMutations.any { it.name == "Time Warp" }) {
                            if (Random().nextInt(10) == 0) {
                                delay(2000)
                            }
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
        _gameState.value = _gameState.value.copy(isGameOver = true)
        gameJob?.cancel()
        gameJob = null
        viewModelScope.launch { preferenceDataStore.clearSavedGame() }
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

        if (_gameState.value.artifacts.any { it.name == "Chaos Orb" }) {
            val newPiece = pieces.random()
            if (isValidPosition(_gameState.value.pieceX, _gameState.value.pieceY, newPiece)) {
                _gameState.value = _gameState.value.copy(piece = newPiece)
                updateGhostPiece()
            }
            return
        }

        val rotatedShape = Array(currentPiece.shape[0].size) { IntArray(currentPiece.shape.size) }
        if (_gameState.value.artifacts.any { it.name == "Inverted Rotation" }) {
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
        var newY = _gameState.value.pieceY
        if (_gameState.value.artifacts.any { it.name == "Spring-loaded Rotator" }) {
            newY -= 1
        }

        if (isValidPosition(_gameState.value.pieceX, newY, newPiece)) {
            _gameState.value = _gameState.value.copy(piece = newPiece, pieceY = newY)
            updateGhostPiece()
        }
    }

    private fun movePiece(dx: Int, dy: Int): Boolean {
        val currentPiece = _gameState.value.piece ?: return false
        var newX = _gameState.value.pieceX + dx
        val newY = _gameState.value.pieceY + dy

        if (_gameState.value.artifacts.any { it.name == "Repulsor" }) {
            if (newX < 0) newX = 0
            if (newX + currentPiece.shape[0].size > boardWidth) newX = boardWidth - currentPiece.shape[0].size
        }

        if (isValidPosition(newX, newY, currentPiece)) {
            _gameState.value = _gameState.value.copy(pieceX = newX, pieceY = newY)
            updateGhostPiece()
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
                        newBoard[boardY][boardX] = if (_gameState.value.selectedMutations.any { it.name == "Colorblind" }) 8 else currentPiece.color
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

        if (_gameState.value.artifacts.any { it.name == "Falling Fragments" } && (linesToClear.size == 2 || linesToClear.size == 4)) {
            dropFragments()
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

    private fun dropFragments() {
        val newBoard = _gameState.value.board.map { it.clone() }.toTypedArray()
        val random = Random()
        repeat(2) {
            val x = random.nextInt(boardWidth)
            var y = 0
            while (y + 1 < boardHeight && newBoard[y + 1][x] == 0) {
                y++
            }
            if (y < boardHeight) {
                newBoard[y][x] = 8 // Garbage block color
            }
        }
        _gameState.value = _gameState.value.copy(board = newBoard)
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

            if (newLinesUntilNextLevel <= 0) {
                newLevel++
                newLinesUntilNextLevel += newLevel * 5
                if (_gameState.value.selectedMutations.any { it.name == "Unyielding" }) {
                    addGarbageLine()
                }
                if (allArtifacts.size > _gameState.value.artifacts.size) {
                    val availableArtifacts = allArtifacts.filterNot { _gameState.value.artifacts.contains(it) }
                    if (availableArtifacts.size >= 2) {
                        val choices = availableArtifacts.shuffled().take(2)
                        _gameState.value = _gameState.value.copy(artifactChoices = choices)
                        pauseGame()
                    }
                }
            }

            if (newLevel % 3 == 0 && currentBoss == null && _gameState.value.level % 3 != 0) {
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
                artifactChoices = emptyList()
            )
            continueGame()
        }
    }

    private fun spawnBoss(level: Int): Boss {
        val boss = bosses.random().copy()
        boss.requiredLines = level * 2
        return boss
    }

    private suspend fun unlockNextMutation() {
        val unlockedNames = preferenceDataStore.unlockedMutations.first()
        val availableToUnlock = allMutations.filter { !unlockedNames.contains(it.name) }
        if (availableToUnlock.isNotEmpty()) {
            val nextMutation = availableToUnlock.random()
            preferenceDataStore.unlockMutation(nextMutation.name)
            _mutations.value = _mutations.value + nextMutation
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
        var state = _gameState.value
        var newPieceQueue = state.pieceQueue

        var pieceToSpawn = if (state.selectedMutations.any { it.name == "Fair Play" }) {
            if (newPieceQueue.isEmpty()) {
                newPieceQueue = pieces.shuffled()
            }
            newPieceQueue.first()
        } else if (state.selectedMutations.any { it.name == "More 'I's" } && Random().nextInt(5) == 0) {
            pieces.first()
        } else {
            state.nextPiece ?: pieces.random()
        }

        newPieceQueue = if (newPieceQueue.isNotEmpty()) newPieceQueue.drop(1) else newPieceQueue

        var nextPiece = if (state.selectedMutations.any { it.name == "Fair Play" }) {
            if (newPieceQueue.isEmpty()) {
                newPieceQueue = pieces.shuffled()
            }
            newPieceQueue.first()
        } else {
            state.secondNextPiece ?: pieces.random()
        }

        val secondNextPiece = if (state.selectedMutations.any { it.name == "Fair Play" }) {
            if (newPieceQueue.size < 2) {
                newPieceQueue = newPieceQueue + pieces.shuffled()
            }
            newPieceQueue[1]
        } else {
            pieces.random()
        }

        if (_gameState.value.artifacts.any { it.name == "Piece Swapper" }) {
            val temp = pieceToSpawn
            pieceToSpawn = nextPiece
            nextPiece = temp
        }

        val startX = if (_gameState.value.artifacts.any { it.name == "Board Shrinker" }) {
            (boardWidth - 2) / 2 - pieceToSpawn.shape[0].size / 2
        } else {
            boardWidth / 2 - pieceToSpawn.shape[0].size / 2
        }
        val startY = 0

        if (!isValidPosition(startX, startY, pieceToSpawn)) {
            return false // Game Over
        }

        if (state.selectedMutations.any { it.name == "Garbage Collector" } && Random().nextInt(10) == 0) {
            val newBoard = state.board.clone()
            val randomX = Random().nextInt(boardWidth)
            newBoard[boardHeight - 1][randomX] = 8 // Garbage block color
            state = state.copy(board = newBoard)
        }

        _gameState.value = state.copy(
            piece = pieceToSpawn,
            nextPiece = nextPiece,
            secondNextPiece = secondNextPiece,
            pieceX = startX,
            pieceY = startY,
            pieceQueue = newPieceQueue
        )
        updateGhostPiece()
        return true
    }

    private fun updateGhostPiece() {
        val state = _gameState.value
        if (state.selectedMutations.any { it.name == "Phantom Piece" }) {
            var ghostY = state.pieceY
            state.piece?.let {
                while (isValidPosition(state.pieceX, ghostY + 1, it)) {
                    ghostY++
                }
                _gameState.value = state.copy(ghostPieceY = ghostY)
            }
        }
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
