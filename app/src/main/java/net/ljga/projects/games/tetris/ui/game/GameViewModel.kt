package net.ljga.projects.games.tetris.ui.game

import androidx.lifecycle.ViewModel
import java.util.*

class GameViewModel : ViewModel() {

    val boardWidth = 10
    val boardHeight = 20

    private val board = Array(boardHeight) { IntArray(boardWidth) }

    data class Piece(val shape: Array<IntArray>, val color: Int)

    private val pieces = listOf(
        // I
        Piece(arrayOf(intArrayOf(1, 1, 1, 1)), 1),
        // O
        Piece(arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)), 2),
        // T
        Piece(arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1)), 3),
        // L
        Piece(arrayOf(intArrayOf(0, 0, 1), intArrayOf(1, 1, 1)), 4),
        // J
        Piece(arrayOf(intArrayOf(1, 0, 0), intArrayOf(1, 1, 1)), 5),
        // S
        Piece(arrayOf(intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)), 6),
        // Z
        Piece(arrayOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)), 7)
    )

    private var currentPiece: Piece = pieces.random()
    private var currentX = 0
    private var currentY = 0
}
