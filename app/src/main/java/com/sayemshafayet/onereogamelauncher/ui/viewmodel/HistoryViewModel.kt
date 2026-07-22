package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.dao.JournalEntryRow
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglExternalSync
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
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
    val orglConfigured: Boolean = false,
    val loading: Boolean = true,
    val syncing: Boolean = false,
    val syncMessage: String? = null,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val libraryRepository: LibraryRepository,
    private val orglExternalSync: OrglExternalSync,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(HistoryUiState())
    val ui: StateFlow<HistoryUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _ui.update {
                    it.copy(orglConfigured = !s.orglDataDirUri.isNullOrBlank())
                }
            }
        }
        viewModelScope.launch {
            reloadFromDisk()
            commitmentRepository.observeJournal().collect {
                reloadFromDatabase()
            }
        }
    }

    fun syncWithDisk() {
        viewModelScope.launch {
            _ui.update { it.copy(syncing = true, syncMessage = null) }
            val result = runCatching {
                withContext(Dispatchers.IO) { orglExternalSync.syncPlayHistory() }
            }
            reloadFromDatabase()
            _ui.update {
                it.copy(
                    syncing = false,
                    syncMessage = result.fold(
                        onSuccess = { sync -> sync.message },
                        onFailure = { e -> e.message ?: "Sync failed" },
                    ),
                )
            }
        }
    }

    private suspend fun reloadFromDisk() {
        _ui.update { it.copy(loading = true, syncMessage = null) }
        val importResult = withContext(Dispatchers.IO) {
            runCatching { orglExternalSync.importPlayHistory() }
        }
        reloadFromDatabase()
        val loadMessage = importResult.fold(
            onSuccess = { result -> result.userMessage() },
            onFailure = { e -> e.message ?: "Could not read journal from disk." },
        )
        _ui.update { it.copy(loading = false, syncMessage = loadMessage) }
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
