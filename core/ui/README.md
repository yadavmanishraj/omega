# :core:ui

Model-aware shared composables (`Artwork`, `SongRow`, `MediaCard`,
loading/error/empty states, section headers). Components take domain
models + lambdas only — never ViewModels or repositories (Now in Android
`core:ui` rule).

```mermaid
graph TD
    CORE_UI[":core:ui"] --> CORE_DESIGNSYSTEM[":core:designsystem"]
    CORE_UI --> CORE_MODEL[":core:model"]
```
