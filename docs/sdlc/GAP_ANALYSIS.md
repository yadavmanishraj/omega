# Gap Analysis — SDLC designs vs. v1 code (commit `a70b050`)

> **Post-rebuild status update — 2026-10-07 (modular rebuild, direct-upstream
> data layer, redesign).** The app was restructured per
> [`MODULARIZATION_PLAN.md`](MODULARIZATION_PLAN.md) (16 modules +
> build-logic convention plugins), the hosted-wrapper data layer was
> replaced by direct JioSaavn upstream calls per
> [`UPSTREAM_SPEC.md`](UPSTREAM_SPEC.md) (validated live in
> [`UPSTREAM_VALIDATION.md`](UPSTREAM_VALIDATION.md)), and Home/Search/
> Library/Settings + shell were redesigned per
> [`REDESIGN_SPEC.md`](REDESIGN_SPEC.md). Rows below that this closes
> are marked inline with **[CLOSED 2026-10-07]**; everything else stands
> as written. New/remaining honest gaps after the rebuild:
>
> - **Fonts deviation: [CLOSED — bundled fonts]** the redesign's
>   Righteous/Poppins families are now wired by BUNDLING the static
>   TTFs in `:core:designsystem` (`res/font/`: Righteous 400; Poppins
>   400/500/600/700; SIL OFL 1.1, license texts in the same directory)
>   instead of the Google Fonts downloadable provider — so no GMS
>   certs array is needed at all and text renders in the brand fonts
>   on first frame. `Theme.kt`'s drop-in point now resolves
>   `DisplayFontFamily`/`BodyFontFamily` to the bundled families;
>   the spec §2.2 type scale is unchanged.
> - **Suggestions:** implemented against upstream radio calls, but
>   `webradio.getSong` is currently broken upstream (always an error
>   body), so suggestions return empty by design and nothing depends
>   on them.
> - **SearchBar:** the redesign assumed `SearchBarInputField`; it does
>   not exist in material3 1.3.1, so Search uses the deprecated
>   `SearchBar(query, active, ...)` overload (suppressed, documented).
> - **Downloads:** **[CLOSED 2026-10-07, wave 2]** the worker writes the
>   full lifecycle to Room (DOWNLOADING + throttled progress, FAILED,
>   cancel cleanup), completed rows are playable, delete removes the
>   file + metadata sidecar and cancels in-flight work. FAILED rows
>   carry a human-readable `errorMessage` — the one deliberate schema
>   change since v1: Room 1 -> 2 via `MIGRATION_1_2` (additive
>   `ALTER TABLE ... ADD COLUMN`, all v1 data preserved; the upgrade
>   path is covered by the on-device QA checklist).
> - **Local playlists:** detail view + playback now exist; rename and
>   reorder are still missing. Still open. **[Add-to-playlist UI entry
>   point added on `feat/add-to-playlist`: song overflow menus in Home,
>   Search, Detail and Library open a shared `:core:ui` playlist picker
>   (with inline playlist creation); Home's offline Downloads path now
>   deep-links to the Library Downloads tab.]**
> - **Room:** `exportSchema` is now true (schema JSON lands on the
>   first real build); the DB name stays `saavn-music.db` and the
>   schema is byte-identical to v1 BY DECISION — the modularization
>   plan required zero data migration, which supersedes the design's
>   `omega.db` rename.
> - **Build verification:** v1 was verified on the user's laptop
>   (commit 1063a0f). The REBUILD is **not yet build-verified** — gates
>   run in the sandbox were ktlint + a kotlinc parse scan + manual
>   module-dependency review; the laptop build is the real gate.

> Written 2026-10-07 as part of the SDLC integration commit. This compares the three design
> deliverables in this folder — [`API_RESEARCH.md`](API_RESEARCH.md),
> [`UIUX_DESIGN.md`](UIUX_DESIGN.md), [`APP_DESIGN.md`](APP_DESIGN.md) — against the v1 code
> **as actually committed**. It is deliberately honest: "Partial" and "Missing" mean the code
> does not do what the design says, even where the README/STUDY.md describe the intent.
> Method: every row was checked by reading the v1 source (`SaavnApi.kt`, `Dtos.kt`,
> `MusicRepository.kt`, `Entities.kt`, `SettingsRepository.kt`, `AppModule.kt`,
> `PlayerController.kt`, `PlaybackService.kt`, `DownloadWorker.kt`, `MainActivity.kt`,
> the `ui/` tree, and the single test file), not by trusting commit messages.

