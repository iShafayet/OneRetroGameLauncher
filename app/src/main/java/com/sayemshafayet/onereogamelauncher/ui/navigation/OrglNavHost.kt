package com.sayemshafayet.onereogamelauncher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sayemshafayet.onereogamelauncher.ui.onboarding.OnboardingScreen
import com.sayemshafayet.onereogamelauncher.ui.shell.ModeShell
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.MainViewModel

@Composable
fun OrglNavHost(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(Unit) { viewModel.ensureCatalog() }

    NavHost(
        navController = navController,
        startDestination = if (settings.onboardingDone) Routes.HOME else Routes.ONBOARDING,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            ModeShell(mainViewModel = viewModel)
        }
    }
}
