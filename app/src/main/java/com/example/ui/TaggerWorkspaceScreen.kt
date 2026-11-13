package com.example.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.SwipeDirection
import com.example.model.TaggingMode
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.ManualTagDialog
import com.example.ui.components.QuickDirectionsSheet
import com.example.ui.components.directionAccentColor
import com.example.ui.components.directionVectorIcon
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.IvoryText
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TealAccent
import com.example.ui.theme.VioletAccent
import com.example.viewmodel.TagSwipeUiState
import com.example.viewmodel.WorkspaceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun TaggerWorkspaceScreen(
    uiState: TagSwipeUiState,
    onPickDirectory: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwipeDirection: (SwipeDirection) -> Unit,
    onTriggerDefaultSwipe: () -> Unit,
    onPreviousImage: () -> Unit,
    onToggleRandomOrder: () -> Unit,
    onSelectMode: (TaggingMode) -> Unit,
    onUpdateDirectionTag: (SwipeDirection, String) -> Unit,
    onSelectDefaultDirection: (SwipeDirection) -> Unit,
    onAddManualTags: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onRequestDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onHardwareKeyDown: (Int) -> Boolean,
    onDismissBanner: () -> Unit,
) {
    when (val ws = uiState.workspace) {
        WorkspaceState.NoFolder -> {
            OnboardingEmptyView(
                emptyFolderName = null,
                selectedMode = uiState.settings.mode,
                onSelectMode = onSelectMode,
                onPickDirectory = onPickDirectory,
                onOpenSettings = onOpenSettings,
            )
        }
        WorkspaceState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianCanvas),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircularProgressIndicator(color = AmberAccent)
                    Text(
                        text = "Scanning folder images...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextSecondary,
                    )
                }
            }
        }
        is WorkspaceState.EmptyFolder -> {
            OnboardingEmptyView(
                emptyFolderName = ws.folderName,
                selectedMode = uiState.settings.mode,
                onSelectMode = onSelectMode,
                onPickDirectory = onPickDirectory,
                onOpenSettings = onOpenSettings,
            )
        }
        is WorkspaceState.Ready -> {
            ActiveCanvasWorkspace(
                uiState = uiState,
                ready = ws,
                onPickDirectory = onPickDirectory,
                onOpenSettings = onOpenSettings,
                onSwipeDirection = onSwipeDirection,
                onTriggerDefaultSwipe = onTriggerDefaultSwipe,
                onPreviousImage = onPreviousImage,
                onToggleRandomOrder = onToggleRandomOrder,
                onUpdateDirectionTag = onUpdateDirectionTag,
                onSelectDefaultDirection = onSelectDefaultDirection,
                onAddManualTags = onAddManualTags,
                onRemoveTag = onRemoveTag,
                onRequestDelete = onRequestDelete,
                onConfirmDelete = onConfirmDelete,
                onCancelDelete = onCancelDelete,
                onHardwareKeyDown = onHardwareKeyDown,
                onDismissBanner = onDismissBanner,
            )
        }
    }
}

