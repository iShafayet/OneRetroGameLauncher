package com.sayemshafayet.onereogamelauncher.systems

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EsSystemsParserTest {
    private val parser = EsSystemsParser()

    @Test
    fun parseSystems_extractsFolderExtensionsAndCore() {
        val xml = """
            <?xml version="1.0"?>
            <systemList>
              <system>
                <name>snes</name>
                <fullname>Nintendo SNES</fullname>
                <path>%ROMPATH%/snes</path>
                <extension>.smc .SMC .sfc .SFC .zip .ZIP</extension>
                <command label="Snes9x">%EMULATOR_RETROARCH% %EXTRA_LIBRETRO%=snes9x_libretro_android.so %EXTRA_ROM%=%ROM%</command>
                <platform>snes</platform>
                <theme>snes</theme>
              </system>
            </systemList>
        """.trimIndent()

        val systems = parser.parseSystems(ByteArrayInputStream(xml.toByteArray()))
        assertEquals(1, systems.size)
        val snes = systems.first()
        assertEquals("snes", snes.name)
        assertEquals("snes", snes.folder)
        assertTrue(snes.extensions.contains("smc"))
        assertTrue(snes.extensions.contains("zip"))
        assertEquals("RETROARCH", parser.defaultEmulatorKeyFromCommands(snes.commands))
        assertEquals("snes9x_libretro_android.so", parser.defaultCoreFromCommands(snes.commands))
    }

    @Test
    fun parseFindRules_readsPackageActivityEntries() {
        val xml = """
            <?xml version="1.0"?>
            <ruleList>
              <emulator name="RETROARCH">
                <rule type="androidpackage">
                  <entry>com.retroarch.aarch64/com.retroarch.browser.retroactivity.RetroActivityFuture</entry>
                  <entry>com.retroarch/com.retroarch.browser.retroactivity.RetroActivityFuture</entry>
                </rule>
              </emulator>
            </ruleList>
        """.trimIndent()

        val rules = parser.parseFindRules(ByteArrayInputStream(xml.toByteArray()))
        assertEquals(1, rules.size)
        val entries = rules.getValue("RETROARCH")
        assertEquals(2, entries.size)
        assertEquals("com.retroarch.aarch64", entries[0].packageName)
    }
}
