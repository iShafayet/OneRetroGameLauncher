package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.EmulatorProfileEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameConfigEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.launch.LaunchResolver
import com.sayemshafayet.onereogamelauncher.play.CommitmentRepository
import com.sayemshafayet.onereogamelauncher.scrape.ScrapeForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class GameDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val commitmentRepository: CommitmentRepository,
    private val launchResolver: LaunchResolver,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val gameId: Long = savedStateHandle.get<String>("gameId")?.toLongOrNull() ?: 0L

    val game: StateFlow<GameEntity?> = libraryRepository.observeGame(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val media: StateFlow<List<MediaEntity>> = libraryRepository.observeMedia(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val config: StateFlow<GameConfigEntity?> = libraryRepository.observeGameConfig(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val emulators: StateFlow<List<EmulatorProfileEntity>> = libraryRepository.observeEmulators()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeCommitment = commitmentRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var systemCache: SystemEntity? = null

    suspend fun system(): SystemEntity? {
        systemCache?.let { return it }
        val g = libraryRepository.getGame(gameId) ?: return null
        systemCache = libraryRepository.getSystem(g.systemId)
        return systemCache
    }

    suspend fun launchAllowed(): Boolean = commitmentRepository.isLaunchAllowed(gameId)

    suspend fun launch(): String? = launchResolver.launch(gameId)

    fun toggleFavorite() {
        viewModelScope.launch { libraryRepository.toggleFavorite(gameId) }
    }

    fun toggleShelf(on: Boolean) {
        viewModelScope.launch { libraryRepository.setOnShelf(gameId, on) }
    }

    fun saveConfig(emulatorKey: String?, core: String?, configPath: String?) {
        viewModelScope.launch {
            libraryRepository.saveGameConfig(gameId, emulatorKey, core, configPath)
        }
    }

    fun saveNotes(description: String) {
        viewModelScope.launch {
            libraryRepository.updateGameMetadata(gameId, description = description)
        }
    }

    fun scrapeGame() {
        viewModelScope.launch {
            val g = libraryRepository.getGame(gameId) ?: return@launch
            val sys = libraryRepository.getSystem(g.systemId) ?: return@launch
            ScrapeForegroundService.scrapeGame(context, gameId, sys.folderName)
        }
    }
}
