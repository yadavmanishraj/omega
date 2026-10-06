# Omega — Modularization Plan (Now in Android style)

| Field | Value |
|---|---|
| Product | **Omega** — JioSaavn music client, repo `yadavmanishraj/omega` |
| Current state | Single `:app` module, 20 Kotlin files, package `com.manishraj.saavnmusic` |
| Reference studied | `android/nowinandroid` cloned at `~/workspace/sdlc/architecture/nowinandroid` (depth-1, 2026-10-07) — `docs/ModularizationLearningJourney.md`, `docs/ArchitectureLearningJourney.md`, `settings.gradle.kts`, every module's `build.gradle.kts`, `build-logic/convention/` |
| Companion docs | `APP_DESIGN.md` (same folder) — architecture/data/playback decisions this plan must not contradict |
| Hard constraints (unchanged) | No login/accounts/tracking (so **no analytics module, ever**); Kotlin + Compose + Material 3; minSdk 26 / target 35 / JDK 17 / Kotlin 2.0.21 / AGP 8.7.3; builds are verified on Manish's Windows laptop (Gradle daemon cannot run in the agent sandbox) — every migration step below must leave the app compilable |

---

## 1. How Now in Android modularizes — principles (grounded in the clone)

### 1.1 Module types

From `docs/ModularizationLearningJourney.md` and `settings.gradle.kts` (lines 33–67):

- **`:app`** — the only `android-application` module. Contains "app level and scaffolding classes that bind the rest of the codebase" (`MainActivity`, `NiaApplication`, `NiaApp`, `NiaNavHost`, `TopLevelDestination`/`TopLevelNavItem`). It "depends on all `feature` modules and required `core` modules" (`app/build.gradle.kts` lists every `feature:*:api` + `feature:*:impl` plus `core:common`, `core:ui`, `core:designsystem`, `core:data`, `core:model`).
- **Feature modules** — one responsibility / one user journey each (ForYou, Search, Bookmarks, Topic, Interests, Settings). In current NiA each feature is **split into two Gradle modules**:
  - `feature:<name>:api` — *only* navigation keys (Navigation 3 `NavKey`s, e.g. `feature/topic/api/.../TopicNavKey.kt`) plus a `Navigator.navigateToX()` extension. Applies `AndroidFeatureApiConventionPlugin`, which adds `api(project(":core:navigation"))` and the serialization plugin (keys are `@Serializable`).
  - `feature:<name>:impl` — everything else: screens, ViewModels, feature-local logic. Applies `AndroidFeatureImplConventionPlugin`, which pre-wires `implementation` deps on `:core:ui` and `:core:designsystem` plus lifecycle/navigation3 artifacts.
  - **Rules (verbatim intent from the doc):** "A feature's `api` module should not depend on another feature's `api` or `impl` module. A feature's `impl` should only depend on another feature's `api` module. Both submodules should only depend on the `core` modules that they require." Features **never** depend on another feature's `impl`. Cross-feature navigation happens by calling the target feature's `api` navigation functions; the actual NavHost assembly lives in `:app` (current NiA uses Navigation 3: `core:navigation` holds `NavigationState`/`Navigator`, `app/.../ui/NiaApp.kt` builds the NavDisplay).
  - Settings is the exception that proves the sizing rule: `feature:settings` has **only an `impl`** module (nothing navigates *into* settings with arguments, and nothing needs its key type-safe across features) — see `settings.gradle.kts` line 62.
- **Core modules** — "common library modules containing auxiliary code and specific dependencies that need to be shared." Rule: "These modules can depend on other core modules, but they shouldn't depend on feature nor app modules." Core is a DAG with `:core:model` at the bottom.
- **Miscellaneous** — `:sync:work` (WorkManager background sync), `:benchmarks` (baseline-profile/test module), `:lint` (custom lint checks, `java-library`), `:app-nia-catalog` (design-system showcase app), `:ui-test-hilt-manifest`, and test-support modules (`:core:testing`, `:core:data-test`, `:core:datastore-test`, `:sync:sync-test`, `:core:screenshot-testing`).

### 1.2 The dependency rules that matter

