
package net.ljga.projects.games.tetris.ui.game

import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import net.ljga.projects.games.tetris.data.GameplayDataRepository
import net.ljga.projects.games.tetris.ui.game.GameViewModel.Companion.pieces

private const val TAG = "GameLogic"

fun GameViewModel.createEmptyBoard(): Array<IntArray> = Array(boardHeight) { IntArray(boardWidth) }

fun GameViewModel.applyStartingMutations() {
    _gameState.value = _gameState.value.applyHook<IOnNewGameHook> { hook, state ->
        hook.onNewGame(state, rng)
    }
}

fun GameViewModel.runGame() {
    if (gameJob?.isActive == true) return
    gameJob = viewModelScope.launch {
        if (_gameState.value.piece == null) {
            spawnNewPiece()
        }
        while (true) {
            val currentLevel = _gameState.value.level
            var delayMs = (500 - (currentLevel - 1) * 50).coerceAtLeast(100).toLong()
            delayMs = _gameState.value.applyModifier<ITickDelayModifier>(delayMs) { mod, delay ->
                mod.modifyTickDelay(delay)
            }


            if (_gameState.value.currentBoss?.name == "The Sprinter") {
                delayMs = (delayMs * 0.7).toLong()
            }
            delay(delayMs)
            if (!movePiece(0, 1)) {
                Log.d(TAG, "Piece could not move down. Locking piece.")
                lockPiece()
                val linesCleared = clearLines()
                viewModelScope.launch {
                    var state = _gameState.value
                    _gameState.value.selectedMutations.forEach { mutation ->
                        if (mutation is IOnLineClearHook) {
                            state = mutation.onLineClear(state, linesCleared, rng)
                        }
                    }
                    _gameState.value.artifacts.forEach { artifact ->
                        if (artifact is IOnLineClearHook) {
                            state = artifact.onLineClear(state, linesCleared, rng)
                        }
                    }
                    _gameState.value = state

                    // Animate falling fragments if any were added by artifacts
                    if (_gameState.value.fallingFragments.isNotEmpty()) {
                        animateFallingFragments()
                    }

                    updateScoreAndLevel(linesCleared)
                    if (linesCleared > 0) {
                        delay(300)
                        _gameState.value = _gameState.value.copy(clearingLines = emptyList())
                    }
                    if (!spawnNewPiece()) {
                        Log.d(TAG, "Game Over.")
                        endGame(gameplayDataRepository)
                    }
                }.join()
            }
        }
    }
}

fun GameViewModel.endGame(gameplayDataRepository: GameplayDataRepository) {
    if (_gameState.value.currentScore > highScore.value) {
        _highScore.value = _gameState.value.currentScore
        viewModelScope.launch {
            gameplayDataRepository.updateHighScore(_gameState.value.currentScore)
        }
    }
    
    // Award coins based on score (1 coin per 100 points)
    val coinsEarned = _gameState.value.currentScore / 100
    if (coinsEarned > 0) {
        viewModelScope.launch {
            gameplayDataRepository.addCoins(coinsEarned)
        }
    }

    _gameState.value = _gameState.value.copy(isGameOver = true)
    gameJob?.cancel()
    gameJob = null
    viewModelScope.launch { gameplayDataRepository.clearGameState() }
}

fun GameViewModel.movePiece(dx: Int, dy: Int): Boolean {
    val currentPiece = _gameState.value.piece ?: return false
    val newX = _gameState.value.pieceX + dx
    val newY = _gameState.value.pieceY + dy

    if (isValidPosition(newX, newY, currentPiece, _gameState.value.board)) {
        _gameState.value = _gameState.value.copy(pieceX = newX, pieceY = newY)
        updateGhostPiece()
        return true
    }
    return false
}

