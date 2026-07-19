package com.sayemshafayet.onereogamelauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StarRatingInput(
    stars: Float,
    onStarsChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            val value = i.toFloat()
            val icon = when {
                stars >= value -> Icons.Default.Star
                stars >= value - 0.5f -> Icons.Default.StarHalf
                else -> Icons.Default.StarBorder
            }
            IconButton(
                onClick = {
                    onStarsChange(
                        if (stars == value) value - 0.5f else value,
                    )
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = "$value stars",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
