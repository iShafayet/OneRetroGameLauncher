package com.sayemshafayet.onereogamelauncher.ui.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream
import java.io.OutputStream

/**
 * Soft-fail wrappers for Storage Access Framework I/O.
 * Revoked permissions / deleted trees must never crash the process.
 */
object SafIo {
    fun listChildren(doc: DocumentFile): Array<DocumentFile> =
        runCatching { doc.listFiles() }.getOrDefault(emptyArray())

    fun openInputStream(context: Context, uri: Uri): InputStream? =
        runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()

    fun openOutputStream(context: Context, uri: Uri, mode: String = "wt"): OutputStream? =
        runCatching { context.contentResolver.openOutputStream(uri, mode) }.getOrNull()

    fun isDirectory(doc: DocumentFile): Boolean =
        runCatching { doc.isDirectory }.getOrDefault(false)

    fun canWrite(doc: DocumentFile): Boolean =
        runCatching { doc.canWrite() }.getOrDefault(false)

    fun length(doc: DocumentFile?): Long =
        runCatching { doc?.length() ?: -1L }.getOrDefault(-1L)
}
