package com.servacode.directory.core.maps

import kotlin.math.abs

/**
 * Which of the ways back are a choice, and which are the same way drawn twice.
 *
 * A routing engine asked for alternates answers with whatever it found, and what it found is not
 * always a decision anybody would make. Measured against Raqqa's own roads: across the city a car
 * is given three genuinely different ways, ten minutes against nine and a half over nine
 * kilometres against seven. On foot the same request comes back with three lines that differ by
 * three per cent — the same walk, nudged — and offering those as a choice is asking someone to
 * pick between identical things, which is worse than not asking. And on a three-hundred-metre hop
 * one of them arrives half a minute slower, which is not an alternative, it is the wrong way.
 *
 * So an alternative earns its place by being different enough to be a decision and close enough
 * to be a reasonable one.
 */
object RouteChoices {
    /** Enough to choose between, and few enough to read at a glance on a phone. */
    const val MAX_OFFERED = 3

    /**
     * How far apart two ways must be, as a share of the best one's duration, to be two ways.
     *
     * Ten per cent of a ten-minute drive is a minute — worth a tap. Ten per cent of an hour's
     * walk is six minutes, which is why the walk's three-per-cent variants fall away here.
     */
    const val DIFFERENT_ENOUGH = 0.10

    /** And never less than this, so a short trip does not offer seconds as a decision. */
    const val DIFFERENT_ENOUGH_SECONDS = 20.0

    /**
     * How much worse than the best a way may be and still be offered.
     *
     * Past this it is not an alternative anybody weighs; it is simply the slower way, and a list
     * that includes it makes the reader do the engine's work.
     */
    const val TOLERABLY_WORSE = 1.25

    /**
     * The routes worth putting in front of someone, the engine's own preference first.
     *
     * The first is always kept. It is the engine's answer, arrived at with a cost model that
     * knows about turn penalties and road classes, and this does not second-guess it — it only
     * decides what else is worth showing beside it.
     */
    fun worthOffering(routes: List<NavigationRoute>): List<NavigationRoute> {
        val best = routes.firstOrNull() ?: return emptyList()
        val margin = maxOf(best.durationSeconds * DIFFERENT_ENOUGH, DIFFERENT_ENOUGH_SECONDS)
        val kept = mutableListOf(best)
        for (candidate in routes.drop(1)) {
            if (kept.size >= MAX_OFFERED) break
            if (candidate.durationSeconds > best.durationSeconds * TOLERABLY_WORSE) continue
            val isNew = kept.all { abs(it.durationSeconds - candidate.durationSeconds) >= margin }
            if (isNew) kept += candidate
        }
        return kept
    }

    /**
     * Whether offering these is offering anything.
     *
     * One way there is not a choice, and a row of one chip is a control that does nothing.
     */
    fun isAChoice(offered: List<NavigationRoute>): Boolean = offered.size > 1

    /**
     * Which of the offered ways is followed until someone chooses another: the shortest.
     *
     * Not the engine's own first answer. Measured over twelve trips across Raqqa, that answer is
     * not the shortest way in seven of them, and the detour adds up to 6.6 km — on one trip it
     * is 2.8 km longer and a minute slower than a way it returned itself. Shortest and quickest
     * are the same route in eight of the twelve; across all twelve, taking the shortest saves
     * 6.6 km and costs 2.7 minutes, which is 0.55 km saved against 13 seconds lost on an average
     * trip. That is the trade this makes, and it is the owner's decision.
     *
     * Nothing here can pick a way that is much slower, because [worthOffering] has already
     * refused anything more than [TOLERABLY_WORSE] past the engine's own answer — the one trip
     * in the measurement where the shortest way cost two minutes never reaches this function.
     */
    fun preferred(offered: List<NavigationRoute>): Int {
        if (offered.isEmpty()) return 0
        // The earliest of equals, so the engine's own order still breaks a tie.
        return offered.indices.minByOrNull { offered[it].distanceMeters } ?: 0
    }
}
