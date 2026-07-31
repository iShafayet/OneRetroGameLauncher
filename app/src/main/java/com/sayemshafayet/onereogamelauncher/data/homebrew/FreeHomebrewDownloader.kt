package com.sayemshafayet.onereogamelauncher.data.homebrew

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.sayemshafayet.onereogamelauncher.domain.FreeHomebrewGame
import com.sayemshafayet.onereogamelauncher.ui.util.SafIo
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface FreeHomebrewDownloadResult {
    data class Success(val relativePath: String) : FreeHomebrewDownloadResult
    data class Failed(val message: String) : FreeHomebrewDownloadResult
}

@Singleton
class FreeHomebrewDownloader @Inject constructor(
    private val okHttp: OkHttpClient,
) {
    suspend fun downloadToRomsRoot(
        context: Context,
        romsUri: String,
        romsPath: String?,
        game: FreeHomebrewGame,
    ): FreeHomebrewDownloadResult = withContext(Dispatchers.IO) {
        val bytes = downloadBytes(game.downloadUrl)
            ?: return@withContext FreeHomebrewDownloadResult.Failed(
                "Couldn’t download ${game.title}. Check your connection and try again.",
            )

        val relative = "${game.systemFolder}/${game.fileName}"
        val wroteSaf = writeViaSaf(context, romsUri, game, bytes)
        val wroteFile = if (!romsPath.isNullOrBlank()) {
            writeViaFilesystem(romsPath, game, bytes)
        } else {
            false
        }

        if (wroteSaf || wroteFile) {
            FreeHomebrewDownloadResult.Success(relative)
        } else {
            FreeHomebrewDownloadResult.Failed(
                "Couldn’t write ${game.fileName} into your ROMs folder. " +
                    "Make sure ORGL has write access to that folder.",
            )
        }
    }

    private fun downloadBytes(url: String): ByteArray? {
        val request = Request.Builder().url(url).get().build()
        return runCatching {
            okHttp.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "download failed ${response.code} for $url")
                    return@use null
                }
                response.body?.bytes()
            }
        }.onFailure { Log.e(TAG, "download error for $url", it) }.getOrNull()
    }

    private fun writeViaFilesystem(romsPath: String, game: FreeHomebrewGame, bytes: ByteArray): Boolean {
        return runCatching {
            val systemDir = File(romsPath, game.systemFolder)
            if (!systemDir.exists() && !systemDir.mkdirs()) return false
            // Always use the catalog file name so the extension is exact (e.g. Apotris.gba).
            File(systemDir, game.fileName).writeBytes(bytes)
            true
        }.onFailure { Log.w(TAG, "filesystem write failed", it) }.getOrDefault(false)
    }

    private fun writeViaSaf(
        context: Context,
        romsUri: String,
        game: FreeHomebrewGame,
        bytes: ByteArray,
    ): Boolean {
        val tree = runCatching { Uri.parse(romsUri) }.getOrNull() ?: return false
        val root = runCatching { DocumentFile.fromTreeUri(context, tree) }.getOrNull() ?: return false
        if (!SafIo.canWrite(root)) return false

        val systemDir = findOrCreateDirectory(root, game.systemFolder) ?: return false
        val target = resolveSafTarget(context, systemDir, game.fileName) ?: return false
        val finalName = target.name.orEmpty()
        if (!hasDesiredExtension(finalName, game.fileName)) {
            Log.e(
                TAG,
                "SAF file name missing extension after create/rename: got='$finalName' want='${game.fileName}'",
            )
            return false
        }

        return try {
            SafIo.openOutputStream(context, target.uri, mode = "wt")?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: return false
            true
        } catch (e: Exception) {
            Log.e(TAG, "SAF write failed for ${game.fileName}", e)
            false
        }
    }

    /**
     * Ensure a DocumentFile exists whose display name matches [desiredName] exactly
     * (including extension). SAF providers often strip or invent extensions when
     * createFile() is given a base name only.
     */
    private fun resolveSafTarget(
        context: Context,
        parent: DocumentFile,
        desiredName: String,
    ): DocumentFile? {
        val ext = desiredName.substringAfterLast('.', missingDelimiterValue = "")
        val base = desiredName.substringBeforeLast('.', desiredName)
        val children = parent.listFiles().filter { it.isFile }

        // Exact match.
        children.firstOrNull { it.name.equals(desiredName, ignoreCase = true) }?.let { return it }

        // Extensionless or otherwise mangled leftover from an earlier download — rename it.
        val mangled = children.firstOrNull { doc ->
            val name = doc.name ?: return@firstOrNull false
            name.equals(base, ignoreCase = true) ||
                name.equals("$base.$ext.$ext", ignoreCase = true) ||
                (ext.isNotEmpty() &&
                    name.startsWith("$base.", ignoreCase = true) &&
                    !name.equals(desiredName, ignoreCase = true))
        }
        if (mangled != null) {
            renameSaf(context, mangled, desiredName)?.let { renamed ->
                if (hasDesiredExtension(renamed.name.orEmpty(), desiredName)) return renamed
            }
            // Rename failed; delete mangled copy and recreate cleanly.
            runCatching { mangled.delete() }
        }

        // Create with the FULL file name (including .gba). Do not pass the base name only —
        // application/octet-stream has no extension mapping, so that yields "Apotris" with no suffix.
        val mime = mimeForExtension(ext)
        val created = parent.createFile(mime, desiredName)
            ?: parent.createFile(mime, base)
            ?: return null

        val createdName = created.name.orEmpty()
        if (hasDesiredExtension(createdName, desiredName)) {
            // Prefer exact desired name if the provider only differs by case.
            if (createdName.equals(desiredName, ignoreCase = true)) return created
        }

        // Provider stripped the extension or doubled it — rename to the catalog name.
        renameSaf(context, created, desiredName)?.let { renamed ->
            if (hasDesiredExtension(renamed.name.orEmpty(), desiredName)) return renamed
        }

        // Last resort: if createFile(base) left an extensionless file, try again after delete
        // using DocumentsContract directly with the full display name.
        if (!hasDesiredExtension(createdName, desiredName)) {
            runCatching { created.delete() }
            return createViaDocumentsContract(context, parent, desiredName, mime)
        }

        return created.takeIf { hasDesiredExtension(it.name.orEmpty(), desiredName) }
    }

    private fun createViaDocumentsContract(
        context: Context,
        parent: DocumentFile,
        desiredName: String,
        mime: String,
    ): DocumentFile? {
        val uri = runCatching {
            DocumentsContract.createDocument(context.contentResolver, parent.uri, mime, desiredName)
        }.getOrNull() ?: return null
        val doc = DocumentFile.fromSingleUri(context, uri) ?: return null
        return doc.takeIf { hasDesiredExtension(it.name.orEmpty(), desiredName) }
    }

    private fun renameSaf(context: Context, doc: DocumentFile, newName: String): DocumentFile? {
        val renamedUri = runCatching {
            DocumentsContract.renameDocument(context.contentResolver, doc.uri, newName)
        }.getOrNull() ?: return null
        return DocumentFile.fromSingleUri(context, renamedUri)
            ?: DocumentFile.fromSingleUri(context, doc.uri)
    }

    private fun findOrCreateDirectory(parent: DocumentFile, name: String): DocumentFile? {
        parent.listFiles().firstOrNull {
            it.isDirectory && it.name.equals(name, ignoreCase = true)
        }?.let { return it }
        return parent.createDirectory(name)
    }

    private fun hasDesiredExtension(actualName: String, desiredName: String): Boolean {
        val ext = desiredName.substringAfterLast('.', missingDelimiterValue = "")
        if (ext.isEmpty()) return actualName.equals(desiredName, ignoreCase = true)
        return actualName.endsWith(".$ext", ignoreCase = true) &&
            !actualName.endsWith(".$ext.$ext", ignoreCase = true)
    }

    private fun mimeForExtension(ext: String): String = when (ext.lowercase()) {
        "gba" -> "application/x-gba-rom"
        "gb" -> "application/x-gameboy-rom"
        "gbc" -> "application/x-gameboy-color-rom"
        "nes" -> "application/x-nes-rom"
        "zip" -> "application/zip"
        else -> "application/octet-stream"
    }

    companion object {
        private const val TAG = "FreeHomebrewDownloader"
    }
}
