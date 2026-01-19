package net.ljga.projects.games.tetris.ui.game

import java.util.Locale

fun longToSeedString(seed: Long): String {
    // Convert to Base36 (0-9, a-z) and uppercase
    return seed.toString(36).uppercase(Locale.ROOT)
}

fun seedStringToLong(seedString: String): Long {
    return try {
        seedString.lowercase(Locale.ROOT).toLong(36)
    } catch (e: NumberFormatException) {
        0L
    }
}
