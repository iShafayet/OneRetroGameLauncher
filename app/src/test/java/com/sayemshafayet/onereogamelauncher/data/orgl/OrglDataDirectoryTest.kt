package com.sayemshafayet.onereogamelauncher.data.orgl

import org.junit.Assert.assertTrue
import org.junit.Test

class OrglDataDirectoryTest {
    @Test
    fun incompatibleMessage_mentionsVersions() {
        val msg = OrglDataDirectory.incompatibleMessage(99)
        assertTrue(msg.contains("99"))
        assertTrue(msg.contains(OrglDataDirectory.SPEC_VERSION.toString()))
        assertTrue(msg.contains(OrglDataDirectory.META_FILE_NAME))
    }

    @Test
    fun specVersion_isOne() {
        assertTrue(OrglDataDirectory.SPEC_VERSION == 1)
    }
}
