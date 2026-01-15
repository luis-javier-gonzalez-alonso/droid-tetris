package net.ljga.projects.games.tetris.ui.game

import java.util.Random

interface GameMechanic {
    val name: String
    val description: String
}

interface IOnNewGameHook : GameMechanic {
    fun onNewGame(gameState: GameViewModel.GameState): GameViewModel.GameState
}

interface IOnLevelUpHook : GameMechanic {
    fun onLevelUp(gameState: GameViewModel.GameState): GameViewModel.GameState
}

interface IOnPieceSpawnHook : GameMechanic {
    fun onPieceSpawn(gameState: GameViewModel.GameState): GameViewModel.GameState
}

interface IOnLineClearHook : GameMechanic {
    suspend fun onLineClear(gameState: GameViewModel.GameState, linesCleared: Int): GameViewModel.GameState
}

interface ITickDelayModifier : GameMechanic {
    fun modifyTickDelay(delay: Long): Long
}

interface IScoreModifier : GameMechanic {
    fun modifyScore(points: Int, linesCleared: Int, level: Int): Int
}

interface IRotationOverride : GameMechanic {
    fun onRotate(gameState: GameViewModel.GameState): GameViewModel.GameState?
}

interface IRotationDirectionModifier : GameMechanic {
    fun isInverted(): Boolean
}

interface IPostRotationPlacementModifier : GameMechanic {
    fun modifyPlacement(x: Int, y: Int, piece: GameViewModel.Piece, gameState: GameViewModel.GameState): Pair<Int, Int>
}

interface IPositionValidator : GameMechanic {
    fun isValidPosition(x: Int, y: Int, piece: GameViewModel.Piece, gameState: GameViewModel.GameState, defaultResult: Boolean): Boolean
}

interface IBeforeLineClearHook : GameMechanic {
    fun beforeLineClear(linesToClear: List<Int>, gameState: GameViewModel.GameState): List<Int>
}

interface IRequiresGhostPiece : GameMechanic

abstract class Mutation(override val name: String, override val description: String) : GameMechanic

abstract class Artifact(override val name: String, override val description: String) : GameMechanic

class UnyieldingMutation : Mutation("Unyielding", "Start with one line of garbage blocks, and a new one for each level"), IOnNewGameHook, IOnLevelUpHook {
    private fun addGarbageLine(board: Array<IntArray>): Array<IntArray> {
        val newBoard = board.map { it.clone() }.toTypedArray()
        val random = Random()
        val randomX = random.nextInt(newBoard[0].size)
        var targetY = newBoard.size - 1

        while (targetY >= 0 && newBoard[targetY][randomX] != 0) {
            targetY--
        }

        if (targetY >= 0) {
            for (y in targetY downTo 1) {
                newBoard[y][randomX] = newBoard[y - 1][randomX]
            }
            newBoard[0][randomX] = 8 // Garbage block color
        }
        return newBoard
    }

    override fun onNewGame(gameState: GameViewModel.GameState): GameViewModel.GameState {
        return gameState.copy(board = addGarbageLine(gameState.board))
    }

    override fun onLevelUp(gameState: GameViewModel.GameState): GameViewModel.GameState {
        return gameState.copy(board = addGarbageLine(gameState.board))
    }
}

class FeatherFallMutation : Mutation("Feather Fall", "Pieces fall 20% slower"), ITickDelayModifier {
    override fun modifyTickDelay(delay: Long): Long {
        return (delay * 1.2).toLong()
    }
}

class LeadFallMutation : Mutation("Lead Fall", "Pieces fall 25% faster"), ITickDelayModifier {
    override fun modifyTickDelay(delay: Long): Long {
        return (delay * 0.75).toLong()
    }
}

class ClairvoyanceMutation : Mutation("Clairvoyance", "See the next two pieces instead of one")

class ColorblindMutation : Mutation("Colorblind", "All pieces are the same color")

class MoreIsMutation : Mutation("More 'I's", "Increases the frequency of 'I' pieces")

class GarbageCollectorMutation : Mutation("Garbage Collector", "Spawning a piece has a chance to add a garbage block"), IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState): GameViewModel.GameState {
        if (Random().nextInt(10) == 0) {
            val newBoard = gameState.board.map { it.clone() }.toTypedArray()
            val randomX = Random().nextInt(newBoard[0].size)
            var targetY = newBoard.size - 1

            while (targetY >= 0 && newBoard[targetY][randomX] != 0) {
                targetY--
            }

            if (targetY >= 0) {
                for (y in targetY downTo 1) {
                    newBoard[y][randomX] = newBoard[y - 1][randomX]
                }
                newBoard[0][randomX] = 8 // Garbage block color
            }
            return gameState.copy(board = newBoard)
        }
        return gameState
    }
}

class TimeWarpMutation : Mutation("Time Warp", "Clearing a line has a 10% chance to freeze the game for 2 seconds"), IOnLineClearHook {
    override suspend fun onLineClear(gameState: GameViewModel.GameState, linesCleared: Int): GameViewModel.GameState {
        if (linesCleared > 0 && Random().nextInt(10) == 0) {
            kotlinx.coroutines.delay(2000)
        }
        return gameState
    }
}

