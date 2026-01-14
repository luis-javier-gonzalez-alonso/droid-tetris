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

private const val TAG = "GameViewModel"

class GameViewModel(private val preferenceDataStore: PreferenceDataStore) : ViewModel() {

    val boardWidth = 10
    val boardHeight = 20

    private val _gameState = MutableStateFlow(GameState(createEmptyBoard(), null, null, 0, 0, emptyList(), 0))
    val gameState: StateFlow<GameState> = _gameState

    private var _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore

    private var gameJob: Job? = null

    init {
        viewModelScope.launch {
            _highScore.value = preferenceDataStore.highScore.first()
            preferenceDataStore.gameState.first()?.let {
                _gameState.value = it
            }
        }
    }

    data class GameState(
        val board: Array<IntArray>,
        val piece: Piece?,
        val nextPiece: Piece?,
        val pieceX: Int,
        val pieceY: Int,
        val clearingLines: List<Int>,
        val currentScore: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as GameState

            if (!board.contentDeepEquals(other.board)) return false
            if (piece != other.piece) return false
            if (nextPiece != other.nextPiece) return false
            if (pieceX != other.pieceX) return false
            if (pieceY != other.pieceY) return false
            if (clearingLines != other.clearingLines) return false
            if (currentScore != other.currentScore) return false

            return true
        }

        override fun hashCode(): Int {
            var result = board.contentDeepHashCode()
            result = 31 * result + (piece?.hashCode() ?: 0)
            result = 31 * result + (nextPiece?.hashCode() ?: 0)
            result = 31 * result + pieceX
            result = 31 * result + pieceY
            result = 31 * result + clearingLines.hashCode()
            result = 31 * result + currentScore
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
        _gameState.value = GameState(createEmptyBoard(), null, pieces.random(), 0, 0, emptyList(), 0)
        viewModelScope.launch { preferenceDataStore.clearGameState() }
        runGame()
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
                delay(500)
                if (!movePiece(0, 1)) {
                    Log.d(TAG, "Piece could not move down. Locking piece.")
                    lockPiece()
                    val linesCleared = clearLines()
                    updateScore(linesCleared)
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
        val linesToClear = board.indices.filter { y -> board[y].all { it != 0 } }

        if (linesToClear.isNotEmpty()) {
            _gameState.value = _gameState.value.copy(clearingLines = linesToClear)
            delay(300)

            val newBoard = board.toMutableList()
            linesToClear.reversed().forEach { newBoard.removeAt(it) }
            repeat(linesToClear.size) { newBoard.add(0, IntArray(boardWidth)) }
            _gameState.value = _gameState.value.copy(board = newBoard.toTypedArray())
        }
        return linesToClear.size
    }

    private fun updateScore(linesCleared: Int) {
        if (linesCleared > 0) {
            val points = when (linesCleared) {
                1 -> 100
                2 -> 300
                3 -> 600
                4 -> 1000
                else -> 0
            }
            val newScore = _gameState.value.currentScore + points
            _gameState.value = _gameState.value.copy(currentScore = newScore)
        }
    }

    private fun spawnNewPiece(): Boolean {
        val newPiece = _gameState.value.nextPiece ?: pieces.random()
        val startX = boardWidth / 2 - newPiece.shape[0].size / 2
        val startY = 0

        if (!isValidPosition(startX, startY, newPiece)) {
            return false // Game Over
        }

        _gameState.value = _gameState.value.copy(
            piece = newPiece,
            nextPiece = pieces.random(),
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