1. **Direction is one-way:** `app → feature → core`. Nothing in core knows features exist; nothing in a feature knows `:app` exists.
2. **`implementation` between features, `api` only where types leak.** Feature→feature edges are `implementation(projects.feature.topic.api)`. Inside core, `:core:data` uses `api(...)` for `core:common`, `core:database`, `core:datastore`, `core:network` (its public repository signatures expose their types transitively), while `:core:database`/`:core:network` use `api(projects.core.model)` because model types are their return types (`core/database/build.gradle.kts`, `core/network/build.gradle.kts`, `core/data/build.gradle.kts`).
3. **Models live in a pure JVM module.** `:core:model` applies only `JvmLibraryConventionPlugin` (no Android plugin at all) and depends only on `kotlinx-datetime`. Everything else can depend on it for free; it can be unit-tested and reused anywhere.
4. **Data layer is the single source of truth, offline-first** (`docs/ArchitectureLearningJourney.md` §"Data layer", lines 166–252): repositories in `:core:data` expose **streams** (`Flow`), never snapshots; writes are `suspend` functions; each repository orchestrates one or more data sources (`OfflineFirstTopicsRepository` ← `:core:network` + `:core:database` + `:core:datastore`). Background refresh is a separate concern: `:sync:work` depends on `:core:data` and calls `SyncManager.requestSync()` from WorkManager — features never sync.
5. **Hilt bindings live with the code they bind.** Each core module owns a `di/` package: `core/network/.../di/NetworkModule.kt`, `core/database/.../di/`, `core/datastore/.../di/`, `core/data/.../di/DataModule.kt` (+ per-repository `@Binds` modules), `core/common/.../network/di/` (dispatcher qualifiers). There is **no god `AppModule`**; `:app` contributes only app-specific bindings (`app/.../di/JankStatsModule.kt`). The `HiltConventionPlugin` applies KSP + `hilt-compiler` and adds `hilt-android` to Android modules / `hilt-core` to JVM modules, so individual build files never mention Hilt.
6. **Per-module build files are 5–15 lines** because all shared configuration lives in **build-logic convention plugins** (`build-logic/convention/src/main/kotlin/`): `AndroidLibraryConventionPlugin` (applies `com.android.library` + lint plugin, `configureKotlinAndroid`, flavors, managed devices, test defaults, and a **resource prefix derived from the module path** — `:core:module1` → `core_module1_`), `AndroidLibraryComposeConventionPlugin`, `AndroidRoomConventionPlugin`, `AndroidFeatureApi/ImplConventionPlugin`, `HiltConventionPlugin`, `JvmLibraryConventionPlugin`, `AndroidLintConventionPlugin`, `AndroidTestConventionPlugin`, jacoco variants, etc. Plugins are registered by id in `build-logic/convention/build.gradle.kts` and consumed via the version catalog as `libs.plugins.nowinandroid.*`.
7. **One version catalog** (`gradle/libs.versions.toml`) for the whole build, consumed identically by app modules *and* by build-logic; typesafe project accessors are enabled (`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` in `settings.gradle.kts`), so dependencies are written `projects.core.data`, not `project(":core:data")`.
8. **Every module carries a README with its dependency graph**, regenerated by CI (`graphUpdate` task) — modularization is treated as documentation, not folklore.
9. **Right-size, don't cargo-cult.** The doc's own warning (§"Further considerations"): NiA deliberately balanced "overmodularizing a relatively small app" against showcasing a pattern for much larger codebases, and granularity should grow with the code ("If your data layer is small, it's fine to keep it in a single module"). This plan follows that advice literally — see §6 for what Omega intentionally does *not* copy.

### 1.3 NiA module inventory (from `settings.gradle.kts` + build files)

| Module | Plugin type | Purpose | Key project dependencies |
|---|---|---|---|
| `:app` | android-application | Scaffolding, `MainActivity`, NavHost assembly, top-level destinations | all `feature:*:api`+`impl`, `core:common/ui/designsystem/data/model`, `sync:work` |
| `:core:model` | **kotlin-jvm** | App-wide model classes (`Topic`, `NewsResource`, `UserData`) | — (only `kotlinx-datetime`) |
| `:core:common` | **kotlin-jvm** (+Hilt plugin) | `Result`, dispatcher qualifiers (`NiaDispatchers`), utilities | — |
| `:core:navigation` | android-library | Navigation 3 state/`Navigator` shared by feature `api` modules | — |
| `:core:network` | android-library | Retrofit API (`RetrofitNiaNetworkApi`), network DTOs, `NetworkModule` DI, `BuildConfig.BACKEND_URL` | `api core:model`, `api core:common` |
| `:core:database` | android-library (+Room plugin) | Room DB, DAOs, entities, migrations | `api core:model` |
| `:core:datastore` | android-library | Proto DataStore preferences (`NiaPreferences`), serializers | `api core:datastore-proto`, `api core:model`, `impl core:common` |
| `:core:datastore-proto` | kotlin-jvm (protobuf) | `.proto` schemas for DataStore | — |
| `:core:data` | android-library | Repositories (offline-first), `DataModule` `@Binds` | `api core:common/database/datastore/network`, `impl core:analytics/notifications` |
| `:core:domain` | android-library | Use cases shared by ≥2 features (`GetUserNewsResourcesUseCase`…) | `api core:data`, `api core:model` |
| `:core:designsystem` | android-library (Compose) | Theme, icons (`NiaIcons`), Material 3 component wrappers | — (+ `lintPublish :lint`) |
| `:core:ui` | android-library (Compose) | Composite, model-aware components (`NewsFeed`, cards) | `api core:designsystem`, `api core:model`, `api core:analytics` |
| `:core:analytics` | android-library | Analytics abstraction (Firebase in prod flavor) | — |
| `:core:notifications` | android-library | Notification abstraction + posting | `api core:model`, `impl core:common` |
| `:core:testing` | android-library | `NiaTestRunner`, rules, fake repositories for tests | `api core:data/model/common/analytics/notifications` |
| `:core:data-test` / `:core:datastore-test` / `:core:screenshot-testing` | android-library | Test doubles & screenshot-test harness | respective core modules |
| `:feature:foryou:api` / `:impl` | android-library (feature plugins) | News feed + onboarding | impl → `core:domain`, `core:notifications`, `feature:topic:api`, own `api` |
| `:feature:search:api` / `:impl` | android-library | Search UI + recent searches | impl → `core:domain`, `feature:interests:api`, `feature:topic:api`, own `api` |
| `:feature:interests:api` / `:impl` | android-library | Topics list | impl → `core:domain`, `feature:topic:api`, own `api` |
| `:feature:topic:api` / `:impl` | android-library | Topic detail (navigated to from many features) | impl → `core:domain`, own `api` |
| `:feature:bookmarks:api` / `:impl` | android-library | Saved news | impl → `core:domain`, `feature:topic:api`, own `api` |
| `:feature:settings:impl` | android-library | Settings dialog/screen (no `api` split) | impl → `core:data` |
| `:sync:work` | android-library | WorkManager sync workers, FCM | `core:data`, `core:analytics`, `core:notifications` |
| `:sync:sync-test` | android-library | Sync test doubles | `sync:work` |
| `:benchmarks` | android-test | Macrobenchmark + baseline profile generation for `:app` | target project `:app` |
| `:lint` | java-library | Custom lint checks, published into designsystem via `lintPublish` | — |
| `:app-nia-catalog` | android-application | Design-system showcase app | `core:designsystem`, `core:ui` |
| `:ui-test-hilt-manifest` | android-library | Hilt test manifest shim | — |

