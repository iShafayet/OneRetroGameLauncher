# App storage (on-device, outside Room)

Room is covered in [database.md](./database.md). This doc covers **prefs**,
**backup policy**, and **private file dirs**.

Primary source: `data/prefs/SettingsRepository.kt`.

There is **no SharedPreferences** usage in app code — only DataStore Preferences.

---

## Two DataStores

| Name | File (under app files) | Backed up? | Purpose |
|------|------------------------|------------|---------|
| `orgl_settings` | `datastore/orgl_settings.preferences_pb` | **Yes** | Theme, integrations, layout, slots |
| `orgl_device` | `datastore/orgl_device.preferences_pb` | **No** | SAF URIs, onboarding, app mode |

Constants: `SettingsRepository.BACKUP_DATASTORE_NAME` /
`DEVICE_DATASTORE_NAME`.

`AppSettings` is the merged view (`combine` of both stores).

### Backup / transfer rules

- Manifest: `allowBackup=true`
- `res/xml/data_extraction_rules.xml` — cloud backup + device transfer include
  app files, **exclude** `datastore/orgl_device.preferences_pb`
- `res/xml/backup_rules.xml` — pre-12 full-backup mirror of that exclude

**Implication:** restoring a backup does **not** restore folder picks or
onboarding/mode. User must re-select ROMs / ORGL (and optional ES-DE).

---

## Device keys (`orgl_device`)

| Preference key | `AppSettings` field | Notes |
|----------------|---------------------|--------|
| `roms_dir_uri` | `romsDirUri` | Persistable SAF tree |
| `roms_dir_path` | `romsDirPath` | Optional filesystem hint |
| `orgl_data_dir_uri` | `orglDataDirUri` | Required writable tree |
| `orgl_data_dir_path` | `orglDataDirPath` | Path hint |
| `esde_data_dir_uri` | `esdeDataDirUri` | Optional read-only |
| `esde_data_dir_path` | `esdeDataDirPath` | Path hint |
| `app_data_dir_uri` | *(legacy)* | Read as fallback for ES-DE URI |
| `onboarding_done` | `onboardingDone` | Gate into ModeShell |
| `app_mode` | `appMode` | `SETUP` / `PLAY` |

Writers of note:

- `setRomsDir` / `setOrglDataDir` / `setEsdeDataDir` / `clearEsdeDataDir`
- `setOnboardingDone` / `setAppMode`
- `resetForLostSafAccess()` — clears all folder keys, sets
  `onboarding_done=false`, `app_mode=SETUP`

Folder URI validation: `SafFolderAccess` (see [orgl-directory.md](./orgl-directory.md)).

---

## Backup keys (`orgl_settings`)

| Preference key | `AppSettings` field | Notes |
|----------------|---------------------|--------|
| `theme_mode` | `themeMode` | `SYSTEM` / `LIGHT` / `DARK` |
| `ss_user`, `ss_pass`, `ss_devid`, `ss_devpass` | ScreenScraper | |
| `ra_user`, `ra_password`, `ra_token` | RetroAchievements | |
| `ra_api_key` | *(legacy)* | Cleared when setting new RA creds |
| `ra_store_on_disk` | `retroAchievementsStoreOnDisk` | Also writes ORGL file when true |
| `ra_package` | `preferredRetroArchPackage` | |
| `hltb_enabled` | `hltbEnabled` | Default `true` |
| `game_list_layout` | `gameListLayout` | `GRID` / `LIST` |
| `last_scrape_at` | `lastScrapeAt` | Epoch ms as string in store |
| `play_slot_count` | `playSlotCount` | Clamped 1–5 |
| `library_show_favorites` | `libraryShowFavorites` | Default `true` |
| `library_show_recent` | `libraryShowRecent` | Default `true` |

Sensitive values (SS / RA passwords, tokens) are in the **backup** store by
design so they survive device transfer — they are **not** in Room.

Optional on-disk RA copy lives in the ORGL folder (`ra_credentials.json`),
gated by `ra_store_on_disk`.

---

## Private app files (`context.filesDir`)

| Relative path | Role |
|---------------|------|
| `run_cards/<commitmentId>.png` | Local run-card cache (`RunCardStore.LOCAL_DIR`) |
| `collages/` | Collage generator output |
| `downloaded_media/<system>/<type>/` | Scrape write fallback if ORGL tree unavailable |

Portable run cards also exist under the ORGL tree
(`run_cards/<system>_<file>_<committedAt>.png`) — see
[orgl-directory.md](./orgl-directory.md).

### FileProvider

- Authority: `${applicationId}.fileprovider`
- Exposed roots (`file_paths.xml`): `cache/`, `files/`, `external-files/`

Used when another app (e.g. RetroArch) needs a shareable URI for a file ORGL
holds or stages.

---

## Cache / external

Standard Android `cacheDir` / `getExternalFilesDir` are available via
FileProvider paths; ORGL does not treat them as a durable user contract.
Durable user-owned files belong in the **ORGL data directory**.

---

## Mental model

| Concern | Store |
|---------|--------|
| “What folders did I pick?” | Device DataStore (not backed up) |
| “Am I done onboarding / in Play?” | Device DataStore |
| “Theme, slots, API logins” | Backup DataStore |
| “What’s in my library right now?” | Room |
| “What can I copy to another phone?” | ORGL folder (+ optionally backup prefs) |