class FairPlayMutation : Mutation("Fair Play", "All 7 unique pieces will spawn before any are repeated")

class PhantomPieceMutation : Mutation("Phantom Piece", "Shows a ghost of where the current piece will land"), IRequiresGhostPiece

// Artifacts

class SwiftnessCharmArtifact : Artifact("Swiftness Charm", "Increases piece drop speed by 10%"), ITickDelayModifier {
    override fun modifyTickDelay(delay: Long): Long {
        return (delay * 0.9).toLong()
    }
}

class LineClearerArtifact : Artifact("Line Clearer", "Clears an extra line randomly"), IBeforeLineClearHook {
    override fun beforeLineClear(linesToClear: List<Int>, gameState: GameViewModel.GameState): List<Int> {
        if (linesToClear.isNotEmpty()) {
            val randomLine = (0 until gameState.board.size).random()
            if (!linesToClear.contains(randomLine)) {
                return linesToClear + randomLine
            }
        }
        return linesToClear
    }
}

class ScoreMultiplierArtifact : Artifact("Score Multiplier", "Multiplies score from line clears by 1.5x"), IScoreModifier {
    override fun modifyScore(points: Int, linesCleared: Int, level: Int): Int {
        return (points * 1.5).toInt()
    }
}

class SpringLoadedRotatorArtifact : Artifact("Spring-loaded Rotator", "Rotating moves the piece up one space"), IPostRotationPlacementModifier {
    override fun modifyPlacement(x: Int, y: Int, piece: GameViewModel.Piece, gameState: GameViewModel.GameState): Pair<Int, Int> {
        return Pair(x, y - 1)
    }
}

class ChaosOrbArtifact : Artifact("Chaos Orb", "Rotating changes the piece type"), IRotationOverride {
    override fun onRotate(gameState: GameViewModel.GameState): GameViewModel.GameState {
        val newPiece = GameViewModel.pieces.random()
        var rotatedShape = newPiece.shape
        val numRotations = Random().nextInt(4)
        for (i in 0 until numRotations) {
            val currentShape = rotatedShape
            rotatedShape = Array(currentShape[0].size) { IntArray(currentShape.size) }
            for (y in currentShape.indices) {
                for (x in currentShape[y].indices) {
                    rotatedShape[x][currentShape.size - 1 - y] = currentShape[y][x]
                }
            }
        }
        val finalNewPiece = GameViewModel.Piece(rotatedShape, newPiece.color)

        return if (GameViewModel.isValidPosition(gameState.pieceX, gameState.pieceY, finalNewPiece, gameState.board)) {
            gameState.copy(piece = finalNewPiece)
        } else {
            gameState
        }
    }
}

class FallingFragmentsArtifact : Artifact("Falling Fragments", "When completing 2 or 4 lines, 2 single-square pieces drop from the top in random positions."), IOnLineClearHook {
    override suspend fun onLineClear(gameState: GameViewModel.GameState, linesCleared: Int): GameViewModel.GameState {
        if (linesCleared == 2 || linesCleared == 4) {
            // This logic was originally in GameViewModel and starts a coroutine.
            // We'll need to call back into the ViewModel to start the animation.
            // For now, we'll just add the fragments directly.
            val random = Random()
            val newFragments = gameState.fallingFragments.toMutableList()
            repeat(2) {
                newFragments.add(Pair(random.nextInt(gameState.board[0].size), 0))
            }
            return gameState.copy(fallingFragments = newFragments)
        }
        return gameState
    }
}

class BoardWipeArtifact : Artifact("Board Wipe", "Single-use: Clearing 3 lines at the same time clears the entire board."), IOnLineClearHook {
    override suspend fun onLineClear(gameState: GameViewModel.GameState, linesCleared: Int): GameViewModel.GameState {
        if (linesCleared == 3) {
            val newArtifacts = gameState.artifacts.filter { it.name != name }
            return gameState.copy(board = Array(gameState.board.size) { IntArray(gameState.board[0].size) }, artifacts = newArtifacts)
        }
        return gameState
    }
}

class InvertedRotationArtifact : Artifact("Inverted Rotation", "Inverts the rotation direction of pieces"), IRotationDirectionModifier {
    override fun isInverted(): Boolean {
        return true
    }
}

class PieceSwapperArtifact : Artifact("Piece Swapper", "Swap the current piece with the next piece by rotating twice"), IRotationOverride {
    override fun onRotate(gameState: GameViewModel.GameState): GameViewModel.GameState? {
        if (gameState.rotationCount >= 2) {
            return gameState.copy(
                piece = gameState.nextPiece,
                nextPiece = gameState.piece,
                rotationCount = 0
            )
        }
        return null
    }
}

class BoardShrinkerArtifact : Artifact("Board Shrinker", "Reduces the board width by 2 columns"), IPositionValidator {
    override fun isValidPosition(x: Int, y: Int, piece: GameViewModel.Piece, gameState: GameViewModel.GameState, defaultResult: Boolean): Boolean {
        for (py in piece.shape.indices) {
            for (px in piece.shape[py].indices) {
                if (piece.shape[py][px] != 0) {
                    val boardX = x + px
                    if (boardX < 2) return false
                }
            }
        }
        return defaultResult
    }
}
