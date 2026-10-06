package com.manishraj.saavnmusic

import com.manishraj.saavnmusic.data.remote.MediaUrlFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class MediaUrlFactoryTest {
    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec("38346591".toByteArray(Charsets.US_ASCII), "DES"),
        )
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun decryptRoundtripsToPlainCdnUrl() {
        val url = "https://aac.saavncdn.com/450/0123456789abcdef0123456789abcdef_96.mp4"
        assertEquals(url, MediaUrlFactory.decryptToBaseUrl(encrypt(url)))
    }

    @Test
    fun ladderSynthesisesAllRungsByReplacingFirstToken() {
        val url = "https://aac.saavncdn.com/196/abc_96.mp4"
        val ladder = MediaUrlFactory.downloadLadder(encrypt(url), is320kbps = true)
        assertEquals(listOf("12kbps", "48kbps", "96kbps", "160kbps", "320kbps"), ladder.map { it.first })
        assertTrue(ladder.all { it.second.startsWith("https://aac.saavncdn.com/196/abc_") })
        assertEquals("https://aac.saavncdn.com/196/abc_320.mp4", ladder.last().second)
        assertEquals(url, ladder.first { it.first == "96kbps" }.second)
    }

    @Test
    fun ladderCapsAt160When320FlagIsFalse() {
        val url = "https://aac.saavncdn.com/450/abc_96.mp4"
        val ladder = MediaUrlFactory.downloadLadder(encrypt(url), is320kbps = false)
        assertFalse(ladder.any { it.first == "320kbps" })
        assertEquals("160kbps", ladder.last().first)
    }

    @Test
    fun emptyOrGarbageInputYieldsNoUrls() {
        assertNull(MediaUrlFactory.decryptToBaseUrl(null))
        assertNull(MediaUrlFactory.decryptToBaseUrl(""))
        assertTrue(MediaUrlFactory.downloadLadder("not-base64!!", is320kbps = true).isEmpty())
        assertTrue(MediaUrlFactory.downloadLadder(null, is320kbps = true).isEmpty())
    }

    @Test
    fun imageLadderSwapsSizeTokenAndForcesHttps() {
        val ladder = MediaUrlFactory.imageLadder("http://c.saavncdn.com/artists/x-150x150.jpg")
        assertEquals(
            listOf(
                "50x50" to "https://c.saavncdn.com/artists/x-50x50.jpg",
                "150x150" to "https://c.saavncdn.com/artists/x-150x150.jpg",
                "500x500" to "https://c.saavncdn.com/artists/x-500x500.jpg",
            ),
            ladder,
        )
        assertEquals("https://c.saavncdn.com/artists/x-500x500.jpg", MediaUrlFactory.bestImage("http://c.saavncdn.com/artists/x-150x150.jpg"))
        assertNull(MediaUrlFactory.bestImage(null))
    }
}
