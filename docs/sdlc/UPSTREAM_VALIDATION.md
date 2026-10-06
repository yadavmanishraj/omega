# Upstream Validation — Live probes of UPSTREAM_SPEC.md against real JioSaavn servers

> **Date:** 2026-10-07 · **Method:** `curl` from the sandbox with the spec's exact parameter
> construction (§1 base params `_format=json&_marker=0&api_version=4&ctx=web6dot0`, `android`
> only for radio) and a desktop Chrome User-Agent. Song/album/playlist ids below are real ids
> returned by the probes. Raw response dumps omitted by design; structure + evidence only.
>
> **Headline:** the spec's transport, search, details, decryption, quality ladder, browse-modules,
> album and playlist sections are **confirmed live**. Two material corrections: **(1) the lyrics
> rule is inverted** — use the *song id*, never `more_info.lyrics_id` (it is `""`/absent in real
> payloads), and the `has_lyrics` gate must not come from `song.getDetails`; **(2) suggestions
> step 2 (`webradio.getSong`) currently returns an error body for every station tried** —
> autoplay radio cannot be built on it as specced without further investigation.

---

## 1. Global search + typed song search — ✅ PASS (minor corrections)

**Requests used**
- `api.php?__call=autocomplete.get&…base…&query=arijit+singh`
- `api.php?__call=search.getResults&…base…&q=arijit+singh&p=0&n=5`

**Evidence**
- Both HTTP 200, valid JSON (global ≈ 7.2 KB, typed ≈ 19.4 KB for 5 results).
- Global search top-level keys: `topquery, songs, albums, artists, playlists` as specced —
  **plus two sections the spec's model omits: `episodes` and `shows`** (both `{data, position}`).
  Each section is `{data: [...], position: Int}`; `position` is a real JSON int (0–4).
  Item shapes match §4.1: song item keys exactly as specced; `more_info.ctr` is a JSON **number**,
  `mini_obj` a real Boolean, `explicit_content` the **string** `"0"`/`"1"` — spec's type split confirmed.
- Typed search response: `{total, start, results}` with `total` a real Int (4675 for this query).
  **Correction/quirk:** `start` came back as **-4** for `p=0&n=5` — it is an Int, but do not assume
  `start == p*n`; use `results.size` accumulation for paging termination, not `start`.
- Results are full raw songs (§3.1 shape confirmed verbatim, incl. extra undeclared keys
  `button_tooltip_info`, `pro_hva_campaigns`, `modules`, `encrypted_drm_media_url` → `ignoreUnknownKeys`
  is mandatory, as specced). Stringly-typing confirmed: `duration="362"`, `year="2025"`,
  `320kbps="true"`, `has_lyrics="true"/"false"` — all JSON strings.
- **Titles contain raw HTML entities** (e.g. `Gehra Hua (From &quot;Dhurandhar&quot;)`) — spec §8.5's
  unescape-in-mapper note is confirmed necessary.
- `play_count` lives at the **top level** of the song object (String, e.g. `"60321486"` in details);
  it is **not** inside `more_info` (absent there) — matches spec §3.1 table placement.

## 2. Song details + DES decryption + quality ladder — ✅ PASS

**Request:** `api.php?__call=song.getDetails&…base…&pids=YiVML4Zo,1gHtmQ3x` (batch of 2) — HTTP 200,
response is exactly `{songs: [...]}` (only top-level key), batching works as specced.
Single-pid calls behave identically.

**Decryption evidence** (song `YiVML4Zo`, `encrypted_media_url` 96 chars → 72 bytes decoded):
- DES-ECB, key ASCII `"38346591"` (hex `3338333436353931`), standard Base64, PKCS padding —
  decrypts cleanly to a UTF-8 URL of the specced shape:
  `https://aac.saavncdn.com/450/<32-hex-hash>_96.mp4`
  (Sandbox tooling note only: OpenSSL 3 needs `-provider legacy` for single-DES; irrelevant to
  Android JCE, which provides DES natively as the spec says.)
