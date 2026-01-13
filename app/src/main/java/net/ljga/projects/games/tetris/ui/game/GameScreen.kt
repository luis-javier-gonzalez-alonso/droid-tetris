package net.ljga.projects.games.tetris.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GameScreen(gameViewModel: GameViewModel = viewModel()) {
    val gameState by gameViewModel.gameState.collectAsState()

    LaunchedEffect(Unit) {
        gameViewModel.startGame()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.weight(1f)) {
            val squareSize = size.width / gameViewModel.boardWidth

            // Draw the fixed board
            gameState.board.forEachIndexed { y, row ->
                row.forEachIndexed { x, color ->
                    drawRect(
                        color = colorFor(color),
                        topLeft = Offset(x * squareSize, y * squareSize),
                        size = Size(squareSize, squareSize)
                    )
                }
            }

            // Draw the falling piece
            gameState.piece?.let { piece ->
                piece.shape.forEachIndexed { y, row ->
                    row.forEachIndexed { x, value ->
                        if (value != 0) {
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
        0 -> Color.LightGray
        1 -> Color.Cyan
        2 -> Color.Yellow
        3 -> Color.Magenta
        4 -> Color.Blue
        5 -> Color.Green
        6 -> Color.Red
        else -> Color.Gray
    }
}

@Preview
@Composable
fun GameScreenPreview() {
    GameScreen()
}
