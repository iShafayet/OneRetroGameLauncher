package com.sayemshafayet.onereogamelauncher.launch

import android.net.Uri
import android.provider.DocumentsContract
import com.sayemshafayet.onereogamelauncher.library.DiscDescriptorPaths
import com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver
import java.io.File
import kotlinx.serialization.json.Json

/**
 * Resolves ROM paths for standalone emulators (DuckStation, AetherSX2, …).
 *
 * Matches ES-DE Android behavior for AetherSX2/NetherSX2:
 * - `%EXTRA_bootPath%=%ROMSAF%` → DocumentsContract `content://` URI
 * - PS2 system config has **no `.cue` extension** — game entries are `.bin`/`.iso`/`.chd`
 * - For a single-bin `.cue` set we therefore boot the companion `.bin` content URI
 */
object StandaloneRomPaths {

    private val romPathsJsonParser = Json { ignoreUnknownKeys = true }

    fun resolve(
        romPath: String,
        romPathsJson: String,
        romsTreeUri: String?,
        romsDirPath: String? = null,
        systemFolder: String = "",
    ): RetroArchRomPaths.ResolvedRom {
        // Prefer content:// (ES-DE %ROMSAF%) whenever the library entry is SAF-backed.
        if (romPath.startsWith("content:", ignoreCase = true)) {
            resolveContentBootPath(romPath, romPathsJson, romsTreeUri, systemFolder)?.let {
                return it
            }
        }

        filesystemBootPath(romPath, romPathsJson, romsDirPath, systemFolder, romsTreeUri)?.let {
            return it
        }

        readableFile(romPath)?.let { return RetroArchRomPaths.ResolvedRom(romExtra = it) }

        firstReadableEntry(romPathsJson)?.let { return RetroArchRomPaths.ResolvedRom(romExtra = it) }

        if (romPath.startsWith("content:", ignoreCase = true)) {
            val tree = RetroArchRomPaths.treeUriFromDocumentUriString(romPath)
                ?: romsTreeUri?.let(RetroArchRomPaths::canonicalTreeUri)
            return RetroArchRomPaths.ResolvedRom(
                romExtra = romPath,
                grantTreeUri = tree,
                grantDocumentUri = romPath,
                grantDocumentUris = listOf(romPath),
            )
        }

        return RetroArchRomPaths.ResolvedRom(romExtra = romPath)
    }

    private fun resolveContentBootPath(
        romPath: String,
        romPathsJson: String,
        romsTreeUri: String?,
        systemFolder: String,
    ): RetroArchRomPaths.ResolvedRom? {
        val entries = pathsFromJson(romPathsJson)
        val contentUris = linkedSetOf<String>()
        contentUris += romPath
        entries.filter { it.startsWith("content:", ignoreCase = true) }.forEach { contentUris += it }

        val tree = RetroArchRomPaths.treeUriFromDocumentUriString(romPath)
            ?: romsTreeUri?.let(RetroArchRomPaths::canonicalTreeUri)
        val treeDocId = tree?.let { SafPathResolver.resolveTreeDocumentIdString(it) }
        if (tree != null && treeDocId != null) {
            val cueRelative = RetroArchRomPaths.relativeFromContentUri(romPath)
            entries.filter { !it.startsWith("content:", ignoreCase = true) }.forEach { entry ->
                val fullRelative = relativeToRomsRoot(entry, systemFolder)
                if (cueRelative != null && fullRelative.equals(cueRelative, ignoreCase = true)) return@forEach
                documentUriForRelativePath(tree, treeDocId, fullRelative)?.let { contentUris += it }
            }
            // Game folder (parent of the cue) — needed so the emulator can resolve siblings.
            cueRelative?.substringBeforeLast('/', "")?.takeIf { it.isNotBlank() }?.let { parentRel ->
                documentUriForRelativePath(tree, treeDocId, parentRel)?.let { contentUris += it }
            }
        }

        val bootPath = selectBootContentUri(romPath, contentUris.toList())
        val grants = linkedSetOf<String>()
        grants += contentUris
        grants += bootPath
        systemFolderDocumentUri(romsTreeUri ?: tree, systemFolder)?.let { grants += it }

        return RetroArchRomPaths.ResolvedRom(
            romExtra = bootPath,
            grantTreeUri = tree,
            grantDocumentUri = bootPath,
            grantDocumentUris = grants.toList(),
        )
    }

    /**
     * ES-DE's Android ps2 `<extension>` list has no `.cue` — it launches the `.bin`
     * (or iso/chd) via `%ROMSAF%`. Mirror that for single-bin cue sheets.
     */
    internal fun selectBootContentUri(primary: String, contentUris: List<String>): String {
        if (!extensionOf(primary).equals("cue", ignoreCase = true)) return primary
        val binUris = contentUris.filter {
            it != primary && extensionOf(it).equals("bin", ignoreCase = true)
        }
        if (binUris.size == 1) return binUris.single()
        return primary
    }

    /** @deprecated kept for tests; prefer [selectBootContentUri]. */
    internal fun selectBootPath(primary: String, contentUris: List<String>, systemFolder: String): String =
        selectBootContentUri(primary, contentUris)

    internal fun relativeToRomsRoot(relativeEntry: String, systemFolder: String): String {
        val rel = relativeEntry.replace('\\', '/').trimStart('/')
        val folder = systemFolder.trim('/').trim()
        return when {
            folder.isBlank() -> rel
            rel.startsWith("$folder/", ignoreCase = true) -> rel
            else -> "$folder/$rel"
        }
    }

