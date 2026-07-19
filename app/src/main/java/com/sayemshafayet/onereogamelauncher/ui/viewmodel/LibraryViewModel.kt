package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SystemWithCount(
    val system: SystemEntity,
    val gameCount: Int,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val _systemsWithCounts = MutableStateFlow<List<SystemWithCount>>(emptyList())

    val systemsWithCounts: StateFlow<List<SystemWithCount>> = _systemsWithCounts.asStateFlow()

    val visibleSystems: StateFlow<List<SystemWithCount>> = _systemsWithCounts
        .map { systems ->
            systems
                .filter { it.gameCount > 0 }
                .sortedBy { it.system.displayName.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
