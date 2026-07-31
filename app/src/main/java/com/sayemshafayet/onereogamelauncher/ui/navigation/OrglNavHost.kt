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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sayemshafayet.onereogamelauncher.legal.LegalDocumentKind
import com.sayemshafayet.onereogamelauncher.ui.legal.LegalDocumentScreen
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
            if (currentRoute != Routes.ONBOARDING && currentRoute?.startsWith("legal/") != true) {
                navController.navigate(Routes.ONBOARDING) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    val startDestination = if (!onboardingDone) Routes.ONBOARDING else Routes.HOME

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(
            route = Routes.LEGAL_DOCUMENT,
            arguments = listOf(
                navArgument("documentKind") { type = NavType.StringType },
            ),
        ) { entry ->
            val kind = entry.arguments?.getString("documentKind").orEmpty()
            val document = when (kind) {
                "privacy" -> LegalDocumentKind.Privacy
                "terms" -> LegalDocumentKind.Terms
                else -> LegalDocumentKind.Privacy
            }
            LegalDocumentScreen(
                document = document,
                onBack = { navController.popBackStack() },
                onOpenLinkedDocument = { linked ->
                    navController.navigate(Routes.legalDocument(linked.name.lowercase())) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onOpenLegalDocument = { document ->
                    navController.navigate(Routes.legalDocument(document.name.lowercase()))
                },
            )
        }
        composable(Routes.HOME) {
            ModeShell(mainViewModel = viewModel)
        }
    }
}
