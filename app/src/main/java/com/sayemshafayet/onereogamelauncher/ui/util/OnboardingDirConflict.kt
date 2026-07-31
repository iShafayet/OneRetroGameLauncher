package com.sayemshafayet.onereogamelauncher.ui.util

/**
 * Rejects ORGL data dirs that are the same as, a parent of, or a child of the ROMs root.
 * Compares normalized absolute path hints when available; otherwise compares URI strings.
 */
object OnboardingDirConflict {
    fun conflicts(
        romsPath: String?,
        orglPath: String?,
        romsUri: String?,
        orglUri: String?,
    ): Boolean {
        val romsP = normalizePath(romsPath)
        val orglP = normalizePath(orglPath)
        if (romsP != null && orglP != null) {
            return romsP == orglP ||
                orglP.startsWith("$romsP/") ||
                romsP.startsWith("$orglP/")
        }
        val romsU = romsUri?.trim()?.takeIf { it.isNotEmpty() }
        val orglU = orglUri?.trim()?.takeIf { it.isNotEmpty() }
        return romsU != null && orglU != null && romsU == orglU
    }

    private fun normalizePath(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw.trim().trimEnd('/').lowercase()
    }
}
