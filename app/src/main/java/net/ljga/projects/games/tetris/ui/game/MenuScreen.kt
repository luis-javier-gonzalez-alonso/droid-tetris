package net.ljga.projects.games.tetris.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import net.ljga.projects.games.tetris.R

@Composable
fun MenuScreen(
    gameViewModel: GameViewModel,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    onNewDebugGame: (List<Mutation>, List<Artifact>) -> Unit,
    onMutations: () -> Unit,
    onShop: () -> Unit,
    onSettings: () -> Unit
) {
    val gameState by gameViewModel.gameState.collectAsState()
    val highScore by gameViewModel.highScore.collectAsState()
    val coins by gameViewModel.coins.collectAsState()
    var showDebugMenu by remember { mutableStateOf(false) }

    if (showDebugMenu) {
        DebugMenuDialog(
            gameViewModel = gameViewModel,
            onDismiss = { showDebugMenu = false },
            onStartGame = {
                showDebugMenu = false
                onNewDebugGame(gameViewModel.debugMutations, gameViewModel.debugArtifacts)
            }
        )
    }

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
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.title_fracture),
                    style = TextStyle(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                        ),
                        shadow = Shadow(
                            color = Color.Black,
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
                Text(
                    text = stringResource(R.string.title_grid),
                    style = TextStyle(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFF7971E), Color(0xFFFFD200))
                        ),
                        shadow = Shadow(
                            color = Color.Black,
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Stats Pill
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CA1AF)),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.high_score), fontSize = 10.sp, color = Color.Gray)
                        Text("$highScore", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_badge_coin),
                            contentDescription = "Coins",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$coins", fontSize = 20.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Buttons
            if (gameState.piece != null && !gameState.isGameOver) {
                MenuButton(text = stringResource(R.string.continue_game), onClick = onContinue, color = Color(0xFF2ECC71))
                Spacer(modifier = Modifier.height(16.dp))
            }

            MenuButton(text = stringResource(R.string.new_game), onClick = onNewGame, color = Color(0xFF3498DB))
            Spacer(modifier = Modifier.height(16.dp))

            MenuButton(text = stringResource(R.string.mutations), onClick = onMutations, color = Color(0xFF9B59B6))
            Spacer(modifier = Modifier.height(16.dp))

            MenuButton(text = stringResource(R.string.shop), onClick = onShop, color = Color(0xFFF1C40F))
            Spacer(modifier = Modifier.height(16.dp))

            MenuButton(text = stringResource(R.string.settings), onClick = onSettings, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            TextButton(onClick = { showDebugMenu = true }) {
                Text(stringResource(R.string.debug_mode), color = Color.Gray)
            }
        }
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit, color: Color) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun DebugMenuDialog(
    gameViewModel: GameViewModel,
    onDismiss: () -> Unit,
    onStartGame: () -> Unit
) {
    var selectedMutations by remember { mutableStateOf(gameViewModel.debugMutations) }
    var selectedArtifacts by remember { mutableStateOf(gameViewModel.debugArtifacts) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF2C3E50)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Debug Menu", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Mutations", color = Color.Cyan)
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(gameViewModel.allMutations) { mutation ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedMutations.contains(mutation),
                                onCheckedChange = {
                                    selectedMutations = if (it) {
                                        selectedMutations + mutation
                                    } else {
                                        selectedMutations - mutation
                                    }
                                    gameViewModel.debugMutations = selectedMutations
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color.Cyan, uncheckedColor = Color.Gray)
                            )
                            Text(mutation.name, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Artifacts", color = Color.Cyan)
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(gameViewModel.allArtifacts) { artifact ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedArtifacts.contains(artifact),
                                onCheckedChange = {
                                    selectedArtifacts = if (it) {
                                        selectedArtifacts + artifact
                                    } else {
                                        selectedArtifacts - artifact
                                    }
                                    gameViewModel.debugArtifacts = selectedArtifacts
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color.Cyan, uncheckedColor = Color.Gray)
                            )
                            Text(artifact.name, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        gameViewModel.newGame(selectedMutations, selectedArtifacts)
                        onStartGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60))
                ) {
                    Text("Start Debug Game")
                }
            }
        }
    }
}
