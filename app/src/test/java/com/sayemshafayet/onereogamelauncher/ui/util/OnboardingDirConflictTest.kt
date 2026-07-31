package com.sayemshafayet.onereogamelauncher.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingDirConflictTest {
    @Test
    fun pathHints_detectNesting() {
        assertTrue(
            OnboardingDirConflict.conflicts(
                romsPath = "/storage/emulated/0/ROMs",
                orglPath = "/storage/emulated/0/ROMs/ORGL-Data",
                romsUri = null,
                orglUri = null,
            ),
        )
        assertFalse(
            OnboardingDirConflict.conflicts(
                romsPath = "/storage/emulated/0/ROMs",
                orglPath = "/storage/emulated/0/ORGL-Data",
                romsUri = null,
                orglUri = null,
            ),
        )
    }

    @Test
    fun documentIds_detectNestingWhenPathsMissing() {
        val roms = "content://com.android.externalstorage.documents/tree/primary%3AROMs"
        val nested =
            "content://com.android.externalstorage.documents/tree/primary%3AROMs%2FORGL-Data"
        val sibling =
            "content://com.android.externalstorage.documents/tree/primary%3AORGL-Data"

        assertTrue(
            OnboardingDirConflict.conflicts(
                romsPath = null,
                orglPath = null,
                romsUri = roms,
                orglUri = nested,
            ),
        )
        assertFalse(
            OnboardingDirConflict.conflicts(
                romsPath = null,
                orglPath = null,
                romsUri = roms,
                orglUri = sibling,
            ),
        )
        assertTrue(
            OnboardingDirConflict.conflicts(
                romsPath = null,
                orglPath = null,
                romsUri = roms,
                orglUri = roms,
            ),
        )
    }
}
