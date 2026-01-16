package net.ljga.projects.games.tetris.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    gameViewModel: GameViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val gameState by gameViewModel.gameState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var accumulatedDragX by remember { mutableFloatStateOf(0f) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        onDispose {
            gameViewModel.pauseGame()
        }
    }

    if (gameState.isGameOver) {
        Dialog(onDismissRequest = onBack) {
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Game Over", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your score: ${gameState.currentScore}", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(24.dp))
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

    if (gameState.artifactChoices.isNotEmpty()) {
        ArtifactSelectionDialog(
            choices = gameState.artifactChoices,
            onSelect = { artifact ->
                gameViewModel.selectArtifact(artifact)
            }
        )
    }

    // Mutation/Artifact Acquired Popup
    gameState.pendingMutationPopup?.let { mechanic ->
        MutationPopup(
            mechanic = mechanic,
            onDismiss = {
                gameViewModel.dismissMutationPopup()
            }
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Column: Artifacts
        Column(
            modifier = Modifier
                .width(64.dp)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.1f))
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            gameState.artifacts.forEach { artifact ->
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = artifact.iconResId),
                    contentDescription = artifact.name,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Center: Game Board and Info
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("Score: ${gameState.currentScore}")
                Text("Level: ${gameState.level}")
            }

            gameState.currentBoss?.let {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Boss: ${it.name}", color = Color.Red, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text("Lines: ${it.requiredLines}")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                        gameState.clearingLines,
                        gameState.artifacts.any { it is BoardShrinkerArtifact }
                    )

                    gameState.nextPiece?.let {
                        val startX = gameViewModel.boardWidth / 2 - it.shape[0].size / 2
                        drawPiece(it, startX, 0, squareSize, boardPadding, alpha = 0.3f)
                    }

                    if (gameState.selectedMutations.any { it is ClairvoyanceMutation }) {
                        gameState.secondNextPiece?.let {
                            val startX = gameViewModel.boardWidth / 2 - it.shape[0].size / 2
                            drawPiece(it, startX, 4, squareSize, boardPadding, alpha = 0.15f)
                        }
                    }

                    gameState.piece?.let {
                        if (gameState.selectedMutations.any { it is PhantomPieceMutation }) {
                            drawPiece(it, gameState.pieceX, gameState.ghostPieceY, squareSize, boardPadding, alpha = 0.2f)
                        }
                        drawPiece(it, gameState.pieceX, gameState.pieceY, squareSize, boardPadding)
                    }

                    gameState.fallingFragments.forEach { (x, y) ->
                        drawRect(
                            color = Color.Gray,
                            topLeft = Offset(x * squareSize + boardPadding, y * squareSize + boardPadding),
                            size = Size(squareSize - 2 * boardPadding, squareSize - 2 * boardPadding)
                        )
                    }
                }
            }
        }

        // Right Column: Mutations
        Column(
            modifier = Modifier
                .width(64.dp)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.1f))
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            gameState.selectedMutations.forEach { mutation ->
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = mutation.iconResId),
                    contentDescription = mutation.name,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun ArtifactSelectionDialog(
    choices: List<Artifact>,
    onSelect: (Artifact) -> Unit
) {
    Dialog(onDismissRequest = { /* Prevent dismissal without selection */ }) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Choose an Artifact", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    choices.forEach { artifact ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .aspectRatio(0.7f)
                                .pointerInput(artifact) {
                                    detectTapGestures {
                                        onSelect(artifact)
                                    }
                                },
                            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = artifact.iconResId),
                                    contentDescription = artifact.name,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = artifact.name,
                                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = artifact.description,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MutationPopup(
    mechanic: GameMechanic,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        // Transparent background click to dismiss is handled by Dialog if we don't consume it?
        // But user wants "tap screen".
        // We'll make the surface clickable.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures { onDismiss() }
                },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("New Power Acquired!", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = Color(0xFFD35400))
                Spacer(modifier = Modifier.height(24.dp))
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = mechanic.iconResId),
                    contentDescription = mechanic.name,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(mechanic.name, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)//, androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(mechanic.description, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Tap anywhere to continue", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
        }
    }
}

private fun DrawScope.drawBoardBackground(
    board: Array<IntArray>,
    squareSize: Float,
    padding: Float,
    clearingLines: List<Int>,
    isShrunk: Boolean
) {
    board.forEachIndexed { y, row ->
        val alpha = if (clearingLines.contains(y)) 0.5f else 1f
        row.forEachIndexed { x, color ->
            val squareColor = if (isShrunk && x < 2) {
                Color.DarkGray
            } else {
                colorFor(color)
            }
            if (color != 0) {
                drawRect(
                    color = squareColor.copy(alpha = alpha),
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
