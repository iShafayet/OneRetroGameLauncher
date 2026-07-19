package com.sayemshafayet.onereogamelauncher.hltb

import android.util.Log
import com.sayemshafayet.onereogamelauncher.data.db.dao.HltbCacheDao
import com.sayemshafayet.onereogamelauncher.data.db.entity.HltbCacheEntity
import com.sayemshafayet.onereogamelauncher.domain.HltbEstimate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class HowLongToBeatClient @Inject constructor(
    private val http: OkHttpClient,
    private val cacheDao: HltbCacheDao,
) {
    companion object {
        private const val TAG = "HowLongToBeatClient"
        private const val INIT_URL = "https://howlongtobeat.com/api/find/init"
        private const val FIND_URL = "https://howlongtobeat.com/api/find"
        private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private val json = Json { ignoreUnknownKeys = true }
    }

    suspend fun search(title: String): HltbEstimate? {
        val key = title.trim().lowercase()
        if (key.isBlank()) return null
        cacheDao.get(key)?.let { cached ->
            if (System.currentTimeMillis() - cached.cachedAt < CACHE_TTL_MS) {
                return cached.toEstimate()
            }
        }
        val estimate = runCatching { searchLive(title) }.getOrElse {
            Log.w(TAG, "HLTB search failed for $title", it)
            null
        } ?: return null
        cacheDao.put(
            HltbCacheEntity(
                queryKey = key,
                gameId = estimate.gameId,
                title = estimate.title,
                mainHours = estimate.mainHours,
                mainExtraHours = estimate.mainExtraHours,
                completionistHours = estimate.completionistHours,
                cachedAt = System.currentTimeMillis(),
            ),
        )
        return estimate
    }

    private fun searchLive(title: String): HltbEstimate? {
        val tokenData = fetchInitToken() ?: return null
        val payload = buildSearchPayload(title, tokenData)
        val request = Request.Builder()
            .url(FIND_URL)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", USER_AGENT)
            .header("Origin", "https://howlongtobeat.com")
            .header("Referer", "https://howlongtobeat.com/")
            .header("x-auth-token", tokenData.token)
            .header("x-hp-key", tokenData.hpKey)
            .header("x-hp-val", tokenData.hpVal)
            .build()
        val body = http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string()
        } ?: return null
        val root = json.parseToJsonElement(body).jsonObject
        val data = root["data"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        val gameId = data["game_id"]?.jsonPrimitive?.longOrNull
        val gameName = data["game_name"]?.jsonPrimitive?.contentOrNull ?: title
        val mainSec = data["comp_main"]?.jsonPrimitive?.intOrNull
        val extraSec = data["comp_plus"]?.jsonPrimitive?.intOrNull
        val compSec = data["comp_100"]?.jsonPrimitive?.intOrNull
        return HltbEstimate(
            gameId = gameId,
            title = gameName,
            mainHours = mainSec?.toHours(),
            mainExtraHours = extraSec?.toHours(),
            completionistHours = compSec?.toHours(),
        )
    }

    private fun fetchInitToken(): HltbToken? {
        val request = Request.Builder()
            .url(INIT_URL)
            .post("".toRequestBody(null))
            .header("User-Agent", USER_AGENT)
            .header("Origin", "https://howlongtobeat.com")
            .header("Referer", "https://howlongtobeat.com/")
            .build()
        val body = http.newCall(request).execute().use { it.body?.string() } ?: return null
        val root = json.parseToJsonElement(body).jsonObject
        val token = root["token"]?.jsonPrimitive?.contentOrNull ?: return null
        val hpKey = root["hpKey"]?.jsonPrimitive?.contentOrNull ?: return null
        val hpVal = root["hpVal"]?.jsonPrimitive?.contentOrNull ?: return null
        return HltbToken(token, hpKey, hpVal)
    }

    private fun buildSearchPayload(title: String, token: HltbToken): String {
        val terms = title.split(" ")
            .joinToString(", ") { "\"${it.replace("\"", "\\\"")}\"" }
        return """
            {
              "searchType": "games",
              "searchTerms": [$terms],
              "searchPage": 1,
              "size": 5,
              "searchOptions": {
                "games": {
                  "userId": 0,
                  "platform": "",
                  "sortCategory": "popular",
                  "rangeCategory": "main",
                  "rangeTime": {"min": null, "max": null},
                  "gameplay": {"perspective": "", "flow": "", "genre": "", "difficulty": ""},
                  "rangeYear": {"min": "", "max": ""},
                  "modifier": ""
                },
                "users": {"sortCategory": "postcount"},
                "lists": {"sortCategory": "follows"},
                "filter": "",
                "sort": 0,
                "randomizer": 0
              },
              "useCache": true,
              "${token.hpKey}": "${token.hpVal}"
            }
        """.trimIndent()
    }

    private fun Int.toHours(): Double = this / 3600.0

    private fun HltbCacheEntity.toEstimate() = HltbEstimate(
        gameId = gameId,
        title = title,
        mainHours = mainHours,
        mainExtraHours = mainExtraHours,
        completionistHours = completionistHours,
    )

    private data class HltbToken(val token: String, val hpKey: String, val hpVal: String)
}
