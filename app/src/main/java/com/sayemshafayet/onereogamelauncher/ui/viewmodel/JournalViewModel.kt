package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.dao.JournalEntryRow
import com.sayemshafayet.onereogamelauncher.play.CommitmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class JournalViewModel @Inject constructor(
    commitmentRepository: CommitmentRepository,
) : ViewModel() {
    val entries: StateFlow<List<JournalEntryRow>> = commitmentRepository.observeJournal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
