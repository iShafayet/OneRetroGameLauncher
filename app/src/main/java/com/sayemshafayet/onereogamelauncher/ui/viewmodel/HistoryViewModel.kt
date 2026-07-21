package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.dao.JournalEntryRow
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.play.CommitmentRepository
import com.sayemshafayet.onereogamelauncher.ui.components.pickBoxArt
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HistoryEntryUi(
    val entry: JournalEntryRow,
    val boxArtPath: String?,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val _entries = MutableStateFlow<List<HistoryEntryUi>>(emptyList())
    val entries: StateFlow<List<HistoryEntryUi>> = _entries.asStateFlow()

    init {
        viewModelScope.launch {
            commitmentRepository.observeJournal().collect { rows ->
                _entries.value = rows.map { row ->
                    val media = libraryRepository.observeMedia(row.gameId).first()
                    HistoryEntryUi(
                        entry = row,
                        boxArtPath = pickBoxArt(media.associate { it.type to it.path }),
                    )
                }
            }
        }
    }
}
