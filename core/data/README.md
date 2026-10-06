# :core:data

The single source of truth: `MusicRepository` orchestrates
:core:network (remote), :core:database (local library) and
:core:datastore (settings). Features depend on this module — never on
the network/database modules directly. `DataModule` provides the API
client (it needs settings, which sit in :core:datastore).

```mermaid
graph TD
    CORE_DATA[":core:data"] --> CORE_NETWORK[":core:network"]
    CORE_DATA --> CORE_DATABASE[":core:database"]
    CORE_DATA --> CORE_DATASTORE[":core:datastore"]
    CORE_DATA --> CORE_MODEL[":core:model"]
    CORE_DATA --> CORE_COMMON[":core:common"]
```
