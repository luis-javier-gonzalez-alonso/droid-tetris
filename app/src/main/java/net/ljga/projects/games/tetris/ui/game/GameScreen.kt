package net.ljga.projects.games.tetris.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GameScreen(gameViewModel: GameViewModel = viewModel()) {
    val gameState by gameViewModel.gameState.collectAsState()
    val currentScore by gameViewModel.currentScore.collectAsState()
    val highScore by gameViewModel.highScore.collectAsState()

    LaunchedEffect(Unit) {
        gameViewModel.startGame()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("Score: $currentScore")
            Text("High Score: $highScore")
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
                .aspectRatio(gameViewModel.boardWidth.toFloat() / gameViewModel.boardHeight.toFloat())
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        gameViewModel.rotate()
                    })
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            if (dragAmount > 0) {
                                gameViewModel.moveRight()
                            } else {
                                gameViewModel.moveLeft()
                            }
                        }
                    )
                }
        ) {
            val squareSize = minOf(size.width / gameViewModel.boardWidth, size.height / gameViewModel.boardHeight)
            val boardPadding = 4.dp.toPx()

            drawBoardBackground(gameState.board, squareSize, boardPadding, gameState.clearingLines)

            gameState.piece?.let {
                drawGhostPiece(it, gameState.pieceX, gameState.ghostY, squareSize, boardPadding)
                drawPiece(it, gameState.pieceX, gameState.pieceY, squareSize, boardPadding)
            }
        }
    }
}

private fun DrawScope.drawBoardBackground(board: Array<IntArray>, squareSize: Float, padding: Float, clearingLines: List<Int>) {
    board.forEachIndexed { y, row ->
        val alpha = if (clearingLines.contains(y)) 0.5f else 1f
        row.forEachIndexed { x, color ->
            if (color != 0) {
                drawRect(
                    color = colorFor(color).copy(alpha = alpha),
                    topLeft = Offset(x * squareSize + padding, y * squareSize + padding),
                    size = Size(squareSize - 2 * padding, squareSize - 2 * padding)
                )
            }
        }
    }
}

private fun DrawScope.drawPiece(piece: GameViewModel.Piece, x: Int, y: Int, squareSize: Float, padding: Float) {
    piece.shape.forEachIndexed { row_idx, row ->
        row.forEachIndexed { col_idx, value ->
            if (value != 0) {
                drawRect(
                    color = colorFor(piece.color),
                    topLeft = Offset((x + col_idx) * squareSize + padding, (y + row_idx) * squareSize + padding),
                    size = Size(squareSize - 2 * padding, squareSize - 2 * padding)
                )
            }
        }
    }
}

private fun DrawScope.drawGhostPiece(piece: GameViewModel.Piece, x: Int, y: Int, squareSize: Float, padding: Float) {
    piece.shape.forEachIndexed { row_idx, row ->
        row.forEachIndexed { col_idx, value ->
            if (value != 0) {
                drawRect(
                    color = colorFor(piece.color).copy(alpha = 0.3f),
                    topLeft = Offset((x + col_idx) * squareSize + padding, (y + row_idx) * squareSize + padding),
                    size = Size(squareSize - 2 * padding, squareSize - 2 * padding)
                )
            }
        }
    }
}

private fun colorFor(value: Int): Color {
    return when (value) {
        0 -> Color.LightGray
        1 -> Color.Cyan
        2 -> Color.Yellow
        3 -> Color.Magenta
        4 -> Color.Blue
        5 -> Color.Green
        6 -> Color.Red
        7 -> Color.Green
        else -> Color.Gray
    }
}

@Preview
@Composable
fun GameScreenPreview() {
    GameScreen()
}
