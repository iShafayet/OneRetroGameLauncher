package com.sayemshafayet.onereogamelauncher.library

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver
import java.io.File

/**
 * Lazy, read-only resolver for ES-DE-style `downloaded_media/<system>/<type>/<file>`.
 *
 * Indexes **per system + media folder** on demand (not the entire tree up front).
 * Prefer SAF [DocumentFile] — `java.io.File.listFiles()` is unreliable on secondary storage.
 */
class MediaLibrary private constructor(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val label: String,
    private val mediaRootDoc: DocumentFile?,
    private val mediaRootFile: File?,
) {
    /** Cache: (systemLower, mediaFolderLower) → basenameLower → path */
    private val folderCache =
        mutableMapOf<Pair<String, String>, Map<String, String>>()

    /** Cache: systemLower → DocumentFile system dir */
    private val systemDocCache = mutableMapOf<String, DocumentFile?>()

    fun find(
        systemKeys: Collection<String>,
        baseNames: Collection<String>,
        mediaFolder: String,
    ): String? {
        val keys = expandKeys(baseNames)
        if (keys.isEmpty()) return null
        val folder = mediaFolder.lowercase()
        for (system in systemKeys.map { it.lowercase() }.filter { it.isNotBlank() }.distinct()) {
            val index = folderIndex(system, folder)
            for (key in keys) {
                index[key]?.let { return it }
            }
        }
        return null
    }

    fun isAvailable(): Boolean = mediaRootDoc != null || mediaRootFile != null

    fun describe(): String = buildString {
        append(label)
        append(" doc=").append(mediaRootDoc?.uri)
        append(" file=").append(mediaRootFile?.absolutePath)
        append(" available=").append(isAvailable())
    }

    private fun folderIndex(system: String, mediaFolder: String): Map<String, String> {
        val cacheKey = system to mediaFolder
        folderCache[cacheKey]?.let { return it }
        val built = buildFolderIndex(system, mediaFolder)
        folderCache[cacheKey] = built
        if (built.isNotEmpty()) {
            Log.i(TAG, "$label indexed $system/$mediaFolder → ${built.size} files")
        }
        return built
    }

    private fun buildFolderIndex(system: String, mediaFolder: String): Map<String, String> {
        val out = linkedMapOf<String, String>()

        // SAF first
        val systemDoc = systemDoc(system)
        if (systemDoc != null) {
            val typeDir = findChildDir(systemDoc, mediaFolder)
            if (typeDir != null) {
                typeDir.listFiles().forEach { file ->
                    if (!file.isFile) return@forEach
                    val name = file.name ?: return@forEach
                    val base = name.substringBeforeLast('.')
                    if (base.isBlank()) return@forEach
                    val path = storagePathFor(file)
                    putAllKeys(out, base, path)
                }
            }
        }

        // Filesystem fallback (primary storage / readable mounts)
        if (out.isEmpty() && mediaRootFile != null) {
            val typeDir = File(File(mediaRootFile, system), mediaFolder)
            if (typeDir.isDirectory) {
                typeDir.listFiles()?.forEach { file ->
                    if (!file.isFile) return@forEach
                    val base = file.nameWithoutExtension
                    if (base.isBlank()) return@forEach
                    // Always prefer content-style access when we can; File path only if readable
                    val path = if (file.canRead()) file.absolutePath else file.absolutePath
                    putAllKeys(out, base, path)
                }
            }
        }
        return out
    }

    private fun systemDoc(system: String): DocumentFile? {
        if (mediaRootDoc == null) return null
        systemDocCache[system]?.let { return it }
        // Try exact + case-insensitive
        val found = findChildDir(mediaRootDoc, system)
        systemDocCache[system] = found
        return found
    }

    companion object {
        private const val TAG = "MediaLibrary"
        fun open(
            context: Context,
            uriString: String?,
            pathHint: String?,
            label: String,
        ): MediaLibrary {
            var docRoot: DocumentFile? = null
            var fileRoot: File? = null

            if (!uriString.isNullOrBlank()) {
                val uri = runCatching { Uri.parse(uriString) }.getOrNull()
                val tree = uri?.let { DocumentFile.fromTreeUri(context, it) }
                if (tree == null) {
                    Log.w(TAG, "$label: cannot open tree URI=$uriString")
                } else {
                    val children = tree.listFiles().mapNotNull { it.name }
                    Log.i(
                        TAG,
                        "$label SAF root name=${tree.name} children(${children.size})=" +
                            children.take(20),
                    )
                    docRoot = findDownloadedMediaDoc(tree, depth = 0)
                    if (docRoot == null) {
                        Log.w(
                            TAG,
                            "$label: no downloaded_media under SAF root. " +
                                "Pick the ES-DE data folder (the one that contains downloaded_media/).",
                        )
                    } else {
                        Log.i(
                            TAG,
                            "$label: using media root name=${docRoot.name} uri=${docRoot.uri}",
                        )
                    }
                }
            }

            if (!pathHint.isNullOrBlank()) {
                val root = File(SafPathResolver.normalize(pathHint))
                fileRoot = findDownloadedMediaFile(root)
                Log.i(
                    TAG,
                    "$label pathHint=$pathHint → fileRoot=${fileRoot?.absolutePath} " +
                        "listable=${fileRoot?.list()?.take(5)}",
                )
            }

            if (docRoot == null && fileRoot == null) {
                if (uriString.isNullOrBlank() && pathHint.isNullOrBlank()) {
                    Log.i(TAG, "$label not configured")
                }
            }

            return MediaLibrary(context, label, docRoot, fileRoot)
        }

        /** Prefer matching against raw + ES-DE-sanitized basenames. */
        fun expandKeys(baseNames: Collection<String>): List<String> {
            val out = linkedSetOf<String>()
            for (raw in baseNames) {
                val t = raw.trim()
                if (t.isBlank()) continue
                out += t.lowercase()
                out += sanitizeEsde(t).lowercase()
                // Stem without trailing "(USA)" etc. as weak fallback
                val stripped = t.replace(Regex("""\s*\([^)]*\)\s*"""), " ").trim()
                if (stripped.isNotBlank() && stripped != t) {
                    out += stripped.lowercase()
                    out += sanitizeEsde(stripped).lowercase()
                }
            }
            return out.toList()
        }

        /** Roughly mirror ES-DE forbidden filename chars. */
        fun sanitizeEsde(name: String): String =
            name.replace(Regex("""[\\/:*?"<>|]"""), "_")
                .replace(Regex("""\s+"""), " ")
                .trim()
                .trimEnd('.')

        private fun putAllKeys(map: MutableMap<String, String>, base: String, path: String) {
            map.putIfAbsent(base.lowercase(), path)
            map.putIfAbsent(sanitizeEsde(base).lowercase(), path)
        }

        private fun findDownloadedMediaDoc(root: DocumentFile, depth: Int): DocumentFile? {
            if (root.name.equals("downloaded_media", ignoreCase = true)) return root
            if (looksLikeMediaRootDoc(root)) return root
            // Immediate child
            findChildDir(root, "downloaded_media")?.let { return it }
            if (depth >= 2) return null
            // Search one/two levels (e.g. Emulation/ES-DE/downloaded_media)
            root.listFiles().forEach { child ->
                if (!child.isDirectory) return@forEach
                findDownloadedMediaDoc(child, depth + 1)?.let { return it }
            }
            return null
        }

        private fun looksLikeMediaRootDoc(root: DocumentFile): Boolean {
            val children = root.listFiles().filter { it.isDirectory }
            // A couple of system dirs that themselves contain covers/miximages
            var hits = 0
            for (sys in children.take(12)) {
                val types = sys.listFiles()
                if (types.any {
                        it.isDirectory && (
                            it.name.equals("covers", true) ||
                                it.name.equals("miximages", true) ||
                                it.name.equals("screenshots", true)
                            )
                    }
                ) {
                    hits++
                }
                if (hits >= 1) return true
            }
            return false
        }

        private fun findDownloadedMediaFile(root: File): File? {
            if (!root.exists()) return null
            if (root.name.equals("downloaded_media", ignoreCase = true) && root.isDirectory) {
                return root
            }
            File(root, "downloaded_media").takeIf { it.isDirectory }?.let { return it }
            // depth-2 search
            root.listFiles()?.filter { it.isDirectory }?.forEach { child ->
                if (child.name.equals("downloaded_media", true)) return child
                File(child, "downloaded_media").takeIf { it.isDirectory }?.let { return it }
            }
            if (looksLikeMediaRootFile(root)) return root
            return null
        }

        private fun looksLikeMediaRootFile(root: File): Boolean {
            val children = root.listFiles()?.filter { it.isDirectory }.orEmpty()
            return children.any { sys ->
                File(sys, "covers").isDirectory || File(sys, "miximages").isDirectory
            }
        }

        private fun findChildDir(parent: DocumentFile, name: String): DocumentFile? {
            parent.listFiles().forEach { child ->
                if (child.isDirectory && child.name.equals(name, ignoreCase = true)) return child
            }
            return null
        }

        private fun storagePathFor(file: DocumentFile): String {
            // Prefer content:// — File paths on SD are often not readable by Coil/BitmapFactory.
            return file.uri.toString()
        }
    }
}
