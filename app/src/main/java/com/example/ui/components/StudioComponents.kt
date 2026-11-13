package com.example.ui.components

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.HotkeyBinding
import com.example.model.SwipeDirection
import com.example.model.TaggingMode
import com.example.model.keyCodeToReadableLabel
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.IvoryText
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceBright
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TealAccent
import com.example.ui.theme.VioletAccent

fun directionAccentColor(direction: SwipeDirection): Color = when (direction) {
    SwipeDirection.UP -> AmberAccent
    SwipeDirection.RIGHT -> TealAccent
    SwipeDirection.LEFT -> CoralAccent
    SwipeDirection.DOWN -> VioletAccent
}

fun directionVectorIcon(direction: SwipeDirection): ImageVector = when (direction) {
    SwipeDirection.UP -> Icons.Rounded.ArrowUpward
    SwipeDirection.DOWN -> Icons.Rounded.ArrowDownward
    SwipeDirection.LEFT -> Icons.AutoMirrored.Rounded.ArrowBack
    SwipeDirection.RIGHT -> Icons.AutoMirrored.Rounded.ArrowForward
}

@Composable
fun DeleteConfirmDialog(
    fileName: String,
    mode: TaggingMode,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteForever,
                contentDescription = "Delete image",
                tint = CoralAccent,
                modifier = Modifier.size(28.dp),
            )
        },
        title = {
            Text(
                text = "Delete image?",
                style = MaterialTheme.typography.titleLarge,
                color = IvoryText,
            )
        },
        text = {
            val sidecarNote = if (mode == TaggingMode.DATASET_SIDECAR) {
                " Its matching .txt caption file will also be removed."
            } else {
                ""
            }
            Text(
                text = "\"$fileName\" will be permanently deleted from this folder.$sidecarNote",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextSecondary,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralAccent,
                    contentColor = Color.Black,
                ),
                modifier = Modifier.testTag("confirm_delete_button"),
            ) {
                Text("Delete", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button"),
            ) {
                Text("Cancel", color = SlateTextSecondary)
            }
        },
    )
}

@Composable
fun ManualTagDialog(
    fileName: String,
    onAddTags: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        title = {
            Text(
                text = "Append Caption / Tags",
                style = MaterialTheme.typography.titleLarge,
                color = IvoryText,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "For $fileName — separate multiple tags with commas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextSecondary,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("e.g. golden hour, candid, 35mm") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_tag_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        onAddTags(input)
                    }
                    onDismiss()
                },
                enabled = input.isNotBlank(),
                modifier = Modifier.testTag("manual_tag_submit"),
            ) {
                Text("Append")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SlateTextSecondary)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickDirectionsSheet(
    directionTags: Map<SwipeDirection, String>,
    defaultDirection: SwipeDirection,
    onUpdateDirection: (SwipeDirection, String) -> Unit,
    onSelectDefaultDirection: (SwipeDirection) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val draftTags = remember(directionTags) {
        mutableStateOf(directionTags)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSurface,
        contentColor = IvoryText,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Swipe Direction Meanings",
                style = MaterialTheme.typography.headlineSmall,
                color = IvoryText,
            )
            Text(
                text = "Assign tags to each direction for the current pass. Leave blank to skip without tagging. Tap Default to set your quick-action direction.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextSecondary,
            )

            SwipeDirection.entries.forEach { dir ->
                val accent = directionAccentColor(dir)
                val value = draftTags.value[dir].orEmpty()
                val isDefault = dir == defaultDirection

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StudioSurfaceElevated,
                    border = BorderStroke(
                        width = if (isDefault) 1.5.dp else 1.dp,
                        color = if (isDefault) accent else StudioBorder,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
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
                                        contentDescription = dir.label,
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
                                onClick = { onSelectDefaultDirection(dir) },
                                label = { Text(if (isDefault) "Default" else "Set Default") },
                                leadingIcon = if (isDefault) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent.copy(alpha = 0.2f),
                                    selectedLabelColor = accent,
                                    selectedLeadingIconColor = accent,
                                ),
                            )
                        }
                        OutlinedTextField(
                            value = value,
                            onValueChange = { updated ->
                                draftTags.value = draftTags.value.toMutableMap().apply {
                                    put(dir, updated)
                                }
                                onUpdateDirection(dir, updated)
                            },
                            placeholder = { Text("Tag or leave empty to skip") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_dir_input_${dir.name.lowercase()}"),
                        )
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("quick_directions_done"),
            ) {
                Text("Done")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyCaptureDialog(
    binding: HotkeyBinding,
    onKeyCaptured: (keyCode: Int, keyLabel: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val presetKeys = remember {
        listOf(
            AndroidKeyEvent.KEYCODE_1 to "1",
            AndroidKeyEvent.KEYCODE_2 to "2",
            AndroidKeyEvent.KEYCODE_3 to "3",
            AndroidKeyEvent.KEYCODE_4 to "4",
            AndroidKeyEvent.KEYCODE_5 to "5",
            AndroidKeyEvent.KEYCODE_6 to "6",
            AndroidKeyEvent.KEYCODE_7 to "7",
            AndroidKeyEvent.KEYCODE_8 to "8",
            AndroidKeyEvent.KEYCODE_9 to "9",
            AndroidKeyEvent.KEYCODE_0 to "0",
            AndroidKeyEvent.KEYCODE_Q to "Q",
            AndroidKeyEvent.KEYCODE_W to "W",
            AndroidKeyEvent.KEYCODE_E to "E",
            AndroidKeyEvent.KEYCODE_R to "R",
            AndroidKeyEvent.KEYCODE_A to "A",
            AndroidKeyEvent.KEYCODE_S to "S",
            AndroidKeyEvent.KEYCODE_D to "D",
            AndroidKeyEvent.KEYCODE_F to "F",
            AndroidKeyEvent.KEYCODE_Z to "Z",
            AndroidKeyEvent.KEYCODE_X to "X",
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Keyboard,
                    contentDescription = null,
                    tint = AmberAccent,
                )
                Text(
                    text = "Bind Hotkey #${binding.slotIndex}",
                    style = MaterialTheme.typography.titleLarge,
                    color = IvoryText,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            val code = keyEvent.nativeKeyEvent.keyCode
                            if (code != AndroidKeyEvent.KEYCODE_BACK && code != AndroidKeyEvent.KEYCODE_ESCAPE) {
                                val label = keyCodeToReadableLabel(code)
                                onKeyCaptured(code, label)
                                return@onPreviewKeyEvent true
                            }
                        }
                        false
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioSurface,
                    border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Press any key on your attached keyboard",
                            style = MaterialTheme.typography.titleMedium,
                            color = IvoryText,
                        )
                        Text(
                            text = "Current key: [${binding.keyLabel}]",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberAccent,
                        )
                    }
                }

                Text(
                    text = "Or pick a key manually:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextSecondary,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presetKeys.forEach { (code, label) ->
                        val isCurrent = binding.keyCode == code
                        Surface(
                            onClick = { onKeyCaptured(code, label) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) AmberAccent.copy(alpha = 0.2f) else StudioSurfaceBright,
                            border = BorderStroke(
                                1.dp,
                                if (isCurrent) AmberAccent else StudioBorder,
                            ),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCurrent) AmberAccent else IvoryText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = { onKeyCaptured(AndroidKeyEvent.KEYCODE_UNKNOWN, "UNSET") },
            ) {
                Text("Clear Key")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SlateTextSecondary)
            }
        },
    )
}
