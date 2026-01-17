
package net.ljga.projects.games.tetris.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.ljga.projects.games.tetris.ui.game.GameScreen
import net.ljga.projects.games.tetris.ui.game.GameViewModel
import net.ljga.projects.games.tetris.ui.game.MenuScreen
import net.ljga.projects.games.tetris.ui.game.MutationsScreen
import net.ljga.projects.games.tetris.ui.game.ShopScreen
import net.ljga.projects.games.tetris.ui.game.SettingsScreen

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
                onMutations = {
                    navController.navigate("mutations")
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
        composable("mutations") {
            MutationsScreen(gameViewModel)
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.ljga.projects.games.tetris.ui.game.GameScreen
import net.ljga.projects.games.tetris.ui.game.GameViewModel
import net.ljga.projects.games.tetris.ui.game.MenuScreen
import net.ljga.projects.games.tetris.ui.game.MutationsScreen
import net.ljga.projects.games.tetris.ui.game.MutationsScreen
import net.ljga.projects.games.tetris.ui.game.ShopScreen
import net.ljga.projects.games.tetris.ui.game.SettingsScreen

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
                onMutations = {
                    navController.navigate("mutations")
                },
                onShop = {
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
        composable("mutations") {
            MutationsScreen(gameViewModel)
        }
        composable("shop") {
            ShopScreen(gameViewModel) {
                navController.popBackStack()
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
