package com.sayemshafayet.onereogamelauncher.launch

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameConfigEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.OrglSettings
import com.sayemshafayet.onereogamelauncher.domain.SystemDef
import com.sayemshafayet.onereogamelauncher.systems.EmulatorFindRule
import com.sayemshafayet.onereogamelauncher.systems.EmulatorPackageEntry
import com.sayemshafayet.onereogamelauncher.systems.SystemConfigLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class StandaloneEmulatorProfile(
    val key: String,
    val displayName: String,
    val findRuleKey: String,
    /** Intent action; defaults depend on [launchMode]. */
    val intentAction: String? = null,
    val categories: List<String> = emptyList(),
    /** String extras that receive the resolved ROM path / content URI. */
    val pathExtraKeys: List<String> = emptyList(),
    val booleanExtras: Map<String, Boolean> = emptyMap(),
    /** When true, also set Intent data to the ROM URI (ES-DE `%DATA%=%ROMSAF%`). */
    val putPathAsData: Boolean = false,
    val clearTask: Boolean = true,
    val launchMode: LaunchMode = if (pathExtraKeys.isNotEmpty() && !putPathAsData) {
        LaunchMode.MAIN_WITH_EXTRAS
    } else {
        LaunchMode.VIEW_URI
    },
    val mimeType: String = "*/*",
) {
    enum class LaunchMode {
        /** ACTION_VIEW (or custom) with Intent data set to the ROM URI. */
        VIEW_URI,
        /** ACTION_MAIN (or custom) with path extras; data usually unset. */
        MAIN_WITH_EXTRAS,
    }
}

data class ResolvedEmulator(
    val key: String,
    val packageName: String,
    val activityClass: String,
    val profile: StandaloneEmulatorProfile,
)

