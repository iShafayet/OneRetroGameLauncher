# One Retro Game Launcher (ORGL) — Full Functionality Summary

> Literature inventory of every user-facing capability, with emphasis on **copy**, **CTAs**, and **benefit to the user**.  
> Derived from in-app UI strings, About/Credits, README, and product architecture as of the current codebase  
> (includes wishlist play queue, media gallery/viewer, About/Credits external links, soft cyan focus chrome, and gamepad IME handling).

---

## 1. What the product is (and why it exists)

**App name:** One Retro Game Launcher  
**Short name:** ORGL  
**README / product tagline:** *Stop scrolling your library. Start finishing it.*  
**Onboarding tagline:** *Stop scrolling. Start finishing.*

ORGL is a **commitment-first** Android frontend for retro ROM libraries. It is not an emulator and does not rewrite your ROMs. Emulators stay external (RetroArch or standalone apps). The product thesis is behavioral: reduce endless browsing and help the user **pick one game, lock it in, and finish or consciously drop it**.

### Core user benefits

| Benefit | How ORGL delivers it |
|--------|----------------------|
| Finish games instead of hoarding | Play mode locks a game until finish/drop |
| Build a play queue without mixing “love it” and “play next” | **Wishlist** is separate from favorites; feeds Tonight’s trio |
| Keep ownership of files | ROMs are read-only; app data lives in a dedicated ORGL folder |
| Use what you already scraped | Optional ES-DE `downloaded_media/` + `gamelists/` (read-only) |
| Browse artwork without a vertical dump | Media tab gallery (max 4 across) → fullscreen image/video viewer |
| Launch with your preferred emulator | RetroArch cores or standalone packages, per system or per game |
| See progress without another app hop | Optional RetroAchievements progress (ORGL reads; unlocks happen in RetroArch) |
| Know how long a game might take | Optional HowLongToBeat estimates in Play mode |
| Keep a personal play journal | History of finished/dropped runs with ratings, reviews, and saveable run cards |
| Controller-friendly living-room use | Soft cyan focus chrome, shoulder tab cycling, gamepad IME lock, double-press exit |

### About-screen mission (canonical product copy)

> **Mission:** Help you play one game at a time. Pick something, commit to it, and give it a fair run — instead of drowning in an endless “maybe later” shelf.

> **Vision:** A calm library that stays out of the way: your ROMs stay yours, artwork lives in your ORGL data folder, and emulators stay external. Optional integrations (ES-DE media, ScreenScraper, RetroAchievements, HowLongToBeat) enrich the experience without locking you in.

> **How we work:**
> - Setup mode — organize systems, scrape media, tune emulators.
> - Play mode — commit to a single game until you finish or drop it.
> - Your folders — ROMs are read-only; scrapes write only to ORGL data.
> - Your choice of emulator — RetroArch or standalone, per system or per game.

### About — Project & author (external links open in browser)

| Section | Content / CTAs |
|---------|----------------|
| Hero | Logo · `One Retro Game Launcher` · tagline *Stop scrolling your library. Start finishing it.* |
| **Project** | **Website** → `oneretrogamelauncher.com` · **Source code** → `github.com/iShafayet/OneRetroGameLauncher` |
| **Author** | **Sayem Shafayet** — FOSS engineer/maintainer bio (libre.money, nkrypt.xyz, open ethos) |
| Author links | **Website** → `sayemshafayet.com` · **GitHub** → `github.com/iShafayet` |
| **CTA** | **`Credits`** (top bar handles back) |

---

## 2. App entry & high-level navigation

### Boot flow

1. System splash (`installSplashScreen`)
2. Compose splash — brand logo + **"Loading…"**
3. If onboarding incomplete → **Onboarding wizard**
4. Else → **ModeShell** (Setup or Play)

### Two modes (primary mental model)

| Mode | User job | Bottom / hub |
|------|----------|--------------|
| **Setup** | Organize library, media, emulators, settings | Tabs: **Library** · **History** · **Settings** |
| **Play** | Commit, play, finish/drop, celebrate | Intro → picker → confirm → focus → completion |

**Mode switcher CTAs (top bar):** `Setup` | `Play`

**Setup hub title:** `ORGL` plus version (`VERSION_NAME`). On **vertical phone form factors only** (portrait orientation **and** width/height ratio &lt; 1), the version stacks under the ORGL label; otherwise it stays inline beside the title.

**Setup hub actions:** Library search (Library tab) · About (help) · Setup/Play segmented control. Gamepad hint badges appear when a controller is connected (e.g. search **X**, mode switch **Y**).

