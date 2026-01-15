package net.ljga.projects.games.tetris.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun MenuScreen(
    gameViewModel: GameViewModel,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    onNewDebugGame: (List<GameViewModel.Mutation>, List<GameViewModel.Artifact>) -> Unit,
    onMutations: () -> Unit
) {
    val gameState by gameViewModel.gameState.collectAsState()
    val highScore by gameViewModel.highScore.collectAsState()
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

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("High Score: $highScore")
        Spacer(modifier = Modifier.height(32.dp))

        if (gameState.piece != null) {
            Button(onClick = onContinue) {
                Text("Continue")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(onClick = onNewGame) {
            Text("New Game")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onMutations) {
            Text("Mutations")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { showDebugMenu = true }) {
            Text("Debug Mode")
        }
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
        Surface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Debug Menu", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Mutations")
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
                                }
                            )
                            Text(mutation.name)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Artifacts")
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
                                }
                            )
                            Text(artifact.name)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    gameViewModel.newGame(selectedMutations, selectedArtifacts)
                    onStartGame()
                }) {
                    Text("Start Debug Game")
                }
            }
        }
    }
}
