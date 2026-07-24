package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglExternalSync
import com.sayemshafayet.onereogamelauncher.data.prefs.AppSettings
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.ra.RetroAchievementsClient
import com.sayemshafayet.onereogamelauncher.ui.input.orlgDpadFocusExit
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RaSettingsSaveState {
    data object Idle : RaSettingsSaveState
    data object Saving : RaSettingsSaveState
    data object Success : RaSettingsSaveState
    data class Error(val message: String) : RaSettingsSaveState
}

@HiltViewModel
class RetroAchievementsSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val raClient: RetroAchievementsClient,
    private val orglExternalSync: OrglExternalSync,
) : ViewModel() {
    val settings = settingsRepository.settings

    private val _saveState = MutableStateFlow<RaSettingsSaveState>(RaSettingsSaveState.Idle)
    val saveState = _saveState.asStateFlow()

    private val _diskLoaded = MutableStateFlow(false)
    val diskLoaded = _diskLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { orglExternalSync.loadCredentialsFromDisk() }
                .onSuccess { loaded -> _diskLoaded.value = loaded }
        }
    }

    fun save(user: String, password: String) {
        viewModelScope.launch {
            _saveState.value = RaSettingsSaveState.Saving
            raClient.verifyCredentials(user, password).fold(
                onSuccess = {
                    settingsRepository.setRetroAchievements(user.trim(), password.trim())
                    runCatching { orglExternalSync.saveCredentialsToDiskIfAllowed() }
                    _saveState.value = RaSettingsSaveState.Success
                },
                onFailure = { error ->
                    _saveState.value = RaSettingsSaveState.Error(
                        error.message ?: "Invalid username or password",
                    )
                },
            )
        }
    }

    fun setStoreOnDisk(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRetroAchievementsStoreOnDisk(enabled)
            if (enabled) {
                runCatching { orglExternalSync.saveCredentialsToDiskIfAllowed() }
            } else {
                runCatching { orglExternalSync.clearCredentialsOnDisk() }
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = RaSettingsSaveState.Idle
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroAchievementsSettingsScreen(
    onBack: () -> Unit,
    viewModel: RetroAchievementsSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState(initial = AppSettings())
    val saveState by viewModel.saveState.collectAsState()
    val diskLoaded by viewModel.diskLoaded.collectAsState()
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(settings) {
        user = settings.retroAchievementsUser
        password = settings.retroAchievementsPassword
    }

    Scaffold(
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                "Use the same username and password as RetroArch.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                "ORGL reads your achievement progress only. Unlocks still happen in RetroArch.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (diskLoaded) {
                Text(
                    "Loaded credentials from the ORGL data folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                user,
                {
                    user = it
                    viewModel.clearSaveState()
                },
                label = { Text("Username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .orlgDpadFocusExit(),
                singleLine = true,
                enabled = saveState !is RaSettingsSaveState.Saving,
            )
            OutlinedTextField(
                password,
                {
                    password = it
                    viewModel.clearSaveState()
                },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .orlgDpadFocusExit(),
                singleLine = true,
                enabled = saveState !is RaSettingsSaveState.Saving,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Store on disk")
                    Text(
                        "Save credentials into the ORGL data folder (encrypted ra_credentials.json).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.retroAchievementsStoreOnDisk,
                    onCheckedChange = viewModel::setStoreOnDisk,
                    enabled = saveState !is RaSettingsSaveState.Saving,
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.save(user, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState !is RaSettingsSaveState.Saving &&
                    user.isNotBlank() &&
                    password.isNotBlank(),
            ) {
                if (saveState is RaSettingsSaveState.Saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Save & verify")
                }
            }
            when (val state = saveState) {
                RaSettingsSaveState.Success -> {
                    Text(
                        "Connected. Your achievement progress will appear when you open a game.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                is RaSettingsSaveState.Error -> {
                    Text(
                        state.message,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> Unit
            }
        }
    }
}
