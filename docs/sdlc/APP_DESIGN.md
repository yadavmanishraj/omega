# Omega — Application Design / Architecture (SDLC Design Document)

| Field | Value |
|---|---|
| Product (working name) | **Omega** — a JioSaavn music client. Publishing repo: `yadavmanishraj/omega` |
| Package / applicationId | `com.manishraj.saavnmusic` (already scaffolded in the builder tree; do not rename mid-build) |
| Owner | Manish Raj Yadav (`yadavmanishraj`) |
| Phase (SDLC) | Design — precedes Implementation (§13 is the builder's contract) |
| Status | v1.0 — 2026-10-07 |
| Inputs studied | `sumitkolhe/jiosaavn-api` (TypeScript/Hono, public instance `https://saavn.dev`), `bunnykek/jiosaavn-dl` (Python downloader), `android/skills` (Google, Apache-2.0) — all cloned under `~/workspace/jiosaavn-study/` |
| Hard constraints | **No login, no sign-up, no accounts, no tracking.** Kotlin, Jetpack Compose + Material 3, single device, local-only library. Unofficial third-party API — the app must degrade gracefully when it changes or dies. |

> Companion documents (sibling SDLC agents): REST API research → `~/workspace/sdlc/research/`; UI/UX design → `~/workspace/sdlc/uiux/`. Where this document and the UI/UX document both describe a screen, UI/UX owns pixels/tokens and this document owns state, data, and behaviour.

---

## Table of contents

1. Requirements
2. android/skills mapping
3. Architecture overview
4. Data design
5. Playback design
6. Download design
7. Navigation & state management
8. Error handling & threading
9. Testing strategy
10. CI/CD, release, signing, versioning
11. Security & privacy
12. Risks, decisions & trade-offs
13. Build sequence (SDLC implementation plan) with per-phase acceptance criteria

---

## 1. Requirements

### 1.1 Vision

A professional-grade, fully-featured music app that opens **straight into music** — no splash gate, no account wall, no onboarding form. Search, browse, play, favourite, organise into local playlists, and download for offline — all state lives on the device.

### 1.2 Functional requirements (FR)

| ID | Requirement | API source (verified in `jiosaavn-api/src`) |
|---|---|---|
| FR-1 | Home feed: carousels of albums / playlists / artists / trending searches assembled from global-search seeds and curated search terms (the API has **no dedicated home/trending endpoint** — see §12 D-6) | `GET /api/search?query=` |
| FR-2 | Global search across songs, albums, artists, playlists in one call, with a "top result" section | `GET /api/search?query=` |
| FR-3 | Type-specific search with paging (`page` 0-based, `limit`, response `{total, start, results}`) | `GET /api/search/songs|albums|artists|playlists` |
| FR-4 | Song detail incl. lyrics when `hasLyrics=true` (`?lyrics=true`) | `GET /api/songs/{id}`, `GET /api/songs?id=` / `?link=` |
| FR-5 | Album detail with full track list, play/shuffle all | `GET /api/albums?id=` / `?link=` |
| FR-6 | Artist detail: top songs, top albums, singles, similar artists, bio; paged artist songs/albums (`sortBy`, `sortOrder`) | `GET /api/artists/{id}`, `/{id}/songs`, `/{id}/albums`, `?link=` |
| FR-7 | Playlist detail (Saavn-curated/user playlists) with track list | `GET /api/playlists?id=&page=&limit=`, `?link=` |
| FR-8 | Playback: stream at user-selected quality from `downloadUrl[]`, queue, next/prev, seek, shuffle, repeat (off/all/one), suggestions/autoplay queue extension | `Song.downloadUrl[]`; `GET /api/songs/{id}/suggestions?limit=` |
| FR-9 | Lyrics view for the now-playing song; graceful "no lyrics" state | FR-4 with `lyrics=true` → `lyrics.lyrics` (HTML `<br>` → newlines, as `jiosaavn-dl` does) |
| FR-10 | Favourites: favourite/unfavourite any song from any list or the player; Favourites screen | Local only (Room) |
| FR-11 | Local playlists: create / rename / delete, add/remove/reorder songs, play a local playlist | Local only (Room, cross-ref table) |
| FR-12 | Recently played + full play history (local, capped, clearable) | Local only (Room) |
| FR-13 | Recent searches: save, tap to re-run, clear individually / all | Local only (Room) |
| FR-14 | Downloads: download song/album/playlist at selected quality (highest available ≤ preference, mirroring `jiosaavn-dl`), progress, pause-by-cancel, retry, delete; downloaded items playable fully offline | `downloadUrl[]` (12/48/96/160/320 kbps AAC) + WorkManager |
| FR-15 | Settings: API base URL (default `https://saavn.dev`), streaming quality, download quality, theme (System/Light/Dark, default Dark), dynamic colour toggle, playback cache size, clear history/searches/cache, storage usage | Local only (DataStore) |
| FR-16 | Deep-link / paste-a-link: open a `jiosaavn.com` song/album/artist/playlist link via the API's `link=` variants | `GET /api/{songs,albums,artists,playlists}?link=` |
| FR-17 | Share a song (plain-text share sheet with the Saavn `url`); no account involved | Android Sharesheet |
| FR-18 | Sleep timer, queue screen (view, jump, remove, reorder is v1.1), add-to-playlist from any song row | Local / Media3 |

### 1.3 Non-functional requirements (NFR)

- **NFR-1 Cold start to interactive Home ≤ 2 s** on a mid-range device (cache-first rendering, §4.5); playback service must not block first frame.
- **NFR-2 Playback start ≤ 1.5 s** on 4G for a cached-URL song; no ANR on main thread — all I/O off-main (§8.2).
- **NFR-3 Offline resilience:** favourites, playlists, history, downloads fully usable with airplane mode on; network screens show cached content + offline banner, never a dead end.
- **NFR-4 API fragility tolerance:** DTO parsing must tolerate nulls/missing fields (upstream is an unofficial scraper; fields flip between string/number). One bad item must not fail a whole list (§4.2, §8.1).
- **NFR-5 Battery/data:** streaming cache bounded (default 256 MB, §5.5); downloads only via WorkManager constraints the user controls (unmetered-only toggle, default off); no polling.
- **NFR-6 Accessibility:** all interactive elements ≥ 48 dp, content descriptions on icon buttons, TalkBack-traversable player, no colour-only state.
- **NFR-7 Adaptivity:** phone portrait is primary; layout adapts to tablets/foldables (list-detail where the UI/UX doc specifies) — see skill `adaptive` (§2).
- **NFR-8 Privacy:** zero analytics/crash-reporting SDKs, zero accounts, no device identifiers leave the device; the only egress is to the configured API base URL and JioSaavn/CDN image & media hosts (§11).
- **NFR-9 Release quality:** R8-minified release build, lint clean (or baselined), unit tests green in CI before merge (§9, §10).
- **NFR-10 Platform:** `minSdk 26`, `targetSdk 35`, `compileSdk 35`, JDK 17, Kotlin 2.0.21, AGP 8.7.3 (version catalog already in the builder tree — keep, don't churn).

### 1.4 User stories (selected, with acceptance)

1. *As a listener, I open the app and immediately see music I can play* — no login/onboarding gate; Home renders from cache, then refreshes.
2. *As a listener, I search "Arijit Singh" and get songs, albums, the artist, and playlists in one screen,* then drill into the artist's top songs.
3. *As a listener, I tap a song and it keeps playing* while I browse, lock the screen, or use other apps; I can control it from the notification.
4. *As a commuter, I download a playlist at 160 kbps on Wi-Fi and play it in a tunnel* with no network, from the Downloads screen.
5. *As an organiser, I build a "Gym" playlist on-device,* add songs from search, album, and history screens, and it survives restarts.
6. *As a returning user, my favourites, history, and recent searches are still there* — and I can wipe any of them in Settings.
7. *As a self-hoster, I point the app at my own `jiosaavn-api` instance* by editing the base URL in Settings; no recompile.

### 1.5 Scope

**In scope (v1.0):** FR-1…FR-18 above; one Android phone/tablet app; English UI (catalogue content is multilingual by nature).

**Out of scope:** any account system, sync, social/follow features (upstream `isFollowed` flags are ignored), comments, podcasts/radio stations **as a feature** (the API exposes no first-class radio route in the studied tree — only `isRadioPresent` flags; suggestions (FR-8) cover the "radio-like" need), video, casting (**deferred, designed-for** — §2, §5.6), Wear/TV/XR surfaces, iOS/desktop, Play Billing, an in-app API server, downloading with burnt-in ID3/m4a tagging parity with `jiosaavn-dl` beyond title/artist/album/cover embedded via Media3/WorkManager metadata (full Mutagen-style tagging is v1.1, §6.5), equaliser (platform `AudioEffect` is v1.1).

### 1.6 Success criteria (release gate for v1.0)

- [ ] All FRs demonstrably work against the live `https://saavn.dev` instance (or a recorded-fixture fallback if it is down at test time).
- [ ] `./gradlew testDebugUnitTest lint assembleDebug assembleRelease` green locally and in GitHub Actions on `yadavmanishraj/omega`.
- [ ] No login/sign-up/account string or screen exists anywhere in the APK (grep + manual pass).
- [ ] Airplane-mode test: favourites, a local playlist, history, and ≥ 1 downloaded album play end-to-end.
- [ ] Notification + lockscreen controls verified on a real or emulated Android 14/15 device (foreground-service type `mediaPlayback`).

---

## 2. android/skills mapping

Skills are named **exactly as in the studied repo** (`~/workspace/jiosaavn-study/skills/**/SKILL.md`, front-matter `name:`). The repo is a catalogue of narrow recipes, *not* a general architecture guide — general MVVM/Compose/Hilt practice comes from Now-in-Android / architecture guidance and is marked *(platform guidance)* below rather than being misattributed to a skill.

### 2.1 Applied in v1.0

| Skill (repo path) | What it actually covers | Where it maps in Omega |
|---|---|---|
| `edge-to-edge` (`system/edge-to-edge`) | Migrate Compose apps to edge-to-edge; fix status/nav-bar overlap, IME insets, bar legibility | `MainActivity` (`enableEdgeToEdge`), a single insets policy consumed by the app scaffold, mini-player, and full player (artwork under status bar, controls padded by `WindowInsets`). Acceptance: no control is obscured on gesture-nav or 3-button devices |
| `adaptive` (`jetpack-compose/adaptive`) | Window-size classes, multi-pane (Navigation 3 Scenes), nav rail vs bar, Grid/FlexBox | Home/Search/Library scaffolding switches bottom-bar ↔ nav-rail at `widthSizeClass >= Medium`; Album/Artist/Playlist detail uses list-detail two-pane on expanded width. Phone layouts are unchanged by this |
| `navigation-3` (`navigation/navigation-3`) | Jetpack Navigation 3: install/migrate, deep links, multiple back stacks, Scenes (dialog, bottom-sheet, list-detail), Hilt + ViewModel integration, returning results | **Decision point — see D-2 (§12).** Design target is Navigation 3 typed keys + per-tab back stacks (§7.1). If the builder's pinned catalog (Navigation Compose 2.8.5, already in the tree) is retained for v1.0, the *structure* in §7.1 (typed destinations, per-tab stacks, result-returning flows) must still be followed so migration is mechanical. Whichever is shipped, record the choice in `STUDY.md` |
| `navigation-event` (`navigation/navigation-event`) | Predictive Back via `androidx.navigationevent`, `NavigationBackHandler`, dispatcher scoping in tab/pager hosts, SDK 36 migration notes | Full-player bottom-sheet and queue sheet use predictive-back-aware handlers; tab host scopes back handling so Back collapses the player before popping a tab stack |
| `testing-setup` (`testing/testing-setup`) | Analyse/create a testing strategy: libraries, unit/UI/screenshot/e2e harnesses | The whole of §9 is structured per this skill: test pyramid, fakes-first harness, Compose UI tests, Paparazzi-style screenshot tests optional in v1.1 |
| `android-cli` (`devtools/android-cli`) | `android` CLI: project creation, AVD management, screenshots/UI inspection, SDK management, `android skills add` | Developer workflow only (not shipped code): project scaffolding checks, emulator screenshots for UI review, `android skills add --all` to install this catalogue into the project for future agents |
| `r8-analyzer` (`performance/r8-analyzer`) | Audit R8/ProGuard rules for redundancies and over-broad keeps; app-size optimisation | Release phase (§10.3, Phase 8 in §13): audit `proguard-rules.pro` — expected keeps are *only* kotlinx-serialization DTO rules generated narrowly per-package, plus Media3/Room consumer rules. No `-keep class com.manishraj.** { *; }` |
| `android-profiler` (`profilers/android-profiler`) | Record/analyse system traces, heap dumps, startup & jank, memory | Phase 8 performance pass: cold-start trace, Home scroll jank, player memory (ExoPlayer + Coil caches). Tooling/process only |
| `android-intent-security` (`security/android-intent-security`) | Intent redirection, exported-component and extra-handling hygiene | Manifest audit: only `MainActivity` exported (launcher + `jiosaavn.com` app-links/deep-links, validated — §11); `PlaybackService` and receivers **not** exported; deep-link `link=` values treated as untrusted input, scheme/host allow-listed before hitting the API |
| `android-permissions-security` (`security/android-permissions-security`) | Permission/IPC audit, runtime flows, caller verification | Permission minimisation (§11): `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS` (runtime, Android 13+), `WAKE_LOCK`. **No** storage permission (app-specific storage), no contacts/location/etc. |
| `play-policy-insights` (`play/play-policy-insights`) | Cross-check code vs Play declarations: undeclared data collection, Data Safety, account/identity domains | Only if a Play release is ever attempted: produce the Data Safety answers from §11 (no data collected/shared, no account creation/deletion flow needed *because there are no accounts*). Also the checkpoint that surfaces the unofficial-API/IP risk (§12 R-1) before any store submission |

### 2.2 Designed-for, deferred past v1.0

| Skill | Deferred use |
|---|---|
| `media3-cast-integration` (`media/media3-cast-integration`) | Cast: adds deps, manifest `OptionsProvider`, `CastPlayer`/`RemoteCastPlayer` swap behind the `PlayerController` interface (§5.6). The playback abstraction is designed so this is an additive change, not a refactor |
| `appfunctions` (`device-ai/appfunctions`) | Expose "play my favourites / play <search>" to on-device agents/voice. Nice differentiator once playback is stable; requires no account, fits the privacy model |
| `agp-9-upgrade` (`build-system/agp/agp-9-upgrade`) | When moving off AGP 8.7.3: built-in Kotlin migration, KSP/kapt and BuildConfig changes. Do **not** upgrade mid-v1.0 |

### 2.3 Explicitly **not** applicable — and why (recorded so nobody "adds them for completeness")

| Skill | Why not |
|---|---|
| `restore-credentials`, `verified-email` (`identity/…`) | Both are sign-in/credential flows. The product has **no accounts by design** — applying them would violate the core constraint |
| `play-billing-library-version-upgrade`, `engage-sdk-integration` (`play/…`) | No purchases, no Play Engage publishing in v1.0 |
| `camerax`, `ml-kit-genai-prompt-api`, `wear-compose-m3`, `leanback-to-compose-tv-migration`, display-glasses Glimmer skill | No camera, no on-device GenAI, no Wear/TV/XR surface in scope (§1.5) |

---

## 3. Architecture overview

### 3.1 Style

**MVVM + Repository, unidirectional data flow (UDF), single-activity Compose.** *(Platform guidance: "Guide to app architecture" / Now in Android.)*

```
UI (Compose screens + ViewModels)          ── UiState (StateFlow) ──▶  renders
        │  ▲ events (method calls / UiAction)        │
        ▼  │                                          │
Domain (use cases, pure models, repository INTERFACES)
        │
        ▼
Data (repository IMPLs, Retrofit API, Room DAOs, DataStore, mappers)
        │
        ├──▶ Network: jiosaavn-api instance (default https://saavn.dev)
        ├──▶ Room DB (library: favourites / playlists / history / downloads / searches)
        └──▶ DataStore Preferences (settings)
Playback (Media3 service + PlayerController) — sits beside Data, fed by Domain models
Downloads (WorkManager) — a Data-layer worker writing files + Room state
```

Rules: UI never touches DTOs, DAOs, or Retrofit. Domain never imports Android framework types (except `Uri`-free value types). Data never imports Compose. Mappers live at the data/domain boundary (§4.2).

### 3.2 Module decision — single app module, strict packages (D-1)

**Recommendation: keep the builder tree's single `:app` module for v1.0**, with package-level modularisation that mirrors the future multi-module split, enforced by package conventions + (optionally) Konsist/ArchUnit tests in `src/test`.

Rationale: the future split (`:core:*` + `:feature:*`) costs Gradle/CI complexity and slows a single-developer (agent) build; the API surface between areas is still churning. Package discipline now makes extraction mechanical later. **Trigger to split:** second developer, build time > ~3 min incremental pain, or Cast/Wear added.

Package map under `com.manishraj.saavnmusic` (aligned to what the builder already scaffolded — evolve, don't restart):

```
saavnmusic/
├─ SaavnApplication.kt            @HiltAndroidApp; WorkManager Configuration.Provider (Hilt worker factory)
├─ MainActivity.kt                single activity, edge-to-edge, hosts AppScaffold + NavHost
├─ core/
│  ├─ result/      AppResult, AppError, DataError (§8.1)
│  ├─ dispatchers/ DispatcherQualifiers (@IoDispatcher, @DefaultDispatcher, @MainDispatcher)
│  ├─ network/     BaseUrlInterceptor (dynamic base URL from DataStore), Json config, network monitor
│  └─ common/      Duration/format helpers, ImageSize picker (image[] 50/150/500)
├─ domain/
│  ├─ model/       Song, Album, Artist, Playlist, Lyrics, DownloadItem, SearchResults, enums (Quality, RepeatMode…)
│  ├─ repository/  MusicRepository, LibraryRepository, SettingsRepository, DownloadRepository (interfaces)
│  └─ usecase/     SearchAll, GetAlbum, GetArtist, GetPlaylist, GetSuggestions, PlaySongs, ToggleFavourite,
│                  CreatePlaylist, EnqueueDownload … (thin; only where logic is shared by ≥2 screens or player)
├─ data/
│  ├─ remote/      SaavnApi (Retrofit), dto/ (one file per resource), mapper/ (DTO→domain)
│  ├─ local/       AppDatabase, dao/, entity/, mapper/ (entity↔domain)
│  ├─ repository/  MusicRepositoryImpl, LibraryRepositoryImpl, DownloadRepositoryImpl
│  └─ settings/    SettingsRepositoryImpl (DataStore), SettingsKeys (§4.6)
├─ playback/
│  ├─ PlaybackService.kt         MediaSessionService (Media3)
│  ├─ PlayerController.kt        interface + MediaController-backed impl used by UI
│  ├─ QueueManager.kt            queue state machine (§5.3)
│  └─ PlaybackStateMapper.kt     Media3 state → PlaybackUiState
├─ download/
│  ├─ DownloadWorker.kt          CoroutineWorker (one song per work item)
│  ├─ AlbumPlaylistDownloadEnqueuer.kt
│  └─ DownloadFileStore.kt       paths, temp files, size accounting
└─ ui/
   ├─ app/         AppScaffold (adaptive nav, mini-player slot), AppNavHost, destinations
   ├─ theme/       Theme.kt, Color, Type, artwork-palette extraction helper
   ├─ components/  SongRow, AlbumCard, ArtistCard, Carousel, Shimmer, ErrorRetry, EmptyState, QualityBadge…
   ├─ home/ search/ library/ downloads/ settings/     (one package per feature: Screen + ViewModel + UiState)
   ├─ detail/      AlbumDetail, ArtistDetail, PlaylistDetail
   └─ player/      FullPlayer, MiniPlayer, QueueSheet, LyricsSheet
```

**Future extraction map** (when D-1's trigger fires): `core/*` → `:core:common`, `:core:network`, `:core:database`, `:core:designsystem` (from `ui/theme`+`ui/components`); `ui/<feature>` → `:feature:<feature>`; `playback` → `:core:playback`; `download` → `:core:download`. Interfaces in `domain` are the seams — that is why repositories are interfaces *now*.

### 3.3 Dependency graph (compile-time)

```
:app (ui) ──▶ domain ──▶ (nothing Android)
   │            ▲
   ├──▶ playback ──▶ domain, data(repository impls provide Song→MediaItem resolution)
   ├──▶ download ──▶ domain, data
   └──▶ data ──▶ domain, core
core ──▶ (AndroidX base only)
```
No cycles; `data` implements `domain` interfaces; Hilt binds them (§3.4). UI depends on use cases / repository interfaces and on `PlayerController`, never on impls.

### 3.4 Dependency injection (Hilt)

| Module | Scope | Provides / binds |
|---|---|---|
| `NetworkModule` (`core/network`, `@InstallIn(SingletonComponent)`) | `@Singleton` | `Json` (lenient, `ignoreUnknownKeys`, §4.2), `OkHttpClient` (logging in DEBUG only, timeouts 15 s connect / 30 s read, `BaseUrlInterceptor`), `Retrofit`, `SaavnApi` |
| `DatabaseModule` | `@Singleton` | `AppDatabase` (Room, §4.4), DAOs (`SongDao`-style split: `FavouriteDao`, `PlaylistDao`, `HistoryDao`, `DownloadDao`, `RecentSearchDao`) |
| `DataStoreModule` | `@Singleton` | `DataStore<Preferences>`, `SettingsRepositoryImpl` |
| `RepositoryModule` | `@Singleton` binds | `MusicRepositoryImpl→MusicRepository`, `LibraryRepositoryImpl→LibraryRepository`, `DownloadRepositoryImpl→DownloadRepository` |
| `DispatcherModule` | — | `@IoDispatcher CoroutineDispatcher = Dispatchers.IO`, `@DefaultDispatcher`, `@ApplicationScope CoroutineScope` (SupervisorJob + IO) — used by player/download bookkeeping that outlives screens |
| `PlaybackModule` | `@Singleton` | `PlayerController` (binds to the app's `MediaController` connection to `PlaybackService`), `QueueManager` |
| `WorkerModule` (`androidx.hilt.work`) | — | `HiltWorkerFactory`; `SaavnApplication` implements `Configuration.Provider`. `DownloadWorker` is `@HiltWorker` with `@AssistedInject` |

Notes: no `kapt` — KSP only (catalog already does this). ViewModels are `@HiltViewModel`; Compose gets them via `hiltViewModel()` / Navigation-3's Hilt integration per the `navigation-3` skill.

---

## 4. Data design

### 4.1 The API contract as studied (ground truth for DTOs)

Base: `{baseUrl}/api`, envelope on **every** response: `{ "success": Boolean, "data": … }` (on error: `{success:false, message:String}` + HTTP status). Default base `https://saavn.dev`. No auth headers, no API key.

| Endpoint | Params | `data` shape |
|---|---|---|
| `GET search` | `query` | `{ topQuery:{results,position}, songs:{results,position}, albums:{…}, artists:{…}, playlists:{…} }` — each section `results[]` are *lightweight* items (id, title/name, subtitle, type, url, image[]) |
| `GET search/songs` · `search/albums` · `search/artists` · `search/playlists` | `query, page=0, limit=10` | `{ total:Int, start:Int, results:[Full Song / slim Album / slim Artist / slim Playlist] }` |
| `GET songs/{id}` · `songs?id=` · `songs?link=` | `lyrics=true` (optional) | `[Song]` (array even for one) or `{ …, lyrics?: {lyrics, lyrics_copyright, snippet} }` with lyrics flag — builder must pin the exact shape against the live `/docs` (Scalar) + a recorded fixture |
| `GET songs/{id}/suggestions` | `limit=10` | `[Song]` |
| `GET albums?id=` · `albums?link=` | — | `Album { …, songs:[Song] }` |
| `GET artists/{id}` · `artists?link=` | — | `Artist { topSongs, topAlbums, singles, similarArtists, bio[], … }` |
| `GET artists/{id}/songs` · `/{id}/albums` | `page, sortBy=popularity\|latest\|alphabetical, sortOrder=asc\|desc` | `{ total, lastPage:Boolean, results:[…] }` — note: **different paging envelope** (no `start`) |
| `GET playlists?id=` · `playlists?link=` | `page, limit` | `Playlist { …, songs:[Song] }` |

**Canonical Song JSON** (the Zod `SongModel` in the API repo — mirror it field-for-field in the DTO):
`id, name, type, year?, releaseDate?, duration:Int?(seconds), label?, explicitContent:Boolean, playCount:Int?, language, hasLyrics:Boolean, lyricsId?, url, copyright?, album{id?,name?,url?}, artists{primary[],featured[],all[]} (each {id,name,role,image[],url}), image: Link[], downloadUrl: Link[]` where `Link = { quality:String, url:String }`. Image qualities are `"50x50" | "150x150" | "500x500"`; audio qualities are `"12kbps" | "48kbps" | "96kbps" | "160kbps" | "320kbps"` (AAC). A song may have an **empty `downloadUrl[]`** (region/rights) — playback/download must treat that as a first-class unavailable state, not a crash.

### 4.2 Three model tiers — never collapse them

| Tier | Type examples | Rules |
|---|---|---|
| **Network (DTO)** — `data/remote/dto/` | `SongDto`, `AlbumDto`, `ArtistDto`, `PlaylistDto`, `SearchAllDto`, `PagedDto<T>`, `ApiResponse<T>` wrapper | `@Serializable`, in `data` only. Lenient parsing: nullable where upstream is flaky, `@JsonNames`/defaults for renamed fields, numbers-as-strings tolerated via a custom `FlexInt/FlexLong` serializer (upstream raw payloads mix `"320kbps": "true"`, string counts — the *cleaned* API mostly normalises, but defence is cheap at this one seam). Unknown keys ignored |
| **Domain** — `domain/model/` | `Song(id, name, artists:List<ArtistRef>, album:AlbumRef?, durationSeconds, imageUrls:ImageSet, streamLinks:List<StreamLink>, …)`, `Album`, `Artist`, `Playlist`, `Lyrics(text, copyright)`, `SearchResults(topResult, songs, albums, artists, playlists)` | Pure Kotlin, immutable, no Android/serialization annotations. UI, player, downloads, and Room mappers all speak *only* this tier. `ImageSet`/`StreamLink` carry typed quality enums (`ImageQuality`, `AudioQuality`) parsed once in the mapper (`"320kbps"→AudioQuality.K320`, unknown → skip link) |
| **Persistence (Entity)** — `data/local/entity/` | §4.4 | Room `@Entity` data classes + separate mappers `Entity ↔ Domain`. Entities are *snapshots*: a favourited song keeps its own copy of name/artists/image so Library renders fully offline even if the API changes |

Mapping functions are named `SongDto.toDomain()`, `Song.toEntity(source)`, `SongEntity.toDomain(cachedStreamLinks = …)` and live in `data/*/mapper/`. **Stream/download URLs are treated as volatile:** entities store them (needed for offline metadata) but playback/download resolution re-checks freshness — see §5.2/§6.4 stale-URL handling.

### 4.3 Repository responsibilities

- `MusicRepository` (network-backed, stateless): `searchAll(q)`, `searchSongs/Albums/Artists/Playlists(q,page,limit)`, `getSong(id, lyrics)`, `getAlbum(id|link)`, `getArtist(id|link)`, `getArtistSongs/Albums(...)`, `getPlaylist(id|link)`, `getSuggestions(songId)`. All `suspend`, all return `AppResult<T>` (§8.1), all run on `@IoDispatcher`.
- `LibraryRepository` (Room-backed, reactive): `observeFavourites(): Flow<List<Song>>`, `isFavourite(id): Flow<Boolean>`, `toggleFavourite(song)`, `observePlaylists()`, `createPlaylist(name)`, `addToPlaylist(playlistId, song)`, `removeFromPlaylist(...)`, `reorderPlaylist(...)`, `observeHistory(limit)`, `recordPlay(song)`, `clearHistory()`, `observeRecentSearches()`, `saveSearch(q)`, `clearSearches()`. UI collects these Flows — Room is the single source of truth for library state.
- `DownloadRepository`: §6. `SettingsRepository`: §4.6.

### 4.4 Room schema — DB `omega.db`, version 1, `exportSchema = true`

One shared **song snapshot** table is referenced by favourites, playlists, history, and downloads — a song's metadata is stored once, relationships are separate tables. (This avoids the classic bug of four divergent copies of the same song.)

```sql
-- Canonical local snapshot of a Song (domain fields flattened; lists as delimited/JSON columns, see notes)
song (
  id            TEXT PRIMARY KEY,          -- Saavn song id
  name          TEXT NOT NULL,
  primaryArtists TEXT NOT NULL,            -- display string, e.g. "Arijit Singh"
  artistIdsJson TEXT NOT NULL DEFAULT '[]',-- for artist navigation from library rows
  albumId       TEXT, albumName TEXT,
  durationSec   INTEGER,
  language      TEXT NOT NULL DEFAULT '',
  year          INTEGER,
  explicit      INTEGER NOT NULL DEFAULT 0,
  hasLyrics     INTEGER NOT NULL DEFAULT 0,
  lyricsId      TEXT,
  saavnUrl      TEXT NOT NULL DEFAULT '',
  image50       TEXT, image150 TEXT, image500 TEXT,
  streamLinksJson TEXT NOT NULL DEFAULT '[]', -- last-known downloadUrl[] (volatile, §4.2)
  updatedAt     INTEGER NOT NULL           -- epoch ms of last snapshot write
)

favourite (
  songId        TEXT PRIMARY KEY REFERENCES song(id) ON DELETE CASCADE,
  favouritedAt  INTEGER NOT NULL
)
-- index: favouritedAt DESC

local_playlist (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  name          TEXT NOT NULL,
  createdAt     INTEGER NOT NULL,
  updatedAt     INTEGER NOT NULL,
  coverSongId   TEXT                        -- denormalised for grid art (first song's image); nullable
)

playlist_song (                              -- cross-ref, ordered
  playlistId    INTEGER NOT NULL REFERENCES local_playlist(id) ON DELETE CASCADE,
  songId        TEXT NOT NULL REFERENCES song(id) ON DELETE CASCADE,
  position      INTEGER NOT NULL,            -- explicit order; reorder = transactional rewrite
  addedAt       INTEGER NOT NULL,
  PRIMARY KEY (playlistId, songId)           -- a song appears once per playlist (decision D-7)
)
-- index: (playlistId, position)

play_history (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  songId        TEXT NOT NULL REFERENCES song(id) ON DELETE CASCADE,
  playedAt      INTEGER NOT NULL,
  completed     INTEGER NOT NULL DEFAULT 0,  -- reached ~90% or ended naturally
  source        TEXT NOT NULL DEFAULT ''     -- 'search'|'album'|'playlist'|'library'|… (free-form, analytics-free, local UX only e.g. "Jump back in")
)
-- index: playedAt DESC. Cap: keep newest 500 rows (prune in recordPlay transaction).
-- "Recently played" = SELECT DISTINCT songId ORDER BY playedAt DESC LIMIT 20 over this table.

recent_search (
  query         TEXT PRIMARY KEY,            -- normalised: trim + lowercase for dedupe; display keeps last-typed casing in `displayQuery`
  displayQuery  TEXT NOT NULL,
  searchedAt    INTEGER NOT NULL
)
-- Cap 20 rows, prune oldest.

download (
  songId        TEXT PRIMARY KEY REFERENCES song(id) ON DELETE CASCADE,
  state         TEXT NOT NULL,               -- QUEUED|RUNNING|PAUSED?|COMPLETED|FAILED|CANCELLED (§6.2 — no true PAUSED in v1.0)
  quality       TEXT NOT NULL,               -- 'K48'|'K96'|'K160'|'K320' actually fetched
  sourceUrl     TEXT NOT NULL,               -- URL fetched (may go stale pre-retry, §6.4)
  filePath      TEXT,                        -- relative to filesDir/music/, null until COMPLETED
  bytesTotal    INTEGER NOT NULL DEFAULT 0,
  bytesDone     INTEGER NOT NULL DEFAULT 0,  -- progress mirror for UI; WorkManager progress is source during a run
  workId        TEXT,                        -- WorkManager work UUID (string) for cancel/observe; null when terminal
  error         TEXT,                        -- last failure reason code, null on success
  enqueuedAt    INTEGER NOT NULL,
  completedAt   INTEGER
)
-- index: state, enqueuedAt
```

Notes: (a) Boolean columns are `INTEGER 0/1` per Room convention. (b) JSON columns (`artistIdsJson`, `streamLinksJson`) are deliberate denormalisation for a v1 single-device DB — they are *never queried by content*, only read whole with the row; anything queried/filtered gets a real column/table. (c) Migrations: v1 ships with `fallbackToDestructiveMigration` **disabled**; schema JSON committed to `app/schemas/`; every future version needs a `Migration` + migration test (§9). (d) All writes that touch `song` + a relation happen in one `@Transaction` (snapshot upsert first, then relation) via DAO default methods or a `LibraryLocalDataSource`.

### 4.5 Caching (read path, beyond Room)

- **HTTP cache:** OkHttp cache, 10 MB, in `cacheDir`. API responses are not marked cacheable upstream, so this mainly helps images-adjacent/CDN GETs — do not rely on it for correctness.
- **In-memory repository cache:** `MusicRepositoryImpl` keeps a small LRU (`LinkedHashMap`, ~50 entries, 5-min TTL) for album/artist/playlist detail so back-navigation is instant. Search results are *not* cached in memory (freshness + memory).
- **Images:** Coil `ImageLoader` singleton via Hilt: memory cache 25% of app memory class, disk cache 128 MB in `cacheDir/images`. Pick the smallest `image[]` entry ≥ the layout size (50→thumbnails, 150→rows, 500→cards/player).
- **Home cache-first:** Home's seed sections render last-known results persisted as song snapshots? **No** — v1.0 Home is network-first with in-memory state retained by the ViewModel + a shimmer; offline Home shows favourites/history/downloads ("Your music, offline") assembled from Room. This is a deliberate trade-off (D-8) to avoid a second content-cache schema.

### 4.6 DataStore settings (Preferences, file `omega_settings`)

| Key (name · type · default) | Used by |
|---|---|
| `api_base_url` · String · `"https://saavn.dev"` | `BaseUrlInterceptor` rewrites scheme+host of every Retrofit call; Settings validates it parses as http(s) URL, trailing `/` normalised. Changing it requires no restart (interceptor reads a cached volatile value updated from the DataStore Flow in `@ApplicationScope`) |
| `stream_quality` · String(enum name) · `K160` | Player resolution (§5.2): preferred quality for streaming |
| `download_quality` · String(enum name) · `K320` | Download resolution (§6.3): "highest ≤ preference" (jiosaavn-dl semantics) |
| `theme_mode` · String · `DARK` (`SYSTEM\|LIGHT\|DARK`) | Theme |
| `dynamic_color` · Boolean · `false` | Material You where available; artwork palette (player) is separate and always on |
| `stream_cache_max_mb` · Int · `256` | Media3 `SimpleCache` cap (§5.5) |
| `downloads_wifi_only` · Boolean · `false` | WorkManager `NetworkType.UNMETERED` vs `CONNECTED` (§6.2) |
| `autoplay_suggestions` · Boolean · `true` | Queue extension at end of queue (§5.3) |
| `sleep_timer_end_ms` · Long · absent | Persisted so a timer survives process death (checked/cleared by player on start) — nullable, internal |

Settings are exposed as one `Flow<AppSettings>` data class from `SettingsRepository`; writes are single-key `edit{}` calls. No settings value is ever sent anywhere.

---

## 5. Playback design

### 5.1 Components (Media3 1.5.1, catalog-pinned)

- **`PlaybackService : MediaSessionService`** — owns the single `ExoPlayer` instance and its `MediaSession`. Declared in the manifest with `android:foregroundServiceType="mediaPlayback"`, `exported=true` **only as Media3 requires for the session** (intent-filter `androidx.media.session.MediaButtonReceiver` / `android.media.session.MediaController` per Media3 docs), no custom exported actions (§11). On `onGetSession` return the session; on task removal → stop playback + `stopSelf()` if queue is idle/paused (standard Media3 pattern: keep playing when the user swipes away *only while playing*).
- **Player config:** `ExoPlayer.Builder` with `DefaultMediaSourceFactory` backed by the caching data source (§5.5), `DefaultLoadControl` tuned for music (min buffer 15 s, max 60 s, start 1.5 s, rebuffer 3 s), `setHandleAudioBecomingNoisy(true)`, `setWakeMode(C.WAKE_MODE_NETWORK)` (streaming) — downloads resolve to local files so wake mode per-item is acceptable, `AudioAttributes` = `USAGE_MEDIA / CONTENT_TYPE_MUSIC`, `setHandleAudioFocus(true)` is **not** used — focus is managed explicitly (§5.4) for duck/pause semantics control. (Builder: if explicit focus management proves fiddly in Phase 5, fall back to `Robolectric`-free manual device test checklist in §9 and document.)
- **`PlayerController` (UI-facing)** — an interface implemented over a `MediaController` connected asynchronously to the service (`SessionToken` → `MediaController.Builder.buildAsync()`). Exposes: `state: StateFlow<PlaybackUiState>`, `playQueue(songs, startIndex, sourceLabel)`, `play(song)`, `togglePlayPause()`, `next()/previous()`, `seekTo(ms)`, `setShuffle(Boolean)`, `cycleRepeat()`, `enqueueNext(song)`, `appendToQueue(song)`, `removeFromQueue(index)`, `moveQueueItem(from,to)`, `jumpToQueueItem(index)`, `setSleepTimer(minutes?)`, `clearSleepTimer()`. UI/ViewModels depend **only** on this interface (+ a fake in tests/previews).
- **`PlaybackUiState`** — immutable snapshot mapped from the player every state/position tick: `currentSong: Song?, queue: List<Song>, currentIndex, isPlaying, isBuffering, positionMs, durationMs, shuffle, repeatMode, playbackSpeed(=1f v1.0), sleepTimerEndsAt?, error: AppError?`. Position ticks at 500 ms **only while the full player or mini-player is visible** (a `WhileSubscribed` flow in the owning ViewModel) — no perpetual ticker.

### 5.2 Song → MediaItem resolution

```
Song + preferredQuality (Settings.stream_quality)
  → if a COMPLETED download exists for songId  → MediaItem(uri = file://…, mediaId = songId)   [offline wins, always]
  → else pick stream link: exact preferred quality, else nearest lower, else highest available
  → if downloadUrl[] is empty / all links blank   → resolution failure: AppError.Unavailable("Not streamable in this region")
MediaItem: mediaId = songId, uri, MediaMetadata(title, artist, album, artworkUri = image500, extras = songId + saavnUrl)
```
Resolution lives in a single `MediaItemFactory` used by both `PlayerController` (queue loads) and downloads — one place owns quality fallback so player and downloader never disagree. Resolved URLs are **not persisted into the queue across process death** beyond Media3's own state; on restore, if playback fails with HTTP 403/404 (signed CDN URLs age), the queue manager re-resolves the current song by ID via `MusicRepository.getSong(id)` once and resumes at the saved position (§5.3 recovery).

### 5.3 Queue state machine

Queue truth lives **in ExoPlayer's timeline**; `QueueManager` is the policy layer around it (no parallel queue list to drift out of sync).

States (derived, exposed via `PlaybackUiState`): `Idle` (no items) → `Loading/Buffering` → `Playing` ⇄ `Paused` → `Ended` → (`Autoplay-extend` | back to `Idle`-with-last-item). Error → `Failed(error, queue retained)`.

Rules:
- `playQueue(list, start)` = `setMediaItems(items, start, 0) + prepare + play`. Also fires `LibraryRepository.recordPlay(song, source)` for the started item and on every subsequent auto/manual track transition (player listener `onMediaItemTransition`, reason ≠ seek).
- Shuffle/repeat are player-native (`shuffleModeEnabled`, `repeatMode`) — persisted to DataStore? No: session-only in v1.0 (decision, keeps DataStore surface minimal).
- **Autoplay:** on transition into the last queue item, if `autoplay_suggestions` is on, fetch `getSuggestions(currentSongId, limit=10)` once, append items not already in the queue. Guard flag per queue generation so a failed/empty suggestions call is not retried in a loop. Best-effort: failures are silent (playback must never error because suggestions failed).
- **Recovery:** `Player.Listener.onPlayerError` → classify (§8.1): HTTP 403/404/generic IO on the *current* item → one re-resolve attempt (§5.2) → if that fails, skip to next item and surface a transient error; if the queue exhausts, state = `Failed`. No infinite retry loops (max 1 re-resolve per item per session, tracked by mediaId set).
- Sleep timer: implemented in the controller with the `@ApplicationScope` scope + `delay`; on fire → `pause()` (fade is v1.1). "End of track" variant sets a flag consumed on transition.

### 5.4 Audio focus & noisy

Use `AudioManager` via Media3's `AudioFocusRequestCompat` pattern in the service: request focus on play, **duck** (volume 0.2) on `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`, **pause** on transient loss, **pause + abandon** on permanent loss, resume-on-gain only if we auto-paused (track `wasPlayingBeforeFocusLoss`). Headset unplug / Bluetooth disconnect: `ACTION_AUDIO_BECOMING_NOISY` → pause (also via `setHandleAudioBecomingNoisy(true)`).

### 5.5 Cache strategy (streaming)

`SimpleCache` in `cacheDir/stream_cache`, `LeastRecentlyUsedCacheEvictor(stream_cache_max_mb)`, keyed by a `CustomCacheKey` = **songId + quality** (not the raw URL — signed URLs change and would orphan cache entries; `MediaItem.mediaId` + a data-source factory that sets the cache key from the item). Wrapped by `CacheDataSource` (upstream = OkHttp data source) with `FLAG_BLOCK_ON_CACHE | FLAG_IGNORE_CACHE_ON_ERROR`. Downloads (§6) do **not** share this cache — they are real files in internal storage; conflating them is a classic Media3 pitfall.

### 5.6 Notification, lockscreen, surfaces

Media3's `DefaultMediaNotification.Provider` with the app's small icon, artwork from `MediaMetadata`, actions play/pause/next/prev (+ favourite as a custom `CommandButton` in v1.1). Lockscreen/heads-up behaviour is Media3-default via the session. Wear/Bluetooth controllers work through the standard `MediaSession` — no extra code. **Cast (deferred):** `media3-cast-integration` skill applies at the `Player` seam — `PlaybackService` would host both `ExoPlayer` and `CastPlayer` behind Media3's `ForwardingPlayer`/session multiplexing, and `PlayerController` does not change. That is why UI never touches `ExoPlayer` directly.

---

## 6. Download design

### 6.1 Principle — mirror `jiosaavn-dl`, minus the CLI

`jiosaavn-dl` (Python): resolve song/album/playlist → take the **maximum available quality (320 kbps AAC/.m4a)** → save as `Downloads/<Artist(s)|Various Artists> - <Album> [<Year>]/<NN>. <Title>.m4a` (playlist downloads use the playlist name as folder) → skip files that already exist → tag with title/album/artist/composer/year/label/copyright/language/explicit/track-number, lyrics (if any), and 500-px cover art; a track with no media URL is reported "unavailable in your region" and skipped. Omega keeps every one of those semantics that makes sense on Android, adapted in §6.3–6.5.

### 6.2 Engine — WorkManager, one song per work item

- **`DownloadWorker : CoroutineWorker` (`@HiltWorker`)** — input: `songId`. It reads the `download` row + `song` snapshot, resolves the file URL via `MediaItemFactory`-shared quality logic (§5.2/§6.3), streams bytes (OkHttp) to a temp file, reports `setProgress(bytesDone, bytesTotal)`, then atomically renames to the final path, updates the row to `COMPLETED`, and (v1.1) tags the file. Foreground promotion: `setForeground()` with a progress notification (required for long downloads; also satisfies Android 12+ expedited/long-running expectations) — worker is enqueued as **expedited with fallback** to regular on quota.
- **Enqueueing:** `DownloadRepository.enqueue(song | album | playlist)` upserts snapshots + `download` rows (`QUEUED`) and enqueues one unique work per song: `ExistingWorkPolicy.KEEP` on unique name `download_<songId>` (re-tapping Download is idempotent — `jiosaavn-dl`'s "already downloaded, skip"). Album/playlist enqueue = a loop of song enqueues in track order, **not** a chain (a chain makes one failure stall the album; parallel-independent items with a UI rollup is simpler and recoverable). WorkManager parallelism handles concurrency; cap is the platform's.
- **Constraints:** `NetworkType = downloads_wifi_only ? UNMETERED : CONNECTED`; `requiresStorageNotLow = true`. No charging constraint.
- **Observation:** UI never observes WorkManager directly — `DownloadRepository` merges `WorkManager.getWorkInfoByIdFlow` progress into the `download` rows for `RUNNING` items, and screens observe `observeDownloads(): Flow<List<DownloadItem>>` from Room. Process death: WorkManager state + Room state reconcile on app start (a `RUNNING` row whose work is `ENQUEUED`/missing → back to `QUEUED`/re-enqueue by a startup reconciler in `SaavnApplication`'s `@ApplicationScope`).

### 6.3 States

```
QUEUED ──▶ RUNNING ──▶ COMPLETED
              │  ▲
              ▼  │ retry (auto ≤3, backoff) / user retry (from FAILED)
            FAILED ──user──▶ QUEUED (fresh enqueue)
QUEUED|RUNNING ──user cancel──▶ CANCELLED ──▶ row deleted + temp/final file deleted (user intent = "remove download")
COMPLETED ──user delete──▶ row deleted + file deleted (song snapshot row stays if favourited/in a playlist — cascade is download→file only, §4.4 FK is on song, deleting a download must NOT delete the song row; note the FK direction in the schema: download.songId → song.id, so deleting `download` is safe)
```
There is no true `PAUSED` in v1.0 (WorkManager has no pause; emulating it with cancel-and-resume-byte-offset adds a Range-request correctness burden). Cancel = stop and discard partial (temp file deleted). This is stated in the UI copy ("Cancel download") so it is not a surprise.

### 6.4 Quality selection, stale URLs, storage

- **Quality:** `download_quality` preference is a *ceiling*: pick the highest `downloadUrl[]` entry ≤ preference; if none ≤, pick the lowest available. Record the quality actually fetched in `download.quality` and show it as a badge. ("Highest available" default `K320` reproduces `jiosaavn-dl`.)
- **Stale URLs:** signed CDN URLs can expire between enqueue and execution/retry. The worker **always re-resolves** via `MusicRepository.getSong(songId)` if the stored `sourceUrl` is older than 30 min or the first GET returns 403/404 — then retries the GET once with the fresh URL within the same work run before failing the item.
- **Storage:** app-specific internal storage only: `filesDir/music/<sanitised-artist> - <sanitised-album> [<year>]/<NN - position-in-source>. <sanitised-title>.m4a` for album/playlist-origin downloads, `filesDir/music/Singles/<title>.m4a` for one-off song downloads. Sanitisation mirrors `jiosaavn-dl`'s `sanitize_filename` (strip path separators/illegal chars, collapse whitespace, cap component length 100 chars). No `WRITE_EXTERNAL_STORAGE`/MediaStore in v1.0 (keeps the permission surface at §2/§11; exporting to shared storage is a v1.1 feature). Settings shows total bytes used by `filesDir/music` + stream cache, with "Delete all downloads".
- **Offline playback resolution:** §5.2 checks `download.state == COMPLETED` first, so Library/Downloads/favourites all play offline transparently. If the file is missing but the row says COMPLETED (user cleared app files, corruption) → resolver falls back to streaming and a background repair flips the row to `FAILED(error=FILE_MISSING)` so the UI is honest.

### 6.5 Tagging (scoped)

v1.0: the downloaded `.m4a` is stored as served; the app's metadata lives in Room. Embedded tagging parity with `jiosaavn-dl` (title/album/artist/composer/year/label/copyright/language/track #, lyrics, cover via an MP4 tag writer) is **v1.1**, isolated behind a `TrackTagger` interface with a no-op v1.0 implementation, so it lands without touching the worker pipeline. Rationale: a reliable MP4 tag writer is a dependency decision (no Mutagen equivalent ships in the catalog) that should not block v1.0 downloads.

---

## 7. Navigation & state management

### 7.1 Navigation graph

Three top-level tabs with **independent back stacks** (per `navigation-3` skill's multiple-backstacks pattern; bottom bar ↔ nav rail per `adaptive`):

```
Tabs:  HOME        SEARCH        LIBRARY
       Home        Search        LibraryHome ─▶ Favourites
                                 ├─▶ LocalPlaylistDetail(id)
                                 ├─▶ Downloads
                                 └─▶ History
Shared detail destinations (reachable from ANY tab, pushed on that tab's stack):
       AlbumDetail(id) · ArtistDetail(id) · PlaylistDetail(id)   [Saavn playlist]
       LocalPlaylistDetail(id)                                 [Library stack only]
Overlay destinations (not tabs):
       FullPlayer (bottom-sheet / full-screen scene) · Queue (sheet over player) ·
       Lyrics (sheet/segment within player) · Settings (from top-app-bar action, own stack entry)
       AddToPlaylist (dialog scene, returns a result — playlist id — per navigation-3 result pattern)
Deep links:  https://www.jiosaavn.com/{song|album|artist|featured|song…}/… → resolve via API `link=` → matching detail
             omega://song/{id} etc. internal scheme for share/notification taps
Player:      persistent MiniPlayer sits ABOVE the tab bar in AppScaffold (outside the NavHost) whenever
             PlaybackUiState.currentSong != null; tapping expands to FullPlayer. Notification tap → MainActivity → FullPlayer.
```

Destination arguments are **IDs only** (`albumId: String`, `artistId: String`, `playlistId: String`, `localPlaylistId: Long`) — screens load their own data via their ViewModel (process-death safe, deep-link safe, no parcelled domain objects in bundles). Destinations are typed: Navigation 3 `@Serializable NavKey`s if D-2 lands on Nav3, or typed route objects with Navigation Compose 2.8.5 — either way, no stringly-typed routes.

### 7.2 State management pattern

One pattern, everywhere, no exceptions:

```kotlin
data class XUiState(
    val isLoading: Boolean = false,          // first load only
    val isRefreshing: Boolean = false,       // pull/re-search with content already shown
    val content: … = …,                      // screen payload (empty defaults, never null-collections)
    val error: AppError? = null,             // blocking error (full-screen, with Retry) — mutually exclusive with content
    val transientMessage: String? = null,    // one-shot snackbar text, consumed via onMessageShown()
)
@HiltViewModel class XViewModel(...) : ViewModel() {
    val uiState: StateFlow<XUiState> =
        combine(repo flows…, internal MutableStateFlow) { … }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), XUiState())
    fun onAction(action: XAction) { … }      // single entry for UI events, or explicit funs — pick one per VM, be consistent within a feature
}
```

- Compose collects with `collectAsStateWithLifecycle()`. Screens are stateless composables taking `state + callbacks` (a hoisted variant used by previews/tests, a `hiltViewModel()` wrapper for the real route) — per `testing-setup` skill's UI-test design.
- **One-shot effects** (navigation, snackbar) are *not* a `Channel` by default: navigation is state the NavHost owns, snackbars go through `transientMessage` + consume. Reserve `SharedFlow<Effect>` for cases that genuinely can't be modelled as state, and document why in that VM.
- Paging (search types, artist songs/albums): **manual paging in the ViewModel** (`page`, `total`, append-on-scroll via `LazyColumn` end-detection), not Paging 3 in v1.0 — the API's paging envelopes are inconsistent (§4.1: `start/total` vs `lastPage`) and result counts are small; a hand-rolled `PagedListState` in `core` unifies them. Revisit if a surface needs > a few hundred items.
- Player state is *not* duplicated into screen VMs: screens that need it collect `PlayerController.state` through a small `PlayerViewModel` (app-scoped by being provided at the scaffold level).

---

## 8. Error handling & threading

### 8.1 Result type & error taxonomy

```kotlin
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}
sealed interface AppError {
    data object Offline : AppError                                   // no connectivity / UnknownHost
    data class Http(val code: Int, val message: String?) : AppError  // non-2xx or success=false envelope
    data class Unavailable(val reason: String) : AppError            // empty downloadUrl[], region-blocked, not-found entity
    data class Parse(val cause: String) : AppError                   // DTO/serialization failure (upstream changed shape)
    data class Storage(val cause: String) : AppError                 // Room/file/WorkManager failures
    data class Unknown(val cause: Throwable?) : AppError
}
```

- Repository boundary converts **all** exceptions into `Failure` — no exception crosses into domain/UI. (`CancellationException` is always rethrown, never wrapped.)
- Retrofit layer first checks the envelope: HTTP 200 + `success=false` → `Http(200, message)` failure; HTTP errors map by code; `IOException` → connectivity check → `Offline` or `Http(-1)`.
- UI maps `AppError` → copy + recovery: `Offline` → banner + cached content; `Http` → error state + Retry; `Unavailable` → inline badge ("Not available to stream") with the row disabled for play/download; `Parse` → error state + "Report" is a no-op (no telemetry) — copy says the service response changed.
- Every screen must render all three states — Loading / Content / Error(+retry) — plus Empty where a list can legitimately be empty. This is a Phase acceptance item (§13), not a nicety.

### 8.2 Threading / dispatchers

| Work | Dispatcher |
|---|---|
| Retrofit suspend calls, Room (suspend/Flow DAOs), file I/O, WorkManager | `@IoDispatcher` (`Dispatchers.IO`) |
| DTO→domain mapping of large lists, playlist reorder math, search-result dedupe | `@DefaultDispatcher` if measured hot; default IO is fine in v1.0 |
| UI state combination | `viewModelScope` (Main) — combining Flows only, no blocking work |
| Player position ticker, sleep timer, startup reconciler | `@ApplicationScope` scope (SupervisorJob + IO); ticker switches to Main only to publish state |

Inject dispatchers (never hard-code `Dispatchers.IO` in repositories) so tests can substitute `StandardTestDispatcher`. `runBlocking` is banned outside tests; StrictMode in debug builds during Phase 8.

---

## 9. Testing strategy

Structured by the `testing-setup` skill. **Pyramid: many unit, some integration, few UI/e2e. Fakes over mocks.**

### 9.1 Unit tests (JVM, `src/test`) — the base, target ≥ 80% of test count

- **Mappers:** golden-fixture tests — recorded real API JSON (one fixture per endpoint, committed under `src/test/resources/fixtures/`, captured from the studied API models) → DTO parse → domain assertions (qualities parsed, null tolerance, empty `downloadUrl[]`). These double as an upstream-change tripwire.
- **Repositories:** `MusicRepositoryImpl` against a `FakeSaavnApi` (hand-written fake returning fixture DTOs / throwing) — envelope failure mapping, paging math. `LibraryRepositoryImpl` against in-memory Room (`Room.inMemoryDatabaseBuilder` under Robolectric, or Room's JVM test harness) — favourite toggle idempotence, playlist ordering transaction, history cap (500) and dedupe.
- **ViewModels:** `kotlinx-coroutines-test` + Turbine: state transitions for loading/content/error, search debounce (300 ms), paging append, player-VM mapping from a `FakePlayerController`.
- **Playback policy:** `QueueManager`/autoplay logic and `MediaItemFactory` quality-fallback table tests (pure JVM — this is why resolution logic is kept out of the service).
- **Downloads:** worker logic tested via `TestListenableWorkerBuilder` / WorkManager test harness in instrumented tests; worker *decision* logic (quality pick, stale-URL refresh, filename sanitisation) extracted to pure functions and unit-tested on JVM. Filename sanitiser gets `jiosaavn-dl`'s example names as test cases.
- **Architecture tests (optional but recommended):** Konsist rules — `data` doesn't import `androidx.compose`, `domain` doesn't import `android.*`, UI doesn't import `data.remote`.

### 9.2 Integration / UI tests (`src/androidTest`, emulator in CI optional)

- Room migration tests (`MigrationTestHelper`) from the first schema bump onward; v1 ships the schema baseline.
- Compose UI tests (per `testing-setup`): Home renders sections from a fake repository (Hilt test module swapping repository bindings), Search flow type→results→tap, Favourites toggle round-trip, Full-player controls against `FakePlayerController`. Semantics-driven, no pixel assertions in v1.0.
- MediaSession smoke test (instrumented): start service, load a **bundled silent/local test asset** (never the live network in tests), assert play/pause transitions.
- **No test ever hits `saavn.dev` or JioSaavn** — fixtures + fakes only; a separate manual QA checklist (§9.3) covers the live API.

### 9.3 Manual / release QA checklist (checked before each release, results noted in the PR)

Live-API pass (§1.6), airplane-mode pass, notification/lockscreen on Android 14 + 15, predictive-back gestures, foldable/tablet layout spot-check, upgrade-install over the previous release (Room/DataStore survival).

---

## 10. CI/CD, release, signing, versioning

Target repo: **`yadavmanishraj/omega`** (public, default branch `main`, verified empty at design time — the first push is the app tree described here).

### 10.1 GitHub Actions (`.github/workflows/`)

| Workflow | Trigger | Steps |
|---|---|---|
| `ci.yml` | PR + push to `main` | JDK 17 (Temurin) → Gradle cache → `./gradlew testDebugUnitTest` → `./gradlew lintDebug` → `./gradlew assembleDebug` → upload debug APK + lint/test reports as artifacts (14-day retention). PRs red until green |
| `release.yml` | Tag `v*` | `./gradlew testDebugUnitTest lint assembleRelease bundleRelease` → sign (§10.2) → upload AAB + APK to the GitHub Release for that tag |
| (optional, v1.1) `instrumented.yml` | Manual / nightly | `androidTest` on an emulator runner (API 35) — kept out of the PR gate for speed/stability |

Branch protection on `main`: require `ci.yml` green; direct pushes by the builder agent follow the same rule (push to a short-lived branch + PR, or document a solo-dev exception in the repo README — pick one and be consistent).

### 10.2 Signing

- **Debug:** default debug key (CI/local), never committed beyond the standard generated one.
- **Release:** keystore lives **outside the repo** — locally in the owner's password manager / `~/.keystores/omega.jks`; in CI as GitHub Secrets (`OMEGA_KEYSTORE_BASE64`, `OMEGA_KEY_ALIAS`, `OMEGA_KEYSTORE_PASSWORD`, `OMEGA_KEY_PASSWORD`) decoded to a temp file in `release.yml` only. `signingConfigs` read from env/gradle properties with safe fallbacks so `assembleRelease` *unsigned* still works for contributors. **No keystore, password, or `keystore.properties` with real values is ever committed** — `.gitignore` covers `*.jks`, `*.keystore`, `keystore.properties`, `local.properties`.

### 10.3 Release build hygiene

`isMinifyEnabled + isShrinkResources` on release (already in the scaffold), R8 rules audited with the `r8-analyzer` skill before the first signed release; `android-profiler` pass (§2) for startup/jank; version catalog is the single place versions change.

### 10.4 Versioning

Semantic: `versionName = MAJOR.MINOR.PATCH` (start `1.0.0`), `versionCode` = monotonically increasing integer, computed as `MAJOR*10000 + MINOR*100 + PATCH` while < 2.1B headroom allows, bumped in the same PR as the release notes. Tags `v1.0.0` on `main` drive `release.yml`. Changelog: a `CHANGELOG.md` (Keep-a-Changelog format) updated per release — the repo the API came from uses one; mirror the habit.

---

## 11. Security & privacy

- **No accounts, no identifiers:** no sign-in (hence the `identity/*` skills are non-applicable, §2.3), no advertising ID, no analytics, no crash SDK, no third-party trackers. Privacy story is one sentence: *"Omega stores your library on your device and talks only to the Saavn API instance you configure."*
- **Network:** the default base URL is HTTPS (`https://saavn.dev`). Because users may self-host over HTTP on a LAN, `networkSecurityConfig` permits cleartext **only** as an explicit opt-in tied to a non-HTTPS custom base URL: ship `usesCleartextTraffic=false` with a config that the Settings flow warns about when an `http://` base URL is saved ("sent unencrypted") — never silently allow all cleartext. CDN/media hosts are HTTPS in practice. No certificate pinning (self-hosting + unofficial API make pinning a foot-gun).
- **Components (per `android-intent-security` + `android-permissions-security` skills):** exported = `MainActivity` only (launcher + verified-shape deep links). Deep-link/`link=` input is allow-list validated (https, host ends with `jiosaavn.com`) before it reaches the API, and any failure falls back to Home with a message — never crash, never open arbitrary URLs. `PlaybackService` follows Media3's required exposure and nothing more; no exported providers/receivers. PendingIntents (notification) use `FLAG_IMMUTABLE`.
- **Permissions (final manifest list):** `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS` (runtime-requested in context on Android 13+, denial degrades gracefully), `WAKE_LOCK`. That's all.
- **On-device data:** library DB and downloads are in internal storage (app-sandboxed). **Backup rules:** `android:allowBackup=true` with `data_extraction_rules`/`fullBackupContent` **excluding** `download/` files and stream cache (large, re-derivable) but **including** the Room DB + DataStore (favourites/playlists/history are the user's data and should survive a device move — note downloads' *rows* restore while files don't, which §6.4's missing-file repair already handles).
- **Secrets:** there are none in the app (no keys/tokens exist in this system). CI/release secrets are §10.2 only.
- **Content/IP note:** the app is a client for an unofficial API over a commercial catalogue. That is a product/legal risk for distribution (see R-1), not something code can fix; the design keeps the API endpoint configurable so the app is equally a client for a user's own instance.

---

## 12. Risks, decisions & trade-offs (decision log)

| ID | Decision / risk | Resolution |
|---|---|---|
| D-1 | Single module vs multi-module | Single `:app` + strict packages for v1.0; extraction map in §3.2; revisit on stated triggers |
| D-2 | Navigation 3 (per `navigation-3` skill) vs scaffolded Navigation Compose 2.8.5 | Design targets Nav3 semantics (§7.1). Builder picks one in Phase 1 and records it: Nav3 if its Hilt/Compose integration in the pinned catalog is smooth, else Nav-Compose with identical typed-destination structure. Do not mix both |
| D-3 | Repository interfaces in `domain`, impls in `data` | Yes, despite single module — this is the seam for fakes in tests and for the future module split |
| D-4 | Manual paging vs Paging 3 | Manual (§7.2) — inconsistent upstream envelopes, small result sets |
| D-5 | Downloads via WorkManager, not Media3's `DownloadManager` | WorkManager: Media3 downloads would tie offline files to its cache/index model; we need user-visible file artifacts, album folder semantics à la `jiosaavn-dl`, and quality-at-enqueue semantics. Cost: we write the byte pipeline ourselves (small, §6.2) |
| D-6 | **No home/trending endpoint exists** in the API | Home = composed seed searches + artist/album spotlights + the user's own history/favourites rails. Product risk accepted: Home quality depends on seed curation (owned by the UI/UX doc). If a future API adds modules/home endpoints, `MusicRepository.getHome()` slots in without UI changes |
| D-7 | A song can appear once per local playlist (PK `(playlistId, songId)`) | Simpler ordering/dedupe; duplicate-in-playlist is an acceptable loss for v1.0 (revisit with a surrogate row id if users ask) |
| D-8 | No persisted Home/content cache in Room for v1.0 | §4.5 — offline Home is library-driven. Keeps schema to user data only |
| R-1 | **Unofficial API / catalogue IP** | May break or be taken down at any time; store distribution is risky. Mitigations in-design: configurable base URL, fixture-based tests, defensive parsing (§4.2), honest README disclaimer in the omega repo |
| R-2 | Signed media URLs expire | Player re-resolve once (§5.3); worker refresh rule (§6.4); cache keyed by song+quality (§5.5) |
| R-3 | `songs/{id}` lyrics response shape ambiguity in source | Phase 1 acceptance includes pinning it against live docs + a fixture before the player phase depends on it (§13) |

---

## 13. Build sequence for the builder agent (SDLC implementation plan)

Ordered phases. **A phase is done only when its acceptance criteria pass**; commit per phase to `yadavmanishraj/omega` (branch + PR per §10.1) with the phase number in the PR title. UI copy, colours, spacing, and motion come from the UI/UX document; API field details from the REST API research document + §4.1 here (in case of conflict on *fields*, the research doc's recorded fixtures win; on *structure*, this doc wins).

### Phase 0 — Repo & skeleton hygiene
Settings/catalog already scaffolded — verify: `settings.gradle.kts`, version catalog (§1.3 NFR-10 versions), `.gitignore` (incl. §10.2 secrets), `CHANGELOG.md`, README with disclaimer + base-URL note, `ci.yml` (§10.1).
**Accept:** `./gradlew assembleDebug` builds the scaffold; CI runs green on the omega repo; no login-related dependency or string exists.

### Phase 1 — Core + data foundation
`core/result`, dispatcher qualifiers, `Json`/OkHttp/Retrofit + `BaseUrlInterceptor`, all DTOs + mappers (§4.1–4.2), `SaavnApi` with every endpoint in §4.1, `MusicRepositoryImpl` (no UI). Pin the lyrics shape (R-3) with a recorded fixture. Record the D-2 navigation decision.
**Accept:** mapper + repository unit tests pass against fixtures for **all** endpoints (incl. `success=false`, empty `downloadUrl[]`, both paging envelopes); a debug-only smoke (test or scratch main) fetches a real song/album/artist/playlist from `https://saavn.dev`.

### Phase 2 — Local library (Room + DataStore)
Schema §4.4 exactly (schema JSON exported + committed), DAOs, entity↔domain mappers, `LibraryRepositoryImpl`, `SettingsRepositoryImpl` with all §4.6 keys, Hilt modules §3.4.
**Accept:** Room tests pass (favourite toggle, playlist order transaction, history cap/prune, search dedupe/cap, download-row CRUD); settings Flow emits defaults on a fresh install.

### Phase 3 — App shell + Home + navigation
Single activity, edge-to-edge (`edge-to-edge` skill checklist), adaptive scaffold (`adaptive`), typed navigation graph §7.1 with per-tab stacks, theme (dark default, dynamic colour), Home sections (D-6 strategy) with shimmer/empty/error states, mini-player placeholder slot.
**Accept:** tab navigation preserves per-tab back stacks; predictive back behaves (§2 `navigation-event`); Home renders live data and, in airplane mode, the offline library rails; layout spot-checked at phone + medium widths.

### Phase 4 — Search + detail screens
Global search (debounced 300 ms, recent searches wired to Room), type tabs with manual paging, Album/Artist/Playlist detail (incl. artist paging/sort), paste-a-link resolution (FR-16), song-row actions (play, favourite, add-to-playlist, download-enqueue stub → Phase 6).
**Accept:** all §8.1 error states reachable and tested by fakes; search UI tests pass; tapping any result navigates by ID only.

### Phase 5 — Playback
`PlaybackService`, `PlayerController`, `MediaItemFactory`, queue machine + autoplay suggestions, audio focus/noisy, stream cache, notification/lockscreen, Full Player + Mini Player + Queue sheet + Lyrics, sleep timer, history recording on transitions.
**Accept:** §1.6 playback criteria — background/lockscreen/notification control, queue ops, shuffle/repeat, stale-URL recovery demonstrated by a forced 403 fake, airplane-mode playback of a (Phase 6 or manually placed) local file; instrumented MediaSession smoke test passes.

### Phase 6 — Downloads
`DownloadWorker` + enqueuer + file store (§6), Downloads screen (progress from Room, cancel/retry/delete), Settings storage section, offline-resolution integration with §5.2, startup reconciler.
**Accept:** download a song + an album from the live API; files land at §6.4 paths with correct names/qualities; cancel discards partials; retry after a forced failure succeeds (stale-URL path exercised); downloaded album plays fully in airplane mode; storage accounting matches disk.

### Phase 7 — Library screens + Settings completion
Favourites, Local Playlists (create/rename/delete/add/remove/reorder), History, Recent Searches management, full Settings (base URL with cleartext warning, qualities, theme, cache size, clear-data actions).
**Accept:** every FR-10…FR-15/FR-18 user story passes manually; changing the base URL to a second instance (or a mock server) works without reinstall.

### Phase 8 — Hardening, tests & release readiness
Full test pass (§9 incl. UI tests + architecture rules if adopted), lint baseline/clean, `r8-analyzer` audit, `android-profiler` startup/jank pass, StrictMode clean in debug, backup-rules verification, accessibility pass (NFR-6), `play-policy-insights` checklist **if** a store release is contemplated.
**Accept:** §1.6 release-gate checklist fully green; `assembleRelease` (unsigned) succeeds; signed `release.yml` run on a `v1.0.0` tag produces the GitHub Release with AAB+APK.

---

*End of design. Questions this document leaves open are marked as decisions (D-*) with an owner phase — resolve them there, and amend this file in the same PR.*