Switching from Play → Setup while runs are active shows a trust/guard dialog (see §9). Play uses a distinct amber-forward theme so the commitment mode feels different from Setup browsing.

### Double-press exit

On root hubs: **"Press again to exit"**

---

## 3. Onboarding (first-run wizard)

Six pages that teach the product promise and wire required folders before the user can browse or play.

### Page 0 — Welcome

| Element | Copy |
|---------|------|
| Brand | `ORGL` |
| Title | `One Retro Game Launcher` |
| Tagline | `Stop scrolling. Start finishing.` |
| Body | `One game at a time — the only launcher built around commitment, not infinite browsing.` |
| Trust line | `ES-DE compatible. Your ROMs, your media, your rules. We never touch your files.` |
| **Primary CTA** | **`Let's finish some games`** |

**User benefit:** Immediate clarity that this is not “another library browser” — it’s a finishing tool.

### Page 1 — ROMs folder (required)

| Element | Copy |
|---------|------|
| Title | `Point us at your ROMs` |
| Body | `Pick the ROMs root — one subfolder per system (nes, snes, …). Read-only. ORGL never writes here.` |
| Folder CTA | `Choose ROMs folder` / `Change folder` |
| Path label | `Resolved path` (or SAF-only note) |
| **Primary CTA** | **`Continue`** (enabled once folder linked) |
| Secondary | `Back` |

**User benefit:** Safe link to an ES-DE-style ROM tree without fear of corruption.

### Page 2 — ORGL data folder (required)

| Element | Copy |
|---------|------|
| Title | `ORGL data folder` |
| Body | Scraped artwork / ORGL-owned files in `downloaded_media/`; can reuse a previous install’s folder |
| Spec note | Writes `ORGL.json` marker for future compatibility |
| Folder CTA | `Choose ORGL data folder` / `Change folder` |
| Reuse toast-ish | `Existing ORGL data detected — reusing scraped media and files from this folder.` |
| Dialog | `Incompatible ORGL folder` → **`Choose another folder`** |
| **Primary CTA** | **`Continue`** |

**User benefit:** Portable app data across reinstalls; clear separation from ROMs.

### Page 3 — RetroAchievements (optional)

| Element | Copy |
|---------|------|
| Title | `RetroAchievements` |
| Body | Same username/password as RetroArch; ORGL only reads progress; unlocks still happen in RetroArch |
| Checking | `Checking the ORGL data folder for saved credentials…` |
| Success | `Signed in.` |
| Fields | `Username` · `Password` |
| Switch | `Store on disk` — encrypted credentials in ORGL data folder |
| Action | **`Save & verify`** |
| **Primary CTA** | `Continue` (if connected) / **`Skip for now`** / `Checking…` |
| Secondary | `Back` |

**User benefit:** Optional achievement progress in-app without changing how unlocks are earned.

### Page 4 — ES-DE data folder (optional)

| Element | Copy |
|---------|------|
| Title | `ES-DE data folder` |
| Body | Reuse `downloaded_media/` + `gamelists/` read-only; never writes to ES-DE |
| Tip | `You can skip this and link it later in Settings.` |
| Folder CTA | `Choose ES-DE data folder` / `Change folder` |
| Clear | `Clear selection` |
| **Primary CTA** | `Continue` (if linked) / **`Skip for now`** |
| Secondary | `Back` |

**User benefit:** Instant artwork/metadata if the user already invested in ES-DE scraping.

### Page 5 — Done + first scan

| Element | Copy |
|---------|------|
| Title | `You're set` |
| Scanning body | `First scan running in the background…` |
| Ready body | `Library ready. Pick Setup to browse and scrape, or Play to commit to your first game.` |
| Progress | `Scanning {system}… {n} games` / `Preparing library…` |
| **Primary CTA** | **`Enter ORGL`** / `Scanning…` |

**User benefit:** Leaves onboarding with a usable library and a clear next choice (Setup vs Play).

---

## 4. Setup mode — Library

### Library hub

- Shows systems discovered from the ROMs tree (via `es_systems.xml` definitions).
- Header style: `{n} systems · {m} games`
- Empty state: **`No games found — set your ROMs folder in Settings → Folders, then rescan.`**
- Optional virtual rows (Appearance toggles):
  - **Favorites** — `"All favorites"`
  - **Wishlist** — `"Play queue"` (separate from favorites)
  - **Recent** — `"Recently played"`
