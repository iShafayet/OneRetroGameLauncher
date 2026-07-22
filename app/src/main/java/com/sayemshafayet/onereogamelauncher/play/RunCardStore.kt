package com.sayemshafayet.onereogamelauncher.play

import android.content.Context
import android.net.Uri
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglTreeFiles
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RunCardStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val collageGenerator: CollageGenerator,
    private val settingsRepository: SettingsRepository,
) {
    companion object {
        const val LOCAL_DIR = "run_cards"
        const val ORGL_SUBDIR = "run_cards"
    }

    fun localFile(commitmentId: Long): File =
        File(File(context.filesDir, LOCAL_DIR), "$commitmentId.png")

    fun relativeOrglPath(systemFolder: String, fileName: String, committedAt: Long): String =
        "$ORGL_SUBDIR/${fileBaseName(systemFolder, fileName, committedAt)}.png"

    suspend fun createRunCard(
        commitmentId: Long,
        systemFolder: String,
        fileName: String,
        committedAt: Long,
        input: CollageInput,
    ): String? = withContext(Dispatchers.IO) {
        val local = localFile(commitmentId)
        if (!collageGenerator.generateTo(local, input)) return@withContext null
        copyLocalToOrglDataDir(local, systemFolder, fileName, committedAt)
        local.absolutePath
    }

    suspend fun resolveRunCardPath(
        commitmentId: Long,
        systemFolder: String,
        fileName: String,
        committedAt: Long,
        existingPath: String?,
        orglRelativePath: String? = null,
    ): String? = withContext(Dispatchers.IO) {
        existingPath?.takeIf { it.isNotBlank() && File(it).isFile }?.let { return@withContext it }
        val local = localFile(commitmentId)
        if (local.isFile) return@withContext local.absolutePath
        val relative = orglRelativePath?.takeIf { it.isNotBlank() }
            ?: relativeOrglPath(systemFolder, fileName, committedAt)
        importFromOrglDataDir(local, relative)?.absolutePath
    }

    suspend fun ensureOnOrglDataDir(
        localPath: String?,
        systemFolder: String,
        fileName: String,
        committedAt: Long,
    ): String? = withContext(Dispatchers.IO) {
        val local = localPath?.let { File(it) }?.takeIf { it.isFile } ?: return@withContext null
        if (copyLocalToOrglDataDir(local, systemFolder, fileName, committedAt)) {
            relativeOrglPath(systemFolder, fileName, committedAt)
        } else {
            null
        }
    }

    private fun fileBaseName(systemFolder: String, fileName: String, committedAt: Long): String =
        "${sanitize(systemFolder)}_${sanitize(fileName)}_$committedAt"

    private fun sanitize(value: String): String =
        value.replace(Regex("""[\\/:*?"<>|]"""), "_").take(120)

    private suspend fun copyLocalToOrglDataDir(
        local: File,
        systemFolder: String,
        fileName: String,
        committedAt: Long,
    ): Boolean {
        val tree = orglTreeUri() ?: return false
        val pathHint = orglPathHint(tree)
        val relativePath = relativeOrglPath(systemFolder, fileName, committedAt)
        return OrglTreeFiles.writeBytes(context, tree, relativePath, pathHint, local.readBytes())
    }

    private suspend fun importFromOrglDataDir(destination: File, relativePath: String): File? {
        val tree = orglTreeUri() ?: return null
        val pathHint = orglPathHint(tree)
        val bytes = OrglTreeFiles.readBytes(context, tree, relativePath, pathHint) ?: return null
        destination.parentFile?.mkdirs()
        destination.writeBytes(bytes)
        return destination
    }

    private suspend fun orglTreeUri(): Uri? {
        val uri = settingsRepository.current().orglDataDirUri ?: return null
        return runCatching { Uri.parse(uri) }.getOrNull()
    }

    private suspend fun orglPathHint(tree: Uri): String? {
        val stored = settingsRepository.current().orglDataDirPath
        return OrglTreeFiles.resolvePathHint(context, tree, stored)
    }
}
