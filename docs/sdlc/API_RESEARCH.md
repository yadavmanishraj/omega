# API Research — JioSaavn REST API for the Android Music App (Omega)

> SDLC phase: **Requirements / API Research**
> Sources studied (source code, not just READMEs), cloned under `~/workspace/jiosaavn-study/`:
> - `sumitkolhe/jiosaavn-api` — TypeScript, Hono (`@hono/zod-openapi`), Zod models, Bun/Node ≥ 20, deploys to Cloudflare Workers / Vercel / Docker. Public instance: **https://saavn.dev** (docs at `https://saavn.dev/docs`, OpenAPI at `/swagger`).
> - `bunnykek/jiosaavn-dl` — single-file Python CLI (`jiosaavn.py`, `requests` + `mutagen` + `sanitize_filename`), downloader/tagger only; it talks to JioSaavn directly, not to saavn.dev.
> Date of research: 2026-10-07. Verification method: see §9.

---

## 1. How the wrapper actually works (critical mental model)

```
Android app  ──REST/JSON──▶  saavn.dev (Hono wrapper)  ──▶  https://www.jiosaavn.com/api.php
                              routes mounted under /api        ?__call=<endpoint>&_format=json
                                                                 &_marker=0&api_version=4&ctx=web6dot0
```

- Every wrapper handler calls `useFetch()` (`src/common/helpers/fetch.helper.ts`): builds the upstream URL, picks a **random desktop User-Agent** per request, no cookies, no auth, no API key.
- Upstream raw payloads (snake_case, most numbers as **strings**) are mapped by `create*Payload()` helpers into clean camelCase DTOs. **The app only ever sees the mapped DTOs** — model those, not the raw upstream shapes.
- Two transforms matter most (`src/common/helpers/link.helper.ts`):
  - `createImageLinks(link)` → rewrites the size token to produce **50x50 / 150x150 / 500x500** and forces `https://`.
  - `createDownloadLinks(encrypted_media_url)` → DES-ECB-decrypts the encrypted media URL (key `38346591`, IV `00000000`, `node-forge`), then string-replaces `_96` with `_12/_48/_96/_160/_320`. This is why download URLs are **plain, unsigned CDN URLs** — no token refresh flow is needed (contrast jiosaavn-dl, §8).
- **No caching headers, no ETag, no rate limiting, no auth anywhere in the wrapper** (verified by source search). `cors()` is wide-open. All client-side resilience is the app's job (§7).
- Contexts: default upstream `ctx=web6dot0`; suggestions/radio use `ctx=android` (`ApiContextEnum`).

### Response envelope (uniform)

```jsonc
// success
{ "success": true,  "data": <T> }
// failure (HTTP status set from the thrown HTTPException)
{ "success": false, "message": "song not found" }   // 400 / 404 / 500 etc.
```

Fallback route: `404 {success:false, message:"route not found, check docs at https://saavn.dev/docs"}`.

---

## 2. Endpoint catalog — 14 public REST endpoints

All `GET`, all under base path `/api`, no auth. Plus meta routes: `GET /` (HTML landing), `GET /docs` (Scalar UI), `GET /swagger` (OpenAPI 3.1 JSON).

### 2.1 Search (`src/modules/search/controllers/search.controller.ts`)

| # | Method & path | Params (query) | Returns (`data`) | Upstream `__call` | Notes |
|---|---|---|---|---|---|
| 1 | `GET /search` | `query: String` **(required)** | `GlobalSearch` — sections for topQuery/songs/albums/artists/playlists, each `{results, position}` | `autocomplete.get` | **No pagination.** Lightweight items only (no `downloadUrl`, no duration) → tapping a song here requires a follow-up `GET /songs/{id}` before playback. |
| 2 | `GET /search/songs` | `query` req.; `page: Int = 0`, `limit: Int = 10` | `Paged<Song>` = `{total, start, results: Song[]}` — **full Song DTOs incl. `downloadUrl`** | `search.getResults` (`q,p,n`) | Playable directly from results. Server maps then `slice(0, limit)`. |
| 3 | `GET /search/albums` | same | `Paged<SearchAlbumItem>` | `search.getAlbumResults` | Album items have **no `songs`**; open via `GET /albums?id=`. |
| 4 | `GET /search/artists` | same | `Paged<SearchArtistItem>` | `search.getArtistResults` | Results run through the ArtistMap mapper (id/name/role/image/url). |
| 5 | `GET /search/playlists` | same | `Paged<SearchPlaylistItem>` | `search.getPlaylistResults` | Open via `GET /playlists?id=`. |

### 2.2 Songs (`src/modules/songs/controllers/song.controller.ts`)

