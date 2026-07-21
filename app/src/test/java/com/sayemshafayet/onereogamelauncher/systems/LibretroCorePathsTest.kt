package com.sayemshafayet.onereogamelauncher.systems

import org.junit.Assert.assertEquals
import org.junit.Test

class LibretroCorePathsTest {
    @Test
    fun coreFileNameFromExtra_stripsLegacyDataDataPath() {
        assertEquals(
            "mesen_libretro_android.so",
            LibretroCorePaths.coreFileNameFromExtra(
                "/data/data/%ANDROIDPACKAGE%/cores/mesen_libretro_android.so",
            ),
        )
    }

    @Test
    fun coreFileNameFromExtra_stripsInternalDataPlaceholder() {
        assertEquals(
            "nestopia_libretro_android.so",
            LibretroCorePaths.coreFileNameFromExtra(
                "%INTERNALDATA%/%ANDROIDPACKAGE%/cores/nestopia_libretro_android.so",
            ),
        )
    }

    @Test
    fun coreFileNameFromExtra_keepsBareFilename() {
        assertEquals(
            "fceumm_libretro_android.so",
            LibretroCorePaths.coreFileNameFromExtra("fceumm_libretro_android.so"),
        )
    }
}
