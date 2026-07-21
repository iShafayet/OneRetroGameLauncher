package com.sayemshafayet.onereogamelauncher.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.ui.theme.AmberAccent
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont
import com.sayemshafayet.onereogamelauncher.ui.theme.InkDeep
import com.sayemshafayet.onereogamelauncher.ui.theme.InkLight
import com.sayemshafayet.onereogamelauncher.ui.theme.InkMid
import com.sayemshafayet.onereogamelauncher.ui.theme.Mist

@Composable
fun OrglSplashScreen(
    statusMessage: String = "Loading…",
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(InkDeep, InkMid, InkLight),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                "ORGL",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = BrandFont),
                color = AmberAccent,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "One Retro\nGame Launcher",
                style = MaterialTheme.typography.displayMedium.copy(fontFamily = BrandFont),
                color = Mist,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Stop scrolling. Start finishing.",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = BrandFont),
                color = AmberAccent,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = AmberAccent)
            Spacer(Modifier.height(12.dp))
            Text(
                statusMessage,
                color = Mist.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