**Status key:** ✅ Matches design · 🟡 Partial · ❌ Missing · ⚠️ Contradicts / diverges from design

## 1. API layer (vs. `API_RESEARCH.md` + `APP_DESIGN.md` §4)

| Area | Design | v1 code | Status |
|---|---|---|---|
| Base URL / envelope | `https://saavn.dev/api/`, uniform `{success, data}` envelope | **[CLOSED 2026-10-07 — superseded]** No wrapper at all: the app calls JioSaavn's upstream `api.php` directly (see `UPSTREAM_SPEC.md`); `saavn.dev` no longer exists in DNS | ✅ |
| Endpoint coverage | 14 public REST endpoints (API research §2) | 12 Retrofit methods: global search, 4 typed searches, `songs/{id}`, suggestions, `albums?id=`, `playlists?id=`, `artists/{id}`, `artists/{id}/songs`, `artists/{id}/albums` | 🟡 |
| Batch / link resolution (FR-16) | `GET /songs?ids=` / `?link=`, `albums?link=`, `playlists?link=`, `artists?link=` for paste-a-link & batch resolve | No `ids`/`link` variants at all; no paste-a-link UI or deep links | ❌ |
| Artist paging endpoints | `artists/{id}/songs|albums` with `sortBy`/`sortOrder`, `{total, songs/albums}` envelope | Declared in `SaavnApi` but **never called** — repository only uses `artists/{id}` (`topSongs`/`topAlbums`); response type is also modelled as the search envelope (`SearchResultDto`), and no sort params are passed | ⚠️ |
| **[CLOSED 2026-10-07]** Lyrics now come from upstream `lyrics.getLyrics` with `lyrics_id` = the song id (live-validated; `<br>` → newline handled in the mapper). The original row: **Lyrics response shape pinning** | Architecture risk R-3 / Phase 1 acceptance: pin the exact `songs/{id}?lyrics=true` shape against live `/docs` + a recorded fixture **before** the player depends on it | **Not pinned.** API research found *there is no lyrics endpoint/route in the public wrapper at all* (only `hasLyrics` + `lyricsId` on the Song DTO). v1 nevertheless sends `?lyrics=true`, embeds an optional `LyricsDto` on `SongDto`, and the player shows "Loading lyrics…" / "No lyrics available". Whether any lyrics ever return from `saavn.dev` is unverified — expect the no-lyrics path in practice | ❌ |
| Lyrics `<br>` handling | Architecture FR-9: convert HTML `<br>` to newlines (as `jiosaavn-dl` does) | **[CLOSED 2026-10-07]** Converted in the network mapper (`cleanedLyrics`) and unit-tested | ✅ |
| Error envelope | Failures are `{success:false, message}` + HTTP status; map to a typed result | `ApiResponse` has **no `message` field**; repository ignores `success=false` and throws/catches raw exceptions, surfacing `e.message` to the UI. No `AppResult<T>` type, no error taxonomy (design §8.1), no dispatcher qualifiers (design §8.2) | ⚠️ |
| DTO tolerance | Lenient parsing + numbers-as-strings defence (`FlexInt/FlexLong`), one bad item must not fail a list (NFR-4) | `ignoreUnknownKeys`/`coerceInputValues` are on, fields have defaults — good start; but no string-number tolerance (e.g. `duration`, `playCount` are strict `Long?`) and one malformed item fails the whole response | 🟡 |
| Model tiers | 3 tiers (DTO → pure domain with typed `ImageSet`/`StreamLink` quality enums → entity snapshots) in `model/`/`mapper/` packages | Effectively 2 tiers: DTOs are close to the canonical Song model, but domain `Song` is a flattened display model (single `imageUrl`, `List<Pair<String,String>>` for streams, artists pre-joined to a String). No typed quality enums, no mapper package | 🟡 |
| Image ladder | Pick 50/150/500 by target size via a `bestFor(px)` helper; never assume array order | `bestUrl()` = **last** non-blank entry, used for every surface (rows load 500x500 too) | 🟡 |
| Home / trending source | No home/trending endpoint exists (research §2.6) → compose Home from curated searches (design D-6/FR-1) | **[CLOSED 2026-10-07 — superseded]** Upstream DOES have a home payload: `content.getBrowseModules`, now used with shape-based classification | ✅ |
| Suggestions / radio | `songs/{id}/suggestions` = autoplay/queue-extension engine; cache & prefetch (expensive: 2 upstream calls) | Endpoint + repository + `PlayerViewModel.suggestions()` exist, but **nothing ever calls it** — no autoplay-on-queue-end, no "Related" surface, no caching | 🟡 |

