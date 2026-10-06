# :core:datastore

Preferences DataStore settings (`SettingsRepository` + `AppSettings`) and
`DataStoreModule`. The preferences file is still named `settings` and the
keys are unchanged, so existing installs keep their values.

```mermaid
graph TD
    CORE_DATASTORE[":core:datastore"] --> CORE_MODEL[":core:model"]
    CORE_DATASTORE --> CORE_COMMON[":core:common"]
```
