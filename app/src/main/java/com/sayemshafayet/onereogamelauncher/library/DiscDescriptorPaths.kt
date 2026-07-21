package com.sayemshafayet.onereogamelauncher.library

import java.io.File

/** Parses CUE/GDI-style disc descriptor companion references. */
object DiscDescriptorPaths {
    private val FILE_LINE = Regex("""FILE\s+"([^"]+)"|FILE\s+(\S+)""", RegexOption.IGNORE_CASE)

    val MULTI_FILE_EXTENSIONS = setOf("cue", "gdi", "ccd", "toc", "mds", "m3u")

    fun isMultiFileDescriptor(path: String): Boolean {
        val ext = path.substringAfterLast('.', "").substringBefore('?').lowercase()
        return ext in MULTI_FILE_EXTENSIONS
    }

    fun parseCueFileNames(text: String): List<String> =
        FILE_LINE.findAll(text).map { match ->
            match.groupValues[1].ifBlank { match.groupValues[2] }.replace('\\', '/')
        }.filter { it.isNotBlank() }.toList()

    fun companionPathsRelativeTo(descriptorPath: String, text: String): List<String> {
        val parent = descriptorPath.replace('\\', '/').substringBeforeLast('/', "")
        return parseCueFileNames(text).map { ref ->
            if (parent.isEmpty()) ref else "$parent/$ref"
        }
    }

    fun cueCompanionsReadable(descriptorFile: File): Boolean {
        if (!descriptorFile.isFile || !descriptorFile.canRead()) return false
        val text = runCatching { descriptorFile.readText() }.getOrDefault("")
        if (text.isBlank()) return false
        val refs = parseCueFileNames(text)
        if (refs.isEmpty()) return true
        val dir = descriptorFile.parentFile ?: return false
        return refs.all { ref -> File(dir, ref.replace('\\', '/')).isFile }
    }
}
