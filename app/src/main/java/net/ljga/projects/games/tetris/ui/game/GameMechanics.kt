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

class ClairvoyanceMutation : Mutation("Clairvoyance", "See the next two pieces instead of one"), IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState): GameViewModel.GameState {
        return gameState.copy(secondNextPiece = GameViewModel.pieces.random())
    }
}

class ColorblindMutation : Mutation("Colorblind", "All pieces are the same color"), IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState): GameViewModel.GameState {
        val newPiece = gameState.piece?.copy(color = 8)
        val newNextPiece = gameState.nextPiece?.copy(color = 8)
        val newSecondNextPiece = gameState.secondNextPiece?.copy(color = 8)
        return gameState.copy(piece = newPiece, nextPiece = newNextPiece, secondNextPiece = newSecondNextPiece)
    }
}

class MoreIsMutation : Mutation("More 'I's", "Increases the frequency of 'I' pieces"), IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState): GameViewModel.GameState {
        if (Random().nextInt(5) == 0) {
            return gameState.copy(piece = GameViewModel.pieces.first())
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
        var newPieceQueue = gameState.pieceQueue
        if (newPieceQueue.isEmpty()) {
            newPieceQueue = GameViewModel.pieces.shuffled()
        }
        val pieceToSpawn = newPieceQueue.first()
        newPieceQueue = newPieceQueue.drop(1)

        return gameState.copy(piece = pieceToSpawn, pieceQueue = newPieceQueue)
    }
}

class PhantomPieceMutation : Mutation("Phantom Piece", "Shows a ghost of where the current piece will land"), IOnPieceSpawnHook {
    override fun onPieceSpawn(gameState: GameViewModel.GameState): GameViewModel.GameState {
        var ghostY = gameState.pieceY
        gameState.piece?.let {
            while (isValidPosition(gameState.pieceX, ghostY + 1, it, gameState)) {
                ghostY++
            }
        }
        return gameState.copy(ghostPieceY = ghostY)
    }

    private fun isValidPosition(x: Int, y: Int, piece: GameViewModel.Piece, gameState: GameViewModel.GameState): Boolean {
        for (py in piece.shape.indices) {
            for (px in piece.shape[py].indices) {
                if (piece.shape[py][px] != 0) {
                    val boardX = x + px
                    val boardY = y + py

                    if (boardX < 0 || boardX >= gameState.board[0].size || boardY < 0 || boardY >= gameState.board.size || gameState.board[boardY][boardX] != 0) {
                        return false
                    }
                }
            }
        }
        return true
    }
}
