package com.sayemshafayet.onereogamelauncher.launch

import com.sayemshafayet.onereogamelauncher.data.db.dao.GameConfigDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.GameDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.SystemDao
import com.sayemshafayet.onereogamelauncher.data.prefs.OrglSettings
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.play.PlayStatsTracker
import com.sayemshafayet.onereogamelauncher.systems.SystemConfigLoader
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LaunchResolver @Inject constructor(
    private val gameDao: GameDao,
    private val systemDao: SystemDao,
    private val gameConfigDao: GameConfigDao,
    private val settingsRepository: SettingsRepository,
    private val emulatorLauncher: EmulatorLauncher,
    private val retroArchLauncher: RetroArchLauncher,
    private val systemConfigLoader: SystemConfigLoader,
    private val playStatsTracker: PlayStatsTracker,
) {

    suspend fun resolve(gameId: Long, settings: OrglSettings? = null): LaunchPlan? {
        val game = gameDao.getById(gameId) ?: return null
        val system = systemDao.getById(game.systemId)
        val gameConfig = gameConfigDao.get(gameId)
        val orgSettings = settings ?: settingsRepository.current()
        val systemDef = system?.folderName?.let { systemConfigLoader.systemByFolder(it) }

        val resolved = emulatorLauncher.resolveLaunch(
            game = game,
            system = system,
            systemDef = systemDef,
            gameConfig = gameConfig,
            settings = orgSettings,
        ) ?: return null

        val isRa = resolved.key.equals("RETROARCH", ignoreCase = true)
        val romResolved = if (isRa) {
            RetroArchRomPaths.resolve(
                romPath = game.romPath,
                romPathsJson = game.romPathsJson,
                systemFolder = system?.folderName.orEmpty(),
                romsTreeUri = orgSettings.romsDirUri,
                romsDirPath = orgSettings.romsDirPath,
            )
        } else {
            RetroArchRomPaths.ResolvedRom(romExtra = resolveRomPath(game.romPath))
        }
        val core = resolveCore(
            resolvedKey = resolved.key,
            gameConfig = gameConfig,
            system = system,
            systemDef = systemDef,
            settings = orgSettings,
        )
        val customConfig = gameConfig?.customConfigPath?.takeIf { it.isNotBlank() }
            ?.takeIf { gameConfig.useOverride }

        return LaunchPlan(
            emulatorKey = resolved.key,
            packageName = resolved.packageName,
            activityClass = resolved.activityClass,
            core = core,
            romPath = romResolved.romExtra,
            customConfig = customConfig,
            isRetroArch = isRa,
            grantTreeUri = romResolved.grantTreeUri,
            grantDocumentUri = romResolved.grantDocumentUri,
        )
    }

    suspend fun launch(gameId: Long): String? {
        val settings = settingsRepository.current()
        val plan = resolve(gameId, settings) ?: return "No emulator configured for this game"
        val err = execute(plan, settings.preferredRetroArchPackage)
        if (err == null) playStatsTracker.onLaunchSuccess(gameId)
        return err
    }

    fun execute(plan: LaunchPlan, preferredRetroArchPackage: String = ""): String? {
        if (plan.isRetroArch) {
            val core = plan.core ?: return "No libretro core configured"
            return retroArchLauncher.launch(
                romPath = plan.romPath,
                coreFileName = core,
                preferredPackage = preferredRetroArchPackage.ifBlank { plan.packageName },
                customConfigPath = plan.customConfig,
                grantTreeUri = plan.grantTreeUri,
                grantDocumentUri = plan.grantDocumentUri,
            )
        }
        val resolved = emulatorLauncher.installedForKey(plan.emulatorKey)
            ?: return "${plan.emulatorKey} is not installed"
        return emulatorLauncher.launch(resolved, plan.romPath)
    }

    private fun resolveRomPath(romPath: String): String {
        if (RetroArchLauncher.isLikelyFilesystemPath(romPath)) {
            val file = File(romPath)
            if (file.isFile) return file.absolutePath
        }
        return romPath
    }

    private fun resolveCore(
        resolvedKey: String,
        gameConfig: com.sayemshafayet.onereogamelauncher.data.db.entity.GameConfigEntity?,
        system: com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity?,
        systemDef: com.sayemshafayet.onereogamelauncher.domain.SystemDef?,
        settings: OrglSettings,
    ): String? {
        if (!resolvedKey.equals("RETROARCH", ignoreCase = true)) return null
        if (gameConfig?.useOverride == true) {
            gameConfig.coreOverride?.takeIf { it.isNotBlank() }?.let { return it }
        }
        system?.defaultCore?.takeIf { it.isNotBlank() }?.let { return it }
        return systemDef?.let { systemConfigLoader.firstRetroArchCore(it) }
    }
}
