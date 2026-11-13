package com.example.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppSettings
import com.example.model.HotkeyBinding
import com.example.model.SwipeDirection
import com.example.model.TaggingMode
import com.example.ui.components.KeyCaptureDialog
import com.example.ui.components.directionAccentColor
import com.example.ui.components.directionVectorIcon
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.IvoryText
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceBright
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealAccent

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onPickDirectory: () -> Unit,
    onSelectMode: (TaggingMode) -> Unit,
    onSetRandomOrder: (Boolean) -> Unit,
    onSetConfirmBeforeDelete: (Boolean) -> Unit,
    onSetAutoAdvanceOnHotkey: (Boolean) -> Unit,
    onSetDefaultDirection: (SwipeDirection) -> Unit,
    onUpdateDirectionTag: (SwipeDirection, String) -> Unit,
    onUpdateHotkey: (HotkeyBinding) -> Unit,
    onResetDirections: () -> Unit,
    onResetHotkeys: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val hasHardwareKeyboard = configuration.keyboard != Configuration.KEYBOARD_NOKEYS
    var capturingSlot by remember { mutableStateOf<HotkeyBinding?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to workspace",
                        tint = IvoryText,
                    )
                }
                Column {
                    Text(
                        text = "Studio Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = IvoryText,
                    )
                    Text(
                        text = "Modes, swipe defaults, and 10 keyboard hotkeys",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextSecondary,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                // 1. Tagging Mode
                SettingsSection(title = "TAGGING MODE") {
                    TaggingMode.entries.forEach { mode ->
                        val isSelected = mode == settings.mode
                        val accent = if (mode == TaggingMode.DATASET_SIDECAR) AmberAccent else TealAccent
                        val icon = if (mode == TaggingMode.DATASET_SIDECAR) {
                            Icons.Rounded.Description
                        } else {
                            Icons.Rounded.FolderOpen
                        }

                        Surface(
                            onClick = { onSelectMode(mode) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) StudioSurfaceElevated else StudioSurface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accent else StudioBorder,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_mode_${mode.name.lowercase()}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(accent.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = accent,
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IvoryText,
                                    )
                                    Text(
                                        text = mode.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SlateTextSecondary,
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = accent,
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Folder & Playback Preferences
                SettingsSection(title = "FOLDER & WORKFLOW") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StudioSurface,
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Active Directory",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IvoryText,
                                    )
                                    Text(
                                        text = settings.selectedFolderName ?: "No folder selected",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AmberAccent,
                                    )
                                }
                                OutlinedButton(
                                    onClick = onPickDirectory,
                                    modifier = Modifier.testTag("settings_change_folder_button"),
                                ) {
                                    Text("Change")
                                }
                            }

                            ToggleRow(
                                title = "Random Image Order",
                                subtitle = "Shuffle image sequence on each pass",
                                checked = settings.isRandomOrder,
                                onCheckedChange = onSetRandomOrder,
                                testTag = "settings_random_order_switch",
                            )

                            ToggleRow(
                                title = "Confirm Before Delete",
                                subtitle = "Ask before permanently deleting an image",
                                checked = settings.confirmBeforeDelete,
                                onCheckedChange = onSetConfirmBeforeDelete,
                                testTag = "settings_confirm_delete_switch",
                            )

                            ToggleRow(
                                title = "Auto-Advance on Hotkey",
                                subtitle = "Move to next image immediately after pressing a hotkey",
                                checked = settings.autoAdvanceOnHotkey,
                                onCheckedChange = onSetAutoAdvanceOnHotkey,
                                testTag = "settings_hotkey_advance_switch",
                            )
                        }
                    }
                }

                // 3. Swipe Directions & Default Action
                SettingsSection(
                    title = "SWIPE DIRECTIONS & DEFAULT",
                    trailingAction = {
                        TextButton(onClick = onResetDirections) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    },
                ) {
                    SwipeDirection.entries.forEach { dir ->
                        val accent = directionAccentColor(dir)
                        val isDefault = dir == settings.defaultDirection
                        val currentTag = settings.directionTags[dir].orEmpty()

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = StudioSurface,
                            border = BorderStroke(
                                width = if (isDefault) 1.5.dp else 1.dp,
                                color = if (isDefault) accent else StudioBorder,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(accent.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = directionVectorIcon(dir),
                                                contentDescription = null,
                                                tint = accent,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                        Text(
                                            text = dir.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = IvoryText,
                                        )
                                    }

                                    FilterChip(
                                        selected = isDefault,
                                        onClick = { onSetDefaultDirection(dir) },
                                        label = { Text(if (isDefault) "Default" else "Set Default") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = accent.copy(alpha = 0.2f),
                                            selectedLabelColor = accent,
                                        ),
                                        modifier = Modifier.testTag("settings_default_dir_${dir.name.lowercase()}"),
                                    )
                                }

                                OutlinedTextField(
                                    value = currentTag,
                                    onValueChange = { onUpdateDirectionTag(dir, it) },
                                    label = { Text("Tag appended on ${dir.label.lowercase()}") },
                                    placeholder = { Text("Leave blank to advance without tagging") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_dir_tag_${dir.name.lowercase()}"),
                                )
                            }
                        }
                    }
                }

                // 4. Hardware Keyboard Hotkeys (10 Slots)
                SettingsSection(
                    title = "KEYBOARD HOTKEYS (10 SLOTS)",
                    trailingAction = {
                        TextButton(onClick = onResetHotkeys) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    },
                ) {
                    // Hardware keyboard status pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioSurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (hasHardwareKeyboard) SuccessGreen.copy(alpha = 0.55f) else StudioBorder,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Keyboard,
                                contentDescription = null,
                                tint = if (hasHardwareKeyboard) SuccessGreen else AmberAccent,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (hasHardwareKeyboard) {
                                        "Hardware Keyboard Detected"
                                    } else {
                                        "No Physical Keyboard Attached"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IvoryText,
                                )
                                Text(
                                    text = "Configure all 10 hotkeys anytime. Arrow keys also trigger Up/Down/Left/Right swipes and Space triggers Default.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateTextSecondary,
                                )
                            }
                        }
                    }

                    settings.hotkeys.sortedBy { it.slotIndex }.forEach { hk ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = StudioSurface,
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "#${hk.slotIndex.toString().padStart(2, '0')}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SlateTextMuted,
                                )

                                // Keycap button -> opens key capture dialog
                                Surface(
                                    onClick = { capturingSlot = hk },
                                    shape = RoundedCornerShape(10.dp),
                                    color = StudioSurfaceBright,
                                    border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier.testTag("hotkey_keycap_${hk.slotIndex}"),
                                ) {
                                    Text(
                                        text = hk.keyLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AmberAccent,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    )
                                }

                                OutlinedTextField(
                                    value = hk.tag,
                                    onValueChange = { updatedTag ->
                                        onUpdateHotkey(hk.copy(tag = updatedTag))
                                    },
                                    placeholder = { Text("Tag for hotkey #${hk.slotIndex}") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("hotkey_tag_input_${hk.slotIndex}"),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    capturingSlot?.let { binding ->
        KeyCaptureDialog(
            binding = binding,
            onKeyCaptured = { code, label ->
                onUpdateHotkey(binding.copy(keyCode = code, keyLabel = label))
                capturingSlot = null
            },
            onDismiss = { capturingSlot = null },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = SlateTextSecondary,
            )
            trailingAction?.invoke()
        }
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = IvoryText,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextSecondary,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}
