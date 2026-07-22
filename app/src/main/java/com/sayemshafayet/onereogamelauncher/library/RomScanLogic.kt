package com.sayemshafayet.onereogamelauncher.library

/**
 * Pure scan helpers shared by [RomScanner] and unit tests.
 */
internal object RomScanLogic {
    /** Playlist / disc-descriptor extensions that identify a game even when absent from es_systems.xml. */
    val DESCRIPTOR_EXTENSIONS = setOf("cue", "gdi", "ccd", "toc", "mds")

    fun isPlayableRom(extension: String, systemExtensions: Set<String>): Boolean =
        extension == "m3u" ||
            extension in systemExtensions ||
            extension in DESCRIPTOR_EXTENSIONS

    fun gamelistMatch(
        gamelistByPath: Map<String, GamelistEntry>,
        relativePath: String,
        fileName: String,
    ): GamelistEntry? {
        gamelistByPath[relativePath]?.let { return it }
        val norm = normalizePath(relativePath)
        gamelistByPath.entries.firstOrNull { normalizePath(it.key) == norm }?.value?.let { return it }
        gamelistByPath.entries.firstOrNull {
            val keyNorm = normalizePath(it.key)
            keyNorm.endsWith("/$norm") || norm.endsWith("/$keyNorm") ||
                keyNorm.endsWith(norm) || norm.endsWith(keyNorm)
        }?.value?.let { return it }

        val byFullName = gamelistByPath.entries.filter {
            it.key.substringAfterLast('/').equals(fileName, ignoreCase = true)
        }
        if (byFullName.size == 1) return byFullName.single().value

        val fileStem = fileName.substringBeforeLast('.').lowercase()
        if (fileStem.isBlank()) return null

        val byStem = gamelistByPath.entries.filter {
            it.key.substringAfterLast('/').substringBeforeLast('.').equals(fileStem, ignoreCase = true)
        }
        if (byStem.size == 1) return byStem.single().value

        val scannedParent = relativePath.substringBeforeLast('/', "")
        if (scannedParent.isNotEmpty()) {
            byStem.firstOrNull { (path, _) ->
                val glParent = path.substringBeforeLast('/', "")
                scannedParent.equals(glParent, ignoreCase = true) ||
                    scannedParent.substringAfterLast('/').equals(glParent.substringAfterLast('/'), true) ||
                    glParent.endsWith(scannedParent.substringAfterLast('/'), ignoreCase = true)
            }?.value?.let { return it }
        }
        return byStem.firstOrNull()?.value
    }

    /**
     * Resolve a gamelist media tag relative to the matched entry's directory under the system folder.
     * Returns null when the path points at ES-DE downloaded_media (handled by [MediaLibrary]).
     */
    fun combineGamelistMediaPath(gamelistEntryDir: String, mediaRaw: String): String? {
        val normalized = mediaRaw.trim().replace('\\', '/').removePrefix("./").trimStart('/')
        if (normalized.isEmpty()) return null
        if (normalized.startsWith("downloaded_media/", ignoreCase = true)) return null
        if (normalized.startsWith('/') || normalized.startsWith("content:", ignoreCase = true)) {
            return normalized
        }
        val entryDir = gamelistEntryDir.trim().replace('\\', '/').trim('/')
        return if (entryDir.isNotEmpty()) "$entryDir/$normalized" else normalized
    }

    fun normalizePath(path: String): String =
        path.trim().replace('\\', '/').removePrefix("./").trimStart('/')

    fun resolveScanTitle(
        gamelistEntry: GamelistEntry?,
        fileName: String,
        arcadeMap: ArcadeRomMap?,
    ): String {
        gamelistEntry?.name?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        arcadeMap?.titleFor(fileName)?.let { return it }
        return fileName.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
    }
}