@Singleton
class EmulatorLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val retroArchLauncher: RetroArchLauncher,
    private val systemConfigLoader: SystemConfigLoader,
) {
    companion object {
        private const val TAG = "EmulatorLauncher"

        val SUPPORTED_PROFILES = listOf(
            StandaloneEmulatorProfile(
                key = "DUCKSTATION",
                displayName = "DuckStation",
                findRuleKey = "DUCKSTATION",
                pathExtraKeys = listOf("bootPath"),
                booleanExtras = mapOf("resumeState" to false),
            ),
            StandaloneEmulatorProfile("AETHERSX2", "AetherSX2", "AETHERSX2", pathExtraKeys = listOf("bootPath")),
            StandaloneEmulatorProfile(
                key = "NETHERSX2",
                displayName = "NetherSX2",
                findRuleKey = "AETHERSX2",
                pathExtraKeys = listOf("bootPath"),
            ),
            // ES-DE: MAIN + LEANBACK_LAUNCHER + EXTRA_AutoStartFile=%ROMSAF%
            StandaloneEmulatorProfile(
                key = "DOLPHIN",
                displayName = "Dolphin",
                findRuleKey = "DOLPHIN",
                categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER),
                pathExtraKeys = listOf("AutoStartFile"),
            ),
            // ES-DE: ACTION=me.magnum.melonds.LAUNCH_ROM + EXTRA_uri=%ROMSAF%
            StandaloneEmulatorProfile(
                key = "MELONDS",
                displayName = "melonDS",
                findRuleKey = "MELONDS",
                intentAction = "me.magnum.melonds.LAUNCH_ROM",
                pathExtraKeys = listOf("uri"),
                clearTask = true,
                launchMode = StandaloneEmulatorProfile.LaunchMode.MAIN_WITH_EXTRAS,
            ),
            StandaloneEmulatorProfile("LEMURROID", "Lemuroid", "LEMURROID", launchMode = StandaloneEmulatorProfile.LaunchMode.VIEW_URI),
            // ES-DE: ACTION_VIEW + CATEGORY_DEFAULT + DATA=%ROMSAF%
            StandaloneEmulatorProfile(
                key = "PPSSPP",
                displayName = "PPSSPP",
                findRuleKey = "PPSSPP",
                intentAction = Intent.ACTION_VIEW,
                categories = listOf(Intent.CATEGORY_DEFAULT),
                putPathAsData = true,
                clearTask = false,
                launchMode = StandaloneEmulatorProfile.LaunchMode.VIEW_URI,
            ),
            // ES-DE: ACTION_VIEW + DATA=%ROMSAF%
            StandaloneEmulatorProfile(
                key = "M64PLUS-FZ",
                displayName = "Mupen64Plus FZ",
                findRuleKey = "M64PLUS-FZ",
                intentAction = Intent.ACTION_VIEW,
                putPathAsData = true,
                clearTask = false,
                launchMode = StandaloneEmulatorProfile.LaunchMode.VIEW_URI,
            ),
            // ES-DE: ACTION_VIEW + EXTRA_org.uoyabause.android.FileNameUri=%ROMSAF%
            StandaloneEmulatorProfile(
                key = "YABASANSHIRO-2",
                displayName = "Yaba Sanshiro 2",
                findRuleKey = "YABASANSHIRO-2",
                intentAction = Intent.ACTION_VIEW,
                pathExtraKeys = listOf("org.uoyabause.android.FileNameUri"),
                clearTask = true,
                launchMode = StandaloneEmulatorProfile.LaunchMode.MAIN_WITH_EXTRAS,
            ),
            // ES-DE: ACTION_VIEW + DATA=%ROMSAF%
            StandaloneEmulatorProfile(
                key = "FLYCAST",
                displayName = "Flycast",
                findRuleKey = "FLYCAST",
                intentAction = Intent.ACTION_VIEW,
                putPathAsData = true,
                clearTask = false,
                launchMode = StandaloneEmulatorProfile.LaunchMode.VIEW_URI,
            ),
        )

        private val LEMUROID_PACKAGES = listOf("com.swordfish.lemuroid")
        private val NETHERSX2_ACTIVITIES = listOf(
            "xyz.aethersx2.android.EmulationActivity",
            "xyz.aethersx2.android.MainActivity",
        )
    }

    private val findRules: Map<String, EmulatorFindRule> by lazy {
        systemConfigLoader.loadFindRules().toMutableMap().apply {
            if (!containsKey("LEMURROID")) {
                put(
                    "LEMURROID",
                    EmulatorFindRule(
                        "LEMURROID",
                        LEMUROID_PACKAGES.map { EmulatorPackageEntry(it, "$it.app.shared.game.ExternalGameLauncherActivity") },
                    ),
                )
            }
        }
    }

    fun profileForKey(key: String): StandaloneEmulatorProfile? =
        SUPPORTED_PROFILES.firstOrNull { it.key.equals(key, ignoreCase = true) }

    fun resolveLaunch(
        game: GameEntity,
        system: SystemEntity?,
        systemDef: SystemDef?,
        gameConfig: GameConfigEntity?,
        settings: OrglSettings,
    ): ResolvedEmulator? {
        if (gameConfig?.useOverride == true) {
            gameConfig.emulatorKey?.takeIf { it.isNotBlank() }?.let { key ->
                installedForKey(key, settings.preferredRetroArchPackage)?.let { return it }
            }
        }

        system?.defaultEmulatorKey?.takeIf { it.isNotBlank() }?.let { key ->
            installedForKey(key, settings.preferredRetroArchPackage)?.let { return it }
        }

        if (systemDef != null) {
            val regex = Regex("""%EMULATOR_([A-Z0-9_-]+)%""")
            for (cmd in systemDef.commands) {
                val key = regex.find(cmd.template)?.groupValues?.getOrNull(1) ?: continue
                installedForKey(key, settings.preferredRetroArchPackage)?.let { return it }
            }
        }

        retroArchLauncher.resolvePackage(settings.preferredRetroArchPackage)?.let { pkg ->
            return ResolvedEmulator(
                key = "RETROARCH",
                packageName = pkg,
                activityClass = RetroArchLauncher.ACTIVITY,
                profile = StandaloneEmulatorProfile("RETROARCH", "RetroArch", "RETROARCH"),
            )
        }

        return null
    }

    fun installedForKey(key: String, preferredRetroArchPackage: String? = null): ResolvedEmulator? {
        if (key.equals("RETROARCH", ignoreCase = true)) {
            val pkg = retroArchLauncher.resolvePackage(preferredRetroArchPackage) ?: return null
            return ResolvedEmulator(
                key = "RETROARCH",
                packageName = pkg,
                activityClass = RetroArchLauncher.ACTIVITY,
                profile = StandaloneEmulatorProfile("RETROARCH", "RetroArch", "RETROARCH"),
            )
        }
        val profile = profileForKey(key) ?: return null
        return resolveInstalled(profile)
    }

    /**
     * @return null on success, or error message
     */
    fun launch(
        resolved: ResolvedEmulator,
        romPath: String,
        romUri: String? = null,
        grantTreeUri: String? = null,
        grantDocumentUri: String? = null,
        grantDocumentUris: List<String> = emptyList(),
    ): String? {
        if (resolved.key.equals("RETROARCH", ignoreCase = true)) {
            return "Use RetroArchLauncher for RetroArch"
        }
        val path = preferAbsolutePath(romPath, romUri) ?: return "ROM path is not reachable"
        return launchWithProfile(resolved, path, grantTreeUri, grantDocumentUri, grantDocumentUris)
    }

    private fun launchWithProfile(
        resolved: ResolvedEmulator,
        path: String,
        grantTreeUri: String?,
        grantDocumentUri: String?,
        grantDocumentUris: List<String>,
    ): String? {
        val profile = resolved.profile
        val action = profile.intentAction ?: when (profile.launchMode) {
            StandaloneEmulatorProfile.LaunchMode.VIEW_URI -> Intent.ACTION_VIEW
            StandaloneEmulatorProfile.LaunchMode.MAIN_WITH_EXTRAS -> Intent.ACTION_MAIN
        }
        val contentUri = path.takeIf { it.startsWith("content:", ignoreCase = true) }?.let(Uri::parse)
        val fileUri = when {
            contentUri != null -> contentUri
            else -> {
                val file = File(path)
                if (profile.putPathAsData || profile.launchMode == StandaloneEmulatorProfile.LaunchMode.VIEW_URI) {
                    if (!file.exists()) return "ROM file not found: $path"
                    Uri.fromFile(file)
                } else {
                    null
                }
            }
        }

        val intent = Intent(action).apply {
            setClassName(resolved.packageName, resolved.activityClass)
            var flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            if (profile.clearTask) {
                flags = flags or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            } else {
                flags = flags or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            addFlags(flags)
            profile.categories.forEach { addCategory(it) }
            profile.pathExtraKeys.forEach { putExtra(it, path) }
            profile.booleanExtras.forEach { (key, value) -> putExtra(key, value) }
            if (profile.putPathAsData || profile.launchMode == StandaloneEmulatorProfile.LaunchMode.VIEW_URI) {
                fileUri?.let { setDataAndType(it, profile.mimeType) }
            }
        }
        grantAccess(resolved.packageName, intent, path, grantTreeUri, grantDocumentUri, grantDocumentUris)
        Log.i(TAG, "Launching ${resolved.key} action=$action path=$path grants=${grantDocumentUris.size}")
        return startSafely(intent, resolved.packageName, contentUri ?: fileUri)
    }

    private fun resolveInstalled(profile: StandaloneEmulatorProfile): ResolvedEmulator? {
        if (profile.key == "NETHERSX2") {
            val pkg = "xyz.aethersx2.android"
            if (isPackageInstalled(pkg)) {
                val activity = NETHERSX2_ACTIVITIES.firstOrNull { isActivityAvailable(pkg, it) }
                    ?: NETHERSX2_ACTIVITIES.first()
                return ResolvedEmulator(profile.key, pkg, activity, profile)
            }
            return null
        }
        val rule = findRules[profile.findRuleKey] ?: return null
        for (entry in rule.entries) {
            if (isPackageInstalled(entry.packageName)) {
                return ResolvedEmulator(profile.key, entry.packageName, entry.activityClass, profile)
            }
        }
        return null
    }

    private fun grantAccess(
        pkg: String,
        intent: Intent,
        romPath: String,
        grantTreeUri: String?,
        grantDocumentUri: String?,
        grantDocumentUris: List<String>,
    ) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        val docUris = linkedSetOf<String>()
        grantDocumentUris.forEach { if (it.isNotBlank()) docUris += it }
        grantDocumentUri?.takeIf { it.isNotBlank() }?.let { docUris += it }
        if (romPath.startsWith("content:", ignoreCase = true)) docUris += romPath

        fun grant(uriString: String?) {
            if (uriString.isNullOrBlank()) return
            runCatching {
                val uri = Uri.parse(uriString)
                if (!uri.scheme.equals("content", ignoreCase = true)) return@runCatching
                context.grantUriPermission(pkg, uri, flags)
                intent.addFlags(flags)
            }.onFailure { Log.w(TAG, "grantUriPermission failed for $uriString", it) }
        }

        grant(grantTreeUri)
        docUris.forEach { grant(it) }

        // Prefer newRawUri — ClipData.newUri(null, …) NPEs when resolving content MIME types.
        val contentOnly = docUris.mapNotNull { uriString ->
            runCatching {
                Uri.parse(uriString).takeIf { it.scheme.equals("content", ignoreCase = true) }
            }.getOrNull()
        }
        if (contentOnly.isNotEmpty()) {
            runCatching {
                val clip = android.content.ClipData.newRawUri("rom", contentOnly.first())
                contentOnly.drop(1).forEach { clip.addItem(android.content.ClipData.Item(it)) }
                intent.clipData = clip
            }.onFailure { Log.w(TAG, "Failed to attach ClipData for URI grants", it) }
        }
    }

    private fun startSafely(intent: Intent, targetPackage: String, uri: Uri?): String? {
        uri?.let {
            runCatching {
                context.grantUriPermission(targetPackage, it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        return try {
            context.startActivity(intent)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Launch failed for $targetPackage", e)
            e.message ?: "Could not start ${targetPackage.substringAfterLast('.')}"
        }
    }

    private fun preferAbsolutePath(romPath: String, romUri: String?): String? {
        if (RetroArchLauncher.isLikelyFilesystemPath(romPath) && File(romPath).isFile) {
            return File(romPath).absolutePath
        }
        if (!romUri.isNullOrBlank()) return romUri
        if (romPath.isNotBlank()) return romPath
        return null
    }

    private fun isPackageInstalled(packageName: String): Boolean =
        runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)

    private fun isActivityAvailable(packageName: String, activityClass: String): Boolean =
        runCatching {
            val intent = Intent().setClassName(packageName, activityClass)
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        }.getOrDefault(false)
}
