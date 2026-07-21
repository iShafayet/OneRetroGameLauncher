package com.sayemshafayet.onereogamelauncher.scrape

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sayemshafayet.onereogamelauncher.MainActivity
import com.sayemshafayet.onereogamelauncher.R
import com.sayemshafayet.onereogamelauncher.data.db.dao.GameDao
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScrapeForegroundService : Service() {
    @Inject lateinit var artworkScraper: ArtworkScraper
    @Inject lateinit var gameDao: GameDao
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var scrapeSessionRepository: ScrapeSessionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scrapeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                scrapeJob?.cancel()
                return START_NOT_STICKY
            }
            ACTION_SCRAPE_GAME -> {
                val gameId = intent.getLongExtra(EXTRA_GAME_ID, -1L)
                val systemFolder = intent.getStringExtra(EXTRA_SYSTEM_FOLDER).orEmpty()
                scrapeSessionRepository.queueJob(
                    ScrapeJobConfig(
                        items = listOf(
                            ScrapeJobItem(gameId, systemFolder, systemFolder),
                        ),
                    ),
                )
                startQueuedJob()
            }
            ACTION_SCRAPE_SYSTEM -> {
                val systemFolder = intent.getStringExtra(EXTRA_SYSTEM_FOLDER).orEmpty()
                val ids = intent.getLongArrayExtra(EXTRA_GAME_IDS)?.toList().orEmpty()
                scrapeSessionRepository.queueJob(
                    ScrapeJobConfig(
                        items = ids.map { ScrapeJobItem(it, systemFolder, systemFolder) },
                    ),
                )
                startQueuedJob()
            }
            ACTION_SCRAPE_SESSION -> startQueuedJob()
        }
        return START_NOT_STICKY
    }

    private fun startQueuedJob() {
        val config = scrapeSessionRepository.takeJob() ?: return
        scrapeJob?.cancel()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting scrape…", 0, 0, indeterminate = true))
        scrapeJob = scope.launch {
            var cancelled = false
            try {
                val settings = settingsRepository.settings.first()
                val orglData = settings.orglDataDirPath
                val items = config.items
                scrapeSessionRepository.markRunning(items.size)
                items.forEachIndexed { index, item ->
                    if (scrapeJob?.isCancelled == true) throw CancellationException("Scrape cancelled")
                    val game = gameDao.getById(item.gameId)
                    if (game == null) {
                        scrapeSessionRepository.onGameSkipped(index + 1, items.size)
                        return@forEachIndexed
                    }
                    scrapeSessionRepository.onGameStart(
                        title = game.title,
                        system = item.systemDisplayName,
                        index = index,
                        total = items.size,
                    )
                    updateNotification(game.title, index, items.size)
                    try {
                        artworkScraper.scrapeGame(
                            game = game,
                            systemFolder = item.systemFolder,
                            settings = settings,
                            orglDataDirPath = orglData,
                            retryThreshold = config.retryThreshold,
                            retryDelayMs = config.retryDelayMs,
                        )
                        scrapeSessionRepository.onGameSuccess(index + 1, items.size)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        scrapeSessionRepository.onGameFailed(
                            index + 1,
                            items.size,
                            e.message ?: e::class.java.simpleName,
                        )
                    }
                    updateNotification(game.title, index + 1, items.size)
                }
                settingsRepository.setLastScrapeAt(System.currentTimeMillis())
            } catch (_: CancellationException) {
                cancelled = true
            } finally {
                scrapeSessionRepository.markFinished(cancelled = cancelled)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun updateNotification(title: String, index: Int, total: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(
            NOTIFICATION_ID,
            buildNotification(title, index, total, indeterminate = total <= 0),
        )
    }

    private fun buildNotification(
        title: String,
        index: Int,
        total: Int,
        indeterminate: Boolean,
    ): Notification {
        val cancelIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ScrapeForegroundService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.scrape_notification_title))
            .setContentText(title)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(0, getString(R.string.scrape_cancel), cancelIntent)
        if (indeterminate) {
            builder.setProgress(0, 0, true)
        } else if (total > 0) {
            builder.setProgress(total, index.coerceAtMost(total), false)
        }
        return builder.build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.scrape_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        scrapeJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "orgl_scrape"
        const val NOTIFICATION_ID = 42

        const val ACTION_SCRAPE_GAME = "scrape_game"
        const val ACTION_SCRAPE_SYSTEM = "scrape_system"
        const val ACTION_SCRAPE_SESSION = "scrape_session"
        const val ACTION_CANCEL = "scrape_cancel"

        const val EXTRA_GAME_ID = "game_id"
        const val EXTRA_GAME_IDS = "game_ids"
        const val EXTRA_SYSTEM_FOLDER = "system_folder"

        fun scrapeGame(context: Context, gameId: Long, systemFolder: String) {
            context.startForegroundService(
                Intent(context, ScrapeForegroundService::class.java).apply {
                    action = ACTION_SCRAPE_GAME
                    putExtra(EXTRA_GAME_ID, gameId)
                    putExtra(EXTRA_SYSTEM_FOLDER, systemFolder)
                },
            )
        }

        fun scrapeSystem(context: Context, gameIds: LongArray, systemFolder: String) {
            context.startForegroundService(
                Intent(context, ScrapeForegroundService::class.java).apply {
                    action = ACTION_SCRAPE_SYSTEM
                    putExtra(EXTRA_GAME_IDS, gameIds)
                    putExtra(EXTRA_SYSTEM_FOLDER, systemFolder)
                },
            )
        }

        fun startSession(context: Context) {
            context.startForegroundService(
                Intent(context, ScrapeForegroundService::class.java).apply {
                    action = ACTION_SCRAPE_SESSION
                },
            )
        }

        fun cancel(context: Context) {
            context.startService(
                Intent(context, ScrapeForegroundService::class.java).apply {
                    action = ACTION_CANCEL
                },
            )
        }
    }
}
