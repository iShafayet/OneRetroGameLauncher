# ORGL directory & SAF folder configuration

Three user-selected trees drive the library. Only the **ORGL data** tree is
writable. Spec and I/O live under `data/orgl/` and `ui/util/Saf*.kt`.

---

## The three trees

| Tree | Required | Access | Role |
|------|----------|--------|------|
| **ROMs** | Yes | Read | System subfolders with ROM files; optional per-system `gamelist.xml` |
| **ORGL data** | Yes | **Read + write** | App-owned portable files + scraped media |
| **ES-DE data** | No | Read | Fallback `downloaded_media/` + `gamelists/` |

Persisted as URI + optional path hint pairs in the **device** DataStore
([app-storage.md](./app-storage.md)).

Persistable permissions:

- ROMs / ES-DE: read
- ORGL: read + write

Taken in onboarding / settings when the user picks a folder
(`takePersistableUriPermission`).

---

## ORGL data directory contract

| | |
|--|--|
| Marker file | `ORGL.json` |
| Spec version | **1** (`OrglDataDirectory.SPEC_VERSION`) |
| Prepare API | `OrglDataDirectory.prepare(context, treeUri)` |

### `ORGL.json` fields

| Field | Meaning |
|-------|---------|
| `specVersion` | Must equal app `SPEC_VERSION` (currently `1`) |
| `app` | `"OneRetroGameLauncher"` |
| `createdAtMs` | First stamp |
| `lastOpenedAtMs` | Updated on successful prepare |

**Prepare behavior**

1. Tree must be writable.
2. If marker exists with matching version → reuse, bump `lastOpenedAtMs`.
3. If version mismatches or marker is corrupt → `Incompatible` (user must pick
   another folder).
4. If missing → create marker (SAF + optional filesystem dual-write via path hint).

Incompatible message is user-facing
(`OrglDataDirectory.incompatibleMessage`).

### Expected layout

```
<ORGL root>/
├── ORGL.json                 # required marker (spec v1)
├── downloaded_media/         # ES-DE-shaped media tree (ORGL writes scrapes here)
│   └── <systemFolder>/       # e.g. nes, snes, psx
│       ├── covers | box2d | boxfront | miximages/
│       ├── 3dboxes | box3d/
│       ├── screenshots | titlescreens | titles/
│       ├── marquees | logos/
│       ├── videos/
│       └── fanart/
├── play_history.json         # Play journal (spec v3)
├── ra_credentials.json       # optional encrypted RA (spec v2; reads legacy v1)
└── run_cards/                # portable run-card PNGs
    └── <system>_<file>_<committedAt>.png
```

I/O helper: `OrglTreeFiles` — prefers SAF `content://` documents; can also use
the path hint as a `File` dual path when resolvable.

---

## Portable files

### `play_history.json` (spec **3**)

Module: `OrglPlayHistoryFile`.

Stores **FINISHED / DROPPED** Play commitments for cross-device import/export.
Games are keyed by **system folder + ROM file name** (stable across absolute
paths).

Snapshot shape (conceptual):

```json
{
  "specVersion": 3,
  "updatedAtMs": 0,
  "commitments": [
    {
      "systemFolder": "snes",
      "fileName": "Game.sfc",
      "title": "…",
      "committedAt": 0,
      "releasedAt": 0,
      "status": "FINISHED",
      "review": { "stars": 4.5, "text": "…", "createdAt": 0 },
      "sessions": [{ "startedAt": 0, "endedAt": 0, "durationMs": 0 }],
      "playtimeMs": 0,
      "sessionCount": 0,
      "game": { "systemDisplayName": "…", "genre": "…", "…": "…" },
      "runCardFile": "run_cards/….png"
    }
  ]
}
```

**Not included:** Setup-mode launch counters on `games` (`orglPlaycount`, etc.).

Orchestration: `OrglExternalSync` (import on setup / sync, export on demand).

### `ra_credentials.json` (spec **2**)

Module: `OrglRaCredentialsFile`.

