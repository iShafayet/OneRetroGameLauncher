package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.GameListLayout
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.VirtualLibrarySystem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val systemsWithCounts: StateFlow<List<SystemWithCount>> = combine(
        libraryRepository.systems,
        libraryRepository.observeGameCountsBySystem(),
    ) { systems, counts ->
        val countBySystem = counts.associate { it.systemId to it.gameCount }
        systems.map { sys ->
            SystemWithCount(sys, countBySystem[sys.id] ?: 0)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val visibleSystems: StateFlow<List<LibrarySystemRow>> = combine(
        systemsWithCounts,
        settingsRepository.settings,
        libraryRepository.observeFavorites().map { it.size },
        libraryRepository.observeWishlist().map { it.size },
        libraryRepository.observeRecent().map { it.size },
    ) { systems, settings, favoriteCount, wishlistCount, recentCount ->
        buildList {
            if (settings.libraryShowFavorites) {
                add(LibrarySystemRow.Virtual(VirtualLibrarySystem.FAVORITES, favoriteCount))
            }
            if (settings.libraryShowWishlist) {
                add(LibrarySystemRow.Virtual(VirtualLibrarySystem.WISHLIST, wishlistCount))
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

    val totalGames: StateFlow<Int> = systemsWithCounts
        .map { list -> list.sumOf { it.gameCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Unset until the user toggles; UI applies form-factor default while null. */
    val systemsLayoutPreference: StateFlow<GameListLayout?> = settingsRepository.settings
        .map { it.librarySystemsLayout }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleSystemsLayout(currentEffective: GameListLayout) {
        viewModelScope.launch {
            val next = if (currentEffective == GameListLayout.GRID) {
                GameListLayout.LIST
            } else {
                GameListLayout.GRID
            }
            settingsRepository.setLibrarySystemsLayout(next)
        }
    }

    init {
        viewModelScope.launch {
            libraryRepository.ensureCatalogLoaded()
        }
    }
}