- Hub top-bar search opens **Search library** (cross-system title search).

**User benefit:** Fast jump into systems; favorites / wishlist / recent reduce hunting across platforms.

### System games list

| Control | Copy / behavior |
|---------|-----------------|
| Search | `Search games` |
| Filters | `All` · `★` · `Wish` · `Done` · `Dropped` (`Wish` / `★` hidden when already inside that virtual list) |
| View toggle | `List view` / `Grid view` |
| Emulator gear | `Emulator settings` (physical systems) |

**User benefit:** Filter by personal status (favorite / wishlist / finished / dropped) and find titles quickly.

### System emulator defaults

- Explains defaults apply to the system; individual games can override.
- Emulator dropdown + RetroArch **Core filename** (e.g. `snes9x_libretro_android.so`)
- **CTA:** `Save` → confirmation `Saved`

**User benefit:** Set-and-forget launch config per platform.

### Game detail (tabs: **Game** · **Media** · **Config**)

#### Game tab

| Element | Copy |
|---------|------|
| Primary action | **`Launch`** |
| RA | RetroAchievements button (progress / sign-in prompts) |
| Stats | `Last played` · `Activity` |
| Status chips | `Favorite` · `Wishlist` · `Done` · `Dropped` |
| Notes | Label `Personal notes for this game` · placeholder `Add your thoughts…` · **`Save notes`** |
| Snackbar | `Launched` or launch error |

**Wishlist vs Favorite:** Favorite is “I love this”; Wishlist is the **play queue** used by Play mode’s Tonight’s trio. They are independent flags in the DB.

#### Media tab

| Element | Copy / behavior |
|---------|-----------------|
| Empty | `No media found for this game.` |
| Gallery | Adaptive grid, **max 4 tiles per row**, min cell ~120dp; square tiles with subtle fill + border; image/video thumbnails centered (`Fit`) |
| Tile label | Media type (Box art, 3D box, Screenshot, Title screen, Marquee, Video, Fan art, …) |
| Open | Activate tile → fullscreen **Media viewer** route (`setup/game/{gameId}/media/{mediaId}`) |
| Viewer | Image Fit / video player; top bar title = media type label |
| Metadata | Full-width section under the grid: Description, Genre, Developer, Publisher, Release, Players, Rating — **focusable** so D-pad can reach it after the tiles |

**User benefit:** Modern gallery browsing instead of a tall vertical image dump; inspect one asset at a time fullscreen.

#### Config tab

| Element | Copy |
|---------|------|
| Header | `Launch configuration` |
| System default | Shows current system default |
| Override | `Per-game override` · `Use a different emulator or core for this game only` |
| Fields | `Core filename` · `Custom config path (optional)` |
| CTA | **`Save override`** |

#### Debug launch guard (launch from Setup while Play slots active)

| Step | Copy |
|------|------|
| Title | `Debug launch` |
| Body | Explains launching from Setup while Play is active is **for debugging/testing only** |
| Nudge | `Use Play mode to commit to a game properly.` |
| CTAs | **`I understand`** → countdown **`Take a breath…`** / `Wait…` → **`Launch anyway`** · `Cancel` |
| Ready | `Ready when you are. Tap below to launch.` |

**User benefit:** Still allows testing launches without silently breaking the commitment philosophy.

### Game RetroAchievements screen

| State | Copy / CTAs |
|-------|-------------|
| Loading | `Loading achievements…` |
| Not signed in | `Sign in required` · **`Open RetroAchievements settings`** |
| Error / unsupported | `Could not load achievements` / `Not supported` · `Retry` |
| Progress | `{earned} / {total} unlocked · {pts} / {ptsTotal} points` · Hardcore · Recent unlocks · Achievements list |
| Warning | `Launch setup cannot earn achievements` |

**User benefit:** See unlock progress in context of a game without leaving ORGL; clear that Setup launch won’t earn RA.

---

## 5. Setup mode — History

| Element | Copy |
|---------|------|
| Title | `History` |
| Subtitle | `Every game you finished or dropped in Play mode — with your ratings and reviews.` |
| Loading | `Loading journal from disk…` |
| Empty | `No completed runs yet. Commit to a game in Play mode and finish or drop it — it will show up here.` |
| Status badges | `Finished` / `Dropped` |
| CTA | **`View run card`** |

**User benefit:** A personal journal of intentional play — not just raw launch timestamps.

History entries open the same **run card** UI used after Play completion (history view: **`Back to history`**).

---

