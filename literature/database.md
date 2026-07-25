# Room database (`orgl.db`)

| | |
|--|--|
| File name | `orgl.db` |
| Class | `AppDatabase` |
| Version | **6** |
| Schema export | `exportSchema = false` |
| Migrations | **None** — `fallbackToDestructiveMigration(dropAllTables = true)` |
| Wiring | `di/AppModule.kt` |

Bump `version` carefully: existing installs **wipe and recreate** the DB.

Primary sources: `data/db/AppDatabase.kt`, `entity/Entities.kt`, `dao/Daos.kt`.

---

## Entity relationship (simplified)

```
systems 1──* games 1──0..1 game_config
              │
              ├──* media          (one row per MediaType per game)
              └──* commitments
                    ├──* play_sessions
                    └──0..1 reviews

emulator_profiles   (standalone catalog; no FK to systems)
hltb_cache          (keyed by query string)
```

`JournalDao` is a **read-only join** over commitments / games / systems / reviews /
sessions — not a physical table.

---

## Tables

### `systems`

Catalog of platforms (seeded from assets `systems/es_systems.xml`).

| Column | Notes |
|--------|--------|
| `id` | PK, auto |
| `name` | Unique short id |
| `folderName` | Unique ROM subfolder (`nes`, `snes`, …) |
| `displayName`, `platform` | UI / metadata |
| `extensionsCsv` | Allowed ROM extensions |
| `defaultEmulatorKey`, `defaultCore` | Optional launch defaults |

### `games`

One row per ROM (unique on `systemId` + `romPath`).

| Column | Notes |
|--------|--------|
| `title`, `romPath`, `fileName` | Identity + display |
| `romPathsJson` | Multi-disk set (`"[]"` default) |
| `favorite`, `onShelf` | Library flags (shelf max 5 in app logic) |
| `description` | Scraped / gamelist synopsis |
| `notes` | User notes (ORGL-only) |
| `rating`, `releaseDate`, `developer`, `publisher`, `genre`, `players` | Metadata |
| `esdePlaycount`, `esdeLastPlayed` | From ES-DE gamelist on scan |
| `orglPlaycount`, `orglLastPlayed`, `orglPlaytimeMs` | Device launch stats (not in `play_history.json`) |
| `completedStatus` | `FINISHED` / `DROPPED` / null |
| `raGameId`, `hltbId` | External IDs |
| `unknownExtensionsNote` | Scan diagnostics |
| `lastScrapedAt` | Last ORGL scrape attempt (epoch ms) |

FK: `systemId` → `systems.id` **CASCADE**.

### `game_config`

Per-game emulator override (`gameId` is PK + FK → `games` CASCADE).

| Column | Notes |
|--------|--------|
| `useOverride` | When true, overrides system defaults |
| `emulatorKey`, `coreOverride`, `customConfigPath` | Launch override |

### `emulator_profiles`

Known emulator package/activity pairs (from assets `es_find_rules.xml`).

| Column | Notes |
|--------|--------|
| `key` | PK string |
| `displayName`, `packageName`, `activity` | |
| `intentAction`, `extrasJson`, `systemsCsv` | |
| `installed` | Refreshed against PackageManager |

### `commitments`

Play-mode lock on a game.

| Column | Notes |
|--------|--------|
| `gameId` | FK → `games` CASCADE |
| `committedAt`, `releasedAt` | |
| `status` | `ACTIVE` / `FINISHED` / `DROPPED` |
| `slotIndex` | 0-based play slot while ACTIVE |

### `play_sessions`

Timed sessions under a commitment (`commitmentId` FK CASCADE).

| Column | Notes |
|--------|--------|
| `startedAt`, `endedAt`, `durationMs` | |

### `reviews`

At most one review per commitment (unique `commitmentId`).

| Column | Notes |
|--------|--------|
| `stars`, `text`, `collagePath`, `createdAt` | |

### `media`

**Paths only** — bytes live under ORGL/ES-DE (or app files fallback).

| Column | Notes |
|--------|--------|
| `type` | `MediaType` enum |
| `path` | Absolute path or `content://` URI |
| `provider` | See below |

Unique (`gameId`, `type`).

**Provider values in practice**

| Provider | Origin |
|----------|--------|
| `orgl` | ORGL `downloaded_media/` |
| `es-de` | ES-DE `downloaded_media/` |
| `gamelist` | Paths relative to ROM system folder |
| `screenscraper` / `libretro-thumbnails` | Scrape pipelines |
| `local` | Default / generic |

Scan resolve order prefers ORGL-owned providers, then ES-DE, then gamelist
(see `RomScanner.resolveMedia`).

### `hltb_cache`

HowLongToBeat lookup cache keyed by `queryKey`.

---

## Type converters

Stored as enum **names** (strings):

- `MediaType` (default `UNKNOWN` on bad value)
- `CommitmentStatus` (default `FINISHED`)
- `GameCompletedStatus?` (nullable)

---

## DAO map

| DAO | Responsibility |
|-----|----------------|
| `SystemDao` | Catalog CRUD / observe |
| `GameDao` | Library, search, favorites, recent, shelf, scan stats |
| `GameConfigDao` | Per-game override |
| `EmulatorProfileDao` | Emulator catalog + install flag |
| `CommitmentDao` | Active slots / by game |
| `PlaySessionDao` | Sessions + duration sums |
| `ReviewDao` | One review per commitment |
| `MediaDao` | By game/type; delete by provider |
| `JournalDao` | Joined history rows for UI |
| `HltbCacheDao` | Cache get/put |

Useful aggregate queries (post-scan summary): `scanStatsBySystem`,
`mediaStatsBySystem`, `observeGameCountsBySystem`.

---

## What is *not* in Room

| Data | Where instead |
|------|----------------|
| ROM bytes | ROMs SAF tree |
| Artwork / video files | ORGL or ES-DE `downloaded_media/` |
| Portable finish/drop journal | `play_history.json` in ORGL folder |
| Optional RA secrets on disk | `ra_credentials.json` |
| Theme, credentials prefs, layout | DataStore (see [app-storage.md](./app-storage.md)) |
| SAF folder URIs | Device DataStore |

---

## Destructive migration note

There are **no** `Migration` objects. Changing `AppDatabase.version` drops all
tables. Acceptable while the product is early; before a stable release, either
add real migrations or document a wipe as intentional.