    internal fun documentUriForRelativePath(
        treeUriString: String,
        treeDocId: String,
        relativeToRomsRoot: String,
    ): String? {
        val rel = relativeToRomsRoot.replace('\\', '/').trimStart('/')
        if (rel.isBlank()) return null
        val docId = "${treeDocId.trimEnd('/')}/$rel"
        return runCatching {
            DocumentsContract.buildDocumentUriUsingTree(Uri.parse(treeUriString), docId).toString()
        }.getOrNull() ?: buildDocumentUriFromTree(treeUriString, docId)
    }

    internal fun buildDocumentUriFromTree(treeUriString: String, documentId: String): String? {
        val tree = RetroArchRomPaths.treeUriFromDocumentUriString(treeUriString)
            ?: treeUriString.substringBefore("/document/").takeIf { it.contains("/tree/") }
            ?: return null
        return "$tree/document/${encodeDocumentId(documentId)}"
    }

    internal fun encodeDocumentId(documentId: String): String = buildString(documentId.length * 3) {
        for (ch in documentId) {
            when (ch) {
                '%' -> append("%25")
                ':' -> append("%3A")
                '/' -> append("%2F")
                ' ' -> append("%20")
                else -> append(ch)
            }
        }
    }

    internal fun systemFolderDocumentUri(romsTreeUri: String?, systemFolder: String): String? {
        if (romsTreeUri.isNullOrBlank() || systemFolder.isBlank()) return null
        val tree = RetroArchRomPaths.canonicalTreeUri(romsTreeUri)
        val treeDocId = SafPathResolver.resolveTreeDocumentIdString(tree) ?: return null
        val systemDocId = "${treeDocId.trimEnd('/')}/${systemFolder.trim('/')}"
        return runCatching {
            DocumentsContract.buildDocumentUriUsingTree(Uri.parse(tree), systemDocId).toString()
        }.getOrNull() ?: buildDocumentUriFromTree(tree, systemDocId)
    }

    private fun extensionOf(path: String): String =
        path.substringBeforeLast('?').substringAfterLast('.', "")

    private fun filesystemBootPath(
        romPath: String,
        romPathsJson: String,
        romsDirPath: String?,
        systemFolder: String,
        romsTreeUri: String?,
    ): RetroArchRomPaths.ResolvedRom? {
        if (romsDirPath.isNullOrBlank()) return null
        val root = File(romsDirPath.trim())
        if (!root.isDirectory) return null

        val candidates = buildList {
            readableFile(romPath)?.let { add(File(it)) }
            relativeCandidates(romPath, romPathsJson, systemFolder, romsDirPath, romsTreeUri).forEach { rel ->
                add(File(root, rel))
            }
        }.distinctBy { it.absolutePath }
            .sortedBy { candidate ->
                when {
                    // ES-DE boots .bin for PS2 multi-file sets (no .cue in extensions).
                    candidate.extension.equals("bin", ignoreCase = true) -> 0
                    candidate.extension.equals("cue", ignoreCase = true) -> 1
                    DiscDescriptorPaths.isMultiFileDescriptor(candidate.name) -> 2
                    else -> 3
                }
            }

        for (file in candidates) {
            if (!file.isFile) continue
            if (file.extension.equals("cue", ignoreCase = true)) {
                if (!DiscDescriptorPaths.cueCompanionsReadable(file)) continue
            }
            return RetroArchRomPaths.ResolvedRom(romExtra = file.absolutePath)
        }
        return null
    }

    private fun relativeCandidates(
        romPath: String,
        romPathsJson: String,
        systemFolder: String,
        romsDirPath: String?,
        romsTreeUri: String?,
    ): List<String> =
        RetroArchRomPaths.relativeFromRomsRoot(
            romPath = romPath,
            romPathsJson = romPathsJson,
            systemFolder = systemFolder,
            romsDirPath = romsDirPath,
            romsTreeUri = romsTreeUri,
        )?.let { listOf(it) }.orEmpty() +
            pathsFromJson(romPathsJson).mapNotNull { entry ->
                when {
                    entry.startsWith("content:", ignoreCase = true) -> null
                    RetroArchLauncher.isLikelyFilesystemPath(entry) ->
                        RetroArchRomPaths.relativeFromRomsRoot(
                            romPath = entry,
                            romPathsJson = "[]",
                            systemFolder = systemFolder,
                            romsDirPath = romsDirPath,
                            romsTreeUri = romsTreeUri,
                        ) ?: entry.removePrefix("/").takeIf { it.isNotBlank() }
                    else -> {
                        val folder = systemFolder.trim('/').trim()
                        if (folder.isBlank()) entry else "$folder/${entry.trimStart('/')}"
                    }
                }
            }

    private fun readableFile(path: String): String? {
        if (!RetroArchLauncher.isLikelyFilesystemPath(path)) return null
        val file = File(path)
        return file.takeIf { it.isFile }?.absolutePath
    }

    private fun firstReadableEntry(romPathsJson: String): String? =
        pathsFromJson(romPathsJson).firstNotNullOfOrNull { readableFile(it) }

    private fun pathsFromJson(romPathsJson: String): List<String> =
        runCatching {
            romPathsJsonParser.decodeFromString<List<String>>(romPathsJson)
                .filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
}
