package com.sayemshafayet.onereogamelauncher.scrape

import android.util.Log
import com.sayemshafayet.onereogamelauncher.data.prefs.OrglSettings
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

data class ScreenScraperMedia(
    val type: String,
    val url: String,
    val region: String? = null,
)

data class ScreenScraperGameInfo(
    val id: Int?,
    val name: String?,
    val synopsis: String?,
    val media: List<ScreenScraperMedia>,
)

@Singleton
class ScreenScraperClient @Inject constructor(
    private val http: OkHttpClient,
) {
    companion object {
        private const val TAG = "ScreenScraperClient"
        private const val BASE = "https://www.screenscraper.fr/api2/jeuInfos.php"
        private const val MIN_INTERVAL_MS = 1200L
        private val json = Json { ignoreUnknownKeys = true }
    }

    private val rateMutex = Mutex()
    private var lastRequestAt = 0L

    suspend fun fetchGameInfo(
        settings: OrglSettings,
        romName: String,
        systemId: Int? = null,
        md5: String? = null,
        romType: String? = null,
    ): ScreenScraperGameInfo? {
        if (settings.screenScraperUser.isBlank() || settings.screenScraperPass.isBlank()) {
            return null
        }
        rateMutex.withLock {
            val wait = MIN_INTERVAL_MS - (System.currentTimeMillis() - lastRequestAt)
            if (wait > 0) kotlinx.coroutines.delay(wait)
            lastRequestAt = System.currentTimeMillis()
        }

        val params = linkedMapOf(
            "devid" to settings.screenScraperDevid,
            "devpassword" to settings.screenScraperDevpassword,
            "softname" to "OneRetroGameLauncher",
            "output" to "json",
            "ssid" to settings.screenScraperUser,
            "sspassword" to settings.screenScraperPass,
            "romnom" to romName,
        )
        if (systemId != null) params["systemeid"] = systemId.toString()
        if (!md5.isNullOrBlank()) params["md5"] = md5
        if (!romType.isNullOrBlank()) params["romtype"] = romType

        val query = params.entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, Charsets.UTF_8.name())}"
        }
        val request = Request.Builder().url("$BASE?$query").get().build()

        return runCatching {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "ScreenScraper HTTP ${response.code}")
                    return@use null
                }
                val body = response.body?.string().orEmpty()
                parseResponse(body)
            }
        }.getOrElse {
            Log.e(TAG, "ScreenScraper request failed", it)
            null
        }
    }

    private fun parseResponse(body: String): ScreenScraperGameInfo? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val response = root["response"]?.jsonObject ?: root
        val gameNode = response["jeu"]?.jsonObject ?: return null
        val id = gameNode["id"]?.jsonPrimitive?.intOrNull
        val name = gameNode["noms"]?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
            ?: gameNode["nom"]?.jsonPrimitive?.contentOrNull
        val synopsis = gameNode["synopsis"]?.jsonPrimitive?.contentOrNull
            ?: gameNode["synopsis_us"]?.jsonPrimitive?.contentOrNull
        val media = parseMedia(gameNode)
        return ScreenScraperGameInfo(id = id, name = name, synopsis = synopsis, media = media)
    }

    private fun parseMedia(gameNode: JsonObject): List<ScreenScraperMedia> {
        val result = mutableListOf<ScreenScraperMedia>()
        fun walk(node: JsonElement?, prefix: String = "") {
            when (node) {
                is JsonObject -> node.forEach { (k, v) -> walk(v, if (prefix.isBlank()) k else "$prefix.$k") }
                is JsonArray -> node.forEach { walk(it, prefix) }
                else -> {
                    val url = node?.jsonPrimitive?.contentOrNull
                    if (!url.isNullOrBlank() && url.startsWith("http")) {
                        result += ScreenScraperMedia(type = prefix, url = url)
                    }
                }
            }
        }
        gameNode["medias"]?.let { walk(it) }
        gameNode["media"]?.let { walk(it) }
        return result.distinctBy { it.url }
    }
}
