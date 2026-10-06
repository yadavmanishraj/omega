# Omega — Redesign Spec (UI/UX Pro Max)

**Date:** 2026-10-07 · **Scope:** Home, Search, Library, Settings (+ shared shell: bottom nav, mini-player, full player)
**Stack constraint:** Jetpack Compose + Material 3 only. No login/sign-up anywhere. Dark-first. Must survive **200% system font scaling** without crushed/wrapped components (the user's phone runs a large font size; this already produced real bugs — see §9).

---

## 0. How this spec was derived (method + sources)

This spec applies the system in the cloned repo `nextlevelbuilder/ui-ux-pro-max-skill` (at `~/workspace/sdlc/uiux/ui-ux-pro-max-skill`), following that repo's own prescribed workflow (`/.claude/skills/ui-ux-pro-max/SKILL.md`, "Workflow" §): analyze requirements → generate a design system with its search tool → supplement with targeted domain searches → add stack guidelines. Everything below traces to a concrete file or search result in that repo:

| Source in the skill repo | What was taken from it |
|---|---|
| `SKILL.md` — priority table (10 rule categories, 1→10) | The priority order used to resolve conflicts: Accessibility (1) > Touch (2) > Performance (3) > Style (4) > Layout (5) > Typography & Color (6) > Animation (7) > Forms (8) > Navigation (9) > Charts (10) |
| `scripts/search.py "music streaming entertainment dark immersive" --design-system -p "Omega"` | Resolved style, palette, and font pairing (quoted in §1–§3) |
| `data/products.csv` (via `--domain product`) | Row "Music Streaming": primary style **Dark Mode (OLED) + Vibrant & Block-based**, palette focus "Dark (#121212) + Vibrant accents + Album art colors" |
| `data/styles.csv` (via `--domain style`) | Style `dark-mode-oled`: deep black `#000000`, dark grey `#121212`, minimal glow, text contrast **7:1+**, OLED power optimization, light mode "not-recommended" |
| `data/colors.csv` (via `--domain color`) | Row "Music Streaming" palette, note *"Dark audio + play green"* (full hex set in §3.1) |
| `data/typography.csv` (via `--domain typography`) | Pairing "Music/Entertainment": **Righteous** (display) + **Poppins** (body) |
| `data/ux-guidelines.csv` (via `--domain ux`) | `compact-label-overflow`, error-recovery/feedback rules (§8) |
| `references/quick-reference.md` | The full rule set, cited by rule id throughout (e.g. `chip-collection-reflow`, `dynamic-type`, `bottom-nav-limit`) |
| `references/pro-rules.md` | Native-app rules + the canonical Pre-Delivery Checklist (icon discipline, 48dp targets, safe areas, dark-mode contrast) — basis of §8 |
| `data/stacks/jetpack-compose.csv` (via `--stack jetpack-compose`) | "Material3 tokens, not hardcoded values"; "dark mode via `colorScheme`, not fixed colors" |
| `/.claude/skills/design-system/references/primitive-tokens.md` | 4px-base spacing scale, radius scale, type scale, duration tokens (§3.3–§3.5) |

**Honest gaps (per the skill's own "0 results" rule — do not fabricate):** `--domain icons` searches for music/player icons returned **0 results** (the icons DB is Phosphor/web-oriented). Icon guidance below therefore falls back to `pro-rules.md` (vector-only, one family, token sizes, filled-vs-outline discipline) mapped onto **Material Icons**, the Compose-native set the app already uses. Motion specifics come from `quick-reference.md` §7, not from a GSAP preset (GSAP is web-only and out of scope for Compose).

Where this spec conflicts with the earlier `UIUX_DESIGN.md`, this spec wins for the 4 in-scope pages; the older doc's rules that were device-proven (mini-player is sacred, offline is a mode not an error screen) are retained.

---

## 1. Design direction

**Chosen style: Dark Mode (OLED) + Vibrant & Block-based** — the primary recommendation for product type "Music Streaming" in `products.csv`, with style details from `styles.csv` row `dark-mode-oled`.

Why it fits Omega:

- **Entertainment/night context.** The skill lists Dark Mode (OLED) as "Best For: night-mode apps… entertainment… OLED devices, low-light". Music listening is disproportionately evening/commute/low-light — exactly Omega's use.
- **Album art is the color.** `products.csv` palette focus: "Dark (#121212) + Vibrant accents + **Album art colors**". A near-black canvas makes cover art (the only imagery the app has) carry the visual richness; UI chroma stays minimal — one play-green accent (the `colors.csv` note for this exact palette: *"Dark audio + play green"*).
- **Block-based = cards and rails.** "Vibrant & Block-based" (secondary in the product row) maps cleanly onto Compose: chunky artwork cards, big tappable rows, section blocks — all ≥48dp targets by construction, which serves the large-font user.
- **Power + performance.** OLED true-black backgrounds save battery during long listening sessions; the style's performance cost is rated "low" in `styles.csv`.
- **Anti-patterns to avoid** (from the `--design-system` output): "Cluttered layout" and "Poor audio player UX" — hence §6's player rules (one primary CTA per screen, `primary-action`; player controls get the largest targets on the device).

**Dials** (skill's optional design dials, chosen for the record): variance 4 (balanced/modern), motion 3 (subtle micro-interactions — a utility player, not a showcase), density 5 (standard 16–64dp spacing).

**What changes vs the current look:** the current theme is a green-tinted black (`#0B0F0E`) with a single neon green. The redesign keeps the play-green identity but re-bases surfaces on the skill's neutral midnight system (`#0F0F23` family) so artwork colors aren't fighting a green cast, adds the indigo secondary for gradients/hero tinting, and adds a real display font for brand moments. Light mode is retained (the app already ships it and the user has a theme toggle) even though the style marks light "not-recommended" — it is a secondary, token-complete variant, designed as a pair per rule `dark-mode-pairing`.

---

## 2. Design tokens

Token architecture follows the skill's design-system skill (primitive → semantic → component, `design-system/references/token-architecture.md`): components reference **semantic** Material 3 roles only; raw hex lives in `Theme.kt`. This is also the jetpack-compose stack rule: "Material3 tokens — Don't: Hardcoded values" (severity High).

### 2.1 Color

Skill palette (verbatim from `colors.csv`, "Music Streaming"), then the Material 3 mapping actually implemented. Dark-mode adjustments follow rule `color-dark-mode` ("dark mode uses desaturated / lighter tonal variants, not inverted colors") and `color-accessible-pairs` (4.5:1 AA, 7:1 target for OLED body text per the `dark-mode-oled` checklist).

**Skill palette (source):** Primary `#1E1B4B` · Secondary `#4338CA` · Accent `#22C55E` · Background `#0F0F23` · Foreground `#F8FAFC` · Card `#1B1B30` · Muted `#27273B` · Muted foreground `#94A3B8` · Border `#312E81` · Destructive `#EF4444`.

**Dark scheme (default, reference scheme) → `darkColorScheme`:**

| M3 role | Hex | Source / note |
|---|---|---|
| background | `#0F0F23` | skill background (midnight; near-black, OLED-friendly) |
| surface | `#12122B` | one step above background (style's dark grey `#121212` logic, hue-matched) |
| surfaceVariant / card | `#1B1B30` | skill Card |
| surfaceContainerHighest | `#27273B` | skill Muted (chips, skeleton base, mini-player) |
| primary | `#3BE477` | play-green, lightened from skill accent `#22C55E` for dark-mode legibility (rule `color-dark-mode`); retains Omega's existing brand green |
| onPrimary | `#0F172A` | skill "On Accent" — dark text on the green |
| primaryContainer | `#14532D` | pressed/selected wash for chips & nav indicator base |
| secondary | `#A5B4FC` | skill secondary `#4338CA` lightened to a dark-safe tonal variant |
| secondaryContainer | `#1E1B4B` | skill Primary — used for hero/gradient tinting, selected tab wash |
| onBackground / onSurface | `#F8FAFC` | skill Foreground — ≈14.9:1 on background (exceeds the style's 7:1 OLED target) |
| onSurfaceVariant | `#C3CAD9` | secondary text — lightened from skill muted fg `#94A3B8` so it holds ≥4.5:1 on card `#1B1B30` (≈7.4:1); `#94A3B8` itself is reserved for large text/icons only |
| outline | `#312E81` | skill Border (visible in dark — pro-rules: "separators visible in both themes") |
| outlineVariant | `#27273B` | dividers |
| error | `#EF4444` | skill Destructive; always paired with icon + text (rule `color-not-decorative-only`) |
| onError | `#000000` | per skill palette |
| scrim | `#000000` @ 60% | sheets/dialogs; pro-rules: measure against real background |

**Light scheme → `lightColorScheme`** (paired variant, rule `dark-mode-pairing`):

| M3 role | Hex | Note |
|---|---|---|
| background | `#F8FAFC` | skill Foreground inverted role |
| surface | `#FFFFFF` | card separation via outline, not shadow (pro-rules light surface readability) |
| surfaceVariant | `#E8EAF3` | indigo-tinted grey |
| primary | `#15803D` | green-700: white on it ≈4.6:1 (filled buttons); skill accent darkened for light bg |
| onPrimary | `#FFFFFF` | |
| secondary | `#4338CA` | skill Secondary as-is |
| onBackground / onSurface | `#0F172A` | ≈15:1 on background |
| onSurfaceVariant | `#475569` | ≈7:1 on background |
| outline | `#94A3B8` | visible border in light |
| error | `#DC2626` | skill-family destructive for light |

**Artwork-derived color (kept from current design, now rule-bound):** dynamic color stays **opt-in, default off** (Settings, §5.4). When on, it may only re-tint `secondaryContainer`/hero gradients — never `primary` (play-green is the constant affordance color; rule `consistency`) and never text colors (contrast pairs are pre-verified tokens only).

### 2.2 Typography

Pairing **"Music/Entertainment"** from `typography.csv`: **Righteous** for display, **Poppins** for everything else. Mood per the pairing: "music, entertainment, fun, energetic, bold, performance"; "Best For: Music platforms, entertainment, events".

Compose implementation: bundle via Google Fonts downloadable-fonts provider (`androidx.compose.ui.text.googlefonts`, `GoogleFont.Provider`) with fallback chain `Righteous → Poppins → Roboto/system` so text never blocks on a font download (rule `font-loading`: swap behavior, no invisible text). Righteous ships one weight (400) — use it only where its drawn weight reads as bold display.

| M3 role | Font | Size / weight / leading | Used for |
|---|---|---|---|
| displaySmall | Righteous | 36sp / 400 / 44sp | Home greeting only |
| headlineMedium | Righteous | 28sp / 400 / 36sp | Page titles (Library, Settings), player song title optional-off |
| headlineSmall | Poppins | 24sp / 600 / 32sp | Detail headers (album/artist name) |
| titleLarge | Poppins | 22sp / 600 / 28sp | Section headers on Home |
| titleMedium | Poppins | 16sp / 600 / 24sp | Song row title, card titles, dialog titles |
| titleSmall | Poppins | 14sp / 500 / 20sp | Mini-player title, media-card title |
| bodyLarge | Poppins | 16sp / 400 / 24sp | Primary body, lyrics, settings values |
| bodyMedium | Poppins | 14sp / 400 / 20sp | Secondary text, artist lines, supporting text |
| bodySmall | Poppins | 12sp / 400 / 16sp | Captions/metadata only — never primary content (rule: no body text <12px; type scale 12/14/16/18/24/32 from `primitive-tokens.md`) |
| labelLarge | Poppins | 14sp / 500 / 20sp | Buttons, tab labels, nav labels |
| labelMedium | Poppins | 12sp / 500 / 16sp | Chip labels |

Rules: hierarchy by size + weight, not color alone (`visual-hierarchy`, `weight-hierarchy`: bold headings 600–700, body 400, labels 500). Durations/timers use tabular figures (rule `number-tabular`) — set `fontFeatureSettings = "tnum"` on time labels in the player. Default platform letter-spacing (rule `letter-spacing`). **No fixed-height text containers anywhere** (see §8).

### 2.3 Spacing (4dp base — `primitive-tokens.md` spacing scale; rule `spacing-scale`)

Semantic aliases to define in an `OmegaSpacing` object: `xs 4 · sm 8 · md 12 · lg 16 · xl 24 · xxl 32 · xxxl 48`.
- Screen horizontal gutter: **16dp** (24dp at ≥600dp width, rule `adaptive gutters`).
- Section vertical gap: **24dp**; header-to-content: **8dp**; row internal padding: **12dp vertical / 16dp horizontal**.
- Touch-target gap: **≥8dp** between adjacent targets (rule `touch-spacing`).
- Lists get a bottom content inset ≥ mini-player + nav height so nothing hides behind fixed bars (rule `fixed-element-offset`; pro-rules "scroll and fixed element coexistence").

### 2.4 Corner radii (from `primitive-tokens.md` radius scale)

`sm 4 · md 8 · lg 12 · xl 16 · 2xl 24 · full`. Mapped: artwork in rows **8dp**, media cards **12dp**, hero/detail artwork **16dp**, cards/surfaces **16dp**, chips & buttons **full** (M3 default), bottom sheet top corners **24dp**, dialogs **24dp** (M3 default). One radius per component type, app-wide (rule `effects-match-style`).

### 2.5 Elevation / shadow

Dark OLED: elevation is **tonal, not shadow** — surfaces separate by the §2.1 surface steps (skill style: "low white emission", minimal glow). M3 tonal elevation: screen 0, cards 0 (outline `outlineVariant`), mini-player 3, bottom sheet 1 + scrim, dialogs 6. The single permitted glow: the full-player play button — primary container ring at 12% alpha, no blur-spread shadows elsewhere. Light mode: same tonal steps; cards get `outlineVariant` border instead of shadow (pro-rules light-surface rule).

### 2.6 Icons

Material Icons (vector-only; rule `no-emoji-icons`; pro-rules "Vector-Only Assets"). Sizes as tokens (pro-rules "Consistent Icon Sizing"): **20dp** inline/small, **24dp** default, **28dp** player secondary controls, **40dp** player skip, **48dp-glyph-in-64dp** play button.
Discipline (pro-rules "Filled vs Outline Discipline"): **one style per hierarchy level** — bottom nav & player transport = Filled; row trailing actions & settings rows = Outlined; selected nav item = Filled + indicator pill, unselected = Outlined (M3 idiom). AutoMirrored variants for directional icons (queue, back) — already started in the codebase (`Icons.AutoMirrored.Filled.QueueMusic`).
Every icon-only control gets a `contentDescription` (rule `aria-labels`); icons sitting beside a visible text label are decorative → `contentDescription = null` (pro-rules "Contextual Semantics"). *Current code passes `null` everywhere, including standalone buttons — that flips under this spec (§9).*

### 2.7 Motion

Tokens from `primitive-tokens.md` durations, applied per `quick-reference.md` §7: `fast 150ms` (chip/press feedback — and tap feedback must land ≤100ms, rule `tap-feedback-speed`), `normal 200ms` (state changes), `slow 300ms` (screen/sheet transitions). Exit = 60–70% of enter (`exit-faster-than-enter`). Easing: decelerate on arrival, accelerate on leave (`easing`). List entrances stagger 30–50ms/item, first viewport only (`stagger-sequence`). Animate transform/opacity only (`transform-performance`, `layout-shift-avoid`). All animations interruptible and non-blocking (`interruptible`, `no-blocking-animation`). **Reduced motion:** read the system animator duration scale; at 0, crossfades replace slides and stagger is disabled (rule `reduced-motion`). Press feedback: M3 state layer/ripple, no layout-shifting scale on rows (pro-rules "Stable Interaction States"); 0.97 scale is allowed on media cards only.

---

## 3. Shared shell (applies to all 4 pages)

### 3.1 Bottom navigation

- M3 `NavigationBar`, **4 destinations — Home, Search, Library, Settings** (rule `bottom-nav-limit`: max 5, icons **and** text labels — `nav-label-icon`). Settings is promoted into the bar (it already is in the current build) instead of being hidden in a drawer (`nav-hierarchy`: all four are top-level).
- Selected state: Filled icon + `secondaryContainer` pill indicator + label in `onSurface`; unselected: Outlined icon, label `onSurfaceVariant` (rule `nav-state-active`). Container: `surface` with `outlineVariant` top divider.
- Labels: `labelMedium`, **maxLines = 1, softWrap = false** — at fontScale ≥1.6 the bar grows taller (never wraps, never truncates; pro-rules pre-delivery: verify at largest Dynamic Type).
- State behavior already correct in code and kept: `popUpTo(start) + saveState/restoreState` for the three content tabs (rule `state-preservation`, `back-stack-integrity`). Settings must use the **same** navigate pattern — currently it doesn't (see §9).
- Content inset: every scrollable page pads its end by nav + mini-player height (rule `fixed-element-offset`).

### 3.2 Mini-player (sacred — retained from current design)

Anchored directly above the nav bar, full width, `surfaceContainerHighest` (`#27273B`), tonal elevation 3, no shadow. Contents L→R: 48dp artwork (radius 8), title (`titleSmall`, 1 line ellipsis) + artist (`bodySmall`→use `bodyMedium` at fontScale >1.3), buffering spinner slot (fixed 24dp box so layout never shifts — rule `content-jumping`), prev / play-pause / next `IconButton`s (48dp targets). A 2dp progress hairline (primary) along its top edge. Tap anywhere (except buttons) opens the full player; swipe/back never stops playback. Visible on all 4 pages whenever something is loaded.

### 3.3 Full player (shared overlay)

Existing structure kept, restyled to tokens: top row = collapse chevron (contentDescription "Collapse player") + queue button; artwork = largest square fitting width − 48dp, radius 16, with the §2.5 glow ring when playing; title `headlineSmall` Poppins 600, max **2 lines** (no marquee — marquee is disabled above 130% font scale per the prior design rule, and this user's scale is above that); artist `bodyLarge` `onSurfaceVariant`; seek slider with tabular time labels; transport row: shuffle / prev / **64dp white-or-primary circular play (the screen's single primary CTA, `primary-action`)** / next / repeat, active modes tinted primary + TalkBack state description; secondary action row (favorite, download, lyrics, sleep timer) as Outlined icons with labels announced; speed + quality controls as `FlowRow` chip groups (§8 chip rule). Queue and lyrics remain a bottom sheet / inline expansion from here (sheet: `modal-escape` — drag handle + swipe-down dismiss + close affordance).

### 3.4 Cards, rows, chips

- **MediaCard** (albums/playlists/artists): artwork square radius 12, width 148dp; title `titleSmall` max 2 lines; subtitle `bodySmall` 1 line; whole card is one 48dp+ touch target with ripple; press scale 0.97 (cards only).
- **SongRow**: `ListItem`-based, 72dp min height (grows with font scale), 56dp artwork radius 8, title `titleMedium` 1-line ellipsis, supporting line artist • duration (tabular), optional trailing `IconButton` 48dp. A row is never split across "title wraps to 3 lines" — it grows vertically instead (§8).
- **Chips**: M3 `FilterChip`/`AssistChip`, label `labelMedium`, **labels never wrap and never shrink**: chip collections live in `FlowRow` (gap 8dp) and wrap the *collection* to the next line before any label is compressed — rule `chip-collection-reflow` verbatim: *"Wrap the collection before shrinking labels"*, plus `compact-label-overflow` (single-line labels, disclose truncation). Selected chip: check icon + `primaryContainer`.
- **Section header**: `titleLarge` Poppins 600, optional trailing "See all" `TextButton` (only when a destination exists).
- **Skeletons**: shape-matched shimmer blocks in `surfaceVariant` with reserved dimensions (rules `progressive-loading`, `content-jumping`); shimmer animation disabled under reduced motion (static blocks).
- **Empty / error blocks**: shared components with icon (48dp, `onSurfaceVariant`), `titleMedium` headline, `bodyMedium` explanation, and **one** action button — errors always include a recovery action (rule `error-recovery`: "clear next steps — Try again button + help link"; `error-feedback`: message near the problem, never silent).

---

## 4. Page: Home

**Purpose:** open straight into music (no login, no gate). **Layout (single `LazyColumn`, gutter 16dp):**

1. **Header block** (top, 24dp top padding below status inset): greeting in `displaySmall` Righteous — time-aware copy ("Good morning / afternoon / evening / listening"); subline `bodyMedium` `onSurfaceVariant`: "No account. Just music." A 40dp circular app-mark (primary container, Filled MusicNote) at the right. No search field here — Search is a nav destination (rule `search-accessible` is satisfied by the persistent bottom bar).
2. **Offline banner (conditional):** when connectivity is lost — full-width `secondaryContainer` strip, CloudOff icon + "You're offline — showing downloads & library" + TextButton "Downloads" → Library/Downloads tab. Offline is a mode, not an error screen (prior design rule; skill rule `offline-support`: "Provide offline state messaging and basic fallback").
3. **Jump back in** (only if history non-empty): section header "Jump back in" + horizontal `LazyRow` of MediaCards (148dp) from recent history.
4. **Trending songs**: section header + up to 10 `SongRow`s (tap = play queue from that index). 
5. **Albums / Playlists / Artists**: section header + `LazyRow` of MediaCards each (artists: circular artwork variant, radius full).
6. Bottom inset spacer (§3.1).

**States:**
- *Loading:* per-section skeletons (header bar + card/row placeholders at final dimensions). Sections resolve independently — one slow section never blocks the others.
- *Empty:* a section with zero items is **omitted entirely, header included** (current bug: headers render over empty space). If *all* sections are empty → full-page EmptyState "Nothing here yet / Check your connection and try again" + Retry.
- *Error (per section):* inline error row inside that section only (icon + short message + Retry `TextButton`), other sections keep working. Full-page ErrorState only when the first section fails *and* nothing else loaded. Error copy states cause + fix (rule `error-clarity`), e.g. "Couldn't reach the music service. Check your connection, then retry." — never a raw exception string.
- *Offline:* banner (2) + sections 3–5 replaced by Downloads/Favorites/History content from the local DB; remote-only sections hidden, not errored.

**Interactions:** pull-to-refresh on the whole column; card/row press feedback ≤100ms; stagger-in 30ms/item on first load only; tapping a song shows the mini-player appearing with a 200ms slide+fade from the bottom (`modal-motion` spatial logic: player rises from below).

**Changes vs current UI:** see §9. Headlines: greeting gets the display font; sections become independently-stateful; empty sections disappear; offline variant added.

---

## 5. Page: Search

**Layout (`Column`):**

1. **Search bar** (12dp inset): M3 `SearchBar` with `SearchBarInputField` (already migrated), leading Search icon, placeholder "Songs, albums, artists…", trailing clear (✕) button **only when query non-empty** (48dp, contentDescription "Clear search"). Visible label semantics come from the field itself; the input is the page's primary element and is focused state must never be obscured by the mini-player (rule `focus-not-obscured`).
2. **Idle state** (no query submitted): "Recent searches" section — recent queries as `AssistChip`s in a **`FlowRow`** (wraps at large font sizes; each chip has a trailing ✕ to remove that entry) + a "Clear all" `TextButton` aligned right. Below: "Try" suggestion chips (2–3 static starters). No tab row in idle state.
3. **Results state:** `TabRow` — Songs / Albums / Artists / Playlists (4 tabs, rules: labels `labelLarge`, **maxLines 1, no wrap** — already fixed for Search; at fontScale ≥1.6 switch to `ScrollableTabRow` so labels keep natural width). Tab content per type:
   - Songs → `LazyColumn` of SongRows (tap plays).
   - Albums / Playlists → 2-column `LazyVerticalGrid` of MediaCards (grid reads better than the current plain rows for artwork-led content; 16dp gaps).
   - Artists → rows with circular artwork.
4. Bottom inset spacer.

**States (per tab, retained across tab switches — rule `state-preservation`):**
- *Typing:* debounce 300ms before auto-search (rule `debounce-throttle`); explicit submit on IME search too.
- *Loading:* `ShimmerList` / grid skeleton.
- *No results (searched, zero hits):* dedicated EmptyState — SearchOff icon, "No results for '<query>'", "Check the spelling, or try another name." — **distinct from idle** (current code shows the idle "Search for music" copy for a zero-result search — wrong state, §9).
- *Error:* ErrorState with Retry (re-runs the same query).
- *Offline:* if a search is attempted offline → inline banner "You're offline — search needs a connection" + button to Library; cached recent chips still work.

**Interactions:** chip tap fills + submits immediately; long lists virtualized (`LazyColumn`, rule `virtualize-lists`); keyboard dismisses on scroll; back returns to Home with query + tab restored.

---

## 6. Page: Library

All-local content (Room): no network states except download progress. **Layout (`Column`):**

1. **Title**: "Library" `headlineMedium` Righteous, 16dp gutter.
2. **Segmented tab row**: Favorites / Downloads / History / Playlists — same `TabRow` spec as Search (**single-line labels; `ScrollableTabRow` at large font scale** — the current Library tabs still use bare `Text(t)` and will wrap exactly like the old Search bug). Optional count badge per tab (e.g. "Downloads · 12") — badges announce as a full phrase to TalkBack (rule `contextual-live-badge-updates`), never color-only.
3. **Sort row** (Favorites/Downloads/History): a single `FilterChip`-style button showing current sort with a Sort icon — "Newest first ▾" — opening a menu (Newest / Oldest / A–Z). Replaces the current ambiguous "Sort: Newest / Toggle" text pair (the word "Toggle" names no action — violates descriptive-label rules in pro-rules Interaction).
4. **Tab bodies:**
   - *Favorites:* SongRows; swipe-to-remove is **not** used (gesture-only removal violates `gesture-alternative`) — trailing heart `IconButton` unfavorites; removal shows a Snackbar with **Undo** (rule `undo-support`).
   - *Downloads:* rows with artwork, name, artist, and a status line: quality • size • status; in-progress items show a determinate `LinearProgressIndicator` + % (tabular); failed items show error icon + Retry button; trailing delete `IconButton` → **confirmation dialog** (rule `confirmation-dialogs`) → delete + Undo snackbar. A summary header line: "24 songs · 312 MB" (`bodyMedium`).
   - *History:* SongRows (tap replays), "Clear history" as an **overflow-menu item in the title row** (destructive, error color, separated — rule `destructive-emphasis`/`destructive-nav-separation`) with confirmation dialog.
   - *Playlists:* primary action = `FloatingActionButton` (Add icon, "New playlist") bottom-right above the mini-player — the page's one primary CTA (`primary-action`); playlist rows with song count; create/edit via the existing dialog, restyled: `OutlinedTextField` with visible label "Playlist name", error text below the field if blank on submit (rule `error-placement`), Create disabled until non-blank (rule `disabled-states` clarity).
5. **Empty states** (each with icon + headline + body + action): Favorites — "No favorites yet / Tap the heart on any song to keep it here." Downloads — "No downloads yet / Downloaded songs play offline, no account needed." + button "Find music" → Search. History — "Nothing played yet / What you play shows up here — only on this device." Playlists — "No playlists yet / Make one for a mood, a trip, anything." + button "New playlist".

**Offline:** Library *is* the offline home — everything here works with no connection; a small "On this device" caption under the title sets that expectation.

---

## 7. Page: Settings

Single scroll screen, **grouped cards** (rule `field-grouping`: related fields grouped visually; `progressive-disclosure`: advanced items collapsed). Structure:

1. **Title**: "Settings" `headlineMedium` Righteous.
2. **Appearance card** (`surface`, radius 16, 16dp padding):
   - Theme: `SingleChoiceSegmentedButtonRow` — **System / Dark / Light** (replaces the lone "Dark theme" switch; three-state matches the actual `darkTheme` boolean + system default and the `dark-mode-pairing` rule). Selected segment announces state to TalkBack.
   - "Dynamic / artwork colors" Switch row (kept) with supporting text "Tints surfaces from artwork colors".
   - Switch rows: full-row tap target ≥56dp tall, label `bodyLarge`, supporting `bodyMedium` `onSurfaceVariant`; switch itself is the state indicator + text label (never color alone, rule `color-not-only`).
3. **Playback card**: "Playback quality" label + helper "Higher quality uses more data." + quality `FilterChip`s in **`FlowRow`** (48/96/160/320 kbps — already fixed from Row→FlowRow; keep, with `labelMedium` single-line labels).
4. **Downloads card**: "Download quality" chips (same FlowRow spec) + helper "Downloads use the quality chosen here."
5. **API card** (advanced — inside an expandable section, collapsed by default is **not** used: it must stay one tap away because the app's upstream endpoint is user-configurable by design):
   - `OutlinedTextField` "API base URL" — **visible label above the field** in addition to the floating label (rule `input-labels`: never placeholder-only), helper text below with the current default + "Restart the app after changing" (kept from current copy), keyboard type = URI.
   - Inline validation on save: malformed URL → error text **below the field** in `error` color + error icon (rules `error-placement`, `inline-validation` — validate on submit/blur, not per keystroke).
   - "Save API URL" primary `Button` (full-width, 48dp) → on success, brief Snackbar "API URL saved — restart the app to apply" (rule `success-feedback`; toasts don't steal focus, `toast-accessibility`).
6. **About card**: the existing no-login statement, set in `bodyMedium`: "No login, no account, no tracking. Favorites, downloads, history and playlists live only on this device." + app version line.

**States:** settings persist via DataStore immediately on change (except API URL, which is save-button based); switches/chips reflect saved state on entry; no loading state needed beyond first read (show content skeleton if DataStore hasn't emitted).

---

## 8. Accessibility & robustness rules (acceptance criteria)

Distilled from `SKILL.md` priorities 1–2, `quick-reference.md` §1/§2/§5/§6, and the `pro-rules.md` Pre-Delivery Checklist. These are pass/fail for the redesign:

1. **Contrast:** normal text ≥ **4.5:1** in both themes; large text & meaningful icons ≥ 3:1; dark-theme body text targets **7:1** (OLED style checklist). All §2.1 pairs pre-verified; no new ad-hoc hex in screens (stack rule: semantic tokens only).
2. **Touch targets:** every tappable ≥ **48×48dp** (Android value, pro-rules — do not substitute the web 24px or iOS 44pt numbers); ≥8dp between adjacent targets; icon glyphs may be smaller only if the hit area is expanded.
3. **Font scaling (the headline risk for this user):**
   - Test at 0.85× / 1.0× / 1.3× / **2.0×** (pro-rules: "Verified behavior with Dynamic Type/largest system text size").
   - **No fixed-height containers for text**; rows, list items, nav bar, and settings rows grow vertically (rule `dynamic-type`: "avoid truncation as text grows").
   - **Chip collections wrap (FlowRow) before any label shrinks** (`chip-collection-reflow`); chip/tab/nav labels are single-line (`compact-label-overflow`) — a label may ellipsize only with its full text reachable (row tap / TalkBack label).
   - Tab rows switch to scrollable above fontScale 1.6 rather than compressing tabs.
   - Long tokens (URLs in the API field) wrap within the field; never overflow the card (rule `long-token-wrapping`, adapted: the field scrolls internally).
4. **Semantics:** icon-only buttons have descriptive `contentDescription`s and state descriptions where stateful (shuffle/repeat/favorite/tab selected); decorative icons beside visible text are excluded from the semantics tree; focus/reading order matches visual order (pro-rules Interaction; rule `voiceover-sr`). Errors are announced (live-region semantics on error blocks), not visual-only (rule from `--domain ux` "Error Messages": must be announced).
5. **No color-only meaning:** selected/offline/error/downloaded states always pair color with an icon or text (rule `color-not-only`).
6. **Motion:** reduced-motion respected (§2.7); no animation blocks input; press feedback within 100ms.
7. **Safe areas:** edge-to-edge with insets — no tappable content under status/gesture bars (rule `safe-area-awareness`; pro-rules Layout); scroll content never hidden behind nav + mini-player.
8. **Forms:** visible labels, helper text persisted under complex fields, inline errors under the field, submit gives loading→success/error feedback (rules `input-labels`, `input-helper-text`, `submit-feedback`).
9. **Navigation:** back is predictable and restores scroll/filter/input state; nav placement identical on all pages (`back-behavior`, `state-preservation`, `navigation-consistency`).

---

## 9. Changes vs current UI (developer checklist)

**Theme (`ui/theme/Theme.kt`)**
- Replace both color schemes with §2.1 tables (dark bg `#0B0F0E`→`#0F0F23`, surface `#121715`→`#12122B`, add `primaryContainer`, `secondaryContainer`, `outline`, `error`, `onSurfaceVariant` explicit values; light scheme completed to full role set — currently only 3 roles are set).
- Replace stock `Typography()` with §2.2 (Poppins body family, Righteous display/headline via downloadable fonts + fallback); add `tnum` feature for time labels.
- Add `OmegaSpacing`/`OmegaRadius` token objects (§2.3/§2.4) and use them instead of literal dp in screens.

**Shell (`MainActivity.kt`)**
- Settings nav item: use the same `popUpTo + saveState/restoreState` pattern as the other three tabs (currently plain `navigate`, breaks state preservation).
- Nav labels: `maxLines = 1, softWrap = false`; bar container styling per §3.1.
- Mini-player: add top progress hairline + fixed-size buffering slot (§3.2).

**Components (`ui/components/Components.kt`)**
- `ErrorState`/`EmptyState`: add leading icon slot + optional action button (currently ErrorState has Retry but EmptyState has no action); error copy becomes cause+fix, raw exception text no longer displayed.
- `ShimmerList`: add card-grid skeleton variant for Search grids/Home rails; respect reduced motion.
- `Artwork`: radius becomes a token parameter (8/12/16/full variants), add `contentDescription` param (song/album name) for meaningful artwork.
- `MediaCard` width 150→148dp, title style → `titleSmall` Poppins 500, add press-scale.
- All icon-only buttons across the app: real `contentDescription`s (currently `null` everywhere).

**Home (`Screens.kt` → HomeScreen)**
- Greeting uses `displaySmall` (Righteous); add app-mark; add conditional offline banner + offline content variant.
- Sections become independently stateful; **hide a section (header included) when its data is empty**; per-section inline error instead of one section's error occupying the list; full-page error only when nothing loaded.

**Search (`Screens.kt` → SearchScreen)**
- Recent-search chips: `LazyRow` → `FlowRow` with per-chip remove (✕) (crush risk at large font, same class of bug as Settings chips).
- Add distinct **no-results** state (currently a zero-result search shows the idle "Search for music" state).
- Albums/Playlists tabs: rows → 2-column card grid; Artists: circular artwork.
- Tabs: keep single-line fix; switch to `ScrollableTabRow` at fontScale ≥1.6; add clear-query button in the search field; add offline attempt banner.

**Library (`LibrarySettingsScreens.kt` → LibraryScreen)**
- Tab labels get the single-line treatment Search already has (currently bare `Text(t)` — same wrap bug pending) + optional count badges.
- Replace "Sort: … / Toggle" with a labeled sort chip + menu.
- Downloads rows: add progress bar + % for active, Retry for failed, storage summary header; delete → confirm dialog + Undo snackbar (currently instant delete, no confirm/undo).
- History "Clear history" moves to title-row overflow menu with confirmation.
- Playlists: "New local playlist" button → FAB; create dialog gets blank-name validation + disabled Create.
- All four empty states gain their §6 action buttons.

**Settings (`LibrarySettingsScreens.kt` → SettingsScreen)**
- Flat 16dp column → grouped cards: Appearance / Playback / Downloads / API / About (§7).
- "Dark theme" Switch → System/Dark/Light segmented control (the current switch also can't express "follow system", which `SaavnTheme` defaults to).
- API URL field: persistent label + helper, URI keyboard, inline validation error under field, success Snackbar; **update the stale helper copy** (it still names the old default endpoint).
- Quality chips: keep the `FlowRow` fix; add helper lines; labels → `labelMedium` tokens.
- Section labels ("Playback quality" etc.) get `titleMedium` styling + card grouping instead of bare `Text`.

**Player (`ui/player/PlayerUi.kt`)** — consistency pass only (not a redesign target)
- Token radii/spacing; play button = `primary` with `onPrimary` glyph (currently `FilledIconButton` default colors); tabular time labels; speed chips already FlowRow — align gap to 8dp token; queue sheet gets drag handle + "Up next" header style per §3.3.

---

## 10. Pre-delivery checklist (from `pro-rules.md`, adapted)

- [ ] No emoji icons; one icon family (Material), one style per hierarchy level; icon sizes from tokens
- [ ] All touch targets ≥48dp; ≥8dp gaps; press feedback ≤100ms, no layout-shifting press states
- [ ] Text contrast ≥4.5:1 verified **separately** in dark and light; dark body text ≈7:1
- [ ] Tested at fontScale 0.85×/1.0×/1.3×/2.0× — no crushed chips, no wrapped tab/nav labels, no clipped text
- [ ] Reduced-motion on: no stagger/shimmer/slide, crossfades only
- [ ] Safe areas respected; no list content hidden behind nav + mini-player
- [ ] TalkBack pass: reading order = visual order; every icon-only control named; states announced
- [ ] Every error has a recovery action; every empty state has a next step; offline is a mode, not a dead end
