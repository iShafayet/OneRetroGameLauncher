package com.sayemshafayet.onereogamelauncher.launch

import com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONArray

/**
 * Builds RetroArch-compatible ROM path strings.
 *
 * RetroArch's Android VFS prefers:
 * - absolute filesystem paths (when the RA process can read them), or
 * - `saf://` URIs: `saf://` + encode(treeUri) + `/` + relativePath
 *   where encode turns `/`→`%2F` and `%`→`%25` (RetroArch `retro_vfs_path_join_saf`).
 *
 * Raw `content://…/document/…` URIs only work on very new RetroArch builds.
 */
object RetroArchRomPaths {

    data class ResolvedRom(
        /** Value for RetroArch's `ROM` intent extra. */
        val romExtra: String,
        /** Tree URI to grant to RetroArch, if any. */
        val grantTreeUri: String? = null,
        /** Document/content URI to grant, if any. */
        val grantDocumentUri: String? = null,
        /** Additional document URIs to grant (multi-file disc sets). */
        val grantDocumentUris: List<String> = emptyList(),
    )

    fun resolve(
        romPath: String,
        romPathsJson: String,
        systemFolder: String,
        romsTreeUri: String?,
        romsDirPath: String?,
    ): ResolvedRom {
        val relative = relativeFromRomsRoot(
            romPath = romPath,
            romPathsJson = romPathsJson,
            systemFolder = systemFolder,
            romsDirPath = romsDirPath,
            romsTreeUri = romsTreeUri,
        )

        // 1) Prefer saf:// when we have a ROMs tree + relative path (scoped-storage safe).
        if (!romsTreeUri.isNullOrBlank() && !relative.isNullOrBlank()) {
            val tree = canonicalTreeUri(romsTreeUri)
            return ResolvedRom(
                romExtra = buildSafPath(tree, relative),
                grantTreeUri = tree,
                grantDocumentUri = romPath.takeIf { it.startsWith("content:", ignoreCase = true) },
            )
        }

        // 2) content:// document URI → saf:// (or pass through for newer RetroArch).
        if (romPath.startsWith("content:", ignoreCase = true)) {
            contentUriToSaf(romPath)?.let { saf ->
                return ResolvedRom(
                    romExtra = saf.romExtra,
                    grantTreeUri = saf.grantTreeUri,
                    grantDocumentUri = romPath,
                )
            }
            return ResolvedRom(romExtra = romPath, grantDocumentUri = romPath)
        }

        // 3) Absolute filesystem path — OK when RetroArch has storage access.
        if (RetroArchLauncher.isLikelyFilesystemPath(romPath) && File(romPath).isFile) {
            return ResolvedRom(romExtra = File(romPath).absolutePath)
        }

        return ResolvedRom(romExtra = romPath)
    }

    fun buildSafPath(treeUri: String, relativePath: String): String {
        val rel = relativePath.trim().trimStart('/').replace('\\', '/')
        val encodedTree = encodeTreeForSaf(treeUri)
        return if (rel.isEmpty()) "saf://$encodedTree" else "saf://$encodedTree/$rel"
    }

    /**
     * RetroArch encodes only `/` and `%` in the tree portion (not a full URL encoder).
     */
    fun encodeTreeForSaf(treeUri: String): String = buildString(treeUri.length * 2) {
        for (ch in treeUri) {
            when (ch) {
                '%' -> append("%25")
                '/' -> append("%2F")
                else -> append(ch)
            }
        }
    }