Total: 33 included projects. Omega needs roughly half of these — see §3 and §6.

---

## 2. Current Omega structure (what is being split)

Single `:app` module (`settings.gradle.kts` includes only `:app`), namespace/applicationId `com.manishraj.saavnmusic`, dependencies all declared in `app/build.gradle.kts` (Compose BOM, Navigation Compose 2.8.5, Hilt 2.52, Retrofit/OkHttp, Room 2.6.1, Media3 1.5.1, Coil, WorkManager + Hilt-Work, Preferences DataStore, kotlinx-serialization — versions already centralized in `gradle/libs.versions.toml`, which NiA-style build-logic can reuse as-is).

| Current file(s) | Contents today |
|---|---|
| `MainActivity.kt`, `SaavnApplication.kt` | Single activity + NavHost/bottom nav; `@HiltAndroidApp` app class (also WorkManager `Configuration.Provider` via Hilt worker factory) |
| `domain/Models.kt` | `Song`, `Album`, `Playlist`, `Artist`, `UiState<T>` |
| `data/remote/SaavnApi.kt`, `data/remote/dto/Dtos.kt` | Retrofit service for the JioSaavn wrapper API + DTOs/mappers |
| `data/local/Entities.kt` | Room entities (`Favorite`, `Download`, `History`, `RecentSearch`, `LocalPlaylist`, `LocalPlaylistSong`), DAO, `AppDatabase` |
| `data/repository/MusicRepository.kt` | The one repository: remote + local orchestration |
| `data/settings/SettingsRepository.kt` | Preferences DataStore (base URL, qualities, theme…) |
| `di/AppModule.kt` | **One god module**: provides `Json`, `OkHttpClient`, Retrofit API, Room DB + DAO, `WorkManager` |
| `playback/PlaybackService.kt`, `playback/PlayerController.kt` | Media3 `MediaSessionService` (declared in the app manifest) + `@Singleton PlayerController`, `PlayerState` |
| `download/DownloadWorker.kt` | Hilt `CoroutineWorker` for song/album/playlist downloads |
| `ui/theme/Theme.kt` | Material 3 theme + design tokens ("record store at midnight" palette) |
| `ui/components/Components.kt` | Shared composables (media rows/cards, section headers, state views) |
| `ui/screens/Screens.kt` | Home + Search screens |
| `ui/screens/LibrarySettingsScreens.kt` | Library + Settings screens |
| `ui/player/PlayerUi.kt` | Mini-player + full player + queue/lyrics UI |
| `ui/viewmodel/ViewModels.kt` | `HomeViewModel`, `SearchViewModel`, `DetailViewModel`, `LibraryViewModel`, `PlayerViewModel`, `SettingsViewModel` — all in one file |

---

## 3. Proposed Omega module graph (NiA-shaped, right-sized)

16 projects instead of NiA's 33. Differences from NiA, each justified in §6: no `api`/`impl` feature split yet, no `:core:domain`, no analytics/notifications/testing/benchmark/lint/catalog modules, no proto split, and two music-specific core modules NiA doesn't have (`:core:playback`, `:core:download` — the latter is Omega's analogue of NiA's `:sync:work`).

### 3.1 Module-by-module

