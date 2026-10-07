package com.manishraj.saavnmusic.feature.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [cleanHeaderDescription] — the Detail header's guard against
 * upstream's baked metadata string: duplicate artist enumerations
 * and credit lines must never reach the reader as editorial copy,
 * and editorial text must pass through untouched.
 */
class HeaderDescriptionTextTest {
    private val albumSubtitle = "Jeet Gannguli, Mithoon, and Ankit Tiwari"

    @Test
    fun duplicateArtistsRemovedYearAndTypeKept() {
        assertEquals(
            "2013 · Hindi Album",
            cleanHeaderDescription(
                "2013 · Hindi Album · Jeet Gannguli, Mithoon, and Ankit Tiwari",
                albumSubtitle,
            ),
        )
    }

    @Test
    fun duplicateArtistsRemovedWithDifferentConjunctionStyle() {
        // Subtitle uses ", and"; the description enumerates with "&".
        assertEquals(
            "2013 · Hindi Album",
            cleanHeaderDescription(
                "2013 · Hindi Album · Jeet Gannguli, Mithoon & Ankit Tiwari",
                albumSubtitle,
            ),
        )
    }

    @Test
    fun duplicateArtistSubsetRemoved() {
        // The description names only two of the subtitle's three
        // artists — still a restatement of the subtitle, not a fact.
        assertEquals(
            "2013",
            cleanHeaderDescription("2013 · Jeet Gannguli & Mithoon", albumSubtitle),
        )
    }

    @Test
    fun photoCreditDescriptionBecomesEmpty() {
        assertNull(
            cleanHeaderDescription(
                "Artists On Cover: Sidharth Malhotra & Kiara Advani",
                "Playlist · 45 songs",
            ),
        )
    }

    @Test
    fun copyrightLineSegmentDroppedFactsKept() {
        assertEquals(
            "2019 · Hindi Album",
            cleanHeaderDescription(
                "2019 · ℗ 2019 T-Series · Hindi Album",
                "Some Artist",
            ),
        )
    }

    @Test
    fun labelSegmentDroppedFactsKept() {
        assertEquals(
            "Greatest Hits",
            cleanHeaderDescription("Greatest Hits · Label: Sony Music", "Various Artists"),
        )
    }

    @Test
    fun midSegmentCreditLabelCutEditorialPrefixKept() {
        // No "·" separator: the credit label trails an editorial
        // sentence inside ONE segment — the sentence survives, the
        // credit tail does not.
        assertEquals(
            "Greatest Hits.",
            cleanHeaderDescription("Greatest Hits. Label: Sony Music", "Various Artists"),
        )
    }

    @Test
    fun cleanDescriptionPassesThroughByteIdentical() {
        // Irregular spacing included on purpose: a description with
        // nothing to remove must come back exactly as upstream sent
        // it — the cleaner never rewrites editorial text.
        val clean = "Recorded  live in Mumbai · 2019"
        assertEquals(clean, cleanHeaderDescription(clean, "Someone Else"))
        assertEquals(
            "2013 · Hindi Album",
            cleanHeaderDescription("2013 · Hindi Album", albumSubtitle),
        )
    }

    @Test
    fun artistMentionInsideProseIsKept() {
        // "Performed by …" is prose ABOUT the artist, not an
        // enumeration OF the subtitle — it must survive.
        val prose = "2013 · Performed by Jeet Gannguli"
        assertEquals(prose, cleanHeaderDescription(prose, albumSubtitle))
    }

    @Test
    fun nullAndBlankDescriptionsAreSafe() {
        assertNull(cleanHeaderDescription(null, albumSubtitle))
        assertNull(cleanHeaderDescription("", albumSubtitle))
        assertNull(cleanHeaderDescription("   ", albumSubtitle))
        assertNull(cleanHeaderDescription(null, null))
    }

    @Test
    fun separatorHygieneOnRebuild() {
        val cleaned =
            cleanHeaderDescription(
                "· 2013 ·· Hindi Album · Jeet Gannguli, Mithoon, and Ankit Tiwari ·",
                albumSubtitle,
            )
        assertEquals("2013 · Hindi Album", cleaned)
        assertTrue(cleaned!!.startsWith("2013"))
        assertTrue(!cleaned.contains("··"))
        assertTrue(!cleaned.contains("  "))
    }
}
