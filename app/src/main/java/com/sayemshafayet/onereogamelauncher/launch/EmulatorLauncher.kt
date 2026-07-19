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
    val launchMode: LaunchMode = LaunchMode.VIEW_URI,
    val pathExtraKeys: List<String> = emptyList(),
    val mimeType: String = "*/*",
) {
    enum class LaunchMode {
        VIEW_URI,
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
            StandaloneEmulatorProfile("DUCKSTATION", "DuckStation", "DUCKSTATION", pathExtraKeys = listOf("bootPath", "filename")),
            StandaloneEmulatorProfile("AETHERSX2", "AetherSX2", "AETHERSX2", pathExtraKeys = listOf("bootPath")),
            StandaloneEmulatorProfile(
                key = "NETHERSX2",
                displayName = "NetherSX2",
                findRuleKey = "AETHERSX2",
                pathExtraKeys = listOf("bootPath"),
            ),
            StandaloneEmulatorProfile("DOLPHIN", "Dolphin", "DOLPHIN", pathExtraKeys = listOf("AutoStartISO", "SelectedFile")),
            StandaloneEmulatorProfile("MELONDS", "melonDS", "MELONDS", pathExtraKeys = listOf("PATH", "RomPath")),
            StandaloneEmulatorProfile("LEMURROID", "Lemuroid", "LEMURROID", launchMode = StandaloneEmulatorProfile.LaunchMode.VIEW_URI),
            StandaloneEmulatorProfile("PPSSPP", "PPSSPP", "PPSSPP", pathExtraKeys = listOf("args", "file")),
            StandaloneEmulatorProfile("M64PLUS-FZ", "Mupen64Plus FZ", "M64PLUS-FZ", pathExtraKeys = listOf("filePath")),
            StandaloneEmulatorProfile("YABASANSHIRO-2", "Yaba Sanshiro 2", "YABASANSHIRO-2", pathExtraKeys = listOf("org.uoyabause.android.FileNameUri")),
            StandaloneEmulatorProfile("FLYCAST", "Flycast", "FLYCAST", pathExtraKeys = listOf("gameUri")),
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

    fun installedForKey(key: String): ResolvedEmulator? {
        val profile = profileForKey(key) ?: return null
        if (key.equals("RETROARCH", ignoreCase = true)) {
            val pkg = retroArchLauncher.resolvePackage(null) ?: return null
            return ResolvedEmulator(
                key = "RETROARCH",
                packageName = pkg,
                activityClass = RetroArchLauncher.ACTIVITY,
                profile = StandaloneEmulatorProfile("RETROARCH", "RetroArch", "RETROARCH"),
            )
        }
        return resolveInstalled(profile)
    }

    fun installedProfiles(): List<ResolvedEmulator> =
        (listOf("RETROARCH") + SUPPORTED_PROFILES.map { it.key })
            .distinct()
            .mapNotNull { installedForKey(it) }

    fun resolveLaunch(
        game: GameEntity,
        system: SystemEntity?,
        systemDef: SystemDef?,
        gameConfig: GameConfigEntity?,
        settings: OrglSettings,
    ): ResolvedEmulator? {
        val overrideKey = gameConfig?.emulatorKey?.takeIf { it.isNotBlank() }
        if (overrideKey != null) {
            installedForKey(overrideKey)?.let { return it }
        }

        val systemDefault = system?.defaultEmulatorKey?.takeIf { it.isNotBlank() }
        if (!systemDefault.isNullOrBlank()) {
            installedForKey(systemDefault)?.let { return it }
        }

        val retroCommand = systemDef?.commands?.firstOrNull {
            it.template.contains("%EMULATOR_RETROARCH%")
        }
        if (retroCommand != null && retroArchLauncher.resolvePackage(settings.preferredRetroArchPackage) != null) {
            val pkg = retroArchLauncher.resolvePackage(settings.preferredRetroArchPackage)!!
            return ResolvedEmulator(
                key = "RETROARCH",
                packageName = pkg,
                activityClass = RetroArchLauncher.ACTIVITY,
                profile = StandaloneEmulatorProfile("RETROARCH", "RetroArch", "RETROARCH"),
            )
        }

        return null
    }

    /**
     * @return null on success, or error message
     */
    fun launch(resolved: ResolvedEmulator, romPath: String, romUri: String? = null): String? {
        if (resolved.key.equals("RETROARCH", ignoreCase = true)) {
            return "Use RetroArchLauncher for RetroArch"
        }
        val path = preferAbsolutePath(romPath, romUri) ?: return "ROM path is not reachable"
        return when (resolved.profile.launchMode) {
            StandaloneEmulatorProfile.LaunchMode.VIEW_URI -> launchView(resolved, path)
            StandaloneEmulatorProfile.LaunchMode.MAIN_WITH_EXTRAS -> launchMain(resolved, path)
        }
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

    private fun launchView(resolved: ResolvedEmulator, path: String): String? {
        val uri = when {
            path.startsWith("content:", ignoreCase = true) -> Uri.parse(path)
            else -> {
                val file = File(path)
                if (!file.exists()) return "ROM file not found: $path"
                Uri.fromFile(file)
            }
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, resolved.profile.mimeType)
            setClassName(resolved.packageName, resolved.activityClass)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return startSafely(intent, resolved.packageName, uri)
    }

    private fun launchMain(resolved: ResolvedEmulator, path: String): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(resolved.packageName, resolved.activityClass)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            resolved.profile.pathExtraKeys.forEach { putExtra(it, path) }
            if (path.startsWith("content:", ignoreCase = true)) {
                val uri = Uri.parse(path)
                setData(uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        return startSafely(intent, resolved.packageName, null)
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