| Module | Type | Contents (existing files that move here) | Allowed deps | Forbidden |
|---|---|---|---|---|
| `:app` | android-application | `MainActivity.kt`, `SaavnApplication.kt`, NavHost + bottom-nav assembly, top-level destination definitions, deep-link handling, app-only DI (e.g. WorkManager config glue if not in `:core:download`) | **all** `:feature:*`, `:core:ui`, `:core:designsystem`, `:core:data`, `:core:model`, `:core:playback` | business logic, repositories, DTOs |
| `:core:model` | **kotlin-jvm** | `domain/Models.kt` → `Song`, `Album`, `Playlist`, `Artist` (pure data classes) | kotlinx-serialization/datetime only if needed | Android APIs, Room/Retrofit annotations, any project dep |
| `:core:common` | **kotlin-jvm** (+Hilt convention for JVM) | `UiState<T>` (moved out of Models.kt), `Result`-style helpers, dispatcher qualifiers + module (`@DispatcherIO` etc.), small pure utilities | coroutines-core | Android framework, Compose, other core modules |
| `:core:network` | android-library | `data/remote/SaavnApi.kt`, `data/remote/dto/Dtos.kt` (DTOs + `toDomain()` mappers stay beside the API that produces them, as NiA keeps network models in `:core:network`), `NetworkModule` (provides `Json`, `OkHttpClient`, Retrofit) split out of `AppModule` | `api :core:model`, `api :core:common` | Room, DataStore, `:core:data`, features |
| `:core:database` | android-library (+Room convention) | `data/local/Entities.kt` (entities + DAO + `AppDatabase`), `DatabaseModule` (provides DB + DAO) split out of `AppModule`; exported Room schemas live in `core/database/schemas/` | `api :core:model`, `impl :core:common` | network, DataStore, features |
| `:core:datastore` | android-library | `data/settings/SettingsRepository.kt`, `DataStoreModule` (provides `DataStore<Preferences>` + settings repo) | `api :core:model`, `impl :core:common` | Room, network, features |
| `:core:data` | android-library | `data/repository/MusicRepository.kt` (split into interface + `OfflineFirst`-style impl if it grows; today one class is fine), `DataModule` (`@Binds` repository, misc providers incl. `WorkManager` if kept out of download) | `api :core:model`, `api :core:common`, `api :core:network`, `api :core:database`, `api :core:datastore` | Compose/UI, Media3, features, `:app` |
| `:core:designsystem` | android-library (Compose) | `ui/theme/Theme.kt` (color/type/shape tokens, `OmegaTheme`), icon set object if introduced | Compose BOM/material3 only | `:core:model`, data layer, features |
| `:core:ui` | android-library (Compose) | `ui/components/Components.kt` — model-aware shared composables: media row/card, section header, loading/error/empty states, Coil image helpers | `api :core:designsystem`, `api :core:model` | repositories/ViewModels (components take lambdas + models only, exactly like NiA's `core:ui`) |
| `:core:playback` | android-library | `playback/PlayerController.kt`, `playback/PlayerState`, `playback/PlaybackService.kt` (**its `<service>` manifest entry moves into this module's manifest** and merges into the app), `PlaybackModule` (provides `ExoPlayer`, `MediaSession`) | `:core:model`, `:core:common`, `:core:data` (for history recording / URL refresh-on-403), Media3 | feature modules, Compose UI (player *UI* is a feature) |
| `:core:download` | android-library | `download/DownloadWorker.kt`, download enqueue API (`DownloadManager`-style facade over WorkManager), `DownloadModule` (`HiltWorkerFactory` plumbing) — Omega's `:sync:work` analogue | `:core:model`, `:core:common`, `:core:data`, WorkManager + Hilt-Work | features, Compose |
| `:feature:home` | android-library (feature convention) | `HomeScreen` (from `Screens.kt`), `HomeViewModel` (from `ViewModels.kt`), home-local components | `:core:data` (or `:core:domain` later), `:core:ui`, `:core:designsystem`, `:core:model`, `:core:common`, `:core:playback` (play actions) | other features' code, `:core:network`/`:core:database` directly |
| `:feature:search` | android-library (feature) | `SearchScreen`, `SearchViewModel`, recent-search UI | same core set as home | other features, network/database directly |
| `:feature:library` | android-library (feature) | `LibraryScreen` (favourites/playlists/history/downloads tabs), `LibraryViewModel` | core set + `:core:download` (download actions/state) | other features, network/database directly |
| `:feature:settings` | android-library (feature) | `SettingsScreen`, `SettingsViewModel` | `:core:data`, `:core:datastore` (via data or directly — NiA's settings impl depends on `core:data` only; follow that), `:core:ui`, `:core:designsystem`, `:core:model` | other features |
| `:feature:player` | android-library (feature) | `ui/player/PlayerUi.kt` — `MiniPlayer`, full player, queue sheet, lyrics view; `PlayerViewModel` | `:core:playback`, `:core:data`, `:core:ui`, `:core:designsystem`, `:core:model`, `:core:common` | other features, Media3 service internals |
| `:feature:detail` | android-library (feature) | Album/Playlist/Artist detail screens (from `Screens.kt`/`LibrarySettingsScreens.kt`), `DetailViewModel` | same core set as home | other features |

Why one `:feature:detail` instead of NiA-style `:feature:album`, `:feature:playlist`, `:feature:artist`: the three detail screens share one ViewModel (`DetailViewModel`), one navigation argument pattern (an id), and one track-list UI today. NiA splits by *user journey*, and these three are the same journey ("open a collection/entity, play its songs"). Split into three features the day one of them grows its own sub-flows (e.g. artist follow/bio pages) — the boundary is pre-drawn by keeping three packages inside the module (`detail/album/`, `detail/playlist/`, `detail/artist/`).

Why features may depend on `:core:playback` / `:core:download` directly: NiA features depend on whichever core modules they genuinely need (`feature:foryou:impl` → `core:notifications`). Play/download are *capability* core modules, not data sources, so this stays inside NiA's rules.

### 3.2 Dependency diagram

```
                              ┌─────────────────────────────┐
                              │            :app             │
                              │  MainActivity · NavHost     │
                              │  SaavnApplication · scaffold│
                              └──────┬──────────────────────┘
        ┌──────────────┬───────────┼────────────┬──────────────┬───────────────┐
        ▼              ▼           ▼            ▼              ▼               ▼
 :feature:home  :feature:search :feature:library :feature:settings :feature:player :feature:detail
        └──────────────┴──────┬────┴──────┬───────┴──────────────┴───────────────┘
                              ▼           ▼
                       :core:ui    :core:playback   :core:download
                              │           │               │
                              ▼           └───────┬───────┘
                      :core:designsystem        ▼
                              │           :core:data
                              │          ╱    │     ╲        (api deps)
                              │         ▼     ▼      ▼
                              │   :core:network :core:database :core:datastore
                              │         ╲     │      ╱
                              ▼          ▼    ▼     ▼
                        (Compose only)   :core:model      :core:common
                                          (kotlin-jvm)     (kotlin-jvm)

Rules: arrows point "depends on". Features → core only, never feature → feature.
Core never depends on feature or :app. :core:model / :core:common depend on nothing.
```

### 3.3 Cross-feature navigation without feature→feature deps

Omega currently uses Navigation Compose 2.8.5 with routes assembled centrally (APP_DESIGN §7 / decision D-2 already flags a possible Navigation 3 migration). NiA's current answer is the `api`/`impl` split with `NavKey`s. **Right-sized Omega answer for now:** keep all route definitions and the NavHost in `:app`; each feature exposes only its screen composables + ViewModels, and receives navigation as **lambdas** (`onAlbumClick: (String) -> Unit`, `onArtistClick`, `onPlaylistClick`) injected by `:app` — the pattern Omega's screens already use. This gives the same compile-time guarantee (no feature→feature edges) with zero extra modules. The `api`/`impl` split becomes a mechanical refactor later *if* Navigation 3 typed keys are adopted, because each feature's navigation surface is already isolated in its `navigation/`-adjacent lambdas and route constants can move into a new `feature:x:api` module unchanged in spirit. (See §6.)

### 3.4 Hilt organization (mirroring NiA §1.2 rule 5)

| Module | Owns |
|---|---|
| `:core:common` | `DispatchersModule` (qualifier annotations + providers) |
| `:core:network` | `NetworkModule` — `Json`, `OkHttpClient`, Retrofit `SaavnApi` (base URL read from settings at provision time as today) |
| `:core:database` | `DatabaseModule` — `AppDatabase`, `LibraryDao` |
| `:core:datastore` | `DataStoreModule` — `DataStore<Preferences>`, `SettingsRepository` |
| `:core:data` | `DataModule` — `@Binds MusicRepository`, any cross-source providers |
| `:core:playback` | `PlaybackModule` — `ExoPlayer`, `MediaSession`, `PlayerController` binding |
| `:core:download` | `DownloadModule` — WorkManager/Hilt worker factory pieces |
| `:app` | `AppModule` **shrinks to nothing or app-only bindings**; `SaavnApplication` stays here |

Feature ViewModels keep `@HiltViewModel` and need no modules. Rule going forward: *a binding is declared in the module that owns the implementation* — never re-centralize.

### 3.5 Package & namespace strategy

- **Packages do not change.** `com.manishraj.saavnmusic.data.remote` code physically moves into `:core:network` but keeps its package; same for `data.local` → `:core:database`, `domain` → `:core:model`, `ui.theme` → `:core:designsystem`, etc. Feature packages become `com.manishraj.saavnmusic.feature.home` *only if* a file is being split anyway (e.g. `ViewModels.kt` → one file per feature); otherwise keeping `ui.screens`/`ui.viewmodel` packages inside feature modules is acceptable for step 1 and can be renamed mechanically later. Zero import churn is what keeps each migration step green.
- **Namespace per module = its dominant existing package**, e.g. `:core:network` → `namespace = "com.manishraj.saavnmusic.data.remote"`, `:core:database` → `"…data.local"`, `:core:model` → `"…domain"`, features → `"…ui"` is too broad; use `"com.manishraj.saavnmusic.feature.home"` etc. for the new feature packages. Namespaces only affect `R`/`BuildConfig`/manifest merging, and Omega libraries ship almost no XML resources.
- Adopt NiA's **resource-prefix-from-module-path** convention in the library convention plugin anyway (`core_network_`, `feature_home_`…) so any future XML/string resources are collision-proof from day one.
- **Typesafe project accessors** on (`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` in `settings.gradle.kts`), like NiA, once there is more than one module.

---

## 4. Build-logic convention plugins (mirror of NiA's `build-logic/`)

Create an included build `build-logic/` (registered in root `settings.gradle.kts` via `pluginManagement { includeBuild("build-logic") }`, exactly as NiA's `settings.gradle.kts` line 18). Plugins live in `build-logic/convention/src/main/kotlin/`, are declared in `build-logic/convention/build.gradle.kts` `gradlePlugin { }` block, and are exposed to modules through the root version catalog as `libs.plugins.omega.*` — the same mechanism NiA uses for `libs.plugins.nowinandroid.*`.

| Omega plugin id | Mirrors NiA | Applies / configures |
|---|---|---|
| `omega.android.library` | `AndroidLibraryConventionPlugin` | `com.android.library`, `org.jetbrains.kotlin.android`; `configureKotlinAndroid` equivalent: compileSdk 35, minSdk 26, JVM 17, test runner, `resourcePrefix` derived from module path, lint defaults |
| `omega.android.library.compose` | `AndroidLibraryComposeConventionPlugin` | kotlin-compose plugin, `buildFeatures.compose = true`, Compose BOM `api`/implementation wiring |
| `omega.android.application` | `AndroidApplicationConventionPlugin` | `com.android.application`, targetSdk 35, version defaults |
| `omega.android.application.compose` | `AndroidApplicationComposeConventionPlugin` | compose for `:app` |
| `omega.android.feature` | `AndroidFeatureImplConventionPlugin` | `omega.android.library` + `omega.hilt` + pre-wired `implementation` deps: `:core:ui`, `:core:designsystem`, `:core:model`, lifecycle-viewmodel-compose, hilt-navigation-compose, navigation-compose |
| `omega.hilt` | `HiltConventionPlugin` | KSP + `hilt-compiler`; `hilt-android` on Android modules, `hilt-core` on JVM modules (copy NiA's `withPlugin` trick verbatim in spirit) |
| `omega.jvm.library` | `JvmLibraryConventionPlugin` | `org.jetbrains.kotlin.jvm`, JVM 17 — for `:core:model`, `:core:common` |
| `omega.android.room` | `AndroidRoomConventionPlugin` | Room compiler via KSP, `room-runtime`/`room-ktx`, schema export directory arg (`$projectDir/schemas`) |

Deliberately **not** copied yet: jacoco plugins, flavors plugin (Omega has no demo/prod flavors — APP_DESIGN uses a settings-level base URL instead), Firebase plugin, lint convention beyond AGP defaults, screenshot-testing/roborazzi wiring, managed-devices config. Each can be added as a plugin later without touching module build files — that is the point of the convention-plugin layer.

**Resulting module build files** (the NiA payoff — compare NiA's 6-line `core/model/build.gradle.kts`):

```kotlin
// core/network/build.gradle.kts
plugins {
    alias(libs.plugins.omega.android.library)
    alias(libs.plugins.omega.hilt)
    alias(libs.plugins.kotlin.serialization)
}
android { namespace = "com.manishraj.saavnmusic.data.remote" }
dependencies {
    api(projects.core.model)
    api(projects.core.common)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
}
```

**Version-catalog notes:** keep the existing `gradle/libs.versions.toml` untouched (AGP 8.7.3, Kotlin 2.0.21, Hilt 2.52, Room 2.6.1, Media3 1.5.1… — APP_DESIGN NFR-10 says keep, don't churn). Only *additions*: the eight `omega.*` plugin entries under `[plugins]` pointing at build-logic (NiA declares them in the same catalog), and a `[libraries]` entry for `hilt-core` (needed by `omega.hilt` on JVM modules). build-logic itself gets its own minimal `settings.gradle.kts` + catalog reuse via `VersionCatalogsExtension` (`libs` accessor), as NiA's `build-logic` does.

---

## 5. Migration sequence (compilable at every step)

Golden rules for the whole migration: **one module per commit; move files with `git mv`; never change a package and its module in the same commit; run `testDebugUnitTest assembleDebug` on the laptop after every step** (the established verify loop). Because packages don't change (§3.5), most steps are: create module skeleton → `git mv` files → trim `app/build.gradle.kts` deps → build.

| Step | Action | Why this order / notes |
|---|---|---|
| 0 | **build-logic first.** Create `build-logic/` with `omega.jvm.library`, `omega.android.library`, `omega.hilt` (minimum set), register in settings + catalog, but apply to nothing yet. Build `:app` unchanged to prove build-logic compiles. | Convention plugins are the riskiest new machinery; land them while the app is still monolithic so failures are attributable. |
| 1 | **`:core:model`** (kotlin-jvm). Move `domain/Models.kt`; split `UiState` out to step 2 (leave it in Models.kt temporarily if splitting complicates — but do split: `UiState` is a UI/common concern, models must stay pure). `:app` gets `implementation(projects.core.model)`. | Zero Android deps → cannot break resources/manifest; validates typesafe accessors + jvm plugin. |
| 2 | **`:core:common`** (kotlin-jvm + `omega.hilt`). Move `UiState`, add dispatcher qualifiers/module (repository/controller currently use default dispatchers — introduce `@DispatcherIO` here and adopt incrementally). | Still pure JVM; unblocks every later module's `api(:core:common)`. |
| 3 | **`:core:designsystem`**. Move `ui/theme/Theme.kt`; apply library+compose+hilt-free plugins. `:app` depends on it. | Leaf UI module, no data deps; proves the compose convention plugin. Watch for theme referencing app-level code (it must not). |
| 4 | **`:core:network`**. Move `data/remote/**`; carve `NetworkModule` out of `di/AppModule.kt` into `data.remote.di` package inside the module. | First Hilt split; verify the app still gets the same Retrofit singleton (one `@Module` moved, graph shape unchanged). |
| 5 | **`:core:database`** (+ `omega.android.room`). Move `data/local/Entities.kt`; carve `DatabaseModule` out of `AppModule`; **move the Room schema export dir with it** (`core/database/schemas/`) and update the KSP `room.schemaLocation` arg in the room convention plugin. | ⚠ Room risk: schema JSON files are the migration contract — if the schema location changes silently, Room regenerates schema v1 and future migrations break. Diff the exported schema before/after; it must be byte-identical. DB name/file unchanged → existing installs keep their library. |
| 6 | **`:core:datastore`**. Move `data/settings/SettingsRepository.kt` + `DataStoreModule`. DataStore file name unchanged. | Preferences DataStore (not Proto) → no `datastore-proto` module needed. |
| 7 | **`:core:data`**. Move `data/repository/MusicRepository.kt`; add `DataModule` (`@Binds`). Delete the now-empty `di/AppModule.kt` remnants or leave only app bindings. `:app` now depends on `:core:data` instead of the three source modules directly. | The NiA keystone step: after this, UI code can be forbidden from touching network/database — enforce by simply not depending on them from features later. |
| 8 | **`:core:ui`**. Move `ui/components/Components.kt`; depend on `api :core:designsystem` + `api :core:model`. | Components must take models + lambdas only; if a component reaches for a ViewModel/repository, it stays in its feature instead. |
| 9 | **`:core:playback`**. Move `playback/**`; carve `PlaybackModule`; **move the `PlaybackService` `<service>` declaration from the app manifest into `core/playback/src/main/AndroidManifest.xml`** (library manifest merging keeps the merged result identical — verify with the merged manifest / `processDebugManifest` output). | ⚠ Highest-risk step. The service must remain discoverable by Media3 session clients and keep `foregroundServiceType="mediaPlayback"` + the `MediaSessionService` intent-filter after merging. `PlayerController` is `@Singleton` injected into both the service and ViewModels — the Hilt graph must stay a single app-level graph (it does; modules only contribute bindings). |
| 10 | **`:core:download`**. Move `download/DownloadWorker.kt` + `DownloadModule`. Keep `SaavnApplication`'s WorkManager `Configuration.Provider` (Hilt worker factory) in `:app`. | Analogous to NiA's `:sync:work`. Verify a download still enqueues/runs on device after the move (`@HiltWorker` discovery is classpath-based; no manifest change). |
| 11–16 | **Features, one per commit**, in this order: `:feature:settings` (smallest, NiA also treats it as the simplest) → `:feature:home` → `:feature:search` → `:feature:library` → `:feature:detail` → `:feature:player`. Each: split the relevant screen(s) out of `Screens.kt`/`LibrarySettingsScreens.kt`/`PlayerUi.kt` and its ViewModel out of `ViewModels.kt` (one ViewModel per file now — the single-file `ViewModels.kt` cannot survive modularization), wire navigation lambdas in `:app`'s NavHost, apply `omega.android.feature`. | `:feature:player` last: `MiniPlayer` is rendered by the `:app` scaffold, so `:app` → `:feature:player` is the last edge added; the full-player route also lives there. After each feature lands, `:app`'s `ui/` tree shrinks; at the end `:app` contains only `MainActivity`, `SaavnApplication`, navigation assembly. |
| 17 | **Cleanup & guardrails.** Add a README per module with its dependency graph (NiA practice §1.2 rule 8 — a simple Mermaid block is enough); optionally add a Gradle task/CI check or a `dependencyGuard`-style baseline later. Update `.github/workflows/android-ci.yml` (unchanged commands — Gradle builds the graph). | Documentation step is part of the migration, not an afterthought. |

### Risks register

- **Hilt graph fragmentation (steps 4–10):** moving `@Module`s is safe only if each binding exists exactly once. Symptom of a mistake: duplicate-binding or missing-binding Dagger errors at `kspDebugKotlin`. Mitigation: move one module per commit; never copy-paste a provider into two modules "temporarily."
- **Room schema location (step 5):** see above — byte-identical schema check is a hard gate.
- **MediaSession service (step 9):** a service that compiles but isn't in the merged manifest fails only at runtime (notification/session never appears). Gate: inspect merged manifest + on-device playback test before proceeding to step 10.
- **Circular-dependency temptation:** `PlayerController` (playback) wants repository data (URL refresh, history) and features want `PlayerController`. NiA's answer holds: playback → `:core:data` is legal (core→core, downward); features → `:core:playback` is legal; `:core:data` must **never** depend on playback — if queue state is needed in data, pass it as parameters.
- **Build time / configuration overhead:** 16 projects configure slower than 1 on a low-end laptop. Mitigations already in NiA's setup that Omega should copy into `gradle.properties`: `org.gradle.configuration-cache` (evaluate), `org.gradle.parallel=true`, and keeping convention plugins complete so modules don't accrete bespoke config. Accept the cost — it's the explicit trade the user asked for.
- **Kotlin incremental / KSP across modules:** Hilt + Room KSP now run per-module; a stale-cache "unresolved reference" after a move is usually fixed by `gradlew clean`. Budget one clean build per step in the laptop loop.

---

## 6. What NOT to modularize yet (and why)

Following NiA's own right-sizing warning (§1.2 rule 9) and Omega's constraints:

- **No `feature:*:api` / `feature:*:impl` split, no `:core:navigation` module.** That split exists in NiA to share Navigation 3 `NavKey` types across features and across *multiple apps* (catalog/test apps). Omega has one app, Navigation Compose 2.8.5, and lambda-based navigation from `:app` (§3.3) already enforces feature isolation. Cost today: 6 extra modules + a navigation rewrite; benefit: none until Nav 3 lands (APP_DESIGN D-2). Revisit *with* the Nav 3 migration, not before.
- **No `:core:domain`.** NiA's domain holds use cases shared by ≥2 features. Every Omega operation is currently used by exactly one feature or by `:core:playback`. The first genuine candidate (`PlaySongQueue` shared by home/search/detail/library/player) should be extracted into `:core:domain` *when a second consumer appears* — creating it now would produce a pass-through layer over `MusicRepository`.
- **No `:core:analytics`, no `:core:notifications`.** Analytics is forbidden by the project's privacy constraint (APP_DESIGN NFR-8: zero tracking SDKs). Playback notifications are owned by Media3's session notification manager inside `:core:playback`; a separate notifications abstraction has no second client.
- **No `:sync` module.** NiA syncs a remote catalog into Room. Omega's catalog is never mirrored wholesale — library data is *born* local (favourites/playlists/history) and downloads are user-initiated file fetches, which is exactly `:core:download`'s job. Calling it "sync" would invite the wrong design.
- **No `:core:datastore-proto` split.** NiA splits protobuf schemas because it uses Proto DataStore. Omega uses Preferences DataStore with a handful of keys; one `:core:datastore` module suffices.
- **No `:core:testing` / `:core:data-test` / `:core:datastore-test` modules yet.** Omega has a small unit-test suite (parsing/repository tests in `:app` today; they move to `:core:network` / `:core:data` test source sets during steps 4/7). Extract shared fakes into `:core:testing` when the same fake is needed by a third module — NiA's rule of three, applied to test code.
- **No `:benchmarks`, `:lint`, `:app-catalog`.** Baseline profiles and custom lint are release-hardening (APP_DESIGN phase 8), and a catalog app earns its keep only when the design system outgrows previews. All three slot in later as pure additions — the convention-plugin layer (§4) is what makes that cheap, and building *it* now is the real NiA lesson.
- **No per-quality/per-screen micro-modules** (e.g. `:feature:album` separate from `:feature:detail`, `:core:lyrics`): granularity follows team size and change frequency, and Omega has one developer. The pre-drawn package boundaries inside `:feature:detail` (§3.1) keep future splits mechanical.

---

## 7. End state at a glance

```
SaavnMusic/
├─ app/                       :app — MainActivity, SaavnApplication, NavHost, scaffold
├─ build-logic/convention/    8 omega.* convention plugins
├─ core/
│  ├─ model/                  kotlin-jvm — Song, Album, Playlist, Artist
│  ├─ common/                 kotlin-jvm — UiState, dispatchers, utils
│  ├─ network/                SaavnApi + DTOs + NetworkModule
│  ├─ database/               Room entities/DAO/AppDatabase + schemas/
│  ├─ datastore/              SettingsRepository
│  ├─ data/                   MusicRepository (single source of truth)
│  ├─ designsystem/           OmegaTheme + tokens
│  ├─ ui/                     shared model-aware composables
│  ├─ playback/               PlayerController + PlaybackService (own manifest)
│  └─ download/               DownloadWorker (WorkManager)
├─ feature/
│  ├─ home/  search/  library/  settings/  player/  detail/
├─ gradle/libs.versions.toml  (unchanged versions; + omega.* plugin entries)
└─ settings.gradle.kts        includeBuild("build-logic") + 16 includes
```

Every rule in this plan traces to the NiA clone: module types and dependency rules (`docs/ModularizationLearningJourney.md`), offline-first repository pattern (`docs/ArchitectureLearningJourney.md` §Data layer), per-module DI (`core/*/…/di/` packages), convention plugins (`build-logic/convention/src/main/kotlin/`), and the inventory in `settings.gradle.kts`. Where Omega deviates, §6 says why — which is itself NiA's stated method: "planning beforehand and taking into account all goals… [is] crucial for defining the best fit structure under your own, unique circumstances."
