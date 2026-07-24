package com.sayemshafayet.onereogamelauncher.data.orgl

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.sayemshafayet.onereogamelauncher.ui.util.SafIo
import com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver
import java.io.File

/**
 * Read/write plain files at the root of the ORGL data tree (SAF + optional filesystem path).
 */
object OrglTreeFiles {
    private const val TAG = "OrglTreeFiles"

    fun readText(context: Context, treeUri: Uri, fileName: String, pathHint: String?): String? {
        val fromSaf = readSafText(context, treeUri, fileName)
        // ORGL folders are SAF trees — prefer SAF so a stale/wrong pathHint cannot shadow it.
        if (treeUri.scheme.equals("content", ignoreCase = true)) {
            if (fromSaf != null) return fromSaf
        }
        val fromPath = pathHint?.let { root ->
            val file = File(root, fileName)
            if (file.isFile) runCatching { file.readText(Charsets.UTF_8) }.getOrNull() else null
        }
        return fromPath ?: fromSaf
    }

    private fun readSafText(context: Context, treeUri: Uri, fileName: String): String? {
        val root = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull() ?: return null
        val doc = findFile(root, fileName) ?: return null
        return try {
            SafIo.openInputStream(context, doc.uri)?.use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            Log.w(TAG, "readText failed for $fileName", e)
            null
        }
    }

    fun writeText(
        context: Context,
        treeUri: Uri,
        fileName: String,
        pathHint: String?,
        text: String,
    ): Boolean {
        var ok = false
        pathHint?.let { root ->
            runCatching {
                File(root).mkdirs()
                File(root, fileName).writeText(text, Charsets.UTF_8)
                ok = true
            }.onFailure { Log.w(TAG, "filesystem write failed for $fileName", it) }
        }
        val root = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull() ?: return ok
        if (!SafIo.canWrite(root)) return ok
        val existing = findFile(root, fileName)
        val doc = existing ?: createFile(root, fileName) ?: return ok
        return try {
            SafIo.openOutputStream(context, doc.uri)?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return ok
            true
        } catch (e: Exception) {
            Log.e(TAG, "SAF write failed for $fileName", e)
            ok
        }
    }

    fun delete(context: Context, treeUri: Uri, fileName: String, pathHint: String?): Boolean {
        var deleted = false
        pathHint?.let { root ->
            val file = File(root, fileName)
            if (file.isFile) deleted = file.delete() || deleted
        }
        val root = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull() ?: return deleted
        val doc = findFile(root, fileName) ?: return deleted
        return runCatching { doc.delete() }.getOrDefault(false) || deleted
    }

    fun exists(context: Context, treeUri: Uri, fileName: String, pathHint: String?): Boolean {
        pathHint?.let { root ->
            if (File(root, fileName).isFile) return true
        }
        val root = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull() ?: return false
        return findFile(root, fileName) != null
    }

    fun readBytes(context: Context, treeUri: Uri, relativePath: String, pathHint: String?): ByteArray? {
        pathHint?.let { root ->
            val file = File(root, relativePath)
            if (file.isFile) {
                return runCatching { file.readBytes() }.getOrNull()
            }
        }
        return readSafBytes(context, treeUri, relativePath)
    }

    fun writeBytes(
        context: Context,
        treeUri: Uri,
        relativePath: String,
        pathHint: String?,
        bytes: ByteArray,
    ): Boolean {
        var ok = false
        pathHint?.let { root ->
            runCatching {
                val file = File(root, relativePath)
                file.parentFile?.mkdirs()
                file.writeBytes(bytes)
                ok = true
            }.onFailure { Log.w(TAG, "filesystem write failed for $relativePath", it) }
        }
        val root = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull() ?: return ok
        if (!SafIo.canWrite(root)) return ok
        val target = findOrCreateFile(root, relativePath) ?: return ok
        return try {
            SafIo.openOutputStream(context, target.uri)?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: return ok
            true
        } catch (e: Exception) {
            Log.e(TAG, "SAF write failed for $relativePath", e)
            ok
        }
    }

    private fun readSafBytes(context: Context, treeUri: Uri, relativePath: String): ByteArray? {
        val root = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull() ?: return null
        val doc = findNestedFile(root, relativePath) ?: return null
        return try {
            SafIo.openInputStream(context, doc.uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.w(TAG, "readBytes failed for $relativePath", e)
            null
        }
    }

    private fun findOrCreateFile(root: DocumentFile, relativePath: String): DocumentFile? =
        runCatching {
            val parts = relativePath.split('/').filter { it.isNotBlank() }
            if (parts.isEmpty()) return null
            var dir = root
            for (part in parts.dropLast(1)) {
                dir = SafIo.listChildren(dir).firstOrNull { it.isDirectory && it.name == part }
                    ?: dir.createDirectory(part)
                    ?: return null
            }
            val fileName = parts.last()
            findFile(dir, fileName)?.let { return it }
            val ext = fileName.substringAfterLast('.', missingDelimiterValue = "")
            val base = fileName.substringBeforeLast('.', fileName)
            val mime = when (ext.lowercase()) {
                "json" -> "application/json"
                "png" -> "image/png"
                else -> "application/octet-stream"
            }
            dir.createFile(mime, base)
            findFile(dir, fileName)
        }.getOrNull()

    private fun findNestedFile(root: DocumentFile, relativePath: String): DocumentFile? =
        runCatching {
            val parts = relativePath.split('/').filter { it.isNotBlank() }
            if (parts.isEmpty()) return null
            var dir = root
            for (part in parts.dropLast(1)) {
                dir = SafIo.listChildren(dir).firstOrNull { it.isDirectory && it.name == part }
                    ?: return null
            }
            findFile(dir, parts.last())
        }.getOrNull()

    fun resolvePathHint(context: Context, treeUri: Uri, storedHint: String?): String? =
        storedHint ?: SafPathResolver.resolvePath(context, treeUri)

    private fun findFile(root: DocumentFile, fileName: String): DocumentFile? =
        runCatching {
            val files = SafIo.listChildren(root).filter { it.isFile }
            files.firstOrNull { it.name.equals(fileName, ignoreCase = true) }?.let { return it }
            val base = fileName.substringBeforeLast('.', fileName)
            // SAF createFile("application/json", "play_history") may omit or double the extension.
            files.firstOrNull { doc ->
                val name = doc.name ?: return@firstOrNull false
                name.equals(base, ignoreCase = true) ||
                    name.equals("$base.json", ignoreCase = true) ||
                    name.equals("$fileName.json", ignoreCase = true)
            }
        }.getOrNull()

    private fun createFile(root: DocumentFile, fileName: String): DocumentFile? =
        runCatching {
            val base = fileName.substringBeforeLast('.', fileName)
            val ext = fileName.substringAfterLast('.', missingDelimiterValue = "")
            val mime = when (ext.lowercase()) {
                "json" -> "application/json"
                else -> "application/octet-stream"
            }
            root.createFile(mime, base)
            findFile(root, fileName)
        }.getOrNull()
}
