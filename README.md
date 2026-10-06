# Omega — Saavn Music (Android)

[![Android CI](https://github.com/yadavmanishraj/omega/actions/workflows/android-ci.yml/badge.svg)](https://github.com/yadavmanishraj/omega/actions/workflows/android-ci.yml)

A professional, fully-featured JioSaavn music app for Android. **No login. No sign-up. No account.** The app opens straight into music.

> Publishing target: https://github.com/yadavmanishraj/omega
> Clone: `git clone https://github.com/yadavmanishraj/omega.git`

## SDLC

This app was built following a full SDLC: API research → UI/UX design → application
architecture → implementation. The design deliverables live in
[`docs/sdlc/`](docs/sdlc/):

- [API Research](docs/sdlc/API_RESEARCH.md) — endpoint-by-endpoint study of `jiosaavn-api` (and `jiosaavn-dl`)
- [UI/UX Design](docs/sdlc/UIUX_DESIGN.md) — design system, tokens, screen specs ([palette.html](docs/sdlc/palette.html) renders the palette)
- [Application Design](docs/sdlc/APP_DESIGN.md) — architecture, data, playback, downloads, testing and CI design
- [Gap Analysis](docs/sdlc/GAP_ANALYSIS.md) — honest design-vs-v1 comparison and phased roadmap

CI: the **Android CI** workflow ([runs](https://github.com/yadavmanishraj/omega/actions)) runs
lint, unit tests and a debug build on every push/PR to `main` (and on manual dispatch).

## Features
- **Home** — trending songs list + horizontal carousels (Albums, Playlists, Artists), Recently Played
- **Search** — Songs / Albums / Artists / Playlists tabs, recent searches (on-device), shimmer loading, error + retry, empty states
- **Details** — Album, Playlist and Artist screens (top songs / top albums, Play All / Shuffle)
- **Player** — Media3 ExoPlayer + MediaSession foreground service, mini-player → full player, seek with buffering indicator, shuffle / repeat (off/all/one), queue bottom sheet, lyrics (when the API returns them), favorite + download, playback speed (0.75–1.5x), sleep timer (15/30/60 min), suggestions/radio in the ViewModel layer
- **Library (all local, Room)** — Favorites, Downloads, Play History, user-created Local Playlists, sort toggle, clear history
- **Downloads** — WorkManager worker, quality-selected `downloadUrl`, offline playback from the Downloads library, progress/status + size in Room
- **Settings** — API base URL (default `https://saavn.dev/api/`), playback quality and download quality (48/96/160/320 kbps), dark theme, dynamic color
- **Design** — Material 3, dark-first, dynamic/artwork-tinted surfaces, edge-to-edge, skeleton shimmer

## No-auth decision
Neither the upstream `jiosaavn-api` nor this app has any user account concept: the API is unauthenticated, so an account layer would be pure friction. All personal data (favorites, history, playlists, downloads, settings) lives **on-device only** (Room + DataStore). There are no auth screens, no onboarding account flow, and no tracking.

## Architecture
Single-activity Jetpack Compose, MVVM + repository, clean-ish layers:
`data/remote` (Retrofit + OkHttp + kotlinx.serialization) → `data/repository` → `domain` models → `ui/viewmodel` (StateFlow `UiState`) → `ui/screens` + `ui/player` + `ui/components`. Hilt DI, Navigation Compose (bottom nav: Home / Search / Library + Settings, persistent mini-player), Coil, Room, Media3, WorkManager + Hilt-Work.
Serialization choice: **kotlinx.serialization** — it matches the API's zod-validated, tolerant JSON (lots of nullable/optional fields) with `ignoreUnknownKeys`/`coerceInputValues`, and avoids Moshi codegen setup.

## Build
Requires JDK 17 + Android SDK 35.
```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug   # app/build/outputs/apk/debug/
```
If you self-host `jiosaavn-api`, set its URL in Settings (app restart applies it).

## Credits / licenses
- API contract & response shapes studied from [sumitkolhe/jiosaavn-api](https://github.com/sumitkolhe/jiosaavn-api) (MIT) — no code copied; this is a clean-room Kotlin client of its documented REST API.
- Download behaviour (max-quality `.m4a`, sanitized naming, metadata) modelled on [bunnykek/jiosaavn-dl](https://github.com/bunnykek/jiosaavn-dl) — no code copied.
- Android best-practice patterns from [android/skills](https://github.com/android/skills) (Apache-2.0) — see STUDY.md for exactly which skills were applied and where.
- Content belongs to JioSaavn / its labels. This is an unofficial client for personal use; respect the API's and JioSaavn's terms.
