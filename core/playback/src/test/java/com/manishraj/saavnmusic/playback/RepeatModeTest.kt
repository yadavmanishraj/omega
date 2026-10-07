package com.manishraj.saavnmusic.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The repeat-mode mapping (BUG-1): the UI once read the raw engine
 * Int with ONE/ALL inverted. These tests pin the mapping to
 * Media3's actual constant values in both directions.
 */
class RepeatModeTest {
    @Test
    fun `engine values map to the modes Media3 defines`() {
        // Media3: REPEAT_MODE_OFF = 0, REPEAT_MODE_ONE = 1, REPEAT_MODE_ALL = 2.
        assertEquals(RepeatMode.OFF, repeatModeFromEngine(Player.REPEAT_MODE_OFF))
        assertEquals(RepeatMode.ONE, repeatModeFromEngine(Player.REPEAT_MODE_ONE))
        assertEquals(RepeatMode.ALL, repeatModeFromEngine(Player.REPEAT_MODE_ALL))
    }

    @Test
    fun `modes map back to the same engine values`() {
        assertEquals(Player.REPEAT_MODE_OFF, RepeatMode.OFF.toEngineRepeatMode())
        assertEquals(Player.REPEAT_MODE_ONE, RepeatMode.ONE.toEngineRepeatMode())
        assertEquals(Player.REPEAT_MODE_ALL, RepeatMode.ALL.toEngineRepeatMode())
    }

    @Test
    fun `mapping round-trips for every mode`() {
        for (mode in RepeatMode.entries) {
            assertEquals(mode, repeatModeFromEngine(mode.toEngineRepeatMode()))
        }
    }

    @Test
    fun `unknown engine values read as OFF`() {
        assertEquals(RepeatMode.OFF, repeatModeFromEngine(99))
        assertEquals(RepeatMode.OFF, repeatModeFromEngine(-1))
    }

    @Test
    fun `cycle runs OFF to ALL to ONE to OFF`() {
        var mode = Player.REPEAT_MODE_OFF
        mode = nextEngineRepeatMode(mode)
        assertEquals(Player.REPEAT_MODE_ALL, mode)
        mode = nextEngineRepeatMode(mode)
        assertEquals(Player.REPEAT_MODE_ONE, mode)
        mode = nextEngineRepeatMode(mode)
        assertEquals(Player.REPEAT_MODE_OFF, mode)
    }

    @Test
    fun `displayed cycle matches the engine cycle`() {
        // What the user sees, step by step, must be the mode the
        // engine is actually in after each tap (BUG-1's symptom was
        // the label saying One while the engine was in All).
        var engine = Player.REPEAT_MODE_OFF
        val seen = mutableListOf<RepeatMode>()
        repeat(3) {
            engine = nextEngineRepeatMode(engine)
            seen += repeatModeFromEngine(engine)
        }
        assertEquals(listOf(RepeatMode.ALL, RepeatMode.ONE, RepeatMode.OFF), seen)
    }
}