## 6. Setup mode — Settings hub

### Library

| Row / CTA | Subtitle / help |
|-----------|-----------------|
| **Folders** | `ROMs directory set/not set · ORGL directory set/not set` |
| **`Rescan library`** | `Scans ROMs, ORGL media, and ES-DE metadata/media in one pass.` |

### Integrations

| Row | Subtitle |
|-----|----------|
| **ES-DE** | `Connected` / `Connect ES-DE to use game artwork and information` |
| **ScreenScraper** | `Under construction — use ES-DE for media` |
| **RetroAchievements** | `Signed in` / `Not configured` |
| **HowLongToBeat** | `Enabled` / `Disabled` |
| **RetroArch** | Package name or `Not set` |

### Play

| Row | Subtitle |
|-----|----------|
| **Multiple Now Playing Slots** | `1 slot` / `N slots` |

### Appearance

| Control | Copy |
|---------|------|
| Theme chips | `SYSTEM` / `LIGHT` / `DARK` |
| **Favorites section** | `Virtual system at the top of the library` |
| **Wishlist section** | `Play-queue games highlighted in Play suggestions` |
| **Recent section** | `Recently played games across all systems` |

### Debug

| Row | Subtitle |
|-----|----------|
| **Database** | `Sync and data maintenance` / `ORGL directory not set` |

### Information

| Row | Subtitle |
|-----|----------|
| **About** | `What ORGL is and how it works` |
| **Credits** | `Contributors and acknowledgements` |

---

## 7. Settings detail screens

### Folders

| Section | Copy highlights | CTAs |
|---------|-----------------|------|
| ROMs | Required; one subfolder per system; read-only | `Browse for ROMs folder` / `Change ROMs folder` |
| ORGL data | Required; scraped media + app files; `ORGL.json` | `Browse for ORGL data folder` / `Change ORGL data folder` |
| Rescan | `One scan covers ROMs, ORGL media, and linked ES-DE data.` | **`Rescan library`** |
| Dialog | `Incompatible ORGL folder` | **`Choose another folder`** |

**User benefit:** One place to fix paths and refresh the catalog.

### Library scan

| State | Copy |
|-------|------|
| Running | Status / `Starting scan…` · `Preparing…` · `System X of Y: {name}` |
| ES-DE note | `Including ES-DE gamelists and downloaded_media in this scan.` |
| Stats | Systems · Games found · Media linked · Unrecognized files · current-system progress |
| Outcomes | `Scan complete` / `Scan cancelled` / `Scan failed` |
| CTAs | **`Abort scan`** (running) · **`Done`** (finished) |

**User benefit:** Transparent progress when indexing large libraries; abortable.

### ES-DE integration

| Element | Copy |
|---------|------|
| Help | Optional but recommended; read-only fallback for artwork + metadata; never writes to ES-DE |
| Status | `Linked` / `Not linked` |
| CTAs | `Link ES-DE data folder` / `Change ES-DE data folder` · **`Rescan library`** · **`Unlink ES-DE`** |
| Unlink tip | Clears setting + cached ES-DE-linked media; ES-DE install untouched |
| Rescan tip | ES-DE metadata/media refreshed as part of full library scan |

**User benefit:** Leverage existing ES-DE investment without migrating files.

### ScreenScraper (placeholder UI)

| Element | Copy |
|---------|------|
| Status | `Scraping is under construction.` |
| Body | Credentials/scraping not available yet; link ES-DE instead for already-scraped media |
| CTA | **`Open ES-DE integration`** |

**Note:** Full scrape wizard + ScreenScraper client + foreground service exist in code (`setup/scrape/wizard`, notification channel **Artwork scraping** / title **Scraping artwork** / action **Cancel**), but Settings currently redirects users to ES-DE. Wizard CTAs (when reachable): `Select all` / `Select none` / `Next` / `Back` / scrape options (filter, retry threshold/delay) / `Cancel` / `Done`; outcomes `Scrape finished` / `Scrape cancelled`.

**User benefit (today):** Honest under-construction messaging + clear alternative path.

### RetroAchievements settings

| Element | Copy |
|---------|------|
| Help | Same login as RetroArch; ORGL reads progress only |
| Disk load | `Loaded credentials from the ORGL data folder.` |
| Switch | `Store on disk` → encrypted `ra_credentials.json` |
| CTA | **`Save & verify`** |
| Success | `Connected. Your achievement progress will appear when you open a game.` |
| Error | e.g. `Invalid username or password` |

