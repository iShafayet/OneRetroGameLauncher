package com.sayemshafayet.onereogamelauncher.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sayemshafayet.onereogamelauncher.domain.AppMode
import com.sayemshafayet.onereogamelauncher.domain.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** Backed up to cloud / device transfer — integration prefs, theme, layout, etc. */
private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(
    SettingsRepository.BACKUP_DATASTORE_NAME,
)

/** Device-local only — onboarding, mode, SAF folder URIs (excluded from backup). */
private val Context.deviceDataStore: DataStore<Preferences> by preferencesDataStore(
    SettingsRepository.DEVICE_DATASTORE_NAME,
)

const val MIN_PLAY_SLOTS = 1
const val MAX_PLAY_SLOTS = 5

fun coercePlaySlotCount(count: Int): Int = count.coerceIn(MIN_PLAY_SLOTS, MAX_PLAY_SLOTS)

data class AppSettings(
    val romsDirUri: String? = null,
    val romsDirPath: String? = null,
    /** ORGL’s own data directory — scraped media / ORGL-owned files are written here. */
    val orglDataDirUri: String? = null,
    val orglDataDirPath: String? = null,
    /** Optional ES-DE application data directory — read-only; never written. */
    val esdeDataDirUri: String? = null,
    val esdeDataDirPath: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val screenScraperUser: String = "",
    val screenScraperPass: String = "",
    val screenScraperDevid: String = "",
    val screenScraperDevpassword: String = "",
    val retroAchievementsUser: String = "",
    /** RA account password (Connect API — same as RetroArch). */
    val retroAchievementsPassword: String = "",
    /** Cached Connect API token from login2. */
    val retroAchievementsToken: String = "",
    /** Persist RA credentials into the ORGL data directory when true. */
    val retroAchievementsStoreOnDisk: Boolean = false,
    val preferredRetroArchPackage: String = "",
    val hltbEnabled: Boolean = true,
    val onboardingDone: Boolean = false,
    val appMode: AppMode = AppMode.SETUP,
    /** Grid vs list for system game browsing. */
    val gameListLayout: GameListLayout = GameListLayout.GRID,
    /** Epoch millis when the last scrape batch finished. */
    val lastScrapeAt: Long? = null,
    /** How many independent play slots are available in Play mode (1 = classic one-game). */
    val playSlotCount: Int = MIN_PLAY_SLOTS,
)

enum class GameListLayout { GRID, LIST }

val AppSettings.romsRootPath: String? get() = romsDirPath
val AppSettings.romsRootUri: String? get() = romsDirUri
val AppSettings.screenScraperPassword: String get() = screenScraperPass
val AppSettings.screenScraperDevId: String get() = screenScraperDevid
val AppSettings.screenScraperDevPassword: String get() = screenScraperDevpassword

/** @deprecated Use [esdeDataDirUri]. */
val AppSettings.appDataDirUri: String? get() = esdeDataDirUri