## 2. UI/UX (vs. `UIUX_DESIGN.md`)

| Area | Design | v1 code | Status |
|---|---|---|---|
| No login / no sign-up | No accounts anywhere; app opens straight into music | Fully honoured — no auth screens, no onboarding gate, first composition is Home | ✅ |
| Core theme tokens | Dark bg `#0B0F0E`, surface `#121715`, primary `#3BE477`; light bg `#F7FAF8`, primary `#006B32` (UIUX §3.1) | **[CLOSED 2026-10-07 — superseded by REDESIGN_SPEC]** Full token system in `:core:designsystem` (midnight/indigo schemes, complete role sets, type scale, `OmegaSpacing`/`OmegaRadius`); fonts closed too — Righteous/Poppins bundled in `res/font` (see fonts note above) | ✅ |
| Bottom nav + mini-player | Home / Search / Library, persistent mini-player, Settings reachable | Present (plus Settings as a 4th nav item, where design puts Settings behind a top-app-bar action) | 🟡 |
| **Shared-element transition (signature)** | Mini-player → Full Player **shared-element artwork** animation, 350 ms; fallback slide-up + scale is allowed *only if flagged*; a plain state swap/fade is "not acceptable" (§7) | **Missing.** Full player is a boolean state switch (`showPlayer`) reusing the same artwork composable — there is no `SharedTransitionLayout`, no slide/scale animation, and this was not flagged in the v1 PR notes. Flagged here instead | ❌ |
| Artwork-derived palette | Palette extraction from artwork, player/header gradients, contrast-scrim invariant, teal fallback, 300 ms crossfade (§3.1.3) | **Missing.** `GradientHeader` tints from the theme `primary` colour, not the artwork; "Dynamic / artwork colors" setting actually toggles Material You dynamic colour, not artwork palette. No Palette dependency | ❌ |
| Full player | 64 dp white play button rule, player tab pager (Up Next / Lyrics / Related), swipe artwork for next/prev, blurred-artwork background | Play/pause is a themed `FilledIconButton` (not the white-on-black rule); queue is a bottom sheet (allowed presentation), lyrics is an inline text block under the controls, no Related tab, no artwork swipe gestures, no blur background. Seek slider, shuffle/repeat, speed chips, sleep timer *are* present | 🟡 |
| Lyrics view | Dedicated `LyricsView` (§5.11): unsynced paragraph treatment, copy button, designed no-lyrics state, synced/karaoke mode if timestamps exist | Inline `Text` toggle in the player only; no copy, no scroll treatment, no synced mode — and see the API row above: the data source itself is unproven | 🟡 |
| Search UX | Debounced (300 ms) global search with **top-result** section, chip-filtered typed results, suggestions ghost rows, paging footer | **[CLOSED 2026-10-07]** 300 ms debounce, global top-results section (resolved by id before playback), typed tabs, FlowRow recent chips with per-chip removal, distinct no-results state. Paging footer + voice search still missing | 🟡 |
| Loading / empty / error | Shimmer skeletons, designed empty states, inline retry, offline banner (§8) | `ShimmerList`, `ErrorState` (with retry), `EmptyState` are implemented and used — but the shimmer is static boxes (no shimmer animation), and there is **no offline banner / connectivity state** anywhere | 🟡 |
| Queue | Up Next list with jump, remove, drag-reorder (reorder v1.1 per FR-18) | Queue sheet lists songs and supports jump-to-tap only; no remove/reorder | 🟡 |
| Detail headers | Collapsing artwork-palette `DetailHeader` (176 dp art, eyebrow, meta, Play pill + Shuffle) | Static header with 180 dp artwork, name/artist/description, Play all + Shuffle buttons; no collapse behaviour, no palette gradient | 🟡 |
| Home "Recently played" | Cards navigate / play | **[CLOSED 2026-10-07]** "Jump back in" cards play the history queue from the tapped index | ✅ |
| Accessibility | ≥48 dp targets, content descriptions on **every** icon button, seek semantics, font-scale rules (§9, NFR-6) | Most icon buttons pass `null` content descriptions; no seek TalkBack actions; touch-target sizes unverified | ❌ |
| Responsive / adaptive | Tablet/foldable list-detail layouts (design §10, architecture §2 `adaptive` skill) | Phone-portrait single column only | ❌ |