**User benefit:** Portable encrypted credentials + verified login before expecting progress UI.

### HowLongToBeat settings

| Element | Copy |
|---------|------|
| Help | Unofficial endpoint; cached; offline-safe |
| Switch | `Show HLTB estimates in Play Mode` |

**User benefit:** Optional time estimates on the Now Playing screen without cluttering Setup.

### RetroArch settings

| Element | Copy |
|---------|------|
| Help | Preferred package for launches; known build or Custom |
| Field | `Package` · `Select package` · `Custom…` / `Custom package name` |
| Known pkgs | `com.retroarch.aarch64` · `com.retroarch` · `com.retroarch.ra32` |

**User benefit:** Match the RetroArch build installed on the device (64-bit / 32-bit / custom forks).

### Multiple Now Playing Slots

| Element | Copy |
|---------|------|
| Title | `Multiple Now Playing Slots` |
| Body | Focused play, but multiple independent commitments allowed; finishing/dropping one slot doesn’t affect others |
| Slot bar tip | Switch between active runs; empty slots filled from picker without disturbing others |
| Chips | `1 slot` … `5 slots` |

**User benefit:** Compromise for users who want focus *and* a small concurrent set (e.g. short handheld + long RPG).

### Database (debug)

| Element | Copy |
|---------|------|
| Intro | Debug tools for local data / ORGL folder; more cleanup later |
| Sync section | Imports/exports RA credentials (if on disk) + Play journal; **launch stats stay on this device only** |
| CTA | **`Sync now`** / `Syncing…` |
| Prerequisite | `Link an ORGL data folder under Settings → Library → Folders first.` |

**User benefit:** Move journal/credentials with the ORGL data folder across devices/reinstalls.

### Credits