- Written only when `retroAchievementsStoreOnDisk` is true.
- Secrets encrypted via `OrglRaSecretBox` (`passwordEnc` / `tokenEnc`).
- Legacy spec **1** still readable.

### `run_cards/`

Module: `RunCardStore`.

- Portable name: `run_cards/<system>_<file>_<committedAt>.png`
- Also cached locally as `filesDir/run_cards/<commitmentId>.png`

---

## ROMs tree

Expected shape: root contains **system folders** named like catalog
`folderName` values (`nes/`, `snes/`, `psx/`, …).

| Also used | Notes |
|-----------|--------|
| Per-system `gamelist.xml` | Optional metadata / relative media paths |
| Multi-disk / m3u sets | Handled in scan logic |

Scan entry: `LibraryRepository.scanLibrary` → `RomScanner`.

Heuristic: `SafPathResolver.looksLikeRomsRoot` (child folder names match known
systems).

---

## ES-DE data tree (optional)

Read-only. Never written by ORGL.

| Path | Use |
|------|-----|
| `downloaded_media/<system>/<type>/…` | Artwork / video fallback |
| `gamelists/<system>/gamelist.xml` | Metadata + play counts (ES-DE wins on merge) |

Discovery: `GamelistSources`, `MediaLibrary` (label `"ES-DE"`).

`MediaLibrary` finds `downloaded_media` at the tree root or up to ~2 levels
deep (e.g. `Emulation/ES-DE/downloaded_media`).

---

## Media indexing & priority

Both ORGL and ES-DE trees are opened as `MediaLibrary` instances during scan
(labels `"ORGL"` and `"ES-DE"`).

Lazy index: `downloaded_media/<system>/<mediaFolder>/<file>` (recursive).

**Resolve order** (`RomScanner.resolveMedia`, simplified):

1. Existing DB media from ORGL / scrape providers  
2. ORGL `MediaLibrary` → provider `orgl`  
3. Other existing DB media  
4. ES-DE `MediaLibrary` → provider `es-de`  
5. Gamelist-relative files under the ROM system folder → `gamelist`

Folder names map to `MediaType` via `MediaType.fromEsDeFolder` (covers →
`BOX_2D`, screenshots → `SCREENSHOT`, videos → `VIDEO`, etc.).

Scrapes write under the **ORGL** tree (`ArtworkScraper`), using the same
ES-DE-shaped relative paths. If the ORGL tree is unavailable, scraper may fall
back to `context.filesDir/downloaded_media/…`.

---

## SAF validation (`SafFolderAccess`)

Run on resume / startup after onboarding is done.

| Result | Condition |
|--------|-----------|
| `None` | Onboarding incomplete, or all configured trees OK |
| `ResetForReOnboarding` | ROMs **or** ORGL inaccessible → clear folders, force onboarding |
| `ClearEsdeOnly` | ES-DE configured but inaccessible → drop ES-DE link only |

Checks: persisted URI permission + `DocumentFile` list (and write for ORGL).

Path hints: `SafPathResolver` maps tree URIs to likely filesystem paths
(`/storage/emulated/0/…`, secondary volumes, etc.) for RetroArch paths,
dual-write, and MediaLibrary file fallback.

Low-level SAF I/O: `SafIo` (list/read/write guards that avoid crashes on lost
access).

---

## UI entry points

| Surface | What it configures |
|---------|-------------------|
| Onboarding pages 1–2, 4 | ROMs, ORGL, optional ES-DE |
| Settings → Folders | Same required trees |
| Settings → ES-DE | Optional ES-DE link + rescan CTA |
| Settings → Database (debug) | `OrglExternalSync.syncNow()` |

Onboarding persists folder URIs as they are picked so an interrupted wizard can
resume to the scan/summary step when ROMs + ORGL are already set.

---

## Related

- [data-overview.md](./data-overview.md) — trust boundaries  
- [database.md](./database.md) — `media.path` / providers in Room  
- [app-storage.md](./app-storage.md) — where URIs are stored  
