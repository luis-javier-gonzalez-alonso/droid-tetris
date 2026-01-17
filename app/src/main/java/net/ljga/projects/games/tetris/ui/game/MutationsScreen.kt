package net.ljga.projects.games.tetris.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MutationsScreen(
    gameViewModel: GameViewModel = viewModel()
) {
    val unlockedMutations by gameViewModel.mutations.collectAsState()
    val allMutations = gameViewModel.allMutations

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(allMutations) { mutation ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(mutation.titleResId))
                        Text(text = stringResource(mutation.descResId))
                    }
                    if (unlockedMutations.contains(mutation)) {
                        Text("Unlocked")
                    } else {
                        Text("Locked")
                    }
                }
            }
        }
    }
}