| # | Method & path | Params | Returns | Upstream `__call` | Notes |
|---|---|---|---|---|---|
| 6 | `GET /songs` | `ids: String` (comma-separated) **or** `link: String (URL)`, one required → 400 if neither | `Song[]` (array even for one song) | `song.getDetails` (`pids`) / `webapi.get` (`token,type=song`) | **Batch fetch** — ideal for resolving favourites/history/queue in one call. Link token extracted by regex `jiosaavn.com/song/<slug>/<token>`. |
| 7 | `GET /songs/{id}` | path `id` | `Song[]` (1-element array) | `song.getDetails` | Controller doc mentions lyrics — **not actually included** in the response. 404 if unknown id. |
| 8 | `GET /songs/{id}/suggestions` | path `id`; `limit: Int = 10` | `Song[]` (full DTOs) | Two upstream calls: `webradio.createEntityStation` (`entity_id=["<id>"]`, `entity_type=queue`, ctx=android) → `webradio.getSong` (`stationid, k=limit`, ctx=android) | This is the **radio / "up next" / autoplay** engine. More expensive server-side (2 upstream calls) → cache & prefetch (§7). |

### 2.3 Albums (`src/modules/albums/controllers/album.controller.ts`)

| # | Method & path | Params | Returns | Upstream | Notes |
|---|---|---|---|---|---|
| 9 | `GET /albums` | `id: String` **or** `link: String (URL)` | `Album` (single object, incl. `songs: Song[] \| null`) | `content.getAlbumDetails` / `webapi.get` | **No `/albums/{id}` path variant, no pagination** — full track list in one response. Link regex: `jiosaavn.com/album/<slug>/<token>`. |

### 2.4 Artists (`src/modules/artists/controllers/artist.controller.ts`)

| # | Method & path | Params | Returns | Upstream | Notes |
|---|---|---|---|---|---|
| 10 | `GET /artists` | `id` or `link`; `page=0`, `songCount=10`, `albumCount=10`, `sortBy ∈ {popularity, latest, alphabetical}`, `sortOrder ∈ {asc, desc}` | `Artist` (with `topSongs`, `topAlbums`, `singles`, `similarArtists`) | `artist.getArtistPageDetails` / `webapi.get` | One-shot artist page. Controller default `sortOrder` is inconsistently `asc` in the by-id/link handler vs `desc` in docs — **always pass sort params explicitly.** |
| 11 | `GET /artists/{id}` | path `id`; same query as #10 | `Artist` | `artist.getArtistPageDetails` | |
| 12 | `GET /artists/{id}/songs` | path `id`; `page=0`, `sortBy`, `sortOrder` (**no limit**) | `{total: Int, songs: Song[]}` | `artist.getArtistMoreSong` | Page size is fixed upstream; paginate by incrementing `page` until accumulated ≥ `total`. |
| 13 | `GET /artists/{id}/albums` | path `id`; `page=0`, `sortBy`, `sortOrder` | `{total: Int, albums: Album[]}` | `artist.getArtistMoreAlbum` | Albums here are full Album DTOs (may include `songs` mapping cost) — use only needed fields. |

### 2.5 Playlists (`src/modules/playlists/controllers/playlist.controller.ts`)

| # | Method & path | Params | Returns | Upstream | Notes |
|---|---|---|---|---|---|
| 14 | `GET /playlists` | `id` or `link`, one required → 400; `page=0`, `limit=10` | `Playlist` (incl. `songs: Song[] \| null`, `artists: ArtistMap[] \| null`) | `playlist.getDetails` (`listid,n,p`) / `webapi.get` | **Gotcha:** the use-case slices songs to `limit` and then **overwrites `songCount` with the slice length**. Never trust `Playlist.songCount` from a paged call; keep fetching pages (`page++`, same `limit`) until a page returns < `limit` songs. Link regex also accepts `saavn.com/featured/…` and `s/playlist/…`. |

### 2.6 Defined upstream but **NOT exposed** as REST routes ⚠️

| Upstream call | Where defined | Status | Consequence for the app |
|---|---|---|---|
| `lyrics.getLyrics` | `Endpoints.songs.lyrics` | Constant only — **no route, no use-case, no model** | **There is no lyrics endpoint in the public API.** Song DTO only has `hasLyrics: Boolean` + `lyricsId: String?`. Lyrics feature requires (a) extending/self-hosting the wrapper, (b) calling upstream directly from the app (breaks the clean architecture, exposes the app to raw shapes), or (c) dropping lyrics v1. See §10. |
| `webradio.createEntityStation` | `Endpoints.songs.station` | Internal only (used by suggestions) | No standalone "start radio" endpoint; radio = `/songs/{id}/suggestions`. |
| `content.getBrowseModules` | `Endpoints.modules` | **Unused anywhere** | **There is no Home / browse / charts endpoint in the wrapper**, even though upstream returns rich modules (verified §9: `radio, browse_discover, new_albums, charts, top_shows, new_trending, top_playlists`). Home must be composed client-side (e.g. curated searches) or the wrapper extended & self-hosted. **Top product risk — decide before UI build.** |
| `content.getTrending` | `Endpoints.trending` | **Unused anywhere** | Same as above — no trending endpoint despite the name existing in constants. |

