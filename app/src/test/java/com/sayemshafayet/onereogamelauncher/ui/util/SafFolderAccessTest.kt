package com.sayemshafayet.onereogamelauncher.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SafFolderAccessTest {
    @Test
    fun decideAction_skipsWhenOnboardingNotDone() {
        assertEquals(
            SafFolderAccess.Action.None,
            SafFolderAccess.decideAction(
                onboardingDone = false,
                esdeConfigured = true,
                romsOk = false,
                orglOk = false,
                esdeOk = false,
            ),
        )
    }

    @Test
    fun decideAction_resetsWhenRequiredFolderLost() {
        assertEquals(
            SafFolderAccess.Action.ResetForReOnboarding,
            SafFolderAccess.decideAction(
                onboardingDone = true,
                esdeConfigured = true,
                romsOk = false,
                orglOk = true,
                esdeOk = true,
            ),
        )
        assertEquals(
            SafFolderAccess.Action.ResetForReOnboarding,
            SafFolderAccess.decideAction(
                onboardingDone = true,
                esdeConfigured = false,
                romsOk = true,
                orglOk = false,
                esdeOk = true,
            ),
        )
    }

    @Test
    fun decideAction_clearsEsdeOnlyWhenOptionalFolderLost() {
        assertEquals(
            SafFolderAccess.Action.ClearEsdeOnly,
            SafFolderAccess.decideAction(
                onboardingDone = true,
                esdeConfigured = true,
                romsOk = true,
                orglOk = true,
                esdeOk = false,
            ),
        )
    }

    @Test
    fun decideAction_noneWhenAllReachable() {
        assertEquals(
            SafFolderAccess.Action.None,
            SafFolderAccess.decideAction(
                onboardingDone = true,
                esdeConfigured = true,
                romsOk = true,
                orglOk = true,
                esdeOk = true,
            ),
        )
        assertEquals(
            SafFolderAccess.Action.None,
            SafFolderAccess.decideAction(
                onboardingDone = true,
                esdeConfigured = false,
                romsOk = true,
                orglOk = true,
                esdeOk = false,
            ),
        )
    }
}
