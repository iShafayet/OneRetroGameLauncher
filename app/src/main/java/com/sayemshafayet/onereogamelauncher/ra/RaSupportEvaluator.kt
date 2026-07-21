package com.sayemshafayet.onereogamelauncher.ra

import com.sayemshafayet.onereogamelauncher.domain.RaButtonState
import com.sayemshafayet.onereogamelauncher.domain.RaLookupResult
import com.sayemshafayet.onereogamelauncher.domain.RaResult
import com.sayemshafayet.onereogamelauncher.domain.RaVisualState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RaSupportEvaluator @Inject constructor() {

    fun evaluateButton(
        signedIn: Boolean,
        lookupResult: RaResult<RaLookupResult>?,
        effectiveEmulatorKey: String,
        romHashable: Boolean,
    ): RaButtonState {
        if (!signedIn) {
            return RaButtonState(
                visual = RaVisualState.SIGN_IN_REQUIRED,
                subtitle = "Sign in under Settings → RetroAchievements",
                enabled = true,
            )
        }

        return when (lookupResult) {
            null -> RaButtonState(
                visual = RaVisualState.ERROR,
                subtitle = "Could not check RetroAchievements status",
                enabled = true,
            )
            is RaResult.Failed -> RaButtonState(
                visual = RaVisualState.ERROR,
                subtitle = lookupResult.message,
                enabled = true,
            )
            is RaResult.Unsupported -> RaButtonState(
                visual = RaVisualState.NO_GAME,
                subtitle = lookupResult.message,
                enabled = true,
            )
            is RaResult.Ok -> evaluateSupportedLookup(
                lookup = lookupResult.value,
                effectiveEmulatorKey = effectiveEmulatorKey,
                romHashable = romHashable,
            )
        }
    }

    private fun evaluateSupportedLookup(
        lookup: RaLookupResult,
        effectiveEmulatorKey: String,
        romHashable: Boolean,
    ): RaButtonState {
        if (lookup.totalAchievements <= 0) {
            return RaButtonState(
                visual = RaVisualState.NO_GAME,
                subtitle = "No published achievement set for this game",
                enabled = true,
            )
        }

        val usesRetroArch = effectiveEmulatorKey.equals("RETROARCH", ignoreCase = true)
        val romSupported = lookup.hashRecognized && romHashable

        return if (usesRetroArch && romSupported) {
            RaButtonState(
                visual = RaVisualState.READY,
                subtitle = "${lookup.totalAchievements} achievements on ${lookup.consoleName ?: "RA"}",
                enabled = true,
            )
        } else {
            val reason = when {
                !usesRetroArch && !romSupported ->
                    "Use RetroArch with a verified ROM to earn achievements"
                !usesRetroArch ->
                    "Achievements require launching through RetroArch"
                else ->
                    "ROM hash is not recognized by RetroAchievements"
            }
            RaButtonState(
                visual = RaVisualState.UNSUPPORTED_SETUP,
                subtitle = reason,
                enabled = true,
            )
        }
    }
}
