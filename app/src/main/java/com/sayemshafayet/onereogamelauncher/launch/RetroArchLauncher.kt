package com.sayemshafayet.onereogamelauncher.launch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.sayemshafayet.onereogamelauncher.domain.LaunchCheck
import com.sayemshafayet.onereogamelauncher.domain.LaunchSeverity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Launches games through RetroArch's RetroActivityFuture intent extras.
 * Mirrors ES-DE / Pegasus / DroidArcade contract.
 */
@Singleton
class RetroArchLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "RetroArchLauncher"

        val KNOWN_PACKAGES = listOf(
            "com.retroarch.aarch64",
            "com.retroarch",
            "com.retroarch.ra32",
        )
        const val ACTIVITY = "com.retroarch.browser.retroactivity.RetroActivityFuture"

        fun androidUserId(): Int = Process.myUid() / 100_000

        fun internalDataRoot(userId: Int = androidUserId()): String = "/data/user/$userId"

        fun externalDataRoot(userId: Int = androidUserId()): String =
            if (userId == 0) {
                Environment.getExternalStorageDirectory().absolutePath
            } else {
                "/storage/emulated/$userId"
            }

        fun normalizeCoreFileName(coreFileName: String): String {
            val name = coreFileName.trim()
            return when (name) {
                "mame_libretro_android.so", "mame_libretro.so" -> "mamearcade_libretro_android.so"
                else -> name
            }
        }

        fun isLikelyFilesystemPath(path: String): Boolean {
            if (path.isBlank()) return false
            if (path.startsWith("content:", ignoreCase = true)) return false
            if (path.startsWith("saf:", ignoreCase = true)) return false
            return path.startsWith('/') || path.matches(Regex("^[A-Za-z]:\\\\.*"))
        }
    }

    fun installedPackages(): List<String> {
        val pm = context.packageManager
        return KNOWN_PACKAGES.filter { pkg ->
            runCatching { pm.getPackageInfo(pkg, 0); true }.getOrDefault(false)
        }
    }

    fun resolvePackage(preferred: String?): String? {
        val installed = installedPackages()
        if (preferred != null && preferred in installed) return preferred
        return installed.firstOrNull()
    }

    fun coreLikelyPresent(pkg: String, coreFileName: String): Boolean? {
        val core = normalizeCoreFileName(coreFileName)
        val anyReadable = corePathCandidates(pkg, core).any { File(it).canRead() }
        return if (anyReadable) true else null
    }

    fun buildLaunchability(
        romPath: String,
        coreFileName: String?,
        preferredPackage: String?,
    ): List<LaunchCheck> {
        val checks = mutableListOf<LaunchCheck>()

        val pkg = resolvePackage(preferredPackage?.takeIf { it.isNotBlank() })
        if (pkg == null) {
            checks += LaunchCheck(
                id = "retroarch",
                label = "RetroArch installed",
                severity = LaunchSeverity.ERROR,
                detail = "No known RetroArch package found",
                fixGuidance = "Install RetroArch from retroarch.com or F-Droid, then install cores via Online Updater.",
            )
        } else {
            checks += LaunchCheck("retroarch", "RetroArch installed", LaunchSeverity.OK, pkg)
            val core = normalizeCoreFileName(coreFileName.orEmpty())
            if (core.isNotBlank()) {
                when (coreLikelyPresent(pkg, core)) {
                    true -> checks += LaunchCheck("core", "Core installed", LaunchSeverity.OK, core)
                    false -> checks += LaunchCheck(
                        id = "core",
                        label = "Core installed",
                        severity = LaunchSeverity.ERROR,
                        detail = core,
                        fixGuidance = "Install the core in RetroArch → Online Updater → Core Downloader.",
                    )
                    null -> checks += LaunchCheck(
                        id = "core",
                        label = "Core installed",
                        severity = LaunchSeverity.WARN,
                        detail = "Could not verify $core (normal — RetroArch private storage)",
                        fixGuidance = "Launch may still work if the core is installed in RetroArch.",
                    )
                }
            }
        }

        val reachable = when {
            romPath.isBlank() -> false
            isLikelyFilesystemPath(romPath) -> File(romPath).let { it.exists() && it.isFile }
            romPath.startsWith("content:", ignoreCase = true) -> true
            else -> false
        }
        checks += if (reachable) {
            LaunchCheck("path", "ROM path", LaunchSeverity.OK, romPath)
        } else {
            LaunchCheck(
                id = "path",
                label = "ROM path",
                severity = LaunchSeverity.ERROR,
                detail = romPath.ifBlank { "Empty path" },
                fixGuidance = "Use a real filesystem path under shared storage (/sdcard/...) when possible.",
            )
        }

        return checks
    }

    /**
     * @return null on success, or a short user-facing failure reason
     */
    fun launch(
        romPath: String,
        coreFileName: String,
        preferredPackage: String?,
        customConfigPath: String? = null,
        grantTreeUri: String? = null,
        grantDocumentUri: String? = null,
    ): String? {
        val preferred = preferredPackage?.takeIf { it.isNotBlank() }
        val installed = installedPackages()
        if (installed.isEmpty()) return "RetroArch is not installed"
        if (preferred != null && preferred !in installed) {
            return "Preferred RetroArch ($preferred) is not installed"
        }
        val pkg = resolvePackage(preferred) ?: return "RetroArch is not installed"
        if (preferred == null && installed.size > 1) {
            Log.w(TAG, "Multiple RetroArch packages $installed — using $pkg")
        }

        if (romPath.isBlank()) return "ROM path is empty"
        if (isLikelyFilesystemPath(romPath) && File(romPath).isDirectory) {
            return "ROM file path is missing (got a directory): $romPath"
        }

        val core = normalizeCoreFileName(coreFileName)
        val paths = packagePaths(pkg)
        val corePath = guessCorePath(pkg, core)
        val ime = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            .orEmpty()
            .ifBlank { "com.android.inputmethod.latin/.LatinIME" }
        val configFile = customConfigPath?.takeIf { it.isNotBlank() } ?: paths.configFile

        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(pkg, ACTIVITY)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
            )
            putExtra("ROM", romPath)
            putExtra("LIBRETRO", corePath)
            putExtra("CONFIGFILE", configFile)
            putExtra("IME", ime)
            putExtra("DATADIR", paths.dataDir)
            putExtra("APK", paths.apkPath)
            putExtra("SDCARD", paths.externalStorage)
            putExtra("EXTERNAL", paths.externalFiles)
            putExtra("QUITFOCUS", "")
        }

        grantAccess(pkg, intent, romPath, grantTreeUri, grantDocumentUri)

        return try {
            context.startActivity(intent)
            Log.i(TAG, "Launched pkg=$pkg core=$corePath rom=$romPath")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch RetroArch", e)
            e.message?.takeIf { it.isNotBlank() } ?: "Could not start RetroArch"
        }
    }

    private fun grantAccess(
        pkg: String,
        intent: Intent,
        romPath: String,
        grantTreeUri: String?,
        grantDocumentUri: String?,
    ) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        fun grant(uriString: String?) {
            if (uriString.isNullOrBlank()) return
            runCatching {
                val uri = Uri.parse(uriString)
                if (!uri.scheme.equals("content", ignoreCase = true)) return@runCatching
                context.grantUriPermission(pkg, uri, flags)
                intent.addFlags(flags)
                // ClipData is the reliable way to pass grants across apps on modern Android.
                if (intent.clipData == null) {
                    intent.clipData = android.content.ClipData.newRawUri("rom", uri)
                }
            }.onFailure { Log.w(TAG, "grantUriPermission failed for $uriString", it) }
        }

        grant(grantTreeUri)
        grant(grantDocumentUri)

        when {
            romPath.startsWith("content:", ignoreCase = true) -> grant(romPath)
            romPath.startsWith("saf://", ignoreCase = true) -> {
                // Decode tree portion back to content:// for granting
                val encoded = romPath.removePrefix("saf://").substringBefore('/')
                val tree = decodeSafTree(encoded)
                grant(tree)
            }
        }
    }

    private fun decodeSafTree(encoded: String): String? {
        if (encoded.isBlank()) return null
        return buildString(encoded.length) {
            var i = 0
            while (i < encoded.length) {
                if (encoded[i] == '%' && i + 2 < encoded.length) {
                    val hex = encoded.substring(i + 1, i + 3)
                    val value = hex.toIntOrNull(16)
                    if (value != null) {
                        append(value.toChar())
                        i += 3
                        continue
                    }
                }
                append(encoded[i])
                i++
            }
        }.takeIf { it.startsWith("content:", ignoreCase = true) }
    }

    data class PackagePaths(
        val dataDir: String,
        val configFile: String,
        val externalStorage: String,
        val externalFiles: String,
        val apkPath: String,
    )

    fun packagePaths(pkg: String): PackagePaths {
        val userId = androidUserId()
        val internalRoot = internalDataRoot(userId)
        val externalRoot = externalDataRoot(userId)
        val dataDir = runCatching {
            context.packageManager.getApplicationInfo(pkg, 0).dataDir
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: "$internalRoot/$pkg"
        val externalFiles = "$externalRoot/Android/data/$pkg/files"
        val apkPath = runCatching {
            context.packageManager.getApplicationInfo(pkg, 0).sourceDir
        }.getOrDefault("")
        return PackagePaths(
            dataDir = dataDir,
            configFile = "$externalFiles/retroarch.cfg",
            externalStorage = externalRoot,
            externalFiles = externalFiles,
            apkPath = apkPath,
        )
    }

    fun corePathCandidates(pkg: String, coreFileName: String): List<String> {
        val core = normalizeCoreFileName(coreFileName)
        val paths = packagePaths(pkg)
        return listOf(
            "${paths.dataDir}/cores/$core",
            "${internalDataRoot()}/$pkg/cores/$core",
            "/data/data/$pkg/cores/$core",
            "${paths.externalFiles}/cores/$core",
        ).distinct()
    }

    fun guessCorePath(pkg: String, coreFileName: String): String {
        val candidates = corePathCandidates(pkg, coreFileName)
        return candidates.firstOrNull { File(it).exists() } ?: candidates.first()
    }
}
