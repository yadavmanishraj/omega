# :feature:library

On-device library: favorites, downloads, history, local playlists.

Navigation is received as lambdas from `:app` (no feature depends on
another feature). Shared UI comes from `:core:ui` / `:core:designsystem`
via the `omega.android.feature` convention plugin.

```mermaid
graph TD
    FEATURE_LIBRARY["feature:library"] --> CORE_DATA[":core:data"]
    FEATURE_LIBRARY["feature:library"] --> CORE_DOWNLOAD[":core:download"]
```
