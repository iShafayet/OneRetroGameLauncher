package com.sayemshafayet.onereogamelauncher.scrape

import android.content.Context
import android.util.Log
import com.sayemshafayet.onereogamelauncher.data.db.dao.GameDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.MediaDao
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.OrglSettings
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.domain.ScrapeProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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
    private val gameDao: GameDao,
    private val libretroClient: LibretroThumbnailsClient,
) {
    companion object {
        private const val TAG = "ArtworkScraper"
        const val PROVIDER_SCREENSCRAPER = "screenscraper"
        const val PROVIDER_LIBRETRO = "libretro-thumbnails"
        const val PROVIDER_ORGL = "orgl"
    }

    private val writeMutex = Mutex()

    /**
     * @return true if media and/or metadata was saved; false if nothing useful was found.
     * Throws on hard failures after retries are exhausted (caller may treat as failed).
     */
    suspend fun scrapeGame(
        game: GameEntity,
        systemFolder: String,
        settings: OrglSettings,
        orglDataDirPath: String?,
        retryThreshold: Int = 3,
        retryDelayMs: Long = 2_000L,
        onProgress: (ScrapeProgress) -> Unit = {},
    ): Boolean {
        coroutineContext.ensureActive()
        onProgress(ScrapeProgress(game.title, 0, 1))

        val attempts = retryThreshold.coerceAtLeast(1)
        var lastError: Exception? = null
        var saved = false

        repeat(attempts) { attempt ->
            coroutineContext.ensureActive()
            try {
                saved = scrapeGameOnce(game, systemFolder, settings, orglDataDirPath)
                lastError = null
                return@repeat
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Scrape attempt ${attempt + 1}/$attempts failed for ${game.title}", e)
                if (attempt < attempts - 1) delay(retryDelayMs.coerceAtLeast(0L))
            }
        }

        // Always stamp lastScrapedAt so "scraped" stats reflect attempts with ORGL.
        markScraped(game.id)
        onProgress(ScrapeProgress(game.title, 1, 1))
        if (lastError != null && !saved) throw lastError
        return saved
    }

    private suspend fun scrapeGameOnce(
        game: GameEntity,
        systemFolder: String,
        settings: OrglSettings,
        orglDataDirPath: String?,
    ): Boolean {
        // ScreenScraper is intentionally not attempted — libretro-thumbnails only for now.
        return saveLibretroFallback(game, systemFolder, orglDataDirPath)
    }

    private suspend fun markScraped(gameId: Long) {
        val latest = gameDao.getById(gameId) ?: return
        gameDao.update(latest.copy(lastScrapedAt = System.currentTimeMillis()))
    }

    suspend fun scrapeGames(
        games: List<GameEntity>,
        systemFolder: String,
        settings: OrglSettings,
        orglDataDirPath: String?,
        onProgress: (ScrapeProgress) -> Unit,
        isCancelled: () -> Boolean = { false },
        retryThreshold: Int = 3,
        retryDelayMs: Long = 2_000L,
    ) {
        games.forEachIndexed { index, game ->
            if (isCancelled()) throw CancellationException("Scrape cancelled")
            onProgress(ScrapeProgress(game.title, index, games.size))
            scrapeGame(
                game = game,
                systemFolder = systemFolder,
                settings = settings,
                orglDataDirPath = orglDataDirPath,
                retryThreshold = retryThreshold,
                retryDelayMs = retryDelayMs,
            )
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
}
