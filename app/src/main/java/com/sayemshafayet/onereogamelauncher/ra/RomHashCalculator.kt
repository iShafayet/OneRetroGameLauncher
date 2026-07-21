package com.sayemshafayet.onereogamelauncher.ra

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.sayemshafayet.onereogamelauncher.launch.RetroArchRomPaths
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray

/** Shared ROM archive handling for RA hashing (testable without Android storage). */
internal object RaRomArchive {
    fun unwrapArchive(bytes: ByteArray): ByteArray {
        if (!looksLikeZip(bytes)) return bytes
        return firstZipEntryBytes(bytes) ?: bytes
    }

    fun looksLikeZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte()

    fun firstZipEntryBytes(zipBytes: ByteArray): ByteArray? =
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            generateSequence { zis.nextEntry }
                .firstOrNull { entry ->
                    !entry.isDirectory && !entry.name.startsWith("__MACOSX")
                }
                ?.let { zis.readBytes() }
        }
}

@Singleton
class RomHashCalculator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consoleMapper: RaConsoleMapper,
) {
    companion object {
        private const val TAG = "RomHashCalculator"
        /** Avoid OOM when hashing very large arcade ROMs. */
        private const val MAX_ROM_BYTES = 64L * 1024L * 1024L
    }
    data class RomAccess(
        val romPath: String,
        val romPathsJson: String = "[]",
        val systemFolder: String? = null,
        val romsDirPath: String? = null,
        val romsTreeUri: String? = null,
    )

    /** Raw file MD5 — kept for legacy callers. */
    fun md5Hex(romPath: String): String? =
        raMd5Candidates(RomAccess(romPath)).firstOrNull()

    /**
     * Returns RA-compatible hash candidates, most preferred first.
     * RetroArch / rcheevos may try multiple hashes; we do the same for lookup.
     */
    fun raMd5Candidates(romPath: String, systemFolder: String?): List<String> =
        raMd5Candidates(RomAccess(romPath, systemFolder = systemFolder))

    fun raMd5Candidates(access: RomAccess): List<String> {
        if (access.romPath.isBlank()) return emptyList()
        val bytes = readRomBytes(access) ?: return emptyList()
        return raMd5CandidatesFromBytes(bytes, access.systemFolder)
    }

    fun raMd5CandidatesFromBytes(bytes: ByteArray, systemFolder: String?): List<String> {
        val consoleId = consoleMapper.consoleIdForFolder(systemFolder)
        val candidates = linkedSetOf<String>()

        raHashBytes(bytes, consoleId)?.let { candidates.add(it) }
        md5Bytes(bytes)?.let { candidates.add(it) }

        return candidates.toList()
    }

    fun isHashable(romPath: String, systemFolder: String? = null): Boolean =
        isHashable(RomAccess(romPath, systemFolder = systemFolder))

    fun isHashable(access: RomAccess): Boolean = raMd5Candidates(access).isNotEmpty()

    private fun readRomBytes(access: RomAccess): ByteArray? {
        for (path in candidatePaths(access)) {
            readRomBytesAtPath(path)?.let { return it }
        }
        Log.w(TAG, "Could not read ROM bytes for hashing (paths=${candidatePaths(access).size})")
        return null
    }

    private fun candidatePaths(access: RomAccess): List<String> {
        val paths = linkedSetOf<String>()
        if (access.romPath.isNotBlank()) paths.add(access.romPath)

        pathsFromJson(access.romPathsJson).forEach { entry ->
            paths.add(entry)
            if (!access.romsDirPath.isNullOrBlank()) {
                File(access.romsDirPath, entry).absolutePath.let { paths.add(it) }
                access.systemFolder?.let { folder ->
                    File(access.romsDirPath, "$folder/$entry").absolutePath.let { paths.add(it) }
                }
            }
        }

        RetroArchRomPaths.relativeFromRomsRoot(
            romPath = access.romPath,
            romPathsJson = access.romPathsJson,
            systemFolder = access.systemFolder.orEmpty(),
            romsDirPath = access.romsDirPath,
            romsTreeUri = access.romsTreeUri,
        )?.let { relative ->
            if (!access.romsDirPath.isNullOrBlank()) {
                paths.add(File(access.romsDirPath, relative).absolutePath)
            }
            if (!access.romsTreeUri.isNullOrBlank()) {
                documentUriForRelative(access.romsTreeUri, relative)?.let { paths.add(it) }
            }
        }

        return paths.filter { it.isNotBlank() }
    }

    private fun pathsFromJson(romPathsJson: String): List<String> =
        runCatching {
            val arr = JSONArray(romPathsJson)
            buildList {
                for (i in 0 until arr.length()) {
                    arr.optString(i)?.takeIf { it.isNotBlank() }?.let { add(it) }
                }
            }
        }.getOrDefault(emptyList())

    private fun documentUriForRelative(treeUri: String, relativePath: String): String? {
        val tree = RetroArchRomPaths.canonicalTreeUri(treeUri)
        val root = DocumentFile.fromTreeUri(context, Uri.parse(tree)) ?: return null
        var current: DocumentFile = root
        for (segment in relativePath.split('/').filter { it.isNotBlank() }) {
            current = current.findFile(segment) ?: return null
        }
        return current.uri.toString()
    }

    private fun readRomBytesAtPath(romPath: String): ByteArray? {
        if (romPath.isBlank()) return null
        val raw = when {
            romPath.startsWith("content:", ignoreCase = true) ->
                readContentUriRaw(Uri.parse(romPath))
            romPath.startsWith("saf://", ignoreCase = true) ->
                readSafPath(romPath)
            else -> readFilesystemRaw(romPath)
        } ?: return null
        return RaRomArchive.unwrapArchive(raw)
    }

    private fun readContentUriRaw(uri: Uri): ByteArray? {
        val size = DocumentFile.fromSingleUri(context, uri)?.length() ?: -1L
        if (size > MAX_ROM_BYTES) {
            Log.w(TAG, "Skipping hash — ROM too large ($size bytes)")
            return null
        }
        return readStream(context.contentResolver.openInputStream(uri))
    }

    private fun readSafPath(safPath: String): ByteArray? {
        if (!safPath.startsWith("saf://", ignoreCase = true)) return null
        val payload = safPath.removePrefix("saf://")
        val slash = payload.indexOf('/')
        if (slash <= 0) return null
        val treeUri = decodeSafTree(payload.substring(0, slash))
        val relative = payload.substring(slash + 1)
        val docUri = documentUriForRelative(treeUri, relative) ?: return null
        return readStream(context.contentResolver.openInputStream(Uri.parse(docUri)))
    }

    private fun decodeSafTree(encoded: String): String = buildString(encoded.length) {
        var i = 0
        while (i < encoded.length) {
            when {
                encoded.regionMatches(i, "%2F", 0, 3, ignoreCase = true) -> {
                    append('/')
                    i += 3
                }
                encoded.regionMatches(i, "%25", 0, 3, ignoreCase = true) -> {
                    append('%')
                    i += 3
                }
                else -> {
                    append(encoded[i])
                    i++
                }
            }
        }
    }

    private fun readFilesystemRaw(romPath: String): ByteArray? {
        val file = File(romPath)
        if (!file.isFile) return null
        if (file.length() > MAX_ROM_BYTES) {
            Log.w(TAG, "Skipping hash — ROM too large (${file.length()} bytes)")
            return null
        }
        return runCatching { file.readBytes() }.getOrNull()
    }

    private fun readStream(input: InputStream?): ByteArray? {
        if (input == null) return null
        return input.use { it.readBytes() }
    }

    private fun raHashBytes(bytes: ByteArray, consoleId: Int?): String? {
        if (bytes.isEmpty()) return null
        val payload = when (consoleId) {
            3 -> snesPayload(bytes)
            7, 81 -> nesPayload(bytes)
            8 -> pcePayload(bytes)
            13 -> lynxPayload(bytes)
            51 -> atari7800Payload(bytes)
            else -> bytes
        }
        return md5Bytes(payload)
    }

    private fun snesPayload(bytes: ByteArray): ByteArray {
        val base = (bytes.size / 0x2000) * 0x2000
        return if (bytes.size - base == 512) bytes.copyOfRange(512, bytes.size) else bytes
    }

    private fun nesPayload(bytes: ByteArray): ByteArray {
        return when {
            bytes.size >= 4 && bytes[0] == 'N'.code.toByte() && bytes[1] == 'E'.code.toByte() &&
                bytes[2] == 'S'.code.toByte() && bytes[3] == 0x1A.toByte() ->
                bytes.copyOfRange(16, bytes.size)
            bytes.size >= 4 && bytes[0] == 'F'.code.toByte() && bytes[1] == 'D'.code.toByte() &&
                bytes[2] == 'S'.code.toByte() && bytes[3] == 0x1A.toByte() ->
                bytes.copyOfRange(16, bytes.size)
            else -> bytes
        }
    }

    private fun pcePayload(bytes: ByteArray): ByteArray {
        return if (bytes.size and 512 != 0) bytes.copyOfRange(512, bytes.size) else bytes
    }

    private fun lynxPayload(bytes: ByteArray): ByteArray {
        return if (bytes.size >= 4 && bytes[0] == 'L'.code.toByte() && bytes[1] == 'Y'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() && bytes[3] == 'X'.code.toByte()
        ) {
            bytes.copyOfRange(64, bytes.size)
        } else {
            bytes
        }
    }

    private fun atari7800Payload(bytes: ByteArray): ByteArray {
        return if (bytes.size >= 10 &&
            bytes.copyOfRange(1, 10).contentEquals("ATARI7800".encodeToByteArray())
        ) {
            bytes.copyOfRange(128, bytes.size)
        } else {
            bytes
        }
    }

    private fun md5Bytes(data: ByteArray): String? {
        if (data.isEmpty()) return null
        val digest = MessageDigest.getInstance("MD5")
        digest.update(data)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
