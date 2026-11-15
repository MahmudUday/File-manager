package com.example.ui.viewmodel

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccentPreset
import com.example.data.model.AppPreferences
import com.example.data.model.CategorySummary
import com.example.data.model.ClipboardMode
import com.example.data.model.ClipboardState
import com.example.data.model.FavoriteFolder
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.FileProperties
import com.example.data.model.FileTypeGroup
import com.example.data.model.OperationState
import com.example.data.model.SortDirection
import com.example.data.model.SortOption
import com.example.data.model.StorageVolumeInfo
import com.example.data.model.ThemeMode
import com.example.data.model.ViewMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FileOperationsRepository
import com.example.media.AudioPlayerController
import com.example.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Immutable
data class BreadcrumbSegment(
    val label: String,
    val path: String,
    val isSaf: Boolean = false
)

@Immutable
data class BrowserUiState(
    val currentPath: String = Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0",
    val isCurrentSaf: Boolean = false,
    val rootLabel: String = "Internal Storage",
    val rootPath: String = Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0",
    val breadcrumbs: List<BreadcrumbSegment> = emptyList(),
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val deepSearchEnabled: Boolean = false,
    val searchResults: List<FileItem> = emptyList(),
    val isSearching: Boolean = false,
    val selectedIds: Set<String> = emptySet()
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()
    val displayedItems: List<FileItem>
        get() = if (isSearchActive && searchQuery.isNotBlank()) searchResults else items
}

@Immutable
data class DashboardUiState(
    val volumes: List<StorageVolumeInfo> = emptyList(),
    val categorySummaries: List<CategorySummary> = emptyList(),
    val isLoading: Boolean = true,
    val hasAllFilesAccess: Boolean = false,
    val hasAnyStorageAccess: Boolean = false
)

@Immutable
data class CategoryUiState(
    val category: FileCategory = FileCategory.IMAGES,
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedIds: Set<String> = emptySet()
) {
    val filteredItems: List<FileItem>
        get() = if (searchQuery.isBlank()) items else {
            val q = searchQuery.trim().lowercase()
            items.filter { it.name.lowercase().contains(q) }
        }
}

