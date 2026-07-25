# Data overview — what lives where

ORGL keeps a hard split between **device state**, **portable user files**, and
**read-only external libraries**.

```
┌─────────────────────────────────────────────────────────────┐
│ Device (app private storage)                                │
│  • Room orgl.db          library + play state + media paths │
│  • DataStore orgl_device SAF URIs, onboarding, app mode      │
│  • DataStore orgl_settings theme / integrations (backed up) │
│  • filesDir/run_cards, collages, scrape fallback            │
└─────────────────────────────────────────────────────────────┘
         │                         │
         │ links (paths/URIs)      │ portable journal / media
         ▼                         ▼
┌──────────────────┐    ┌─────────────────────────────────────┐
│ ROMs tree (SAF)  │    │ ORGL data tree (SAF, read+write)    │
│ read-only        │    │ ORGL.json, downloaded_media/,       │
│ system folders   │    │ play_history.json, ra_credentials,  │
│ + optional       │    │ run_cards/                          │
│ gamelist.xml     │    └─────────────────────────────────────┘
└──────────────────┘
         ▲
         │ optional read-only fallback
┌──────────────────┐
│ ES-DE data (SAF) │
│ downloaded_media │
│ gamelists/       │
│ never written    │
└──────────────────┘
```

## Trust boundaries

| Area | ORGL may | ORGL must not |
|------|----------|----------------|
| ROMs folder | List / open files for scan & launch | Modify or delete ROMs |
| ORGL data folder | Create/update marker, media, journal, credentials, run cards | — |
| ES-DE data folder | Read media + gamelists | Write anything |
| Room DB | Full ownership on-device | Assume it is portable across devices without export |

## Portability cheat sheet

| Want to keep when changing phones | How |
|-----------------------------------|-----|
| Scraped artwork | Copy the ORGL data folder (`downloaded_media/`) |
| Finished / dropped journal + run cards | Same folder (`play_history.json`, `run_cards/`) |
| Optional RA credentials on disk | Same folder (`ra_credentials.json`) if enabled |
| Theme / ScreenScraper / RA prefs in app | Android backup of `orgl_settings` |
| Library catalog, active commitments, launch stats | **Not portable** — live in Room on that device |
| Folder picks (SAF URIs) | **Not backed up** — re-pick on new device / after restore |

Launch stats on `games` (`orglPlaycount`, `orglLastPlayed`, `orglPlaytimeMs`) stay
on-device only. The portable journal is Play-mode finish/drop history, not those
counters.

## Related docs

- [database.md](./database.md) — schema detail  
- [app-storage.md](./app-storage.md) — prefs & private files  
- [orgl-directory.md](./orgl-directory.md) — folder layout & SAF  