suspend fun GameViewModel.clearLines(): Int {
    val board = _gameState.value.board
    var linesToClear = board.indices.filter { y ->
        board[y].all { it != 0 }
    }.toMutableList()

    for (artifact in _gameState.value.artifacts) {
        if (artifact is IBeforeLineClearHook) {
            linesToClear = artifact.beforeLineClear(linesToClear, _gameState.value, rng).toMutableList()
        }
    }

    val linesToClearList = linesToClear.toList()

    if (linesToClearList.isNotEmpty()) {
        _gameState.value = _gameState.value.copy(clearingLines = linesToClearList)
        delay(300)

        // Use Strategy for clearing logic
        val strategy = _gameState.value.artifacts.filterIsInstance<ILineClearStrategy>()
            .find { it.supportedLineCounts.contains(linesToClearList.size) }

        _gameState.value = if (strategy != null) {
            strategy.execute(_gameState.value, linesToClearList, rng)
        } else {
            DefaultLineClearStrategy.execute(_gameState.value, linesToClearList)
        }
    }
    return linesToClearList.size
}

suspend fun GameViewModel.animateFallingFragments() {
    val fragmentAnimationJob = viewModelScope.launch {
        var currentFragments = _gameState.value.fallingFragments.toMutableList()
        if (currentFragments.isEmpty()) return@launch

        for (i in 0 until boardHeight) {
            delay(50)
            val nextFragments = mutableListOf<Pair<Int, Int>>()
            var fragmentsLanded = false
            currentFragments.forEach { (x, y) ->
                val nextY = y + 1
                if (nextY < boardHeight && _gameState.value.board[nextY][x] == 0) {
                    nextFragments.add(Pair(x, nextY))
                } else {
                    nextFragments.add(Pair(x, y))
                    fragmentsLanded = true
                }
            }
            currentFragments = nextFragments.toMutableList()
            _gameState.value = _gameState.value.copy(fallingFragments = currentFragments)
            if (fragmentsLanded) {
                val finalBoard = _gameState.value.board.map { it.clone() }.toTypedArray()
                currentFragments.forEach { (x, y) ->
                    if (y >= 0 && y < boardHeight && x >= 0 && x < boardWidth) {
                        finalBoard[y][x] = 8 // Color for falling fragments
                    }
                }
                _gameState.value = _gameState.value.copy(board = finalBoard, fallingFragments = emptyList())
                return@launch
            }
        }
    }
    fragmentAnimationJob.join()
}

suspend fun GameViewModel.updateScoreAndLevel(linesCleared: Int) {
    if (linesCleared > 0) {
        var points = when (linesCleared) {
            1 -> 100
            2 -> 300
            3 -> 600
            4 -> 1000
            else -> 0
        } * _gameState.value.level

        // Apply Mutation Modifiers
        val multiplier = _gameState.value.selectedMutations.map { it.scoreMultiplier }.fold(1f) { acc, m -> acc * m }
        points = (points * multiplier).toInt()

        for (artifact in _gameState.value.artifacts) {
            if (artifact is IScoreModifier) {
                points = artifact.modifyScore(points, linesCleared, _gameState.value.level)
            }
        }

        val newScore = _gameState.value.currentScore + points

        var currentBoss = _gameState.value.currentBoss
        if (currentBoss != null && !_gameState.value.isDebugMode) {
            currentBoss.requiredLines -= linesCleared
            if (currentBoss.requiredLines <= 0) {
                _gameState.value = _gameState.value.copy(currentScore = _gameState.value.currentScore + 5000 * _gameState.value.level)
                // unlockNextMutation() - REMOVED for Badge Shop system
                addRandomMutationToRun(gameplayDataRepository)
                currentBoss = null
            }
        }

        var newLinesUntilNextLevel = _gameState.value.linesUntilNextLevel - linesCleared
        var newLevel = _gameState.value.level

        if (newLinesUntilNextLevel <= 0 && !_gameState.value.isDebugMode) {
            newLevel++
            newLinesUntilNextLevel += newLevel * 5
            var state = _gameState.value
            _gameState.value.selectedMutations.forEach { mutation ->
                if (mutation is IOnLevelUpHook) {
                    state = mutation.onLevelUp(state, rng)
                }
            }
            _gameState.value.artifacts.forEach { artifact ->
                if (artifact is IOnLevelUpHook) {
                    state = artifact.onLevelUp(state, rng)
                }
            }
            _gameState.value = state

            if (allArtifacts.size > _gameState.value.artifacts.size) {
                val availableArtifacts = allArtifacts.filterNot { _gameState.value.artifacts.contains(it) }
                if (availableArtifacts.size >= 2) {
                    val choices = availableArtifacts.shuffled(rng).take(2)
                    _gameState.value = _gameState.value.copy(artifactChoices = choices)
                    pauseGame()
                }
            }
        }

        if (newLevel % 3 == 0 && currentBoss == null && !_gameState.value.isDebugMode) {
            currentBoss = spawnBoss(newLevel)
        }

        _gameState.value = _gameState.value.copy(
            currentScore = newScore,
            level = newLevel,
            linesUntilNextLevel = newLinesUntilNextLevel,
            currentBoss = currentBoss
        )
    }
}

