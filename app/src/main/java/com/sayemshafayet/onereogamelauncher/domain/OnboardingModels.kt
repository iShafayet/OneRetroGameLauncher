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
    BEGINNER_COMING_SOON,
    /** @deprecated Migrated to [BEGINNER_ORGL] on resume. */
    BEGINNER_STUB,
}

data class DetectedEmulator(
    val key: String,
    val label: String,
    val packageName: String,
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
