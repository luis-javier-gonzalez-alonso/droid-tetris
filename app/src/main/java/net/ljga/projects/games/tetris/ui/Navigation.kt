/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ljga.projects.games.tetris.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.ljga.projects.games.tetris.ui.game.GameScreen
import net.ljga.projects.games.tetris.ui.game.GameViewModel
import net.ljga.projects.games.tetris.ui.game.MenuScreen

@Composable
fun MainNavigation(gameViewModel: GameViewModel) {
    val navController = rememberNavController()
    val highScore by gameViewModel.highScore.collectAsState()

    NavHost(navController = navController, startDestination = "menu") {
        composable("menu") { 
            MenuScreen(
                highScore = highScore,
                onStartGame = { navController.navigate("game") }
            )
        }
        composable("game") { GameScreen(gameViewModel) }
    }
}
