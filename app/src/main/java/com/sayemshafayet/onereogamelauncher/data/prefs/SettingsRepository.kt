package com.sayemshafayet.onereogamelauncher.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sayemshafayet.onereogamelauncher.domain.AppMode
import com.sayemshafayet.onereogamelauncher.domain.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("orgl_settings")

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
    val retroAchievementsPassword: String = "",
    /** Cached Connect API token from login2; not shown in UI. */
    val retroAchievementsToken: String = "",
    val preferredRetroArchPackage: String = "",
    val hltbEnabled: Boolean = true,
    val onboardingDone: Boolean = false,
    val appMode: AppMode = AppMode.SETUP,
)

val AppSettings.romsRootPath: String? get() = romsDirPath
val AppSettings.romsRootUri: String? get() = romsDirUri
val AppSettings.screenScraperPassword: String get() = screenScraperPass
val AppSettings.screenScraperDevId: String get() = screenScraperDevid
val AppSettings.screenScraperDevPassword: String get() = screenScraperDevpassword

/** @deprecated Use [esdeDataDirUri]. */
val AppSettings.appDataDirUri: String? get() = esdeDataDirUri

/** @deprecated Use [retroAchievementsPassword] / token flow. */
val AppSettings.retroAchievementsApiKey: String
    get() = retroAchievementsToken.ifBlank { retroAchievementsPassword }

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val romsDirUri = stringPreferencesKey("roms_dir_uri")
        val romsDirPath = stringPreferencesKey("roms_dir_path")
        val orglDataDirUri = stringPreferencesKey("orgl_data_dir_uri")
        val orglDataDirPath = stringPreferencesKey("orgl_data_dir_path")
        val esdeDataDirUri = stringPreferencesKey("esde_data_dir_uri")
        val esdeDataDirPath = stringPreferencesKey("esde_data_dir_path")
        // Legacy keys
        val appDataDirUri = stringPreferencesKey("app_data_dir_uri")
        val theme = stringPreferencesKey("theme_mode")
        val ssUser = stringPreferencesKey("ss_user")
        val ssPass = stringPreferencesKey("ss_pass")
        val ssDevid = stringPreferencesKey("ss_devid")
        val ssDevPass = stringPreferencesKey("ss_devpass")
        val raUser = stringPreferencesKey("ra_user")
        val raPassword = stringPreferencesKey("ra_password")
        val raToken = stringPreferencesKey("ra_token")
        val raApiKey = stringPreferencesKey("ra_api_key")
        val raPackage = stringPreferencesKey("ra_package")
        val hltbEnabled = booleanPreferencesKey("hltb_enabled")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val appMode = stringPreferencesKey("app_mode")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val password = p[Keys.raPassword].orEmpty().ifBlank { p[Keys.raApiKey].orEmpty() }
        AppSettings(
            romsDirUri = p[Keys.romsDirUri],
            romsDirPath = p[Keys.romsDirPath],
            orglDataDirUri = p[Keys.orglDataDirUri],
            orglDataDirPath = p[Keys.orglDataDirPath],
            esdeDataDirUri = p[Keys.esdeDataDirUri] ?: p[Keys.appDataDirUri],
            esdeDataDirPath = p[Keys.esdeDataDirPath],
            themeMode = runCatching {
                ThemeMode.valueOf(p[Keys.theme] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            screenScraperUser = p[Keys.ssUser].orEmpty(),
            screenScraperPass = p[Keys.ssPass].orEmpty(),
            screenScraperDevid = p[Keys.ssDevid].orEmpty(),
            screenScraperDevpassword = p[Keys.ssDevPass].orEmpty(),
            retroAchievementsUser = p[Keys.raUser].orEmpty(),
            retroAchievementsPassword = password,
            retroAchievementsToken = p[Keys.raToken].orEmpty(),
            preferredRetroArchPackage = p[Keys.raPackage].orEmpty(),
            hltbEnabled = p[Keys.hltbEnabled] ?: true,
            onboardingDone = p[Keys.onboardingDone] ?: false,
            appMode = runCatching {
                AppMode.valueOf(p[Keys.appMode] ?: AppMode.SETUP.name)
            }.getOrDefault(AppMode.SETUP),
        )
    }

    suspend fun setRomsDir(uri: String, pathHint: String?) {
        context.dataStore.edit {
            it[Keys.romsDirUri] = uri
            if (pathHint != null) it[Keys.romsDirPath] = pathHint else it.remove(Keys.romsDirPath)
        }
    }

    suspend fun setOrglDataDir(uri: String, pathHint: String?) {
        context.dataStore.edit {
            it[Keys.orglDataDirUri] = uri
            if (pathHint != null) it[Keys.orglDataDirPath] = pathHint else it.remove(Keys.orglDataDirPath)
        }
    }

    suspend fun setEsdeDataDir(uri: String, pathHint: String?) {
        context.dataStore.edit {
            it[Keys.esdeDataDirUri] = uri
            if (pathHint != null) it[Keys.esdeDataDirPath] = pathHint else it.remove(Keys.esdeDataDirPath)
        }
    }

    /** @deprecated Use [setEsdeDataDir]. */
    suspend fun setAppDataDirUri(uri: String) {
        setEsdeDataDir(uri, null)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setScreenScraper(user: String, pass: String, devid: String, devpassword: String) {
        context.dataStore.edit {
            it[Keys.ssUser] = user
            it[Keys.ssPass] = pass
            it[Keys.ssDevid] = devid
            it[Keys.ssDevPass] = devpassword
        }
    }

    suspend fun setRetroAchievements(user: String, password: String) {
        context.dataStore.edit {
            it[Keys.raUser] = user
            it[Keys.raPassword] = password
            it.remove(Keys.raToken)
            it.remove(Keys.raApiKey)
        }
    }

    suspend fun setRetroAchievementsToken(token: String) {
        context.dataStore.edit { it[Keys.raToken] = token }
    }

    suspend fun setPreferredRetroArchPackage(pkg: String) {
        context.dataStore.edit { it[Keys.raPackage] = pkg }
    }

    suspend fun setHltbEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.hltbEnabled] = enabled }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.onboardingDone] = done }
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setAppMode(mode: AppMode) {
        context.dataStore.edit { it[Keys.appMode] = mode.name }
    }
}

typealias OrglSettings = AppSettings
