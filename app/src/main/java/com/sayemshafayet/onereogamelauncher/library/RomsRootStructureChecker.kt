package com.sayemshafayet.onereogamelauncher.library

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.sayemshafayet.onereogamelauncher.domain.RomsStructureCheck
import com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shallow check that a ROMs root follows the ES-DE / ORGL layout:
 * one immediate child directory per system (`nes`, `gba`, …).
 */
@Singleton
class RomsRootStructureChecker @Inject constructor() {

    fun check(
        context: Context,
        romsUri: String?,
        romsPath: String?,
        knownFolders: Collection<String>,
    ): RomsStructureCheck {
        val known = knownFolders.map { it.lowercase() }.toSet()
        if (known.isEmpty()) {
            return RomsStructureCheck(
                matchedFolders = emptyList(),
                childDirectoryNames = emptyList(),
                isValid = false,
            )
        }

        val children = listChildDirectories(context, romsUri, romsPath)
        val matched = children
            .filter { it.lowercase() in known }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }

        return RomsStructureCheck(
            matchedFolders = matched,
            childDirectoryNames = children.sortedBy { it.lowercase() },
            isValid = matched.isNotEmpty(),
        )
    }

    private fun listChildDirectories(
        context: Context,
        romsUri: String?,
        romsPath: String?,
    ): List<String> {
        if (!romsUri.isNullOrBlank()) {
            val tree = runCatching { Uri.parse(romsUri) }.getOrNull()
            if (tree != null) {
                val root = runCatching { DocumentFile.fromTreeUri(context, tree) }.getOrNull()
                if (root != null) {
                    return root.listFiles()
                        .filter { it.isDirectory }
                        .mapNotNull { it.name }
                        .filter { it.isNotBlank() }
                }
            }
        }
        val path = romsPath?.takeIf { it.isNotBlank() } ?: return emptyList()
        val dir = File(path)
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            .orEmpty()
    }

    companion object {
        /** Example folders shown when teaching the layout. */
        val EXAMPLE_FOLDERS = listOf("nes", "snes", "gba", "gb", "n64", "psx", "psp", "genesis")

        fun looksLikeRomsRoot(path: String?, knownFolders: Collection<String>): Boolean {
            if (path.isNullOrBlank()) return false
            return SafPathResolver.looksLikeRomsRoot(File(path), knownFolders)
        }
    }
}