---

## 3. Pagination model (summary)

| Family | Parameters | Response fields | Termination condition |
|---|---|---|---|
| Search (songs/albums/artists/playlists) | `page` 0-based, `limit` (default 10) | `total`, `start`, `results[]` | `start + results.size >= total`, or empty page |
| Global search | — | per-section `position` only | n/a (single shot) |
| Playlist songs | `page`, `limit` (on `GET /playlists`) | embedded `songs[]` | returned songs < `limit` |
| Artist songs / albums | `page` only | `total`, `songs[]`/`albums[]` | accumulated >= `total` |
| Albums, song details, suggestions | — / `limit` (suggestions only) | full object / arrays | n/a |

Fits Jetpack Paging 3 cleanly for search + artist lists (`total`/`start` available); playlist paging needs a custom `PagingSource` keyed on page with the "short page = end" rule.

---

## 4. Media ladders

### 4.1 Images — `image: QualityUrl[]` on every entity

| `quality` | Use in app |
|---|---|
| `50x50` | Queue rows, notifications (small) |
| `150x150` | List rows, cards |
| `500x500` | Player, detail headers, download cover art |

Always pick by target size (`image.bestFor(px)` helper); never assume order — select by matching `quality`. A raw single URL from search-global items also follows the `…-150x150.jpg` pattern, but typed search/detail DTOs already give the ladder.

### 4.2 Audio — `downloadUrl: QualityUrl[]` on every full `Song`

| `quality` | `_xx` token in CDN URL | Indicative use |
|---|---|---|
| `12kbps` | `_12` | Never for music; ignore except ultra-low-data mode |
| `48kbps` | `_48` | Data-saver streaming |
| `96kbps` | `_96` | The "source" URL that was decrypted (always exists) |
| `160kbps` | `_160` | **Recommended default streaming** |
| `320kbps` | `_320` | **Recommended download / high-quality streaming** |

**Caveats (from source + verification):**
1. The ladder is **synthesised by string replacement** — all five entries are returned **regardless of whether the track is actually available at 320kbps** (`more_info."320kbps"` upstream is the ground truth, but the wrapper does **not** expose it in the Song DTO). A 320 URL may therefore fail (403/404) for some tracks. Playback/download code **must implement descending fallback**: try requested quality → on HTTP error, drop to next lower rung and retry once.
2. Format is AAC in an `.mp4`/`.m4a` CDN file (`aac.saavncdn.com/.../*.mp4`) — natively supported by Media3/ExoPlayer, no transcoding.
3. Global-search song items, search-album/artist/playlist items have **no** `downloadUrl` — only full `Song` DTOs (from search-songs, details, album/playlist/artist payloads, suggestions) are playable.

---

## 5. JSON → Kotlin model mapping (exact)

Envelope + shared:

```kotlin
@Serializable data class ApiResponse<T>(val success: Boolean, val data: T? = null, val message: String? = null)
@Serializable data class QualityUrl(val quality: String, val url: String)   // JSON: DownloadLinkModel — used for BOTH image and downloadUrl
@Serializable data class Paged<T>(val total: Int, val start: Int, val results: List<T>)
```

### 5.1 `Song` (`SongModel`, mapped in `song.helper.ts`)

