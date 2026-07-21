package com.sayemshafayet.onereogamelauncher.ui.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Resolve Storage Access Framework tree URIs to absolute filesystem paths.
 *
 * Tree URIs look like:
 * `content://…/tree/primary%3AROMs%2FFavorites`
 * Document IDs must be percent-decoded (`%3A`→`:`, `%2F`→`/`) before building a real path.
 */
object SafPathResolver {

    fun resolvePath(context: Context, uri: Uri): String? {
        resolveTreeDocumentId(uri)?.let { docId ->
            documentIdToFilesystemPath(docId)?.let { candidate ->
                if (File(candidate).isDirectory) return normalize(candidate)
            }
        }
        // Last resort: raw file://
        if (uri.scheme.equals("file", ignoreCase = true)) {
            return uri.path?.let { normalize(it) }?.takeIf { File(it).isDirectory }
        }
        return null
    }

    /** Human-readable label for settings UI (never show raw %2F/%3A). */
    fun displayLabel(uriString: String?, pathHint: String?): String {
        if (!pathHint.isNullOrBlank()) return pathHint
        if (uriString.isNullOrBlank()) return "Not set"
        return runCatching {
            val uri = Uri.parse(uriString)
            resolveTreeDocumentId(uri)?.let { docId ->
                documentIdToFilesystemPath(docId) ?: percentDecode(docId)
            } ?: percentDecode(uriString)
        }.getOrDefault(uriString)
    }

    fun resolveTreeDocumentId(uri: Uri): String? {
        val fromContract = runCatching {
            if (DocumentsContract.isTreeUri(uri)) {
                DocumentsContract.getTreeDocumentId(uri)
            } else {
                null
            }
        }.getOrNull()
        if (!fromContract.isNullOrBlank()) return percentDecode(fromContract)

        return resolveTreeDocumentIdString(uri.toString())
    }

    fun resolveTreeDocumentIdString(treeUriString: String): String? {
        val marker = "/tree/"
        val idx = treeUriString.indexOf(marker)
        if (idx < 0) return null
        val encoded = treeUriString.substring(idx + marker.length)
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
        return percentDecode(encoded).takeIf { it.isNotBlank() }
    }

    fun documentIdToFilesystemPath(docId: String): String? {
        val decoded = percentDecode(docId)
        val split = decoded.split(":", limit = 2)
        if (split.size != 2) return null
        val volume = split[0]
        val relative = split[1].trimStart('/').trimEnd('/')
        if (relative.isBlank()) return null
        val candidates = if (volume.equals("primary", ignoreCase = true)) {
            listOf(
                "/storage/emulated/0/$relative",
                "/sdcard/$relative",
            )
        } else {
            // Removable / custom volumes: Android docs use /storage/<uuid>, but many
            // devices (and bind mounts) expose the same tree under /mnt/…
            listOf(
                "/storage/$volume/$relative",
                "/mnt/media_rw/$volume/$relative",
                "/mnt/$volume/$relative",
            )
        }
        val normalized = candidates.map { normalize(it) }
        return normalized.firstOrNull { path ->
            File(path).exists()
        } ?: normalized.firstOrNull()
    }

    /**
     * True if [root] looks like an ES-DE ROMs directory (contains at least one known system folder).
     */
    fun looksLikeRomsRoot(root: File, knownFolders: Collection<String>): Boolean {
        if (!root.isDirectory) return false
        val children = root.list()?.map { it.lowercase() }?.toSet().orEmpty()
        if (children.isEmpty()) return false
        return knownFolders.any { it.lowercase() in children }
    }

    fun percentDecode(input: String): String {
        if (!input.contains('%')) return input
        return runCatching {
            // Path-safe: keep literal '+' (URLDecoder would turn it into space)
            URLDecoder.decode(input.replace("+", "%2B"), StandardCharsets.UTF_8.name())
        }.getOrDefault(input)
    }

    fun normalize(path: String): String = percentDecode(path)

    /** Build a canonical tree Uri string for storage (as returned by the picker). */
    fun canonicalTreeUriString(uri: Uri): String = uri.toString()
}
