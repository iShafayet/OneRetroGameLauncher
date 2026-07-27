# Privacy Policy — One Retro Game Launcher (ORGL)

**Last updated:** July 27, 2026  
**Effective for app version:** 0.3.x and later

Sayem Shafayet (“we”, “us”, “the developer”) operates **One Retro Game Launcher** (“ORGL”, “the app”). This policy explains what ORGL does and does not do with information on your device.

If you do not agree with this policy, do not use the app.

---

## Summary

- ORGL includes **no analytics, ads, crash reporters, or telemetry SDKs**.
- ORGL does **not** sell your data.
- Your ROM library, play journal, and settings stay **on your device** and in folders **you choose**.
- Network access is used **only when you enable optional features** (for example RetroAchievements or artwork lookup).
- Third-party services you connect to may collect data under **their own** policies — that is outside ORGL’s control.

---

## Information ORGL stores locally

ORGL may store the following on your device:

| Data | Purpose |
|------|---------|
| ROM and media file references | Library browsing and launching games you already have |
| Game metadata and artwork | Display in the library and Play mode |
| Play journal and launch stats | ORGL’s “one game at a time” workflow |
| App preferences | Theme, layout, emulator choices, integrations |
| Folder access URIs | Access ROM and ORGL data folders you select via Android’s Storage Access Framework |
| Optional integration credentials | Only if you enter them (RetroAchievements, ScreenScraper, etc.) |

ORGL **does not bundle ROMs** and **does not upload your ROM files** to the developer.

Scraped artwork and ORGL-owned files are written only to the **ORGL data folder** you designate. Your ROMs folder is treated as **read-only**.

---

## Network use and optional third-party services

ORGL requests the `INTERNET` permission because some features contact external services **only when you turn them on or use them**:

| Service | When used | What may be sent |
|---------|-----------|------------------|
| **RetroAchievements** | If you sign in | Username, password, and API requests to retroachievements.org |
| **ScreenScraper** | If configured | Account details and game search terms to screenscraper.fr |
| **HowLongToBeat** | If enabled | Game title search queries to howlongtobeat.com |
| **libretro-thumbnails** | During artwork fallback | Public thumbnail URLs (GitHub) |

These services are operated by third parties. They may log requests, use cookies, or apply their own analytics or telemetry. **ORGL does not control and is not responsible for third-party data practices.** Review their policies before use:

- RetroAchievements: https://retroachievements.org  
- ScreenScraper: https://www.screenscraper.fr  
- HowLongToBeat: https://howlongtobeat.com  

If you do not configure these integrations, ORGL does not need to contact them for your account.

---

## What ORGL does not collect

ORGL does **not**:

- Run Firebase, Google Analytics, or similar tracking
- Display advertisements
- Fingerprint your device for marketing
- Upload your library or ROM contents to developer-operated servers
- Require an ORGL account or cloud login

There is no developer-operated backend that receives your personal library data.

---

## Permissions

| Permission | Why |
|------------|-----|
| **Internet** | Optional integrations and artwork download |
| **Notifications** | Scrape progress (you can deny on Android 13+) |
| **Foreground service** | Long-running library scrape jobs |
| **Wake lock** | Keep scrape jobs running while the screen is off |

Folder access uses Android’s document picker; ORGL does not request broad storage permissions for arbitrary file access.

---

## Children’s privacy

ORGL is a general-purpose game library tool and is **not directed at children under 13**. We do not knowingly collect personal information from children. If you believe a child has provided integration credentials in the app, remove them in Settings or uninstall the app.

---

## Data retention and deletion

You can delete ORGL-related data by:

- Clearing app data or uninstalling ORGL
- Removing folder access in Android settings
- Deleting files inside your ORGL data folder

Integration credentials stored in the ORGL data folder can be removed there or by clearing saved settings.

---

## Security

Credentials you save (for example RetroAchievements) may be stored in app preferences and, if you opt in, encrypted in your ORGL data folder on disk. No method of storage is perfectly secure; use integrations at your own discretion.

---

## Open source

ORGL is open source under the **GNU General Public License v3.0**. Source code is published at:

https://github.com/iShafayet/OneRetroGameLauncher

---

## Changes to this policy

We may update this policy from time to time. The “Last updated” date will change when we do. Continued use after an update means you accept the revised policy, subject to any in-app re-acceptance flow.

---

## Contact

**Sayem Shafayet**  
Website: https://sayemshafayet.com  
Project: https://oneretrogamelauncher.com  
GitHub: https://github.com/iShafayet/OneRetroGameLauncher

For privacy questions, open an issue on GitHub or contact the developer through the project website.
