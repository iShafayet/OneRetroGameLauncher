package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.dao.JournalEntryRow
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglExternalSync
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.play.CommitmentRepository
import com.sayemshafayet.onereogamelauncher.ui.components.pickBoxArt
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HistoryEntryUi(
    val entry: JournalEntryRow,
    val boxArtPath: String?,
)

data class HistoryUiState(
    val entries: List<HistoryEntryUi> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val libraryRepository: LibraryRepository,
    private val orglExternalSync: OrglExternalSync,
) : ViewModel() {
    private val _ui = MutableStateFlow(HistoryUiState())
    val ui: StateFlow<HistoryUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            reloadFromDisk()
            commitmentRepository.observeJournal().collect {
                reloadFromDatabase()
            }
        }
    }

    private suspend fun reloadFromDisk() {
        _ui.update { it.copy(loading = true) }
        withContext(Dispatchers.IO) {
            runCatching { orglExternalSync.importPlayHistory() }
        }
        reloadFromDatabase()
    }

    private suspend fun reloadFromDatabase() {
        val entries = withContext(Dispatchers.IO) {
            commitmentRepository.getJournal().map { row ->
                val media = libraryRepository.mediaForGame(row.gameId)
                HistoryEntryUi(
                    entry = row,
                    boxArtPath = pickBoxArt(media.associate { it.type to it.path }),
                )
            }
        }
        _ui.update { it.copy(entries = entries, loading = false) }
    }
}
