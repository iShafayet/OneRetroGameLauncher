package com.sayemshafayet.onereogamelauncher.ui.util

/**
 * Rejects ORGL data dirs that are the same as, a parent of, or a child of the ROMs root.
 *
 * Prefers normalized absolute path hints; when those are missing (common with SAF),
 * compares decoded tree document IDs (e.g. `primary:ROMs` vs `primary:ROMs/ORGL-Data`).
 * Falls back to exact URI string equality.
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
            return isSameOrNested(romsP, orglP)
        }

        val romsDoc = documentIdFromUri(romsUri)
        val orglDoc = documentIdFromUri(orglUri)
        if (romsDoc != null && orglDoc != null) {
            return isSameOrNested(romsDoc, orglDoc)
        }

        val romsU = romsUri?.trim()?.takeIf { it.isNotEmpty() }
        val orglU = orglUri?.trim()?.takeIf { it.isNotEmpty() }
        return romsU != null && orglU != null && romsU == orglU
    }

    private fun isSameOrNested(a: String, b: String): Boolean =
        a == b || b.startsWith("$a/") || a.startsWith("$b/")

    private fun normalizePath(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw.trim().trimEnd('/').lowercase()
    }

    private fun documentIdFromUri(uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        // Prefer URI-string parsing so JVM unit tests (and odd SAF hosts) don't depend on
        // DocumentsContract stubs returning incomplete tree IDs.
        val docId = SafPathResolver.resolveTreeDocumentIdString(uriString) ?: return null
        return normalizePath(docId.replace('\\', '/'))
    }
}