fun GameViewModel.lockPiece() {
    val currentPiece = _gameState.value.piece ?: return
    val newBoard = _gameState.value.board.map { it.clone() }.toTypedArray()
    for (py in currentPiece.shape.indices) {
        for (px in currentPiece.shape[py].indices) {
            if (currentPiece.shape[py][px] != 0) {
                val boardX = _gameState.value.pieceX + px
                val boardY = _gameState.value.pieceY + py
                if (boardY >= 0 && boardY < boardHeight && boardX >= 0 && boardX < boardWidth) {
                    newBoard[boardY][boardX] = if (_gameState.value.selectedMutations.any { it is ColorblindMutation }) 8 else currentPiece.color
                }
            }
        }
    }
    _gameState.value = _gameState.value.copy(board = newBoard, piece = null, rotationCount = 0)
}

suspend fun GameViewModel.spawnNewPiece(): Boolean {
    var state = _gameState.value

    val pieceToSpawn = state.nextPiece ?: pieces.random(rng)
    val nextPiece = state.secondNextPiece ?: pieces.random(rng)
    val secondNextPiece = pieces.random(rng)

    val startX = boardWidth / 2 - pieceToSpawn.shape[0].size / 2
    val startY = 0

    state = state.copy(
        piece = pieceToSpawn,
        nextPiece = nextPiece,
        secondNextPiece = secondNextPiece,
        pieceX = startX,
        pieceY = startY
    )

    _gameState.value.selectedMutations.forEach { mutation ->
        if (mutation is IOnPieceSpawnHook) {
            state = mutation.onPieceSpawn(state, rng)
        }
    }
    _gameState.value.artifacts.forEach { artifact ->
        if (artifact is IOnPieceSpawnHook) {
            state = artifact.onPieceSpawn(state, rng)
        }
    }

    if (!isValidPosition(state.pieceX, state.pieceY, state.piece!!, state.board)) {
        return false // Game Over
    }

    _gameState.value = state
    updateGhostPiece()
    return true
}

fun GameViewModel.updateGhostPiece() {
    val state = _gameState.value
    var ghostY = state.pieceY

    if (_gameState.value.selectedMutations.any { it is IRequiresGhostPiece } || _gameState.value.artifacts.any { it is IRequiresGhostPiece }) {
        state.piece?.let {
            while (isValidPosition(state.pieceX, ghostY + 1, it, state.board)) {
                ghostY++
            }
        }
    }
    _gameState.value = state.copy(ghostPieceY = ghostY)
}

fun GameViewModel.addRandomMutationToRun(gameplayDataRepository: GameplayDataRepository) {
    viewModelScope.launch {
        val activeMutationNames = gameplayDataRepository.enabledMutations.first()
        val availableMutations = allMutations.filter { activeMutationNames.contains(it.name) && !_gameState.value.selectedMutations.contains(it) }
        if (availableMutations.isNotEmpty()) {
            val newMutation = availableMutations.random(rng)
            _gameState.value = _gameState.value.copy(
                selectedMutations = _gameState.value.selectedMutations + newMutation,
                pendingMutationPopup = newMutation
            )
            pauseGame()
        }
    }
}

fun GameViewModel.spawnBoss(level: Int): GameViewModel.Boss {
    val boss = bosses.random(rng).copy()
    boss.requiredLines = level * 2
    return boss
}
