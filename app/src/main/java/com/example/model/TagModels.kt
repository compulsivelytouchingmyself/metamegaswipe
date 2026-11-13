package com.example.model

import android.net.Uri
import android.view.KeyEvent

enum class TaggingMode(
    val title: String,
    val shortBadge: String,
    val description: String,
) {
    DATASET_SIDECAR(
        title = "Dataset Mode (.txt Sidecar)",
        shortBadge = "TXT",
        description = "Writes appended comma-separated caption tags into a matching <filename>.txt document inside the working folder.",
    ),
    HEADER_METADATA(
        title = "Folder Organize (Header Metadata)",
        shortBadge = "EXIF",
        description = "Embeds appended tags directly inside each image file's EXIF header metadata (ImageDescription & UserComment).",
    ),
}

enum class WidescreenLayoutMode(
    val title: String,
    val shortLabel: String,
    val description: String,
) {
    AUTO(
        title = "Auto (Adaptive)",
        shortLabel = "Auto",
        description = "Uses split widescreen studio view in landscape, tablets, and Chromebooks; full-screen overlay in portrait.",
    ),
    WIDESCREEN(
        title = "Widescreen Split Pane",
        shortLabel = "Wide",
        description = "Always shows the side inspector pane with clickable 4-way pad, tags, and hotkeys alongside the image.",
    ),
    OVERLAY(
        title = "Full-Bleed Overlay",
        shortLabel = "Full",
        description = "Always keeps the photo full-screen edge-to-edge with floating translucent HUD controls.",
    ),
}

enum class SwipeDirection(
    val label: String,
    val arrowSymbol: String,
) {
    UP("Swipe Up", "↑"),
    DOWN("Swipe Down", "↓"),
    LEFT("Swipe Left", "←"),
    RIGHT("Swipe Right", "→"),
}

data class HotkeyBinding(
    val slotIndex: Int, // 1..10
    val keyCode: Int,
    val keyLabel: String,
    val tag: String,
)

data class AppSettings(
    val selectedTreeUri: String? = null,
    val selectedFolderName: String? = null,
    val mode: TaggingMode = TaggingMode.DATASET_SIDECAR,
    val isRandomOrder: Boolean = false,
    val confirmBeforeDelete: Boolean = true,
    val autoAdvanceOnHotkey: Boolean = false,
    val layoutMode: WidescreenLayoutMode = WidescreenLayoutMode.AUTO,
    val defaultDirection: SwipeDirection = SwipeDirection.RIGHT,
    val directionTags: Map<SwipeDirection, String> = defaultDirectionTags(),
    val hotkeys: List<HotkeyBinding> = defaultHotkeys(),
) {
    companion object {
        fun defaultDirectionTags(): Map<SwipeDirection, String> = mapOf(
            SwipeDirection.UP to "featured",
            SwipeDirection.DOWN to "archive",
            SwipeDirection.LEFT to "reject",
            SwipeDirection.RIGHT to "approved",
        )

        fun defaultHotkeys(): List<HotkeyBinding> = listOf(
            HotkeyBinding(1, KeyEvent.KEYCODE_1, "1", "portrait"),
            HotkeyBinding(2, KeyEvent.KEYCODE_2, "2", "landscape"),
            HotkeyBinding(3, KeyEvent.KEYCODE_3, "3", "close_up"),
            HotkeyBinding(4, KeyEvent.KEYCODE_4, "4", "wide_shot"),
            HotkeyBinding(5, KeyEvent.KEYCODE_5, "5", "outdoor"),
            HotkeyBinding(6, KeyEvent.KEYCODE_6, "6", "indoor"),
            HotkeyBinding(7, KeyEvent.KEYCODE_7, "7", "high_contrast"),
            HotkeyBinding(8, KeyEvent.KEYCODE_8, "8", "low_light"),
            HotkeyBinding(9, KeyEvent.KEYCODE_9, "9", "monochrome"),
            HotkeyBinding(10, KeyEvent.KEYCODE_0, "0", "favorite"),
        )
    }
}

data class ImageItem(
    val uri: Uri,
    val name: String,
    val stem: String,
    val mimeType: String,
    val sizeBytes: Long = 0L,
    val tags: List<String> = emptyList(),
    val metadataLoaded: Boolean = false,
)

fun keyCodeToReadableLabel(keyCode: Int): String {
    return when (keyCode) {
        KeyEvent.KEYCODE_UNKNOWN -> "UNSET"
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> ('0' + (keyCode - KeyEvent.KEYCODE_0)).toString()
        in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> ('A' + (keyCode - KeyEvent.KEYCODE_A)).toString()
        in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> "F${keyCode - KeyEvent.KEYCODE_F1 + 1}"
        KeyEvent.KEYCODE_SPACE -> "SPACE"
        KeyEvent.KEYCODE_ENTER -> "ENTER"
        KeyEvent.KEYCODE_TAB -> "TAB"
        KeyEvent.KEYCODE_GRAVE -> "`"
        KeyEvent.KEYCODE_MINUS -> "-"
        KeyEvent.KEYCODE_EQUALS -> "="
        KeyEvent.KEYCODE_LEFT_BRACKET -> "["
        KeyEvent.KEYCODE_RIGHT_BRACKET -> "]"
        KeyEvent.KEYCODE_BACKSLASH -> "\\"
        KeyEvent.KEYCODE_SEMICOLON -> ";"
        KeyEvent.KEYCODE_APOSTROPHE -> "'"
        KeyEvent.KEYCODE_COMMA -> ","
        KeyEvent.KEYCODE_PERIOD -> "."
        KeyEvent.KEYCODE_SLASH -> "/"
        else -> {
            val raw = KeyEvent.keyCodeToString(keyCode)
                .removePrefix("KEYCODE_")
                .replace("_", " ")
            raw.ifBlank { "KEY $keyCode" }
        }
    }
}
