package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.VirtualLibrarySystem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SystemWithCount(
    val system: SystemEntity,
    val gameCount: Int,
)

sealed interface LibrarySystemRow {
    val id: Long
    val displayName: String
    val subtitle: String
    val gameCount: Int

    data class Virtual(
        val virtual: VirtualLibrarySystem,
        override val gameCount: Int,
    ) : LibrarySystemRow {
        override val id: Long = virtual.id
        override val displayName: String = virtual.displayName
        override val subtitle: String = virtual.subtitle
    }

    data class Physical(
        val row: SystemWithCount,
    ) : LibrarySystemRow {
        override val id: Long = row.system.id
        override val displayName: String = row.system.displayName
        override val subtitle: String = "${row.system.folderName} · ${row.gameCount} games"
        override val gameCount: Int = row.gameCount
    }
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _systemsWithCounts = MutableStateFlow<List<SystemWithCount>>(emptyList())

    val visibleSystems: StateFlow<List<LibrarySystemRow>> = combine(
        _systemsWithCounts,
        settingsRepository.settings,
        libraryRepository.observeFavorites().map { it.size },
        libraryRepository.observeRecent().map { it.size },
    ) { systems, settings, favoriteCount, recentCount ->
        buildList {
            if (settings.libraryShowFavorites) {
                add(LibrarySystemRow.Virtual(VirtualLibrarySystem.FAVORITES, favoriteCount))
            }
            if (settings.libraryShowRecent) {
                add(LibrarySystemRow.Virtual(VirtualLibrarySystem.RECENT, recentCount))
            }
            systems
                .filter { it.gameCount > 0 }
                .sortedBy { it.system.displayName.lowercase() }
                .forEach { add(LibrarySystemRow.Physical(it)) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalGames: StateFlow<Int> = _systemsWithCounts
        .map { list -> list.sumOf { it.gameCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            libraryRepository.ensureCatalogLoaded()
            libraryRepository.systems.collect { systems ->
                _systemsWithCounts.value = systems.map { sys ->
                    SystemWithCount(sys, libraryRepository.countGames(sys.id))
                }
            }
        }
        viewModelScope.launch {
            libraryRepository.scanProgress.collect { progress ->
                if (progress == null) reloadCounts()
            }
        }
    }

    fun reloadCounts() {
        viewModelScope.launch {
            _systemsWithCounts.value = _systemsWithCounts.value.map { row ->
                row.copy(gameCount = libraryRepository.countGames(row.system.id))
            }
        }
    }
}