| JSON field | Kotlin type | Nullable | Notes |
|---|---|---|---|
| `id` | `String` | No | Opaque, e.g. `"3IoDK8qI"` |
| `name` | `String` | No | JSON key is `name`, **not** `title` (title is upstream-only) |
| `type` | `String` | No | `"song"` |
| `year` | `String?` | Yes | **String in Song, Int in Album/Playlist** — upstream inconsistency, do not unify |
| `releaseDate` | `String?` | Yes | ISO-ish date string from upstream `release_date`; parse leniently |
| `duration` | `Int?` (seconds) | Yes | `Long` also fine; upstream sends string, wrapper converts to number |
| `label` | `String?` | Yes | |
| `explicitContent` | `Boolean` | No | wrapper converts `"1"` → true |
| `playCount` | `Long?` | Yes | Can exceed Int range on hits — use `Long` |
| `language` | `String` | No | e.g. `"hindi"`, `"english"` (lowercase) |
| `hasLyrics` | `Boolean` | No | |
| `lyricsId` | `String?` | Yes | Present only when lyrics exist upstream — and even then unreliable (§9) |
| `url` | `String` | No | Canonical `jiosaavn.com/song/…` page URL (share link) |
| `copyright` | `String?` | Yes | |
| `album.id / .name / .url` | `String?` each | Yes | Nested object `SongAlbumRef` |
| `artists.primary[]` | `List<ArtistMap>` | No (may be empty) | |
| `artists.featured[]` | `List<ArtistMap>` | No | |
| `artists.all[]` | `List<ArtistMap>` | No | |
| `image` | `List<QualityUrl>` | No | 50/150/500 ladder (§4.1) |
| `downloadUrl` | `List<QualityUrl>` | No (may be empty) | Empty if upstream had no `encrypted_media_url` → treat song as **unplayable / region-restricted** |

### 5.2 `ArtistMap` (embedded artist reference)

| Field | Kotlin | Nullable |
|---|---|---|
| `id`, `name`, `role` (`"primary_artists"`, `"featured_artists"`, …), `type` | `String` | No |
| `image` | `List<QualityUrl>` | No |
| `url` | `String` | No |

### 5.3 `Album` (`AlbumModel`)

| Field | Kotlin | Nullable | Notes |
|---|---|---|---|
| `id`, `name`, `type`, `language`, `url` | `String` | No | `name` ← upstream `title` |
| `description` | `String` | No | ← `header_desc`; may be `""` |
| `year` | `Int?` | Yes | |
| `playCount` | `Long?` | Yes | |
| `explicitContent` | `Boolean` | No | |
| `songCount` | `Int?` | Yes | trustworthy here (unlike playlists) |
| `artists` | `Artists` (same shape as Song.artists) | No | |
| `image` | `List<QualityUrl>` | No | |
| `songs` | `List<Song>?` | **Yes** | Null when the payload came nested inside artist/search contexts |

### 5.4 `Playlist` (`PlaylistModel`)

| Field | Kotlin | Nullable | Notes |
|---|---|---|---|
| `id`, `name`, `type`, `language`, `url` | `String` | No | |
| `description` | `String?` | **Yes** (differs from Album) | |
| `year`, `playCount`, `songCount` | `Int?` / `Long?` / `Int?` | Yes | ⚠️ `songCount` is **overwritten with the returned slice length** by the playlist use-case — do not display as total |
| `explicitContent` | `Boolean` | No | |
| `image` | `List<QualityUrl>` | No | |
| `songs` | `List<Song>?` | Yes | Paged via `page`/`limit` on the same call (§2.5) |
| `artists` | `List<ArtistMap>?` | Yes | Playlist curators/contributors |

### 5.5 `Artist` (`ArtistModel`)

| Field | Kotlin | Nullable |
|---|---|---|
| `id`, `name`, `url`, `type` | `String` | No |
| `image` | `List<QualityUrl>` | No |
| `followerCount` | `Long?` | Yes |
| `fanCount` | **`String?`** | Yes — upstream quirk, wrapper does NOT convert this one |
| `isVerified`, `isRadioPresent` | `Boolean?` | Yes |
| `dominantLanguage`, `dominantType`, `dob`, `fb`, `twitter`, `wiki` | `String?` | Yes |
| `bio` | `List<BioEntry>?` where `BioEntry(text: String?, title: String?, sequence: Int?)` | Yes — wrapper `JSON.parse`s the upstream bio string; parse failure upstream = server error risk |
| `availableLanguages` | `List<String>` | No (may be empty) |
| `topSongs`, `singles` | `List<Song>?` | Yes |
| `topAlbums` | `List<Album>?` | Yes |
| `similarArtists` | `List<SimilarArtist>?` | Yes; `SimilarArtist(id, name, url, image: List<QualityUrl>, languages: Map<String,String>?, wiki, dob, fb, twitter, isRadioPresent: Boolean, type, dominantType, aka, bio: String?, similarArtists: List<IdName>? )` — note nested `bio` here stays a raw `String?`, unlike Artist.bio |

### 5.6 Search DTOs

**Global search** (`SearchModel`) — every section is `{results: List<T>, position: Int}`:

| Section | Item fields (all `String` unless noted) |
|---|---|
| `topQuery.results[]` / `songs.results[]` | `id, title, image: List<QualityUrl>, album, url, type, description, primaryArtists, singers, language` |
| `albums.results[]` | `id, title, image, artist, url, type, description, year, language, songIds` (comma-joined pids string) |
| `artists.results[]` | `id, title, image, type, description, position: Int` |
| `playlists.results[]` | `id, title, image, url, language, type, description` |

