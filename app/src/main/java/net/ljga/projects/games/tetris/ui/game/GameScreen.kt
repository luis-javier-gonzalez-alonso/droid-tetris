package net.ljga.projects.games.tetris.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GameScreen(gameViewModel: GameViewModel = viewModel()) {
    Canvas(modifier = Modifier.fillMaxSize()) {
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
    }
}

@Preview
@Composable
fun GameScreenPreview() {
    GameScreen()
}
