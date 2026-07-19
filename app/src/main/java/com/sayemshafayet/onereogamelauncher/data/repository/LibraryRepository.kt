package com.sayemshafayet.onereogamelauncher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.sayemshafayet.onereogamelauncher.data.db.dao.CommitmentDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.EmulatorProfileDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.GameConfigDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.GameDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.JournalDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.MediaDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.SystemDao
import com.sayemshafayet.onereogamelauncher.data.db.entity.EmulatorProfileEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameCompletedStatus
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameConfigEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.library.RomScanResult
import com.sayemshafayet.onereogamelauncher.library.RomScanner
import com.sayemshafayet.onereogamelauncher.systems.EmulatorPackageActivity
import com.sayemshafayet.onereogamelauncher.systems.EsSystemsParser
import com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class LibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val systemDao: SystemDao,
    private val gameDao: GameDao,
    private val gameConfigDao: GameConfigDao,
    private val emulatorProfileDao: EmulatorProfileDao,
    private val commitmentDao: CommitmentDao,
    private val mediaDao: MediaDao,
    private val journalDao: JournalDao,
    private val settings: SettingsRepository,
    private val esSystemsParser: EsSystemsParser,
    private val romScanner: RomScanner,
) {
    companion object {
        private const val TAG = "LibraryRepository"
        const val SHELF_MAX = 5
    }

    val systems = systemDao.observeAll()
    val journal = journalDao.observeJournal()
    val activeCommitment = commitmentDao.observeActive()
    val scanProgress = romScanner.scanProgress

    fun observeGamesBySystem(systemId: Long) = gameDao.observeBySystem(systemId)
    fun observeSearch(systemId: Long?, query: String) = gameDao.observeSearch(systemId, query)
    fun observeFavorites() = gameDao.observeFavorites()
    fun observeShelf() = gameDao.observeShelf()
    fun observeGame(gameId: Long) = gameDao.observeById(gameId)
    fun observeMedia(gameId: Long) = mediaDao.observeForGame(gameId)
    fun observeGameConfig(gameId: Long) = gameConfigDao.observe(gameId)
    fun observeEmulators() = emulatorProfileDao.observeAll()

    suspend fun ensureCatalogLoaded() {
        if (systemDao.count() == 0) {
            loadSystemsFromAssets()
        } else {
            // Refresh extensions from assets if any row is missing them (broken earlier installs)
            val sample = systemDao.getAll().take(5)
            if (sample.any { it.extensionsCsv.isBlank() }) {
                Log.w(TAG, "System extensions missing — reloading catalog from assets")
                loadSystemsFromAssets()
            }
        }
        if (emulatorProfileDao.getAll().isEmpty()) {
            loadEmulatorProfilesFromAssets()
        }
    }

    suspend fun loadSystemsFromAssets() {
        val defs = context.assets.open("systems/es_systems.xml").use { esSystemsParser.parseSystems(it) }
        val existing = systemDao.getAll().associateBy { it.name }
        defs.forEach { def ->
            val prev = existing[def.name]
            systemDao.upsert(
                SystemEntity(
                    id = prev?.id ?: 0L,
                    name = def.name,
                    folderName = def.folder,
                    displayName = def.fullName,
                    platform = def.platform,
                    extensionsCsv = def.extensions.joinToString(","),
                    defaultEmulatorKey = esSystemsParser.defaultEmulatorKeyFromCommands(def.commands),
                    defaultCore = esSystemsParser.defaultCoreFromCommands(def.commands),
                ),
            )
        }
        Log.i(TAG, "Loaded ${defs.size} systems from es_systems.xml")
    }

    suspend fun loadEmulatorProfilesFromAssets() {
        val rules = context.assets.open("systems/es_find_rules.xml").use { esSystemsParser.parseFindRules(it) }
        val pm = context.packageManager
        val profiles = rules.flatMap { (key, entries) ->
            entries.map { entry -> entry.toProfile(key, pm) }
        }
        emulatorProfileDao.upsertAll(profiles)
    }

    suspend fun scanLibrary(): RomScanResult {
        ensureCatalogLoaded()
        val current = settings.settings.first()
        val uriString = current.romsDirUri
        val uri = uriString?.let { runCatching { Uri.parse(it) }.getOrNull() }

        // Keep a filesystem path hint for RetroArch launches (never use File walks for discovery
        // on secondary storage — scoped storage lists dirs but returns 0 files).
        if (uri != null) {
            val pathHint = SafPathResolver.resolvePath(context, uri)
                ?: current.romsDirPath?.let { SafPathResolver.normalize(it) }
            if (pathHint != null && pathHint != current.romsDirPath) {
                settings.setRomsDir(uriString!!, pathHint)
            }

            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }.onFailure {
                Log.w(TAG, "takePersistableUriPermission: ${it.message}")
            }

            val orglPath = current.orglDataDirPath?.let { SafPathResolver.normalize(it) }
            val esdePath = current.esdeDataDirPath?.let { SafPathResolver.normalize(it) }
            // Ensure we can read ES-DE / ORGL data trees for media indexing
            listOf(current.orglDataDirUri, current.esdeDataDirUri).forEach { dataUri ->
                if (dataUri.isNullOrBlank()) return@forEach
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        Uri.parse(dataUri),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            Log.i(
                TAG,
                "Scanning via SAF: $uri pathHint=$pathHint " +
                    "orglUri=${current.orglDataDirUri} esdeUri=${current.esdeDataDirUri}",
            )
            return romScanner.scanSaf(
                uri,
                orglDataDirUri = current.orglDataDirUri,
                orglDataDirPath = orglPath,
                esdeDataDirUri = current.esdeDataDirUri,
                esdeDataDirPath = esdePath,
            )
        }

        // No SAF URI — last-resort File scan (primary storage / desktop testing only)
        val path = current.romsDirPath?.let { SafPathResolver.normalize(it) }
        if (!path.isNullOrBlank() && File(path).isDirectory) {
            val orglPath = current.orglDataDirPath?.let { SafPathResolver.normalize(it) }
            val esdePath = current.esdeDataDirPath?.let { SafPathResolver.normalize(it) }
            Log.w(TAG, "No SAF URI; falling back to filesystem scan at $path")
            return romScanner.scan(
                File(path),
                orglDataDirUri = current.orglDataDirUri,
                orglDataDirPath = orglPath,
                esdeDataDirUri = current.esdeDataDirUri,
                esdeDataDirPath = esdePath,
            )
        }

        error(
            "ROMs folder not configured. Open Settings → ES-DE / Library and browse to your ROMs folder.",
        )
    }

    suspend fun getGame(id: Long): GameEntity? = gameDao.getById(id)
    suspend fun getSystem(id: Long): SystemEntity? = systemDao.getById(id)
    suspend fun getSystemByName(name: String): SystemEntity? = systemDao.findByName(name)
    suspend fun getGameConfig(gameId: Long): GameConfigEntity? = gameConfigDao.get(gameId)

    suspend fun setFavorite(gameId: Long, favorite: Boolean) {
        val game = gameDao.getById(gameId) ?: return
        gameDao.update(game.copy(favorite = favorite))
    }

    suspend fun toggleFavorite(gameId: Long) {
        val game = gameDao.getById(gameId) ?: return
        gameDao.update(game.copy(favorite = !game.favorite))
    }

    suspend fun setOnShelf(gameId: Long, onShelf: Boolean) {
        if (onShelf) {
            val count = gameDao.countOnShelf()
            if (count >= SHELF_MAX && gameDao.getById(gameId)?.onShelf != true) {
                error("Shelf is full (max $SHELF_MAX games)")
            }
        }
        val game = gameDao.getById(gameId) ?: return
        gameDao.update(game.copy(onShelf = onShelf))
    }

    suspend fun updateGameMetadata(
        gameId: Long,
        title: String? = null,
        description: String? = null,
        favorite: Boolean? = null,
        completedStatus: GameCompletedStatus? = null,
        clearCompleted: Boolean = false,
    ) {
        val game = gameDao.getById(gameId) ?: return
        gameDao.update(
            game.copy(
                title = title ?: game.title,
                description = description ?: game.description,
                favorite = favorite ?: game.favorite,
                completedStatus = when {
                    clearCompleted -> null
                    completedStatus != null -> completedStatus
                    else -> game.completedStatus
                },
            ),
        )
    }

    suspend fun saveGameConfig(
        gameId: Long,
        emulatorKey: String?,
        coreOverride: String?,
        customConfigPath: String?,
    ) {
        gameConfigDao.upsert(
            GameConfigEntity(
                gameId = gameId,
                emulatorKey = emulatorKey?.ifBlank { null },
                coreOverride = coreOverride?.ifBlank { null },
                customConfigPath = customConfigPath?.ifBlank { null },
            ),
        )
    }

    suspend fun updateSystemDefaults(systemId: Long, emulatorKey: String?, defaultCore: String?) {
        val system = systemDao.getById(systemId) ?: return
        systemDao.update(
            system.copy(
                defaultEmulatorKey = emulatorKey,
                defaultCore = defaultCore,
            ),
        )
    }

    suspend fun refreshEmulatorInstallState() {
        val pm = context.packageManager
        emulatorProfileDao.getAll().forEach { profile ->
            val installed = isPackageInstalled(pm, profile.packageName)
            if (profile.installed != installed) {
                emulatorProfileDao.update(profile.copy(installed = installed))
            }
        }
    }

    suspend fun countGames(systemId: Long? = null): Int =
        if (systemId == null) {
            systemDao.getAll().sumOf { gameDao.countForSystem(it.id) }
        } else {
            gameDao.countForSystem(systemId)
        }

    private fun EmulatorPackageActivity.toProfile(
        emulatorKey: String,
        pm: PackageManager,
    ): EmulatorProfileEntity {
        return EmulatorProfileEntity(
            key = "$emulatorKey:$packageName",
            displayName = emulatorKey.replace('_', ' '),
            packageName = packageName,
            activity = activity,
            installed = isPackageInstalled(pm, packageName),
        )
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean =
        runCatching {
            pm.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)
}
