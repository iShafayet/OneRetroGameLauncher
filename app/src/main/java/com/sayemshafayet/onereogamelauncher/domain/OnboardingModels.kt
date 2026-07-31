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
    BEGINNER_STUB,
}

data class DetectedEmulator(
    val key: String,
    val label: String,
    val packageName: String,
)
