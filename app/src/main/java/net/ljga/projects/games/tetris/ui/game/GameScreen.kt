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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GameScreen(gameViewModel: GameViewModel = viewModel()) {
    val gameState by gameViewModel.gameState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.weight(1f)) {
            val squareSize = size.width / gameViewModel.boardWidth

            // Draw board
            for (y in 0 until gameViewModel.boardHeight) {
                for (x in 0 until gameViewModel.boardWidth) {
                    drawRect(
                        color = Color.LightGray,
                        topLeft = androidx.compose.ui.geometry.Offset(x * squareSize, y * squareSize),
                        size = androidx.compose.ui.geometry.Size(squareSize, squareSize),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
                    )
                }
            }


            gameState.board.forEachIndexed { y, row ->
                row.forEachIndexed { x, color ->
                    drawRect(
                        color = when(color) {
                            0 -> Color.LightGray
                            1 -> Color.Cyan
                            2 -> Color.Yellow
                            3 -> Color.Magenta
                            4 -> Color.Blue
                            5 -> Color.Green
                            6 -> Color.Red
                            else -> Color.Gray
                        },
                        topLeft = androidx.compose.ui.geometry.Offset(x * squareSize, y * squareSize),
                        size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
                    )
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

@Preview
@Composable
fun GameScreenPreview() {
    GameScreen()
}
