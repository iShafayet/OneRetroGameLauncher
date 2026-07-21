package com.sayemshafayet.onereogamelauncher.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

object ImageBitmapDecoder {
    fun decode(context: Context, path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            when {
                path.startsWith("content:", ignoreCase = true) ||
                    path.startsWith("file:", ignoreCase = true) -> {
                    context.contentResolver.openInputStream(Uri.parse(path))?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                path.startsWith("http", ignoreCase = true) -> {
                    java.net.URL(path).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                else -> {
                    val file = File(path)
                    if (file.canRead()) {
                        BitmapFactory.decodeFile(path)
                    } else {
                        context.contentResolver.openInputStream(Uri.parse(path))?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                }
            }
        }.getOrNull()
    }
}