## 3. Architecture, data & playback (vs. `APP_DESIGN.md`)

| Area | Design | v1 code | Status |
|---|---|---|---|
| Overall shape | Single `:app` module, strict packages, MVVM + repositories, Hilt, StateFlow `UiState` | **[CLOSED 2026-10-07]** Now in Android-style modularization: 16 modules + build-logic (see `MODULARIZATION_PLAN.md`); per-module `di/` packages replaced the god AppModule | ✅ |
| Room schema | DB `omega.db`, `exportSchema = true` + committed schemas, **shared `song` snapshot table** referenced by favourite/playlist/history/download tables, transactional snapshot writes, history cap 500, recent-search cap 20 | DB is named `saavn-music.db`, `exportSchema = false`, no schema files; each table stores its **own denormalised copy** of song fields (the exact divergence §4.4 warns about); history query caps *display* at 50 but the table grows unbounded; recent searches display-cap 10, no pruning; no transactions beyond single DAO calls | ⚠️ |
| Settings (DataStore) | File `omega_settings`; keys incl. theme mode `SYSTEM|LIGHT|DARK` (default Dark), stream quality default `K160`, dynamic colour default `false`, cache size, Wi-Fi-only downloads, autoplay toggle; base URL applied live via interceptor, no restart | File is named `settings`; only 5 keys (base URL, stream/download quality, dark boolean, dynamic boolean); defaults differ (stream `320kbps`, dynamic `true`); theme is a boolean (no System option); **base URL is read once at Hilt graph creation — changing it needs an app restart** (documented in UI, but contradicts the design) | ⚠️ |
| **Paging decision** | Architecture D-4 (§7.2) decided: **manual paging in the ViewModel** (`PagedListState`, append-on-scroll) — explicitly *not* Paging 3 in v1.0, because upstream envelopes are inconsistent | **Neither was implemented.** Search fetches a single page (limit 20) and stops; `total`/`start` are discarded; playlists fetch one page at `limit=100` — which also trips the API-research gotcha that a paged playlist call overwrites `songCount` with the slice length and truncates the track list at the limit; artist lists never page at all | ❌ |
| **Cache strategy** | §4.5/§5.5: OkHttp HTTP cache 10 MB, in-memory detail LRU (~50, 5 min TTL), Coil loader with sized memory/disk caches, Media3 `SimpleCache` 256 MB for streaming, cache-first/offline Home fallback | **None of it.** No OkHttp `Cache`, no detail LRU, Coil uses library defaults (no custom `ImageLoader` provided), ExoPlayer has **no streaming cache** (every replay re-streams), Home is network-first with no offline fallback (offline Home = error state + retry, where design §4.5 wants local library content) | ❌ |
| Playback core | Media3 1.5.1 `MediaSessionService`, audio attributes + focus + becoming-noisy, notification/lockscreen | Present and correct in outline: Media3 1.5.1, `MediaSessionService`, `USAGE_MEDIA` attributes with focus handling, `setHandleAudioBecomingNoisy(true)`. No `SimpleCache` data source (above), queue state lives in the controller's StateFlow rather than the designed queue state machine (§5.3) | 🟡 |
| Sleep timer | Persisted end-time so it survives process death (§4.6) | In-memory coroutine delay in `PlayerController`; dies with the process, not persisted; UI state resets per player open | 🟡 |
| Downloads engine | WorkManager, one work per song, states QUEUED/RUNNING/COMPLETED/FAILED/CANCELLED mirrored in Room with progress bytes, cancel, Wi-Fi-only constraint from settings | WorkManager + unique work per song and network constraint exist, but: worker writes **no progress**, Room row is only written on success with hardcoded `status=COMPLETED, progress=100` (no FAILED rows, no cancel), constraint is always `CONNECTED` (no Wi-Fi-only setting), worker builds a bare `OkHttpClient()` bypassing DI, and deleting a download deletes only the Room row — **the file stays on disk** | 🟡 |
| Download semantics (`jiosaavn-dl`) | Max-quality ≤ preference, sanitised `Artist - Title.m4a`, album folders `NN. Title.m4a`, skip-if-exists, unavailable = first-class state | File naming/sanitising matches and is unit-tested; but album/playlist "download all" does not exist (song-only enqueue), no skip-if-exists check, empty `downloadUrl[]` surfaces as worker failure/retry rather than an "unavailable" state | 🟡 |
| Tagging | v1.0: metadata in Room; embedded MP4 tagging is v1.1 behind a `TrackTagger` interface with a no-op v1.0 impl (§6.5) | Sidecar JSON file written next to the `.m4a` instead; **no `TrackTagger` interface exists**, so the v1.1 seam the design promised is not in place | 🟡 |
| Local playlists (FR-11) | Create / **rename** / delete, **add/remove/reorder** songs, play a local playlist | Create + delete work; `addToPlaylist` exists in repository/ViewModel but **no UI calls it** (no add-to-playlist action on any song row); no rename, remove, reorder, playlist detail, or play-from-playlist | 🟡 |
| History / recents (FR-12/13) | Record on play, capped & pruned, clearable; recents tap-to-rerun, clear individually/all | Record + clear-history + tap-to-rerun + clear-all recents all work; caps/pruning missing (above); no per-item recent removal; plays are recorded even when playback never actually starts (recorded at queue-build time) | 🟡 |
| Share (FR-17) | Plain-text Sharesheet with the Saavn URL | No share action anywhere | ❌ |
| Offline playback of downloads | Downloads playable fully offline from Library (design story 4) | Download rows in Library are **not playable at all** — the Downloads tab rows have no click/play action (a separate `DownloadsScreen` composable exists but is not wired into navigation); favourites/history rows reconstruct `Song`s with only a stored single `streamUrl`, losing the quality ladder | ❌ |
| Testing | §9: unit tests for mappers, repositories (fake API), Room (in-memory), ViewModels (Turbine/coroutines-test); `androidTest` UI tests; ≥80% of test count at JVM level | **3 unit tests in 1 file** (DTO parsing, quality fallback, file naming). No repository/ViewModel/Room tests, no fixtures beyond the one inline JSON, no `androidTest` source set | 🟡 |
| CI/CD | §10.1: GitHub Actions running lint + unit tests + assemble on push/PR | **Added in this integration commit** (`.github/workflows/android-ci.yml`, "Android CI") — but see verification status below | 🟡 |
| **CI / build verification status** | Release gate (§1.6): tests + lint + debug/release builds green locally *and* in Actions | ⚠️ **Nothing has ever compiled.** The v1 sandbox could not run the Gradle daemon (documented in `STUDY.md` §5), so `testDebugUnitTest`/`assembleDebug` are **UNVERIFIED**, no APK exists, and no CI run has happened yet. The first Actions run on `yadavmanishraj/omega` *is* the first real build — expect it to surface compile errors that then need fixing. Release build (R8 config exists in `build.gradle.kts`) is likewise unverified; no signing config (design §10.2 expects a documented keystore path before any release) | ⚠️ |
| Security / privacy | §11: no analytics/crash SDKs, minimal permissions, cleartext off, no identifiers leaving the device | Matches: permissions are minimal (internet, network state, foreground-service media playback, notifications), `usesCleartextTraffic=false`, no analytics SDKs. One nit: HTTP logging interceptor is compiled into release with `BASIC` level | 🟡 |