    fun relativeFromRomsRoot(
        romPath: String,
        romPathsJson: String,
        systemFolder: String,
        romsDirPath: String?,
        romsTreeUri: String?,
    ): String? {
        // romPathsJson entry is usually relative to the system folder (SAF scan).
        firstRomPathsEntry(romPathsJson)?.let { entry ->
            val normalized = entry.replace('\\', '/').trimStart('/')
            if (normalized.isNotBlank() && !normalized.startsWith("content:", ignoreCase = true)) {
                if (RetroArchLauncher.isLikelyFilesystemPath(normalized)) {
                    relativeUnderRoot(normalized, romsDirPath)?.let { return it }
                } else {
                    // Relative within system folder
                    val folder = systemFolder.trim('/').trim()
                    return if (folder.isBlank()) normalized else "$folder/$normalized"
                }
            }
        }

        if (romPath.startsWith("content:", ignoreCase = true)) {
            relativeFromContentUri(romPath)?.let { return it }
        }

        if (RetroArchLauncher.isLikelyFilesystemPath(romPath)) {
            relativeUnderRoot(romPath, romsDirPath)?.let { return it }
            // Fallback: …/ROMs/<system>/file from path segments when path hint missing
            if (systemFolder.isNotBlank()) {
                val marker = "/${systemFolder.trim('/')}/"
                val idx = romPath.replace('\\', '/').indexOf(marker, ignoreCase = true)
                if (idx >= 0) {
                    return romPath.replace('\\', '/')
                        .substring(idx + 1)
                        .trimStart('/')
                }
            }
        }

        // Last resort from tree URI + document id comparison already handled above
        if (!romsTreeUri.isNullOrBlank() && romPath.startsWith("content:", ignoreCase = true)) {
            relativeFromContentUri(romPath)?.let { return it }
        }
        return null
    }

    fun contentUriToSaf(contentUri: String): ResolvedRom? {
        if (!contentUri.startsWith("content:", ignoreCase = true)) return null
        val treeUri = treeUriFromDocumentUriString(contentUri) ?: return null
        val relative = relativeFromContentUri(contentUri) ?: return null
        return ResolvedRom(
            romExtra = buildSafPath(treeUri, relative),
            grantTreeUri = treeUri,
            grantDocumentUri = contentUri,
        )
    }

    fun relativeFromContentUri(contentUri: String): String? {
        if (!contentUri.startsWith("content:", ignoreCase = true)) return null
        val treeEncoded = contentUri.substringAfter("/tree/", missingDelimiterValue = "")
            .substringBefore('/')
            .substringBefore('?')
            .takeIf { it.isNotBlank() } ?: return null
        val docEncoded = contentUri.substringAfter("/document/", missingDelimiterValue = "")
            .substringBefore('?')
            .substringBefore('#')
            .takeIf { it.isNotBlank() } ?: return null

        val decodedTree = SafPathResolver.percentDecode(treeEncoded)
        val decodedDoc = SafPathResolver.percentDecode(docEncoded)
        if (!decodedDoc.startsWith(decodedTree)) return null
        return decodedDoc.removePrefix(decodedTree).trimStart('/').takeIf { it.isNotBlank() }
    }

    fun treeUriFromDocumentUriString(contentUri: String): String? {
        val schemeAuth = contentUri.substringBefore("/tree/")
        if (schemeAuth == contentUri || !schemeAuth.startsWith("content:", ignoreCase = true)) {
            return null
        }
        val treeDocEncoded = contentUri.substringAfter("/tree/")
            .substringBefore('/')
            .substringBefore('?')
            .takeIf { it.isNotBlank() } ?: return null
        return "$schemeAuth/tree/$treeDocEncoded"
    }

    fun canonicalTreeUri(romsTreeUri: String): String =
        treeUriFromDocumentUriString(romsTreeUri) ?: romsTreeUri

    private fun relativeUnderRoot(absolutePath: String, romsDirPath: String?): String? {
        if (romsDirPath.isNullOrBlank()) return null
        val root = File(romsDirPath).absolutePath.trimEnd('/')
        val abs = File(absolutePath).absolutePath
        if (!abs.startsWith(root)) return null
        return abs.removePrefix(root).trimStart('/').takeIf { it.isNotBlank() }
    }

    private fun firstRomPathsEntry(romPathsJson: String): String? =
        runCatching {
            val arr = JSONArray(romPathsJson)
            if (arr.length() == 0) return@runCatching null
            arr.optString(0).takeIf { it.isNotBlank() }
        }.getOrNull()

    /** URL-encode helper kept for tests / alternate encodings. */
    fun urlEncodePathSegment(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
}
