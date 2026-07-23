package com.sayemshafayet.onereogamelauncher.scrape

import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.Request

data class LibretroThumbnailSet(
    val boxartUrl: String?,
    val titleUrl: String?,
    val snapUrl: String?,
)

@Singleton
class LibretroThumbnailsClient @Inject constructor(
    private val http: OkHttpClient,
) {
    companion object {
        private const val BASE = "https://thumbnails.libretro.com"
    }

    private val systemMap = mapOf(
        "nes" to "Nintendo_-_Nintendo_Entertainment_System",
        "snes" to "Nintendo_-_Super_Nintendo_Entertainment_System",
        "n64" to "Nintendo_-_Nintendo_64",
        "gb" to "Nintendo_-_Game_Boy",
        "gbc" to "Nintendo_-_Game_Boy_Color",
        "gba" to "Nintendo_-_Game_Boy_Advance",
        "nds" to "Nintendo_-_Nintendo_DS",
        "3ds" to "Nintendo_-_Nintendo_3DS",
        "genesis" to "Sega_-_Mega_Drive_-_Genesis",
        "megadrive" to "Sega_-_Mega_Drive_-_Genesis",
        "mastersystem" to "Sega_-_Master_System_-_Mark_III",
        "gamegear" to "Sega_-_Game_Gear",
        "saturn" to "Sega_-_Saturn",
        "dreamcast" to "Sega_-_Dreamcast",
        "psx" to "Sony_-_PlayStation",
        "ps1" to "Sony_-_PlayStation",
        "psp" to "Sony_-_PlayStation_Portable",
        "pcengine" to "NEC_-_PC_Engine_-_TurboGrafx_16",
        "tg16" to "NEC_-_PC_Engine_-_TurboGrafx_16",
        "atari2600" to "Atari_-_2600",
        "atari7800" to "Atari_-_7800",
        "neogeo" to "SNK_-_Neo_Geo",
        "neogeocd" to "SNK_-_Neo_Geo_CD",
        "wonderswan" to "Bandai_-_WonderSwan",
        "wonderswancolor" to "Bandai_-_WonderSwan_Color",
        "arcade" to "FBNeo_-_Arcade_Games",
        "mame" to "MAME",
    )

    fun libretroSystemName(systemFolder: String): String? =
        systemMap[systemFolder.lowercase()]?.let { cdnSystemName(it) }

    private fun cdnSystemName(githubStyle: String): String =
        githubStyle.replace("_-_", " - ").replace("_", " ")

    fun thumbnailUrls(systemFolder: String, gameTitle: String): LibretroThumbnailSet? {
        val system = libretroSystemName(systemFolder) ?: return null
        val safeName = gameTitle.trim()
        if (safeName.isBlank()) return null
        val systemEncoded = URLEncoder.encode(system, Charsets.UTF_8.name()).replace("+", "%20")
        val encoded = URLEncoder.encode(safeName, Charsets.UTF_8.name()).replace("+", "%20")
        return LibretroThumbnailSet(
            boxartUrl = "$BASE/$systemEncoded/Named_Boxarts/$encoded.png",
            titleUrl = "$BASE/$systemEncoded/Named_Titles/$encoded.png",
            snapUrl = "$BASE/$systemEncoded/Named_Snaps/$encoded.png",
        )
    }

    suspend fun firstAvailable(systemFolder: String, gameTitle: String): Map<String, String> {
        val urls = thumbnailUrls(systemFolder, gameTitle) ?: return emptyMap()
        val result = linkedMapOf<String, String>()
        urls.boxartUrl?.let { if (exists(it)) result["boxart"] = it }
        urls.titleUrl?.let { if (exists(it)) result["title"] = it }
        urls.snapUrl?.let { if (exists(it)) result["snap"] = it }
        return result
    }

    private fun exists(url: String): Boolean =
        runCatching {
            http.newCall(Request.Builder().url(url).head().build()).execute().use { it.isSuccessful }
        }.getOrDefault(false)
}
