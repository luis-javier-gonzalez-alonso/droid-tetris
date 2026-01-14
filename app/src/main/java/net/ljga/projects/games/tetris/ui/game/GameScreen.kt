package net.ljga.projects.games.tetris.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GameScreen(
    gameViewModel: GameViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val gameState by gameViewModel.gameState.collectAsState()

    var accumulatedDragX by remember { mutableFloatStateOf(0f) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        onDispose {
            gameViewModel.pauseGame()
        }
    }

    if (gameState.isGameOver) {
        Dialog(onDismissRequest = onBack) {
            Surface {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Game Over", modifier = Modifier.padding(bottom = 8.dp))
                    Text("Your score: ${gameState.currentScore}")
                    Button(onClick = {
                        gameViewModel.newGame()
                        onBack()
                    }) {
                        Text("New Game")
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Score: ${gameState.currentScore}")
            Text("Level: ${gameState.level}")
            Text("Lines: ${gameState.linesUntilNextLevel}")
        }

        gameState.currentBoss?.let {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Boss: ${it.name}")
                Text("Lines to clear: ${it.requiredLines}")
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .aspectRatio(gameViewModel.boardWidth.toFloat() / gameViewModel.boardHeight.toFloat())
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { gameViewModel.rotate() })
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                accumulatedDragX = 0f
                                accumulatedDragY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                accumulatedDragX += dragAmount.x
                                accumulatedDragY += dragAmount.y

                                val squareSizeWidth =
                                    (size.width / 4) / gameViewModel.boardWidth.toFloat()
                                val squareSizeHeight =
                                    (size.height / 4) / gameViewModel.boardHeight.toFloat()

                                if (accumulatedDragX > squareSizeWidth) {
                                    gameViewModel.moveRight()
                                    accumulatedDragX -= squareSizeWidth
                                } else if (accumulatedDragX < -squareSizeWidth) {
                                    gameViewModel.moveLeft()
                                    accumulatedDragX += squareSizeWidth
                                }

                                if (accumulatedDragY > squareSizeHeight) {
                                    gameViewModel.moveDown()
                                    accumulatedDragX = 0f
                                    accumulatedDragY -= squareSizeHeight
                                }
                            }
                        )
                    }
            ) {
                val squareSize = minOf(
                    size.width / gameViewModel.boardWidth,
                    size.height / gameViewModel.boardHeight
                )
                val boardPadding = 2.dp.toPx()

                drawBoardBackground(
                    gameState.board,
                    squareSize,
                    boardPadding,
                    gameState.clearingLines
                )

                gameState.nextPiece?.let {
                    val startX = gameViewModel.boardWidth / 2 - it.shape[0].size / 2
                    drawPiece(it, startX, 0, squareSize, boardPadding, alpha = 0.3f)
                }

                if (gameState.selectedMutations.any { it.name == "Clairvoyance" }) {
                    gameState.secondNextPiece?.let {
                        val startX = gameViewModel.boardWidth / 2 - it.shape[0].size / 2
                        drawPiece(it, startX, 4, squareSize, boardPadding, alpha = 0.15f)
                    }
                }

                gameState.piece?.let {
                    if (gameState.selectedMutations.any { it.name == "Phantom Piece" }) {
                        drawPiece(it, gameState.pieceX, gameState.ghostPieceY, squareSize, boardPadding, alpha = 0.2f)
                    }
                    drawPiece(it, gameState.pieceX, gameState.pieceY, squareSize, boardPadding)
                }
            }
            Column(
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                if (gameState.artifacts.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(gameState.artifacts) { artifact ->
                            Card(modifier = Modifier.padding(end = 8.dp)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(artifact.name)
                                    Text(artifact.description)
                                }
                            }
                        }
                    }
                }

                if (gameState.selectedMutations.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(gameState.selectedMutations) { mutation ->
                            Card(modifier = Modifier.padding(end = 8.dp)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(mutation.name)
                                    Text(mutation.description)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawBoardBackground(
    board: Array<IntArray>,
    squareSize: Float,
    padding: Float,
    clearingLines: List<Int>
) {
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

private fun DrawScope.drawPiece(
    piece: GameViewModel.Piece,
    x: Int,
    y: Int,
    squareSize: Float,
    padding: Float,
    alpha: Float = 1f
) {
    piece.shape.forEachIndexed { row_idx, row ->
        row.forEachIndexed { col_idx, value ->
            if (value != 0) {
                val pieceColor = if (piece.color == 8) Color.DarkGray else colorFor(piece.color)
                drawRect(
                    color = pieceColor.copy(alpha = alpha),
                    topLeft = Offset(
                        (x + col_idx) * squareSize + padding,
                        (y + row_idx) * squareSize + padding
                    ),
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
        8 -> Color.DarkGray
        else -> Color.Gray
    }
}

@Preview
@Composable
fun GameScreenPreview() {
    GameScreen()
}