- Flavor label: `F-Droid / FOSS build` or `Google Play build`
- Author line: Created by **Sayem Shafayet**; GPL-3.0 one-game-at-a-time frontend — ES-DE compatible, external emulators only
- Project pointers: `oneretrogamelauncher.com` · `github.com/iShafayet/OneRetroGameLauncher`
- Data sources: ES-DE defs, ScreenScraper, libretro-thumbnails, RetroAchievements, HowLongToBeat
- Libraries: Compose, Material 3, Room, DataStore, Hilt, OkHttp, Coil, Coroutines
- **License:** focusable **`GNU GPL v3`** → opens [`LICENSE` on GitHub](https://github.com/iShafayet/OneRetroGameLauncher/blob/main/LICENSE) in the browser
- Version: `v{VERSION_NAME}`

---

## 8. Play mode (commitment loop)

Play mode is the product’s emotional core. Copy repeatedly reinforces lock-in, fairness, and non-shameful dropping.

### 8.1 Intro

| Element | Copy |
|---------|------|
| Label | `Play mode` |
| Headline | `One game.\nFull focus.` |
| Body | Built around commitment — not endless browsing; fair run; don’t move on until done |
| Rule 1 | **Pick one game** — Choose from suggestions or search your library. |
| Rule 2 | **Commit** — That game locks in — no switching until you release it. |
| Rule 3 | **Finish or drop** — Mark it done or move on. Then you can pick again. |
| **Primary CTA** | **`Let's pick a game to play`** |

**User benefit:** Ritualizes commitment before the library temptation appears.

### 8.2 Game picker

| Element | Copy |
|---------|------|
| Title | `Choose your game` |
| Help link | `How it works` (back to intro) |
| Banner | `One game locked in until you finish or drop it — then you can pick again.` |
| Empty library | `Scan your ROM folders in Setup first — then come back to commit to a game.` |
| Search | Placeholder `Search your library…` |
| No matches | `No matches — try another title or clear search to see suggestions.` |
| Section | **Tonight's trio** — subtitle: `Wishlist picks first when available; the rest are suggested at random.` |
| Badges | `Wishlist` · `Suggested` |
| CTA | **`Shuffle trio`** |
| Shelf | **Your shelf** — `Shortlisted games — max 5 in Setup.` (shown when non-empty) |

**Tonight’s trio rules (implementation):**

1. Slot 1 — Wishlist if any wishlisted games exist, else random **Suggested**
2. Slot 2 — Second Wishlist pick if wishlist size &gt; 10, else **Suggested**
3. Slot 3 — Always **Suggested** (random from remaining)

**User benefit:** Decision aid biased toward the user’s play queue, with shuffle for fresh suggestions — without a separate “wild card” path.

> Implementation note: shelf is shown in Play; DB/API support `onShelf`, but Setup UI for adding/removing shelf games may be incomplete. Wishlist is the primary Setup-managed play queue.

### 8.3 Confirm selection

| Element | Copy |
|---------|------|
| Title | `Confirm your pick` |
| Body | About to lock this game for Play slot N; other slots unaffected |
| Loading | `Loading game…` |
| Card label | `Selected game` |
| Rules header | `What happens next` |
| Rules | Play mode stays on this game only · Finish or drop to pick again · Setup mode stays available anytime |
| **Primary CTA** | **`Confirm selection`** |
| Secondary | **`Choose another game`** |

**User benefit:** Explicit consent before the lock — reduces accidental commitments.

### 8.4 Focus (Now playing)

| Element | Copy |
|---------|------|
| Label | `Now playing` / `Slot N` |
| Empty | `No game in this slot` · `Pick a game to commit to this slot. Other slots are unaffected.` · **`Pick a game`** |
| **Primary CTA** | **`Play`** (launches emulator) |
| RA | RetroAchievements button |
| Stats | `Last played` · `This run` |
| HLTB | `HLTB main: {hours}` (if enabled) |
| Menu | **`More options`** → `Mark as finished` · `Give up` · `History` |

#### Finish / drop dialogs

| Path | Title | Body | CTA |
|------|-------|------|-----|
| Finish | `Finished!` | `Nice run. Release the lock and pick your next game when you're ready.` | **`Release lock`** |
| Drop | `Give up?` | `That's okay — drop this one and you can pick something else.` | **`Release lock`** |
| Shared | | Star rating · `Review (optional)` | `Cancel` |

**User benefit:** Launch is one tap; ending a run is intentional and compassionate (especially drop copy). Reviews create lasting History value.

### 8.5 Completion / run card

| Context | Headline | Subtitle |
|---------|----------|----------|
| Just finished | `Run complete!` | `You finished {game}. Nice work — that's what Play mode is for.` |
| Just dropped | `Run ended` | `You dropped {game}. No shame — every run teaches you something.` |
| History finished | `Finished run` | `{game} · {system}` |
| History dropped | `Dropped run` | `{game} · {system}` |

| CTA | Behavior |
|-----|----------|
| **`Save run card to gallery`** | Saves collage → `Saved to gallery` / `Saved to Pictures/ORGL` |
| **`Start a new adventure`** | Clears completion and returns to Play picker flow |
| **`Back to history`** | History view only |

**User benefit:** Celebration artifact + gallery shareability + frictionless next commitment.

### 8.6 Multi-slot bar

- Labels like `Slot X of Y` · `In progress` / `Empty`
- Previous / Next slot affordances (content descriptions)
- Shoulder buttons (L1/R1) can cycle slots when multi-slot is enabled

**User benefit:** Parallel commitments without losing the per-slot lock model.

---

## 9. Mode-switch trust dialogs (philosophy enforcement)

### Play → Setup guard

| Element | Copy |
|---------|------|
| Title | `Heads up — you're on a run` / `Heads up — you have active runs` |
| Body | Setup is for maintenance (organize, settings, scrape) — **don't browse for something else to play** |
| Trust line | `Be on your best behavior. ORGL trusts you.` |
| Confirm | **`Enter Setup`** |
| Dismiss | **`Stay in Play`** |

**User benefit:** Soft accountability without hard-blocking Setup for legitimate maintenance.

### Setup launch while Play active

See Debug launch dialog in §4 — same philosophy, stricter friction (countdown).

---

## 10. Launch & emulator functionality

ORGL resolves how to start a game and hands off to an external app via intents / FileProvider URI grants.

### RetroArch

- Preferred package from Settings
- Core path validation with user-facing fix guidance, e.g.:
  - Install RetroArch from retroarch.com or F-Droid, then install cores via Online Updater
  - Install the core in RetroArch → Online Updater → Core Downloader
- Uses RetroArch activity extras for ROM + core

### Standalone emulators (supported launch targets)

Includes (among others): DuckStation, AetherSX2, NetherSX2, Dolphin, melonDS, Lemuroid, PPSSPP, Mupen64Plus FZ, Yaba Sanshiro 2, Flycast — using ES-DE-style intent patterns where applicable.

### Per-system / per-game config

- System default emulator + core
- Per-game override (emulator, core, optional custom config path)

**User benefit:** One launcher UX across many backends; fine-grained overrides when a game needs a special core.

### Disc / multi-file ROMs

Library scanning understands disc descriptors / related paths so multi-file sets resolve correctly for launch.

### Arcade titles

Asset map (`arcade/rom_map.json`) helps surface human-readable arcade titles.

---

## 11. Library scanning & media linking

### What a scan does

In one pass:

1. Walks ROMs tree by system folder / extensions from ES-DE system definitions
2. Indexes games into the local database
3. Links media from ORGL `downloaded_media/`
4. If ES-DE linked: imports gamelist metadata and `downloaded_media/` as read-only fallback
5. Reports unrecognized files

### Media providers (conceptual)

Local / ORGL / ES-DE / ScreenScraper (when scraping is enabled) / libretro-thumbnails fallback (scraper path).

**User benefit:** One “Rescan” CTA keeps catalog + artwork coherent after folder or ES-DE changes.

---

## 12. RetroAchievements (deep)

- Connect API login (`Save & verify`)
- Optional encrypted on-disk credentials in ORGL data folder (portable)
- Per-game progress UI (earned/total, points, hardcore, recent unlocks, list)
- ROM hashing / support evaluation (unsupported / sign-in-needed states with guidance like “Sign in under Settings → RetroAchievements”)
- Explicit product stance: **ORGL does not unlock achievements**; RetroArch (or other RA-capable clients) does

**User benefit:** Motivation and progress visibility without duplicating the unlock pipeline.

---

## 13. HowLongToBeat

- Optional toggle
- Unofficial endpoint, cached on device, graceful offline
- Surfaces on Focus as `HLTB main: …`

**User benefit:** Reality-check playtime before / during a commitment.

---

## 14. Hardware / gamepad / focus / IME

- Optional `android.hardware.gamepad` feature
- Focus helpers (`orlgFocusable`, `OrlgInitialFocus`, `orlgListFocus`) for D-pad navigation
- **Focus chrome:** soft logo-cyan ring / wash (`FocusRing`, `FocusFill`, `SelectionIndicator`) — quiet enough not to compete with content; grid tiles often draw their own border/scale
- **Filter chips / tabs:** `OrglFilterChip` and tab strips keep focus fill inside the control; L1/R1 hint badges sit inline on tab bars when a gamepad is connected
- Typical mappings:
  - Activate: Enter / D-pad Center / Space / A / Start
  - Back: B / Esc / Back — **dismisses IME first** (if open) before navigating
  - About (Setup hubs): X
  - Mode switch hint: Y
  - Shoulders L1/R1 (or PageUp/Down): cycle Setup tabs, Play slots, or Game detail tabs (game detail only — not media viewer)
- **Text fields (gamepad):** `orlgDpadFocusExit` — D-pad can leave the field; soft keyboard stays locked until **A / gamepad confirm** (`isGamepadConfirm`); touch focus still opens IME normally. Shared `OrglKeyboardOptions` + Done/Search dismiss helpers on search/single-line/password fields.
- Double-press back to exit on mode roots

**User benefit:** Couch / handheld use without hunting for a touchscreen; keyboards don’t fight D-pad navigation.

---

## 15. Data the user owns (mental model)

> Deep technical notes: [literature/README.md](./README.md)
> (`data-overview`, `database`, `app-storage`, `orgl-directory`).

### On device (Room + prefs)

- Systems, games, media links, emulator profiles
- Game flags: favorite, **wishlisted** (play queue), onShelf, completed status, notes
- Commitments (ACTIVE / FINISHED / DROPPED) + slot index
- Play sessions, reviews (stars, text, collage path)
- HLTB cache
- Theme, slot count, favorites / **wishlist** / recent toggles, integration settings
- Folder URIs/paths, onboarding flag, current app mode

### In ORGL data folder (portable)

| File / dir | Role |
|------------|------|
| `ORGL.json` | Spec marker |
| `downloaded_media/` | Scraped / ORGL media |
| `play_history.json` | Portable Play journal |
| `ra_credentials.json` | Encrypted RA credentials (optional) |
| Run card images | Collages for History / gallery |

### Never written by ORGL

- ROM files / ROM tree
- ES-DE application data (when linked)

**User benefit:** Clear trust boundary — “your ROMs, your media, your rules.”

---

## 16. Distribution flavors

| Flavor | Credits string |
|--------|----------------|
| **fdroid** (default Make build) | `F-Droid / FOSS build` |
| **play** | `Google Play build` |

Same application id; flavor-specific label only. License: GPL-3.0.

---

## 17. CTA & copy index (high-signal)

### Brand & promise

| Context | Exact copy |
|---------|------------|
| Tagline | Stop scrolling. Start finishing. |
| Positioning | One game at a time — the only launcher built around commitment, not infinite browsing. |
| Trust | ES-DE compatible. Your ROMs, your media, your rules. We never touch your files. |
| Welcome CTA | **Let's finish some games** |
| Enter app | **Enter ORGL** |

### Mode & commitment

| Context | Exact copy |
|---------|------------|
| Mode switch | Setup · Play |
| Play intro CTA | **Let's pick a game to play** |
| Commit CTA | **Confirm selection** |
| Escape hatch | Choose another game |
| Focus primary | **Play** |
| End run | Mark as finished · Give up · **Release lock** |
| Complete primary | **Start a new adventure** |
| Run card | **Save run card to gallery** |
| Guard | **Enter Setup** · Stay in Play |
| Setup launch bypass | I understand · Launch anyway |
| Tonight’s trio | Shuffle trio · badges Wishlist / Suggested |

### Library & settings

| Context | Exact copy |
|---------|------------|
| Rescan | **Rescan library** |
| Empty library | No games found — set your ROMs folder in Settings → Folders, then rescan. |
| Wishlist chip | Wishlist |
| Media empty | No media found for this game. |
| ScreenScraper | Under construction — use ES-DE for media |
| RA connect | **Save & verify** |
| RA success | Connected. Your achievement progress will appear when you open a game. |
| License | GNU GPL v3 (opens GitHub LICENSE) |
| Exit | Press again to exit |

### Tone notes (product voice)

- **Commitment without guilt:** drop dialogs are kind (`No shame — every run teaches you something.`)
- **Trust with accountability:** `Be on your best behavior. ORGL trusts you.`
- **Setup vs Play separation:** Setup is maintenance; Play is the intentional run
- **Ownership & safety:** repeated “read-only” / “never writes” language around ROMs and ES-DE

---

## 18. Known incomplete / in-progress surfaces

1. **ScreenScraper UI** — Settings + placeholder screen say under construction; scrape wizard/service still in codebase but not the recommended path today.
2. **Shelf management in Setup** — Play shows “Your shelf”; Setup shortlist UI may be missing (Wishlist is the shipped play-queue UX).
3. **ScreenScraper credential form** — prefs may exist; Settings form not shipped in the placeholder screen.
4. **Database debug** — copy promises more cleanup options later.

---

## 19. End-to-end user journeys (benefit map)

### Journey A — First install to first finished game

1. Welcome → **Let's finish some games**
2. Link ROMs + ORGL folders → **Continue**
3. Optional RA / ES-DE → **Skip** or **Continue**
4. **Enter ORGL** (scan completes)
5. Switch to **Play** → **Let's pick a game to play**
6. Pick from Tonight’s trio / shelf / search → **Confirm selection**
7. **Play** → play in emulator → return
8. **Mark as finished** → rate/review → **Release lock**
9. Celebrate → optional **Save run card to gallery** → **Start a new adventure**

**Benefit:** Guided path from empty install to a completed commitment with a keepsake.

### Journey B — ES-DE power user

1. Onboard or Settings → link ES-DE data
2. **Rescan library**
3. Browse Setup library with artwork/metadata already present; open Media gallery → fullscreen viewer as needed
4. Optionally still use Play commitment loop

**Benefit:** Zero re-scrape to get a beautiful library.

### Journey C — Maintenance during an active run

1. On a run in Play
2. Switch to Setup → guard dialog → **Enter Setup** (or **Stay in Play**)
3. Tweak folders/settings/scrape intent without changing the locked game
4. Return to Play Focus → **Play** continues the same commitment

**Benefit:** Maintenance without abandoning the lock philosophy.

### Journey D — Build a play queue

1. In Setup, open games → mark **Wishlist** (and optionally Favorite)
2. Enable **Wishlist section** in Appearance if desired
3. Switch to Play → Tonight’s trio prefers wishlist titles
4. **Shuffle trio** until a pick feels right → **Confirm selection**

**Benefit:** Curate “play next” separately from permanent favorites.

---

## 20. One-line product summary

**ORGL helps retro collectors stop doom-scrolling their ROM libraries by committing to one game at a time (with a wishlist play queue feeding suggestions), launching it in their preferred external emulator, optionally enriching it with ES-DE/RA/HLTB data and a modern media gallery, and journaling finished or dropped runs — all while keeping ROMs read-only and app data portable.**
