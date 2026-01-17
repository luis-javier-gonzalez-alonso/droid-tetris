package net.ljga.projects.games.tetris.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ljga.projects.games.tetris.R.*

@Composable
fun GameScreen(
    gameViewModel: GameViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val gameState by gameViewModel.gameState.collectAsState()
    val sensitivity by gameViewModel.preferenceDataStore.touchSensitivity.collectAsState(initial = 2.0f)
    val coroutineScope = rememberCoroutineScope()

    var accumulatedDragX by remember { mutableFloatStateOf(0f) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        onDispose {
            gameViewModel.pauseGame()
        }
    }

    // Main Game Container with Dark Gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            )
    ) {
        if (gameState.isGameOver) {
            Dialog(onDismissRequest = onBack) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF2C3E50),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE74C3C))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(string.game_over), style = MaterialTheme.typography.headlineMedium, color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(string.your_score, gameState.currentScore), style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                gameViewModel.newGame()
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498DB))
                        ) {
                            Text(stringResource(string.new_game), color = Color.White)
                        }
                    }
                }
            }
        }

        if (gameState.artifactChoices.isNotEmpty()) {
            ArtifactSelectionDialog(
                choices = gameState.artifactChoices,
                onSelect = {
                    gameViewModel.selectArtifact(it)
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

        Box(modifier = Modifier.fillMaxSize()) {
            // Center: Game Board and Info
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Info Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(stringResource(string.score_label, gameState.currentScore), color = Color.White, fontWeight = FontWeight.Bold)
                    Text(stringResource(string.level_label, gameState.level), color = Color.Cyan, fontWeight = FontWeight.Bold)
                }

                gameState.currentBoss?.let {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(string.boss_label, stringResource(it.nameResId)), color = Color(0xFFE74C3C), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(string.lines_label, it.requiredLines), color = Color.White)
                    }
                }

                // Game Board Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CA1AF)), // Board Border
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .aspectRatio(gameViewModel.boardWidth.toFloat() / gameViewModel.boardHeight.toFloat())
                            .padding(8.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
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

                                            val squareSizeWidth = size.width / gameViewModel.boardWidth
                                            val squareSizeHeight = size.height / gameViewModel.boardHeight

                                            val threshold = squareSizeWidth / sensitivity

                                            if (accumulatedDragX > threshold) {
                                                gameViewModel.moveRight()
                                                accumulatedDragX = 0f
                                            } else if (accumulatedDragX < -threshold) {
                                                gameViewModel.moveLeft()
                                                accumulatedDragX = 0f
                                            }

                                            if (accumulatedDragY > squareSizeHeight / sensitivity) {
                                                gameViewModel.moveDown()
                                                accumulatedDragY = 0f
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
                                drawPiece(it, gameViewModel.boardWidth / 2 - it.shape[0].size / 2, 0, squareSize, boardPadding, alpha = 0.3f)
                            }

                            if (gameState.selectedMutations.any { it is ClairvoyanceMutation }) {
                                gameState.secondNextPiece?.let {
                                    drawPiece(it, gameViewModel.boardWidth / 2 - it.shape[0].size / 2, 4, squareSize, boardPadding, alpha = 0.15f)
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
            }

            // Left Column: Artifacts
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp)
                    .padding(start = 8.dp, top = 16.dp, bottom = 16.dp)
                    .align(Alignment.CenterStart),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                gameState.artifacts.forEach { artifact ->
                    Image(
                        painter = painterResource(id = artifact.iconResId),
                        contentDescription = stringResource(artifact.titleResId),
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer(alpha = 0.8f)
                    )
                }
            }

            // Right Column: Mutations
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp)
                    .padding(end = 8.dp, top = 16.dp, bottom = 16.dp)
                    .align(Alignment.CenterEnd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                gameState.selectedMutations.forEach { mutation ->
                    Image(
                        painter = painterResource(id = mutation.iconResId),
                        contentDescription = stringResource(mutation.titleResId),
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer(alpha = 0.8f)
                    )
                }
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
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF2C3E50),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Choose an Artifact", style = MaterialTheme.typography.titleLarge, color = Color.White)
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
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Image(
                                    painter = painterResource(id = artifact.iconResId),
                                    contentDescription = stringResource(artifact.titleResId),
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = stringResource(artifact.titleResId),
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(artifact.descResId),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    color = Color.LightGray
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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures { onDismiss() }
                },
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF2C3E50),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(string.new_power_title), style = MaterialTheme.typography.headlineSmall, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Image(
                    painter = painterResource(id = mechanic.iconResId),
                    contentDescription = stringResource(mechanic.titleResId),
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(mechanic.titleResId), style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(mechanic.descResId), style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.LightGray)
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(string.tap_continue), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
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

// Updated Vibrant Neon Palette
private fun colorFor(value: Int): Color {
    return when (value) {
        0 -> Color.DarkGray.copy(alpha = 0.3f) // Empty Grid
        1 -> Color(0xFF00E5FF) // Cyan (I)
        2 -> Color(0xFFFFD700) // Gold (O)
        3 -> Color(0xFFD500F9) // Purple (T)
        4 -> Color(0xFF2979FF) // Blue (J)
        5 -> Color(0xFF00C853) // Green (S) - Neon
        6 -> Color(0xFFFF1744) // Red (Z) - Neon
        7 -> Color(0xFFFF6D00) // Orange (L) - Neon
        8 -> Color.Gray       // Debris/Lock
        else -> Color.Gray
    }
}

@Preview
@Composable
fun GameScreenPreview() {
    GameScreen()
}
