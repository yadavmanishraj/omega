# :feature:home

Home feed (browse sections, trending, recently played).

Navigation is received as lambdas from `:app` (no feature depends on
another feature). Shared UI comes from `:core:ui` / `:core:designsystem`
via the `omega.android.feature` convention plugin.

```mermaid
graph TD
    FEATURE_HOME["feature:home"] --> CORE_DATA[":core:data"]
```
