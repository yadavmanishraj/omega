# :core:download

User-initiated downloads (Omega's analogue of Now in Android's
`:sync:work`): the WorkManager `DownloadWorker` plus the
`DownloadModule` bindings. Workers are discovered by Hilt's worker
factory, which `SaavnApplication` (:app) installs as the WorkManager
Configuration.Provider.

```mermaid
graph TD
    CORE_DOWNLOAD[":core:download"] --> CORE_DATA[":core:data"]
    CORE_DOWNLOAD --> CORE_MODEL[":core:model"]
    CORE_DOWNLOAD --> CORE_COMMON[":core:common"]
```
