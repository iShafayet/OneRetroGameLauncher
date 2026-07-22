package com.sayemshafayet.onereogamelauncher.play

import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tracks ORGL-owned launch counts, last-played timestamps, and playtime for Setup-mode launches.
 * Play-mode commitment sessions store duration in [play_sessions]; this tracker skips timing
 * when a commitment is active for the launched game.
 */
@Singleton
class PlayStatsTracker @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val commitmentRepository: CommitmentRepository,
) {
    private val mutex = Mutex()
    private var openGameId: Long? = null
    private var startedAtMs: Long = 0L

    suspend fun onLaunchSuccess(gameId: Long) {
        libraryRepository.recordGameLaunch(gameId)
        mutex.withLock {
            openGameId = gameId
            startedAtMs = System.currentTimeMillis()
        }
    }

    suspend fun onAppForeground() {
        val snapshot = mutex.withLock {
            val id = openGameId ?: return
            val start = startedAtMs
            openGameId = null
            startedAtMs = 0L
            id to start
        }
        val (gameId, startedAt) = snapshot
        if (startedAt <= 0L) return

        val active = commitmentRepository.getActiveForGame(gameId)
        if (active != null) {
            // Play mode records session duration via CommitmentRepository.
            return
        }

        val durationMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
        libraryRepository.addGamePlaytime(gameId, durationMs)
    }
}
