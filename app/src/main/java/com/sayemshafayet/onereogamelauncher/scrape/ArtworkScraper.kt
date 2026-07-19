package com.sayemshafayet.onereogamelauncher.scrape

import android.content.Context
import android.util.Log
import com.sayemshafayet.onereogamelauncher.data.db.dao.MediaDao
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.OrglSettings
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.domain.ScrapeProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.coroutines.coroutineContext

@Singleton
class ArtworkScraper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: OkHttpClient,
    private val mediaDao: MediaDao,
    private val screenScraperClient: ScreenScraperClient,
    private val libretroClient: LibretroThumbnailsClient,
) {
    companion object {
        private const val TAG = "ArtworkScraper"
        const val PROVIDER_SCREENSCRAPER = "screenscraper"
        const val PROVIDER_LIBRETRO = "libretro-thumbnails"
        const val PROVIDER_ORGL = "orgl"
    }

    private val writeMutex = Mutex()

    suspend fun scrapeGame(
        game: GameEntity,
        systemFolder: String,
        settings: OrglSettings,
        orglDataDirPath: String?,
        onProgress: (ScrapeProgress) -> Unit = {},
    ): Boolean {
        coroutineContext.ensureActive()
        onProgress(ScrapeProgress(game.title, 0, 1))

        val romFile = File(game.romPath)
        val romName = romFile.name
        val md5 = runCatching { md5Hex(romFile) }.getOrNull()

        var saved = false
        if (settings.screenScraperUser.isNotBlank()) {
            val info = screenScraperClient.fetchGameInfo(
                settings = settings,
                romName = romName,
                md5 = md5,
            )
            if (info != null) {
                saved = saveScreenScraperMedia(game, systemFolder, orglDataDirPath, info.media) || saved
            }
        }

        if (!saved) {
            saved = saveLibretroFallback(game, systemFolder, orglDataDirPath) || saved
        }

        onProgress(ScrapeProgress(game.title, 1, 1))
        return saved
    }

    suspend fun scrapeGames(
        games: List<GameEntity>,
        systemFolder: String,
        settings: OrglSettings,
        orglDataDirPath: String?,
        onProgress: (ScrapeProgress) -> Unit,
        isCancelled: () -> Boolean = { false },
    ) {
        games.forEachIndexed { index, game ->
            if (isCancelled()) throw CancellationException("Scrape cancelled")
            onProgress(ScrapeProgress(game.title, index, games.size))
            scrapeGame(game, systemFolder, settings, orglDataDirPath)
            onProgress(ScrapeProgress(game.title, index + 1, games.size))
        }
    }

    private suspend fun saveLibretroFallback(
        game: GameEntity,
        systemFolder: String,
        orglDataDirPath: String?,
    ): Boolean {
        val urls = libretroClient.firstAvailable(systemFolder, game.title)
        if (urls.isEmpty()) return false
        var any = false
        urls.forEach { (kind, url) ->
            val type = when (kind) {
                "boxart" -> MediaType.BOX_2D
                "title" -> MediaType.TITLE
                "snap" -> MediaType.SCREENSHOT
                else -> MediaType.UNKNOWN
            }
            val ext = "png"
            val fileName = "${sanitizeFileName(game.title)}.$ext"
            val dest = mediaDestDir(systemFolder, type, orglDataDirPath).resolve(fileName)
            if (downloadTo(url, dest)) {
                upsertMedia(game.id, type, dest.absolutePath, PROVIDER_LIBRETRO)
                any = true
            }
        }
        return any
    }

    private suspend fun saveScreenScraperMedia(
        game: GameEntity,
        systemFolder: String,
        orglDataDirPath: String?,
        media: List<ScreenScraperMedia>,
    ): Boolean {
        var any = false
        for (item in media) {
            val type = mapScreenScraperType(item.type)
            if (type == MediaType.UNKNOWN) continue
            val ext = item.url.substringAfterLast('.', "png").substringBefore('?')
            val fileName = "${sanitizeFileName(game.title)}_${type.name.lowercase()}.$ext"
            val dest = mediaDestDir(systemFolder, type, orglDataDirPath).resolve(fileName)
            if (downloadTo(item.url, dest)) {
                upsertMedia(game.id, type, dest.absolutePath, PROVIDER_SCREENSCRAPER)
                any = true
            }
        }
        return any
    }

    private suspend fun upsertMedia(gameId: Long, type: MediaType, path: String, provider: String) {
        writeMutex.withLock {
            mediaDao.upsert(
                MediaEntity(
                    gameId = gameId,
                    type = type,
                    path = path,
                    provider = provider,
                ),
            )
        }
    }

    /**
     * Writes only under the ORGL data directory (never ES-DE). Falls back to app-private storage
     * when the user has not selected an ORGL data folder yet.
     */
    fun mediaDestDir(
        systemFolder: String,
        type: MediaType,
        orglDataDirPath: String?,
    ): File {
        val esFolder = when (type) {
            MediaType.BOX_2D -> "covers"
            MediaType.BOX_3D -> "3dboxes"
            MediaType.SCREENSHOT -> "screenshots"
            MediaType.TITLE -> "titlescreens"
            MediaType.MARQUEE -> "marquees"
            MediaType.VIDEO -> "videos"
            MediaType.FANART -> "fanart"
            MediaType.UNKNOWN -> "miximages"
        }
        val relative = "downloaded_media/$systemFolder/$esFolder"
        if (!orglDataDirPath.isNullOrBlank()) {
            val candidate = File(orglDataDirPath, relative)
            if (candidate.exists() || candidate.mkdirs()) return candidate
        }
        return File(context.filesDir, relative).apply { mkdirs() }
    }

    private fun mapScreenScraperType(raw: String): MediaType {
        val key = raw.lowercase()
        return when {
            "box" in key && "3d" in key -> MediaType.BOX_3D
            "box" in key || "jaquette" in key -> MediaType.BOX_2D
            "ss" in key || "snap" in key || "screen" in key -> MediaType.SCREENSHOT
            "title" in key -> MediaType.TITLE
            "wheel" in key || "marquee" in key || "logo" in key -> MediaType.MARQUEE
            "video" in key -> MediaType.VIDEO
            "fanart" in key -> MediaType.FANART
            else -> MediaType.UNKNOWN
        }
    }

    private fun downloadTo(url: String, dest: File): Boolean =
        runCatching {
            dest.parentFile?.mkdirs()
            http.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                if (!response.isSuccessful) return false
                dest.outputStream().use { out ->
                    response.body?.byteStream()?.copyTo(out)
                }
                true
            }
        }.getOrElse {
            Log.w(TAG, "Download failed: $url", it)
            false
        }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("""[\\/:*?"<>|]"""), "_").take(120)

    private fun md5Hex(file: File): String {
        if (!file.isFile) error("Not a file")
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
