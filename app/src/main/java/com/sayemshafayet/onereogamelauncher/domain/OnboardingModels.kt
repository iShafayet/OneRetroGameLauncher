package com.sayemshafayet.onereogamelauncher.domain

/**
 * First-run path chosen at the onboarding fork.
 * [NONE] means the user has not chosen yet.
 */
enum class OnboardingType {
    NONE,
    BEGINNER,
    PRO,
}

/**
 * Persisted wizard position for resume. Welcome / welcome-resume are derived
 * from [com.sayemshafayet.onereogamelauncher.data.prefs.AppSettings.onboardingStarted]
 * and are not stored as steps.
 */
enum class OnboardingStep {
    TOS,
    FORK,
    PRO_ROMS,
    PRO_ORGL,
    PRO_RA,
    PRO_ESDE,
    PRO_SCAN,
    PRO_EMULATORS,
    PRO_DONE,
    BEGINNER_ORGL,
    BEGINNER_HAVE_ROMS,
    BEGINNER_ROMS_SETUP,
    BEGINNER_ROMS_SUMMARY,
    BEGINNER_NO_ROMS_HELP,
    BEGINNER_FREE_GAMES,
    BEGINNER_EMULATORS,
    BEGINNER_TRY_LAUNCH,
    BEGINNER_ADVANCED,
    BEGINNER_DONE,
    /** @deprecated Migrated to [BEGINNER_ADVANCED] on resume. */
    BEGINNER_COMING_SOON,
    /** @deprecated Migrated to [BEGINNER_ORGL] on resume. */
    BEGINNER_STUB,
}

data class DetectedEmulator(
    val key: String,
    val label: String,
    val packageName: String,
    /** RetroArch only: core filenames when the query broadcast succeeded. */
    val installedCores: List<String>? = null,
    /** RetroArch only: false when the cores broadcast timed out / unsupported. */
    val coreQuerySupported: Boolean? = null,
)

/** Recommended RetroArch core for a library system during beginner onboarding. */
data class BeginnerSystemCoreNeed(
    val displayName: String,
    val folderName: String,
    /** Friendly recommended core name (falls back to filename). */
    val recommendedCoreLabel: String?,
    /** Friendly name of a different installed core for this system, if any. */
    val foundCoreLabel: String?,
    /** true when the recommended core is installed; null when we could not query. */
    val recommendedInstalled: Boolean?,
)

/** A game + installed emulator we can offer to try during beginner onboarding. */
data class BeginnerTryLaunchOffer(
    val gameId: Long,
    val gameTitle: String,
    val systemDisplayName: String,
    val emulatorKey: String,
    val emulatorLabel: String,
    val isRetroArch: Boolean,
    val coreFileName: String?,
)

/** Result of a shallow ROMs-root layout check (immediate child folders only). */
data class RomsStructureCheck(
    val matchedFolders: List<String>,
    val childDirectoryNames: List<String>,
    val isValid: Boolean,
)

data class BeginnerSystemPreview(
    val displayName: String,
    val folderName: String,
    val gameCount: Int,
    /** Up to two example titles found for this system. */
    val sampleTitles: List<String>,
)

data class BeginnerLibraryPreview(
    val systems: List<BeginnerSystemPreview>,
    val gamesFound: Int,
)

data class FreeHomebrewGame(
    val id: String,
    val title: String,
    val systemFolder: String,
    val fileName: String,
    val downloadUrl: String,
    val homepageUrl: String,
    val blurb: String,
)
