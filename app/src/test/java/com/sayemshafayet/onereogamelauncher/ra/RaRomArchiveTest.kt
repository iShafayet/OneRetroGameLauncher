package com.sayemshafayet.onereogamelauncher.ra

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RaRomArchiveTest {

    @Test
    fun unwrapArchive_extractsFirstRomFromZip() {
        val rom = ByteArray(512 + 1024) { it.toByte() }
        val zip = zipOf("game.smc" to rom)
        val extracted = RaRomArchive.unwrapArchive(zip)
        assertArrayEquals(rom, extracted)
    }

    @Test
    fun unwrapArchive_passesThroughNonZip() {
        val raw = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        assertArrayEquals(raw, RaRomArchive.unwrapArchive(raw))
    }

    @Test
    fun looksLikeZip_detectsPkHeader() {
        assertTrue(RaRomArchive.looksLikeZip(byteArrayOf(0x50, 0x4B, 0x03, 0x04)))
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            entries.forEach { (name, data) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
