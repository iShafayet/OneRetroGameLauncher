package com.sayemshafayet.onereogamelauncher.hltb

import android.util.Log
import com.sayemshafayet.onereogamelauncher.data.db.dao.HltbCacheDao
import com.sayemshafayet.onereogamelauncher.data.db.entity.HltbCacheEntity
import com.sayemshafayet.onereogamelauncher.domain.HltbEstimate
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

sealed interface HltbLookupResult {
    data class Found(val estimate: HltbEstimate) : HltbLookupResult
    /** Search succeeded but no playtime data for this title. */
    data object NotFound : HltbLookupResult
    /** Network / HTTP / parse failure — distinct from an empty catalog match. */
    data class Failed(val message: String?) : HltbLookupResult
}

@Singleton
class HowLongToBeatClient @Inject constructor(
    private val http: OkHttpClient,
    private val cacheDao: HltbCacheDao,
) {
    companion object {
        private const val TAG = "HowLongToBeatClient"
        const val INIT_URL = "https://howlongtobeat.com/api/bleed/init"
        const val FIND_URL = "https://howlongtobeat.com/api/bleed"
        /** Cache positive lookups for at least one month. */
        const val CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private val json = Json { ignoreUnknownKeys = true }
    }

    /**
     * Look up playtime estimates for [title].
     * Positive results (any hours field set) are cached for [CACHE_TTL_MS].
     * Empty / not-found results are **not** cached so a later retry can succeed.
     */
    suspend fun search(title: String): HltbLookupResult = withContext(Dispatchers.IO) {
        val key = title.trim().lowercase()
        if (key.isBlank()) return@withContext HltbLookupResult.NotFound

        val cached = cacheDao.get(key)
        val now = System.currentTimeMillis()
        if (cached != null && cached.hasCacheableValue() && now - cached.cachedAt < CACHE_TTL_MS) {
            return@withContext HltbLookupResult.Found(cached.toEstimate())
        }

        val live = runCatching { searchLive(title) }
            .onFailure { Log.w(TAG, "HLTB search failed for $title", it) }

        live.exceptionOrNull()?.let { err ->
            if (cached != null && cached.hasCacheableValue()) {
                return@withContext HltbLookupResult.Found(cached.toEstimate())
            }
            return@withContext HltbLookupResult.Failed(err.message ?: err::class.simpleName)
        }

        val estimate = live.getOrNull()
        if (estimate != null && estimate.hasCacheableValue()) {
            cacheDao.put(
                HltbCacheEntity(
                    queryKey = key,
                    gameId = estimate.gameId,
                    title = estimate.title,
                    mainHours = estimate.mainHours,
                    mainExtraHours = estimate.mainExtraHours,
                    completionistHours = estimate.completionistHours,
                    cachedAt = now,
                ),
            )
            return@withContext HltbLookupResult.Found(estimate)
        }

        HltbLookupResult.NotFound
    }

    /** @throws IOException on transport / HTTP failures */
    private fun searchLive(title: String): HltbEstimate? {
        val tokenData = fetchInitToken()
        val payload = buildSearchPayload(title, tokenData)
        val request = Request.Builder()
            .url(FIND_URL)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", USER_AGENT)
            .header("Origin", "https://howlongtobeat.com")
            .header("Referer", "https://howlongtobeat.com/")
            .header("Accept", "*/*")
            .header("x-auth-token", tokenData.token)
            .header("x-hp-key", tokenData.hpKey)
            .header("x-hp-val", tokenData.hpVal)
            .build()
        val body = http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HLTB find HTTP ${response.code}")
            }
            response.body?.string()
                ?: throw IOException("HLTB find empty body")
        }
        val root = json.parseToJsonElement(body).jsonObject
        val data = root["data"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        val gameId = data["game_id"]?.jsonPrimitive?.longOrNull
        val gameName = data["game_name"]?.jsonPrimitive?.contentOrNull ?: title
        return HltbEstimate(
            gameId = gameId,
            title = gameName,
            mainHours = data["comp_main"].toSeconds()?.toHours(),
            mainExtraHours = data["comp_plus"].toSeconds()?.toHours(),
            completionistHours = data["comp_100"].toSeconds()?.toHours(),
        )
    }

    private fun fetchInitToken(): HltbToken {
        val request = Request.Builder()
            .url("$INIT_URL?t=${System.currentTimeMillis()}")
            .get()
            .header("User-Agent", USER_AGENT)
            .header("Origin", "https://howlongtobeat.com")
            .header("Referer", "https://howlongtobeat.com/")
            .header("Accept", "*/*")
            .build()
        val body = http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HLTB init HTTP ${response.code}")
            }
            response.body?.string()
                ?: throw IOException("HLTB init empty body")
        }
        val root = json.parseToJsonElement(body).jsonObject
        val token = root["token"]?.jsonPrimitive?.contentOrNull
            ?: throw IOException("HLTB init missing token")
        val hpKey = root["hpKey"]?.jsonPrimitive?.contentOrNull
            ?: throw IOException("HLTB init missing hpKey")
        val hpVal = root["hpVal"]?.jsonPrimitive?.contentOrNull
            ?: throw IOException("HLTB init missing hpVal")
        return HltbToken(token, hpKey, hpVal)
    }

    private fun buildSearchPayload(title: String, token: HltbToken): String {
        val terms = title.split(" ")
            .filter { it.isNotBlank() }
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

    private fun JsonElement?.toSeconds(): Int? {
        val primitive = this?.jsonPrimitive ?: return null
        primitive.intOrNull?.let { return it }
        primitive.longOrNull?.let { return it.toInt().coerceAtLeast(0) }
        primitive.doubleOrNull?.let { return it.toInt() }
        return primitive.contentOrNull?.toDoubleOrNull()?.toInt()
    }

    private fun Int.toHours(): Double = this / 3600.0

    private fun HltbCacheEntity.toEstimate() = HltbEstimate(
        gameId = gameId,
        title = title,
        mainHours = mainHours,
        mainExtraHours = mainExtraHours,
        completionistHours = completionistHours,
    )

    private fun HltbCacheEntity.hasCacheableValue(): Boolean =
        mainHours != null || mainExtraHours != null || completionistHours != null

    private data class HltbToken(val token: String, val hpKey: String, val hpVal: String)
}

fun HltbEstimate.hasCacheableValue(): Boolean =
    mainHours != null || mainExtraHours != null || completionistHours != null

/** Prefer main story hours; fall back to extra / 100% for the compact rail. */
fun HltbEstimate.displayHours(): Double? =
    mainHours ?: mainExtraHours ?: completionistHours
