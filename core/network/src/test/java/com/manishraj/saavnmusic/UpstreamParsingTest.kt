package com.manishraj.saavnmusic

import com.manishraj.saavnmusic.data.remote.cleanedLyrics
import com.manishraj.saavnmusic.data.remote.dto.RawAlbumDto
import com.manishraj.saavnmusic.data.remote.dto.RawArtistPageDto
import com.manishraj.saavnmusic.data.remote.dto.RawBrowseModulesDto
import com.manishraj.saavnmusic.data.remote.dto.RawGlobalSearchDto
import com.manishraj.saavnmusic.data.remote.dto.RawLyricsDto
import com.manishraj.saavnmusic.data.remote.dto.RawPagedDto
import com.manishraj.saavnmusic.data.remote.dto.RawPlaylistDto
import com.manishraj.saavnmusic.data.remote.dto.RawSongDto
import com.manishraj.saavnmusic.data.remote.dto.RawStationEntryDto
import com.manishraj.saavnmusic.data.remote.toDomain
import com.manishraj.saavnmusic.data.remote.toHomeContent
import com.manishraj.saavnmusic.domain.TopResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Parsing/mapping tests against raw upstream payload shapes (trimmed
 * versions of the live responses recorded in UPSTREAM_VALIDATION.md).
 * The encrypted media URL in fixtures is generated at runtime from a
 * known plain URL, so the DES path is exercised end to end.
 */
class UpstreamParsingTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec("38346591".toByteArray(Charsets.US_ASCII), "DES"),
        )
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }

    private val encA = encrypt("https://aac.saavncdn.com/450/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa_96.mp4")
    private val encB = encrypt("https://aac.saavncdn.com/999/bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb_96.mp4")

    // search.getResults shape: stringly-typed numbers, "total" as a STRING
    // here (upstream is inconsistent), has_lyrics/320kbps as strings.
    private val searchSongsFixture =
        """
        {"total":"4675","start":-4,"results":[
          {"id":"aRZbUYD7","title":"Tum Hi Ho","subtitle":"Mithoon, Arijit Singh","type":"song",
           "perma_url":"https://www.jiosaavn.com/song/tum-hi-ho/x","image":"https://c.saavncdn.com/430/Aashiqui-2-150x150.jpg",
           "language":"hindi","year":"2013","play_count":"60321486","explicit_content":"0",
           "more_info":{"duration":"262","has_lyrics":"true","lyrics_id":"","320kbps":"true",
             "encrypted_media_url":"$encA","album":"Aashiqui 2","album_id":"1",
             "artistMap":{"primary_artists":[{"id":"art1","name":"Arijit Singh"}],"featured_artists":[],
               "artists":[{"id":"art1","name":"Arijit Singh"}]}}},
          {"id":"s2","title":"Gehra Hua (From &quot;Dhurandhar&quot;)","type":"song",
           "image":"http://c.saavncdn.com/001/X-150x150.jpg","language":"hindi","year":"2025",
           "more_info":{"duration":"200","has_lyrics":"false","320kbps":"false",
             "encrypted_media_url":"$encB","album":"Dhurandhar",
             "artistMap":{"primary_artists":[{"id":"art2","name":"Singer &amp; Co"}],"featured_artists":[],"artists":[]}}}
        ]}
        """.trimIndent()

    @Test
    fun parsesSearchSongsAndMapsDomain() {
        val paged = json.decodeFromString<RawPagedDto<RawSongDto>>(searchSongsFixture)
        assertEquals(4675, paged.total)
        assertEquals(2, paged.results.size)

        val first = paged.results[0].toDomain()
        assertEquals("Tum Hi Ho", first.name)
        assertEquals("Arijit Singh", first.artist)
        assertEquals(262L, first.durationSec)
        assertTrue(first.hasLyrics)
        assertTrue(first.imageUrl!!.contains("500x500"))
        // Full ladder incl. 320 (flag true); stream fallback prefers 160.
        assertEquals(5, first.downloadUrls.size)
        assertTrue(first.streamUrl!!.endsWith("_160.mp4"))

        val second = paged.results[1].toDomain()
        // HTML entities unescaped for display.
        assertEquals("Gehra Hua (From \"Dhurandhar\")", second.name)
        assertEquals("Singer & Co", second.artist)
        assertFalse(second.hasLyrics)
        // 320 flag false -> ladder capped, no _320 URL anywhere.
        assertEquals(4, second.downloadUrls.size)
        assertFalse(second.downloadUrls.any { it.second.contains("_320") })
        // http image upgraded to https.
        assertTrue(second.imageUrl!!.startsWith("https://"))
    }

    @Test
    fun pagedTotalAcceptsPlainNumbersToo() {
        val paged = json.decodeFromString<RawPagedDto<RawSongDto>>("""{"total":7,"start":0,"results":[]}""")
        assertEquals(7, paged.total)
    }

    // content.getAlbumDetails shape: list_count (String) is the true total.
    private val albumFixture =
        """
        {"id":"38682222","title":"Bhediya","header_desc":"A desc","type":"album",
         "image":"https://c.saavncdn.com/430/Bhediya-150x150.jpg","language":"hindi","year":"2022",
         "list_count":"6","list_type":"album",
         "list":[{"id":"x1","title":"Thumkeshwari","more_info":{"duration":"180","320kbps":"true","encrypted_media_url":"$encA",
            "artistMap":{"primary_artists":[{"id":"art1","name":"Arijit Singh"}],"featured_artists":[],"artists":[]}}}],
         "more_info":{"song_count":"6","artistMap":{"primary_artists":[{"id":"art9","name":"Sachin-Jigar"}],"featured_artists":[],"artists":[]}}}
        """.trimIndent()

    @Test
    fun parsesAlbumDetailsUsingListCountAsTotal() {
        val album = json.decodeFromString<RawAlbumDto>(albumFixture).toDomain()
        assertEquals("Bhediya", album.name)
        assertEquals("Sachin-Jigar", album.artist)
        assertEquals(6, album.songCount)
        assertEquals(1, album.songs.size)
        assertEquals("Thumkeshwari", album.songs[0].name)
    }

    @Test
    fun lyricsDecodeAndCleanBrSeparators() {
        val dto =
            json.decodeFromString<RawLyricsDto>(
                """{"lyrics":"Tum hi ho<br>Ab tum hi ho","lyrics_copyright":"Lyrics powered by JioSaavn","snippet":"Tum hi ho"}""",
            )
        assertEquals("Tum hi ho\nAb tum hi ho", dto.cleanedLyrics())
        // Error bodies (HTTP 200, no lyrics key) decode to null lyrics.
        val error = json.decodeFromString<RawLyricsDto>("""{"status":"failure","error":{"msg":"Something went wrong"}}""")
        assertNull(error.cleanedLyrics())
    }

    @Test
    fun stationEntriesDecodeFromNumericKeyedMap() {
        val element =
            json.parseToJsonElement(
                """{"stationid":"st1","0":{"song":{"id":"aRZbUYD7","title":"Tum Hi Ho"}},"1":{"song":{"id":"s2","title":"Other"}}}""",
            )
        val obj = element as kotlinx.serialization.json.JsonObject
        val songs =
            obj.entries
                .filter { it.key.toIntOrNull() != null }
                .sortedBy { it.key.toInt() }
                .map { json.decodeFromJsonElement<RawStationEntryDto>(it.value).song }
        assertEquals(listOf("aRZbUYD7", "s2"), songs.map { it?.id })
        // The current (broken) upstream shape carries an error key instead.
        val broken =
            json.parseToJsonElement(
                """{"stationid":"st1","error":"No new song found for current radio."}""",
            ) as kotlinx.serialization.json.JsonObject
        assertTrue(broken.entries.none { it.key.toIntOrNull() != null })
    }

    @Test
    fun globalSearchSectionsMapToLiteDomain() {
        val fixture =
            """
            {"topquery":{"data":[{"id":"t1","title":"Arijit Singh Hits","image":"https://c.saavncdn.com/x-150x150.jpg",
                "more_info":{"album":"Hits","primary_artists":"Arijit Singh","language":"hindi"}}],"position":0},
             "songs":{"data":[],"position":1},
             "albums":{"data":[{"id":"al1","title":"Aashiqui 2","more_info":{"music":"Mithoon","year":"2013"}}],"position":2},
             "artists":{"data":[{"id":"art1","title":"Arijit Singh","image":"https://c.saavncdn.com/a-150x150.jpg"}],"position":3},
             "playlists":{"data":[],"position":4},
             "episodes":{"data":[],"position":5},"shows":{"data":[],"position":6}}
            """.trimIndent()
        val result = json.decodeFromString<RawGlobalSearchDto>(fixture).toDomain()
        val top = result.topResults.single()
        assertTrue(top is TopResult.SongResult)
        assertEquals("Arijit Singh Hits", (top as TopResult.SongResult).song.name)
        assertNull(top.song.streamUrl)
        assertEquals("Mithoon", result.albums.single().artist)
        assertEquals("Arijit Singh", result.artists.single().name)
    }

    @Test
    fun globalSearchTopQueryKeepsEntityTypes() {
        // Regression (A17 audit F-02): topquery is mixed-type. An
        // artist top hit used to be force-mapped to a Song carrying
        // the ARTIST id — the row rendered as a song, its tap tried
        // to resolve the artist id as a song, failed silently, and
        // the highest-traffic row in Search was a dead control.
        val fixture =
            """
            {"topquery":{"data":[
                {"id":"art1","title":"Arijit Singh","type":"artist","image":"https://c.saavncdn.com/a-150x150.jpg"},
                {"id":"al1","title":"Aashiqui 2","type":"album","image":"https://c.saavncdn.com/x-150x150.jpg",
                 "more_info":{"music":"Mithoon","year":"2013"}},
                {"id":"pl1","title":"Romance Hits","type":"playlist","image":"https://c.saavncdn.com/p-150x150.jpg"},
                {"id":"","title":"Id-less","type":"artist"}
            ],"position":0},
             "songs":{"data":[],"position":1},
             "albums":{"data":[],"position":2},
             "artists":{"data":[],"position":3},
             "playlists":{"data":[],"position":4}}
            """.trimIndent()
        val result = json.decodeFromString<RawGlobalSearchDto>(fixture).toDomain()
        // The id-less item is dropped: never render a row that
        // cannot be acted on.
        assertEquals(3, result.topResults.size)
        val artist = result.topResults[0]
        assertTrue(artist is TopResult.ArtistResult)
        assertEquals("art1", artist.id)
        assertEquals("Arijit Singh", (artist as TopResult.ArtistResult).artist.name)
        val album = result.topResults[1]
        assertTrue(album is TopResult.AlbumResult)
        assertEquals("al1", album.id)
        assertEquals("Mithoon", (album as TopResult.AlbumResult).album.artist)
        val playlist = result.topResults[2]
        assertTrue(playlist is TopResult.PlaylistResult)
        assertEquals("pl1", playlist.id)
    }

    @Test
    fun artistPageMapsSingles() {
        // Regression (A17 audit F-14): the artist DTO parsed
        // `singles` but the domain mapper dropped them, so a whole
        // catalogue slice never reached the artist screen.
        val fixture =
            """
            {"artistId":"art1","name":"Arijit Singh","follower_count":"107959415",
             "topSongs":[{"id":"s1","title":"Tum Hi Ho"}],
             "topAlbums":[{"id":"al1","title":"Aashiqui 2"}],
             "singles":[{"id":"sg1","title":"Single One"},{"id":"sg2","title":"Single Two"}]}
            """.trimIndent()
        val artist = json.decodeFromString<RawArtistPageDto>(fixture).toDomain(json)
        assertEquals(listOf("sg1", "sg2"), artist.singles.map { it.id })
        assertEquals(listOf("Single One", "Single Two"), artist.singles.map { it.name })
        assertEquals(listOf("s1"), artist.topSongs.map { it.id })
    }

    @Test
    fun browseModulesAreClassifiedByShapeNotType() {
        val fixture =
            """
            {"new_trending":[
               {"id":"s1","title":"Song One","type":"album","image":"https://c.saavncdn.com/1-150x150.jpg",
                "more_info":{"encrypted_media_url":"$encA","duration":"200","has_lyrics":"true","320kbps":"true",
                  "artistMap":{"primary_artists":[{"id":"art1","name":"Artist One","image":"https://c.saavncdn.com/a-150x150.jpg"}],"featured_artists":[],"artists":[]}}},
               {"id":"al1","title":"Album One","type":"song","image":"https://c.saavncdn.com/2-150x150.jpg",
                "more_info":{"song_count":"9","artistMap":{"primary_artists":[{"id":"art1","name":"Artist One"}],"featured_artists":[],"artists":[]}}},
               {"id":"pl1","title":"Playlist One","type":"song","image":"https://c.saavncdn.com/3-150x150.jpg",
                "more_info":{"song_count":"37"}}
             ],
             "new_albums":[],
             "charts":[],
             "top_playlists":[
               {"id":"pl2","title":"Top Playlist","image":"https://c.saavncdn.com/4-150x150.jpg","more_info":{"song_count":"45"}}
             ],
             "browse_discover":[]}
            """.trimIndent()
        val home = json.decodeFromString<RawBrowseModulesDto>(fixture).toHomeContent(json)
        assertEquals(listOf("Song One"), home.trendingSongs.map { it.name })
        assertEquals(listOf("Album One"), home.albums.map { it.name })
        assertEquals(9, home.albums.single().songCount)
        assertEquals(setOf("Playlist One", "Top Playlist"), home.playlists.map { it.name }.toSet())
        // Artists rail derived from the trending song's artist map.
        assertEquals(listOf("Artist One"), home.artists.map { it.name })
        assertTrue(
            home.trendingSongs
                .single()
                .downloadUrls
                .isNotEmpty(),
        )
    }

    // Regression (on-device QA, 2026-10-07): search.getAlbumResults
    // returns "list": "" (empty string) on album items; decoding that
    // as List<RawSongDto> threw and took down the whole Search screen.
    @Test
    fun albumSearchResultWithStringListDecodes() {
        val fixture =
            """
            {"total":2,"start":0,"results":[
              {"id":"al1","title":"Album One","type":"album","list_count":"0","list":""},
              {"id":"al2","title":"Album Two","type":"album","list_count":"1",
               "list":[{"id":"s1","title":"Song One","type":"song"}]}
            ]}
            """.trimIndent()
        val page = json.decodeFromString<RawPagedDto<RawAlbumDto>>(fixture)
        assertEquals(2, page.results.size)
        assertTrue(page.results[0].list.isEmpty())
        assertEquals(listOf("Song One"), page.results[1].list.map { it.title })
    }

    @Test
    fun playlistWithStringListDecodes() {
        val fixture =
            """
            {"id":"pl1","title":"Playlist One","type":"playlist","list_count":"37","list":""}
            """.trimIndent()
        val playlist = json.decodeFromString<RawPlaylistDto>(fixture)
        assertEquals("Playlist One", playlist.title)
        assertTrue(playlist.list.isEmpty())
    }
}
