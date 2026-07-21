package com.sayemshafayet.onereogamelauncher.systems

/**
 * ES-DE Android core paths look like:
 * `%INTERNALDATA%/%ANDROIDPACKAGE%/cores/mesen_libretro_android.so`
 * or legacy `/data/data/%ANDROIDPACKAGE%/cores/mesen_libretro_android.so`
 *
 * RetroArch's intent wants the core **filename** (or a resolved absolute path).
 * We always reduce extras to the `.so` basename so UI/launch don't show `/data/data/`.
 */
object LibretroCorePaths {
    private val CORE_SO = Regex("""([A-Za-z0-9._-]+_libretro(?:_android)?\.so)""", RegexOption.IGNORE_CASE)

    fun coreFileNameFromExtra(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return trimmed
        CORE_SO.find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        val afterSlash = trimmed.substringAfterLast('/')
        return afterSlash.ifBlank { trimmed }
    }
}
