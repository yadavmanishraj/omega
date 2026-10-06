# Upstream Spec — How `jiosaavn-api` Talks Directly to JioSaavn (for in-app replication)

> **Purpose:** implementation-ready specification for making the Android app (Omega) call
> **JioSaavn's upstream servers directly**, replicating exactly what `sumitkolhe/jiosaavn-api`
> does internally — **no hosted wrapper instance (saavn.dev, saavn.sumit.co, self-hosted) involved at all.**
>
> **Source of truth:** the repository source code, read file-by-file for this spec.
> Clone used: `~/workspace/jiosaavn-study/jiosaavn-api` (the path `~/workspace/sdlc/api/jiosaavn-api`
> in the task brief did not exist; the study clone from the earlier API research is the same repo).
> Every claim cites `file:line` in that repo. Companion doc: `API_RESEARCH.md` (wrapper-level view).
> Supplementary evidence for lyrics only (the wrapper never fetches lyrics): `bunnykek/jiosaavn-dl/jiosaavn.py`,
> same study folder. Live probes run for this spec are marked **[probe 2026-10-07]**.
>
> **Mental model change vs. the old app:** there is no `{success, data}` envelope, no camelCase DTOs,
> no REST paths. There is exactly **one GET endpoint** — `https://www.jiosaavn.com/api.php` —
> multiplexed by a `__call` parameter. Responses are raw, snake_case, and **stringly-typed**
> (most numbers/booleans arrive as JSON strings). All mapping logic the wrapper did in
> `create*Payload()` helpers must now happen in the app's repository layer.

---

## 1. The single request builder — `useFetch` (`src/common/helpers/fetch.helper.ts`)

Every upstream call in the repo goes through this one function. Replicate it exactly.

**Base URL** (`fetch.helper.ts:18`): `https://www.jiosaavn.com/api.php`

**Fixed query parameters, appended first, in this order** (`fetch.helper.ts:20-24`):

| Param | Value | Notes |
|---|---|---|
| `__call` | the endpoint name, e.g. `search.getResults` | from `src/common/constants/endpoint.constant.ts` (full table in §2) |
| `_format` | `json` | always |
| `_marker` | `0` | always |
| `api_version` | `4` | always |
| `ctx` | `web6dot0` (default) or `android` | `ApiContextEnum` (`src/common/enums/context.enum.ts:1-4`): `WEB6DOT0='web6dot0'`, `ANDROID='android'`. Only the two radio/station calls pass `android` (§4.9); everything else uses the default |

**Call-specific params** are then appended in object order (`fetch.helper.ts:26`), values stringified.
Full inventory per capability in §4. Params the repo **never sends** (checked by source search — do not add them):
`includeMetaTags`, any `language`/`lang` request param, any cookie, any auth token/key. (`language` exists only
as a *response* field.) No `n`/`p` short names except where §4 says so.

**Headers** (`fetch.helper.ts:30-32`) — exactly two:

| Header | Value |
|---|---|
| `Content-Type` | `application/json` (yes, even on GET — replicate or omit; harmless either way, repo sends it) |
| `User-Agent` | **one random entry per request** from the pool in `src/common/constants/user-agents.constant.ts` — ~100 real browser UA strings (Chrome/Edge/Firefox/Safari, Windows/macOS/Linux/Android/iOS; file is 102 lines). Selection: `userAgents[Math.floor(Math.random() * userAgents.length)]` (`fetch.helper.ts:28`) |

**Explicitly absent in the repo** (verified by source search over all of `src/`): no cookies, no
`Authorization`, no `Referer`, no retry logic, no timeout/abort, no rate limiting, no response caching.
The app must add its own politeness (debounce, caching, ≤ a few parallel calls) — see §8.

**Response handling in repo:** `response.json()` unconditionally; use-cases detect failure by
*absence of expected data* (`if (!data) throw 404`, `if (!data.songs?.length) throw 404`) — upstream
signals many errors as **HTTP 200 with an error/empty body**, not as HTTP status codes. The Android
client must do the same: parse first, validate expected keys, treat missing/empty as "not found".

**Kotlin mapping (transport):**

```kotlin
// One Retrofit/OkHttp service for everything:
@GET("api.php")
suspend fun call(
    @Query("__call") call: String,
    @Query("ctx") ctx: String = "web6dot0",
    @QueryMap params: Map<String, String>,
): JsonObject  // or typed DTO per call — see per-capability sections
// Plus an OkHttp interceptor that appends _format=json, _marker=0, api_version=4 to EVERY request,
// and sets a desktop-browser User-Agent header (rotate from a small built-in list, like the repo pool).
// Base URL: https://www.jiosaavn.com/
```

---

## 2. Endpoint constant table (`src/common/constants/endpoint.constant.ts:1-32`)

| Constant | `__call` value | Used by repo? |
|---|---|---|
| `search.all` | `autocomplete.get` | ✅ §4.1 |
| `search.songs` | `search.getResults` | ✅ §4.2 |
| `search.albums` | `search.getAlbumResults` | ✅ §4.3 |
| `search.artists` | `search.getArtistResults` | ✅ §4.4 |
| `search.playlists` | `search.getPlaylistResults` | ✅ §4.5 |
| `songs.id` | `song.getDetails` | ✅ §4.6 |
| `songs.link` | `webapi.get` | ✅ §4.6/§4.12 (with `type=song`) |
| `songs.suggestions` | `webradio.getSong` | ✅ §4.9 (after station) |
| `songs.station` | `webradio.createEntityStation` | ✅ §4.9 |
| `songs.lyrics` | `lyrics.getLyrics` | ❌ **constant only — no code path calls it** (§5) |
| `albums.id` | `content.getAlbumDetails` | ✅ §4.7 |
| `albums.link` | `webapi.get` | ✅ (with `type=album`) |
| `artists.id` | `artist.getArtistPageDetails` | ✅ §4.8 |
| `artists.link` | `webapi.get` | ✅ (with `type=artist`) |
| `artists.songs` | `artist.getArtistMoreSong` | ✅ §4.8 |
| `artists.albums` | `artist.getArtistMoreAlbum` | ✅ §4.8 |
| `playlists.id` | `playlist.getDetails` | ✅ §4.10 |
| `playlists.link` | `webapi.get` | ✅ (with `type=playlist`) |
| `modules` | `content.getBrowseModules` | ❌ **constant only — never called** (§6) |
| `trending` | `content.getTrending` | ❌ **constant only — never called** (§6) |