- **Quality ladder verified live:** range GETs (`Range: bytes=0-99`) against the synthesised rungs
  of that URL returned **HTTP 206** for `_96`, `_160` and `_320` alike (song's `320kbps` flag was
  `"true"`). Plain unsigned CDN links, no token — as specced. Descending-fallback advice stands for
  songs whose flag is `"false"` (not re-probed here).

## 3. Lyrics — ✅ RESOLVED (spec §5 rule is INVERTED — correction required)

Test song: `aRZbUYD7` ("Tum Hi Ho") — a song that demonstrably has lyrics upstream.

**What the payloads actually contain (the trap):**
- In `search.getResults` results: `has_lyrics="true"`, `lyrics_id=""` (**empty string**).
- In `song.getDetails` (batch *and* single-pid, 10 songs checked incl. "Tum Hi Ho"):
  `has_lyrics="false"` and **`lyrics_id` key absent entirely** — for the very same songs.
  → `song.getDetails` **cannot** be used to gate or address lyrics. The spec's gate
  ("only call when `has_lyrics=='true'`") silently yields zero lyrics if fed from details.

**The two calls** (`__call=lyrics.getLyrics`, base params, `ctx=web6dot0`):
- `lyrics_id=aRZbUYD7` (**the song id**) → HTTP 200, **real lyrics**. Response keys:
  `lyrics`, `lyrics_copyright` (`"Lyrics powered by JioSaavn"`), `script_tracking_url`, `snippet`.
  `lyrics` uses `<br>` separators only (no `<br/>` observed); cleaning = spec's
  `replace("<br>", "\n")`, confirmed sufficient.
- `lyrics_id=` (empty — i.e. what `more_info.lyrics_id` actually holds in search payloads) →
  HTTP 200 `{"error":{"code":"INPUT_INVALID","msg":"Empty strings are not allowed."}}` — fails.
- Control, song without lyrics (`yXCLyL-9`, song-id form) → HTTP 200 error body:
  `{"status":"failure","error":{"msg":"Something went wrong please try again"}}`
  (confirms spec §7: errors arrive as HTTP 200 bodies; absence of the `lyrics` key = no lyrics).

**Corrected implementation rule:** call `lyrics.getLyrics` with **`lyrics_id` = the song's own
`id`** (the jiosaavn-dl behaviour — dl is right, the wrapper-model inference was wrong). Do not
gate on `song.getDetails.has_lyrics` (always `"false"` in probes); if a gate is wanted, the
*search-result* payload's `has_lyrics` was accurate for every song checked. Treat any body
without a `lyrics` key as "no lyrics available".

## 4. Browse modules (Home feed) — ✅ PASS (structure simpler than spec §6 implies)

**Request:** `api.php?__call=content.getBrowseModules&…base…` — HTTP 200, **169,214 bytes**
(spec recorded ~169 KB ✓).

Top-level keys **exactly** as specced: `radio, browse_discover, new_albums, charts, top_shows,
new_trending, top_playlists`. Item counts and shapes in this probe:

| Section | Shape | Count |
|---|---|---|
| `new_trending` | top-level **array** of entities | 24 |
| `new_albums` | top-level **array** | 20 |
| `charts` | top-level **array** of playlist-shaped items | 7 |
| `top_playlists` | top-level **array** | 24 |
| `browse_discover` | top-level **array** of channel items (`type="channel"`) | 28 |
| `radio` | object `{featured_stations: [...]}` (`type="radio_station"`) | 28 |
| `top_shows` | object `{shows: [...], badge, last_page}` | 92 shows |

**Corrections/notes vs spec §6:** sections are mostly **direct arrays**, not "nested module data"
objects — DTOs can be simpler than the spec's cautious JsonObject-first sketch for the five array
sections. But entity `type` inside sections is **mixed/unreliable** (`new_trending[0]` is an album;
`new_albums[0]` carries `type="song"` while being album-shaped with a `list`) — branch on
shape/keys, not on `type`, extending the warning the spec gave for `content.getTrending`.

