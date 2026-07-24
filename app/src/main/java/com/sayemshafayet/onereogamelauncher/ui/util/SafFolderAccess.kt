package com.sayemshafayet.onereogamelauncher.ui.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.sayemshafayet.onereogamelauncher.data.prefs.AppSettings

/**
 * Validates that persisted Storage Access Framework tree URIs are still reachable.
 * Used to send users back through onboarding when folder permissions were revoked
 * (e.g. after reinstall or manual permission removal in system settings).
 */
object SafFolderAccess {

    enum class Action {
        /** Folders are reachable, or onboarding has not finished yet. */
        None,

        /** ROMs and/or ORGL data folder access was lost — clear device folders and re-onboard. */
        ResetForReOnboarding,

        /** Optional ES-DE folder access was lost — clear ES-DE only. */
        ClearEsdeOnly,
    }

    fun validate(context: Context, settings: AppSettings): Action {
        if (!settings.onboardingDone) return Action.None

        val romsConfigured = !settings.romsDirUri.isNullOrBlank()
        val orglConfigured = !settings.orglDataDirUri.isNullOrBlank()
        val esdeConfigured = !settings.esdeDataDirUri.isNullOrBlank()

        val romsOk = romsConfigured && isTreeAccessible(context, settings.romsDirUri, requireWrite = false)
        val orglOk = orglConfigured && isTreeAccessible(context, settings.orglDataDirUri, requireWrite = true)
        val esdeOk = !esdeConfigured ||
            isTreeAccessible(context, settings.esdeDataDirUri, requireWrite = false)

        return decideAction(
            onboardingDone = true,
            esdeConfigured = esdeConfigured,
            romsOk = romsOk,
            orglOk = orglOk,
            esdeOk = esdeOk,
        )
    }

    /** Pure decision logic — unit-tested without Android stubs. */
    fun decideAction(
        onboardingDone: Boolean,
        esdeConfigured: Boolean,
        romsOk: Boolean,
        orglOk: Boolean,
        esdeOk: Boolean,
    ): Action {
        if (!onboardingDone) return Action.None
        if (!romsOk || !orglOk) return Action.ResetForReOnboarding
        if (esdeConfigured && !esdeOk) return Action.ClearEsdeOnly
        return Action.None
    }

    fun isTreeAccessible(context: Context, uriString: String?, requireWrite: Boolean): Boolean {
        if (uriString.isNullOrBlank()) return false
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return false
        if (!hasPersistedTreePermission(context, uri, requireWrite)) return false
        return probeTreeAccess(context, uri, requireWrite)
    }

    private fun hasPersistedTreePermission(
        context: Context,
        treeUri: Uri,
        requireWrite: Boolean,
    ): Boolean {
        val treeId = treeDocumentId(treeUri)
        return context.contentResolver.persistedUriPermissions.any { perm ->
            if (!perm.isReadPermission) return@any false
            if (requireWrite && !perm.isWritePermission) return@any false
            if (perm.uri == treeUri) return@any true
            if (treeId != null) {
                treeDocumentId(perm.uri) == treeId
            } else {
                false
            }
        }
    }

    private fun probeTreeAccess(context: Context, treeUri: Uri, requireWrite: Boolean): Boolean {
        return runCatching {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
            if (!SafIo.isDirectory(root)) return false
            if (requireWrite && !SafIo.canWrite(root)) return false
            SafIo.listChildren(root)
            true
        }.getOrDefault(false)
    }

    private fun treeDocumentId(uri: Uri): String? = runCatching {
        if (DocumentsContract.isTreeUri(uri)) {
            DocumentsContract.getTreeDocumentId(uri)
        } else {
            null
        }
    }.getOrNull()
}
