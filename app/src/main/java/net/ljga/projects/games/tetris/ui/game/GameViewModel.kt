package net.ljga.projects.games.tetris.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class GameViewModel : ViewModel() {

    val boardWidth = 10
    val boardHeight = 20

    private val _gameState = MutableStateFlow(GameState(createEmptyBoard()))
    val gameState: StateFlow<GameState> = _gameState

    private var gameJob: Job?

    data class GameState(val board: Array<IntArray>)
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

    private var currentPiece: Piece = pieces.random()
    private var currentX = 0
    private var currentY = 0

    init {
        gameJob = viewModelScope.launch {
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

    fun moveLeft() {
        movePiece(-1, 0)
    }

    fun moveRight() {
        movePiece(1, 0)
    }

    fun rotate() {
        val rotatedShape = Array(currentPiece.shape[0].size) { IntArray(currentPiece.shape.size) }
        for (y in currentPiece.shape.indices) {
            for (x in currentPiece.shape[y].indices) {
                rotatedShape[x][currentPiece.shape.size - 1 - y] = currentPiece.shape[y][x]
            }
        }
        if (isValidPosition(currentX, currentY, Piece(rotatedShape, currentPiece.color))) {
            currentPiece = Piece(rotatedShape, currentPiece.color)
            updateBoard()
        }
    }

    private fun movePiece(dx: Int, dy: Int): Boolean {
        if (isValidPosition(currentX + dx, currentY + dy, currentPiece)) {
            currentX += dx
            currentY += dy
            updateBoard()
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
        val newBoard = _gameState.value.board.map { it.clone() }.toTypedArray()
        for (py in currentPiece.shape.indices) {
            for (px in currentPiece.shape[py].indices) {
                if (currentPiece.shape[py][px] != 0) {
                    newBoard[currentY + py][currentX + px] = currentPiece.color
                }
            }
        }
        _gameState.value = GameState(newBoard)
    }

    private fun clearLines() {
        val newBoard = _gameState.value.board.toMutableList()
        var linesCleared = 0
        for (y in newBoard.indices.reversed()) {
            if (newBoard[y].all { it != 0 }) {
                newBoard.removeAt(y)
                linesCleared++
            }
        }
        for (i in 0 until linesCleared) {
            newBoard.add(0, IntArray(boardWidth))
        }
        _gameState.value = GameState(newBoard.toTypedArray())
    }

    private fun spawnNewPiece() {
        currentPiece = pieces.random()
        currentX = boardWidth / 2 - currentPiece.shape[0].size / 2
        currentY = 0
        if (!isValidPosition(currentX, currentY, currentPiece)) {
            // Game Over
            gameJob?.cancel()
        }
    }

    private fun updateBoard() {
        val newBoard = _gameState.value.board.map { it.clone() }.toTypedArray()
        // Draw current piece on a temporary board
        for (py in currentPiece.shape.indices) {
            for (px in currentPiece.shape[py].indices) {
                if (currentPiece.shape[py][px] != 0) {
                    newBoard[currentY + py][currentX + px] = currentPiece.color
                }
            }
        }
        _gameState.value = GameState(newBoard)
    }

    private fun createEmptyBoard(): Array<IntArray> = Array(boardHeight) { IntArray(boardWidth) }
}
