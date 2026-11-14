package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AccentPreset
import com.example.data.model.AppPreferences
import com.example.data.model.FavoriteFolder
import com.example.data.model.FileItem
import com.example.data.model.RecentFileEntry
import com.example.data.model.SortDirection
import com.example.data.model.SortOption
import com.example.data.model.ThemeMode
import com.example.data.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onyx_files_prefs")

class UserPreferencesRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val SORT_DIRECTION = stringPreferencesKey("sort_direction")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val SHOW_EXTENSIONS = booleanPreferencesKey("show_extensions")
        val FAVORITES_JSON = stringPreferencesKey("favorites_json")
        val RECENTS_JSON = stringPreferencesKey("recents_json")
        val SAF_URIS = stringSetPreferencesKey("saf_uris")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.PURE_AMOLED
        val accent = prefs[Keys.ACCENT_PRESET]?.let { runCatching { AccentPreset.valueOf(it) }.getOrNull() }
            ?: AccentPreset.CYAN
        val viewMode = prefs[Keys.VIEW_MODE]?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() }
            ?: ViewMode.LIST
        val sortOption = prefs[Keys.SORT_OPTION]?.let { runCatching { SortOption.valueOf(it) }.getOrNull() }
            ?: SortOption.NAME
        val sortDirection = prefs[Keys.SORT_DIRECTION]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
            ?: SortDirection.ASCENDING

        val favorites = prefs[Keys.FAVORITES_JSON]?.let { raw ->
            runCatching { json.decodeFromString<List<FavoriteFolder>>(raw) }.getOrDefault(emptyList())
        } ?: emptyList()

        val recents = prefs[Keys.RECENTS_JSON]?.let { raw ->
            runCatching { json.decodeFromString<List<RecentFileEntry>>(raw) }.getOrDefault(emptyList())
        } ?: emptyList()

        AppPreferences(
            themeMode = theme,
            accentPreset = accent,
            viewMode = viewMode,
            sortOption = sortOption,
            sortDirection = sortDirection,
            showHiddenFiles = prefs[Keys.SHOW_HIDDEN] ?: false,
            showFileExtensions = prefs[Keys.SHOW_EXTENSIONS] ?: true,
            favorites = favorites,
            recentFiles = recents,
            safTreeUris = prefs[Keys.SAF_URIS]?.toList() ?: emptyList()
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentPreset(preset: AccentPreset) {
        context.dataStore.edit { it[Keys.ACCENT_PRESET] = preset.name }
    }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { it[Keys.VIEW_MODE] = mode.name }
    }

    suspend fun setSort(option: SortOption, direction: SortDirection) {
        context.dataStore.edit {
            it[Keys.SORT_OPTION] = option.name
            it[Keys.SORT_DIRECTION] = direction.name
        }
    }

    suspend fun setShowHiddenFiles(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_HIDDEN] = show }
    }

    suspend fun setShowFileExtensions(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_EXTENSIONS] = show }
    }

    suspend fun toggleFavorite(folder: FavoriteFolder) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES_JSON]?.let {
                runCatching { json.decodeFromString<List<FavoriteFolder>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()

            val exists = current.any { it.path == folder.path && it.uriString == folder.uriString }
            val updated = if (exists) {
                current.filterNot { it.path == folder.path && it.uriString == folder.uriString }
            } else {
                listOf(folder) + current
            }
            prefs[Keys.FAVORITES_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun removeFavorite(folder: FavoriteFolder) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES_JSON]?.let {
                runCatching { json.decodeFromString<List<FavoriteFolder>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            val updated = current.filterNot { it.path == folder.path && it.uriString == folder.uriString }
            prefs[Keys.FAVORITES_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun recordRecentFile(item: FileItem) {
        if (item.isDirectory) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENTS_JSON]?.let {
                runCatching { json.decodeFromString<List<RecentFileEntry>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()

            val entry = RecentFileEntry(
                name = item.name,
                path = item.path,
                uriString = item.uriString,
                mimeType = item.mimeType,
                size = item.size,
                openedAt = System.currentTimeMillis()
            )
            val updated = (listOf(entry) + current.filterNot { it.path == item.path && it.uriString == item.uriString })
                .take(40)
            prefs[Keys.RECENTS_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun clearRecentFiles() {
        context.dataStore.edit { it.remove(Keys.RECENTS_JSON) }
    }

    suspend fun addSafTreeUri(uriString: String) {
        context.dataStore.edit { prefs ->
            val existing = prefs[Keys.SAF_URIS] ?: emptySet()
            prefs[Keys.SAF_URIS] = existing + uriString
        }
    }

    suspend fun removeSafTreeUri(uriString: String) {
        context.dataStore.edit { prefs ->
            val existing = prefs[Keys.SAF_URIS] ?: emptySet()
            prefs[Keys.SAF_URIS] = existing - uriString
        }
    }
}
