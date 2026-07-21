package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.launch.EmulatorLauncher
import com.sayemshafayet.onereogamelauncher.systems.SystemConfigLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmulatorChoice(
    val key: String,
    val label: String,
    val installed: Boolean,
)

data class CoreChoice(
    val fileName: String,
    val label: String,
)

data class SystemEmulatorUi(
    val system: SystemEntity? = null,
    val emulatorKey: String = "",
    val core: String = "",
    val emulatorChoices: List<EmulatorChoice> = emptyList(),
    val coreChoices: List<CoreChoice> = emptyList(),
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SystemEmulatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val systemConfigLoader: SystemConfigLoader,
    private val emulatorLauncher: EmulatorLauncher,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val systemId: Long = savedStateHandle.get<String>("systemId")?.toLongOrNull() ?: 0L

    private val _ui = MutableStateFlow(SystemEmulatorUi())
    val ui: StateFlow<SystemEmulatorUi> = _ui.asStateFlow()

    val system: StateFlow<SystemEntity?> = libraryRepository.systems.map { list ->
        list.firstOrNull { it.id == systemId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { reload() }
    }

    fun setEmulatorKey(key: String) {
        _ui.update {
            it.copy(
                emulatorKey = key,
                saved = false,
                error = null,
                core = if (key.equals("RETROARCH", ignoreCase = true)) it.core else "",
            )
        }
    }

    fun setCore(core: String) {
        _ui.update { it.copy(core = core, saved = false, error = null) }
    }

    fun save() {
        viewModelScope.launch {
            val state = _ui.value
            val key = state.emulatorKey.trim().ifBlank { null }
            if (key == null) {
                _ui.update { it.copy(error = "Select an emulator") }
                return@launch
            }
            val core = state.core.trim().ifBlank { null }
                ?.takeIf { key.equals("RETROARCH", ignoreCase = true) }
            libraryRepository.updateSystemDefaults(systemId, key, core)
            reload()
            _ui.update { it.copy(saved = true, error = null) }
        }
    }

    private suspend fun reload() {
        val sys = libraryRepository.getSystem(systemId) ?: return
        val settings = settingsRepository.current()
        val def = systemConfigLoader.systemByFolder(sys.folderName)
        val fromCommands = def?.let { systemConfigLoader.emulatorOptionsForSystem(it) }.orEmpty()
        val choices = buildEmulatorChoices(fromCommands, settings.preferredRetroArchPackage)
        val cores = def?.let { systemConfigLoader.retroArchCoresForSystem(it) }
            .orEmpty()
            .map { (label, file) -> CoreChoice(fileName = file, label = label) }

        val selectedKey = sys.defaultEmulatorKey?.takeIf { it.isNotBlank() }
            ?: choices.firstOrNull { it.installed }?.key
            ?: choices.firstOrNull()?.key
            ?: ""
        val selectedCore = usableCoreFile(sys.defaultCore)
            ?: cores.firstOrNull()?.fileName
            ?: ""

        _ui.value = SystemEmulatorUi(
            system = sys,
            emulatorKey = selectedKey,
            core = selectedCore,
            emulatorChoices = choices,
            coreChoices = cores,
        )
    }

    private fun usableCoreFile(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val normalized = com.sayemshafayet.onereogamelauncher.systems.LibretroCorePaths.coreFileNameFromExtra(raw)
        return normalized.takeIf { it.endsWith(".so", ignoreCase = true) }
    }

    private fun buildEmulatorChoices(
        catalog: List<Pair<String, String>>,
        preferredRa: String,
    ): List<EmulatorChoice> {
        val byKey = linkedMapOf<String, EmulatorChoice>()

        fun add(key: String, labelHint: String) {
            val canonical = when {
                key.equals("RETROARCH", ignoreCase = true) -> "RETROARCH"
                else -> emulatorLauncher.profileForKey(key)?.key ?: return
            }
            if (byKey.containsKey(canonical)) return
            val label = when (canonical) {
                "RETROARCH" -> "RetroArch"
                else -> emulatorLauncher.profileForKey(canonical)?.displayName ?: labelHint.ifBlank { canonical }
            }
            val installed = emulatorLauncher.installedForKey(canonical, preferredRa) != null
            byKey[canonical] = EmulatorChoice(canonical, label, installed)
        }

        for ((key, label) in catalog) add(key, label)
        add("RETROARCH", "RetroArch")
        for (profile in EmulatorLauncher.SUPPORTED_PROFILES) {
            if (emulatorLauncher.installedForKey(profile.key, preferredRa) != null) {
                add(profile.key, profile.displayName)
            }
        }
        return byKey.values.sortedWith(
            compareByDescending<EmulatorChoice> { it.installed }.thenBy { it.label.lowercase() },
        )
    }
}
