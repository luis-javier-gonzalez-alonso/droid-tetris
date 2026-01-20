
package net.ljga.projects.games.tetris.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.ljga.projects.games.tetris.ui.game.screens.GameScreen
import net.ljga.projects.games.tetris.ui.game.GameViewModel
import net.ljga.projects.games.tetris.ui.game.screens.MenuScreen
import net.ljga.projects.games.tetris.ui.game.screens.SettingsScreen
import net.ljga.projects.games.tetris.ui.game.screens.ShopScreen

@Composable
fun MainNavigation(gameViewModel: GameViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "menu") {
        composable("menu") {
            MenuScreen(
                gameViewModel = gameViewModel,
                onContinue = {
                    gameViewModel.continueGame()
                    navController.navigate("game")
                },
                onNewGame = {
                    gameViewModel.newGame()
                    navController.navigate("game")
                },
                onNewDebugGame = { mutations, artifacts ->
                    gameViewModel.newGame(mutations, artifacts)
                    navController.navigate("game")
                },
                onShop = {
                    navController.navigate("shop")
                },
                onSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable("game") {
            GameScreen(gameViewModel) {
                navController.popBackStack()
            }
        }
        composable("shop") {
            ShopScreen(gameViewModel) {
                navController.popBackStack()
            }
        }
        composable("settings") {
            SettingsScreen(gameViewModel) {
                navController.popBackStack()
            }
        }
    }
}
