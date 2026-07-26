package com.sayemshafayet.onereogamelauncher.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.ui.theme.SelectionIndicator

/** Tab strip — touch selects a tab; cycle with L1 / R1 via [onPreviewKeyEvent] on a parent. */
@Composable
fun OrglTabStrip(
    labels: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedColor = SelectionIndicator
    val unselected = MaterialTheme.colorScheme.onSurfaceVariant
    val showHints = rememberShowGamepadHints()
    Column(
        modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showHints) {
                GamepadHintBadge("L1", compact = true, modifier = Modifier.padding(end = 4.dp))
            }
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    Modifier
                        .weight(1f)
                        .focusProperties { canFocus = false }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(index) },
                        )
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            unselected
                        },
                    )
                }
            }
            if (showHints) {
                GamepadHintBadge("R1", compact = true, modifier = Modifier.padding(start = 4.dp))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, _ ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(if (index == selectedIndex) selectedColor else Color.Transparent),
                )
            }
        }
        HorizontalDivider()
    }
}

/** Bottom nav display — touch works; excluded from D-pad focus (use L1 / R1). */
@Composable
fun OrglBottomNavStrip(
    labels: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    icons: @Composable (index: Int, selected: Boolean) -> Unit,
    onTabClick: (Int) -> Unit,
) {
    val selectedColor = SelectionIndicator
    val unselected = MaterialTheme.colorScheme.onSurfaceVariant
    val showHints = rememberShowGamepadHints()
    Surface(
        modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false },
        tonalElevation = 3.dp,
    ) {
        Column(Modifier.focusProperties { canFocus = false }) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .focusProperties { canFocus = false },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showHints) {
                    GamepadHintBadge("L1", compact = true, modifier = Modifier.padding(end = 4.dp))
                }
                labels.forEachIndexed { index, label ->
                    val selected = index == selectedIndex
                    Column(
                        Modifier
                            .weight(1f)
                            .focusProperties { canFocus = false }
                            .clickable(onClick = { onTabClick(index) })
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.focusProperties { canFocus = false }) {
                            icons(index, selected)
                        }
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                unselected
                            },
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .focusProperties { canFocus = false },
                        )
                    }
                }
                if (showHints) {
                    GamepadHintBadge("R1", compact = true, modifier = Modifier.padding(start = 4.dp))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                labels.forEachIndexed { index, _ ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                if (index == selectedIndex) selectedColor else Color.Transparent,
                            ),
                    )
                }
            }
        }
    }
}

fun cycleTabIndex(current: Int, delta: Int, tabCount: Int): Int {
    if (tabCount <= 0) return current
    return ((current + delta) % tabCount + tabCount) % tabCount
}
