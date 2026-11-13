package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import com.example.model.HotkeyBinding
import com.example.model.SwipeDirection
import com.example.model.TaggingMode
import com.example.model.WidescreenLayoutMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tagswipe_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SELECTED_TREE_URI = stringPreferencesKey("selected_tree_uri")
        val SELECTED_FOLDER_NAME = stringPreferencesKey("selected_folder_name")
        val TAGGING_MODE = stringPreferencesKey("tagging_mode")
        val IS_RANDOM_ORDER = booleanPreferencesKey("is_random_order")
        val CONFIRM_BEFORE_DELETE = booleanPreferencesKey("confirm_before_delete")
        val AUTO_ADVANCE_ON_HOTKEY = booleanPreferencesKey("auto_advance_on_hotkey")
        val LAYOUT_MODE = stringPreferencesKey("layout_mode")
        val DEFAULT_DIRECTION = stringPreferencesKey("default_direction")
        val DIRECTION_TAGS_JSON = stringPreferencesKey("direction_tags_json")
        val HOTKEYS_JSON = stringPreferencesKey("hotkeys_json")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val mode = prefs[Keys.TAGGING_MODE]
            ?.let { runCatching { TaggingMode.valueOf(it) }.getOrNull() }
            ?: TaggingMode.DATASET_SIDECAR

        val defaultDir = prefs[Keys.DEFAULT_DIRECTION]
            ?.let { runCatching { SwipeDirection.valueOf(it) }.getOrNull() }
            ?: SwipeDirection.RIGHT

        val layoutMode = prefs[Keys.LAYOUT_MODE]
            ?.let { runCatching { WidescreenLayoutMode.valueOf(it) }.getOrNull() }
            ?: WidescreenLayoutMode.AUTO

        val directionTags = parseDirectionTags(prefs[Keys.DIRECTION_TAGS_JSON])
        val hotkeys = parseHotkeys(prefs[Keys.HOTKEYS_JSON])

        AppSettings(
            selectedTreeUri = prefs[Keys.SELECTED_TREE_URI],
            selectedFolderName = prefs[Keys.SELECTED_FOLDER_NAME],
            mode = mode,
            isRandomOrder = prefs[Keys.IS_RANDOM_ORDER] ?: false,
            confirmBeforeDelete = prefs[Keys.CONFIRM_BEFORE_DELETE] ?: true,
            autoAdvanceOnHotkey = prefs[Keys.AUTO_ADVANCE_ON_HOTKEY] ?: false,
            layoutMode = layoutMode,
            defaultDirection = defaultDir,
            directionTags = directionTags,
            hotkeys = hotkeys,
        )
    }

    suspend fun setSelectedFolder(uri: String?, name: String?) {
        context.settingsDataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(Keys.SELECTED_TREE_URI)
                prefs.remove(Keys.SELECTED_FOLDER_NAME)
            } else {
                prefs[Keys.SELECTED_TREE_URI] = uri
                prefs[Keys.SELECTED_FOLDER_NAME] = name ?: "Selected Folder"
            }
        }
    }

    suspend fun setMode(mode: TaggingMode) {
        context.settingsDataStore.edit { it[Keys.TAGGING_MODE] = mode.name }
    }

    suspend fun setRandomOrder(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.IS_RANDOM_ORDER] = enabled }
    }

    suspend fun setConfirmBeforeDelete(confirm: Boolean) {
        context.settingsDataStore.edit { it[Keys.CONFIRM_BEFORE_DELETE] = confirm }
    }

    suspend fun setAutoAdvanceOnHotkey(autoAdvance: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_ADVANCE_ON_HOTKEY] = autoAdvance }
    }

    suspend fun setLayoutMode(layoutMode: WidescreenLayoutMode) {
        context.settingsDataStore.edit { it[Keys.LAYOUT_MODE] = layoutMode.name }
    }

    suspend fun setDefaultDirection(direction: SwipeDirection) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_DIRECTION] = direction.name }
    }

    suspend fun updateDirectionTag(direction: SwipeDirection, tag: String) {
        context.settingsDataStore.edit { prefs ->
            val current = parseDirectionTags(prefs[Keys.DIRECTION_TAGS_JSON]).toMutableMap()
            current[direction] = tag
            prefs[Keys.DIRECTION_TAGS_JSON] = serializeDirectionTags(current)
        }
    }

    suspend fun updateHotkey(binding: HotkeyBinding) {
        context.settingsDataStore.edit { prefs ->
            val current = parseHotkeys(prefs[Keys.HOTKEYS_JSON]).toMutableList()
            val idx = current.indexOfFirst { it.slotIndex == binding.slotIndex }
            if (idx >= 0) {
                current[idx] = binding
            }
            prefs[Keys.HOTKEYS_JSON] = serializeHotkeys(current)
        }
    }

    suspend fun resetDirectionsToDefaults() {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DIRECTION_TAGS_JSON] = serializeDirectionTags(AppSettings.defaultDirectionTags())
            prefs[Keys.DEFAULT_DIRECTION] = SwipeDirection.RIGHT.name
        }
    }

    suspend fun resetHotkeysToDefaults() {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.HOTKEYS_JSON] = serializeHotkeys(AppSettings.defaultHotkeys())
        }
    }

    private fun parseDirectionTags(jsonStr: String?): Map<SwipeDirection, String> {
        val defaults = AppSettings.defaultDirectionTags().toMutableMap()
        if (jsonStr.isNullOrBlank()) return defaults
        return runCatching {
            val obj = JSONObject(jsonStr)
            SwipeDirection.entries.forEach { dir ->
                if (obj.has(dir.name)) {
                    defaults[dir] = obj.getString(dir.name)
                }
            }
            defaults
        }.getOrDefault(defaults)
    }

    private fun serializeDirectionTags(map: Map<SwipeDirection, String>): String {
        val obj = JSONObject()
        SwipeDirection.entries.forEach { dir ->
            obj.put(dir.name, map[dir] ?: "")
        }
        return obj.toString()
    }

    private fun parseHotkeys(jsonStr: String?): List<HotkeyBinding> {
        val defaults = AppSettings.defaultHotkeys()
        if (jsonStr.isNullOrBlank()) return defaults
        return runCatching {
            val arr = JSONArray(jsonStr)
            val bySlot = defaults.associateBy { it.slotIndex }.toMutableMap()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val slot = item.optInt("slotIndex", -1)
                if (slot in 1..10) {
                    bySlot[slot] = HotkeyBinding(
                        slotIndex = slot,
                        keyCode = item.optInt("keyCode", 0),
                        keyLabel = item.optString("keyLabel", "UNSET"),
                        tag = item.optString("tag", ""),
                    )
                }
            }
            (1..10).mapNotNull { bySlot[it] }
        }.getOrDefault(defaults)
    }

    private fun serializeHotkeys(list: List<HotkeyBinding>): String {
        val arr = JSONArray()
        list.sortedBy { it.slotIndex }.forEach { binding ->
            arr.put(
                JSONObject().apply {
                    put("slotIndex", binding.slotIndex)
                    put("keyCode", binding.keyCode)
                    put("keyLabel", binding.keyLabel)
                    put("tag", binding.tag)
                },
            )
        }
        return arr.toString()
    }
}
