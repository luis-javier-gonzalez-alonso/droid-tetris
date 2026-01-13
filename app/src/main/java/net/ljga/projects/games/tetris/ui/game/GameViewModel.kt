package net.ljga.projects.games.tetris.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

private const val TAG = "GameViewModel"

class GameViewModel : ViewModel() {

    val boardWidth = 10
    val boardHeight = 20

    private val _gameState = MutableStateFlow(GameState(createEmptyBoard(), null, null, 0, 0, emptyList()))
    val gameState: StateFlow<GameState> = _gameState

    private var _currentScore = MutableStateFlow(0)
    val currentScore: StateFlow<Int> = _currentScore

    private var _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore

    private var gameJob: Job? = null

    data class GameState(val board: Array<IntArray>, val piece: Piece?, val nextPiece: Piece?, val pieceX: Int, val pieceY: Int, val clearingLines: List<Int>)
    data class Piece(val shape: Array<IntArray>, val color: Int)

    private val pieces = listOf(
        Piece(arrayOf(intArrayOf(1, 1, 1, 1)), 1), // I
        Piece(arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)), 2), // O
        Piece(arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1)), 3), // T
        Piece(arrayOf(intArrayOf(0, 0, 1), intArrayOf(1, 1, 1)), 4), // L
        Piece(arrayOf(intArrayOf(1, 0, 0), intArrayOf(1, 1, 1)), 5), // J
        Piece(arrayOf(intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)), 6), // S
        Piece(arrayOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)), 7)  // Z
    )

    fun startGame() {
        if (gameJob == null || gameJob?.isActive == false) {
            Log.d(TAG, "Starting new game...")
            _gameState.value = GameState(createEmptyBoard(), null, pieces.random(), 0, 0, emptyList())
            _currentScore.value = 0 // Reset score
            gameJob = viewModelScope.launch {
                spawnNewPiece()
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
                            Log.d(TAG, "Game Over. Resetting.")
                            if (_currentScore.value > _highScore.value) {
                                _highScore.value = _currentScore.value
                            }
                            gameJob?.cancel()
                            gameJob = null
                            break
                        }
                    }
                }
            }
        }
    }

    fun moveLeft() {
        movePiece(-1, 0)
    }

    fun moveRight() {
        movePiece(1, 0)
    }

    fun rotate() {
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
            _currentScore.value += points
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
