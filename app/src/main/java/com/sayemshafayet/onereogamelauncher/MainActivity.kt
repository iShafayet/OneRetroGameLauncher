package com.sayemshafayet.onereogamelauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.ui.navigation.OrglNavHost
import com.sayemshafayet.onereogamelauncher.ui.theme.OrglTheme
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsState()
            OrglTheme(
                themeMode = settings.themeMode,
                isPlayMode = settings.appMode == com.sayemshafayet.onereogamelauncher.domain.AppMode.PLAY,
            ) {
                OrglNavHost(viewModel = viewModel)
            }
        }
    }
}