@Composable
private fun ActiveCanvasWorkspace(
    uiState: TagSwipeUiState,
    ready: WorkspaceState.Ready,
    onPickDirectory: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwipeDirection: (SwipeDirection) -> Unit,
    onTriggerDefaultSwipe: () -> Unit,
    onPreviousImage: () -> Unit,
    onToggleRandomOrder: () -> Unit,
    onUpdateDirectionTag: (SwipeDirection, String) -> Unit,
    onSelectDefaultDirection: (SwipeDirection) -> Unit,
    onAddManualTags: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onRequestDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onHardwareKeyDown: (Int) -> Boolean,
    onDismissBanner: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val hasHardwareKeyboard = configuration.keyboard != Configuration.KEYBOARD_NOKEYS

    var showManualTagDialog by remember { mutableStateOf(false) }
    var showDirectionsSheet by remember { mutableStateOf(false) }
    var showHotkeyStrip by remember(hasHardwareKeyboard) { mutableStateOf(hasHardwareKeyboard) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(showManualTagDialog, showDirectionsSheet, uiState.showDeleteConfirm) {
        if (!showManualTagDialog && !showDirectionsSheet && !uiState.showDeleteConfirm) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    // Auto-hide feedback banner after 2.6 seconds
    LaunchedEffect(uiState.feedbackBanner) {
        if (uiState.feedbackBanner != null) {
            delay(2600)
            onDismissBanner()
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val swipeThresholdPx = with(LocalDensity.current) { 110.dp.toPx() }

    LaunchedEffect(ready.currentItem.uri, ready.currentIndex, ready.passCount) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
    }

    val activeDragDirection by remember {
        derivedStateOf {
            val dx = offsetX.value
            val dy = offsetY.value
            val mag = max(abs(dx), abs(dy))
            if (mag < swipeThresholdPx * 0.22f) {
                null
            } else if (abs(dx) > abs(dy)) {
                if (dx > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
            } else {
                if (dy < 0) SwipeDirection.UP else SwipeDirection.DOWN
            }
        }
    }

    val dragProgress by remember {
        derivedStateOf {
            val mag = max(abs(offsetX.value), abs(offsetY.value))
            (mag / swipeThresholdPx).coerceIn(0f, 1f)
        }
    }

    val currentItem = ready.currentItem
    val settings = uiState.settings

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (!showManualTagDialog && !showDirectionsSheet && !uiState.showDeleteConfirm &&
                    keyEvent.type == KeyEventType.KeyDown
                ) {
                    onHardwareKeyDown(keyEvent.nativeKeyEvent.keyCode)
                } else {
                    false
                }
            },
    ) {
        val isCompactWidth = maxWidth < 420.dp

        // 1. Full-screen swipeable image stage
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentItem.uri, ready.currentIndex, ready.passCount) {
                    detectTapGestures(
                        onDoubleTap = { onTriggerDefaultSwipe() },
                    )
                }
                .pointerInput(currentItem.uri, ready.currentIndex, ready.passCount) {
                    detectDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val dx = offsetX.value
                                val dy = offsetY.value
                                val mag = max(abs(dx), abs(dy))
                                if (mag >= swipeThresholdPx) {
                                    val dir = if (abs(dx) > abs(dy)) {
                                        if (dx > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
                                    } else {
                                        if (dy < 0) SwipeDirection.UP else SwipeDirection.DOWN
                                    }
                                    val targetX = when (dir) {
                                        SwipeDirection.LEFT -> -1400f
                                        SwipeDirection.RIGHT -> 1400f
                                        else -> dx * 1.4f
                                    }
                                    val targetY = when (dir) {
                                        SwipeDirection.UP -> -1600f
                                        SwipeDirection.DOWN -> 1600f
                                        else -> dy * 1.4f
                                    }
                                    launch { offsetX.animateTo(targetX, tween(140)) }
                                    offsetY.animateTo(targetY, tween(140))
                                    onSwipeDirection(dir)
                                } else {
                                    launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                                    offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                launch { offsetX.animateTo(0f, spring()) }
                                offsetY.animateTo(0f, spring())
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                offsetY.snapTo(offsetY.value + dragAmount.y)
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentItem.uri)
                    .crossfade(120)
                    .build(),
                contentDescription = currentItem.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt())
                    }
                    .graphicsLayer {
                        rotationZ = (offsetX.value / 45f).coerceIn(-10f, 10f)
                        val scale = 1f - (dragProgress * 0.04f)
                        scaleX = scale
                        scaleY = scale
                    }
                    .testTag("fullscreen_image_canvas"),
            )

            // Directional swipe HUD reticle
            activeDragDirection?.let { dir ->
                val accent = directionAccentColor(dir)
                val tag = settings.directionTags[dir].orEmpty().trim()
                val label = if (tag.isNotEmpty()) "+ $tag" else "Next (skip)"

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ObsidianCanvas.copy(alpha = 0.86f),
                    border = BorderStroke(2.dp, accent.copy(alpha = (0.4f + dragProgress * 0.6f).coerceIn(0f, 1f))),
                    tonalElevation = 8.dp,
                    modifier = Modifier.graphicsLayer {
                        alpha = (0.35f + dragProgress * 0.65f).coerceIn(0f, 1f)
                        val s = 0.9f + dragProgress * 0.14f
                        scaleX = s
                        scaleY = s
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = directionVectorIcon(dir),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(26.dp),
                        )
                        Column {
                            Text(
                                text = dir.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.headlineSmall,
                                color = IvoryText,
                            )
                        }
                    }
                }
            }
        }

        // 2. Top edge gradient scrim for legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianCanvas.copy(alpha = 0.82f),
                            ObsidianCanvas.copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // 3. Bottom edge gradient scrim for legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ObsidianCanvas.copy(alpha = 0.68f),
                            ObsidianCanvas.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        )

        // 4. Top Translucent HUD Bar (with small Delete button in top right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left telemetry cluster: folder + pass/index + mode badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    onClick = onPickDirectory,
                    shape = RoundedCornerShape(12.dp),
                    color = StudioSurface.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.8f)),
                    modifier = Modifier.testTag("change_folder_pill"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = "Select folder",
                            tint = AmberAccent,
                            modifier = Modifier.size(16.dp),
                        )
                        if (!isCompactWidth) {
                            Text(
                                text = ready.folderName,
                                style = MaterialTheme.typography.labelLarge,
                                color = IvoryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = "P${ready.passCount} · ${ready.currentIndex + 1}/${ready.items.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SlateTextSecondary,
                        )
                    }
                }

                val modeColor = if (settings.mode == TaggingMode.DATASET_SIDECAR) AmberAccent else TealAccent
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = modeColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, modeColor.copy(alpha = 0.5f)),
                ) {
                    Text(
                        text = settings.mode.shortBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = modeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    )
                }
            }

            // Right utility icons + small top-right Delete button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(
                    onClick = onToggleRandomOrder,
                    modifier = Modifier.testTag("shuffle_toggle_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = if (settings.isRandomOrder) "Random order on" else "Random order off",
                        tint = if (settings.isRandomOrder) VioletAccent else SlateTextSecondary,
                    )
                }

                IconButton(
                    onClick = { showDirectionsSheet = true },
                    modifier = Modifier.testTag("quick_directions_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenWith,
                        contentDescription = "Edit swipe directions",
                        tint = IvoryText,
                    )
                }

                IconButton(
                    onClick = { showHotkeyStrip = !showHotkeyStrip },
                    modifier = Modifier.testTag("toggle_hotkeys_hud_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Keyboard,
                        contentDescription = "Toggle hotkey bar",
                        tint = if (showHotkeyStrip) AmberAccent else SlateTextSecondary,
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("open_settings_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Open settings",
                        tint = IvoryText,
                    )
                }

                // Small Delete Button in Top Right (48dp touch target, compact 32dp visual pill)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_image_button")
                        .clickable(onClick = onRequestDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CoralAccent.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.6f)),
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete current image",
                                tint = CoralAccent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        // 5. Bottom Translucent Dock: Feedback toast, filename, tags, swipe legend & hotkeys
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Feedback banner
            AnimatedVisibility(
                visible = uiState.feedbackBanner != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                uiState.feedbackBanner?.let { msg ->
                    val bannerAccent = if (uiState.feedbackIsError) CoralAccent else TealAccent
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioSurfaceElevated.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, bannerAccent.copy(alpha = 0.55f)),
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (uiState.feedbackIsError) CoralAccent else IvoryText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }

            // Active Keyboard Hotkeys strip (shown when keyboard attached or toggled on)
            AnimatedVisibility(visible = showHotkeyStrip) {
                val activeHotkeys = settings.hotkeys.filter { it.tag.isNotBlank() }
                if (activeHotkeys.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        activeHotkeys.forEach { hk ->
                            Surface(
                                onClick = { onHardwareKeyDown(hk.keyCode) },
                                shape = RoundedCornerShape(8.dp),
                                color = StudioSurface.copy(alpha = 0.84f),
                                border = BorderStroke(1.dp, StudioBorder),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AmberAccent.copy(alpha = 0.2f),
                                    ) {
                                        Text(
                                            text = hk.keyLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberAccent,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        )
                                    }
                                    Text(
                                        text = hk.tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IvoryText,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Filename + Action controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (ready.items.size > 1) {
                        Surface(
                            onClick = onPreviousImage,
                            shape = CircleShape,
                            color = StudioSurface.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("previous_image_button"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = "Previous image",
                                    tint = IvoryText,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    Text(
                        text = currentItem.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = IvoryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Quick trigger for user's Default Direction
                    val defaultDir = settings.defaultDirection
                    val defaultAccent = directionAccentColor(defaultDir)
                    val defaultTag = settings.directionTags[defaultDir].orEmpty().trim()
                    Surface(
                        onClick = onTriggerDefaultSwipe,
                        shape = RoundedCornerShape(10.dp),
                        color = defaultAccent.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, defaultAccent.copy(alpha = 0.6f)),
                        modifier = Modifier.testTag("default_swipe_action_button"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = "Apply default swipe action",
                                tint = defaultAccent,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "${defaultDir.arrowSymbol} ${defaultTag.ifBlank { "Next" }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = defaultAccent,
                            )
                        }
                    }

                    // + Caption button
                    Surface(
                        onClick = { showManualTagDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = StudioSurfaceElevated.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.testTag("add_manual_caption_button"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add manual caption",
                                tint = IvoryText,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Caption",
                                style = MaterialTheme.typography.labelMedium,
                                color = IvoryText,
                            )
                        }
                    }
                }
            }

            // Appended tags row (removable chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentItem.tags.isEmpty()) {
                    Text(
                        text = "No tags yet — swipe any direction or tap + Caption",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextMuted,
                    )
                } else {
                    currentItem.tags.forEach { tag ->
                        Surface(
                            onClick = { onRemoveTag(tag) },
                            shape = RoundedCornerShape(8.dp),
                            color = StudioSurfaceElevated.copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, StudioBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = IvoryText,
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove tag $tag",
                                    tint = SlateTextSecondary,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Compact 4-way directional swipe legend (tappable for accessibility / mouse users)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SwipeDirection.entries.forEach { dir ->
                    val accent = directionAccentColor(dir)
                    val tag = settings.directionTags[dir].orEmpty().trim()
                    val isDefault = dir == settings.defaultDirection

                    Surface(
                        onClick = { onSwipeDirection(dir) },
                        shape = RoundedCornerShape(8.dp),
                        color = StudioSurface.copy(alpha = 0.78f),
                        border = BorderStroke(
                            width = if (isDefault) 1.2.dp else 1.dp,
                            color = if (isDefault) accent.copy(alpha = 0.7f) else StudioBorder.copy(alpha = 0.7f),
                        ),
                        modifier = Modifier.testTag("swipe_chip_${dir.name.lowercase()}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = dir.arrowSymbol,
                                style = MaterialTheme.typography.labelMedium,
                                color = accent,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = tag.ifBlank { "skip" },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tag.isBlank()) SlateTextMuted else IvoryText,
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showDeleteConfirm) {
        DeleteConfirmDialog(
            fileName = currentItem.name,
            mode = settings.mode,
            onConfirm = onConfirmDelete,
            onDismiss = onCancelDelete,
        )
    }

    if (showManualTagDialog) {
        ManualTagDialog(
            fileName = currentItem.name,
            onAddTags = onAddManualTags,
            onDismiss = { showManualTagDialog = false },
        )
    }

    if (showDirectionsSheet) {
        QuickDirectionsSheet(
            directionTags = settings.directionTags,
            defaultDirection = settings.defaultDirection,
            onUpdateDirection = onUpdateDirectionTag,
            onSelectDefaultDirection = onSelectDefaultDirection,
            onDismiss = { showDirectionsSheet = false },
        )
    }
}
