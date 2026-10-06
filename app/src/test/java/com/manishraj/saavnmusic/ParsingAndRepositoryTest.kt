package com.manishraj.saavnmusic
import com.manishraj.saavnmusic.data.remote.dto.*; import com.manishraj.saavnmusic.data.repository.toDomain; import com.manishraj.saavnmusic.domain.*
import kotlinx.serialization.json.Json; import org.junit.Assert.*; import org.junit.Test
class ParsingAndRepositoryTest {
    private val json=Json{ignoreUnknownKeys=true; coerceInputValues=true}
    // Fixture trimmed from a real saavn.dev /api/search/songs response shape (SongModel in jiosaavn-api).
    private val fixture="""{"success":true,"data":{"total":1,"start":0,"results":[{"id":"yDeAS8Eh","name":"Test Song","type":"song","year":"2022","duration":214,"language":"hindi","hasLyrics":true,"album":{"id":"1","name":"Test Album"},"artists":{"primary":[{"id":"a1","name":"Test Singer"}],"featured":[],"all":[{"id":"a1","name":"Test Singer"}]},"image":[{"quality":"50x50","url":"https://x/50x50.jpg"},{"quality":"500x500","url":"https://x/500x500.jpg"}],"downloadUrl":[{"quality":"48kbps","url":"https://x/_48.mp4"},{"quality":"96kbps","url":"https://x/_96.mp4"},{"quality":"160kbps","url":"https://x/_160.mp4"},{"quality":"320kbps","url":"https://x/_320.mp4"}]}]}}"""
    @Test fun parsesSongAndPicksHighestStreamAndArtwork(){ val r=json.decodeFromString<ApiResponse<SearchResultDto<SongDto>>>(fixture); val s=r.data!!.results.first().toDomain(); assertEquals("Test Song",s.name); assertEquals("Test Singer",s.artist); assertTrue(s.streamUrl!!.endsWith("_320.mp4")); assertTrue(s.imageUrl!!.contains("500x500")); assertEquals(214L,s.durationSec) }
    @Test fun qualitySelectionFallsBackToBest(){ val links=listOf(LinkDto("96kbps","u96"),LinkDto("320kbps","u320")); assertEquals("u96",links.urlForQuality("96kbps")); assertEquals("u320",links.urlForQuality("999kbps")) }
    @Test fun downloadNamingMirrorsJiosaavnDl(){ val s=Song("1","A/B: Song","Some Artist",null,null,null,null); assertEquals("Some Artist - AB Song.m4a",downloadFileName(s)); assertEquals("01. AB Song.m4a",downloadFileName(s,1,10)); assertEquals("3:45",formatDuration(225)) }
}