## 4. Settings-screen spec items (vs. `UIUX_DESIGN.md` §5.17 + `APP_DESIGN.md` §4.6)

| Item | Design | v1 code | Status |
|---|---|---|---|
| **Incognito listening toggle** | Settings → "Incognito listening" (default OFF, "Don't save plays to history"), History screen banner while on (§5.9/§5.17) | **Missing entirely** — no setting key, no toggle, `recordPlay()` is unconditional, no banner | ❌ |
| Clear history / searches / cache from Settings | Settings "Data & Privacy" group | Clear-history lives in the Library History tab; clear-searches in Search; neither is in Settings; no cache-size display or clear-cache action (and no app-managed cache to clear yet — §3) | 🟡 |
| Theme control | `SYSTEM / LIGHT / DARK` segmented control, default Dark | **[CLOSED 2026-10-07]** Segmented System/Dark/Light control (`themeMode` key, migrated from the old boolean, default Dark) | ✅ |
| Playback / download quality | Separate pickers over the API ladder | Present (48/96/160/320 chips for both) — but stream default is 320, design default is 160 | 🟡 |

## 5. Phased roadmap

**Phase A — Prove the build (before anything else)**
1. Push and watch the first "Android CI" run; fix compile errors until `lintDebug`,
   `testDebugUnitTest`, `assembleDebug` are green. This is the release gate in design §1.6
   and currently the project's single biggest unknown.
