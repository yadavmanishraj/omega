# Omega Music — UI/UX Design Specification

> **Product:** Omega — professional-grade Android music app (Kotlin, Jetpack Compose, Material 3)
> **Backend:** JioSaavn unofficial API (`sumitkolhe/jiosaavn-api` shape: global search, songs/albums/artists/playlists, details, suggestions, lyrics, `downloadUrl[]` streams)
> **Publishing target:** `yadavmanishraj/omega`
> **Owner of this doc:** UI/UX Design agent, SDLC Phase 2 (parallel to API Research, feeds Application Design + Build)
> **Hard constraints:** **No login / no sign-up, ever.** No account wall, no onboarding gate, no "sign in to continue" upsell. App cold-starts straight into music. Everything personal (favorites, playlists, history, downloads) is **local/on-device in Room**.
> **Status:** Build-ready. Tokens in §3 are copy-paste values for the builder.

---

## 1. Product Feel & Design Principles

### 1.1 One-line feel
**"A record store at midnight — dark, artwork-led, instant, and calm."**
The app should feel like the music is already playing when you open it. Artwork *is* the UI: color, hero imagery, and hierarchy all derive from cover art. Chrome recedes; content glows.

### 1.2 Design principles (in priority order)

| # | Principle | What it means in practice | Anti-pattern it prevents |
|---|-----------|---------------------------|--------------------------|
| 1 | **Music in ≤2 taps, playback in ≤1** | Any visible song row plays on tap. Home content is tappable/playable, never informational dead-ends. Resume-play chip on cold start if history exists. | Splash screens, login walls, "learn more" interstitials |
| 2 | **Artwork is the interface** | Every list, hero, player and card leads with artwork at a consistent radius. Player background is derived from the current artwork. | Grey placeholder grids, text-only catalogs |
| 3 | **Dark-first, not dark-only** | Designed, tuned and QA'd on the dark scheme first. Light scheme is a first-class token swap, not an inversion afterthought. | Washed-out light themes, illegible artwork scrim text |
| 4 | **Calm density** | Generous 16 dp screen margins, one primary action per screen, max 5 bottom-nav destinations. Lists breathe; the player does not shout. | Spotify-style feature soup on the player, banner ads, stacked FABs |
| 5 | **Thumb-native** | Primary actions live in the bottom 60% of the screen. Mini-player is a thumb target. Seek, play/pause, favorite are all reachable one-handed. | Top-corner-only actions, 32 dp icon buttons |
| 6 | **State is always visible** | Playing / paused / buffering / downloaded / favorited / queued — each has an unmistakable visual (and non-color) cue everywhere a song appears. | "Is it playing?" ambiguity, duplicate downloads |
| 7 | **Offline is a normal state, not an error** | Downloads and cached artwork remain fully browsable offline. Offline UI degrades to Library, never to a dead error screen. | Full-screen "No internet" on launch |
| 8 | **Motion explains, never decorates** | Every animation answers "where did that go / where did this come from?" Shared-element artwork (list → player) is the signature transition. | Bouncy gratuitous springs, crossfades that lose place |
| 9 | **Respectful & private by construction** | No accounts, no tracking UI, no social pressure. History can be cleared in 2 taps; incognito-listening toggle in Settings. | Share-to-unlock, follower counts, streaks |
| 10 | **Accessible by default** | See §9. Contrast, 48 dp targets, TalkBack labels and font scaling are acceptance criteria, not a polish pass. | Icon-only unlabeled controls, 11 sp grey text |

