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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class ScrapeForegroundService : Service() {
    @Inject lateinit var artworkScraper: ArtworkScraper
    @Inject lateinit var gameDao: GameDao
    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scrapeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                scrapeJob?.cancel()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SCRAPE_GAME -> {
                val gameId = intent.getLongExtra(EXTRA_GAME_ID, -1L)
                val systemFolder = intent.getStringExtra(EXTRA_SYSTEM_FOLDER).orEmpty()
                startScrape(listOf(gameId), systemFolder)
            }
            ACTION_SCRAPE_SYSTEM -> {
                val systemFolder = intent.getStringExtra(EXTRA_SYSTEM_FOLDER).orEmpty()
                val ids = intent.getLongArrayExtra(EXTRA_GAME_IDS)?.toList().orEmpty()
                startScrape(ids, systemFolder)
            }
        }
        return START_NOT_STICKY
    }

    private fun startScrape(gameIds: List<Long>, systemFolder: String) {
        scrapeJob?.cancel()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting scrape…", 0, 0, indeterminate = true))
        scrapeJob = scope.launch {
            try {
                val settings = settingsRepository.settings.first()
                val orglData = settings.orglDataDirPath
                val games = gameIds.mapNotNull { gameDao.getById(it) }
                artworkScraper.scrapeGames(
                    games = games,
                    systemFolder = systemFolder,
                    settings = settings,
                    orglDataDirPath = orglData,
                    onProgress = { progress ->
                        updateNotification(
                            progress.currentTitle,
                            progress.index,
                            progress.total,
                        )
                    },
                    isCancelled = { scrapeJob?.isCancelled == true },
                )
            } catch (_: CancellationException) {
                // user cancelled
            } finally {
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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
    }
}
