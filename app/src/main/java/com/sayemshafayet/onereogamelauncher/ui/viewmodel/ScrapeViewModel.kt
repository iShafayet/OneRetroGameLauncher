package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.dao.GameDao
import com.sayemshafayet.onereogamelauncher.data.db.dao.MediaDao
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.domain.ScrapeGameFilter
import com.sayemshafayet.onereogamelauncher.domain.ScrapeSessionState
import com.sayemshafayet.onereogamelauncher.scrape.ScrapeForegroundService
import com.sayemshafayet.onereogamelauncher.scrape.ScrapeJobConfig
import com.sayemshafayet.onereogamelauncher.scrape.ScrapeJobItem
import com.sayemshafayet.onereogamelauncher.scrape.ScrapeSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

data class ScrapeStatsUi(
    val totalGames: Int = 0,
    val scrapedGames: Int = 0,
    val lastScrapeLabel: String = "Never",
    val ssConfigured: Boolean = false,
)

data class ScrapeSystemRow(
    val system: SystemEntity,
    val gameCount: Int,
    val scrapedCount: Int,
    val selected: Boolean = true,
)

enum class ScrapeWizardStep { SYSTEMS, OPTIONS, PROGRESS }

data class ScrapeWizardUi(
    val step: ScrapeWizardStep = ScrapeWizardStep.SYSTEMS,
    val systems: List<ScrapeSystemRow> = emptyList(),
    val filter: ScrapeGameFilter = ScrapeGameFilter.MISSING_ANY_MEDIA,
    val retryThreshold: Int = 3,
    val retryDelaySec: Int = 2,
    val estimatedGames: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ScrapeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val gameDao: GameDao,
    private val mediaDao: MediaDao,
    private val settingsRepository: SettingsRepository,
    private val scrapeSessionRepository: ScrapeSessionRepository,
) : ViewModel() {

    val session: StateFlow<ScrapeSessionState> = scrapeSessionRepository.state

    private val _stats = MutableStateFlow(ScrapeStatsUi())
    val stats: StateFlow<ScrapeStatsUi> = _stats.asStateFlow()

    private val _wizard = MutableStateFlow(ScrapeWizardUi())
    val wizard: StateFlow<ScrapeWizardUi> = _wizard.asStateFlow()

    init {
        refreshStats()
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _stats.update {
                    it.copy(
                        ssConfigured = s.screenScraperUser.isNotBlank(),
                        lastScrapeLabel = formatLastScrape(s.lastScrapeAt),
                    )
                }
            }
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            val total = gameDao.countAll()
            val scraped = gameDao.countScraped()
            val last = settingsRepository.current().lastScrapeAt
            _stats.update {
                it.copy(
                    totalGames = total,
                    scrapedGames = scraped,
                    lastScrapeLabel = formatLastScrape(last),
                    ssConfigured = settingsRepository.current().screenScraperUser.isNotBlank(),
                )
            }
        }
    }

    fun prepareWizard(preselectedSystemId: Long? = null) {
        viewModelScope.launch {
            _wizard.update { it.copy(loading = true, error = null, step = ScrapeWizardStep.SYSTEMS) }
            val systems = libraryRepository.systems.first()
            val rows = systems.mapNotNull { sys ->
                val count = gameDao.countForSystem(sys.id)
                if (count <= 0) return@mapNotNull null
                val scraped = gameDao.countScrapedForSystem(sys.id)
                val selected = when (preselectedSystemId) {
                    null -> true
                    else -> sys.id == preselectedSystemId
                }
                ScrapeSystemRow(sys, count, scraped, selected = selected)
            }.sortedBy { it.system.displayName.lowercase() }
            val step = if (
                preselectedSystemId != null && rows.any { it.selected }
            ) {
                ScrapeWizardStep.OPTIONS
            } else {
                ScrapeWizardStep.SYSTEMS
            }
            _wizard.update {
                it.copy(
                    loading = false,
                    systems = rows,
                    step = step,
                    estimatedGames = estimateSelected(rows, it.filter),
                )
            }
        }
    }

    fun toggleSystem(systemId: Long) {
        _wizard.update { state ->
            val systems = state.systems.map {
                if (it.system.id == systemId) it.copy(selected = !it.selected) else it
            }
            state.copy(
                systems = systems,
                estimatedGames = estimateSelectedSync(systems, state.filter),
            )
        }
        // Recompute estimate async for accuracy with filter
        viewModelScope.launch {
            val state = _wizard.value
            _wizard.update {
                it.copy(estimatedGames = estimateSelected(state.systems, state.filter))
            }
        }
    }

    fun selectAllSystems(selected: Boolean) {
        _wizard.update { state ->
            val systems = state.systems.map { it.copy(selected = selected) }
            state.copy(systems = systems)
        }
        viewModelScope.launch {
            val state = _wizard.value
            _wizard.update {
                it.copy(estimatedGames = estimateSelected(state.systems, state.filter))
            }
        }
    }

    fun setFilter(filter: ScrapeGameFilter) {
        _wizard.update { it.copy(filter = filter) }
        viewModelScope.launch {
            val state = _wizard.value
            _wizard.update {
                it.copy(estimatedGames = estimateSelected(state.systems, filter))
            }
        }
    }

    fun setRetryThreshold(value: Int) {
        _wizard.update { it.copy(retryThreshold = value.coerceIn(1, 10)) }
    }

    fun setRetryDelaySec(value: Int) {
        _wizard.update { it.copy(retryDelaySec = value.coerceIn(0, 60)) }
    }

    fun goToOptions() {
        val selected = _wizard.value.systems.any { it.selected }
        if (!selected) {
            _wizard.update { it.copy(error = "Select at least one system") }
            return
        }
        _wizard.update { it.copy(step = ScrapeWizardStep.OPTIONS, error = null) }
    }

    fun goToSystems() {
        _wizard.update { it.copy(step = ScrapeWizardStep.SYSTEMS, error = null) }
    }

    fun startScrape() {
        viewModelScope.launch {
            val state = _wizard.value
            val items = mutableListOf<ScrapeJobItem>()
            for (row in state.systems.filter { it.selected }) {
                val games = gameDao.getBySystem(row.system.id)
                for (game in games) {
                    if (matchesFilter(game, state.filter)) {
                        items += ScrapeJobItem(
                            gameId = game.id,
                            systemFolder = row.system.folderName,
                            systemDisplayName = row.system.displayName,
                        )
                    }
                }
            }
            if (items.isEmpty()) {
                _wizard.update {
                    it.copy(error = "No games match the selected condition")
                }
                return@launch
            }
            scrapeSessionRepository.queueJob(
                ScrapeJobConfig(
                    items = items,
                    retryThreshold = state.retryThreshold,
                    retryDelayMs = state.retryDelaySec * 1_000L,
                ),
            )
            _wizard.update {
                it.copy(step = ScrapeWizardStep.PROGRESS, error = null, estimatedGames = items.size)
            }
            ScrapeForegroundService.startSession(context)
        }
    }

    fun cancelScrape() {
        ScrapeForegroundService.cancel(context)
    }

    private suspend fun matchesFilter(game: GameEntity, filter: ScrapeGameFilter): Boolean =
        when (filter) {
            ScrapeGameFilter.ALL -> true
            ScrapeGameFilter.MISSING_METADATA -> game.description.isNullOrBlank()
            ScrapeGameFilter.MISSING_ANY_MEDIA -> mediaDao.countForGame(game.id) == 0
            ScrapeGameFilter.MISSING_VIDEO ->
                mediaDao.countForGameType(game.id, MediaType.VIDEO) == 0
        }

    private fun estimateSelectedSync(rows: List<ScrapeSystemRow>, filter: ScrapeGameFilter): Int {
        // Cheap estimate before async refine
        return when (filter) {
            ScrapeGameFilter.ALL -> rows.filter { it.selected }.sumOf { it.gameCount }
            else -> rows.filter { it.selected }.sumOf { (it.gameCount - it.scrapedCount).coerceAtLeast(0) }
        }
    }

    private suspend fun estimateSelected(
        rows: List<ScrapeSystemRow>,
        filter: ScrapeGameFilter,
    ): Int {
        var total = 0
        for (row in rows.filter { it.selected }) {
            val games = gameDao.getBySystem(row.system.id)
            total += games.count { matchesFilter(it, filter) }
        }
        return total
    }

    private fun formatLastScrape(epochMs: Long?): String {
        if (epochMs == null || epochMs <= 0L) return "Never"
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(epochMs))
    }
}
