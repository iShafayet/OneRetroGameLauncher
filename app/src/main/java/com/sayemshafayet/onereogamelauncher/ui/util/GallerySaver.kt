package com.sayemshafayet.onereogamelauncher.ui.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object GallerySaver {
    fun savePng(context: Context, sourceFile: File, displayName: String): Result<Uri> {
        if (!sourceFile.exists()) {
            return Result.failure(IllegalStateException("Image file not found"))
        }
        val safeName = displayName
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .take(80)
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$safeName.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/ORGL",
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return Result.failure(IllegalStateException("Could not create gallery entry"))

        return runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            } ?: error("Could not open gallery output stream")
            val published = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, published, null, null)
            uri
        }.onFailure {
            resolver.delete(uri, null, null)
        }
    }
}