class MainViewModel(
    private val appContext: Context,
    private val prefsRepo: UserPreferencesRepository,
    private val fileRepo: FileOperationsRepository,
    val audioController: AudioPlayerController
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = prefsRepo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppPreferences()
    )

    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _browserState = MutableStateFlow(BrowserUiState())
    val browserState: StateFlow<BrowserUiState> = _browserState.asStateFlow()

    private val _categoryState = MutableStateFlow(CategoryUiState())
    val categoryState: StateFlow<CategoryUiState> = _categoryState.asStateFlow()

    private val _clipboard = MutableStateFlow(ClipboardState())
    val clipboard: StateFlow<ClipboardState> = _clipboard.asStateFlow()

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    private val _propertiesState = MutableStateFlow<FileProperties?>(null)
    val propertiesState: StateFlow<FileProperties?> = _propertiesState.asStateFlow()

    private val _isLoadingProperties = MutableStateFlow(false)
    val isLoadingProperties: StateFlow<Boolean> = _isLoadingProperties.asStateFlow()

    private var directoryJob: Job? = null
    private var searchJob: Job? = null
    private var categoryJob: Job? = null

    init {
        refreshPermissionsAndDashboard()
        val initialPath = Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"
        openDirectory(path = initialPath, isSaf = false, rootLabel = "Internal Storage", rootPath = initialPath)
    }

    fun refreshOnResume() {
        val prevHasAll = _dashboardState.value.hasAllFilesAccess
        val nowHasAll = PermissionHelper.hasAllFilesAccess(appContext)
        val nowHasAny = PermissionHelper.hasAnyStorageAccess(appContext)
        if (prevHasAll != nowHasAll || _dashboardState.value.volumes.isEmpty()) {
            refreshPermissionsAndDashboard()
            reloadCurrentDirectory()
        } else {
            _dashboardState.update { it.copy(hasAllFilesAccess = nowHasAll, hasAnyStorageAccess = nowHasAny) }
        }
    }

    fun refreshPermissionsAndDashboard() {
        viewModelScope.launch {
            val hasAll = PermissionHelper.hasAllFilesAccess(appContext)
            val hasAny = PermissionHelper.hasAnyStorageAccess(appContext)
            val safUris = preferences.value.safTreeUris
            val volumes = withContext(Dispatchers.IO) { fileRepo.getStorageVolumes(safUris) }
            _dashboardState.update {
                it.copy(
                    volumes = volumes,
                    hasAllFilesAccess = hasAll,
                    hasAnyStorageAccess = hasAny,
                    isLoading = it.categorySummaries.isEmpty()
                )
            }
            val summaries = fileRepo.getCategorySummaries()
            _dashboardState.update {
                it.copy(
                    categorySummaries = summaries,
                    isLoading = false
                )
            }
        }
    }

    fun openVolume(volume: StorageVolumeInfo) {
        val targetPath = volume.uriString ?: volume.path
        openDirectory(
            path = targetPath,
            isSaf = volume.isSafMounted,
            rootLabel = volume.name,
            rootPath = targetPath
        )
    }

    fun openDirectory(
        path: String,
        isSaf: Boolean = false,
        rootLabel: String? = null,
        rootPath: String? = null
    ) {
        val resolvedRootLabel = rootLabel ?: _browserState.value.rootLabel
        val resolvedRootPath = rootPath ?: _browserState.value.rootPath
        val breadcrumbs = buildBreadcrumbs(path, isSaf, resolvedRootLabel, resolvedRootPath)

        _browserState.update {
            it.copy(
                currentPath = path,
                isCurrentSaf = isSaf,
                rootLabel = resolvedRootLabel,
                rootPath = resolvedRootPath,
                breadcrumbs = breadcrumbs,
                isLoading = true,
                errorMessage = null,
                selectedIds = emptySet(),
                isSearchActive = false,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
        reloadCurrentDirectory()
    }

    fun reloadCurrentDirectory() {
        val current = _browserState.value
        val prefs = preferences.value
        directoryJob?.cancel()
        directoryJob = viewModelScope.launch {
            _browserState.update { it.copy(isLoading = it.items.isEmpty(), errorMessage = null) }
            fileRepo.listDirectoryStream(
                path = current.currentPath,
                isSafUri = current.isCurrentSaf,
                showHidden = prefs.showHiddenFiles,
                sortOption = prefs.sortOption,
                sortDirection = prefs.sortDirection
            )
                .catch { e ->
                    _browserState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Unable to open folder")
                    }
                }
                .collect { items ->
                    _browserState.update {
                        it.copy(items = items, isLoading = false, errorMessage = null)
                    }
                }
        }
    }

    fun navigateUpDirectory(): Boolean {
        val state = _browserState.value
        if (state.isSelectionMode) {
            clearSelection()
            return true
        }
        if (state.isSearchActive) {
            setSearchActive(false)
            return true
        }
        if (state.breadcrumbs.size > 1) {
            val parent = state.breadcrumbs[state.breadcrumbs.lastIndex - 1]
            openDirectory(parent.path, parent.isSaf, state.rootLabel, state.rootPath)
            return true
        }
        return false
    }

    private fun buildBreadcrumbs(
        path: String,
        isSaf: Boolean,
        rootLabel: String,
        rootPath: String
    ): List<BreadcrumbSegment> {
        if (isSaf) {
            return listOf(BreadcrumbSegment(rootLabel, path, isSaf = true))
        }
        val list = mutableListOf(BreadcrumbSegment(rootLabel, rootPath, isSaf = false))
        if (path == rootPath || !path.startsWith(rootPath)) {
            return list
        }
        val relative = path.removePrefix(rootPath).trimStart(File.separatorChar)
        if (relative.isEmpty()) return list
        var running = rootPath
        relative.split(File.separatorChar).filter { it.isNotEmpty() }.forEach { segment ->
            running = "$running${File.separator}$segment"
            list.add(BreadcrumbSegment(segment, running, isSaf = false))
        }
        return list
    }

    fun setSearchActive(active: Boolean) {
        _browserState.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (active) it.searchQuery else "",
                searchResults = if (active) it.searchResults else emptyList(),
                isSearching = false
            )
        }
        if (!active) searchJob?.cancel()
    }

    fun updateSearchQuery(query: String) {
        _browserState.update { it.copy(searchQuery = query) }
        runBrowserSearch(query, _browserState.value.deepSearchEnabled)
    }

    fun toggleDeepSearch() {
        val next = !_browserState.value.deepSearchEnabled
        _browserState.update { it.copy(deepSearchEnabled = next) }
        if (_browserState.value.searchQuery.isNotBlank()) {
            runBrowserSearch(_browserState.value.searchQuery, next)
        }
    }

    private fun runBrowserSearch(query: String, deep: Boolean) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _browserState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        val state = _browserState.value
        val prefs = preferences.value
        searchJob = viewModelScope.launch {
            delay(180L)
            _browserState.update { it.copy(isSearching = true) }
            fileRepo.searchFiles(
                query = query,
                rootPath = state.currentPath,
                isSafUri = state.isCurrentSaf,
                showHidden = prefs.showHiddenFiles,
                deepSearch = deep
            )
                .catch {
                    _browserState.update { it.copy(isSearching = false) }
                }
                .collect { results ->
                    _browserState.update { it.copy(searchResults = results, isSearching = false) }
                }
        }
    }

    fun openCategory(category: FileCategory) {
        categoryJob?.cancel()
        _categoryState.value = CategoryUiState(category = category, isLoading = true)
        categoryJob = viewModelScope.launch {
            val items = fileRepo.queryCategoryFiles(
                category = category,
                showHidden = preferences.value.showHiddenFiles
            )
            val sorted = fileRepo.sortItems(
                items,
                preferences.value.sortOption,
                preferences.value.sortDirection
            )
            _categoryState.update { it.copy(items = sorted, isLoading = false) }
        }
    }

    fun updateCategorySearch(query: String) {
        _categoryState.update { it.copy(searchQuery = query) }
    }

    fun toggleCategorySelection(id: String) {
        _categoryState.update { state ->
            val next = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = next)
        }
    }

    fun clearCategorySelection() {
        _categoryState.update { it.copy(selectedIds = emptySet()) }
    }

    fun toggleSelection(id: String) {
        _browserState.update { state ->
            val next = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = next)
        }
    }

    fun selectAllDisplayed() {
        _browserState.update { state ->
            val allIds = state.displayedItems.map { it.id }.toSet()
            state.copy(selectedIds = allIds)
        }
    }

    fun clearSelection() {
        _browserState.update { it.copy(selectedIds = emptySet()) }
    }

    fun setClipboard(mode: ClipboardMode, items: List<FileItem>) {
        _clipboard.value = ClipboardState(mode = mode, items = items)
        clearSelection()
        clearCategorySelection()
    }

    fun clearClipboard() {
        _clipboard.value = ClipboardState()
    }

    fun pasteHere() {
        val clip = _clipboard.value
        if (!clip.isActive) return
        val destPath = _browserState.value.currentPath
        val isDestSaf = _browserState.value.isCurrentSaf
        val isMove = clip.mode == ClipboardMode.CUT

        viewModelScope.launch {
            _operationState.value = OperationState.Running(
                title = if (isMove) "Moving items" else "Copying items",
                progress = 0f,
                detail = "Preparing..."
            )
            fileRepo.pasteItems(clip.items, destPath, isDestSaf, isMove)
                .catch { e ->
                    _operationState.value = OperationState.Error(e.message ?: "Operation failed")
                }
                .collect { (progress, detail) ->
                    _operationState.value = OperationState.Running(
                        title = if (isMove) "Moving items" else "Copying items",
                        progress = progress,
                        detail = detail
                    )
                }
            if (_operationState.value !is OperationState.Error) {
                _clipboard.value = ClipboardState()
                _operationState.value = OperationState.Success(
                    if (isMove) "Moved ${clip.items.size} item(s)" else "Copied ${clip.items.size} item(s)"
                )
                reloadCurrentDirectory()
                refreshPermissionsAndDashboard()
            }
        }
    }

    fun createFolder(name: String) {
        val state = _browserState.value
        viewModelScope.launch {
            val result = fileRepo.createFolder(state.currentPath, state.isCurrentSaf, name)
            result.onSuccess {
                _operationState.value = OperationState.Success("Folder '$name' created")
                reloadCurrentDirectory()
            }.onFailure {
                _operationState.value = OperationState.Error(it.message ?: "Failed to create folder")
            }
        }
    }

    fun createFile(name: String) {
        val state = _browserState.value
        viewModelScope.launch {
            val result = fileRepo.createEmptyFile(state.currentPath, state.isCurrentSaf, name)
            result.onSuccess {
                _operationState.value = OperationState.Success("File '$name' created")
                reloadCurrentDirectory()
            }.onFailure {
                _operationState.value = OperationState.Error(it.message ?: "Failed to create file")
            }
        }
    }

    fun renameItem(item: FileItem, newName: String) {
        viewModelScope.launch {
            val result = fileRepo.renameItem(item, newName)
            result.onSuccess {
                _operationState.value = OperationState.Success("Renamed to '$newName'")
                clearSelection()
                reloadCurrentDirectory()
                refreshCategoryIfLoaded()
            }.onFailure {
                _operationState.value = OperationState.Error(it.message ?: "Rename failed")
            }
        }
    }

    fun deleteItems(items: List<FileItem>) {
        viewModelScope.launch {
            val result = fileRepo.deleteItems(items)
            result.onSuccess { count ->
                _operationState.value = OperationState.Success("Deleted $count item(s)")
                clearSelection()
                clearCategorySelection()
                reloadCurrentDirectory()
                refreshCategoryIfLoaded()
                refreshPermissionsAndDashboard()
            }.onFailure {
                _operationState.value = OperationState.Error(it.message ?: "Delete failed")
            }
        }
    }

    fun compressItemsToZip(items: List<FileItem>, archiveName: String) {
        val destPath = _browserState.value.currentPath
        viewModelScope.launch {
            _operationState.value = OperationState.Running("Compressing Archive", 0f, "Collecting files...")
            fileRepo.compressToZip(items, destPath, archiveName)
                .catch { e ->
                    _operationState.value = OperationState.Error(e.message ?: "Compression failed")
                }
                .collect { (progress, detail) ->
                    _operationState.value = OperationState.Running("Compressing Archive", progress, detail)
                }
            if (_operationState.value !is OperationState.Error) {
                clearSelection()
                _operationState.value = OperationState.Success("Archive created")
                reloadCurrentDirectory()
            }
        }
    }

    fun extractZip(zipItem: FileItem) {
        val destPath = if (_browserState.value.currentPath.isNotBlank() && !_browserState.value.isCurrentSaf) {
            File(zipItem.path).parent ?: _browserState.value.currentPath
        } else {
            _browserState.value.currentPath
        }
        viewModelScope.launch {
            _operationState.value = OperationState.Running("Extracting Archive", 0.1f, "Opening ${zipItem.name}...")
            fileRepo.extractZip(zipItem, destPath)
                .catch { e ->
                    _operationState.value = OperationState.Error(e.message ?: "Extraction failed")
                }
                .collect { (progress, detail) ->
                    _operationState.value = OperationState.Running("Extracting Archive", progress, detail)
                }
            if (_operationState.value !is OperationState.Error) {
                _operationState.value = OperationState.Success("Archive extracted")
                reloadCurrentDirectory()
                refreshCategoryIfLoaded()
            }
        }
    }

    fun inspectProperties(item: FileItem, includeChecksum: Boolean = false) {
        viewModelScope.launch {
            _isLoadingProperties.value = true
            _propertiesState.value = fileRepo.getFileProperties(item, includeChecksum)
            _isLoadingProperties.value = false
        }
    }

    fun dismissProperties() {
        _propertiesState.value = null
    }

    fun clearOperationState() {
        _operationState.value = OperationState.Idle
    }

    private fun refreshCategoryIfLoaded() {
        if (_categoryState.value.items.isNotEmpty()) {
            openCategory(_categoryState.value.category)
        }
    }

    fun recordFileOpened(item: FileItem) {
        viewModelScope.launch {
            prefsRepo.recordRecentFile(item)
        }
    }

    fun playAudioFromList(clickedItem: FileItem, contextItems: List<FileItem>) {
        recordFileOpened(clickedItem)
        val audioQueue = contextItems.filter { it.typeGroup == FileTypeGroup.AUDIO }.ifEmpty { listOf(clickedItem) }
        val startIndex = audioQueue.indexOfFirst { it.id == clickedItem.id }.coerceAtLeast(0)
        audioController.playTracks(audioQueue, startIndex, expandSheet = false)
    }

    fun toggleFavoriteCurrentFolder() {
        val state = _browserState.value
        val name = state.breadcrumbs.lastOrNull()?.label ?: "Folder"
        toggleFavorite(
            FavoriteFolder(
                name = name,
                path = state.currentPath,
                uriString = if (state.isCurrentSaf) state.currentPath else null,
                isSaf = state.isCurrentSaf
            )
        )
    }

    fun toggleFavorite(folder: FavoriteFolder) {
        viewModelScope.launch { prefsRepo.toggleFavorite(folder) }
    }

    fun removeFavorite(folder: FavoriteFolder) {
        viewModelScope.launch { prefsRepo.removeFavorite(folder) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setAccentPreset(preset: AccentPreset) {
        viewModelScope.launch { prefsRepo.setAccentPreset(preset) }
    }

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch { prefsRepo.setViewMode(mode) }
    }

    fun toggleViewMode() {
        val next = if (preferences.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        setViewMode(next)
    }

    fun setSort(option: SortOption, direction: SortDirection) {
        viewModelScope.launch {
            prefsRepo.setSort(option, direction)
            _browserState.update { state ->
                state.copy(items = fileRepo.sortItems(state.items, option, direction))
            }
            _categoryState.update { state ->
                state.copy(items = fileRepo.sortItems(state.items, option, direction))
            }
        }
    }

    fun setShowHiddenFiles(show: Boolean) {
        viewModelScope.launch {
            prefsRepo.setShowHiddenFiles(show)
            reloadCurrentDirectory()
        }
    }

    fun setShowFileExtensions(show: Boolean) {
        viewModelScope.launch { prefsRepo.setShowFileExtensions(show) }
    }

    fun setHighRefreshRate(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setHighRefreshRate(enabled) }
    }

    fun clearRecentFiles() {
        viewModelScope.launch { prefsRepo.clearRecentFiles() }
    }

    fun addSafTreeUri(uriString: String) {
        viewModelScope.launch {
            prefsRepo.addSafTreeUri(uriString)
            refreshPermissionsAndDashboard()
        }
    }

    fun removeSafTreeUri(uriString: String) {
        viewModelScope.launch {
            prefsRepo.removeSafTreeUri(uriString)
            refreshPermissionsAndDashboard()
        }
    }

    fun createShareIntent(items: List<FileItem>) = fileRepo.createShareIntent(items)
    fun createOpenExternalIntent(item: FileItem) = fileRepo.createOpenExternalIntent(item)
    fun createOpenAsIntent(item: FileItem, mimeType: String) = fileRepo.createOpenAsIntent(item, mimeType)

    class Factory(
        private val context: Context,
        private val prefsRepo: UserPreferencesRepository,
        private val fileRepo: FileOperationsRepository,
        private val audioController: AudioPlayerController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(context.applicationContext, prefsRepo, fileRepo, audioController) as T
        }
    }
}