---

## 3. Shared raw building blocks

### 3.1 The raw Song object (the single most important shape)

Defined by `SongAPIResponseModel` (`src/modules/songs/models/song.model.ts:5-57`). The **same object**
appears in: search-songs results, `song.getDetails` (`songs[]`), album/playlist `list[]`, artist
`topSongs[]`/`singles[]`, and station songs. All scalar fields are **strings** unless noted.

Top level:

| Field | Type | → App field (repo mapping, `src/modules/songs/helpers/song.helper.ts`) |
|---|---|---|
| `id` | String | `id` (L7) |
| `title` | String | `name` (L8) — note: app "name" comes from `title` |
| `subtitle` | String | unused by repo |
| `header_desc` | String | unused for songs |
| `type` | String | `type` (L9), `"song"` |
| `perma_url` | String | `url` (L19) — canonical page URL / share link |
| `image` | String (URL) | image ladder via `createImageLinks` (L31, §3.3) |
| `language` | String | `language` (L16), lowercase e.g. `"hindi"` |
| `year` | String | `year` (L10, kept as String, `null` if empty) |
| `play_count` | String | `playCount = Number(play_count)` (L15, `null` if empty) — use `Long` in Kotlin |
| `explicit_content` | String | `explicitContent = (explicit_content == "1")` (L14) |
| `list_count`, `list_type`, `list` | String | vestigial on songs; on albums/playlists `list` is an **array** instead (§4.7/§4.10) — upstream is polymorphic here, parse defensively |

`more_info` object (`song.model.ts:20-56`) — all Strings unless noted:

| Field | → App field (song.helper.ts) |
|---|---|
| `release_date` | `releaseDate` (L11) |
| `duration` | `duration = Number(duration)` **seconds** (L12) |
| `label` | `label` (L13) |
| `has_lyrics` | `hasLyrics = (has_lyrics == "true")` (L17) — string `"true"`/`"false"` |
| `lyrics_id` | `lyricsId` (L18) — **often absent entirely** when `has_lyrics="false"` (verified live in API_RESEARCH §9); DTO field must be nullable with default |
| `copyright_text` | `copyright` (L20) |
| `album_id` / `album` / `album_url` | `album.id` / `album.name` / `album.url` (L21-25) |
| `encrypted_media_url` | → **decrypt → download/stream ladder** (L32, §3.2). Absent/empty ⇒ song unplayable |
| `320kbps` | `"true"`/`"false"` — whether the top rung truly exists. Repo reads it in the model but **does not expose it**; the app SHOULD use it to cap the ladder (§3.2) |
| `artistMap.primary_artists[]` / `.featured_artists[]` / `.artists[]` | `artists.primary` / `.featured` / `.all` (L26-30), each item §3.4 |
| `music`, `origin`, `is_dolby_content` (Boolean), `encrypted_cache_url`, `rights{...}`, `cache_state`, `lyrics_snippet`, `starred`, `label_url`, `vcode`, `vlink`, `triller_available` (Boolean), `request_jiotune_flag` (Boolean), `webp` | declared in the model; unused by the mapping — ignore in Kotlin (with `ignoreUnknownKeys`) |

⚠️ The Zod model declares every field required, but live payloads **omit** fields (e.g. `lyrics_id`)
and add undeclared ones (`modules`, `encrypted_drm_media_url`, …) — API_RESEARCH §9. Kotlin DTOs:
**every field nullable/having a default**, `ignoreUnknownKeys = true`.

### 3.2 Media URL decryption + quality ladder — EXACT (`src/common/helpers/link.helper.ts:3-28`)

```
createDownloadLinks(encryptedMediaUrl):
  if encryptedMediaUrl is empty → return []            (song unplayable)
  key = "38346591"        (link.helper.ts:14)  — 8 ASCII chars = DES key bytes
  iv  = "00000000"        (link.helper.ts:15)  — passed to the decipher but see below
  cipher = node-forge createDecipher('DES-ECB', key)   (link.helper.ts:18)
  decipher.start({ iv })                               (link.helper.ts:19)
  input = base64-decode(encryptedMediaUrl)             (link.helper.ts:17, forge util.decode64 = standard Base64)
  output bytes → decryptedLink (a plain CDN URL string, ends in ..._96.mp4)
  for each quality in [_12→"12kbps", _48→"48kbps", _96→"96kbps", _160→"160kbps", _320→"320kbps"]:
      url = decryptedLink.replace("_96", quality.id)   (link.helper.ts:26 — FIRST occurrence string replace)
```

Facts an implementer must not get wrong:

- **Cipher: DES in ECB mode**, PKCS#7 padding (node-forge default for `DES-ECB`; forge's finish
  validates/removes padding). Because the mode is ECB, **the IV is cryptographically ignored** —
  `decipher.start({iv})` is ceremony. In Kotlin/Android use
  `Cipher.getInstance("DES/ECB/PKCS5Padding")` (PKCS5Padding ≡ PKCS#7 for 8-byte blocks) with
  `SecretKeySpec("38346591".toByteArray(Charsets.US_ASCII), "DES")`. **No BouncyCastle needed** —
  the platform JCE provides DES. (DES is weak crypto; here it's only URL obfuscation, as upstream intends.)
- **Base64:** standard alphabet. Android `java.util.Base64.getDecoder()` / Okio `decodeBase64()`.
  Strip whitespace if present. Decrypted bytes are a UTF-8 URL string; forge's `getBytes()` returns
  a byte-string — treat output as UTF-8 (may include trailing padding bytes if padding handling
  differs — trim at first NUL / after `.mp4` if observed).
- **The ladder is synthesised**: the repo returns all 5 rungs for every song regardless of the
  `more_info."320kbps"` flag. If that flag is `"false"`, `_320` (and possibly `_160`) URLs may 403/404.
  App rule (improvement over repo, keep): cap ladder by the flag, and **always implement descending
  fallback on HTTP error** (try requested rung → next lower → …).
- Result URLs are **plain unsigned CDN links** (`https://aac.saavncdn.com/.../*.mp4`, AAC in MP4 —
  Media3/ExoPlayer native). No token, no expiry handling in the repo.
- Worked shape: decrypted base ends `_96.mp4`; `_320` rung = same URL with `_96`→`_320`.

**Kotlin mapping:**

```kotlin
object MediaUrlFactory {
    private val QUALITIES = listOf("12" to "12kbps", "48" to "48kbps", "96" to "96kbps",
                                   "160" to "160kbps", "320" to "320kbps")
    fun decrypt(encryptedMediaUrl: String?): List<QualityUrl> {
        if (encryptedMediaUrl.isNullOrBlank()) return emptyList()
        val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec("38346591".toByteArray(Charsets.US_ASCII), "DES"))
        val plain = String(cipher.doFinal(Base64.getDecoder().decode(encryptedMediaUrl)), Charsets.UTF_8)
        return QUALITIES.map { (token, label) -> QualityUrl(label, plain.replace("_96", "_$token")) }
        // note: Kotlin String.replace replaces ALL occurrences; TS replaces first only.
        // The token "_96" occurs once in practice (in the filename); if paranoid use replaceFirst.
    }
}
```

(Use `replaceFirst("_96", …)` to mirror JS `String.replace` with a string pattern exactly.)

### 3.3 Image ladder — EXACT (`src/common/helpers/link.helper.ts:30-41`)

```
createImageLinks(link):
  if !link → []
  for quality in ["50x50", "150x150", "500x500"]:
      url = link.replace(/150x150|50x50/, quality)   // swap the size token
                .replace(/^http:\/\//, "https://")    // force https
```

Upstream `image` is a **single String URL** (usually containing `-150x150.jpg`). Kotlin: same two
replacements (`Regex("150x150|50x50")`, then `http://`→`https://` prefix). If the URL contains
neither token, all three rungs equal the original — harmless.

### 3.4 ArtistMap (embedded artist reference)

Raw (`src/modules/artists/models/artist-map.model.ts:4-11`): `id, name, role, type, image (String URL), perma_url` — all Strings.
Mapping (`src/modules/artists/helpers/artist.helper.ts:53-62`): `url ← perma_url`, `image` → §3.3 ladder.
`role` values seen: `"primary_artists"`, `"featured_artists"`, `"singer"`, etc. — treat as opaque String.

```kotlin
@Serializable data class RawArtistMapDto(val id: String? = null, val name: String? = null,
    val role: String? = null, val type: String? = null, val image: String? = null,
    @SerialName("perma_url") val permaUrl: String? = null)
```

---

## 4. Capabilities

For every capability: **Request** (exact upstream call as the repo builds it), **Response** (raw shape),
**Repo mapping** (what the wrapper extracted — the app should extract the same), **Kotlin mapping**.

### 4.1 Global search — `autocomplete.get`

**Request** (`src/modules/search/use-cases/search-all/search-all.use-case.ts:11-14`):

```
GET https://www.jiosaavn.com/api.php?__call=autocomplete.get&_format=json&_marker=0&api_version=4&ctx=web6dot0&query=<query>
```

Only call-specific param: `query` (the raw search string). No paging — single shot.

**Response** (`SearchAPIResponseModel`, `src/modules/search/models/search.model.ts:4-131`): object with
5 sections, each `{ data: [...], position: Int }`: `albums`, `songs`, `playlists`, `artists`, **`topquery`** (lowercase upstream).

Item shapes differ per section (all Strings unless noted):
- `songs.data[]` / `topquery.data[]`: `id, title, subtitle, type, image, perma_url, explicit_content, mini_obj (Boolean), description`, `more_info{ album, ctr (Number), score?, vcode, vlink?, primary_artists (String, comma-joined names), singers (String), video_available (Boolean), triller_available (Boolean), language }`
- `albums.data[]`: + `more_info{ music (String — artist names), ctr, year, is_movie, language, song_pids (String — comma-joined song ids) }`
- `artists.data[]`: `id, title, image, extra, type, mini_obj, isRadioPresent (Boolean), ctr (Number), entity (Number), description, position (Number)` — **no `perma_url`** in this section's model
- `playlists.data[]`: + `more_info{ firstname, artist_name (Array<String>), entity_type, entity_sub_type, video_available, is_dolby_content, sub_types, images, lastname, language }`

**Repo mapping** (`createSearchPayload`, `src/modules/search/helpers/search.helper.ts:13-96`): renames
`topquery`→`topQuery`; per item keeps `id, title, image→ladder, url←perma_url, type, description` +
songs/topQuery: `album←more_info.album, primaryArtists←more_info.primary_artists, singers←more_info.singers, language←more_info.language`;
albums: `artist←more_info.music, year←more_info.year, songIds←more_info.song_pids`;
artists: `position←item.position`; playlists: `language←more_info.language`.

⚠️ These are **lightweight** items: no `encrypted_media_url`, no duration → **not playable**.
A tapped song must be resolved via §4.6 (`song.getDetails`) before playback.

**Kotlin mapping:**

```kotlin
@Serializable data class RawGlobalSearchDto(
    val topquery: RawSectionDto<RawGlobalSongItemDto>? = null,
    val songs: RawSectionDto<RawGlobalSongItemDto>? = null,
    val albums: RawSectionDto<RawGlobalAlbumItemDto>? = null,
    val artists: RawSectionDto<RawGlobalArtistItemDto>? = null,
    val playlists: RawSectionDto<RawGlobalPlaylistItemDto>? = null)
@Serializable data class RawSectionDto<T>(val data: List<T> = emptyList(), val position: Int = 0)
// item DTOs: every field String? = null except the Booleans/Ints noted above; more_info as nested DTOs.
```

### 4.2 Search songs — `search.getResults` (paged, playable)

**Request** (`src/modules/search/use-cases/search-songs/search-songs.use-case.ts:18-25`):

```
GET …/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&q=<query>&p=<page>&n=<limit>
```

Params renamed: `q` = query, `p` = page (**0-based**), `n` = limit. (Wrapper REST defaults: page 0, limit 10 — `search.controller.ts:85-97`.)

**Response** (`SearchSongAPIResponseModel`, `src/modules/search/models/search-song.model.ts:4-8`):
`{ total: Int, start: Int, results: [ <raw Song, §3.1> ] }` — here `total`/`start` are real JSON numbers.
Repo (`search-songs.use-case.ts:27-31`) maps each result with `createSongPayload` and `slice(0, limit)`
(upstream may over-deliver). Termination: `start + results.size >= total`.

**Kotlin mapping:** `@Serializable data class RawPagedDto<T>(val total: Int = 0, val start: Int = 0, val results: List<T> = emptyList())` with `T = RawSongDto` (§4.6). Results are full songs → decrypt (§3.2) → playable immediately.

### 4.3 Search albums — `search.getAlbumResults` (paged)

**Request** (`search-albums.use-case.ts:18-25`): same `q, p, n` renaming, `__call=search.getAlbumResults`.

**Response** (`SearchAlbumAPIResponseModel`, `search-album.model.ts:5-32`): `{ total: Int, start: Int, results: [...] }` where each result is an **album-shaped object**: `id, title, subtitle, header_desc, type, perma_url, image, language, year (String), play_count (String), explicit_content (String), list_count, list_type, list: [raw Song]` and `more_info{ query, text, music, song_count (String), artistMap }`.

**Repo mapping** (`createSearchAlbumPayload`, `search.helper.ts:115-137`): `{ total: Number, start: Number, results: [{ id, name←title, description←header_desc, url←perma_url, year←Number(year), type, playCount←Number(play_count), language, explicitContent←explicit_content=="1", artists←more_info.artistMap (→§3.4 groups), image→ladder }] }` — the embedded `list` is **dropped** by the search mapping (open the album via §4.7 for tracks).

**Kotlin mapping:** `RawPagedDto<RawSearchAlbumDto>`; map to domain Album-lite (no songs). Note `Number(total)` in repo implies upstream *may* send them as strings for albums/playlists — **use a flexible Int serializer** (accept number or numeric string) for `total`/`start` in all paged DTOs.

### 4.4 Search artists — `search.getArtistResults` (paged)

**Request** (`search-artists.use-case.ts:19-26`): `__call=search.getArtistResults&q=&p=&n=`.

**Response** (`SearchArtistAPIResponseModel`, `search-artist.model.ts:4-22`): `{ total, start, results: [...] }`, item = ArtistMap variant: `name, id, ctr (Number), entity (Number), image (String URL), role, perma_url, type, mini_obj (Boolean), isRadioPresent (Boolean), is_followed (Boolean)`.

**Repo mapping** (`search-artists.use-case.ts:30-34`): `createArtistMapPayload` (§3.4) per item + `slice(0, limit)`.

**Kotlin mapping:** `RawPagedDto<RawArtistMapDto>` — same DTO as §3.4 (extra keys ignored).

### 4.5 Search playlists — `search.getPlaylistResults` (paged)

**Request** (`search-playlists.use-case.ts:19-26`): `__call=search.getPlaylistResults&q=&p=&n=`.

**Response** (`SearchPlaylistAPIResponseModel`, `search-playlist.model.ts:4-34`): `{ total, start, results: [...] }`, item: `id, title, subtitle, type, image, perma_url, explicit_content, mini_obj, numsongs (any)`, `more_info{ uid, firstname, artist_name (any), entity_type, entity_sub_type, video_available (Boolean), is_dolby_content (any), sub_types (any), images (any), lastname, song_count (String), language }`.

**Repo mapping** (`createSearchPlaylistPayload`, `search.helper.ts:98-113`): `{ total: Number(total), start: Number(start), results: [{ id, name←title, type, image→ladder, url←perma_url, songCount←Number(more_info.song_count), language←more_info.language, explicitContent←explicit_content=="1" }] }`.

**Kotlin mapping:** `RawPagedDto<RawSearchPlaylistDto>` with flexible Int for total/start (see §4.3).

### 4.6 Song details by id (+ by link) — `song.getDetails` / `webapi.get`

**Request by id** (`src/modules/songs/use-cases/get-song-by-id/get-song-by-id.use-case.ts:17-22`):

```
GET …/api.php?__call=song.getDetails&_format=json&_marker=0&api_version=4&ctx=web6dot0&pids=<ids>
```

`pids` = one song id **or comma-separated ids** (batch fetch — the wrapper's `GET /songs?ids=a,b,c` passes the string through verbatim). Ideal for resolving favourites/history/queue in one call.

**Request by link** (`get-song-by-link.use-case.ts:13-16`): `__call=webapi.get&token=<token>&type=song`, where `token` is the last path segment of a `jiosaavn.com/song/<slug>/<token>` URL (wrapper extracts it by regex in its controller).

**Response:** `{ songs: [ <raw Song, §3.1> ] }` (`get-song-by-id.use-case.ts:17`). Repo: if `!data.songs?.length` → "song not found" (L24); else map each with `createSongPayload`.

**Lyrics are NOT part of this response.** The wrapper controller's doc string claims "Optionally,
include lyrics in the response" (`song.controller.ts:92`) but **no code implements it** — lyrics are
a separate upstream call (§5). `more_info.lyrics_snippet` exists on the raw song but is only a teaser line.

**Kotlin mapping:**

```kotlin
@Serializable data class RawSongDto(               // mirrors §3.1; ALL fields defaulted
    val id: String? = null, val title: String? = null, val subtitle: String? = null,
    val type: String? = null, @SerialName("perma_url") val permaUrl: String? = null,
    val image: String? = null, val language: String? = null, val year: String? = null,
    @SerialName("play_count") val playCount: String? = null,
    @SerialName("explicit_content") val explicitContent: String? = null,
    @SerialName("more_info") val moreInfo: RawSongMoreInfoDto? = null)
@Serializable data class RawSongMoreInfoDto(
    @SerialName("release_date") val releaseDate: String? = null,
    val duration: String? = null, val label: String? = null,
    @SerialName("has_lyrics") val hasLyrics: String? = null,
    @SerialName("lyrics_id") val lyricsId: String? = null,
    @SerialName("copyright_text") val copyrightText: String? = null,
    @SerialName("album_id") val albumId: String? = null, val album: String? = null,
    @SerialName("album_url") val albumUrl: String? = null,
    @SerialName("encrypted_media_url") val encryptedMediaUrl: String? = null,
    @SerialName("320kbps") val is320kbps: String? = null,
    val artistMap: RawArtistMapGroupDto? = null)
@Serializable data class RawArtistMapGroupDto(
    @SerialName("primary_artists") val primary: List<RawArtistMapDto> = emptyList(),
    @SerialName("featured_artists") val featured: List<RawArtistMapDto> = emptyList(),
    @SerialName("artists") val all: List<RawArtistMapDto> = emptyList())
@Serializable data class RawSongDetailsDto(val songs: List<RawSongDto> = emptyList())
```

Conversions in the mapper (repository layer): `durationSec = duration?.toIntOrNull()`,
`playCount = playCount?.toLongOrNull()`, `explicit = explicitContent == "1"`,
`hasLyrics = hasLyricsStr == "true"`, `image ladder §3.3`, `stream ladder §3.2`.

### 4.7 Album details — `content.getAlbumDetails` / `webapi.get`

**Request by id** (`src/modules/albums/use-cases/get-album-by-id/get-album-by-id.use-case.ts:13-16`):
`__call=content.getAlbumDetails&albumid=<numeric album id>`.
**By link** (`get-album-by-link.use-case.ts:13-19`): `__call=webapi.get&token=<token>&type=album`.

**Response** (`AlbumAPIResponseModel`, `src/modules/albums/models/album.model.ts:5-27`): a single album object:
`id, title, subtitle, header_desc, type, perma_url, image, language, year (String), play_count (String), explicit_content (String), list_count, list_type, list: [raw Song]` + `more_info{ artistMap, song_count (String), copyright_text, is_dolby_content (Boolean), label_url }`.

**Repo mapping** (`createAlbumPayload`, `src/modules/albums/helpers/album.helper.ts:7-25`):
`name←title, description←header_desc, year←Number, playCount←Number, explicitContent←=="1",
songCount←Number(more_info.song_count), artists←more_info.artistMap groups, image→ladder,
songs←list.map(createSongPayload)`. **No pagination** — full track list in one response.

**Kotlin mapping:** `RawAlbumDto` mirroring the above (`list: List<RawSongDto>`); reuse `RawSongDto`/`RawArtistMapGroupDto`.

### 4.8 Artist details + artist songs + artist albums

**Artist page — request** (`src/modules/artists/use-cases/get-artist-by-id/get-artist-by-id.use-case.ts:22-32`):

```
GET …/api.php?__call=artist.getArtistPageDetails&…&artistId=<id>&n_song=<songCount>&n_album=<albumCount>&page=<page>&sort_order=<asc|desc>&category=<popularity|latest|alphabetical>
```

Param derivation: `artistId` (id), `n_song` ← songCount, `n_album` ← albumCount, `page` (0-based),
`sort_order` ← sortOrder, `category` ← sortBy. Wrapper REST defaults: page 0, songCount 10,
albumCount 10, sortOrder `desc` (`artist.controller.ts:44-73`). By-link variant (`get-artist-by-link.use-case.ts:24-33`):
`__call=webapi.get&token=&type=artist` + the same `n_song/n_album/page/sort_order/category`.

**Artist page — response** (`ArtistAPIResponseModel`, `src/modules/artists/models/artist.model.ts:5-124`):
one big object. Fields the app needs: `artistId` **and** `id` (both present via `.extend`), `name, subtitle, image, follower_count (String), fan_count (String), type, isVerified (Boolean), dominantLanguage, dominantType, isRadioPresent (Boolean), bio (String — **JSON-encoded string**, see below), dob, fb, twitter, wiki, urls{ albums, bio, comments, songs, overview }, availableLanguages (Array<String>), is_followed (Boolean)`,
plus content arrays: `topSongs: [raw Song]`, `topAlbums: [raw Album §4.7]`, `singles: [raw Song]`,
`dedicated_artist_playlist: [playlist items]`, `featured_artist_playlist: [playlist items]`,
`similarArtists: [...]` (raw item: `id, name, perma_url, image_url (note: not `image`), languages (JSON-encoded String), bio (JSON-encoded String), similar (JSON-encoded String), wiki, dob, fb, twitter, isRadioPresent (Boolean), type, dominantType, aka`, …), `topEpisodes: [any]`.

**Repo mapping** (`createArtistPayload`, `src/modules/artists/helpers/artist.helper.ts:12-51`):
`id ← artistId || id`; `url ← urls.overview || perma_url`; `followerCount ← Number(follower_count)`;
`fanCount ← fan_count` (**left as String**); `bio ← JSON.parse(bio)` → array of `{text, title, sequence}`;
`topSongs/topAlbums/singles` mapped with the song/album mappers; `similarArtists` mapped with
`image←image_url`→ladder and `languages/bio/similar` each `JSON.parse`d. ⚠️ In Kotlin, parse `bio`
defensively: model it as `String?` and JSON-decode in the mapper inside try/catch — a malformed
bio must not kill the whole artist page.

**Artist songs — request** (`get-artist-songs.use-case.ts:20-28`):
`__call=artist.getArtistMoreSong&artistId=&page=&sort_order=&category=` (no count param — page size is fixed upstream).
**Response** (`ArtistSongAPIResponseModel`, `artist-song.model.ts:4-18`): artist header fields + `topSongs{ songs: [raw Song], total: Int }`. Repo returns `{ total: data.topSongs.total, songs: mapped }` (`get-artist-songs.use-case.ts:32-35`). Paginate `page++` until accumulated ≥ `total`.

**Artist albums — request** (`get-artist-albums.use-case.ts:20-28`):
`__call=artist.getArtistMoreAlbum&artistId=&page=&sort_order=&category=`.
**Response** (`artist-album.model.ts:4-18`): `topAlbums{ albums: [raw Album], total: Int }`; repo returns `{ total, albums: mapped }`.

**Kotlin mapping:**

```kotlin
@Serializable data class RawArtistPageDto(
    val artistId: String? = null, val id: String? = null, val name: String? = null,
    val image: String? = null, @SerialName("follower_count") val followerCount: String? = null,
    @SerialName("fan_count") val fanCount: String? = null, val isVerified: Boolean? = null,
    val dominantLanguage: String? = null, val dominantType: String? = null,
    val bio: String? = null, val dob: String? = null, val fb: String? = null,
    val twitter: String? = null, val wiki: String? = null,
    val availableLanguages: List<String> = emptyList(), val isRadioPresent: Boolean? = null,
    val topSongs: List<RawSongDto> = emptyList(), val topAlbums: List<RawAlbumDto> = emptyList(),
    val singles: List<RawSongDto> = emptyList(),
    val similarArtists: List<RawSimilarArtistDto> = emptyList())
@Serializable data class RawArtistSongsDto(val topSongs: RawArtistSongsPageDto? = null)
@Serializable data class RawArtistSongsPageDto(val songs: List<RawSongDto> = emptyList(), val total: Int = 0)
@Serializable data class RawArtistAlbumsDto(val topAlbums: RawArtistAlbumsPageDto? = null)
@Serializable data class RawArtistAlbumsPageDto(val albums: List<RawAlbumDto> = emptyList(), val total: Int = 0)
```

### 4.9 Suggestions / radio — `webradio.createEntityStation` → `webradio.getSong` (ctx=android)

The repo's only "radio" feature; powers recommendations/autoplay. **Two sequential calls, both with `ctx=android`.**

**Step 1 — create station** (`src/modules/songs/use-cases/create-song-station/create-song-station.use-case.ts:11-20`):

```
GET …/api.php?__call=webradio.createEntityStation&_format=json&_marker=0&api_version=4&ctx=android
    &entity_id=<JSON>&entity_type=queue
```

`entity_id` derivation (L11): `JSON.stringify([encodeURIComponent(songId)])` — i.e. the literal
string `["<url-encoded song id>"]`, then URL-encoded again as a query value by the URL builder.
Response: `{ stationid: String }` (L13, L24). Missing/`!ok` → repo throws 500 "could not create station".

**Step 2 — fetch station songs** (`get-song-suggestions.use-case.ts:26-33`):

```
GET …/api.php?__call=webradio.getSong&_format=json&_marker=0&api_version=4&ctx=android&stationid=<stationId>&k=<limit>
```

`k` = limit (wrapper REST default 10). **Response** (`SongSuggestionAPIResponseModel`, `song-suggestion.model.ts:4-15`):
an object with key `stationid` **plus numeric-string keys** (`"0"`, `"1"`, …), each value `{ song: <raw Song> }`.
Repo (`get-song-suggestions.use-case.ts:39-46`): drops `stationid`, takes `Object.values(rest)`,
maps `element.song` via `createSongPayload`, filters nulls, `slice(0, limit)`.

**Kotlin mapping:** model step-2 response as `Map<String, RawStationEntryDto>` (kotlinx.serialization
Map DTO), remove the `"stationid"` entry, sort remaining keys numerically, map `entry.song`.
Cache the stationId per seed song for the session; refetching `webradio.getSong` with the same
stationid yields the next batch (that's how "more radio" works upstream).

```kotlin
@Serializable data class RawStationEntryDto(val song: RawSongDto? = null)
@Serializable data class RawStationCreatedDto(val stationid: String? = null)
```

There is **no other radio/station API in the repo**: no station-by-genre/mood/language calls, no
featured-station list endpoint (the browse-modules payload contains a `radio` section — §6 — but
the repo never fetches it).

### 4.10 Playlist details — `playlist.getDetails` / `webapi.get`

**Request by id** (`src/modules/playlists/use-cases/get-playlist-by-id/get-playlist-by-id.use-case.ts:19-26`):
`__call=playlist.getDetails&listid=<id>&n=<limit>&p=<page>` — note param names `listid`, `n`, `p` (wrapper REST defaults page 0, limit 10).
**By link** (`get-playlist-by-link.use-case.ts:19-27`): `__call=webapi.get&token=<token>&n=<limit>&p=<page>&type=playlist`.
(Repo quirk: the by-link use-case references `Endpoints.albums.link` — same string `webapi.get`, no behavioural difference.)

**Response** (`PlaylistAPIResponseModel`, `src/modules/playlists/models/playlist.model.ts:6-55`): single object:
`id, title, subtitle, header_desc, type, perma_url, image, language, year (String), play_count (String), explicit_content (String), list_count (String), list_type, list: [raw Song], description (String, top-level, via .extend)` + `more_info{ uid, is_dolby_content (Boolean), subtype[], last_updated, username, firstname, lastname, is_followed, isFY (Boolean), follower_count (String), fan_count (String), playlist_type, share, sub_types[], images[], H2 (String?), subheading, video_count (String), artists: [ArtistMap-raw] }`.

**Repo mapping** (`createPlaylistPayload`, `playlist.helper.ts:7-23` + use-case L30-35):
`name←title, description←header_desc, year←Number, playCount←Number, songCount←Number(list_count), artists←more_info.artists (§3.4), image→ladder, songs←list.map(...)`, then the use-case **overwrites `songCount` with the returned slice length** and slices `songs` to `limit`.
⚠️ Direct-upstream consequence for the app: use `list_count` as the true total; page with `p++`/same `n` until accumulated songs ≥ `list_count` or a short page returns. Ignore the wrapper's overwritten value — it was a wrapper artefact.

**Kotlin mapping:** `RawPlaylistDto` mirroring the above (`list: List<RawSongDto>`, `more_info.artists: List<RawArtistMapDto>`).

### 4.11 Song by link / share-URL resolution — `webapi.get` (all entity types)

Covered inline above: `__call=webapi.get&token=<token>&type=<song|album|artist|playlist>` (+ type-specific extras: artist/playlist paging params). Token = last URL path segment. Response shapes are the **same** as the by-id variants for each type (songs wrapped as `{songs:[...]}`). Keep this: it lets the app resolve any pasted/shared JioSaavn link (and Android App Links) with one call.

---

## 5. Lyrics — `lyrics.getLyrics` (NOT implemented in the repo)

**Repo status, stated plainly:** `Endpoints.songs.lyrics = 'lyrics.getLyrics'` exists
(`endpoint.constant.ts:13`) but **no use-case, service, controller, or model in the repo ever calls
it** (verified by source search). The song payload only carries `more_info.has_lyrics` (`"true"`/`"false"`)
and `more_info.lyrics_id` (`song.helper.ts:17-18`). **There is no repo code for the lyrics request
parameters, response parsing, or cleaning to replicate.** What follows is the best available evidence
from the sibling repo the parent project also studied — treat it as *probable, verify at build time*:

From `bunnykek/jiosaavn-dl/jiosaavn.py:17,63-66` (talks to the same upstream directly):

```
GET https://www.jiosaavn.com/api.php?__call=lyrics.getLyrics&ctx=web6dot0&api_version=4&_format=json&_marker=0&lyrics_id=<id>
```

- dl passes **the song's `id`** as `lyrics_id` (jiosaavn.py:65: `lyrics_api + json["id"]`, gated on `json["has_lyrics"] == "true"`).
- Response field used: `lyrics` — a String with `<br>` line breaks; dl's cleaning is exactly
  `lyrics.replace("<br>", "\n")` (jiosaavn.py:66). A `lyrics_copyright` field also exists upstream (per prior research).
- ⚠️ **Identifier ambiguity (the known unknown):** the wrapper's song model exposes a *separate*
  `more_info.lyrics_id`, implying the dedicated id is the correct parameter value; dl passes the
  song id and works in practice for songs that have lyrics; prior live testing (API_RESEARCH §9)
  showed passing the id of a song **without** lyrics returns an error body `{status, error}`.
  **Implementation rule:** only call when `has_lyrics == "true"`; try `more_info.lyrics_id` first,
  fall back to the song `id`; treat a body lacking a `lyrics` key as "no lyrics".

**Kotlin mapping:**

```kotlin
@Serializable data class RawLyricsDto(val lyrics: String? = null,
    @SerialName("lyrics_copyright") val lyricsCopyright: String? = null)
// clean: lyrics?.replace("<br>", "\n")?.replace("<br/>", "\n") — repo evidence covers only "<br>".
```

---

## 6. Home / trending — constants only in the repo (verified live shape)

The repo **defines but never calls** (`endpoint.constant.ts:30-31`, zero usages elsewhere in `src/`):

| `__call` | Extra params in repo | Live shape **[probe 2026-10-07]** (base params only, `ctx=web6dot0`) |
|---|---|---|
| `content.getBrowseModules` | none known | HTTP 200, ~169 KB. Top-level keys exactly: `radio`, `browse_discover`, `new_albums`, `charts`, `top_shows`, `new_trending`, `top_playlists`. Each section contains nested module data with entity lists in the same raw shapes as §3 (songs/albums/playlists as in §4). This is the only viable Home source. |
| `content.getTrending` | none known | HTTP 200, ~34 KB. Top level is a **JSON array** (not object) of entity objects in album/search-item shape (`id, title, subtitle, header_desc, type, perma_url, image, …`). Mixed entity types possible — branch on `type`. |

Because the repo never calls these, **there is no repo mapping to replicate** — the app team must
define its own DTOs for the sections it uses (recommend: `new_trending`, `charts`, `top_playlists`,
`new_albums` first), parsing items with the existing `RawSongDto`/`RawAlbumDto`-style tolerant DTOs.
Both calls work with **only the §1 base params** — no language param is required (content appears
geo/IP-localised upstream; unverified how localisation behaves for Nepal IPs — flag for device testing).

**Kotlin mapping (starting point):**

```kotlin
@Serializable data class RawBrowseModulesDto(
    @SerialName("new_trending") val newTrending: JsonObject? = null,
    val charts: JsonObject? = null,
    @SerialName("top_playlists") val topPlaylists: JsonObject? = null,
    @SerialName("new_albums") val newAlbums: JsonObject? = null,
    val radio: JsonObject? = null)
// Parse sections as JsonObject first; extract entity arrays per section once their
// inner key layout is confirmed on-device. Do NOT hard-model the full 169 KB tree blind.
```

---

## 7. Transport summary — headers, cookies, UA, retries (repo facts)

| Aspect | Repo behaviour (source-verified) |
|---|---|
| Method | GET only |
| Host/path | `https://www.jiosaavn.com/api.php` |
| Headers | `Content-Type: application/json`; `User-Agent`: random per request from ~100-entry browser pool (`user-agents.constant.ts`, chosen at `fetch.helper.ts:28`) |
| Cookies | **None sent, none stored.** No session is needed for any call in this spec |
| Auth | None — no key, token, or signing anywhere |
| Retries/backoff | **None in repo.** App-side recommendation (unchanged from API_RESEARCH §7): max 2 retries, exponential backoff, only on 5xx/timeout/IO; never on "not found" bodies |
| Timeouts | None set in repo (fetch default). App should set explicit OkHttp timeouts (e.g. 15 s connect / 20 s read) |
| Rate limits | None documented or handled in repo; upstream may throttle by IP — keep search debounced (~300 ms), prefetch suggestions sparingly, ≤3 parallel downloads |
| Errors | Often HTTP 200 + error/empty body; detect by missing expected keys (`songs`, `list`, `stationid`, `lyrics`) |

---

## 8. Global Kotlin implementation notes

1. **JSON config (mandatory):** `Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }`.
   Upstream adds fields freely (e.g. `modules`, `encrypted_drm_media_url` on songs) and omits declared ones.
2. **Stringly-typed numbers:** `play_count`, `duration`, `year` (songs/albums), `list_count`, `song_count`,
   `follower_count` arrive as **Strings** → DTO `String?`, convert with `toLongOrNull()`/`toIntOrNull()` in mappers.
   Paged `total`/`start` are numbers in the songs model but the repo `Number(...)`-coerces them for
   albums/playlists → use a **flexible Int serializer** (accepts JSON number or numeric string) for all paged DTOs.
   Booleans are split: some real (`is_dolby_content`, `mini_obj`, `isVerified`), some string-encoded
   (`explicit_content=="1"`, `has_lyrics=="true"`, `320kbps`) — per-field, per §3/§4 tables. Do not generalise.
3. **DES decrypt** (§3.2): platform JCE `DES/ECB/PKCS5Padding`, key bytes = ASCII `"38346591"`, standard Base64,
   `replaceFirst("_96", token)` ladder. Cache decrypted ladders per song id in Room — decryption is cheap but pointless to repeat.
4. **Mapping layer:** port the repo's `create*Payload` functions 1:1 into the app repository
   (`song.helper.ts` 33 lines, `album.helper.ts`, `playlist.helper.ts`, `artist.helper.ts`,
   `search.helper.ts` 137 lines, `link.helper.ts` 41 lines). They are the spec for field selection;
   anything they drop, the app doesn't need.
5. **HTML entities:** upstream titles/names may contain entities (`&amp;`, `&#039;`) — the repo does
   **not** unescape them (jiosaavn-dl does, for filenames). Unescape in the app's mapper for display.
6. **No base-URL setting needed anymore:** the upstream host is fixed (`www.jiosaavn.com`). The old
   Settings → API base URL feature becomes obsolete; if kept, it has no upstream meaning.
7. **What disappears:** the wrapper's `{success, data}` envelope, camelCase DTOs in `data/remote/dto/Dtos.kt`,
   and the Retrofit paths in `data/remote/SaavnApi.kt` are all replaced by §1's single `api.php` call +
   the raw DTOs in §4. Domain models can stay unchanged if the new mappers target them.

---

## 9. Coverage matrix — app capability → upstream

| App capability | Upstream `__call` | Repo status |
|---|---|---|
| Global search | `autocomplete.get` | ✅ implemented in repo (§4.1) |
| Search songs / albums / artists / playlists | `search.getResults` / `getAlbumResults` / `getArtistResults` / `getPlaylistResults` | ✅ (§4.2–4.5) |
| Song details (single + batch) | `song.getDetails` (`pids`) | ✅ (§4.6) |
| Resolve share link | `webapi.get` (`token`, `type`) | ✅ (§4.11) |
| Album details | `content.getAlbumDetails` | ✅ (§4.7) |
| Playlist details (paged) | `playlist.getDetails` | ✅ (§4.10) |
| Artist page / more songs / more albums | `artist.getArtistPageDetails` / `getArtistMoreSong` / `getArtistMoreAlbum` | ✅ (§4.8) |
| Suggestions / autoplay radio | `webradio.createEntityStation` → `webradio.getSong` (ctx=android) | ✅ (§4.9) |
| Lyrics | `lyrics.getLyrics` | ⚠️ constant only — §5 evidence-based reconstruction |
| Home / browse modules | `content.getBrowseModules` | ⚠️ constant only — §6 live-verified shape |
| Trending | `content.getTrending` | ⚠️ constant only — §6 live-verified shape |
| Genre/mood/language stations, podcasts/shows browse, user library | — | ❌ **do not exist** as repo calls (shows exist only inside browse-modules data) |
