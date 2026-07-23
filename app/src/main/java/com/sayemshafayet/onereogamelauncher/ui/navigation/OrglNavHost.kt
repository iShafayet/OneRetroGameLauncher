package com.sayemshafayet.onereogamelauncher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsState()
    val onboardingDone = settings.onboardingDone
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkSafAccessOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(onboardingDone) {
        if (!onboardingDone) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != Routes.ONBOARDING) {
                navController.navigate(Routes.ONBOARDING) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (onboardingDone) Routes.HOME else Routes.ONBOARDING,
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
