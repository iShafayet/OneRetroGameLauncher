package com.sayemshafayet.onereogamelauncher.data.orgl

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.sayemshafayet.onereogamelauncher.ui.util.SafIo
import java.io.File
import org.json.JSONObject

/**
 * Manages the ORGL data directory contract: `ORGL.json` at the root with
 * [SPEC_VERSION]. Existing compatible directories are reused as-is (idempotent).
 */
object OrglDataDirectory {
    const val META_FILE_NAME = "ORGL.json"
    const val SPEC_VERSION = 1
    private const val TAG = "OrglDataDirectory"

    data class Meta(
        val specVersion: Int = SPEC_VERSION,
        val app: String = "OneRetroGameLauncher",
        val createdAtMs: Long = System.currentTimeMillis(),
        val lastOpenedAtMs: Long = System.currentTimeMillis(),
    )

    sealed class PrepareResult {
        data class Ready(val meta: Meta, val reusedExisting: Boolean) : PrepareResult()
        data class Incompatible(val foundVersion: Int?) : PrepareResult()
        data class Failed(val message: String) : PrepareResult()
    }

    /**
     * Validate / stamp [ORGL.json] under the given tree URI.
     * Write access is required.
     */
    fun prepare(context: Context, treeUri: Uri): PrepareResult {
        return runCatching { prepareInternal(context, treeUri) }
            .getOrElse { e ->
                Log.w(TAG, "prepare failed", e)
                PrepareResult.Failed(e.message ?: "Could not prepare ORGL data folder")
            }
    }

    private fun prepareInternal(context: Context, treeUri: Uri): PrepareResult {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return PrepareResult.Failed("Could not open the selected folder")
        if (!SafIo.canWrite(root)) {
            return PrepareResult.Failed("ORGL data folder must be writable")
        }

        val existing = findMetaDocument(root)
        return if (existing != null) {
            when (val parsed = readMeta(context, existing)) {
                is MetaRead.Ok -> {
                    if (parsed.meta.specVersion != SPEC_VERSION) {
                        PrepareResult.Incompatible(parsed.meta.specVersion)
                    } else {
                        val updated = parsed.meta.copy(lastOpenedAtMs = System.currentTimeMillis())
                        writeMeta(context, existing, updated)
                        PrepareResult.Ready(updated, reusedExisting = true)
                    }
                }
                is MetaRead.BadVersion -> PrepareResult.Incompatible(parsed.version)
                is MetaRead.Corrupt -> PrepareResult.Incompatible(null)
                is MetaRead.Failed -> PrepareResult.Failed(parsed.message)
            }
        } else {
            // Also check filesystem path if we can resolve it (legacy empty / unmarked dirs).
            val pathHint = runCatching {
                com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver.resolvePath(context, treeUri)
            }.getOrNull()
            if (pathHint != null) {
                val file = File(pathHint, META_FILE_NAME)
                if (file.isFile) {
                    return when (val parsed = readMetaFile(file)) {
                        is MetaRead.Ok -> {
                            if (parsed.meta.specVersion != SPEC_VERSION) {
                                PrepareResult.Incompatible(parsed.meta.specVersion)
                            } else {
                                val updated = parsed.meta.copy(lastOpenedAtMs = System.currentTimeMillis())
                                runCatching { file.writeText(encodeMeta(updated)) }
                                PrepareResult.Ready(updated, reusedExisting = true)
                            }
                        }
                        is MetaRead.BadVersion -> PrepareResult.Incompatible(parsed.version)
                        is MetaRead.Corrupt -> PrepareResult.Incompatible(null)
                        is MetaRead.Failed -> PrepareResult.Failed(parsed.message)
                    }
                }
            }

            val created = createMetaDocument(root)
                ?: return PrepareResult.Failed("Could not create $META_FILE_NAME in the folder")
            val meta = Meta()
            if (!writeMeta(context, created, meta)) {
                return PrepareResult.Failed("Could not write $META_FILE_NAME")
            }
            // Prefer also writing via File when path is available (belt and suspenders).
            pathHint?.let {
                runCatching { File(it, META_FILE_NAME).writeText(encodeMeta(meta)) }
            }
            Log.i(TAG, "Created $META_FILE_NAME (specVersion=$SPEC_VERSION)")
            PrepareResult.Ready(meta, reusedExisting = false)
        }
    }

    fun incompatibleMessage(foundVersion: Int?): String {
        val versionLabel = foundVersion?.toString() ?: "unknown / unreadable"
        return "This folder already has an $META_FILE_NAME from an incompatible ORGL install " +
            "(spec version $versionLabel; this app requires $SPEC_VERSION).\n\n" +
            "Move or rename that folder, then choose an empty folder — or pick a different directory."
    }

    private sealed class MetaRead {
        data class Ok(val meta: Meta) : MetaRead()
        data class BadVersion(val version: Int) : MetaRead()
        data object Corrupt : MetaRead()
        data class Failed(val message: String) : MetaRead()
    }

    private fun findMetaDocument(root: DocumentFile): DocumentFile? =
        SafIo.listChildren(root).firstOrNull {
            it.isFile && it.name.equals(META_FILE_NAME, ignoreCase = true)
        }

    private fun createMetaDocument(root: DocumentFile): DocumentFile? =
        runCatching {
            // Display name without extension — SAF appends .json from the MIME type on many devices.
            root.createFile("application/json", "ORGL")
            findMetaDocument(root)
        }.getOrNull()

    private fun readMeta(context: Context, doc: DocumentFile): MetaRead {
        return try {
            val text = SafIo.openInputStream(context, doc.uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return MetaRead.Failed("Could not read $META_FILE_NAME")
            parseMetaText(text)
        } catch (e: Exception) {
            Log.w(TAG, "readMeta failed", e)
            MetaRead.Failed(e.message ?: "Could not read $META_FILE_NAME")
        }
    }

    private fun readMetaFile(file: File): MetaRead =
        try {
            parseMetaText(file.readText())
        } catch (e: Exception) {
            MetaRead.Failed(e.message ?: "Could not read $META_FILE_NAME")
        }

    private fun parseMetaText(text: String): MetaRead {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return MetaRead.Corrupt
        return try {
            val json = JSONObject(trimmed)
            if (!json.has("specVersion")) return MetaRead.Corrupt
            val version = json.getInt("specVersion")
            if (version != SPEC_VERSION) return MetaRead.BadVersion(version)
            MetaRead.Ok(
                Meta(
                    specVersion = version,
                    app = json.optString("app", "OneRetroGameLauncher"),
                    createdAtMs = json.optLong("createdAtMs", System.currentTimeMillis()),
                    lastOpenedAtMs = json.optLong("lastOpenedAtMs", System.currentTimeMillis()),
                ),
            )
        } catch (_: Exception) {
            MetaRead.Corrupt
        }
    }

    private fun encodeMeta(meta: Meta): String {
        val json = JSONObject()
        json.put("specVersion", meta.specVersion)
        json.put("app", meta.app)
        json.put("createdAtMs", meta.createdAtMs)
        json.put("lastOpenedAtMs", meta.lastOpenedAtMs)
        return json.toString(2)
    }

    private fun writeMeta(context: Context, doc: DocumentFile, meta: Meta): Boolean {
        return try {
            SafIo.openOutputStream(context, doc.uri)?.use { out ->
                out.write(encodeMeta(meta).toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return false
            true
        } catch (e: Exception) {
            Log.e(TAG, "writeMeta failed", e)
            false
        }
    }
}
