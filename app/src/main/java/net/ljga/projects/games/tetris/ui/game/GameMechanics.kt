package net.ljga.projects.games.tetris.ui.game

import kotlin.random.Random

import net.ljga.projects.games.tetris.R

/**
 * Base interface for all game mechanics (mutations and artifacts).
 * Provides common properties for display in the UI.
 */
interface GameMechanic {
    /** Display name of the mechanic (Stable ID for persistence) */
    val name: String
    /** Resource ID for the mechanic's title (User-facing) */
    val titleResId: Int
    /** Resource ID for the mechanic's description (User-facing) */
    val descResId: Int
    /** Resource ID for the mechanic's icon */
    val iconResId: Int
}

/**
 * Hook called when a new game starts.
 * Use to modify initial game state (e.g., adding starting obstacles).
 */
interface IOnNewGameHook : GameMechanic {
    fun onNewGame(gameState: GameViewModel.GameState, rng: Random): GameViewModel.GameState
}

/**
 * Hook called when the player advances to a new level.
 * Use for level-dependent effects (e.g., adding obstacles per level).
 */
interface IOnLevelUpHook : GameMechanic {
    fun onLevelUp(gameState: GameViewModel.GameState, rng: Random): GameViewModel.GameState
}

/**
 * Hook called when a new piece spawns.
 * Use to modify the spawned piece or game state (e.g., change piece color, position).
 */
interface IOnPieceSpawnHook : GameMechanic {
    fun onPieceSpawn(gameState: GameViewModel.GameState, rng: Random): GameViewModel.GameState
}

/**
 * Strategy for handling line clears.
 * Replaces the default behavior of simply removing the lines.
 */
interface ILineClearStrategy : GameMechanic {
    /** The number of lines this strategy applies to. */
    val supportedLineCounts: Set<Int>

    /**
     * Execute the strategy.
     * @param gameState The current game state.
     * @param linesIndices The indices of the lines that are being cleared (sorted).
     * @return The new game state.
     */
    suspend fun execute(gameState: GameViewModel.GameState, linesIndices: List<Int>, rng: Random): GameViewModel.GameState
}

/**
 * Hook called after lines are cleared.
 * Suspending function to support animations (e.g., falling fragments, time freeze).
 * NOTE: This runs AFTER the strategy execution.
 */
interface IOnLineClearHook : GameMechanic {
    suspend fun onLineClear(gameState: GameViewModel.GameState, linesCleared: Int, rng: Random): GameViewModel.GameState
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
    fun onRotate(gameState: GameViewModel.GameState, gameViewModel: GameViewModel): GameViewModel.GameState?
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
    fun beforeLineClear(linesToClear: List<Int>, gameState: GameViewModel.GameState, rng: Random): List<Int>
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
abstract class Mutation(
    override val name: String,
    override val titleResId: Int,
    override val descResId: Int,
    override val iconResId: Int,
    val scoreMultiplier: Float = 1.0f
) : GameMechanic

/**
 * Base class for all artifacts.
 * Artifacts are temporary power-ups collected during gameplay.
 */
abstract class Artifact(
    override val name: String,
    override val titleResId: Int,
    override val descResId: Int,
    override val iconResId: Int
) : GameMechanic


class UnyieldingMutation : Mutation("Unyielding", R.string.mut_unyielding_title, R.string.mut_unyielding_desc, R.drawable.ic_mutation_unyielding, 2.0f), IOnNewGameHook, IOnLevelUpHook {
    private fun addGarbageLine(board: Array<IntArray>, rng: Random): Array<IntArray> {
        val newBoard = board.map { it.clone() }.toTypedArray()
        val randomX = rng.nextInt(newBoard[0].size)
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

    override fun onNewGame(gameState: GameViewModel.GameState, rng: Random): GameViewModel.GameState {
        return gameState.copy(board = addGarbageLine(gameState.board, rng))
    }

    override fun onLevelUp(gameState: GameViewModel.GameState, rng: Random): GameViewModel.GameState {
        return gameState.copy(board = addGarbageLine(gameState.board, rng))
    }
}

class FeatherFallMutation : Mutation("Feather Fall", R.string.mut_feather_fall_title, R.string.mut_feather_fall_desc, R.drawable.ic_mutation_featherfall, 0.8f), ITickDelayModifier {
    override fun modifyTickDelay(delay: Long): Long {
        return (delay * 1.2).toLong()
    }
}

class LeadFallMutation : Mutation("Lead Fall", R.string.mut_lead_fall_title, R.string.mut_lead_fall_desc, R.drawable.ic_mutation_leadfall, 1.25f), ITickDelayModifier {
    override fun modifyTickDelay(delay: Long): Long {
        return (delay * 0.75).toLong()
    }
}

class ClairvoyanceMutation : Mutation("Clairvoyance", R.string.mut_clairvoyance_title, R.string.mut_clairvoyance_desc, R.drawable.ic_mutation_clairvoyance, 0.8f)

class ColorblindMutation : Mutation("Colorblind", R.string.mut_colorblind_title, R.string.mut_colorblind_desc, R.drawable.ic_mutation_colorblind, 1.5f), IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState, rng: Random): GameViewModel.GameState {
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

class MoreIsMutation : Mutation("More 'I's", R.string.mut_more_is_title, R.string.mut_more_is_desc, R.drawable.ic_mutation_more_is, 0.9f), IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState, rng: Random): GameViewModel.GameState {
        if (rng.nextInt(5) == 0) {
            val iPiece = GameViewModel.pieces.first() // I-piece is first in the list
            return gameState.copy(piece = iPiece)
        }
        return gameState
    }
}

