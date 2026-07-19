package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ScreenScraperSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val settings = settingsRepository.settings

    fun save(user: String, pass: String, devid: String, devpass: String) {
        viewModelScope.launch {
            settingsRepository.setScreenScraper(user.trim(), pass, devid.trim(), devpass)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScraperSettingsScreen(
    onBack: () -> Unit,
    viewModel: ScreenScraperSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState(
        initial = com.sayemshafayet.onereogamelauncher.data.prefs.AppSettings(),
    )
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var devid by remember { mutableStateOf("") }
    var devpass by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        user = settings.screenScraperUser
        pass = settings.screenScraperPass
        devid = settings.screenScraperDevid
        devpass = settings.screenScraperDevpassword
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ScreenScraper") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                "Primary artwork scraper. Media is written under your ORGL data folder’s downloaded_media/. ES-DE is never modified.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(user, { user = it; saved = false }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                pass,
                { pass = it; saved = false },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(devid, { devid = it; saved = false }, label = { Text("Dev ID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                devpass,
                { devpass = it; saved = false },
                label = { Text("Dev password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.save(user, pass, devid, devpass)
                    saved = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
            if (saved) {
                Text("Saved.", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
