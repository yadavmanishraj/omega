# STUDY.md — deep study findings & how they map to this app

Study clones (kept separate, not part of the app): `~/workspace/jiosaavn-study/{jiosaavn-api,jiosaavn-dl,skills}`

## 1. sumitkolhe/jiosaavn-api (TypeScript)
- **Stack:** TypeScript on Hono (`@hono/zod-openapi`), zod schemas double as OpenAPI docs + runtime validation, Vitest specs per controller/use-case, deploy targets Vercel / Cloudflare Workers (`wrangler.toml`) / Docker / Bun. Every module follows `controllers → services → use-cases → helpers → models` — the same separation this app mirrors in Kotlin (`SaavnApi → MusicRepository → ViewModels → screens`).
- **Public instance / base URL:** docs at https://saavn.dev/docs ; routes are mounted under `/api` in `src/app.ts`, so the client base URL is `https://saavn.dev/api/`. No auth, no API key, no rate-limit documented in the repo (upstream JioSaavn may throttle; app therefore retries via UI error states, not aggressive polling).
- **Endpoints actually in the controllers** (verified in source, not just README):
  | App call | Route | Upstream JioSaavn (`endpoint.constant.ts`) |
  |---|---|---|
  | GET `search?query=` | Search all (topQuery+songs+albums+artists+playlists) | `autocomplete.get` |
  | GET `search/songs?query=&page=&limit=` | Song search | `search.getResults` |
  | GET `search/albums` / `search/artists` / `search/playlists` | Typed search | `search.getAlbumResults` / `getArtistResults` / `getPlaylistResults` |
  | GET `songs/{id}` (+ `?ids=`, `?link=`, `?lyrics=true`) | Song details, optional lyrics embedded | `song.getDetails`, `lyrics.getLyrics` |
  | GET `songs/{id}/suggestions?limit=` | Suggestions / radio-style queue | `webradio.getSong` (+ station `webradio.createEntityStation`) |
  | GET `albums?id=` / `albums?link=` | Album + full track list | `content.getAlbumDetails` |
  | GET `playlists?id=&page=&limit=` / `?link=` | Playlist + songs | `playlist.getDetails` |
  | GET `artists/{id}`, `artists/{id}/songs`, `artists/{id}/albums` | Artist page, paged songs/albums | `artist.getArtistPageDetails`, `artist.getArtistMoreSong/Album` |
  (The constants file also names `content.getTrending` / `content.getBrowseModules`, but no controller in the current tree exposes a home/trending route — so Home in this app is built from curated search carousels, and this gap is documented rather than faked.)
- **Response envelope & models:** every response is `{ success: boolean, data: … }`. Clean models (`SongModel`, `AlbumModel`, `PlaylistModel`, `ArtistModel`, search models) are mapped from raw JioSaavn payloads by helpers. Song fields used by the app: `id, name, year, releaseDate, duration (sec), label, explicitContent, playCount, language, hasLyrics, lyricsId, url, album{id,name,url}, artists{primary,featured,all}, image[], downloadUrl[]`.
- **downloadUrl / image structure (`common/helpers/link.helper.ts`):** the API DES-decrypts JioSaavn's `encrypted_media_url` and rewrites the `_96` marker to produce **5 qualities: 12, 48, 96, 160, 320 kbps** (`.mp4`/AAC streams). Images are rewritten to **50x50, 150x150, 500x500**. App rule: playback/download pick the user-selected quality, falling back to the highest available (`urlForQuality()` in `Dtos.kt`, unit-tested); artwork picks the largest image.
- **Lyrics:** there is no standalone `/lyrics` route in the current controllers; lyrics come back on the song payload when requested (`GET songs/{id}?lyrics=true`). The app does exactly that in `MusicRepository.songWithLyrics()`.

## 2. bunnykek/jiosaavn-dl (Python)
- Single-file CLI (`jiosaavn.py`, deps: requests, mutagen, sanitize-filename, pycryptodome). Takes a JioSaavn song/album/playlist URL, resolves IDs, fetches the highest quality (**320 kbps AAC in an .m4a container**), skips already-downloaded files, checks regional availability, and writes to `Downloads/`.
- **Naming:** album → folder `Artist - Album [Year]/`, tracks `NN. Title.m4a`; single tracks `Artist - Title.m4a`; all names passed through `sanitize()` + HTML-unescape. → Mirrored in `domain/Models.kt`: `sanitizeFileName()` + `downloadFileName()` (unit-tested), WorkManager `DownloadWorker` saves `.m4a` under `Music/SaavnMusic/`.
- **Tagging:** mutagen MP4 atoms — title, album, artist, composer, album-artist, track n/of, year/date, copyright, lyrics, cover art, language, label. → Android equivalent: the worker stores a metadata sidecar JSON and Room row (title/artist/album/artwork/quality/size/path); full in-file M4A atom embedding is the one jiosaavn-dl behaviour **not** fully ported (noted honestly — MediaMetadata is instead attached to the Media3 `MediaItem`, so the player/notification show full metadata).

