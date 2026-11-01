package com.cinestream.tv

/** Only indices from the already-resolved, selected title/episode are eligible. Never loop. */
internal fun nextSourceIndex(current: Int, attempted: Set<Int>, count: Int): Int? =
    ((current + 1 until count) + (0 until current.coerceAtMost(count))).firstOrNull { it !in attempted }
