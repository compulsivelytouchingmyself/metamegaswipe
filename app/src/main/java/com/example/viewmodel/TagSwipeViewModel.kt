package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DirectoryMediaRepository
import com.example.data.SettingsRepository
import com.example.data.TagWriteResult
import com.example.model.AppSettings
import com.example.model.HotkeyBinding
import com.example.model.ImageItem
import com.example.model.SwipeDirection
import com.example.model.TaggingMode
import com.example.model.WidescreenLayoutMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface WorkspaceState {
    data object NoFolder : WorkspaceState
    data object Loading : WorkspaceState
    data class EmptyFolder(val folderName: String) : WorkspaceState
    data class Ready(
        val folderName: String,
        val items: List<ImageItem>,
        val currentIndex: Int,
        val passCount: Int,
    ) : WorkspaceState {
        val currentItem: ImageItem get() = items[currentIndex]
    }
}

data class TagSwipeUiState(
    val settings: AppSettings = AppSettings(),
    val workspace: WorkspaceState = WorkspaceState.NoFolder,
    val isBusy: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val feedbackBanner: String? = null,
    val feedbackIsError: Boolean = false,
)

class TagSwipeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val mediaRepo = DirectoryMediaRepository(application)

    private val _uiState = MutableStateFlow(TagSwipeUiState())
    val uiState: StateFlow<TagSwipeUiState> = _uiState.asStateFlow()

    // Preserves natural sort order so turning off shuffle restores deterministic order
    private var orderedItems: List<ImageItem> = emptyList()

    init {
        viewModelScope.launch {
            val initialSettings = settingsRepo.settingsFlow.first()
            _uiState.update { it.copy(settings = initialSettings) }

            val savedUri = initialSettings.selectedTreeUri
            if (!savedUri.isNullOrBlank()) {
                val parsed = runCatching { Uri.parse(savedUri) }.getOrNull()
                if (parsed != null && hasPersistedPermission(parsed)) {
                    loadDirectory(parsed, initialSettings)
                } else {
                    settingsRepo.setSelectedFolder(null, null)
                }
            }

            settingsRepo.settingsFlow
                .distinctUntilChanged()
                .collect { updated ->
                    val prev = _uiState.value.settings
                    _uiState.update { it.copy(settings = updated) }
                    if (prev.mode != updated.mode) {
                        onModeChangedRefreshTags(updated)
                    }
                }
        }
    }

    fun onDirectoryPicked(uri: Uri?) {
        if (uri == null) return
        val contentResolver = getApplication<Application>().contentResolver
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            contentResolver.takePersistableUriPermission(uri, flags)
        }
        viewModelScope.launch {
            val folderName = mediaRepo.getFolderDisplayName(uri)
            settingsRepo.setSelectedFolder(uri.toString(), folderName)
            val currentSettings = _uiState.value.settings.copy(
                selectedTreeUri = uri.toString(),
                selectedFolderName = folderName,
            )
            loadDirectory(uri, currentSettings)
        }
    }

    fun refreshDirectory() {
        val uriStr = _uiState.value.settings.selectedTreeUri ?: return
        val uri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: return
        viewModelScope.launch {
            loadDirectory(uri, _uiState.value.settings)
        }
    }

    private suspend fun loadDirectory(treeUri: Uri, settings: AppSettings) {
        val folderName = settings.selectedFolderName ?: mediaRepo.getFolderDisplayName(treeUri)
        _uiState.update { it.copy(workspace = WorkspaceState.Loading, feedbackBanner = null) }

        val scanned = mediaRepo.scanDirectory(treeUri)
        orderedItems = scanned
        if (scanned.isEmpty()) {
            _uiState.update {
                it.copy(workspace = WorkspaceState.EmptyFolder(folderName))
            }
            return
        }

        val sessionItems = if (settings.isRandomOrder) scanned.shuffled() else scanned
        val firstWithTags = hydrateItemTags(treeUri, sessionItems.first(), settings.mode)
        val updatedList = sessionItems.toMutableList().apply { set(0, firstWithTags) }

        _uiState.update {
            it.copy(
                workspace = WorkspaceState.Ready(
                    folderName = folderName,
                    items = updatedList,
                    currentIndex = 0,
                    passCount = 1,
                ),
            )
        }
    }

    fun onSwipeDirection(direction: SwipeDirection) {
        val state = _uiState.value
        val ready = state.workspace as? WorkspaceState.Ready ?: return
        val treeUri = state.settings.selectedTreeUri?.let { Uri.parse(it) } ?: return
        if (state.isBusy) return

        val configuredTag = state.settings.directionTags[direction].orEmpty().trim()
        val currentItem = ready.currentItem

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            if (configuredTag.isNotEmpty()) {
                when (val result = mediaRepo.appendTag(treeUri, currentItem, configuredTag, state.settings.mode)) {
                    is TagWriteResult.Success -> {
                        updateItemInReadyState(currentItem.uri, result.tags)
                        showFeedback("${direction.arrowSymbol} +\"$configuredTag\" (${currentItem.name})", isError = false)
                    }
                    is TagWriteResult.Skipped -> {
                        showFeedback("Skipped ${currentItem.name}", isError = false)
                    }
                    is TagWriteResult.Error -> {
                        showFeedback(result.message, isError = true)
                    }
                }
            } else {
                showFeedback("${direction.arrowSymbol} Next (no tag bound)", isError = false)
            }

            advanceToNextImage(treeUri, state.settings)
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    fun onTriggerDefaultSwipe() {
        onSwipeDirection(_uiState.value.settings.defaultDirection)
    }

    fun onPreviousImage() {
        val state = _uiState.value
        val ready = state.workspace as? WorkspaceState.Ready ?: return
        val treeUri = state.settings.selectedTreeUri?.let { Uri.parse(it) } ?: return
        if (ready.items.size <= 1) return

        val prevIndex = if (ready.currentIndex > 0) ready.currentIndex - 1 else ready.items.lastIndex
        viewModelScope.launch {
            val target = ready.items[prevIndex]
            val hydrated = if (target.metadataLoaded) target else hydrateItemTags(treeUri, target, state.settings.mode)
            _uiState.update { current ->
                val ws = current.workspace as? WorkspaceState.Ready ?: return@update current
                val updated = ws.items.toMutableList().apply { set(prevIndex, hydrated) }
                current.copy(workspace = ws.copy(items = updated, currentIndex = prevIndex))
            }
        }
    }

    fun onAddManualTags(rawTags: String) {
        val state = _uiState.value
        val ready = state.workspace as? WorkspaceState.Ready ?: return
        val treeUri = state.settings.selectedTreeUri?.let { Uri.parse(it) } ?: return
        val trimmed = rawTags.trim()
        if (trimmed.isEmpty()) return

        val currentItem = ready.currentItem
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            when (val result = mediaRepo.appendTag(treeUri, currentItem, trimmed, state.settings.mode)) {
                is TagWriteResult.Success -> {
                    updateItemInReadyState(currentItem.uri, result.tags)
                    showFeedback("Appended \"$trimmed\"", isError = false)
                }
                is TagWriteResult.Skipped -> Unit
                is TagWriteResult.Error -> showFeedback(result.message, isError = true)
            }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    fun onRemoveTag(tagToRemove: String) {
        val state = _uiState.value
        val ready = state.workspace as? WorkspaceState.Ready ?: return
        val treeUri = state.settings.selectedTreeUri?.let { Uri.parse(it) } ?: return
        val currentItem = ready.currentItem
        val remaining = currentItem.tags.filterNot { it.equals(tagToRemove, ignoreCase = true) }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            when (val result = mediaRepo.replaceTags(treeUri, currentItem, remaining, state.settings.mode)) {
                is TagWriteResult.Success -> {
                    updateItemInReadyState(currentItem.uri, result.tags)
                    showFeedback("Removed \"$tagToRemove\"", isError = false)
                }
                is TagWriteResult.Skipped -> Unit
                is TagWriteResult.Error -> showFeedback(result.message, isError = true)
            }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    fun onDeleteRequested() {
        val state = _uiState.value
        if (state.workspace !is WorkspaceState.Ready || state.isBusy) return
        if (state.settings.confirmBeforeDelete) {
            _uiState.update { it.copy(showDeleteConfirm = true) }
        } else {
            executeDeleteCurrent()
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun executeDeleteCurrent() {
        val state = _uiState.value
        val ready = state.workspace as? WorkspaceState.Ready ?: return
        val treeUri = state.settings.selectedTreeUri?.let { Uri.parse(it) } ?: return
        val target = ready.currentItem

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, showDeleteConfirm = false) }
            val deleted = mediaRepo.deleteImage(
                treeUri = treeUri,
                item = target,
                deleteSidecar = state.settings.mode == TaggingMode.DATASET_SIDECAR,
            )
            if (!deleted) {
                showFeedback("Could not delete ${target.name}", isError = true)
                _uiState.update { it.copy(isBusy = false) }
                return@launch
            }

            orderedItems = orderedItems.filterNot { it.uri == target.uri }
            val remaining = ready.items.filterNot { it.uri == target.uri }
            if (remaining.isEmpty()) {
                _uiState.update {
                    it.copy(
                        workspace = WorkspaceState.EmptyFolder(ready.folderName),
                        isBusy = false,
                        feedbackBanner = "Deleted ${target.name} — folder is now empty",
                        feedbackIsError = false,
                    )
                }
                return@launch
            }

            var nextPass = ready.passCount
            val nextIndex = if (ready.currentIndex >= remaining.size) {
                nextPass += 1
                0
            } else {
                ready.currentIndex
            }

            val nextItem = hydrateItemTags(treeUri, remaining[nextIndex], state.settings.mode)
            val updatedList = remaining.toMutableList().apply { set(nextIndex, nextItem) }

            _uiState.update {
                it.copy(
                    workspace = ready.copy(
                        items = updatedList,
                        currentIndex = nextIndex,
                        passCount = nextPass,
                    ),
                    isBusy = false,
                    feedbackBanner = "Deleted ${target.name}",
                    feedbackIsError = false,
                )
            }
        }
    }

    /**
     * Handles physical hardware keyboard presses on the full-screen canvas.
     * Returns true if the key event was consumed.
     */
    fun onHardwareKeyDown(keyCode: Int): Boolean {
        val state = _uiState.value
        val ready = state.workspace as? WorkspaceState.Ready ?: return false
        val treeUri = state.settings.selectedTreeUri?.let { Uri.parse(it) } ?: return false
        if (state.isBusy || state.showDeleteConfirm) return false

        // 1. Check user-defined 10 hotkeys first
        val matchingHotkey = state.settings.hotkeys.firstOrNull {
            it.keyCode == keyCode && it.keyCode != KeyEvent.KEYCODE_UNKNOWN && it.tag.isNotBlank()
        }
        if (matchingHotkey != null) {
            val currentItem = ready.currentItem
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true) }
                when (val result = mediaRepo.appendTag(treeUri, currentItem, matchingHotkey.tag, state.settings.mode)) {
                    is TagWriteResult.Success -> {
                        updateItemInReadyState(currentItem.uri, result.tags)
                        showFeedback("[${matchingHotkey.keyLabel}] +\"${matchingHotkey.tag}\"", isError = false)
                    }
                    is TagWriteResult.Skipped -> Unit
                    is TagWriteResult.Error -> showFeedback(result.message, isError = true)
                }
                if (state.settings.autoAdvanceOnHotkey) {
                    advanceToNextImage(treeUri, state.settings)
                }
                _uiState.update { it.copy(isBusy = false) }
            }
            return true
        }

        // 2. Hardware directional keys map to swipe directions
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                onSwipeDirection(SwipeDirection.UP)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                onSwipeDirection(SwipeDirection.DOWN)
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                onSwipeDirection(SwipeDirection.LEFT)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                onSwipeDirection(SwipeDirection.RIGHT)
                true
            }
            KeyEvent.KEYCODE_SPACE -> {
                onTriggerDefaultSwipe()
                true
            }
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL -> {
                onDeleteRequested()
                true
            }
            else -> false
        }
    }

    fun toggleRandomOrder() {
        val next = !_uiState.value.settings.isRandomOrder
        setRandomOrder(next)
    }

    fun setRandomOrder(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setRandomOrder(enabled)
            val ready = _uiState.value.workspace as? WorkspaceState.Ready ?: return@launch
            val currentItem = ready.currentItem
            val treeUri = _uiState.value.settings.selectedTreeUri?.let { Uri.parse(it) } ?: return@launch

            val reordered = if (enabled) {
                val others = ready.items.filter { it.uri != currentItem.uri }.shuffled()
                listOf(currentItem) + others
            } else {
                val existingByUri = ready.items.associateBy { it.uri }
                val natural = orderedItems.mapNotNull { existingByUri[it.uri] }
                if (natural.isEmpty()) ready.items else natural
            }
            val newIndex = if (enabled) 0 else reordered.indexOfFirst { it.uri == currentItem.uri }.coerceAtLeast(0)
            val hydrated = hydrateItemTags(treeUri, reordered[newIndex], _uiState.value.settings.mode)
            val finalList = reordered.toMutableList().apply { set(newIndex, hydrated) }

            _uiState.update {
                it.copy(
                    workspace = ready.copy(items = finalList, currentIndex = newIndex),
                    feedbackBanner = if (enabled) "Random order enabled" else "Sequential filename order",
                    feedbackIsError = false,
                )
            }
        }
    }

    fun setMode(mode: TaggingMode) {
        viewModelScope.launch { settingsRepo.setMode(mode) }
    }

    fun setConfirmBeforeDelete(confirm: Boolean) {
        viewModelScope.launch { settingsRepo.setConfirmBeforeDelete(confirm) }
    }

    fun setAutoAdvanceOnHotkey(autoAdvance: Boolean) {
        viewModelScope.launch { settingsRepo.setAutoAdvanceOnHotkey(autoAdvance) }
    }

    fun setLayoutMode(layoutMode: WidescreenLayoutMode) {
        viewModelScope.launch {
            settingsRepo.setLayoutMode(layoutMode)
            showFeedback("View: ${layoutMode.title}", isError = false)
        }
    }

    fun toggleWidescreenView(currentlyWide: Boolean) {
        val next = if (currentlyWide) WidescreenLayoutMode.OVERLAY else WidescreenLayoutMode.WIDESCREEN
        setLayoutMode(next)
    }

    fun setDefaultDirection(direction: SwipeDirection) {
        viewModelScope.launch { settingsRepo.setDefaultDirection(direction) }
    }

    fun updateDirectionTag(direction: SwipeDirection, tag: String) {
        viewModelScope.launch { settingsRepo.updateDirectionTag(direction, tag) }
    }

    fun updateHotkey(binding: HotkeyBinding) {
        viewModelScope.launch { settingsRepo.updateHotkey(binding) }
    }

    fun resetDirectionsToDefaults() {
        viewModelScope.launch { settingsRepo.resetDirectionsToDefaults() }
    }

    fun resetHotkeysToDefaults() {
        viewModelScope.launch { settingsRepo.resetHotkeysToDefaults() }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(feedbackBanner = null) }
    }

    private suspend fun onModeChangedRefreshTags(settings: AppSettings) {
        val ready = _uiState.value.workspace as? WorkspaceState.Ready ?: return
        val treeUri = settings.selectedTreeUri?.let { Uri.parse(it) } ?: return
        val resetItems = ready.items.map { it.copy(metadataLoaded = false, tags = emptyList()) }.toMutableList()
        val currentHydrated = hydrateItemTags(treeUri, resetItems[ready.currentIndex], settings.mode)
        resetItems[ready.currentIndex] = currentHydrated
        _uiState.update {
            it.copy(
                workspace = ready.copy(items = resetItems),
                feedbackBanner = "Switched to ${settings.mode.title}",
                feedbackIsError = false,
            )
        }
    }

    private suspend fun advanceToNextImage(treeUri: Uri, settings: AppSettings) {
        val ready = _uiState.value.workspace as? WorkspaceState.Ready ?: return
        if (ready.items.isEmpty()) return

        val nextRawIndex = ready.currentIndex + 1
        if (nextRawIndex < ready.items.size) {
            val nextItem = ready.items[nextRawIndex]
            val hydrated = if (nextItem.metadataLoaded) nextItem else hydrateItemTags(treeUri, nextItem, settings.mode)
            _uiState.update { current ->
                val ws = current.workspace as? WorkspaceState.Ready ?: return@update current
                val updated = ws.items.toMutableList().apply { set(nextRawIndex, hydrated) }
                current.copy(workspace = ws.copy(items = updated, currentIndex = nextRawIndex))
            }
        } else {
            // Image set finished -> repeat for another pass so a new set of tags can be appended!
            val nextPass = ready.passCount + 1
            val lastUri = ready.currentItem.uri
            val nextPassItems = if (settings.isRandomOrder) {
                val shuffled = ready.items.shuffled().toMutableList()
                if (shuffled.size > 1 && shuffled.first().uri == lastUri) {
                    val swapIdx = (1 until shuffled.size).random()
                    val temp = shuffled[0]
                    shuffled[0] = shuffled[swapIdx]
                    shuffled[swapIdx] = temp
                }
                shuffled
            } else {
                ready.items
            }
            val firstHydrated = hydrateItemTags(treeUri, nextPassItems.first(), settings.mode)
            val updated = nextPassItems.toMutableList().apply { set(0, firstHydrated) }

            _uiState.update { current ->
                val ws = current.workspace as? WorkspaceState.Ready ?: return@update current
                current.copy(
                    workspace = ws.copy(
                        items = updated,
                        currentIndex = 0,
                        passCount = nextPass,
                    ),
                    feedbackBanner = "Set complete — starting Pass #$nextPass (new tags append)",
                    feedbackIsError = false,
                )
            }
        }
    }

    private suspend fun hydrateItemTags(treeUri: Uri, item: ImageItem, mode: TaggingMode): ImageItem {
        val tags = mediaRepo.readTags(treeUri, item, mode)
        return item.copy(tags = tags, metadataLoaded = true)
    }

    private fun updateItemInReadyState(uri: Uri, tags: List<String>) {
        _uiState.update { state ->
            val ws = state.workspace as? WorkspaceState.Ready ?: return@update state
            val updated = ws.items.map { item ->
                if (item.uri == uri) item.copy(tags = tags, metadataLoaded = true) else item
            }
            state.copy(workspace = ws.copy(items = updated))
        }
    }

    private fun showFeedback(message: String, isError: Boolean) {
        _uiState.update { it.copy(feedbackBanner = message, feedbackIsError = isError) }
    }

    private fun hasPersistedPermission(uri: Uri): Boolean {
        val persisted = getApplication<Application>().contentResolver.persistedUriPermissions
        return persisted.any { it.uri == uri && it.isReadPermission && it.isWritePermission }
    }
}
