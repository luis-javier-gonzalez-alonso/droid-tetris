package net.ljga.projects.games.tetris.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MenuScreen(
    gameViewModel: GameViewModel,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    onMutations: () -> Unit
) {
    val gameState by gameViewModel.gameState.collectAsState()
    val highScore by gameViewModel.highScore.collectAsState()

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
    }
}
