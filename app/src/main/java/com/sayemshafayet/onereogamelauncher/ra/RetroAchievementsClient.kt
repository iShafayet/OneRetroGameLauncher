package com.sayemshafayet.onereogamelauncher.ra

import android.util.Log
import com.sayemshafayet.onereogamelauncher.data.prefs.OrglSettings
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.domain.RaProgress
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * RetroAchievements via username + password (Connect login2 → token),
 * then Connect endpoints for game hash lookup and unlock progress.
 */
@Singleton
class RetroAchievementsClient @Inject constructor(
    private val http: OkHttpClient,
    private val settingsRepository: SettingsRepository,
) {
    companion object {
        private const val TAG = "RetroAchievementsClient"
        private const val CONNECT = "https://retroachievements.org/dorequest.php"
        private const val WEB_API = "https://retroachievements.org/API"
        private val json = Json { ignoreUnknownKeys = true }
    }

    suspend fun fetchProgress(
        settings: OrglSettings,
        romFile: File?,
        title: String?,
        knownGameId: Int? = null,
    ): RaProgress? {
        if (settings.retroAchievementsUser.isBlank() || settings.retroAchievementsPassword.isBlank()) {
            return null
        }
        val token = ensureToken(settings) ?: return null
        val user = settings.retroAchievementsUser
        val gameId = knownGameId
            ?: resolveGameId(user, token, romFile, title)
            ?: return null
        return fetchUnlocks(user, token, gameId, title)
            ?: fetchWebProgress(token, user, gameId, title)
    }

    private suspend fun ensureToken(settings: OrglSettings): String? {
        if (settings.retroAchievementsToken.isNotBlank()) {
            return settings.retroAchievementsToken
        }
        val token = login(settings.retroAchievementsUser, settings.retroAchievementsPassword)
            ?: return null
        settingsRepository.setRetroAchievementsToken(token)
        return token
    }

    private fun login(user: String, password: String): String? {
        val body = FormBody.Builder()
            .add("u", user)
            .add("p", password)
            .add("r", "login2")
            .build()
        val request = Request.Builder()
            .url(CONNECT)
            .post(body)
            .header("User-Agent", "OneRetroGameLauncher/1.0")
            .build()
        return runCatching {
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w(TAG, "login2 HTTP ${response.code}: $text")
                    return@use null
                }
                val root = json.parseToJsonElement(text).jsonObject
                if (root["Success"]?.jsonPrimitive?.contentOrNull == "false") {
                    Log.w(TAG, "login2 failed: $text")
                    return@use null
                }
                root["Token"]?.jsonPrimitive?.contentOrNull
            }
        }.getOrElse {
            Log.e(TAG, "login2 error", it)
            null
        }
    }

    private fun resolveGameId(user: String, token: String, romFile: File?, title: String?): Int? {
        romFile?.let { file ->
            md5Hex(file)?.let { hash ->
                gameIdByHash(user, token, hash)?.let { return it }
            }
        }
        // Fallback: Web API game list by title using token as y (works for some accounts)
        title?.takeIf { it.isNotBlank() }?.let { t ->
            val url = "$WEB_API/API_GetGameList.php".toHttpUrlOrNull()!!.newBuilder()
                .addQueryParameter("y", token)
                .addQueryParameter("u", user)
                .addQueryParameter("f", "5")
                .addQueryParameter("i", t)
                .build()
            parseGameListId(httpGet(url.toString()))?.let { return it }
        }
        return null
    }

    private fun gameIdByHash(user: String, token: String, md5: String): Int? {
        val url = CONNECT.toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("r", "gameid")
            .addQueryParameter("u", user)
            .addQueryParameter("t", token)
            .addQueryParameter("m", md5)
            .build()
        val body = httpGet(url.toString()) ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        return root["GameID"]?.jsonPrimitive?.intOrNull
            ?: root["gameID"]?.jsonPrimitive?.intOrNull
    }

    private fun fetchUnlocks(user: String, token: String, gameId: Int, title: String?): RaProgress? {
        val unlocksUrl = CONNECT.toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("r", "unlocks")
            .addQueryParameter("u", user)
            .addQueryParameter("t", token)
            .addQueryParameter("g", gameId.toString())
            .addQueryParameter("h", "0")
            .build()
        val patchUrl = CONNECT.toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("r", "patch")
            .addQueryParameter("u", user)
            .addQueryParameter("t", token)
            .addQueryParameter("g", gameId.toString())
            .build()

        val unlocksBody = httpGet(unlocksUrl.toString()) ?: return null
        val patchBody = httpGet(patchUrl.toString())

        val unlockRoot = runCatching { json.parseToJsonElement(unlocksBody).jsonObject }.getOrNull()
            ?: return null
        val earnedIds = unlockRoot["UserUnlocks"]?.jsonArray
            ?: unlockRoot["Unlocks"]?.jsonArray
        val earned = earnedIds?.size ?: 0

        var total = 0
        var gameTitle = title
        if (patchBody != null) {
            val patch = runCatching { json.parseToJsonElement(patchBody).jsonObject }.getOrNull()
            val patchData = patch?.get("PatchData")?.jsonObject
            total = patchData?.get("Achievements")?.jsonArray?.size
                ?: patch?.get("Achievements")?.jsonArray?.size
                ?: 0
            gameTitle = patchData?.get("Title")?.jsonPrimitive?.contentOrNull ?: gameTitle
        }

        return RaProgress(
            gameId = gameId,
            title = gameTitle,
            earned = earned,
            total = total,
            softcoreEarned = earned,
            recentUnlocks = emptyList(),
        )
    }

    private fun fetchWebProgress(token: String, user: String, gameId: Int, title: String?): RaProgress? {
        val url = "$WEB_API/API_GetGameInfoAndUserProgress.php".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("y", token)
            .addQueryParameter("u", user)
            .addQueryParameter("g", gameId.toString())
            .build()
        val body = httpGet(url.toString()) ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val earned = root["NumAwarded"]?.jsonPrimitive?.intOrNull ?: 0
        val total = root["NumAchievements"]?.jsonPrimitive?.intOrNull ?: 0
        if (total == 0 && earned == 0) return null
        return RaProgress(
            gameId = gameId,
            title = root["Title"]?.jsonPrimitive?.contentOrNull ?: title,
            earned = earned,
            total = total,
            softcoreEarned = root["NumAwardedHardcore"]?.jsonPrimitive?.intOrNull ?: 0,
            recentUnlocks = root["RecentUnlocks"]?.jsonArray?.mapNotNull {
                it.jsonObject["Title"]?.jsonPrimitive?.contentOrNull
            }.orEmpty(),
        )
    }

    private fun parseGameListId(body: String?): Int? {
        if (body.isNullOrBlank()) return null
        val element = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return null
        val array = runCatching { element.jsonArray }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?: element.jsonObject["Results"]?.jsonArray
            ?: return null
        return array.firstOrNull()?.jsonObject?.get("ID")?.jsonPrimitive?.intOrNull
    }

    private fun httpGet(url: String): String? =
        runCatching {
            http.newCall(
                Request.Builder()
                    .url(url)
                    .get()
                    .header("User-Agent", "OneRetroGameLauncher/1.0")
                    .build(),
            ).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} for $url")
                    return@use null
                }
                response.body?.string()
            }
        }.getOrElse {
            Log.e(TAG, "request failed", it)
            null
        }

    private fun md5Hex(file: File): String? {
        if (!file.isFile) return null
        return runCatching {
            val digest = MessageDigest.getInstance("MD5")
            if (file.extension.equals("zip", ignoreCase = true)) {
                ZipFile(file).use { zip ->
                    val entry = zip.entries().asSequence()
                        .firstOrNull { !it.isDirectory && !it.name.startsWith("__MACOSX") }
                    if (entry != null) {
                        zip.getInputStream(entry).use { input ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                digest.update(buffer, 0, read)
                            }
                        }
                        return@runCatching digest.digest().joinToString("") { "%02x".format(it) }
                    }
                }
            }
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()
    }
}