### 1.3 Personality / voice
- Confident, quiet, music-literate. Microcopy names the music, not the mechanics ("Made from what you played", not "Algorithmic recommendations v2").
- Never guilt, never FOMO. Empty states invite; error states explain and offer one clear next action.
- Bollywood / Indian catalog is first-class (JioSaavn's strength): Hindi, Punjabi, Tamil, Telugu, etc. section labels and language chips on Home; transliterated titles rendered as returned by API, never forced-translated.

---

## 2. Benchmarking — Spotify / YouTube Music / Apple Music

### 2.1 Borrow / Avoid matrix

| App | Borrow (adopt) | Avoid (reject) | Our adaptation |
|-----|----------------|----------------|----------------|
| **Spotify** | Mini-player with progress hairline + swipe-to-next-track gesture; Home as stacked horizontal carousels by mood/time; green-on-dark "now playing" equalizer bars on the playing row; Queue as drag-to-reorder list; long-press / ⋮ context sheet for songs | Login wall before any value; cluttered Home with podcasts/audiobooks/ads mixed into music; tiny shuffle/repeat icons buried on player; forcing social features | Keep carousels + mini-player gestures + EQ bars. Home is **music only**. Shuffle/repeat are 48 dp, labeled-by-state player controls. Zero ads surface — there is none to design. |
| **YouTube Music** | Search with filter chips (Songs / Albums / Artists / Playlists / All) and voice-search affordance; "Quick picks" grid (2-column, 4 rows) from history; Up Next / Lyrics / Related as player tabs (swipeable); like/dislike thumb affordance clarity | Video-first clutter; inconsistent artwork aspect ratios; algorithm rows with no explanation; bright red brand overpowering artwork | Adopt chip-filtered search + player tab pager (Up Next / Lyrics / Related). Keep artwork strictly 1:1 (songs/albums) and circle (artists). Brand color is a restrained green-teal (§3.1) that yields to artwork color. |
| **Apple Music** | Large, beautiful typography — oversized display titles on Home greeting and player song title with marquee; frosted-glass (blur) player background from artwork; time-synced lyrics with tap-to-seek per line, active line enlarged; clean Library IA (Playlists / Artists / Albums / Downloaded) | White-first design that makes artwork pop less; slow, heavy page transitions; paywall prompts interrupting browsing ("Join Apple Music"); small seek affordances | Adopt blurred-artwork player, karaoke lyrics treatment, and Library IA — in dark-first. No paywall pattern exists to copy; our equivalent temptation — an API-error modal — is banned (use inline retry, §8). Lyrics active-line treatment is a signature detail (§5.11). |
| **JioSaavn (native, reference)** | Regional language prominence; editorial playlists for festivals / charts (Top 50, Weekly Top); album pages crediting label/language/year | Dated visual density, inconsistent radii, aggressive upgrade prompts | Surface charts/editorial on Home when API returns them; use one radius scale (§3.3) everywhere. |

### 2.2 What makes Omega *not* a clone
1. **Instant-on:** competitors gate or upsell; Omega cold-starts to Home with a Resume row. First screen = playable music.
2. **Dynamic artwork color system-wide:** not only the Player — Home hero, Album/Playlist headers and the mini-player tint derive from the relevant artwork's palette (with guaranteed-contrast scrim, §3.1.3).
3. **Downloads are visible, honest, first-class:** quality (kbps) and size shown before download, progress in-place on the row itself — borrowed from `jiosaavn-dl`'s transparency, which all three benchmarks hide.
4. **Local playlists with zero account friction:** create in 2 taps from any song's ⋮ menu.

---

## 3. Design System — Tokens (copy-paste for the builder)

Conventions: Compose `MaterialTheme.colorScheme` / `Typography` / custom `OmegaTokens` (spacing, radius, elevation). All hex are sRGB. Dark is the reference scheme (`dynamicColor` on Android 12+ is **opt-in in Settings, default OFF** — brand consistency + artwork color beats wallpaper color for a music app; when ON, it only tints surfaces, artwork-derived player color still wins).

### 3.1 Color

#### 3.1.1 Dark scheme (primary, default)

| Role (M3 token) | Hex | Use |
|---|---|---|
| `primary` | `#1ED760`→ tuned `#3BE477` on dark¹ | Play FAB is *not* this (see `onPrimary`-on-white rule, player) — primary is for selected chips, active nav indicator, links, progress |
| `onPrimary` | `#003314` | Text/icons on primary |
| `primaryContainer` | `#0E3B21` | Selected filter chip bg, active queue row tint |
| `onPrimaryContainer` | `#7BF0A2` | Text on primaryContainer |
| `secondary` | `#A8C7C2` | Secondary accents, lyrics inactive highlight |
| `tertiary` | `#FFB86B` | Download-complete badge, quality (320 kbps) tag — warm accent only |
| `background` | `#0B0F0E` | App background — near-black with a green hint, **not** pure `#000` (avoids OLED smear banding on artwork) |
| `onBackground` | `#F2F5F3` | Primary text on background (contrast 15.8:1) |
| `surface` | `#121715` | Cards, sheets base |
| `surfaceVariant` / `surfaceContainer` | `#1C2321` | Carousel cards, search field, mini-player |
| `surfaceContainerHigh` | `#262E2B` | Bottom sheets, menus, player queue drawer |
| `onSurface` | `#F2F5F3` | Primary text (15.2:1 on surface) |
| `onSurfaceVariant` | `#B9C4BF` | Secondary text — artist names, durations (8.9:1 — AA+ for normal text) |
| `outline` | `#3A4541` | Dividers, card borders (decorative only) |
| `outlineVariant` | `#262E2B` | Hairline separators in lists |
| `error` | `#FFB4A8` / container `#5C1A12` | Inline errors only (never full-screen bg) |
| Scrim over artwork | `#000000` @ 55% → 0% gradient | Text legibility on artwork headers (see §3.1.3) |
| Playing indicator | `primary` bars on transparent | Animated EQ (3 bars) — always paired with "Now playing" semantics |

¹ Spotify-bright green is a deliberate familiarity cue for "play/go", but Omega's value `#3BE477` is slightly softer to sit with artwork palettes. **Play button rule:** the main Play/Pause control is a 64 dp circle, `surface` = white `#FFFFFF`, icon = black `#000000` (Apple/Spotify shared convention) — highest-contrast element on the Player, independent of theme/artwork color. Do not theme-tint it.

#### 3.1.2 Light scheme

| Role | Hex | Notes |
|---|---|---|
| `primary` | `#006B32` | 5.9:1 on white for text use |
| `onPrimary` | `#FFFFFF` | |
| `primaryContainer` | `#A9F2BC` / `onPrimaryContainer` `#00210C` | Selected chips |
| `background` | `#F7FAF8` | Green-tinted paper white |
| `onBackground` | `#171D1A` | 14.9:1 |
| `surface` | `#FFFFFF` / `surfaceVariant` `#E4EBE7` / `surfaceContainerHigh` `#EDF2EF` | |
| `onSurface` | `#171D1A` / `onSurfaceVariant` `#4A5550` (7.4:1) | |
| `outline` | `#6F7A75` / `outlineVariant` `#CBD5CF` | |
| `tertiary` | `#8A5A00` / error `#BA1A1A`, container `#FFDAD6` | |
| Scrim over artwork | `#000000` @ 45% → 0% | Header text is white-on-scrim in **both** themes (artwork headers do not theme-flip) |

#### 3.1.3 Artwork-derived (dynamic) color — rules, not just vibes
- Extract palette from artwork (Coil bitmap → AndroidX Palette, or Media3/Compose equivalent) on image load; cache by artwork URL in memory.
- Generate: `artDominant`, `artVibrant`, `artMutedDark` (darken vibrant to L* ≤ 0.18).
- **Player background** = vertical gradient `artMutedDark` (top, 100%) → `background` (bottom). **Album/Playlist/Artist header** = same gradient over the header region only; body returns to `background`.
- **Guaranteed contrast invariant:** any text over artwork color sits on a scrim of black @ ≥45% *or* the derived color must pass 4.5:1 against `onBackground` white — test both in code, pick scrim if either fails. Never place `onSurfaceVariant` text directly on artwork color.
- **Fallback palette** (no artwork / extraction fails): deep teal `#0E3B33` → `background`. Never grey, never a random hue.
- Crossfade 300 ms when palette changes (track change) — no hard cuts (§7).

#### 3.1.4 Semantic / state colors
| State | Token | Non-color cue (mandatory pair) |
|---|---|---|
| Playing | `primary` | Animated EQ bars + bold title |
| Downloaded | `tertiary` icon `#FFB86B` (dark) | Filled ↓-in-circle icon, always present (not color-only) |
| Favorite | `primary` filled heart | Filled vs. outline heart shape change |
| Error | `error` | ⚠ icon + copy (§8), never color alone |
| Explicit (if API flags) | `onSurfaceVariant` outlined "E" box | Text glyph "E", 12 dp box |

### 3.2 Typography

Font: **System default (Roboto / device sans) via Material 3 type scale** — no bundled font (APK size, font-scaling fidelity, Dynamic Type parity). Optional builder upgrade: `Poppins` for `display*` only if APK budget allows; tokens below are size/weight/leading, font-family agnostic.

| M3 style | Size / Line height / Weight | Used for |
|---|---|---|
| `displayLarge` | 40 sp / 44 / Bold (700), -0.5 tracking | Home greeting hero ("Good evening") — max 1/screen |
| `displayMedium` | 32 / 38 / Bold | Album/Playlist hero title (long titles step down to `headlineLarge`) |
| `headlineLarge` | 28 / 34 / Bold | Screen titles in collapsing app bars (Search, Library) |
| `headlineMedium` | 24 / 30 / SemiBold (600) | Section headers on Home ("Trending songs"), Player song title |
| `headlineSmall` | 20 / 26 / SemiBold | Sheet titles, dialog titles |
| `titleLarge` | 18 / 24 / SemiBold | Album/Playlist list titles, settings group headers |
| `titleMedium` | 16 / 22 / Medium (500) | **Song title in rows (default)**, card titles — the workhorse |
| `titleSmall` | 14 / 20 / Medium | Mini-player title, carousel card titles |
| `bodyLarge` | 16 / 24 / Regular | Lyrics inactive lines, about/description text |
| `bodyMedium` | 14 / 20 / Regular | Artist line in rows (with `onSurfaceVariant`), descriptions clamped 2 lines |
| `bodySmall` | 12 / 16 / Regular | Durations, meta (year • language • label) — minimum text size in app is 12 sp |
| `labelLarge` | 14 / 20 / SemiBold | Buttons, chips, bottom-nav labels (selected) |
| `labelMedium` | 12 / 16 / Medium | Bottom-nav labels (unselected), badges, quality tags ("320") |
| `labelSmall` | 11 / 14 / Medium, +0.5 tracking, UPPERCASE | Overline eyebrows ("ALBUM", "PLAYLIST", "SINGLE") only |

Rules: Song titles never truncate to <1 full line in rows (ellipsis at 1 line in rows, 2 lines on Player). Lyrics active line = `bodyLarge` SemiBold scaled 1.15× (§5.11). Support font scaling to 200% without clipping: rows grow vertically, player title marquee is **disabled** above 130% scaling (wrap to 2 lines instead).

### 3.3 Spacing, radius, elevation, layout tokens

```kotlin
object OmegaSpacing { // dp
  val xxs = 2.dp; val xs = 4.dp; val sm = 8.dp; val md = 12.dp
  val lg = 16.dp   // THE screen margin & default content padding
  val xl = 24.dp   // section vertical gap (header -> content = md, section -> section = xl)
  val xxl = 32.dp; val xxxl = 48.dp // player vertical rhythm
}
object OmegaRadius {
  val artworkRow = 8.dp      // 56dp row thumbnails
  val artworkCard = 12.dp    // carousel cards
  val artworkHero = 16.dp    // album/player large artwork (player: 16, not full-bleed square)
  val sheetTop = 28.dp       // M3 bottom sheet standard
  val chip = 8.dp            // M3 filter chip (stadium in search = fully rounded, see components)
  val card = 16.dp; val dialog = 24.dp
}
// List row anatomy (all song rows everywhere — invariant):
// [16 margin][56 artwork, r8][12 gap][title/subtitle column][8][trailing action 48dp]
// Row height: 72.dp (56 + 8+8 padding). Two-line rows never vary height.
```

| Token | Value | Use |
|---|---|---|
| Screen margin | 16 dp | All screens, all widths <600 dp |
| Content max width | 600 dp center | Tablets/foldables: cap content column, carousels may bleed to edge |
| Section gap | 24 dp vertical | Between Home carousels |
| Elevation L0 | 0 dp, `background` | Base screens |
| Elevation L1 | 0 dp + `surface` color (tonal, no shadow) | Cards — **tonal elevation only in dark**; shadows read as dirt on dark |
| Elevation L2 | `surfaceContainerHigh` + 3 dp shadow (light theme only) | Mini-player (must visually float above nav bar), menus |
| Mini-player height | 64 dp + 2 dp progress hairline | Sits directly above bottom nav, full-bleed width, 8 dp side inset, r12 |
| Bottom nav height | 80 dp (M3 standard, icon+label) | 3 destinations (§4) |
| Player artwork size | min(screenWidth − 48 dp, 360 dp), 1:1, r16 | Scales down on small/landscape |

Grid: 4 dp base. Carousel card widths: Songs/Albums 148 dp (artwork 148×148), Playlists 148 dp, Artists 120 dp (circle Ø120), Quick-picks grid cell = (width − 48)/2.

### 3.4 Iconography
- **Set:** Material Symbols Rounded (filled variant for selected/active states, outlined otherwise) — single set, no mixing.
- Sizes: 24 dp default; 20 dp in chips/badges; 32 dp player secondary controls; play/pause glyph 36 dp inside 64 dp button.
- Key glyphs (builder: use these exact symbols): `play_arrow`, `pause`, `skip_next`, `skip_previous`, `shuffle`, `repeat` / `repeat_one` (badge "1"), `favorite` / `favorite_border`, `download` / `download_done` / `downloading`, `queue_music`, `lyrics`, `more_vert`, `search`, `home`, `library_music`, `settings`, `history`, `timer` (sleep timer), `speed`, `share`, `playlist_add`, `check_circle` (downloaded), `error`, `cloud_off` (offline), `close`, `drag_handle` (queue reorder), `arrow_back`.
- State rule: **active toggle = filled icon + `primary` tint + a11y state announced** (shuffle/repeat/favorite/download-done). Inactive = outlined + `onSurfaceVariant`.
- No custom-drawn icons except the app launcher icon and the EQ bars animation.

---

## 4. Information Architecture & Navigation

### 4.1 IA map

```
Omega (no auth layer — root IS Home)
├── Bottom Nav (3, always visible except Full Player / Lyrics full-screen)
│   ├── Home            [home]        start destination
│   ├── Search          [search]      search field is focused-ready, not auto-focused
│   └── Library         [library_music]
│        ├── Favorites
│        ├── Playlists  (local playlists + saved Saavn playlists, segmented)
│        ├── Downloads
│        ├── Recently Played (History)
│        └── Local: Artists/Albums derived from downloads+favorites (v1: via Downloads/Favorites filters; no separate screens required)
├── Detail (pushed, no bottom nav label, back = system + top app bar)
│   ├── Album detail
│   ├── Playlist detail (Saavn editorial/user-from-API)
│   ├── Artist detail
│   ├── Local Playlist detail / edit
│   └── Genre/Language/Chart listing (generic "Collection" screen reusing Playlist layout) — if API design provides it
├── Player (modal layer, NOT a nav destination — overlays everything)
│   ├── Full Player (swipe-down / back to dismiss to mini-player)
│   ├── Queue (player tab / bottom sheet from player + from ⋮ menus)
│   └── Lyrics (player tab; full-screen lyrics route from ⋮)
├── Search flow
│   ├── Search landing (recent searches, trending searches if API provides, language chips)
│   └── Search Results (tabs: All | Songs | Albums | Artists | Playlists)
├── Global sheets/dialogs (any context)
│   ├── Song context sheet (⋮ / long-press) — Play Next, Add to Queue, Add to Playlist,
│   │    Go to Album, Go to Artist, Download, Favorite, Share, Song info
│   ├── Add-to-Playlist picker sheet (incl. "+ New playlist" inline)
│   ├── Create/Edit Playlist dialog
│   ├── Quality picker sheet (streaming / download quality)
│   ├── Sleep timer sheet, Playback speed sheet (player ⋮)
│   └── Clear history / Delete download / Remove playlist confirms (dialogs, destructive in error color)
└── Settings (pushed from Home avatar-less toolbar gear + Library header gear)
     ├── Appearance (Theme: System/Dark/Light; Dynamic color toggle; Artwork color on player toggle)
     ├── Playback (Streaming quality, Gapless/crossfade if Media3 supports, Normalize volume if available)
     ├── Downloads (Download quality, Download on Wi-Fi only, Storage used + Clear all downloads)
     ├── Data & Privacy (Clear listening history, Incognito listening toggle, Cache size + Clear cache)
     └── About (version, API source credit: sumitkolhe/jiosaavn-api, license/attribution, OSS licenses)
```

Design decision — **3 bottom-nav items, not 5**: Home / Search / Library matches user intent frequency and keeps targets large. Favorites/Downloads/History live *inside* Library as prominent shortcut cards (1 extra tap, zero discovery cost because they're visual tiles, §5.13) — this beats a 5-tab bar for a v1 with no social/downloads-store sections. Player is an overlay, never a tab.

### 4.2 Routes (Navigation Compose — for Application Design to align)

| Route pattern | Screen | Entry points | Arguments / notes |
|---|---|---|---|
| `home` | Home | start, nav | — |
| `search` | Search landing | nav | — |
| `search/results/{query}` | Search results | submit, recent/trending tap | `initialTab` optional query arg |
| `album/{albumId}` | Album detail | cards, rows, player ⋮ | ID from API `id` |
| `playlist/{playlistId}` | Playlist detail | cards, rows | API playlist id |
| `artist/{artistId}` | Artist detail | cards, rows, player | API artist id |
| `library` | Library root | nav | — |
| `library/favorites` | Favorites | Library tile | — |
| `library/playlists` | Local Playlists list | Library tile | — |
| `library/playlist/{localId}` | Local playlist detail/edit | list row | Room id (Long) |
| `library/downloads` | Downloads | Library tile, download-complete notification tap | — |
| `library/history` | Recently Played | Library tile, Home "Recently played" See-all | — |
| `settings` | Settings root | gear icons | sub-sections are in-page groups in v1 (single scroll screen), routes `settings/{section}` reserved |
| `player` | Full Player | mini-player tap, notification tap | Not in bottom-nav graph; presented as dialog/full-screen destination over current graph |
| `player/queue` | Queue | player queue button | Sheet or player-tab (see §5.10) |
| `player/lyrics` | Lyrics full-screen | player lyrics tab / ⋮ | Falls back to tab if builder implements tabs only |

### 4.3 Deep links
| URI | Maps to | Fallback if target fails to load |
|---|---|---|
| `omega://album/{id}`, `omega://playlist/{id}`, `omega://artist/{id}` | Respective detail screens | Detail error state (§8) with "Go Home" |
| `omega://play/song/{id}` | Detail-less: fetch song, start playback, open Full Player | Toast "Couldn't play that song" + stay on Home |
| `https://www.jiosaavn.com/song|album|playlist|artist/...` (App Links, best-effort) | Resolve via API search/id if the API design supports URL resolution; otherwise open in browser | Browser intent — never trap user in an error screen for a link we can't resolve |
| Notification taps | Now-playing → `player`; Download complete → `library/downloads`; Download failed → `library/downloads` with failed filter highlighted | — |

Share targets share JioSaavn web URLs (not omega:// links) so recipients without the app land somewhere useful.

### 4.4 Back behavior & state restoration
- Back from any detail → exact scroll position of the source list (save/restore list state — non-negotiable).
- Back / swipe-down on Player → returns to whatever was beneath, mini-player visible, playback uninterrupted.
- Back on a bottom-nav root → Android predictive back exits app; **playback continues** (foreground service) — exiting UI never stops music without an explicit Stop from notification/player.
- Process death: restore nav stack (SavedState), queue + position (MediaSession), and Home scroll. Search query restored on results screen.

---

## 5. Screen-by-Screen Design

Global chrome rules (apply to every screen unless noted):
- Top app bar: transparent over `background`, title in `headlineLarge` on root screens; collapsing to `titleLarge` on scroll for detail screens.
- Every scrollable screen reserves bottom content padding = mini-player (64) + nav (80) + 16 dp so the last row is never hidden behind chrome.
- Pull-to-refresh on Home, Album, Playlist, Artist, Library lists (M3 `PullToRefreshBox`, `primary` indicator). Not on Player/Search-typing.

### 5.1 Home
**Purpose:** Playable, personalized-without-accounts start. Cold start lands here in all states (online, offline, first run).

```
┌──────────────────────────────┐
│ Good evening            ⚙    │  displayLarge greeting (time-of-day), gear -> Settings
│ [▶ Resume: <last song> ]     │  ONLY if history exists — primaryContainer pill card
│ 🔍 Search songs, artists…    │  fake search bar (tap -> Search, keyboard opens there)
│ ── Jump back in ──────── →   │  Recently played — horizontal song cards 148
│ [art][art][art][art]         │
│ ── Trending songs ────── →   │  Ranked list preview: 4 song rows w/ rank # (See all -> Collection)
│  1 [art] Title · Artist  ⋮   │
│ ── New albums ────────── →   │  Album cards 148 + title/artist
│ ── Playlists for you ─── →   │  Playlist cards 148 (editorial/charts from API)
│ ── Top artists ──────── →    │  Artist circles Ø120 + name
│ ── Browse by language ──     │  Chip/cloud grid: Hindi Punjabi Tamil Telugu English…
│ [chip][chip][chip]           │  tap -> Collection/Playlist-style listing for that language if API supports
│ ── Charts ────────────── →   │  Playlist cards (Top 50 etc.) if returned
└──────────────────────────────┘
 [ mini-player ]  [ Home|Search|Library ]
```

- **Data sections are API-driven and order-tolerant:** render sections in API/Repository order; any section that returns empty/fails is **omitted silently** (never an empty section header, never a whole-screen error because one rail failed). If ALL sections fail → Home error state (§8).
- First run (no history): no Resume card, no "Jump back in"; greeting + Trending + Albums + Playlists carry the screen. **No onboarding, no permission ask before first play.** Storage permission is never asked (app-private storage for downloads).
- Greeting by local time: 05–12 "Good morning", 12–17 "Good afternoon", 17–22 "Good evening", 22–05 "Good night". Name is NEVER used — there is no account/name to use.
- "See all →" per section → `Collection` listing (full vertical list of that rail's type) or the type's tab in Search Results if no collection endpoint exists (Application Design to decide; UI is identical either way).

### 5.2 Search — Landing
```
┌──────────────────────────────┐
│ [←][ search field........][x]│  auto-focus ONLY when arrived via Home search bar tap; mic icon at end (voice search if available, else omit — never a dead icon)
│ Recent searches         Clear│  labelLarge header + text button (confirm NOT required; undo snackbar 4s)
│  🕘 arijit singh          x  │  rows: history icon, query, per-item remove (48dp)
│  🕘 kesariya              x  │
│ Trending now                 │  ONLY if API provides; else omit section entirely
│  1 Kesariya                  │  ranked rows, tap = search that query
│ Browse languages             │
│ [Hindi][Punjabi][Tamil]…     │  chips grid -> results filtered / collection
└──────────────────────────────┘
```
- Typing shows **live suggestions dropdown** (debounced 300 ms per API design): up to 8 rows, typed substring bolded; suggestion tap searches it; arrow icon fills query without searching. Keyboard search/IME action submits → Results, `All` tab.
- Empty query + submit = no-op (no error shake, no toast).

### 5.3 Search — Results
```
┌──────────────────────────────┐
│ [←][ query            ][x]   │
│ (All)(Songs)(Albums)(Artists)(Playlists)  scrollable stadium chips, selected = primaryContainer
│ ── Top result ───────────    │  ALL tab only: hero card — best match, large artwork,
│ [ big art ] Title            │  type eyebrow (SONG/ALBUM/ARTIST), primary action = Play (songs) / Open
│ ── Songs ────────────────    │  ALL tab: max 4 song rows + "See all songs >" -> Songs tab
│ [art] Title · Artist ▶eq ⋮   │
│ ── Albums ─────────── →      │  horizontal cards (ALL) / vertical grid 2-col (Albums tab)
│ ── Artists ───────────→      │  circles row (ALL) / vertical rows (Artists tab)
│ ── Playlists ──────────→    │  cards row (ALL) / grid (Playlists tab)
└──────────────────────────────┘
```
- Tabs are **filter chips + content switch**, keeping the same query and scroll-reset per tab. Paging: infinite scroll, footer skeleton row while loading next page, "End of results · N found" footer at end.
- Per-tab empty state (§8) — e.g. no albums: "No albums for 'xyz'" + suggestion to check spelling / try Songs tab. Zero results on All: illustration-free empty state with 3 trending suggestion chips.
- Song rows in results show download state and favorite state live (same row component as everywhere, §6).

### 5.4 Album Detail
```
┌──────────────────────────────┐
│ ←                        ⋮   │  ⋮: Play, Shuffle play, Download album, Share
│ ▓▓ header (artwork gradient)▓│
│      [ artwork 176, r16 ]    │
│      Album Title (2 lines)   │  displayMedium -> headlineLarge step-down
│      Artist · 2024 · Hindi   │  bodyMedium, onSurface over scrim
│      12 songs · 44 min       │
│  [ ▶ Play ]  [ ⤨ Shuffle ]   │  Play = white 56dp pill (primary action), Shuffle = tonal pill
│  [♡][⬇ Download all][share]  │  48dp icon buttons row
│  1 [art*] Track title    ⋮   │  *track rows may omit artwork if album art identical — keep 56 art anyway for visual rhythm; builder choice, consistent app-wide
│  2  Track title (playing: EQ + primary title)
│  About this album            │  description text if API returns it, clamped 3 + Expand
│  More by <Artist>        →   │  album cards rail, if API suggestions available
│  You might also like     →   │  album cards rail, optional
└──────────────────────────────┘
```
- Header collapses on scroll to compact bar: 40 dp artwork + title + mini Play button (sticky) — user can always play/pause without scrolling back.
- "Download all": one action, progress aggregates in header button ("Downloading 3/12…"), cancellable from Downloads screen. Already-downloaded tracks show ✓ per row.

### 5.5 Playlist Detail (Saavn/editorial)
Identical skeleton to Album with these differences: eyebrow `PLAYLIST`, meta = curator/source (or "JioSaavn") · song count · duration · followers if API returns (render only if present); header artwork may be a 2×2 mosaic if playlist art is a collage in API image — else single art. Add: **Save to Library** bookmark toggle (saves reference locally — distinct from local playlists) and per-row remove is NOT available (not user's playlist). Sort control in ⋮ if API returns unsorted (v1: omit sort if not trivial).

### 5.6 Artist Detail
```
┌──────────────────────────────┐
│ ←                        ⋮   │  ⋮: Shuffle play artist, Share
│ ▓▓ blurred art header ▓▓▓▓▓ │
│      ( circular art Ø144 )   │
│      Artist Name             │
│      1.2M listeners (if API) │
│  [ ▶ Play ]  [ ⤨ Shuffle ]   │  plays Top Songs queue
│ ── Top songs ───────────     │  5 rows + Expand to 10 (in-place)
│ ── Albums ────────────── →   │  cards rail
│ ── Singles & EPs ─────── →   │  cards rail, only if API separates them
│ ── Playlists featuring ── →  │  cards rail, optional
│  About                       │  bio text if returned, clamped + Expand; monthly listeners / dominant language meta chips
└──────────────────────────────┘
```
Follow button: **Do not ship a fake Follow.** With no accounts and possibly no local-follow feature in v1 architecture, either implement *local* Follow (Room, affects Home "Your artists" rail) with label "Follow" persisted locally — preferred if Architecture includes it — or omit the button entirely. A button that does nothing / requires login is banned by §1.

### 5.7 Full Player — see §5.10 (player system, treated as one unit with mini-player/queue/lyrics)
### 5.8 Queue — §5.10.3 · Lyrics — §5.11

### 5.9 Library (root)
```
┌──────────────────────────────┐
│ Library                   ⚙  │
│ ┌──────────┐ ┌──────────┐    │  2×2 shortcut tiles (surface, r16, icon + label + count)
│ │ ♡ Favorites│ │ ⬇ Downloads│  │  counts = live Room counts ("128 songs", "24 · 312 MB")
│ │  128 songs │ │ 24 · 312MB │  │
│ └──────────┘ └──────────┘    │
│ ┌──────────┐ ┌──────────┐    │
│ │ 🕘 History │ │ ≣ Playlists│  │
│ │  Recently  │ │ 6 playlists│  │
│ └──────────┘ └──────────┘    │
│ ── Recently played ───── →   │  rail (same as Home's) — Library doubles as history home
│ ── Your playlists ────── +   │  vertical rows: mosaic/cover, name, N songs; + = create
│ ── Saved playlists ─────     │  Saavn playlists saved in §5.5, distinct section + eyebrow "SAAVN"
│ ── Downloaded albums ─── →   │  cards rail derived from downloads, only if any
└──────────────────────────────┘
```

### 5.10 Player System — Mini-Player, Full Player, Queue

#### 5.10.1 Mini-player (persistent)
- Anatomy: `[56 art r8][title 1-line / artist 1-line][♡ 48][▶/⏸ 48]`, progress hairline 2 dp on top edge (`primary`, or buffering indeterminate shimmer), bg `surfaceContainer` L2 floating 8 dp inset above nav.
- Gestures: tap body → Full Player (shared-element artwork, §7); swipe left/right on body → next/previous track (with 150 ms artwork slide); swipe up → Full Player; swipe down → **nothing** (never dismiss-to-stop; stopping requires pause).
- States: buffering = play button becomes circular progress (tap still pauses intent); error-to-load track = auto-skip to next after toast "Couldn't play — skipped" (max 3 consecutive skips, then stop + error state on mini-player with Retry).
- Hidden ONLY on: Full Player, full-screen Lyrics, and when queue is empty AND nothing has ever played this install (fresh first run before first tap).

#### 5.10.2 Full Player
```
┌──────────────────────────────┐
│  ⌄ (drag handle)        ⋮    │  ⋮ sheet: Sleep timer, Speed, Add to playlist, Go to album/artist, Share, Song info
│ ▒▒ gradient from artwork ▒▒  │
│      ┌──────────────┐        │
│      │  artwork     │ 360 max │  shared-element from mini/list; swipe art L/R = next/prev (with peek of adjacent art if cheap, else slide)
│      └──────────────┘        │
│  Song Title (headlineMedium, │  marquee if >1 line & fontScale ≤1.3
│  Artist · Album (tap -> go)  │
│  ────●━━━━━━━━━━━━━━━       │  seek bar: custom — 4dp track, thumb grows to 20dp on drag,
│  1:23              -2:41     │  times labelSmall tabular; buffered track lighter segment
│  ⤨    ⏮     ( ⏸ )    ⏭    🔁 │  shuffle 32 / prev 40 / WHITE 64 play / next 40 / repeat 32
│  ♡    ⬇      ━━━ Queue  🎤   │  bottom row: Favorite / Download (progress ring when downloading)
│                              │  + Queue button + Lyrics button (with labels under icons, labelSmall)
│  ● ○ ○  (Up Next preview)    │  tapping bottom row items opens tabs below OR:
│ [Tabs: Up Next | Lyrics | Related] swipeable pager region (replaces lower half on swipe-up / tab tap)
└──────────────────────────────┘
```
- Layout contract: artwork + controls fit without scroll on ≥640 dp tall screens; on smaller screens artwork shrinks first (floor 200 dp), never controls.
- Seek: drag shows time tooltip above thumb; release commits; while seeking, times update live; accessibility seek via TalkBack actions ±10 s (§9).
- Repeat cycles: off → all → one (icon gains "1" badge + `primary`). Shuffle toggle is persisted per-queue. Both announce via semantics.
- Playback extras in ⋮: Sleep timer (Off/5/10/15/30/45/60 min / End of track — selected shows countdown chip on player next to ⋮), Speed (0.5–2.0× in 0.25 steps; note: pitch-corrected by Media3), Normalize/other EQ: **omit in v1 unless Architecture confirms Media3 support** (no dead menu items).
- Background playback / lock screen / notification are MediaSession defaults — design requirement: notification shows artwork, title/artist, prev/play/next, favorite action if platform allows; **never** a "login" anything.

#### 5.10.3 Queue (Up Next)
- Presentation: bottom sheet (from mini-player long-press / ⋮ "Open queue") AND player tab — **same composable**, sheet height 70% with drag handle.
```
│ Queue                    ✕ Clear (confirm only if >1 & not playing-only)
│ ─ Now playing ──────────────
│ [art] Title · Artist  EQ ⋮   │  not draggable, not removable
│ ─ Up next (from: Album X) ── │  source label — where this queue came from (album/playlist/search/radio)
│ [≡][art] Title · Artist  ✕   │  drag handle reorder (haptic on pickup/drop), swipe-left or ✕ removes
│ ─ Played earlier (collapsed) │  history section, dimmed 60%, tap replays from there
```
- Empty queue (nothing queued beyond current): "Nothing up next" + [Browse trending] button → Home.
- Add actions everywhere come from song ⋮: **Play Next** (inserts at +1, snackbar "Will play next") and **Add to Queue** (appends, snackbar "Added to queue").

### 5.11 Lyrics
- Two presentations, one component: **Player tab** (lower-half pager) and **full-screen route** (for reading; mini-player remains available when exiting).
- Unsynced lyrics (most likely API shape — plain text from lyrics endpoint): paragraph text `bodyLarge`, 24 dp padding, scrollable, source/copyright line at bottom if API returns it (`bodySmall`, variant). Copy button in header (copies plain text).
- Synced lyrics (if timestamps exist in API design): karaoke mode — inactive lines `onSurfaceVariant`, active line `onBackground` SemiBold 1.15×, auto-scroll keeps active line at 40% height, tap any line seeks to it, user scroll pauses auto-scroll for 5 s (resume pill "Follow" appears).
- **No-lyrics state is common — design it well:** centered `lyrics` icon + "No lyrics for this song" + [Search again isn't an action] — offer [View album] only. Never show a fake/web-searched lyrics result.
- Loading = 6 shimmer text lines of varying widths. Error = inline retry row, player keeps playing above.

### 5.12 Favorites
- Library tile destination. Vertical song-row list of favorited songs (newest first; ⋮ sort: Recently added / Title A–Z / Artist).
- Header actions: [▶ Play all] [⤨ Shuffle] [⬇ Download all] — same pattern as Album for consistency.
- Unlike from row ♡ or ⋮ → row animates out (collapse, 250 ms) **with Undo snackbar (4 s)** — no confirm dialogs for un-favoriting.
- Empty state: heart outline illustration-glyph, "Songs you love will live here", "Tap ♡ on any song to save it", [Discover trending] → Home.

### 5.13 (Reserved — Library root is §5.9)

### 5.14 Local Playlists — list, create, detail/edit
- **List** (Library → Playlists, also Library root section): rows with cover = 2×2 mosaic of first 4 song arts (or `tertiary`-tinted `playlist` glyph tile if empty), name, "N songs · duration".
- **Create:** from list [+], from song ⋮ → Add to Playlist → "+ New playlist", from Favorites/Album header ⋮. Dialog: single text field "Playlist name" (auto-focus, 60-char cap, IME Done = Create), [Cancel][Create]. Created playlist opens its (empty) detail with snackbar "Playlist created — add songs from any ⋮ menu" if created standalone; if created mid-"add song" flow, song is added and snackbar says "Added to <name>" with [View].
- **Detail:** Album-detail skeleton with differences: eyebrow `YOUR PLAYLIST`, Play/Shuffle/Download-all header actions, **Edit mode** (pencil in app bar): drag-reorder rows (≡ handles appear), swipe/remove per row, Rename + Delete playlist in ⋮ (Delete = confirm dialog, error-colored, copy states songs/downloads are NOT deleted). Cover picker in edit mode: choose from member-song arts grid (tap to set) — no image-file picker in v1.
- Empty detail: "No songs yet" + [Add songs] → Search with a persistent banner "Adding to <playlist name>" — tapping a result's + adds it (checkmark feedback, stays in Search for multiple adds, Done in banner exits mode). This multi-add flow is a v1 must-have; single-song ⋮ add alone makes playlist creation feel broken.

### 5.15 Downloads
```
┌──────────────────────────────┐
│ Downloads          ⋮         │  ⋮: Sort, Download quality, Clear all (confirm)
│ 24 songs · 312 MB · Wi-Fi…   │  storage summary line (bodySmall) + free-space if API/platform cheap: omit if not
│ [Active (2)] [Downloaded]    │  segmented control IF active downloads exist; else single list
│ [art] Title · 320kbps        │  downloading row: linear progress in-row + % + Pause ✕ Cancel (48dp each)
│ [art] Title ✓ Downloaded ⋮   │  ⋮: Play, Add to playlist, Favorite, Delete download (confirm, frees size in copy)
│ Failed row: ⚠ "Failed — tap to retry" full-row tappable
└──────────────────────────────┘
```
- Downloaded songs play **offline from local file**, row shows `download_done` + quality tag. Deleting a download never deletes favorite/playlist membership (copy in confirm: "The song stays in your library.").
- Sort: Recently downloaded (default) / Title / Size. Filter chip: All / This device storage is the only source — no cloud section exists.

### 5.16 Recently Played / History
- Full list version of Home's rail: song rows grouped by **Today / Yesterday / This week / Earlier** sticky date headers.
- Header ⋮: Clear history (confirm dialog: "This removes your listening history and 'Jump back in'. Downloads and favorites won't change." [Cancel][Clear]).
- Incognito note: if Settings → Incognito listening is ON, a persistent banner tops this screen: "Incognito is on — new plays aren't being saved" [Turn off].
- Empty: "Nothing here yet — your plays will show up here" + [Browse trending].

### 5.17 Settings (single scroll screen, grouped cards, no sub-routes in v1)
```
Appearance
  Theme                    System >   (radio dialog: System / Dark / Light)
  Dynamic color (wallpaper) [toggle, default OFF]
  Artwork colors in player  [toggle, default ON]
Playback
  Streaming quality         High 320 > (sheet: Auto / Low 48 / Medium 96 / High 160 / Very High 320 kbps — values MUST match API downloadUrl qualities delivered by API research; labels show kbps always)
  Download quality          Very High 320 > (same sheet)
Downloads & storage
  Download on Wi-Fi only    [toggle, default ON]
  Storage used by downloads 312 MB    [Clear all downloads — destructive row, confirm]
  Music cache               84 MB     [Clear cache]
Privacy
  Incognito listening       [toggle, default OFF]  subtitle: "Don't save plays to history"
  Listening history                 [Clear history — confirm, same as §5.16]
About
  Version 1.0.0
  Music data & API          JioSaavn via open-source API (sumitkolhe/jiosaavn-api) >
  Open-source licenses      >
```
- Every row is a full-width 56 dp tappable target with current value visible without opening (value on right, `onSurfaceVariant`).
- Toggles use M3 Switch; state persists in DataStore (Architecture's call — UI requirement is instant apply, no Save button anywhere in Settings).
- Theme changes apply live including Player artwork gradient re-derivation; no restart, no flicker to white.

---

## 6. Component Inventory (with states)

Builder rule: build these once in `ui/components`, reuse everywhere. A song must look and behave identically on Home, Search, Album, Queue and Favorites — state badges are part of the component, not screen-level patches.

| Component | Anatomy / key tokens | States & variants |
|---|---|---|
| **SongRow** (the atom) | 72 dp; 56 art r8; title `titleMedium` 1-line; subtitle `bodyMedium` variant (Artist • duration or Album); trailing slot (⋮ default) | `default` / `playing` (EQ bars overlay on art bottom-left + title in `primary`, semibold) / `paused-current` (pause-bars glyph, static) / `buffering` (art overlay spinner) / `downloaded` (small ✓ badge bottom-right of art) / `downloading` (art overlay ring %) / `disabled-offline` (if in a *remote-only* list while offline: 55% opacity + cloud_off glyph in subtitle) / pressed (M3 ripple/state layer) |
| **ArtistRow** | Circle art Ø56, name, "Artist • N songs" | Same playing logic n/a; chevron-free (whole row taps) |
| **AlbumCard / PlaylistCard** | 148 art r12, title `titleSmall` 2-line, subtitle `bodySmall` 1-line | Pressed scale 0.98; loading = shimmer art block; playlist variant adds song-count in subtitle |
| **ArtistCard** | Ø120 circle, centered name below | — |
| **Carousel (Rail)** | Header row (`headlineMedium` + "See all" text-button w/ arrow) + horizontal LazyRow, 12 dp item gap, 16 dp start pad, end-bleed | Skeleton rail (§8); empty → section omitted at screen level |
| **QuickPicksGrid** | 2-col × up-to-4 rows of compact SongRows (art 48) | Home/Library reuse |
| **FilterChipRow** | Stadium chips, 36 dp high, selected = `primaryContainer` bg + `onPrimaryContainer` text + check icon 16 | Selected/unselected/disabled; horizontally scrollable, edge fade |
| **DetailHeader** | Gradient from artwork palette, 176 art, eyebrow, title, meta, action row (Play pill 56 white + Shuffle tonal + icon actions) | Collapsed (56 dp bar: 40 art + title + play) ↔ expanded, scroll-driven; skeleton variant |
| **PlayButton (white)** | 64 dp player / 56 dp header pill; white bg, black glyph | Play / Pause / Buffering-spinner (black spinner on white) |
| **SeekBar** | Custom Canvas/Slider: track 4 dp (played = white on player, `primary` elsewhere if reused; buffered = white 35%; rest = white 25% on player / `surfaceVariant` elsewhere), thumb 12 dp → 20 dp dragging, time tooltip pill | Idle / dragging (times + tooltip) / buffering (indeterminate top hairline) / disabled (no stream yet) |
| **MiniPlayer** | §5.10.1 anatomy, `surfaceContainer` L2, r12, 8 inset | Playing / paused / buffering / error-skipped toast source / hidden (first-run) |
| **ContextSheet (Song ⋮)** | M3 modal bottom sheet r28 top: header = SongRow (non-tappable) + divider + action rows 56 dp (icon 24 + label) | Actions conditional: Download ↔ Delete download; Favorite ↔ Remove from favorites; "Go to radio/similar" only if API supports it — order fixed: Play next, Add to queue, Add to playlist, Favorite, Download, Go to album, Go to artist, Share, Song info |
| **LyricsView** | §5.11 | Synced / unsynced / loading-shimmer / none / error-retry |
| **QualityTag** | `labelMedium` in 4 dp-radius outline box: "96" "160" "320" | Shown in Downloads + Settings-picked rows only — not on every browse row (noise) |
| **StorageBar** | Linear bar: downloads share vs free, `bodySmall` labels | Settings/Downloads summary |
| **Snackbar patterns** | M3 snackbar, single line + 1 action max | Confirmations with Undo (favorite removal, queue clear, history clear via Undo is NOT offered — dialog instead), add-to feedback (no Undo) |
| **EmptyState** | Centered: 48 dp outlined glyph in `surfaceVariant` circle Ø96, `titleMedium` headline, `bodyMedium` variant subcopy, 0–1 primary tonal button | Per-screen copy in §5/§8 — component takes icon/headline/body/action |
| **ErrorRow / ErrorState** | Inline row variant (list sections) + full variant (screen-level, §8) | Retry button always; never auto-retry loops |

---

## 7. Motion & Animation Spec

Durations use M3 motion tokens; easing = `FastOutSlowIn` (emphasized) unless noted. All animations must respect the system "Remove animations" / animator-duration-scale = 0 → instant state changes, no broken intermediate states.

| Interaction | Spec |
|---|---|
| Mini → Full Player | **Signature transition.** Shared-element artwork (art bounds animate from 56 r8 → up to 360 r16), container expands upward 350 ms emphasized; title/artist cross-slide; background color crossfades to artwork gradient over same duration. Reverse on dismiss is a true mirror. If shared-element is not feasible in the chosen nav setup, fallback = slide-up 300 ms + artwork scale — **builder must flag which shipped** in PR notes; a plain fade is not acceptable. |
| Player drag-to-dismiss | Follows finger (offset + artwork shrinks toward mini size); release past 35% height or fling velocity → completes to mini-player; else springs back. Playback never hiccups during gesture. |
| Bottom-nav tab switch | Content crossfade 200 ms; no slide (tabs are peers); selected indicator = M3 pill morph; scroll state per tab preserved. |
| Detail push (Album/Playlist/Artist) | Slide-in from right 300 ms + slight parallax on source card (scale 0.98); pop mirrors. On Android 14+ predictive back, page must follow gesture (Navigation Compose default — do not override). |
| Bottom sheets | M3 standard: slide up 300 ms emphasized-decelerate, scrim fade; swipe-down dismiss; half-expanded state only for Queue sheet. |
| Song row play tap | Immediate: art gets EQ overlay (bars animate 3-bar loop, 900 ms cycle, heights 40/80/55%), title color crossfade 150 ms. Audio feedback (player state) must land within one frame of any buffering spinner showing — never a silent dead tap. |
| Favorite tap | Heart: scale pop 1 → 1.35 → 1 (250 ms spring, damping 0.5) + fill crossfade; haptic `confirm` tick if platform haptics on. |
| Seek thumb | Scale to 20 dp on touch-down (100 ms); tooltip fades in; buffered/played segments never animate width except real progress (linear, no easing on progress itself). |
| Carousel / list entrance | First load only: content fades+rises 8 dp staggered 30 ms/item capped at 6 items. **Never** re-animate on scroll-return or config change. Skeleton → content crossfade 200 ms. |
| Artwork palette change (track change) | Gradient crossfade 300 ms; artwork image crossfade 250 ms (Coil crossfade enabled) with 8 dp slide if swipe-initiated. |
| Download start on row | ⬇ icon morphs to ring progress (200 ms); completion = ring completes → ✓ badge pops + `tertiary` flash 300 ms. |
| Queue reorder | Item lifts (shadow/elevation L2 + scale 1.02) on long-press-drag; other items animate to new positions (300 ms); haptic on pickup and on drop. |
| Pull-to-refresh | M3 default spinner in `primary`; content does not blank — refresh is in-place over existing content. |

Reduced-motion: when animator scale = 0 or Settings-accessibility requests it — shared-element becomes instant cut, entrance staggers removed, EQ bars render static at mid-height (still conveys "playing" with title color + semantics).

---

## 8. Loading, Empty, Error & Offline States (with copy)

Copy tone: short, plain, names the thing, one action. No "Oops!", no exclamation marks, no technical error codes shown to users (log them).

### 8.1 Loading
| Context | Treatment |
|---|---|
| Cold start / Home first load | Full skeleton Home: greeting bar + 3 rails of shimmer cards + 4 row skeletons. Shimmer = `surfaceVariant` base with 40% white sweep 1200 ms, r matching real content exactly (layout shift = bug). Cap skeleton at API timeout; then content or error state — never skeleton forever. |
| Detail screens | Skeleton header (art block + 2 text bars) + 6 row skeletons. |
| Search typing / paging | Suggestions: 3 ghost rows; Results paging: footer skeleton row only (existing results stay interactive, incl. playback). |
| Player stream start | Artwork + titles render instantly from row data (never wait for stream); play button → buffering spinner; seek disabled until buffered. If >8 s: inline player banner "Taking a while — check your connection" with [Retry] [Play next]. |
| Images | Coil: placeholder = `surfaceVariant` block with centered `music_note` glyph 24 dp variant; error = same + broken-image glyph. Fade-in 250 ms. Cached images never re-shimmer. |

### 8.2 Empty states (headline / body / action)
| Screen | Headline | Body | Action |
|---|---|---|---|
| Search — no results (All) | `No results for "{query}"` | "Check the spelling, or try a different song, album or artist." | 3 trending chips (tap = search) |
| Search tab — e.g. Albums | `No albums for "{query}"` | "Try the Songs tab — or a shorter search." | [View Songs] |
| Favorites | "Songs you love will live here" | "Tap the heart on any song to save it here." | [Browse trending] → Home |
| Local Playlists | "No playlists yet" | "Make playlists for moods, drives and everything in between." | [+ New playlist] |
| Local Playlist detail | "No songs yet" | "Add songs from search, or from any song's menu." | [Add songs] → Search-add mode (§5.14) |
| Downloads | "Nothing downloaded yet" | "Download songs to listen offline, anywhere." | [Browse trending] |
| History | "Nothing here yet" | "Songs you play will show up here." | [Browse trending] |
| Queue (nothing up next) | "Nothing up next" | "Queue songs from any song's menu." | [Browse trending] |
| Lyrics | "No lyrics for this song" | — (no false promise) | [View album] if known |
| Library "Your artists/albums" style derived rails | — | Section omitted entirely (no empty rails in Library) | — |

### 8.3 Error & offline
| Situation | Treatment | Copy |
|---|---|---|
| **Offline at launch** (no network) | App opens normally. Home renders Downloads/Favorites/Recents from Room under a slim top banner; remote rails omitted. Mini-player/downloads fully functional. | Banner: "You're offline — showing your downloads and library." (cloud_off icon, dismissible per session) |
| Offline mid-session | Same banner appears; current playback of downloaded/cached continues; remote row taps give snackbar. | Snackbar: "You're offline. Downloaded songs still play." |
| Remote row tapped offline | No navigation to dead page; snackbar only. Detail screens for cached metadata may open but show offline error body for missing parts. | "Can't load this while offline." |
| Home all-sections fail (online) | Full ErrorState: `error` glyph circle, headline, body, [Retry]. Greeting + search bar remain functional above it. | Headline: "Couldn't load music" / Body: "Check your connection and try again." / [Retry] |
| One Home rail fails | Rail omitted silently; a subtle inline ErrorRow appears only after pull-to-refresh also fails (avoid punishing transient errors). | "Couldn't refresh — pull to retry" |
| Detail load fails | ErrorState in body, header skeleton replaced by generic fallback header (title if known from nav source else "Couldn't load"). | "Couldn't load this album." / [Retry] [Go back] |
| Search fails | Results area ErrorState, query + chips stay editable. | "Search didn't work." / [Try again] |
| Stream fails | Auto-skip logic §5.10.1; if queue exhausted: Player error banner. | Toast: "Couldn't play that song — skipped." / Banner: "Couldn't play this song." [Retry] [Next] |
| Download fails | Row → failed state in Downloads (§5.15), retry resumes where API allows (else restarts — size/progress copy must not claim resume if it restarts). | "Download failed." tap row to retry; notification mirrors this. |
| Storage full on download | Pre-flight check before starting: dialog with needed size vs available. | "Not enough space — this needs {size}. Free up space or pick a lower quality in Settings." |
| API returns empty track list (album/playlist exists but no tracks) | Detail header renders; body EmptyState. | "No songs available for this {album/playlist} right now." |

Retry policy (UI contract): every Retry re-runs only the failed unit (rail/section/single download/track) — never a full-app reload.

---

## 9. Accessibility

Non-negotiables — these are release criteria:

| Area | Requirement |
|---|---|
| **Contrast** | All text ≥ 4.5:1 against its actual background incl. over artwork (enforced by §3.1.3 scrim invariant); large/display text ≥ 3:1; icon-only controls ≥ 3:1. Verified values listed in §3.1 tables (onSurfaceVariant is 8.9:1 dark / 7.4:1 light — do not mute it further). Disabled states exempt but must still be perceivable (≥ 3:1 or paired label). |
| **Touch targets** | All interactive elements ≥ **48 × 48 dp** (chips 36 dp tall are the sole exception *only* where an adjacent 48 dp row hit-area extends — filter chips themselves get 48 dp tall touch area via padding). Player secondary controls 48 dp min. ⋮ buttons 48 dp. Seek thumb touch area 48 dp tall despite 4 dp visual track. |
| **Content descriptions** | Every icon button labeled: "Play", "Pause", "Next track", "Previous track", "Shuffle on/off", "Repeat off/all/one", "Favorite / Remove from favorites, {title}", "Download {title}", "More options for {title}", "Open queue", "Lyrics". Artwork images: decorative (null description) when adjacent text names the item; on Player, artwork description = "{Album} artwork". EQ playing indicator exposes `stateDescription = "Now playing"`. SeekBar: semantics with progress + TalkBack actions "Seek forward 10 seconds / back 10 seconds", and announces elapsed/total. |
| **Font scaling** | Fully usable at 200%: rows grow, cards keep art size + wrap titles to 2 lines, player switches title marquee off (§3.2), bottom nav labels never truncate (they wrap/hide-label fallback = selected-only label above 160%). No fixed-height text containers anywhere. Test at 0.85×, 1.0×, 1.3×, 2.0×. |
| **Screen reader flow** | Logical traversal: header → actions → list. Carousel items reachable individually; "See all" after rail header. Mini-player announces track changes politely (live region) — "Now playing: {title} by {artist}". Sheets trap focus; dialogs announce title first. Playback state changes (play/pause) announced by the control itself. |
| **Non-color cues** | Playing = EQ + bold/primary title + semantics. Favorite = filled shape. Downloaded = icon badge. Repeat-one = "1" badge. Error = icon + words. Quality = numerals. Nothing is conveyed by color alone (§3.1.4). |
| **Motion** | Respect animator-duration-scale / remove-animations (§7); no flashing content (EQ bars animate opacity/scale only, < 3 flashes/s irrelevant at these amplitudes); shimmer pauses when screen reader is active if it interferes (acceptable: keep, it's non-essential decoration marked invisible to a11y). |
| **Hearing / captions adjacent** | Lyrics, when present, are the hearing-access path — they must be reachable in ≤2 taps from Player and fully TalkBack-navigable line-by-line. Synced auto-scroll must not fight manual TalkBack scrolling (§5.11 pause rule). |
| **Input alternatives** | Every gesture has a button equivalent: swipe-next on mini-player ↔ Next button; drag-reorder queue ↔ ⋮ "Move up/down" in queue row menu (add these menu items for a11y even if hidden from the visual ⋮ elsewhere); swipe-dismiss player ↔ the ⌄ chevron button (visible, 48 dp). |
| **Focus & keyboard** | External keyboard/DPAD: visible focus indicator (`primary` 2 dp ring) on all controls; player shortcuts (space = play/pause) optional but focus order is mandatory on TV-adjacent form factors — v1 phones/tablets only, still keep focus visible. |

---

## 10. Responsive & Form Factors

| Class | Behavior |
|---|---|
| Compact phone (<600 dp wide) — **reference** | Exactly as §5. Bottom nav 3 items. Carousels bleed right edge. |
| Landscape phone | Player: two-pane (artwork left 45%, controls right) — controls never overlap artwork; Home rails unchanged; lists keep 16 dp margins with max-width 600 centered only if width > 840. |
| Medium (600–840, small tablet / foldable open) | Content max-width 600 centered for lists/details; Home rails get 2 rows visible width-wise; Library tiles 3-across. Bottom nav stays (no rail in v1). |
| Expanded (>840) | Same centered column + wider rails; Player artwork caps at 360 dp (§3.3). No multi-pane redesign in v1 — consistency over novelty; flag for v2. |
| Foldable posture | No special posture handling v1 except avoiding the hinge: centered 600 column naturally clears it. |

Dark-mode default: **Theme default = System**, but the design/QA reference is Dark (§3.1.1). First-launch appearance follows system — do not force dark against a light-system user; showcase screenshots use dark.

---

## 11. Handoff Checklist & Top UX Risks for the Builder

### 11.1 Assets / tokens to create first
- [ ] `OmegaColors` (dark+light schemes §3.1), `OmegaTypography` (§3.2), `OmegaSpacing/Radius` (§3.3) as theme objects.
- [ ] Artwork palette extraction utility with the §3.1.3 contrast-scrim fallback + unit-testable contrast check.
- [ ] Components in §6 in a single `ui/components` package with `@Preview` for every listed state (incl. playing/downloaded/offline row variants).
- [ ] Skeleton components geometrically identical to real components (same heights/radii) — build them from the same layout skeletons.
- [ ] Empty/Error state component (§8) — screens supply copy only.

### 11.2 Top 5 must-not-miss UX details (also in final report)
1. **Mini-player is sacred** — persistent, gesture-capable, never hidden by navigation, never stops on back/exit.
2. **Shared-element artwork into the Player** (or the documented slide+scale fallback) — it is the app's signature feel.
3. **Playing state visible on every song row everywhere** (EQ + title treatment + semantics), driven by one playback-state source.
4. **Offline opens to Library/Downloads, not an error** — offline is a mode, not a failure.
5. **Lyrics + Queue are player tabs/sheets, not separate apps** — same component, two presentations, playback never interrupts to reach them.

### 11.3 Explicit non-goals (do not build)
No login/sign-up/onboarding gate · no social/follow/share-to-unlock (share = system sheet only) · no ads/upsell surfaces · no video · no podcasts/audiobooks sections · no fake Follow/paywall buttons · no settings "Save" buttons · no full-screen error for partial failures.

---
*End of UIUX_DESIGN.md — companion: `palette.html` (same folder) renders these tokens visually.*
