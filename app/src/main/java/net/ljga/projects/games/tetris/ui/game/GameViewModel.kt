package net.ljga.projects.games.tetris.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    val boardWidth = 10
    val boardHeight = 20

    private val _gameState = MutableStateFlow(GameState(createEmptyBoard(), null, 0, 0))
    val gameState: StateFlow<GameState> = _gameState

    private var gameJob: Job? = null

    data class GameState(val board: Array<IntArray>, val piece: Piece?, val pieceX: Int, val pieceY: Int)
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
        if (gameJob == null) {
            gameJob = viewModelScope.launch {
                spawnNewPiece()
                while (true) {
                    delay(500)
                    if (!movePiece(0, 1)) {
                        lockPiece()
                        clearLines()
                        spawnNewPiece()
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
        val piece = _gameState.value.piece ?: return
        val rotatedShape = Array(piece.shape[0].size) { IntArray(piece.shape.size) }
        for (y in piece.shape.indices) {
            for (x in piece.shape[y].indices) {
                rotatedShape[x][piece.shape.size - 1 - y] = piece.shape[y][x]
            }
        }

        if (isValidPosition(_gameState.value.pieceX, _gameState.value.pieceY, Piece(rotatedShape, piece.color))) {
            _gameState.value = _gameState.value.copy(piece = Piece(rotatedShape, piece.color))
        }
    }

    private fun movePiece(dx: Int, dy: Int): Boolean {
        val piece = _gameState.value.piece ?: return false
        val newX = _gameState.value.pieceX + dx
        val newY = _gameState.value.pieceY + dy

        if (isValidPosition(newX, newY, piece)) {
            _gameState.value = _gameState.value.copy(pieceX = newX, pieceY = newY)
            return true
        }
        return false
    }

    private fun isValidPosition(x: Int, y: Int, piece: Piece): Boolean {
        for (py in piece.shape.indices) {
            for (px in piece.shape[py].indices) {
                if (piece.shape[py][px] != 0) {
                    val newX = x + px
                    val newY = y + py
                    if (newX < 0 || newX >= boardWidth || newY < 0 || newY >= boardHeight || _gameState.value.board[newY][newX] != 0) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun lockPiece() {
        val piece = _gameState.value.piece ?: return
        val newBoard = _gameState.value.board.map { it.clone() }.toTypedArray()
        for (py in piece.shape.indices) {
            for (px in piece.shape[py].indices) {
                if (piece.shape[py][px] != 0) {
                    newBoard[_gameState.value.pieceY + py][_gameState.value.pieceX + px] = piece.color
                }
            }
        }
        _gameState.value = _gameState.value.copy(board = newBoard)
    }

    private fun clearLines() {
        val newBoard = _gameState.value.board.toMutableList()
        var linesCleared = 0
        val iterator = newBoard.iterator()
        while(iterator.hasNext()){
            val row = iterator.next()
            if (row.all { it != 0 }) {
                iterator.remove()
                linesCleared++
            }
        }

        for (i in 0 until linesCleared) {
            newBoard.add(0, IntArray(boardWidth))
        }

        if (linesCleared > 0) {
            _gameState.value = _gameState.value.copy(board = newBoard.toTypedArray())
        }
    }

    private fun spawnNewPiece() {
        val newPiece = pieces.random()
        val newX = boardWidth / 2 - newPiece.shape[0].size / 2
        val newY = 0

        if (!isValidPosition(newX, newY, newPiece)) {
            // Game Over
            gameJob?.cancel()
            gameJob = null
            _gameState.value = GameState(createEmptyBoard(), null, 0, 0) // Reset state
        } else {
            _gameState.value = _gameState.value.copy(piece = newPiece, pieceX = newX, pieceY = newY)
        }
    }

    private fun createEmptyBoard(): Array<IntArray> = Array(boardHeight) { IntArray(boardWidth) }
}
