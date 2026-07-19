package com.sayemshafayet.onereogamelauncher.scrape

import com.sayemshafayet.onereogamelauncher.domain.ScrapeSessionState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScrapeJobItem(
    val gameId: Long,
    val systemFolder: String,
    val systemDisplayName: String,
)

data class ScrapeJobConfig(
    val items: List<ScrapeJobItem>,
    val retryThreshold: Int = 3,
    val retryDelayMs: Long = 2_000L,
)

@Singleton
class ScrapeSessionRepository @Inject constructor() {
    private val _state = MutableStateFlow(ScrapeSessionState())
    val state: StateFlow<ScrapeSessionState> = _state.asStateFlow()

    @Volatile
    var pendingJob: ScrapeJobConfig? = null
        private set

    fun queueJob(config: ScrapeJobConfig) {
        pendingJob = config
        _state.value = ScrapeSessionState(
            running = false,
            finished = false,
            total = config.items.size,
            pending = config.items.size,
        )
    }

    fun takeJob(): ScrapeJobConfig? {
        val job = pendingJob
        pendingJob = null
        return job
    }

    fun markRunning(total: Int) {
        _state.value = ScrapeSessionState(
            running = true,
            finished = false,
            total = total,
            pending = total,
            startedAtMs = System.currentTimeMillis(),
        )
    }

    fun onGameStart(title: String, system: String, index: Int, total: Int) {
        _state.update {
            it.copy(
                running = true,
                currentTitle = title,
                currentSystem = system,
                index = index,
                total = total,
                pending = (total - index).coerceAtLeast(0),
            )
        }
    }

    fun onGameSuccess(index: Int, total: Int) {
        _state.update {
            it.copy(
                index = index,
                total = total,
                completed = it.completed + 1,
                pending = (total - index).coerceAtLeast(0),
                lastError = null,
            )
        }
    }

    fun onGameFailed(index: Int, total: Int, error: String?) {
        _state.update {
            it.copy(
                index = index,
                total = total,
                failed = it.failed + 1,
                pending = (total - index).coerceAtLeast(0),
                lastError = error,
            )
        }
    }

    fun onGameSkipped(index: Int, total: Int) {
        _state.update {
            it.copy(
                index = index,
                total = total,
                skipped = it.skipped + 1,
                pending = (total - index).coerceAtLeast(0),
            )
        }
    }

    fun markFinished(cancelled: Boolean) {
        _state.update {
            it.copy(
                running = false,
                finished = true,
                cancelled = cancelled,
                finishedAtMs = System.currentTimeMillis(),
                pending = 0,
                currentTitle = if (cancelled) "Cancelled" else "Done",
            )
        }
    }

    fun clear() {
        pendingJob = null
        _state.value = ScrapeSessionState()
    }
}
