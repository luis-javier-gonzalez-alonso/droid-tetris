package net.ljga.projects.games.tetris.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GameScreen(gameViewModel: GameViewModel = viewModel()) {
    val gameState by gameViewModel.gameState.collectAsState()

    LaunchedEffect(Unit) {
        gameViewModel.startGame()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Take available space, but respecting aspect ratio
                .padding(8.dp) // Add some padding around the canvas
                .aspectRatio(gameViewModel.boardWidth.toFloat() / gameViewModel.boardHeight.toFloat()) // Maintain aspect ratio
        ) {
            val boardWidthPx = size.width
            val boardHeightPx = size.height
            val squareSize = minOf(boardWidthPx / gameViewModel.boardWidth, boardHeightPx / gameViewModel.boardHeight)

            // Draw the fixed board parts
            gameState.board.forEachIndexed { y, row ->
                row.forEachIndexed { x, color ->
                    if (color != 0) { // Only draw if there's a block
                        drawRect(
                            color = colorFor(color),
                            topLeft = Offset(x * squareSize, y * squareSize),
                            size = Size(squareSize, squareSize)
                        )
                    }
                }
            }

            // Draw the falling piece
            gameState.piece?.let { piece ->
                piece.shape.forEachIndexed { y, row ->
                    row.forEachIndexed { x, value ->
                        if (value != 0) { // Draw only if the shape has a block here
                            drawRect(
                                color = colorFor(piece.color),
                                topLeft = Offset((gameState.pieceX + x) * squareSize, (gameState.pieceY + y) * squareSize),
                                size = Size(squareSize, squareSize)
                            )
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { gameViewModel.moveLeft() }) { Text("Left") }
            Button(onClick = { gameViewModel.rotate() }) { Text("Rotate") }
            Button(onClick = { gameViewModel.moveRight() }) { Text("Right") }
        }
    }
}

private fun colorFor(value: Int): Color {
    return when (value) {
        0 -> Color.LightGray // Background color, should not be drawn as blocks
        1 -> Color.Cyan     // I
        2 -> Color.Yellow   // O
        3 -> Color.Magenta  // T
        4 -> Color.Blue     // L
        5 -> Color.Green    // J
        6 -> Color.Red      // S
        7 -> Color.Green    // Z (Note: Z color was not defined, using Green as fallback)
        else -> Color.Gray // Fallback color for any unexpected values
    }
}

@Preview
@Composable
fun GameScreenPreview() {
    GameScreen()
}
