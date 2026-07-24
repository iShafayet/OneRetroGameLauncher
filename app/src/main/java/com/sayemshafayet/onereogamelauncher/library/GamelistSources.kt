package com.sayemshafayet.onereogamelauncher.library

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.sayemshafayet.onereogamelauncher.ui.util.SafIo
import com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver
import java.io.File

/**
 * Loads ES-DE / EmulationStation [gamelist.xml] files.
 *
 * ES-DE stores authoritative metadata under `{ES-DE data}/gamelists/<system>/gamelist.xml`.
 * Legacy installs may still keep a copy inside each ROM system folder — that is used as fallback.
 */
object GamelistSources {
    private const val TAG = "GamelistSources"

    /** ES-DE entries win; ROM-folder gamelist fills gaps. */
    fun merge(
        esde: Map<String, GamelistEntry>,
        romFolder: Map<String, GamelistEntry>,
    ): Map<String, GamelistEntry> {
        if (esde.isEmpty()) return romFolder
        if (romFolder.isEmpty()) return esde
        val out = LinkedHashMap(romFolder)
        esde.forEach { (path, entry) -> out[path] = entry }
        return out
    }

    fun loadFromRomFolder(
        systemDir: File,
        parser: GamelistParser,
    ): Map<String, GamelistEntry> {
        val file = File(systemDir, "gamelist.xml")
        if (!file.isFile) return emptyMap()
        return runCatching {
            parseToMap(parser, file.inputStream().buffered(), "rom:${systemDir.name}")
        }.getOrDefault(emptyMap())
    }

    fun loadFromRomFolderSaf(
        context: Context,
        systemDir: DocumentFile,
        systemFolder: String,
        parser: GamelistParser,
    ): Map<String, GamelistEntry> {
        val gamelistDoc = findDocumentFile(systemDir, "gamelist.xml") ?: return emptyMap()
        val stream = SafIo.openInputStream(context, gamelistDoc.uri) ?: return emptyMap()
        return stream.use { parseToMap(parser, it, "rom-saf:$systemFolder") }
    }

    fun loadFromEsdeDataDir(
        context: Context,
        esdeDataDirUri: String?,
        esdeDataDirPath: String?,
        systemFolder: String,
        parser: GamelistParser,
    ): Map<String, GamelistEntry> {
        return runCatching {
            if (!esdeDataDirUri.isNullOrBlank()) {
                val fromSaf = loadFromEsdeSaf(context, esdeDataDirUri, systemFolder, parser)
                if (fromSaf != null) return@runCatching fromSaf
            }
            if (!esdeDataDirPath.isNullOrBlank()) {
                val fromFile = loadFromEsdeFile(
                    SafPathResolver.normalize(esdeDataDirPath),
                    systemFolder,
                    parser,
                )
                if (fromFile != null) return@runCatching fromFile
            }
            emptyMap()
        }.onFailure {
            Log.w(TAG, "ES-DE gamelist load failed for $systemFolder — ignoring", it)
        }.getOrDefault(emptyMap())
    }

    private fun loadFromEsdeSaf(
        context: Context,
        esdeDataDirUri: String,
        systemFolder: String,
        parser: GamelistParser,
    ): Map<String, GamelistEntry>? {
        val root = runCatching { Uri.parse(esdeDataDirUri) }
            .getOrNull()
            ?.let { DocumentFile.fromTreeUri(context, it) }
            ?: return null
        val gamelistsRoot = findGamelistsRootDoc(root, depth = 0) ?: run {
            Log.w(TAG, "No gamelists/ under ES-DE SAF root for $systemFolder")
            return emptyMap()
        }
        val systemDir = findChildDir(gamelistsRoot, systemFolder) ?: run {
            Log.d(TAG, "No gamelists/$systemFolder under ES-DE data dir")
            return emptyMap()
        }
        val gamelistDoc = findDocumentFile(systemDir, "gamelist.xml") ?: return emptyMap()
        val stream = SafIo.openInputStream(context, gamelistDoc.uri) ?: return emptyMap()
        return stream.use { parseToMap(parser, it, "esde-saf:$systemFolder") }
    }

    private fun loadFromEsdeFile(
        esdeRootPath: String,
        systemFolder: String,
        parser: GamelistParser,
    ): Map<String, GamelistEntry>? {
        val root = File(esdeRootPath)
        if (!root.exists()) return null
        val gamelistsRoot = findGamelistsRootFile(root) ?: run {
            Log.w(TAG, "No gamelists/ under ES-DE path $esdeRootPath")
            return emptyMap()
        }
        val gamelistFile = File(File(gamelistsRoot, systemFolder), "gamelist.xml")
        if (!gamelistFile.isFile) {
            Log.d(TAG, "No gamelist at ${gamelistFile.absolutePath}")
            return emptyMap()
        }
        return runCatching {
            parseToMap(parser, gamelistFile.inputStream().buffered(), "esde:$systemFolder")
        }.getOrDefault(emptyMap())
    }

    private fun parseToMap(
        parser: GamelistParser,
        input: java.io.InputStream,
        source: String,
    ): Map<String, GamelistEntry> =
        runCatching { parser.parse(input) }
            .onFailure { Log.w(TAG, "Failed to parse $source: ${it.message}") }
            .getOrDefault(emptyList())
            .also { entries ->
                if (entries.isNotEmpty()) {
                    Log.i(TAG, "Loaded ${entries.size} gamelist entries from $source")
                }
            }
            .associateBy { it.path }

    private fun findGamelistsRootDoc(root: DocumentFile, depth: Int): DocumentFile? {
        if (root.name.equals("gamelists", ignoreCase = true)) return root
        findChildDir(root, "gamelists")?.let { return it }
        if (depth >= 2) return null
        SafIo.listChildren(root).forEach { child ->
            if (!child.isDirectory) return@forEach
            findGamelistsRootDoc(child, depth + 1)?.let { return it }
        }
        return null
    }

    private fun findGamelistsRootFile(root: File): File? {
        if (root.name.equals("gamelists", ignoreCase = true) && root.isDirectory) return root
        File(root, "gamelists").takeIf { it.isDirectory }?.let { return it }
        root.listFiles()?.filter { it.isDirectory }?.forEach { child ->
            if (child.name.equals("gamelists", true)) return child
            File(child, "gamelists").takeIf { it.isDirectory }?.let { return it }
        }
        return null
    }

    private fun findChildDir(parent: DocumentFile, name: String): DocumentFile? {
        SafIo.listChildren(parent).forEach { child ->
            if (child.isDirectory && child.name.equals(name, ignoreCase = true)) return child
        }
        return null
    }

    private fun findDocumentFile(dir: DocumentFile, fileName: String): DocumentFile? {
        SafIo.listChildren(dir).forEach { child ->
            if (child.isFile && child.name.equals(fileName, ignoreCase = true)) return child
            if (child.isDirectory) {
                findDocumentFile(child, fileName)?.let { return it }
            }
        }
        return null
    }
}
