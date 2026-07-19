package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.sayemshafayet.onereogamelauncher.scrape.ScrapeForegroundService

@HiltViewModel
class ScrapeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    val systems = libraryRepository.systems.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun scrapeSystem(systemId: Long, folderName: String) {
        viewModelScope.launch {
            val games = libraryRepository.observeGamesBySystem(systemId).first()
            if (games.isEmpty()) {
                _message.value = "No games in this system"
                return@launch
            }
            ScrapeForegroundService.scrapeSystem(
                context,
                games.map { it.id }.toLongArray(),
                folderName,
            )
            _message.value = "Scraping ${games.size} games — check notification for progress"
        }
    }

    fun scrapeAll() {
        viewModelScope.launch {
            val systems = libraryRepository.systems.first()
            var total = 0
            systems.forEach { sys ->
                val games = libraryRepository.observeGamesBySystem(sys.id).first()
                if (games.isNotEmpty()) {
                    ScrapeForegroundService.scrapeSystem(
                        context,
                        games.map { it.id }.toLongArray(),
                        sys.folderName,
                    )
                    total += games.size
                }
            }
            _message.value = if (total > 0) {
                "Queued $total games across ${systems.size} systems"
            } else {
                "No games to scrape — scan your library first"
            }
        }
    }
}
