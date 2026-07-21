package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sayemshafayet.onereogamelauncher.domain.RaAchievement
import com.sayemshafayet.onereogamelauncher.domain.RaVisualState
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.GameRetroAchievementsViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.RaScreenOutcome

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameRetroAchievementsScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: GameRetroAchievementsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()

    Scaffold(
    ) { padding ->
        when {
            ui.loading -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Loading achievements…")
                }
            }
            !ui.signedIn -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Sign in required", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "ORGL shows your RetroAchievements progress here using the same account as RetroArch. " +
                            "Add your username and password under Settings. " +
                            "Achievements are still earned in RetroArch.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = onOpenSettings) {
                        Text("Open RetroAchievements settings")
                    }
                }
            }
            ui.details == null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val isError = ui.outcome == RaScreenOutcome.ERROR
                    Text(
                        if (isError) "Could not load achievements" else "Not supported",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Text(
                        ui.message ?: if (isError) {
                            "Something went wrong while contacting RetroAchievements."
                        } else {
                            "This game or ROM is not recognized on RetroAchievements."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ui.button?.subtitle?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isError) {
                        OutlinedButton(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                val details = ui.details!!
                val progress = if (details.total > 0) {
                    details.earned.toFloat() / details.total
                } else {
                    0f
                }
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(details.title, style = MaterialTheme.typography.headlineSmall)
                        details.consoleName?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${details.earned} / ${details.total} unlocked · " +
                                "${details.pointsEarned} / ${details.pointsTotal} points",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Hardcore: ${details.hardcoreEarned}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    ui.button?.takeIf { it.visual == RaVisualState.UNSUPPORTED_SETUP }?.let { button ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                ),
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "Launch setup cannot earn achievements",
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        button.subtitle,
                                        modifier = Modifier.padding(top = 4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        "Use RetroArch with a ROM hash recognized by RetroAchievements, " +
                                            "and sign in to RA inside RetroArch.",
                                        modifier = Modifier.padding(top = 8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }

                    if (details.recentUnlocks.isNotEmpty()) {
                        item {
                            Text("Recent unlocks", style = MaterialTheme.typography.titleMedium)
                            details.recentUnlocks.forEach { unlock ->
                                Text("· $unlock", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    item {
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Text("Achievements", style = MaterialTheme.typography.titleMedium)
                    }

                    items(details.achievements, key = { it.id }) { achievement ->
                        AchievementRow(achievement)
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(achievement: RaAchievement) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.earned) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = achievement.badgeUrl,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Fit,
                alpha = if (achievement.earned) 1f else 0.35f,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                if (achievement.description.isNotBlank()) {
                    Text(
                        achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    buildString {
                        append("${achievement.points} pts")
                        if (achievement.earnedHardcore) append(" · Hardcore")
                        achievement.earnedAt?.let { append(" · $it") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
