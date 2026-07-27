package com.sayemshafayet.onereogamelauncher.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LegalDocTitle("Privacy Policy — One Retro Game Launcher (ORGL)")
        LegalBody("Last updated: July 27, 2026")
        LegalBody("Effective for app version: 0.3.x and later")
        LegalBody(
            "Sayem Shafayet (“we”, “us”, “the developer”) operates One Retro Game Launcher " +
                "(“ORGL”, “the app”). This policy explains what ORGL does and does not do with " +
                "information on your device.",
        )
        LegalBody("If you do not agree with this policy, do not use the app.")

        LegalDocDivider()
        LegalSectionHeading("Summary")
        LegalBulletList(
            listOf(
                "ORGL includes no analytics, ads, crash reporters, or telemetry SDKs.",
                "ORGL does not sell your data.",
                "Your ROM library, play journal, and settings stay on your device and in folders you choose.",
                "Network access is used only when you enable optional features (for example RetroAchievements or artwork lookup).",
                "Third-party services you connect to may collect data under their own policies — that is outside ORGL’s control.",
            ),
        )

        LegalDocDivider()
        LegalSectionHeading("Information ORGL stores locally")
        LegalBody("ORGL may store the following on your device:")
        LegalTable(
            column1Header = "Data",
            column2Header = "Purpose",
            rows = listOf(
                "ROM and media file references" to "Library browsing and launching games you already have",
                "Game metadata and artwork" to "Display in the library and Play mode",
                "Play journal and launch stats" to "ORGL’s “one game at a time” workflow",
                "App preferences" to "Theme, layout, emulator choices, integrations",
                "Folder access URIs" to "Access ROM and ORGL data folders you select via Android’s Storage Access Framework",
                "Optional integration credentials" to "Only if you enter them (RetroAchievements, ScreenScraper, etc.)",
            ),
        )
        LegalBody("ORGL does not bundle ROMs and does not upload your ROM files to the developer.")
        LegalBody(
            "Scraped artwork and ORGL-owned files are written only to the ORGL data folder you " +
                "designate. Your ROMs folder is treated as read-only.",
        )

        LegalDocDivider()
        LegalSectionHeading("Network use and optional third-party services")
        LegalBody(
            "ORGL requests the INTERNET permission because some features contact external services " +
                "only when you turn them on or use them:",
        )
        LegalTable(
            column1Header = "Service",
            column2Header = "When used / What may be sent",
            rows = listOf(
                "RetroAchievements" to "If you sign in — username, password, and API requests to retroachievements.org",
                "ScreenScraper" to "If configured — account details and game search terms to screenscraper.fr",
                "HowLongToBeat" to "If enabled — game title search queries to howlongtobeat.com",
                "libretro-thumbnails" to "During artwork fallback — public thumbnail URLs (GitHub)",
            ),
        )
        LegalBody(
            "These services are operated by third parties. They may log requests, use cookies, or apply " +
                "their own analytics or telemetry. ORGL does not control and is not responsible for " +
                "third-party data practices. Review their policies before use:",
        )
        LegalUrlLink("RetroAchievements", "https://retroachievements.org")
        LegalUrlLink("ScreenScraper", "https://www.screenscraper.fr")
        LegalUrlLink("HowLongToBeat", "https://howlongtobeat.com")
        LegalBody("If you do not configure these integrations, ORGL does not need to contact them for your account.")

        LegalDocDivider()
        LegalSectionHeading("What ORGL does not collect")
        LegalBody("ORGL does not:")
        LegalBulletList(
            listOf(
                "Run Firebase, Google Analytics, or similar tracking",
                "Display advertisements",
                "Fingerprint your device for marketing",
                "Upload your library or ROM contents to developer-operated servers",
                "Require an ORGL account or cloud login",
            ),
        )
        LegalBody("There is no developer-operated backend that receives your personal library data.")

        LegalDocDivider()
        LegalSectionHeading("Permissions")
        LegalTable(
            column1Header = "Permission",
            column2Header = "Why",
            rows = listOf(
                "Internet" to "Optional integrations and artwork download",
                "Notifications" to "Scrape progress (you can deny on Android 13+)",
                "Foreground service" to "Long-running library scrape jobs",
                "Wake lock" to "Keep scrape jobs running while the screen is off",
            ),
        )
        LegalBody(
            "Folder access uses Android’s document picker; ORGL does not request broad storage " +
                "permissions for arbitrary file access.",
        )

        LegalDocDivider()
        LegalSectionHeading("Children’s privacy")
        LegalBody(
            "ORGL is a general-purpose game library tool and is not directed at children under 13. " +
                "We do not knowingly collect personal information from children. If you believe a child " +
                "has provided integration credentials in the app, remove them in Settings or uninstall the app.",
        )

        LegalDocDivider()
        LegalSectionHeading("Data retention and deletion")
        LegalBody("You can delete ORGL-related data by:")
        LegalBulletList(
            listOf(
                "Clearing app data or uninstalling ORGL",
                "Removing folder access in Android settings",
                "Deleting files inside your ORGL data folder",
            ),
        )
        LegalBody(
            "Integration credentials stored in the ORGL data folder can be removed there or by clearing saved settings.",
        )

        LegalDocDivider()
        LegalSectionHeading("Security")
        LegalBody(
            "Credentials you save (for example RetroAchievements) may be stored in app preferences and, " +
                "if you opt in, encrypted in your ORGL data folder on disk. No method of storage is " +
                "perfectly secure; use integrations at your own discretion.",
        )

        LegalDocDivider()
        LegalSectionHeading("Open source")
        LegalBody("ORGL is open source under the GNU General Public License v3.0. Source code is published at:")
        LegalUrlLink("GitHub repository", "https://github.com/iShafayet/OneRetroGameLauncher")

        LegalDocDivider()
        LegalSectionHeading("Changes to this policy")
        LegalBody(
            "We may update this policy from time to time. The “Last updated” date will change when we do. " +
                "Continued use after an update means you accept the revised policy, subject to any in-app " +
                "re-acceptance flow.",
        )

        LegalDocDivider()
        LegalSectionHeading("Contact")
        LegalContactBlock()
        Spacer(Modifier.height(4.dp))
        LegalBody(
            "For privacy questions, open an issue on GitHub or contact the developer through the project website.",
        )
    }
}