Note the key is **`topQuery`** (camelCase) in the mapped output, although upstream calls it `topquery`.

**Typed search** items (all inside `Paged<T>` = `{total, start, results}`):
- Songs → full `Song` (§5.1).
- Albums → `SearchAlbumItem(id, name, description, year: Int?, type, playCount: Long?, language, explicitContent: Boolean, artists: Artists, url, image: List<QualityUrl>)` — no `songs`.
- Artists → exactly `ArtistMap` shape (id/name/role/type/image/url).
- Playlists → `SearchPlaylistItem(id, name, type, image, url, songCount: Int?, language, explicitContent: Boolean)`.

### 5.7 Lyrics / suggestions / station DTOs

- **Lyrics: no DTO exists in the API.** Upstream `lyrics.getLyrics` (per jiosaavn-dl code) returns at least `{lyrics: String (with <br> line breaks), lyrics_copyright: String, …}` and an error shape `{status, error}` when the id is wrong. jiosaavn-dl passes **the song's id** as `lyrics_id` and replaces `<br>` → `\n`. But the wrapper's own sample (§9) proves song-id ≠ lyrics_id in general — a self-hosted lyrics route must map via the song's real `lyricsId`.
- **Suggestions:** response is simply `Song[]` — no station id leaks to the client (station creation is internal, §2.2 #8).
- There is **no** DTO for: home modules, trending, charts, radio stations list, podcasts/shows, genres/moods, user/library (by design — app is no-login, library is local Room).

### 5.8 Parser tolerance rules (mandatory)

Verified live (§9): upstream payloads contain **extra fields** the Zod models don't declare (`modules`, `button_tooltip_info`, `pro_hva_campaigns`, `encrypted_drm_media_url`, `label_id`, `has_trivia`, …) and **omit fields the models declare** (e.g. `lyrics_id` absent for a song with `has_lyrics=false`). The wrapper hides most of this, but defensive client parsing is still required because the wrapper passes through converted values loosely:

- kotlinx.serialization: `ignoreUnknownKeys = true`, `coerceInputValues = true`, `isLenient = true`.
- Give **every** DTO field a default (`null` / `emptyList()` / `""`) so a missing key never crashes a screen.
- Treat numbers defensively: several counts arrive converted by the wrapper, but an upstream change can regress them to strings — prefer custom flexible serializers for `playCount`/`fanCount` if tests show flakiness.

---

## 6. Retrofit service sketch (for the builder)

```kotlin
interface SaavnApi {
    // Search
    @GET("search") suspend fun globalSearch(@Query("query") q: String): ApiResponse<GlobalSearch>
    @GET("search/songs") suspend fun searchSongs(@Query("query") q: String, @Query("page") page: Int = 0, @Query("limit") limit: Int = 20): ApiResponse<Paged<Song>>
    @GET("search/albums") suspend fun searchAlbums(@Query("query") q: String, @Query("page") page: Int = 0, @Query("limit") limit: Int = 20): ApiResponse<Paged<SearchAlbumItem>>
    @GET("search/artists") suspend fun searchArtists(@Query("query") q: String, @Query("page") page: Int = 0, @Query("limit") limit: Int = 20): ApiResponse<Paged<ArtistMap>>
    @GET("search/playlists") suspend fun searchPlaylists(@Query("query") q: String, @Query("page") page: Int = 0, @Query("limit") limit: Int = 20): ApiResponse<Paged<SearchPlaylistItem>>

    // Songs
    @GET("songs") suspend fun songsByIds(@Query("ids") ids: String): ApiResponse<List<Song>>          // comma-join ids
    @GET("songs") suspend fun songByLink(@Query("link") link: String): ApiResponse<List<Song>>
    @GET("songs/{id}") suspend fun songById(@Path("id") id: String): ApiResponse<List<Song>>
    @GET("songs/{id}/suggestions") suspend fun suggestions(@Path("id") id: String, @Query("limit") limit: Int = 20): ApiResponse<List<Song>>

    // Albums / Artists / Playlists
    @GET("albums") suspend fun albumById(@Query("id") id: String): ApiResponse<Album>
    @GET("albums") suspend fun albumByLink(@Query("link") link: String): ApiResponse<Album>
    @GET("artists/{id}") suspend fun artistById(@Path("id") id: String, @Query("page") page: Int = 0,
        @Query("songCount") songs: Int = 10, @Query("albumCount") albums: Int = 10,
        @Query("sortBy") sortBy: String = "popularity", @Query("sortOrder") sortOrder: String = "desc"): ApiResponse<Artist>
    @GET("artists/{id}/songs") suspend fun artistSongs(@Path("id") id: String, @Query("page") page: Int = 0,
        @Query("sortBy") sortBy: String = "popularity", @Query("sortOrder") sortOrder: String = "desc"): ApiResponse<ArtistSongs>
    @GET("artists/{id}/albums") suspend fun artistAlbums(@Path("id") id: String, @Query("page") page: Int = 0,
        @Query("sortBy") sortBy: String = "popularity", @Query("sortOrder") sortOrder: String = "desc"): ApiResponse<ArtistAlbums>
    @GET("playlists") suspend fun playlistById(@Query("id") id: String, @Query("page") page: Int = 0, @Query("limit") limit: Int = 50): ApiResponse<Playlist>
    @GET("playlists") suspend fun playlistByLink(@Query("link") link: String, @Query("page") page: Int = 0, @Query("limit") limit: Int = 50): ApiResponse<Playlist>
}
```

Deep-link bonus: the `link` variants mean the app can resolve **any JioSaavn share link/URL pasted or opened via App Links** into playable content with one call — use for "Open link" + Android App Links for `jiosaavn.com/song|album|artist/…` and `featured/…`.

---

## 7. Caching, offline, errors, resilience

**Server reality:** no `ETag`, no `Cache-Control`, no compression guarantees. Design accordingly:

| Layer | Recommendation |
|---|---|
| OkHttp HTTP cache | 25 MB disk cache + network interceptor that **synthesises** `Cache-Control` for GETs: global/typed search `max-age=300`, song/album/artist/playlist details `max-age=3600`, suggestions `max-age=1800`. Offline interceptor: `only-if-cached, max-stale=86400` when connectivity is lost, so cached detail screens still open. |
| Coil | Default disk cache for artwork; the 500x500 URL doubles as the download cover and share art — it will already be warm. |
| Room | Source of truth for everything user-owned (no-login design): favourites, local playlists, history, download registry, **and a `song_cache` table keyed by song id** storing the last full Song DTO (esp. `downloadUrl`). Playback should read the cache first, refresh in background. |
| Stream-URL staleness | CDN URLs are unsigned plain links, but treat them as perishable: on ExoPlayer HTTP 403/404/Gone for a track, invalidate that song's cache row, re-fetch `GET /songs/{id}`, rebuild the media item, retry once — then apply quality fallback (§4.2), then skip to next queue item and surface a toast. |
| Suggestions prefetch | When a track is ~70% played (or on player open), fetch suggestions once and append as "radio queue" if the user enabled autoplay; cache per seed-song for the session. It costs the server 2 upstream calls — never poll it. |
| Search UX | 300 ms debounce, cancel previous in-flight search (Retrofit coroutine cancellation), cache last query's first page in memory. |

**Error model** — map everything into one sealed type at the repository boundary:

```kotlin
sealed interface ApiError {
    data class Http(val code: Int, val serverMessage: String?) : ApiError   // from {success:false, message}
    data object NotFound : ApiError          // 404 — bad/stale id, deleted content
    data object BadRequest : ApiError        // 400 — missing ids/link (programmer error; log, don't retry)
    data object NoConnectivity : ApiError
    data object Timeout : ApiError
    data class Parse(val cause: Throwable) : ApiError   // upstream shape drift (§5.8)
    data class Unknown(val cause: Throwable) : ApiError
}
```

- A `success:false` body can accompany any non-2xx; always try to parse `message` for display/logging.
- Retry policy: **max 2 retries, exponential backoff (500 ms → 2 s), only for 5xx / timeout / IO**. Never retry 400/404. Playback errors use the dedicated refresh-and-fallback path above, not blind retries.
- Empty ≠ error: `total=0`/empty `results`, `album.songs=null`, empty `downloadUrl` (region-restricted) are **valid states** — model them in UI state, don't throw.
- Base-URL health: on repeated 5xx/timeouts from the default instance, show a non-blocking banner pointing to Settings → API server (see §8) rather than dead-ending the user.

---

## 8. Configurable base URL strategy

- Default: `https://saavn.dev/api/` — **trailing slash is mandatory** for Retrofit relative paths above.
- The wrapper is trivially self-hostable (Docker / Vercel / Cloudflare buttons in its README) → power users and the developer can run a private instance. Settings screen should offer: *Default (saavn.dev) / Custom URL*, persisted in DataStore.
- Implementation: don't rebuild Retrofit on change. Build OkHttp with a **host-rewrite interceptor** that swaps scheme/host/port/path-prefix from the current DataStore value per request (or use a dynamic `BaseUrlProvider` + `@Url` for the few calls). Validate custom input: must be `https://`, normalise to end with `/api/` (append if the user pasted a bare host), test with a cheap call (`GET search?query=a`) before saving.
- Ship **no** hard-coded fallback list of third-party mirrors (trust/quality unknown); one default + custom is the honest strategy.
- Note for release planning: depending on a single unofficial public instance is an availability **and** policy risk (§10) — the custom-URL escape hatch is the mitigation, plus graceful offline mode from Room/OkHttp caches.

---

## 9. Verification log (2026-10-07, from this research environment)

**Public instance (saavn.dev):**
- ❌ `GET https://saavn.dev/api/search?query=Believer`, `…/api/search/songs`, `…/api/search/albums` — **failed from this environment**: TCP/TLS connect succeeds, server returns **empty reply** (curl exit `000`). This proves unreachability *from this egress/geo*, **not** that the instance is globally down. Builder/QA must re-verify from the target market (Nepal/India) and from a device before treating saavn.dev as dead. The mapped-shape contract in §5 rests on the Zod models + helpers, which are authoritative in source.

**Upstream (www.jiosaavn.com/api.php) — the data the wrapper maps — all ✅ 200:**
| Call | Result |
|---|---|
| `autocomplete.get&query=Believer` (raw global search) | 200, sections albums/songs/playlists/artists/topquery present |
| `song.getDetails&pids=3IoDK8qI` | 200 — "Levitating", `duration=203`, `"320kbps"=true`, `has_lyrics=false`, **`lyrics_id` absent** (see §5.8), extra fields `modules`, `encrypted_drm_media_url`, … present |
| `search.getResults&q=Believer&p=0&n=3` | 200 — `{total: 211, start: 0, results: 3}` confirms paging fields |
| `lyrics.getLyrics&lyrics_id=<song id>` | 200 but body `{status, error}` — confirms song id is **not** a valid lyrics id |
| `content.getAlbumDetails&albumid=23241654` | 200 — "Future Nostalgia", `list` = 13 tracks |
| `playlist.getDetails&listid=82914609` | 200 — "Best of Indie - English" |
| `content.getBrowseModules` | 200, ~170 KB — keys `radio, browse_discover, new_albums, charts, top_shows, new_trending, top_playlists` (rich Home data exists upstream; wrapper doesn't expose it, §2.6) |

Light-touch only: 9 read-only calls total, no media downloads.

---

## 10. jiosaavn-dl learnings for the Downloads feature

jiosaavn-dl (Python) solves the same problem **without** the wrapper. What transfers to Android:

1. **URL resolution — two different mechanisms.** dl calls upstream `song.generateAuthToken` with `bitrate=320` and the `encrypted_media_url` to get a signed `auth_url` (then rewrites `web`→`aac` in the host). The wrapper instead DES-decrypts once and hands us reusable plain URLs for **all** rungs. **Android decision: use the wrapper's `downloadUrl`** — simpler, quality-selectable, no token expiry handling. Keep generateAuthToken knowledge only as a last-resort fallback note.
2. **Always max quality, M4A container.** dl hard-codes 320kbps AAC `.m4a`. Android: default download quality = user preference (default 320, fallback descending per §4.2), extension `.m4a`, `audio/mp4` MediaStore MIME.
3. **Region/unavailability check.** dl checks `media_preview_url` presence; wrapper equivalent is **`downloadUrl` empty or all rungs failing** → mark track unavailable, don't create a zombie download entry.
4. **File/folder naming (adopt this scheme):**
   - Album folder: `"<PrimaryArtists> - <Album> [<Year>]"`, collapsing to `"Various Artists - …"` when >1 comma in the artist string.
   - Track file: `"<NN>. <Title>.m4a"` — `NN` = zero-padded 2-digit position in album/playlist; single songs still get a folder + `cover.jpg` (500x500 image).
   - Playlist folder: `"Playlist - <Name>"`.
   - **Sanitise every path segment** (dl uses `sanitize_filename`) and **HTML-unescape** titles/artists (`&amp;`, `&#039;`, …) before naming — Saavn strings contain entities.
5. **Metadata tagging map (mutagen MP4 atoms → Android):**

   | dl tag | Source field | Android equivalent |
   |---|---|---|
   | `©nam` title | song title | Room download record + MediaStore `TITLE`; embed via TagLib if in scope |
   | `©alb` album, `aART` album artist, `©ART` artists, `©wrt` composer (`music`) | album / primary artists | same |
   | `©day` release date, `trkn` (pos,total) | `release_date`, position | same |
   | `cprt` copyright, custom label, custom language | `copyright_text`, `label`, `language` | same |
   | `rtng` 2=clean / 4=explicit | `explicit_content` (note: dl compares to int `0` — a latent bug vs the string form; wrapper already gives us a clean `Boolean`) | same |
   | `©lyr` lyrics | lyrics endpoint, `<br>`→newline | only if lyrics feature is built (§2.6) |
   | `covr` cover JPEG | 500x500 image | embed or sidecar `cover.jpg` |

   Pragmatic Android scope: write the file via MediaStore/Downloads, keep **all** metadata in the Room download registry (player reads from DB, not file tags) and set MediaStore display fields. True in-file ID3/MP4 embedding needs TagLib/ffmpeg-kit — flag as stretch, not v1 blocker.
6. **Idempotency:** dl skips a track if the target file already exists. Android equivalent: download registry keyed by `songId + quality`; re-download = verify file exists in MediaStore, else re-queue.
7. **Whole-collection downloads:** album = iterate album `songs` in order; playlist = fetch **all pages first** (§2.5 gotcha) then enqueue with positions. dl uses upstream `n=1000` for playlists in one shot — the wrapper has no such shortcut; paging is mandatory.
8. Downloads must run under **WorkManager** (constraints: unmetered option, retry on IO only), with progress from OkHttp response body — dl's "download whole body into memory" (`session.get(...).content`) is an anti-pattern to avoid on mobile for albums/playlists.

---

## 11. API gaps, risks & open questions

**Gaps (feature-shaped):**
1. **No Home/browse/trending/charts endpoint** (§2.6) — biggest gap. Options: (a) compose Home from typed searches + fixed editorial queries, (b) extend + self-host the wrapper with a modules route (upstream shape verified rich), (c) hybrid: ship (a) in v1, (b) later. **Needs a decision before UI freeze.**
2. **No lyrics endpoint** (§2.6, §5.7) — same three options; a self-hosted route is ~1 use-case of work upstream-side.
3. No genres/moods/languages browse, no podcasts/shows (upstream `top_shows` exists in modules only), no "top artists" chart endpoint.
4. No ETag/conditional requests, no `If-Modified-Since` — bandwidth is on the client caches in §7.
5. Playlist `songCount` is corrupted by paging (§2.5); album `description` vs playlist `description` nullability differs; Song `year:String?` vs Album `year:Int?`; `fanCount:String?` — all catalogued in §5, all must be reflected in DTOs exactly as specified there.

**Risks:**
6. **Single unofficial upstream.** JioSaavn can change `api.php` (or the DES key/URL scheme in `createDownloadLinks`) at any time and break every endpoint at once; saavn.dev itself is one maintainer's deployment. Mitigations: configurable base URL (§8), defensive parsing (§5.8), Room-first playback for favourites/downloads.
7. **320kbps ladder is synthetic** (§4.2) — playback/download fallback is not optional.
8. **Availability from target markets is unverified** (§9) — saavn.dev was unreachable from this research egress. Must be tested from Nepal/India networks + a real device early; if flaky, self-hosting the wrapper (Docker/Vercel/CF) becomes a P0, not a nice-to-have.
9. **Legal/ToS:** unofficial API, downloads of commercial content. Distribution (Play Store) and branding ("JioSaavn" name/artwork) carry policy risk — product decision for the owner, flagged here, not resolved here.
10. No documented rate limits upstream or in the wrapper — behave politely anyway (debounce, prefetch sparingly, ≤3 parallel downloads) to avoid IP throttling of shared instances.

**Open questions for parent/owner:**
- Q1: Home strategy — client-composed (v1) vs self-hosted extended wrapper?
- Q2: Lyrics in v1 (requires Q1-style wrapper extension or direct upstream call)?
- Q3: Will the project self-host an instance for release, or rely on saavn.dev + custom-URL setting?
- Q4: In-file tag embedding (TagLib) in download scope, or DB-only metadata for v1?

---

## 12. Top recommendations for the builder (summary — detailed in body)

1. Model DTOs **exactly** per §5 (esp. nullability quirks) with lenient kotlinx settings — shape drift is verified, not hypothetical.
2. Playback must implement **refresh-on-403 + descending quality fallback** around `downloadUrl`; default stream 160kbps, download 320kbps.
3. Resolve §11-Q1 (Home) **before** building Home UI — there is no home endpoint today.
4. Use Paging 3 for typed search & artist lists; custom paging for playlists; one-shot loads for albums; batch-resolve library items with `GET /songs?ids=`.
5. Build the base-URL setting + OkHttp/Room caching in §7–§8 from day one — with an unofficial single-instance API, resilience *is* the feature.