class GarbageCollectorMutation : Mutation("Garbage Collector", R.string.mut_garbage_collector_title, R.string.mut_garbage_collector_desc, R.drawable.ic_mutation_unyielding, 1.3f), IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState, rng: Random): GameViewModel.GameState {
        if (rng.nextInt(10) == 0) {
            val newBoard = gameState.board.map { it.clone() }.toTypedArray()
            val randomX = rng.nextInt(newBoard[0].size)
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

class TimeWarpMutation : Mutation("Time Warp", R.string.mut_time_warp_title, R.string.mut_time_warp_desc, R.drawable.ic_mutation_unyielding, 0.9f), IOnLineClearHook {
    override suspend fun onLineClear(gameState: GameViewModel.GameState, linesCleared: Int, rng: Random): GameViewModel.GameState {
        if (linesCleared > 0 && rng.nextInt(10) == 0) {
            kotlinx.coroutines.delay(2000)
        }
        return gameState
    }
}

class FairPlayMutation : Mutation("Fair Play", R.string.mut_fair_play_title, R.string.mut_fair_play_desc, R.drawable.ic_mutation_unyielding, 0.8f), IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState, rng: Random): GameViewModel.GameState {
        var queue = gameState.pieceQueue
        if (queue.isEmpty()) {
            queue = GameViewModel.pieces.shuffled(rng)
        }
        val piece = queue.first()
        var nextQueue = queue.drop(1)
        // Pre-fill next pieces from queue
        var nextPiece = gameState.nextPiece
        var secondNextPiece = gameState.secondNextPiece
        if (nextQueue.isEmpty()) {
            nextQueue = GameViewModel.pieces.shuffled(rng)
        }
        nextPiece = nextQueue.first()
        nextQueue = nextQueue.drop(1)
        if (nextQueue.isEmpty()) {
            nextQueue = GameViewModel.pieces.shuffled(rng)
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

class PhantomPieceMutation : Mutation("Phantom Piece", R.string.mut_phantom_piece_title, R.string.mut_phantom_piece_desc, R.drawable.ic_mutation_unyielding, 0.7f), IRequiresGhostPiece

// Artifacts

class SwiftnessCharmArtifact : Artifact("Swiftness Charm", R.string.art_swiftness_charm_title, R.string.art_swiftness_charm_desc, R.drawable.ic_mutation_unyielding), ITickDelayModifier {
    override fun modifyTickDelay(delay: Long): Long {
        return (delay * 0.9).toLong()
    }
}

class LineClearerArtifact : Artifact("Line Clearer", R.string.art_line_clearer_title, R.string.art_line_clearer_desc, R.drawable.ic_mutation_unyielding), IBeforeLineClearHook {
    override fun beforeLineClear(linesToClear: List<Int>, gameState: GameViewModel.GameState, rng: Random): List<Int> {
        if (linesToClear.isNotEmpty()) {
            val randomLine = (0 until gameState.board.size).random(rng)
            if (!linesToClear.contains(randomLine)) {
                return linesToClear + randomLine
            }
        }
        return linesToClear
    }
}

class ScoreMultiplierArtifact : Artifact("Score Multiplier", R.string.art_score_multiplier_title, R.string.art_score_multiplier_desc, R.drawable.ic_mutation_unyielding), IScoreModifier {
    override fun modifyScore(points: Int, linesCleared: Int, level: Int): Int {
        return (points * 1.5).toInt()
    }
}

class SpringLoadedRotatorArtifact : Artifact("Spring-loaded Rotator", R.string.art_spring_loaded_title, R.string.art_spring_loaded_desc, R.drawable.ic_mutation_unyielding), IPostRotationPlacementModifier {
    override fun modifyPlacement(x: Int, y: Int, piece: GameViewModel.Piece, gameState: GameViewModel.GameState): Pair<Int, Int> {
        return Pair(x, y - 1)
    }
}

class ChaosOrbArtifact : Artifact("Chaos Orb", R.string.art_chaos_orb_title, R.string.art_chaos_orb_desc, R.drawable.ic_mutation_unyielding), IRotationOverride {
    override fun onRotate(gameState: GameViewModel.GameState, gameViewModel: GameViewModel): GameViewModel.GameState {
        val newPiece = GameViewModel.pieces.random(gameViewModel.rng)
        var rotatedShape = newPiece.shape
        val numRotations = gameViewModel.rng.nextInt(4)
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

        return if (gameViewModel.isValidPosition(gameState.pieceX, gameState.pieceY, finalNewPiece, gameState.board)) {
            gameState.copy(piece = finalNewPiece)
        } else {
            gameState
        }
    }
}

class FallingFragmentsArtifact : Artifact("Falling Fragments", R.string.art_falling_fragments_title, R.string.art_falling_fragments_desc, R.drawable.ic_mutation_unyielding), ILineClearStrategy {
    override val supportedLineCounts: Set<Int> = setOf(2, 4)

    override suspend fun execute(gameState: GameViewModel.GameState, linesIndices: List<Int>, rng: Random): GameViewModel.GameState {
        // First, perform standard line clearing logic
        var state = DefaultLineClearStrategy.execute(gameState, linesIndices)

        // Then add falling fragments
        val newFragments = state.fallingFragments.toMutableList()
        repeat(2) {
            newFragments.add(Pair(rng.nextInt(state.board[0].size), 0))
        }
        return state.copy(fallingFragments = newFragments)
    }
}

class BoardWipeArtifact : Artifact("Board Wipe", R.string.art_board_wipe_title, R.string.art_board_wipe_desc, R.drawable.ic_mutation_unyielding), ILineClearStrategy {
    override val supportedLineCounts: Set<Int> = setOf(3)

    override suspend fun execute(gameState: GameViewModel.GameState, linesIndices: List<Int>, rng: Random): GameViewModel.GameState {
        // Completely wipe the board instead of just clearing lines
        val newArtifacts = gameState.artifacts.filter { it.name != name }
        return gameState.copy(board = Array(gameState.board.size) { IntArray(gameState.board[0].size) }, artifacts = newArtifacts)
    }
}

class InvertedRotationArtifact : Artifact("Inverted Rotation", R.string.art_inverted_rotation_title, R.string.art_inverted_rotation_desc, R.drawable.ic_mutation_unyielding), IRotationDirectionModifier {
    override fun isInverted(): Boolean {
        return true
    }
}

class PieceSwapperArtifact : Artifact("Piece Swapper", R.string.art_piece_swapper_title, R.string.art_piece_swapper_desc, R.drawable.ic_mutation_unyielding), IRotationOverride {
    override fun onRotate(gameState: GameViewModel.GameState, gameViewModel: GameViewModel): GameViewModel.GameState? {
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

class BoardShrinkerArtifact : Artifact("Board Shrinker", R.string.art_board_shrinker_title, R.string.art_board_shrinker_desc, R.drawable.ic_mutation_unyielding), IPositionValidator, IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState, rng: Random): GameViewModel.GameState {
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

object DefaultLineClearStrategy {
    fun execute(gameState: GameViewModel.GameState, linesIndices: List<Int>): GameViewModel.GameState {
        val board = gameState.board
        val newBoard = board.toMutableList()
        linesIndices.sorted().reversed().forEach { newBoard.removeAt(it) }
        repeat(linesIndices.size) { newBoard.add(0, IntArray(board[0].size)) }
        return gameState.copy(board = newBoard.toTypedArray())
    }
}
