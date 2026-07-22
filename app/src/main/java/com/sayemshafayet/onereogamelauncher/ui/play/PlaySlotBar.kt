package com.sayemshafayet.onereogamelauncher.ui.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PlaySlotBar(
    slotCount: Int,
    currentSlot: Int,
    occupiedSlots: Set<Int>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 1.dp) {
        ColumnWithDivider {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.focusProperties { canFocus = false },
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous slot")
                }
                ColumnCentered(
                    currentSlot = currentSlot,
                    slotCount = slotCount,
                    occupied = currentSlot in occupiedSlots,
                )
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.focusProperties { canFocus = false },
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next slot")
                }
            }
        }
    }
}

@Composable
private fun ColumnWithDivider(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column {
        content()
        HorizontalDivider()
    }
}

@Composable
private fun ColumnCentered(
    currentSlot: Int,
    slotCount: Int,
    occupied: Boolean,
) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            "Slot ${currentSlot + 1} of $slotCount",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            if (occupied) "In progress" else "Empty",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
