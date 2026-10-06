package com.manishraj.saavnmusic

import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.downloadFileName
import com.manishraj.saavnmusic.domain.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelUtilsTest {
    @Test
    fun downloadNamingMirrorsJiosaavnDl() {
        val s = Song("1", "A/B: Song", "Some Artist", null, null, null, null)
        assertEquals("Some Artist - AB Song.m4a", downloadFileName(s))
        assertEquals("01. AB Song.m4a", downloadFileName(s, 1, 10))
    }

    @Test
    fun formatsDurations() {
        assertEquals("3:45", formatDuration(225))
        assertEquals("", formatDuration(null))
        assertEquals("0:07", formatDuration(7))
    }
}
