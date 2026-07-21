package com.sayemshafayet.onereogamelauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.AppMode
import com.sayemshafayet.onereogamelauncher.ui.navigation.OrglNavHost
import com.sayemshafayet.onereogamelauncher.ui.splash.OrglSplashScreen
import com.sayemshafayet.onereogamelauncher.ui.theme.OrglTheme
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsState()
            val startupReady by viewModel.startupReady.collectAsState()

            if (!startupReady) {
                // Outside OrglTheme so light/dynamic Material colors never flash first.
                OrglSplashScreen()
            } else {
                OrglTheme(
                    themeMode = settings.themeMode,
                    isPlayMode = settings.appMode == AppMode.PLAY,
                ) {
                    OrglNavHost(viewModel = viewModel)
                }
            }
        }
    }
}
