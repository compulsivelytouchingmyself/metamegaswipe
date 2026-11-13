package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.TaggingMode
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.IvoryText
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TealAccent

@Composable
fun OnboardingEmptyView(
    emptyFolderName: String?,
    selectedMode: TaggingMode,
    onSelectMode: (TaggingMode) -> Unit,
    onPickDirectory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianCanvas, Color(0xFF12151F), ObsidianCanvas),
                ),
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmberAccent.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.4f)),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "TagSwipe",
                            style = MaterialTheme.typography.headlineSmall,
                            color = IvoryText,
                        )
                        Text(
                            text = "FULL-SCREEN DATASET & METADATA STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateTextSecondary,
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("onboarding_settings_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Open Settings",
                        tint = IvoryText,
                    )
                }
            }

            // Hero Artwork Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = StudioSurface,
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero),
                        contentDescription = "Darkroom studio workflow illustration",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        ObsidianCanvas.copy(alpha = 0.85f),
                                    ),
                                ),
                            ),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = if (emptyFolderName != null) {
                                "No images found in \"$emptyFolderName\""
                            } else {
                                "Swipe-speed image captioning"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = IvoryText,
                        )
                        Text(
                            text = if (emptyFolderName != null) {
                                "Pick another folder containing JPG, PNG, WebP, or HEIC images."
                            } else {
                                "Swipe Up, Down, Left, or Right to append tags — sets loop automatically for multi-pass tagging."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateTextSecondary,
                        )
                    }
                }
            }

            // Mode Quick Selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "CHOOSE TAGGING MODE",
                    style = MaterialTheme.typography.labelMedium,
                    color = SlateTextSecondary,
                )

                TaggingMode.entries.forEach { mode ->
                    val isSelected = mode == selectedMode
                    val accent = if (mode == TaggingMode.DATASET_SIDECAR) AmberAccent else TealAccent
                    val icon = if (mode == TaggingMode.DATASET_SIDECAR) {
                        Icons.Rounded.Description
                    } else {
                        Icons.Rounded.FolderOpen
                    }

                    Surface(
                        onClick = { onSelectMode(mode) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) StudioSurfaceElevated else StudioSurface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accent else StudioBorder,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_mode_${mode.name.lowercase()}"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IvoryText,
                                    )
                                }
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateTextSecondary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            Button(
                onClick = onPickDirectory,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberAccent,
                    contentColor = ObsidianCanvas,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("select_directory_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CreateNewFolder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (emptyFolderName != null) "Choose Different Directory" else "Select Image Directory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