## 5. Album + playlist details — ✅ PASS (`list_count` is the truth)

**Album:** `api.php?__call=content.getAlbumDetails&…base…&albumid=38682222` ("Bhediya") — HTTP 200,
single album object in the §4.7 shape. `list_count="6"` (String) == `len(list)=6` ==
`more_info.song_count="6"`. Full track list in one response, no paging — as specced.

**Playlist:** `api.php?__call=playlist.getDetails&…base…&listid=802336660&n=100&p=0`
("Arijit Singh - Sad Songs - Hindi") — HTTP 200, §4.10 shape. **`list_count="25"` (String) ==
`len(list)=25`** at `n=100`. This confirms the spec's warning concretely: the field is the true
total; the returned `list` is just the requested page (at the wrapper's default `n=10` it would
hold 10 while `list_count` stays 25 — the wrapper's slice-length overwrite is what corrupted the
count). App rule stands: trust `list_count`, page with `p++` until accumulated ≥ `list_count`.

## 6. Suggestions / radio two-step — ⚠️ PARTIAL: step 1 ✅, step 2 ❌ FAILS as specced

**Step 1** `api.php?__call=webradio.createEntityStation&…&ctx=android&entity_id=["<songId>"]&entity_type=queue`
— HTTP 200 `{"stationid": "…"}` for every seed tried (2 songs + 1 featured-station name). Exactly as specced.

**Step 2** `api.php?__call=webradio.getSong&…&ctx=android&stationid=<id>&k=<5|10>` — **never returned
songs**. Every attempt (3 different stations, fresh station each time, k=5 and k=10) returned
HTTP 200 with:
```json
{"stationid": "<same id>", "error": "No new song found for current radio."}
```
Variant with `ctx=web6dot0` for step 2 returned `[]` (empty array) instead. No numeric-keyed song
map (§4.9's documented shape) was ever observed.

**Assessment:** the repo's construction was followed exactly, so this is an upstream/behaviour
divergence, not a spec transcription error — consistent with the wrapper's own suggestions
endpoint being reported broken upstream in the past (API_RESEARCH). **Do not ship autoplay
suggestions on this flow without a new investigation** (candidates: different `entity_type`,
additional params upstream now requires, or geo/IP-dependent station population — all unverified).
"No new song found" must be handled as a normal empty-result in any case.

---

## Spec-correction summary (for the implementer)

1. **Lyrics (§5):** parameter is the **song id**, full stop. `more_info.lyrics_id` is `""` in
   search payloads and absent in details payloads; the "try lyrics_id first" fallback is harmful
   (empty string → `INPUT_INVALID` error). Never gate on `song.getDetails.has_lyrics` (`"false"`
   even for songs with lyrics); search-payload `has_lyrics` was accurate in all probes.
2. **Global search (§4.1):** response also contains `episodes` and `shows` sections.
3. **Paged search (§4.2):** `start` can be negative (-4 observed at p=0) — don't derive paging from it.
4. **Browse modules (§6):** five of seven sections are direct top-level arrays; only `radio`
   (`featured_stations`) and `top_shows` (`shows`) are wrapper objects. Entity `type` values
   inside sections are unreliable — branch on shape.
5. **Suggestions (§4.9):** step 2 currently returns `{"stationid", "error": "No new song found
   for current radio."}` for all stations probed — feature is **not** validated; treat as broken
   upstream pending re-investigation.
6. Everything else in the spec — base-param construction, UA-only headers, song/album/playlist
   shapes, stringly-typed fields, DES-ECB key `"38346591"` + `_96` ladder, image ladder inputs,
   `list_count` semantics, HTTP-200 error bodies — **matched live behaviour**.
