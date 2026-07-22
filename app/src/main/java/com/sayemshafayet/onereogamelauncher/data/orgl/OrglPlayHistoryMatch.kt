package com.sayemshafayet.onereogamelauncher.data.orgl

import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity

internal fun normalizeRomFileName(fileName: String): String =
    fileName.trim()
        .substringAfterLast('/')
        .substringAfterLast('\\')

internal fun resolveSystemForHistory(
    folderOrName: String,
    systemsByFolder: Map<String, SystemEntity>,
    systemsByName: Map<String, SystemEntity>,
    systemsByDisplayName: Map<String, SystemEntity>,
): SystemEntity? {
    val key = folderOrName.trim().lowercase()
    if (key.isEmpty()) return null
    return systemsByFolder[key]
        ?: systemsByName[key]
        ?: systemsByDisplayName[key]
}

/** Match within a preloaded system game list (no I/O). */
internal fun matchGameInList(
    fileName: String,
    title: String?,
    games: List<GameEntity>,
): GameEntity? {
    val baseName = normalizeRomFileName(fileName)
    if (baseName.isNotEmpty()) {
        games.firstOrNull { it.fileName.equals(baseName, ignoreCase = true) }?.let { return it }
        games.firstOrNull { normalizeRomFileName(it.fileName).equals(baseName, ignoreCase = true) }
            ?.let { return it }
        games.firstOrNull { game ->
            normalizeRomFileName(game.romPath).equals(baseName, ignoreCase = true) ||
                game.romPath.endsWith("/$baseName", ignoreCase = true) ||
                game.romPath.endsWith("\\$baseName", ignoreCase = true) ||
                game.romPath.endsWith(baseName, ignoreCase = true)
        }?.let { return it }
    }
    val trimmedTitle = title?.trim().orEmpty()
    if (trimmedTitle.isNotEmpty()) {
        games.firstOrNull { it.title.equals(trimmedTitle, ignoreCase = true) }?.let { return it }
    }
    return null
}

internal fun pickBestGameCandidate(
    systemId: Long?,
    candidates: List<GameEntity>,
): GameEntity? {
    if (candidates.isEmpty()) return null
    if (candidates.size == 1) return candidates.first()
    if (systemId != null) {
        candidates.firstOrNull { it.systemId == systemId }?.let { return it }
    }
    return candidates.first()
}