## 3. android/skills — catalog & what was actually applied
Repo layout (categories → skills): build-system/agp-9-upgrade, camera/camerax, device-ai/{appfunctions, ml-kit-genai-prompt-api}, devtools/android-cli, identity/{restore-credentials, verified-email}, jetpack-compose/{adaptive, migration, theming(styles)}, media/media3-cast-integration, navigation/{navigation-3, navigation-event}, performance/r8-analyzer, play/{engage-sdk, play-billing-upgrade, play-policy-insights}, profilers/android-profiler, security/{android-intent-security, android-permissions-security}, system/edge-to-edge, testing/testing-setup, tv/leanback-migration, wear/wear-compose-m3, xr/glimmer.
Applied:
- **system/edge-to-edge** — `enableEdgeToEdge()` before `setContent`, `adjustResize`, Scaffold insets in `MainActivity.kt`.
- **jetpack-compose/theming (+ adaptive patterns)** — Material 3 `darkColorScheme`/dynamic color, dark-first palette, in `ui/theme/Theme.kt`; layouts use LazyRow carousels + LazyColumn/Grid-friendly components that adapt to width.
- **navigation/navigation-3** — *evaluated and documented, not adopted blindly:* the skill is a Nav2→Nav3 migration guide; this is a greenfield app on the stable Navigation Compose 2.8 with per-tab `saveState/restoreState` multiple-back-stack behaviour (the exact pattern Nav3's "Common UI" recipe describes). Adopting the still-new Nav3 runtime was judged riskier for build stability; the destination structure (typed ids for album/playlist/artist) maps 1:1 to Nav3 `NavKey`s for a future migration.
- **media/media3-cast-integration** — its core stack requirements are followed (unified Media3 versions for exoplayer/session/ui, MediaSessionService architecture); the Cast module itself is **not** added because it requires Media3 ≥ 1.9 + a Cast receiver app-id — recorded as a follow-up, not silently claimed.
- **security/android-permissions-security** — minimal permission set only (INTERNET, network state, foreground-service media playback, notifications); app-specific download directory so **no storage permission** is requested; no exported components except the launcher activity and MediaSession service.
- **security/android-intent-security** — no custom deep-link/intent handling surface; only MAIN/LAUNCHER and Media3 session intent-filters are exported.
- **testing/testing-setup** — its analysis checklist (DI framework, JUnit, serialization parsing) drove the test setup: JUnit4 + kotlinx-coroutines-test unit tests for JSON parsing against a real response-shaped fixture, quality selection and file naming (`app/src/test/.../ParsingAndRepositoryTest.kt`); Compose UI / screenshot tests are scaffolded as future work per the skill's strategy doc approach.
- **performance/r8-analyzer** — release build enables R8 minify + resource shrink with keep rules for serialized DTOs in `app/proguard-rules.pro`; the analyzer skill's workflow applies once a release mapping file exists (post-first-release step).
- **identity skills — deliberately NOT applied:** restore-credentials / verified-email exist in the repo, but the product requirement is *no login / no accounts*, so there is nothing to restore or verify. This is a conscious, documented exclusion.
- Not applicable here: camera, wear, tv, xr, play-billing (no purchases), device-ai — catalogued above for completeness.

## 4. No-auth & UI/UX system (top-priority refinement)
- No auth screens, no account requirement, no onboarding gate; first composition is Home. Library = Room only.
- Bottom nav Home / Search / Library (+ Settings), persistent mini-player above it; mini→full player is a state switch sharing the same `PlayerController` StateFlow (shared-element feel, same artwork component at both sizes).
- Design tokens: dark-first M3 scheme (lime primary on near-black green-tinted surfaces), dynamic color opt-in, large artwork (500x500 URL), shimmer skeletons (`ShimmerList`), `ErrorState` with retry, `EmptyState`s everywhere a list can be empty.
- Quality model everywhere: Settings holds separate playback & download quality from the API's {48,96,160,320} kbps user-facing set (12 kbps exists in the API but is not offered as a listening quality).

## 5. Build verification (honest status — updated after real attempts)
- Installed under `~/workspace/tools`: **Temurin JDK 17.0.20.1** (direct tarball), **Gradle 8.9** (direct zip), **Android SDK platform 35 + build-tools 35.0.0** (direct zips from dl.google.com, unpacked manually).
- `sdkmanager` could NOT install packages here: its remote fetch dies with `java.util.NoSuchElementException at HttpURLConnection.doTunneling` through this environment's egress proxy — hence the manual zip install above.
- Gradle could NOT run any build here (verified 2026-10-07): even an empty project (`gradle help --no-daemon`, fresh `GRADLE_USER_HOME`, proxies unset, IPv4-stack forced) fails with:
  ```
  FAILURE: Build failed with an exception.
  * What went wrong: Could not receive a message from the daemon.
  (--info log: "Removing daemon from the registry due to communication failure", daemon on 127.0.0.1)
  ```
  The Gradle daemon starts, but client↔daemon communication is blocked in this sandbox, so `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` are **UNVERIFIED — not claimed as passing**. No APK was produced.
- Wrapper: `gradle/wrapper/gradle-wrapper.jar` + `gradle-wrapper.properties` (Gradle 8.9) and `gradlew`/`gradlew.bat` were placed from the official gradle/gradle v8.9.0 sources because `gradle wrapper` itself needs the daemon.
- Static checks that DID pass: both XML files parse as well-formed; every Kotlin file has balanced braces/parens; version catalog + build scripts reviewed against the pinned versions (AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Media3 1.5.1, Room 2.6.1, Hilt 2.52).
- First thing to do on a normal dev machine / CI: `./gradlew testDebugUnitTest assembleDebug` and fix any compile errors that surface (the code has never been compiler-checked).
