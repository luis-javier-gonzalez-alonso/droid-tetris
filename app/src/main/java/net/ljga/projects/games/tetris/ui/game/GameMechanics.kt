package net.ljga.projects.games.tetris.ui.game

import java.util.Random

/**
 * Base interface for all game mechanics (mutations and artifacts).
 * Provides common properties for display in the UI.
 */
interface GameMechanic {
    /** Display name of the mechanic */
    val name: String
    /** User-facing description of what the mechanic does */
    val description: String
}

/**
 * Hook called when a new game starts.
 * Use to modify initial game state (e.g., adding starting obstacles).
 */
interface IOnNewGameHook : GameMechanic {
    fun onNewGame(gameState: GameViewModel.GameState): GameViewModel.GameState
}

/**
 * Hook called when the player advances to a new level.
 * Use for level-dependent effects (e.g., adding obstacles per level).
 */
interface IOnLevelUpHook : GameMechanic {
    fun onLevelUp(gameState: GameViewModel.GameState): GameViewModel.GameState
}

/**
 * Hook called when a new piece spawns.
 * Use to modify the spawned piece or game state (e.g., change piece color, position).
 */
interface IOnPieceSpawnHook : GameMechanic {
    fun onPieceSpawn(gameState: GameViewModel.GameState): GameViewModel.GameState
}

/**
 * Hook called after lines are cleared.
 * Suspending function to support animations (e.g., falling fragments, time freeze).
 */
interface IOnLineClearHook : GameMechanic {
    suspend fun onLineClear(gameState: GameViewModel.GameState, linesCleared: Int): GameViewModel.GameState
}

/**
 * Modifier for the game tick delay (piece fall speed).
 * Return modified delay in milliseconds.
 */
interface ITickDelayModifier : GameMechanic {
    fun modifyTickDelay(delay: Long): Long
}

/**
 * Modifier for score calculation.
 * Receives the base points and returns the modified score.
 */
interface IScoreModifier : GameMechanic {
    fun modifyScore(points: Int, linesCleared: Int, level: Int): Int
}

/**
 * Override the default rotation behavior.
 * Return null to fall through to default rotation, or a new state to override.
 */
interface IRotationOverride : GameMechanic {
    fun onRotate(gameState: GameViewModel.GameState): GameViewModel.GameState?
}

/**
 * Modifier for rotation direction.
 * Return true to invert the rotation direction.
 */
interface IRotationDirectionModifier : GameMechanic {
    fun isInverted(): Boolean
}

/**
 * Modifier applied after rotation to adjust piece placement.
 * Use for effects like moving the piece up after rotation.
 */
interface IPostRotationPlacementModifier : GameMechanic {
    fun modifyPlacement(x: Int, y: Int, piece: GameViewModel.Piece, gameState: GameViewModel.GameState): Pair<Int, Int>
}

/**
 * Custom position validation for pieces.
 * Use to add additional constraints (e.g., board shrinking).
 */
interface IPositionValidator : GameMechanic {
    fun isValidPosition(x: Int, y: Int, piece: GameViewModel.Piece, gameState: GameViewModel.GameState, defaultResult: Boolean): Boolean
}

/**
 * Hook called before lines are cleared.
 * Use to modify which lines will be cleared (e.g., adding extra lines).
 */
interface IBeforeLineClearHook : GameMechanic {
    fun beforeLineClear(linesToClear: List<Int>, gameState: GameViewModel.GameState): List<Int>
}

/**
 * Marker interface for mechanics that require the ghost piece to be visible.
 * When any mechanic implements this, the ghost piece position is calculated.
 */
interface IRequiresGhostPiece : GameMechanic

/**
 * Base class for all mutations.
 * Mutations are permanent modifiers selected at the start of a run.
 */
abstract class Mutation(override val name: String, override val description: String) : GameMechanic

/**
 * Base class for all artifacts.
 * Artifacts are temporary power-ups collected during gameplay.
 */
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

class ColorblindMutation : Mutation("Colorblind", "All pieces are the same color"), IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState): GameViewModel.GameState {
        val piece = gameState.piece ?: return gameState
        val nextPiece = gameState.nextPiece
        val secondNextPiece = gameState.secondNextPiece
        return gameState.copy(
            piece = GameViewModel.Piece(piece.shape, 8),
            nextPiece = nextPiece?.let { GameViewModel.Piece(it.shape, 8) },
            secondNextPiece = secondNextPiece?.let { GameViewModel.Piece(it.shape, 8) }
        )
    }
}

class MoreIsMutation : Mutation("More 'I's", "Increases the frequency of 'I' pieces"), IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState): GameViewModel.GameState {
        if (Random().nextInt(5) == 0) {
            val iPiece = GameViewModel.pieces.first() // I-piece is first in the list
            return gameState.copy(piece = iPiece)
        }
        return gameState
    }
}

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

class FairPlayMutation : Mutation("Fair Play", "All 7 unique pieces will spawn before any are repeated"), IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState): GameViewModel.GameState {
        var queue = gameState.pieceQueue
        if (queue.isEmpty()) {
            queue = GameViewModel.pieces.shuffled()
        }
        val piece = queue.first()
        var nextQueue = queue.drop(1)
        // Pre-fill next pieces from queue
        var nextPiece = gameState.nextPiece
        var secondNextPiece = gameState.secondNextPiece
        if (nextQueue.isEmpty()) {
            nextQueue = GameViewModel.pieces.shuffled()
        }
        nextPiece = nextQueue.first()
        nextQueue = nextQueue.drop(1)
        if (nextQueue.isEmpty()) {
            nextQueue = GameViewModel.pieces.shuffled()
        }
        secondNextPiece = nextQueue.first()
        return gameState.copy(
            piece = piece,
            nextPiece = nextPiece,
            secondNextPiece = secondNextPiece,
            pieceQueue = nextQueue
        )
    }
}

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

class BoardShrinkerArtifact : Artifact("Board Shrinker", "Reduces the board width by 2 columns"), IPositionValidator, IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState): GameViewModel.GameState {
        val piece = gameState.piece ?: return gameState
        // Adjust spawn X to be centered in the shrunk (8-column) area
        val shrunkWidth = gameState.board[0].size - 2
        val newX = 2 + (shrunkWidth / 2 - piece.shape[0].size / 2)
        return gameState.copy(pieceX = newX)
    }

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