/** True when username and password are configured. */
fun AppSettings.retroAchievementsConfigured(): Boolean =
    retroAchievementsUser.isNotBlank() && retroAchievementsPassword.isNotBlank()

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        /** Included in cloud backup — keep in sync with res/xml/data_extraction_rules.xml */
        const val BACKUP_DATASTORE_NAME = "orgl_settings"

        /** Excluded from cloud backup — keep in sync with res/xml/data_extraction_rules.xml */
        const val DEVICE_DATASTORE_NAME = "orgl_device"
    }

    private object BackupKeys {
        val theme = stringPreferencesKey("theme_mode")
        val ssUser = stringPreferencesKey("ss_user")
        val ssPass = stringPreferencesKey("ss_pass")
        val ssDevid = stringPreferencesKey("ss_devid")
        val ssDevPass = stringPreferencesKey("ss_devpass")
        val raUser = stringPreferencesKey("ra_user")
        val raPassword = stringPreferencesKey("ra_password")
        val raToken = stringPreferencesKey("ra_token")
        val raApiKey = stringPreferencesKey("ra_api_key")
        val raStoreOnDisk = booleanPreferencesKey("ra_store_on_disk")
        val raPackage = stringPreferencesKey("ra_package")
        val hltbEnabled = booleanPreferencesKey("hltb_enabled")
        val gameListLayout = stringPreferencesKey("game_list_layout")
        val lastScrapeAt = stringPreferencesKey("last_scrape_at")
        val playSlotCount = intPreferencesKey("play_slot_count")
    }

    private object DeviceKeys {
        val romsDirUri = stringPreferencesKey("roms_dir_uri")
        val romsDirPath = stringPreferencesKey("roms_dir_path")
        val orglDataDirUri = stringPreferencesKey("orgl_data_dir_uri")
        val orglDataDirPath = stringPreferencesKey("orgl_data_dir_path")
        val esdeDataDirUri = stringPreferencesKey("esde_data_dir_uri")
        val esdeDataDirPath = stringPreferencesKey("esde_data_dir_path")
        val appDataDirUri = stringPreferencesKey("app_data_dir_uri")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val appMode = stringPreferencesKey("app_mode")
    }

    val settings: Flow<AppSettings> = combine(
        context.backupDataStore.data,
        context.deviceDataStore.data,
    ) { backup, device -> mergeSettings(backup, device) }

    private fun mergeSettings(backup: Preferences, device: Preferences): AppSettings =
        AppSettings(
            romsDirUri = device[DeviceKeys.romsDirUri],
            romsDirPath = device[DeviceKeys.romsDirPath],
            orglDataDirUri = device[DeviceKeys.orglDataDirUri],
            orglDataDirPath = device[DeviceKeys.orglDataDirPath],
            esdeDataDirUri = device[DeviceKeys.esdeDataDirUri] ?: device[DeviceKeys.appDataDirUri],
            esdeDataDirPath = device[DeviceKeys.esdeDataDirPath],
            themeMode = runCatching {
                ThemeMode.valueOf(backup[BackupKeys.theme] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            screenScraperUser = backup[BackupKeys.ssUser].orEmpty(),
            screenScraperPass = backup[BackupKeys.ssPass].orEmpty(),
            screenScraperDevid = backup[BackupKeys.ssDevid].orEmpty(),
            screenScraperDevpassword = backup[BackupKeys.ssDevPass].orEmpty(),
            retroAchievementsUser = backup[BackupKeys.raUser].orEmpty(),
            retroAchievementsPassword = backup[BackupKeys.raPassword].orEmpty(),
            retroAchievementsToken = backup[BackupKeys.raToken].orEmpty(),
            retroAchievementsStoreOnDisk = backup[BackupKeys.raStoreOnDisk] ?: false,
            preferredRetroArchPackage = backup[BackupKeys.raPackage].orEmpty(),
            hltbEnabled = backup[BackupKeys.hltbEnabled] ?: true,
            onboardingDone = device[DeviceKeys.onboardingDone] ?: false,
            appMode = runCatching {
                AppMode.valueOf(device[DeviceKeys.appMode] ?: AppMode.SETUP.name)
            }.getOrDefault(AppMode.SETUP),
            gameListLayout = runCatching {
                GameListLayout.valueOf(backup[BackupKeys.gameListLayout] ?: GameListLayout.GRID.name)
            }.getOrDefault(GameListLayout.GRID),
            lastScrapeAt = backup[BackupKeys.lastScrapeAt]?.toLongOrNull(),
            playSlotCount = coercePlaySlotCount(backup[BackupKeys.playSlotCount] ?: MIN_PLAY_SLOTS),
        )

    suspend fun setRomsDir(uri: String, pathHint: String?) {
        context.deviceDataStore.edit {
            it[DeviceKeys.romsDirUri] = uri
            if (pathHint != null) it[DeviceKeys.romsDirPath] = pathHint else it.remove(DeviceKeys.romsDirPath)
        }
    }

    suspend fun setOrglDataDir(uri: String, pathHint: String?) {
        context.deviceDataStore.edit {
            it[DeviceKeys.orglDataDirUri] = uri
            if (pathHint != null) it[DeviceKeys.orglDataDirPath] = pathHint else it.remove(DeviceKeys.orglDataDirPath)
        }
    }

    suspend fun setEsdeDataDir(uri: String, pathHint: String?) {
        context.deviceDataStore.edit {
            it[DeviceKeys.esdeDataDirUri] = uri
            if (pathHint != null) it[DeviceKeys.esdeDataDirPath] = pathHint else it.remove(DeviceKeys.esdeDataDirPath)
        }
    }

    suspend fun clearEsdeDataDir() {
        context.deviceDataStore.edit {
            it.remove(DeviceKeys.esdeDataDirUri)
            it.remove(DeviceKeys.esdeDataDirPath)
            it.remove(DeviceKeys.appDataDirUri)
        }
    }

    /** @deprecated Use [setEsdeDataDir]. */
    suspend fun setAppDataDirUri(uri: String) {
        setEsdeDataDir(uri, null)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.backupDataStore.edit { it[BackupKeys.theme] = mode.name }
    }

    suspend fun setScreenScraper(user: String, pass: String, devid: String, devpassword: String) {
        context.backupDataStore.edit {
            it[BackupKeys.ssUser] = user
            it[BackupKeys.ssPass] = pass
            it[BackupKeys.ssDevid] = devid
            it[BackupKeys.ssDevPass] = devpassword
        }
    }

    suspend fun setRetroAchievements(user: String, password: String) {
        context.backupDataStore.edit {
            it[BackupKeys.raUser] = user
            it[BackupKeys.raPassword] = password
            it.remove(BackupKeys.raApiKey)
            it.remove(BackupKeys.raToken)
        }
    }

    suspend fun setRetroAchievementsToken(token: String) {
        context.backupDataStore.edit { it[BackupKeys.raToken] = token }
    }

    suspend fun setRetroAchievementsStoreOnDisk(enabled: Boolean) {
        context.backupDataStore.edit { it[BackupKeys.raStoreOnDisk] = enabled }
    }

    suspend fun setPreferredRetroArchPackage(pkg: String) {
        context.backupDataStore.edit { it[BackupKeys.raPackage] = pkg }
    }

    suspend fun setHltbEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[BackupKeys.hltbEnabled] = enabled }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.deviceDataStore.edit { it[DeviceKeys.onboardingDone] = done }
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setAppMode(mode: AppMode) {
        context.deviceDataStore.edit { it[DeviceKeys.appMode] = mode.name }
    }

    suspend fun setGameListLayout(layout: GameListLayout) {
        context.backupDataStore.edit { it[BackupKeys.gameListLayout] = layout.name }
    }

    suspend fun setLastScrapeAt(epochMs: Long) {
        context.backupDataStore.edit { it[BackupKeys.lastScrapeAt] = epochMs.toString() }
    }

    suspend fun setPlaySlotCount(count: Int) {
        context.backupDataStore.edit { it[BackupKeys.playSlotCount] = coercePlaySlotCount(count) }
    }
}

typealias OrglSettings = AppSettings
