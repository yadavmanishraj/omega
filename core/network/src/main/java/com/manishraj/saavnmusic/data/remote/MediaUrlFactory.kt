package com.manishraj.saavnmusic.data.remote

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Media + image URL construction, ported 1:1 from the jiosaavn-api
 * repository's `link.helper.ts` (docs/sdlc/UPSTREAM_SPEC.md §3.2-§3.3).
 *
 * Upstream stores an obfuscated media URL per song: DES in ECB mode
 * (key bytes are the ASCII string "38346591"; the IV the repo passes is
 * ignored by ECB), standard Base64. The decrypted URL is a plain CDN
 * link ending in `_96.mp4`; the quality ladder is synthesised by
 * replacing the first `_96` token. Verified live (UPSTREAM_VALIDATION
 * §2): range GETs return 206 for the synthesised rungs.
 */
object MediaUrlFactory {
    private val QUALITY_TOKENS = listOf("12", "48", "96", "160", "320")

    /** Decrypts an upstream `encrypted_media_url` to the plain base CDN URL, or null when impossible. */
    fun decryptToBaseUrl(encryptedMediaUrl: String?): String? {
        if (encryptedMediaUrl.isNullOrBlank()) return null
        return try {
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec("38346591".toByteArray(Charsets.US_ASCII), "DES"),
            )
            val bytes = cipher.doFinal(Base64.getDecoder().decode(encryptedMediaUrl.trim()))
            val plain = String(bytes, Charsets.UTF_8)
            // Guard against padding/byte-string leftovers: cut right after ".mp4" when present.
            val mp4End = plain.indexOf(".mp4")
            if (mp4End >= 0) plain.substring(0, mp4End + 4) else plain.trim { it <= ' ' }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * The full quality ladder as (label, url) pairs, ascending.
     * When the song's `320kbps` flag is false the top rung is dropped —
     * it may not exist upstream (spec §3.2 improvement over the repo,
     * which returns all five blindly).
     */
    fun downloadLadder(
        encryptedMediaUrl: String?,
        is320kbps: Boolean,
    ): List<Pair<String, String>> {
        val base = decryptToBaseUrl(encryptedMediaUrl) ?: return emptyList()
        return QUALITY_TOKENS
            .filter { token -> is320kbps || token != "320" }
            .map { token -> "${token}kbps" to base.replaceFirst("_96", "_$token") }
    }

    /** Image ladder as (quality, url) pairs: 50x50, 150x150, 500x500 (repo order). */
    fun imageLadder(link: String?): List<Pair<String, String>> {
        if (link.isNullOrBlank()) return emptyList()
        return listOf("50x50", "150x150", "500x500").map { quality ->
            quality to
                link
                    .replace(Regex("150x150|50x50"), quality)
                    .replace(Regex("^http://"), "https://")
        }
    }

    /** Largest available artwork URL for a raw upstream image link. */
    fun bestImage(link: String?): String? = imageLadder(link).lastOrNull { it.second.isNotBlank() }?.second
}