2. Pin the lyrics question with evidence (API research §10): either record a live
   `saavn.dev` fixture proving what `?lyrics=true` returns, or formally adopt the
   "no lyrics from the public API" finding and change the player copy/state accordingly.
   Decide: self-host an extended wrapper, or drop lyrics for v1.
3. Fix playlist fetching to page until a short page returns (API research §2.5 gotcha) —
   correctness bug, not an enhancement.

**Phase B — Close the design's v1.0 promises**
4. Manual paging per decision D-4 (search types + artist songs/albums, wiring the two
   already-declared artist endpoints with sort params and the correct `{total, songs/albums}`
   envelope).
5. Cache strategy as designed: OkHttp cache, detail LRU, configured Coil loader,
   Media3 `SimpleCache`; offline Home fallback to local library.
6. Downloads hardening: progress + FAILED state in Room, cancel, Wi-Fi-only setting,
   delete removes the file, download rows play offline, album/playlist download-all,
   empty-`downloadUrl` "unavailable" state; add the `TrackTagger` seam.
7. Settings parity: `omega_settings` naming decision (migrate or document), tri-state
   theme, stream-quality default 160, live base-URL application, incognito listening
   toggle end-to-end (setting → `recordPlay` gate → History banner).
8. Room rework toward §4.4: shared song snapshot table, `exportSchema = true` with
   committed schemas, caps/pruning (history 500, recents 20) — with a real `Migration`,
   since `fallbackToDestructiveMigration` stays off.

**Phase C — UX signature & polish**
9. Shared-element mini→full player transition (or implement and document the
   slide+scale fallback in the PR, per UIUX §7 — an undocumented state swap is not
   acceptable); artwork Palette extraction with the contrast-scrim invariant;
   white 64 dp play-button rule; player tab pager (Up Next / Lyrics / Related
   wired to suggestions/autoplay).
10. Remaining FRs: paste-a-link/deep links (FR-16), share (FR-17), local-playlist
    detail (rename/remove/reorder/play), Recently-played cards that actually play.
11. Accessibility pass (content descriptions, seek semantics, target sizes) and
    adaptive layouts; test-suite build-out toward design §9 (repository, Room,
    ViewModel tests + first `androidTest` smoke tests).

*End of GAP_ANALYSIS.md — companion docs: [`API_RESEARCH.md`](API_RESEARCH.md) ·
[`UIUX_DESIGN.md`](UIUX_DESIGN.md) · [`APP_DESIGN.md`](APP_DESIGN.md) ·
[`palette.html`](palette.html)*
