package com.sayemshafayet.onereogamelauncher.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TermsOfServiceContent(
    modifier: Modifier = Modifier,
    onOpenPrivacy: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LegalDocTitle("Terms of Service — One Retro Game Launcher (ORGL)")
        LegalBody("Last updated: July 27, 2026")
        LegalBody(
            "These Terms of Service (“Terms”) govern your use of One Retro Game Launcher " +
                "(“ORGL”, “the app”) provided by Sayem Shafayet (“we”, “us”, “the developer”).",
        )
        if (onOpenPrivacy != null) {
            LegalInlinePrivacyLink(onOpenPrivacy)
        } else {
            LegalBody(
                "By installing or using ORGL, you agree to these Terms and to our Privacy Policy. " +
                    "If you do not agree, do not use the app.",
            )
        }

        LegalDocDivider()
        LegalSectionHeading("1. What ORGL is")
        LegalBody(
            "ORGL is a game library launcher and organizer for retro titles. It helps you browse " +
                "games you already have, manage a “one game at a time” play workflow, and launch " +
                "external emulators you install separately.",
        )
        LegalBody("ORGL is not an emulator and does not include ROMs, BIOS files, or copyrighted game content.")

        LegalDocDivider()
        LegalSectionHeading("2. Your content and legal responsibility")
        LegalBody("You are solely responsible for:")
        LegalBulletList(
            listOf(
                "Games, ROMs, disc images, and media you point ORGL at",
                "Ensuring you have the legal right to use that content in your jurisdiction",
                "Emulators and apps you install and launch from ORGL",
            ),
        )
        LegalBody("We do not encourage or facilitate piracy. ORGL only reads files and folders you select.")

        LegalDocDivider()
        LegalSectionHeading("3. External software and services")
        LegalBody(
            "ORGL may open or integrate with third-party emulators and online services (RetroArch, " +
                "standalone emulators, RetroAchievements, ScreenScraper, HowLongToBeat, ES-DE-compatible " +
                "folders, etc.).",
        )
        LegalBody(
            "Those products are not controlled by us. Their availability, behavior, terms, and privacy " +
                "practices are your responsibility to review. We are not liable for third-party services, " +
                "including any downtime, data loss, account issues, or policy changes.",
        )

        LegalDocDivider()
        LegalSectionHeading("4. No warranty")
        LegalBody(
            "ORGL is provided “as is” and “as available”, without warranties of any kind, express or " +
                "implied, including merchantability, fitness for a particular purpose, and non-infringement.",
        )
        LegalBody(
            "Library scans, artwork downloads, achievement sync, and game launches may fail due to device " +
                "configuration, permissions, emulator setup, or network conditions.",
        )

        LegalDocDivider()
        LegalSectionHeading("5. Limitation of liability")
        LegalBody(
            "To the maximum extent permitted by law, the developer shall not be liable for any indirect, " +
                "incidental, special, consequential, or punitive damages, or any loss of data, profits, or " +
                "goodwill, arising from your use of ORGL or any emulator or third-party service launched through it.",
        )
        LegalBody(
            "Our total liability for any claim relating to ORGL shall not exceed the amount you paid for the " +
                "app (typically zero for the free/open-source distribution).",
        )

        LegalDocDivider()
        LegalSectionHeading("6. Acceptable use")
        LegalBody("You agree not to use ORGL to:")
        LegalBulletList(
            listOf(
                "Violate applicable laws or third-party rights",
                "Attempt to reverse engineer the app beyond rights granted by the GPL-3.0 license",
                "Interfere with the app’s normal operation or other users’ systems (where applicable)",
            ),
        )

        LegalDocDivider()
        LegalSectionHeading("7. Open-source license")
        LegalBody(
            "ORGL’s source code is licensed under the GNU General Public License v3.0. These Terms apply " +
                "to use of the distributed app; software freedom under the GPL is described in the LICENSE " +
                "file and at:",
        )
        LegalUrlLink(
            "GPL-3.0 LICENSE",
            "https://github.com/iShafayet/OneRetroGameLauncher/blob/main/LICENSE",
        )
        LegalBody(
            "If there is a conflict between these Terms and the GPL regarding the source code, the GPL " +
                "governs the source code; these Terms govern your use of the prebuilt app as a user.",
        )

        LegalDocDivider()
        LegalSectionHeading("8. Updates and changes")
        LegalBody(
            "We may update ORGL and these Terms from time to time. Material changes may require you to " +
                "accept updated Terms inside the app before continuing. The “Last updated” date reflects " +
                "the current version.",
        )

        LegalDocDivider()
        LegalSectionHeading("9. Termination")
        LegalBody(
            "You may stop using ORGL at any time by uninstalling it. We may discontinue the project or " +
                "distribution channels without notice, subject to open-source obligations for published source code.",
        )

        LegalDocDivider()
        LegalSectionHeading("10. Governing law")
        LegalBody(
            "These Terms are governed by the laws applicable in the developer’s jurisdiction, without regard " +
                "to conflict-of-law rules, except where mandatory consumer protection laws in your country " +
                "provide otherwise.",
        )

        LegalDocDivider()
        LegalSectionHeading("11. Contact")
        LegalContactBlock()
    }
}
