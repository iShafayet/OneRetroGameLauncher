package com.sayemshafayet.onereogamelauncher.launch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetroArchCoreMatchTest {
    @Test
    fun coresMatch_acceptsAndroidSuffixVariants() {
        assertTrue(
            RetroArchLauncher.coresMatch(
                "mgba_libretro_android.so",
                "mgba_libretro_android.so",
            ),
        )
        assertTrue(
            RetroArchLauncher.coresMatch(
                "mgba_libretro_android.so",
                "mgba_libretro.so",
            ),
        )
        assertTrue(
            RetroArchLauncher.coresMatch(
                "snes9x_libretro.so",
                "snes9x_libretro_android.so",
            ),
        )
    }

    @Test
    fun coresMatch_rejectsDifferentCores() {
        assertFalse(
            RetroArchLauncher.coresMatch(
                "mgba_libretro_android.so",
                "nestopia_libretro_android.so",
            ),
        )
    }
}
