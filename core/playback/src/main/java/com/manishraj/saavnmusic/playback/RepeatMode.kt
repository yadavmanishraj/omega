package com.manishraj.saavnmusic.playback

import androidx.media3.common.Player

/**
 * UI-facing repeat mode.
 *
 * [PlayerState.repeatMode] carries the ENGINE value (Media3's
 * [Player.REPEAT_MODE_OFF] / [Player.REPEAT_MODE_ONE] /
 * [Player.REPEAT_MODE_ALL] = 0 / 1 / 2). Every interpretation of that
 * Int — label, icon, toggle state — must go through this mapping.
 *
 * BUG-1 (exhaustive QA, E10/E11): the player UI read the raw Int with
 * 1 = "all" and 2 = "one", the exact inverse of Media3's constants,
 * so the UI showed "Repeat one" while the engine was in
 * REPEAT_MODE_ALL (advance at track end) and vice versa. The engine
 * and [PlayerController.cycleRepeat] were always correct; only the
 * display mapping was swapped.
 */
enum class RepeatMode {
    OFF,
    ONE,
    ALL,
}

/** Engine (Media3) value for this UI mode. */
fun RepeatMode.toEngineRepeatMode(): Int =
    when (this) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    }

/** UI mode for an engine (Media3) repeat value; unknown values read as [RepeatMode.OFF]. */
fun repeatModeFromEngine(engineMode: Int): RepeatMode =
    when (engineMode) {
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        else -> RepeatMode.OFF
    }

/**
 * Next engine repeat value in the transport cycle
 * OFF → ALL → ONE → OFF (the order [PlayerController.cycleRepeat]
 * has always applied to the engine).
 */
fun nextEngineRepeatMode(engineMode: Int): Int =
    when (repeatModeFromEngine(engineMode)) {
        RepeatMode.OFF -> Player.REPEAT_MODE_ALL
        RepeatMode.ALL -> Player.REPEAT_MODE_ONE
        RepeatMode.ONE -> Player.REPEAT_MODE_OFF
    }
