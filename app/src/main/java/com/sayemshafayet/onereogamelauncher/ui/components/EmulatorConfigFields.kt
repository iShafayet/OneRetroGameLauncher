package com.sayemshafayet.onereogamelauncher.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sayemshafayet.onereogamelauncher.ui.input.orlgDpadFocusExit
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.CoreChoice
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.EmulatorChoice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulatorDropdown(
    choices: List<EmulatorChoice>,
    selectedKey: String,
    onSelected: (String) -> Unit,
    enabled: Boolean = true,
    label: String = "Emulator",
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = choices.firstOrNull { it.key.equals(selectedKey, ignoreCase = true) }
    val display = when {
        selected != null -> {
            if (selected.installed) selected.label else "${selected.label} (not installed)"
        }
        selectedKey.isNotBlank() -> selectedKey
        else -> "Select emulator"
    }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && enabled) },
            modifier = Modifier
                .fillMaxWidth()
                .orlgDpadFocusExit(enabled = !(expanded && enabled))
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
        ) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (choice.installed) choice.label
                            else "${choice.label} (not installed)",
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(choice.key)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreDropdown(
    choices: List<CoreChoice>,
    selectedCore: String,
    onSelected: (String) -> Unit,
    enabled: Boolean = true,
    label: String = "RetroArch core",
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = choices.firstOrNull { it.fileName == selectedCore }
    val display = when {
        selected != null -> "${selected.label} (${selected.fileName})"
        selectedCore.isNotBlank() -> selectedCore
        else -> "Select core"
    }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && enabled) },
            modifier = Modifier
                .fillMaxWidth()
                .orlgDpadFocusExit(enabled = !(expanded && enabled))
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
        ) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text("${choice.label} — ${choice.fileName}") },
                    onClick = {
                        expanded = false
                        onSelected(choice.fileName)
                    },
                )
            }
        }
    }
}
