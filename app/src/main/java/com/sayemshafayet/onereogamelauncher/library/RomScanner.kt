package com.sayemshafayet.onereogamelauncher.library

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.sayemshafayet.onereogamelauncher.data.db.dao.GameDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.MediaDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.SystemDao
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.domain.ScanProgress
import com.sayemshafayet.onereogamelauncher.domain.SystemDef
import com.sayemshafayet.onereogamelauncher.systems.EsSystemsParser
import com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class UnknownFileEntry(
    val systemFolder: String,
    val relativePath: String,
    val absolutePath: String,
)

data class RomScanResult(
    val systemsScanned: Int,
    val gamesFound: Int,
    val mediaLinked: Int = 0,
    val unknownFiles: List<UnknownFileEntry>,
)

private data class ScannedGame(
    val romPath: String,
    val relativePath: String,
    val absolutePath: String,
    val fileName: String,
    val title: String,
    val romPaths: List<String>,
    val gamelist: GamelistEntry?,
)

private data class FoundFile(
    val name: String,
    val relativePath: String,
    /** Absolute filesystem path when available, else content URI string. */
    val storagePath: String,
    val extension: String,
)

@Singleton
class RomScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val systemDao: SystemDao,
    private val gameDao: GameDao,
    private val mediaDao: MediaDao,
    private val gamelistParser: GamelistParser,
    private val esSystemsParser: EsSystemsParser,
) {
    companion object {
        private const val TAG = "RomScanner"
        private val COMPANION_EXTS = setOf("bin", "img", "iso", "raw")
        private val ORGL_MEDIA_PROVIDERS = setOf(
            "orgl",
            "screenscraper",
            "libretro-thumbnails",
        )
        private val ES_DE_MEDIA_FOLDERS = listOf(
            "covers" to MediaType.BOX_2D,
            "box2d" to MediaType.BOX_2D,
            "boxfront" to MediaType.BOX_2D,
            // ES-DE grid art — use as cover when no box art exists
            "miximages" to MediaType.BOX_2D,
            "3dboxes" to MediaType.BOX_3D,
            "box3d" to MediaType.BOX_3D,
            "screenshots" to MediaType.SCREENSHOT,
            "titlescreens" to MediaType.TITLE,
            "titles" to MediaType.TITLE,
            "marquees" to MediaType.MARQUEE,
            "logos" to MediaType.MARQUEE,
            "videos" to MediaType.VIDEO,
            "fanart" to MediaType.FANART,
        )
    }

    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    val scanProgress: StateFlow<ScanProgress?> = _scanProgress.asStateFlow()

    suspend fun scan(
        romsRoot: File,
        orglDataDirUri: String? = null,
        orglDataDirPath: String? = null,
        esdeDataDirUri: String? = null,
        esdeDataDirPath: String? = null,
    ): RomScanResult = withContext(Dispatchers.IO) {
        require(romsRoot.isDirectory) { "ROMs root is not a directory: ${romsRoot.absolutePath}" }
        Log.i(TAG, "File scan root=${romsRoot.absolutePath} children=${romsRoot.list()?.take(10)}")
        val orglIndex = MediaLibrary.open(context, orglDataDirUri, orglDataDirPath, "ORGL")
        val esdeIndex = MediaLibrary.open(context, esdeDataDirUri, esdeDataDirPath, "ES-DE")
        Log.i(TAG, "Media libs: ${orglIndex.describe()} | ${esdeIndex.describe()}")
        scanInternal(
            systemFolderExists = { folder -> File(romsRoot, folder).isDirectory },
            listSystemFiles = { folder, _ -> listFilesystemFiles(File(romsRoot, folder)) },
            loadGamelist = { folder ->
                val rom = GamelistSources.loadFromRomFolder(File(romsRoot, folder), gamelistParser)
                val esde = GamelistSources.loadFromEsdeDataDir(
                    context,
                    esdeDataDirUri,
                    esdeDataDirPath,
                    folder,
                    gamelistParser,
                )
                GamelistSources.merge(esde, rom)
            },
            readTextFile = { path -> runCatching { File(path).readText() }.getOrNull() },
            orglMediaIndex = orglIndex,
            esdeMediaIndex = esdeIndex,
            systemDirHint = { folder -> File(romsRoot, folder) },
            resolveGamelistMedia = { folder, rel ->
                val f = File(File(romsRoot, folder), rel.removePrefix("./"))
                f.takeIf { it.isFile }?.absolutePath
            },
        )
    }

    suspend fun scanSaf(
        treeUri: Uri,
        orglDataDirUri: String? = null,
        orglDataDirPath: String? = null,
        esdeDataDirUri: String? = null,
        esdeDataDirPath: String? = null,
    ): RomScanResult = withContext(Dispatchers.IO) {
        // Mirror DroidArcade ValidationEngine: always walk via DocumentFile, never java.io.File.
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("Cannot open ROMs folder URI — re-pick the folder in Settings → Folders")
        require(root.isDirectory) { "ROMs URI is not a folder" }

        val systemDirs = root.listFiles()
            .filter { it.isDirectory && !it.name.isNullOrBlank() }
            .associateBy { it.name!!.lowercase() }

        Log.i(
            TAG,
            "SAF DocumentFile root children=${systemDirs.size}: ${systemDirs.keys.take(20)}",
        )
        if (systemDirs.isEmpty()) {
            error(
                "No folders found under the selected ROMs directory. " +
                    "Pick the folder that contains nes/, snes/, psx/, …",
            )
        }

        val orglIndex = MediaLibrary.open(context, orglDataDirUri, orglDataDirPath, "ORGL")
        val esdeIndex = MediaLibrary.open(context, esdeDataDirUri, esdeDataDirPath, "ES-DE")
        Log.i(TAG, "Media libs: ${orglIndex.describe()} | ${esdeIndex.describe()}")

        scanInternal(
            systemFolderExists = { folder -> systemDirs.containsKey(folder.lowercase()) },
            listSystemFiles = { folder, _ ->
                val dir = systemDirs[folder.lowercase()]
                if (dir == null) emptyList() else listDocumentFilesRecursive(dir)
            },
            loadGamelist = { folder ->
                val dir = systemDirs[folder.lowercase()]
                val rom = if (dir == null) {
                    emptyMap()
                } else {
                    GamelistSources.loadFromRomFolderSaf(context, dir, folder, gamelistParser)
                }
                val esde = GamelistSources.loadFromEsdeDataDir(
                    context,
                    esdeDataDirUri,
                    esdeDataDirPath,
                    folder,
                    gamelistParser,
                )
                GamelistSources.merge(esde, rom)
            },
            readTextFile = { path ->
                when {
                    path.startsWith("content:", ignoreCase = true) ->
                        context.contentResolver.openInputStream(Uri.parse(path))
                            ?.use { it.reader().readText() }
                    else -> runCatching { File(path).readText() }.getOrNull()
                }
            },
            orglMediaIndex = orglIndex,
            esdeMediaIndex = esdeIndex,
            systemDirHint = { null },
            resolveGamelistMedia = { folder, rel ->
                systemDirs[folder.lowercase()]?.let { resolveDocumentRelative(it, rel) }
            },
        )
    }

    /**
     * Recursive DocumentFile walk — same approach as DroidArcade ValidationEngine.collectFiles().
     */
    private fun listDocumentFilesRecursive(dir: DocumentFile): List<FoundFile> {
        val out = mutableListOf<FoundFile>()
        fun walk(current: DocumentFile, prefix: String) {
            current.listFiles().forEach { child ->
                val name = child.name ?: return@forEach
                when {
                    child.isDirectory -> {
                        val next = if (prefix.isEmpty()) name else "$prefix/$name"
                        walk(child, next)
                    }
                    child.isFile -> {
                        val rel = if (prefix.isEmpty()) name else "$prefix/$name"
                        val ext = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
                        // Always keep the SAF document URI as the storage key. Derived
                        // filesystem paths are often unreadable by RetroArch under scoped storage.
                        val storage = child.uri.toString()
                        out += FoundFile(
                            name = name,
                            relativePath = rel,
                            storagePath = storage,
                            extension = ext,
                        )
                    }
                }
            }
        }
        walk(dir, "")
        return out
    }

    private fun findDocumentFile(dir: DocumentFile, fileName: String): DocumentFile? {
        dir.listFiles().forEach { child ->
            if (child.isFile && child.name.equals(fileName, ignoreCase = true)) return child
            if (child.isDirectory) {
                findDocumentFile(child, fileName)?.let { return it }
            }
        }
        return null
    }

    private suspend fun scanInternal(
        systemFolderExists: (String) -> Boolean,
        listSystemFiles: (String, Set<String>) -> List<FoundFile>,
        loadGamelist: (String) -> Map<String, GamelistEntry>,
        readTextFile: (String) -> String?,
        orglMediaIndex: MediaLibrary,
        esdeMediaIndex: MediaLibrary,
        systemDirHint: (String) -> File?,
        resolveGamelistMedia: (String, String) -> String?,
    ): RomScanResult {
        val systems = systemDao.getAll()
        val defsByFolder = systems.associateBy { it.folderName.lowercase() }

        // Prefer extensions from bundled es_systems.xml (source of truth)
        val assetDefs = runCatching {
            context.assets.open("systems/es_systems.xml").use { esSystemsParser.parseSystems(it) }
        }.getOrDefault(emptyList()).associateBy { it.folder.lowercase() }

        val systemDefs = systems.map { entity ->
            val asset = assetDefs[entity.folderName.lowercase()]
            val extensions = asset?.extensions
                ?.takeIf { it.isNotEmpty() }
                ?: entity.toSystemDef().extensions
            SystemDef(
                name = entity.name,
                fullName = entity.displayName,
                folder = entity.folderName,
                extensions = extensions,
                platform = entity.platform,
                commands = asset?.commands.orEmpty(),
            )
        }

        var totalGames = 0
        var totalMedia = 0
        var systemsDone = 0
        val unknown = mutableListOf<UnknownFileEntry>()
        val systemsToScan = systemDefs.filter { systemFolderExists(it.folder) }
        Log.i(
            TAG,
            "systemsInDb=${systems.size} foldersPresent=${systemsToScan.size} " +
                "names=${systemsToScan.take(15).map { it.folder }}",
        )

        for (def in systemsToScan) {
            val systemEntity = defsByFolder[def.folder.lowercase()] ?: continue
            val gamelistByPath = loadGamelist(def.folder)
            val found = listSystemFiles(def.folder, def.extensions)
            Log.i(
                TAG,
                "System ${def.folder}: files=${found.size} exts=${def.extensions.take(8)}",
            )
            val hiddenFromM3u = collectM3uHiddenPaths(found, readTextFile)
            val companions = collectCompanionPaths(found, readTextFile)
            val hidden = hiddenFromM3u + companions

            val romFiles = mutableListOf<FoundFile>()
            val unknownInSystem = mutableListOf<UnknownFileEntry>()

            for (file in found) {
                if (file.name.equals("gamelist.xml", ignoreCase = true)) continue
                if (file.relativePath in hidden || file.storagePath in hidden) continue
                when {
                    file.extension == "m3u" -> romFiles += file
                    file.extension in def.extensions -> romFiles += file
                    file.extension.isNotEmpty() -> unknownInSystem += UnknownFileEntry(
                        systemFolder = def.folder,
                        relativePath = file.relativePath,
                        absolutePath = file.storagePath,
                    )
                }
            }
            unknown += unknownInSystem

            val scannedGames = buildScannedGames(romFiles, gamelistByPath, hidden, readTextFile)
            val existingByPath = gameDao.getBySystem(systemEntity.id).associateBy { it.romPath }
            val keepPaths = mutableListOf<String>()
            val systemDir = systemDirHint(def.folder)

            for (game in scannedGames) {
                keepPaths += game.romPath
                val existing = existingByPath[game.romPath]
                    ?: existingByPath.values.firstOrNull { it.fileName == game.fileName }
                val merged = mergeGameEntity(
                    systemEntity.id,
                    systemDir,
                    game,
                    existing,
                    gamelistByPath[game.relativePath] ?: game.gamelist,
                )
                // Prefer update-by-id when we already know the row — avoids REPLACE edge cases
                val gameId = if (existing != null) {
                    gameDao.update(merged.copy(id = existing.id))
                    existing.id
                } else {
                    gameDao.upsert(merged.copy(id = 0L))
                }

                val existingMedia = mediaDao.forGame(gameId)
                val media = resolveMedia(
                    orglIndex = orglMediaIndex,
                    esdeIndex = esdeMediaIndex,
                    systemFolder = def.folder,
                    systemName = systemEntity.name,
                    game = game,
                    gamelist = gamelistByPath[game.relativePath] ?: game.gamelist,
                    existing = existingMedia,
                    resolveGamelistMedia = resolveGamelistMedia,
                )
                mediaDao.deleteForGame(gameId)
                if (media.isNotEmpty()) {
                    mediaDao.upsertAll(media.map { it.copy(gameId = gameId) })
                    totalMedia += media.size
                }
            }

            if (keepPaths.isEmpty()) {
                gameDao.deleteForSystem(systemEntity.id)
            } else {
                gameDao.deleteForSystemExcept(systemEntity.id, keepPaths)
            }

            totalGames += scannedGames.size
            systemsDone++
            _scanProgress.value = ScanProgress(
                systemName = def.fullName,
                gamesFound = scannedGames.size,
                systemsDone = systemsDone,
                systemsTotal = systemsToScan.size,
            )
        }

        _scanProgress.value = null
        Log.i(
            TAG,
            "Scan done: systems=$systemsDone games=$totalGames media=$totalMedia " +
                "unknown=${unknown.size} orgl=${orglMediaIndex.describe()} " +
                "esde=${esdeMediaIndex.describe()}",
        )
        return RomScanResult(
            systemsScanned = systemsDone,
            gamesFound = totalGames,
            mediaLinked = totalMedia,
            unknownFiles = unknown,
        )
    }

    private fun listFilesystemFiles(systemDir: File): List<FoundFile> {
        val out = mutableListOf<FoundFile>()
        systemDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val ext = file.extension.lowercase()
            val rel = file.relativeTo(systemDir).path.replace('\\', '/')
            out += FoundFile(
                name = file.name,
                relativePath = rel,
                storagePath = file.absolutePath,
                extension = ext,
            )
        }
        return out
    }


    private fun collectM3uHiddenPaths(
        files: List<FoundFile>,
        readTextFile: (String) -> String?,
    ): Set<String> {
        val hidden = mutableSetOf<String>()
        files.filter { it.extension == "m3u" }.forEach { m3u ->
            val text = readTextFile(m3u.storagePath) ?: return@forEach
            val parentRel = m3u.relativePath.substringBeforeLast('/', missingDelimiterValue = "")
            text.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
                val normalized = trimmed.replace('\\', '/').trimStart('/')
                hidden += normalized
                hidden += "./$normalized"
                if (parentRel.isNotEmpty()) {
                    hidden += "$parentRel/$normalized"
                }
                files.firstOrNull {
                    it.relativePath.equals(normalized, true) ||
                        it.relativePath.equals("$parentRel/$normalized", true) ||
                        it.name.equals(File(normalized).name, true)
                }?.let { hidden += it.storagePath }
            }
        }
        return hidden
    }

    /**
     * Hide disc companions when a primary playlist/descriptor exists:
     * .cue hides same-basename .bin/.img; .gdi hides sibling tracks; etc.
     */
    private fun collectCompanionPaths(
        files: List<FoundFile>,
        readTextFile: (String) -> String?,
    ): Set<String> {
        val hidden = mutableSetOf<String>()
        val byDir = files.groupBy { it.relativePath.substringBeforeLast('/', missingDelimiterValue = "") }

        for ((dir, group) in byDir) {
            val cues = group.filter { it.extension == "cue" }
            val gdis = group.filter { it.extension == "gdi" }
            val m3us = group.filter { it.extension == "m3u" }

            for (cue in cues) {
                val base = cue.name.substringBeforeLast('.')
                group.filter {
                    it.name.substringBeforeLast('.').equals(base, true) &&
                        it.extension in COMPANION_EXTS
                }.forEach { hidden += it.storagePath; hidden += it.relativePath }

                // FILE lines inside cue
                val text = readTextFile(cue.storagePath).orEmpty()
                Regex("""FILE\s+"([^"]+)"|FILE\s+(\S+)""", RegexOption.IGNORE_CASE)
                    .findAll(text)
                    .forEach { match ->
                        val ref = (match.groupValues[1].ifBlank { match.groupValues[2] })
                            .replace('\\', '/')
                        val rel = if (dir.isEmpty()) ref else "$dir/$ref"
                        hidden += rel
                        group.firstOrNull { it.name.equals(File(ref).name, true) }
                            ?.let { hidden += it.storagePath; hidden += it.relativePath }
                    }
            }

            for (gdi in gdis) {
                // GDI sets keep tracks as .bin/.raw alongside the .gdi
                group.filter { it.extension in setOf("bin", "raw") }
                    .forEach { hidden += it.storagePath; hidden += it.relativePath }
            }

            // If any m3u exists in folder, companions already handled; nothing extra.
            if (m3us.isNotEmpty()) {
                // Prefer m3u titles; hide loose cues that are listed? already via m3u parser.
            }
        }
        return hidden
    }

    private fun buildScannedGames(
        romFiles: List<FoundFile>,
        gamelistByPath: Map<String, GamelistEntry>,
        hidden: Set<String>,
        readTextFile: (String) -> String?,
    ): List<ScannedGame> {
        val games = mutableListOf<ScannedGame>()
        val m3uFiles = romFiles.filter { it.extension == "m3u" }

        for (m3u in m3uFiles) {
            val gl = gamelistMatch(gamelistByPath, m3u.relativePath, m3u.name)
            val discPaths = parseM3uDiscEntries(readTextFile(m3u.storagePath).orEmpty())
            games += ScannedGame(
                romPath = m3u.storagePath,
                relativePath = m3u.relativePath,
                absolutePath = m3u.storagePath,
                fileName = m3u.name,
                title = gl?.name ?: m3u.name.substringBeforeLast('.'),
                romPaths = discPaths.ifEmpty { listOf(m3u.relativePath) },
                gamelist = gl,
            )
        }

        for (file in romFiles) {
            if (file.extension == "m3u") continue
            if (file.relativePath in hidden || file.storagePath in hidden) continue
            val gl = gamelistMatch(gamelistByPath, file.relativePath, file.name)
            games += ScannedGame(
                romPath = file.storagePath,
                relativePath = file.relativePath,
                absolutePath = file.storagePath,
                fileName = file.name,
                title = gl?.name ?: file.name.substringBeforeLast('.'),
                romPaths = listOf(file.relativePath),
                gamelist = gl,
            )
        }
        return games.distinctBy { it.romPath }
    }

    private fun gamelistMatch(
        gamelistByPath: Map<String, GamelistEntry>,
        relativePath: String,
        fileName: String,
    ): GamelistEntry? {
        gamelistByPath[relativePath]?.let { return it }
        val norm = normalizePath(relativePath)
        gamelistByPath.entries.firstOrNull { normalizePath(it.key) == norm }?.value?.let { return it }
        gamelistByPath.entries.firstOrNull {
            val keyNorm = normalizePath(it.key)
            keyNorm.endsWith(norm) || norm.endsWith(keyNorm)
        }?.value?.let { return it }
        val fileStem = fileName.substringBeforeLast('.').lowercase()
        if (fileStem.isNotBlank()) {
            return gamelistByPath.entries.firstOrNull { (path, _) ->
                path.substringAfterLast('/').substringBeforeLast('.').equals(fileStem, ignoreCase = true)
            }?.value
        }
        return null
    }

    private fun parseM3uDiscEntries(text: String): List<String> =
        text.lineSequence().map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.replace('\\', '/').trimStart('/') }
            .toList()

    private fun mergeGameEntity(
        systemId: Long,
        systemDir: File?,
        scanned: ScannedGame,
        existing: GameEntity?,
        gamelist: GamelistEntry?,
    ): GameEntity {
        val gl = gamelist ?: scanned.gamelist
        val romPathsJson = JSONArray(
            scanned.romPaths.map { path ->
                if (systemDir != null) {
                    File(systemDir, path).takeIf { it.isFile }?.absolutePath ?: path
                } else {
                    path
                }
            },
        ).toString()
        // ORGL-owned fields win when present; fall back to ES-DE gamelist / scan defaults.
        val derivedTitle = scanned.fileName.substringBeforeLast('.')
        val existingTitle = existing?.title?.takeIf { it.isNotBlank() }
        val title = when {
            existingTitle == null -> gl?.name?.takeIf { it.isNotBlank() } ?: scanned.title
            gl?.name?.isNotBlank() == true &&
                existingTitle.equals(derivedTitle, ignoreCase = true) -> gl.name
            else -> existingTitle
        }
        return GameEntity(
            id = existing?.id ?: 0L,
            systemId = systemId,
            title = title,
            romPath = scanned.romPath,
            romPathsJson = romPathsJson,
            fileName = scanned.fileName,
            favorite = existing?.favorite ?: gl?.favorite ?: false,
            description = pickMetadata(existing?.description, gl?.desc),
            notes = existing?.notes,
            rating = existing?.rating ?: gl?.rating,
            releaseDate = pickMetadata(existing?.releaseDate, gl?.releasedate),
            developer = pickMetadata(existing?.developer, gl?.developer),
            publisher = pickMetadata(existing?.publisher, gl?.publisher),
            genre = pickMetadata(existing?.genre, gl?.genre),
            players = pickMetadata(existing?.players, gl?.players),
            esdePlaycount = gl?.playcount ?: existing?.esdePlaycount ?: 0,
            esdeLastPlayed = gl?.lastplayed ?: existing?.esdeLastPlayed,
            orglPlaycount = existing?.orglPlaycount ?: 0,
            orglLastPlayed = existing?.orglLastPlayed,
            orglPlaytimeMs = existing?.orglPlaytimeMs ?: 0L,
            completedStatus = existing?.completedStatus,
            onShelf = existing?.onShelf ?: false,
            raGameId = existing?.raGameId,
            hltbId = existing?.hltbId,
            unknownExtensionsNote = existing?.unknownExtensionsNote,
            lastScrapedAt = existing?.lastScrapedAt,
        )
    }

    private fun pickMetadata(existing: String?, fromGamelist: String?): String? =
        existing?.takeIf { it.isNotBlank() } ?: fromGamelist?.takeIf { it.isNotBlank() }

    private fun resolveMedia(
        orglIndex: MediaLibrary,
        esdeIndex: MediaLibrary,
        systemFolder: String,
        systemName: String,
        game: ScannedGame,
        gamelist: GamelistEntry?,
        existing: List<MediaEntity>,
        resolveGamelistMedia: (String, String) -> String?,
    ): List<MediaEntity> {
        val gl = gamelist ?: game.gamelist
        val media = linkedMapOf<MediaType, MediaEntity>()
        val systemKeys = listOf(systemName, systemFolder).filter { it.isNotBlank() }
        val baseNames = buildList {
            add(game.fileName.substringBeforeLast('.'))
            add(game.title)
            gl?.name?.let { add(it) }
            gl?.image?.substringAfterLast('/')?.substringBeforeLast('.')?.let { add(it) }
            gl?.thumbnail?.substringAfterLast('/')?.substringBeforeLast('.')?.let { add(it) }
        }.map { it.trim() }.filter { it.isNotBlank() }.distinct()

        fun putIfAbsent(type: MediaType, path: String, provider: String) {
            if (type in media) return
            if (path.isBlank()) return
            if (!mediaPathExists(path)) return
            media[type] = MediaEntity(gameId = 0, type = type, path = path, provider = provider)
        }

        fun fillFromIndex(index: MediaLibrary, provider: String) {
            if (!index.isAvailable()) return
            for ((folder, type) in ES_DE_MEDIA_FOLDERS) {
                if (type in media) continue
                index.find(systemKeys, baseNames, folder)?.let { putIfAbsent(type, it, provider) }
            }
        }

        // 1) Keep ORGL-owned DB entries that still exist
        for (item in existing) {
            if (item.provider in ORGL_MEDIA_PROVIDERS) {
                putIfAbsent(item.type, item.path, item.provider)
            }
        }
        // 2) ORGL data directory
        fillFromIndex(orglIndex, provider = "orgl")
        // 3) Other existing entries
        for (item in existing) {
            putIfAbsent(item.type, item.path, item.provider)
        }
        // 4) ES-DE data directory (read-only fallback)
        fillFromIndex(esdeIndex, provider = "es-de")
        // 5) Gamelist-relative paths under the ROM system folder (read-only)
        gl?.image?.let { raw ->
            resolveGamelistMedia(systemFolder, raw.removePrefix("./"))?.let {
                putIfAbsent(MediaType.BOX_2D, it, "gamelist")
            }
        }
        gl?.thumbnail?.let { raw ->
            resolveGamelistMedia(systemFolder, raw.removePrefix("./"))?.let {
                putIfAbsent(MediaType.BOX_2D, it, "gamelist")
            }
        }
        gl?.marquee?.let { raw ->
            resolveGamelistMedia(systemFolder, raw.removePrefix("./"))?.let {
                putIfAbsent(MediaType.MARQUEE, it, "gamelist")
            }
        }
        gl?.video?.let { raw ->
            resolveGamelistMedia(systemFolder, raw.removePrefix("./"))?.let {
                putIfAbsent(MediaType.VIDEO, it, "gamelist")
            }
        }

        return media.values.toList()
    }

    private fun mediaPathExists(path: String): Boolean {
        if (path.startsWith("content:", ignoreCase = true)) return true
        val file = File(path)
        return file.canRead() || file.isFile
    }

    private fun resolveDocumentRelative(systemDir: DocumentFile, raw: String): String? {
        val normalized = raw.trim().replace('\\', '/').removePrefix("./").trimStart('/')
        if (normalized.isEmpty()) return null
        if (normalized.startsWith("/")) {
            return normalized.takeIf { File(it).canRead() || it.startsWith("content:", true) }
        }
        var current: DocumentFile = systemDir
        val parts = normalized.split('/').filter { it.isNotBlank() }
        for ((index, part) in parts.withIndex()) {
            val next = current.listFiles().firstOrNull { child ->
                child.name.equals(part, ignoreCase = true) &&
                    (index == parts.lastIndex || child.isDirectory)
            } ?: return null
            current = next
        }
        if (!current.isFile) return null
        val fsPath = runCatching {
            DocumentsContract.getDocumentId(current.uri)
        }.getOrNull()?.let { SafPathResolver.documentIdToFilesystemPath(it) }
        return fsPath?.takeIf { File(it).canRead() } ?: current.uri.toString()
    }

    private fun normalizePath(path: String): String =
        path.trim().replace('\\', '/').removePrefix("./").trimStart('/')

    private fun SystemEntity.toSystemDef(): SystemDef =
        SystemDef(
            name = name,
            fullName = displayName,
            folder = folderName,
            extensions = extensionsCsv.split(',')
                .map { it.trim().removePrefix(".").lowercase() }
                .filter { it.isNotBlank() }
                .toSet(),
            platform = platform,
            commands = emptyList(),
        )
}
