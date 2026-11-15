package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentPlatinum
import com.example.ui.theme.AccentViolet
import kotlinx.serialization.Serializable

enum class FileCategory(val label: String) {
    RECENT("Recent"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    DOWNLOADS("Downloads"),
    APKS("APK Files"),
    ARCHIVES("Archives"),
    LARGE_FILES("Large Files")
}

enum class FileTypeGroup {
    FOLDER,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    APK,
    CODE,
    OTHER
}

@Immutable
data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val uriString: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String,
    val extension: String,
    val isHidden: Boolean,
    val childCount: Int = 0,
    val isSafDocument: Boolean = false,
    val typeGroup: FileTypeGroup = FileTypeGroup.OTHER,
    val formattedSize: String = "",
    val formattedDate: String = ""
) {
    val displayNameWithoutExtension: String
        get() = if (isDirectory || extension.isEmpty() || !name.contains('.')) {
            name
        } else {
            name.substringBeforeLast('.')
        }
}

@Immutable
data class StorageVolumeInfo(
    val id: String,
    val name: String,
    val subtitle: String,
    val path: String,
    val uriString: String?,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val isRemovable: Boolean,
    val isSafMounted: Boolean = false
) {
    val usedFraction: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

@Immutable
data class CategorySummary(
    val category: FileCategory,
    val count: Int = 0,
    val totalBytes: Long = 0L
)

enum class SortOption(val label: String) {
    NAME("Name"),
    SIZE("Size"),
    DATE("Date"),
    TYPE("Type")
}

enum class SortDirection(val label: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

enum class ViewMode(val label: String) {
    LIST("List"),
    GRID("Grid")
}

enum class ThemeMode(val label: String, val description: String) {
    SYSTEM("System Default", "Automatically match your device Dark or Light theme"),
    PURE_AMOLED("Pure AMOLED", "True #000000 pitch-black canvas for OLED power efficiency"),
    DEEP_OBSIDIAN("Deep Obsidian", "Subtle slate-black layers with soft ambient contrast"),
    LIGHT("Light Mode", "Crisp daylight surfaces with luminous contrast")
}

enum class AccentPreset(val label: String, val color: Color) {
    CYAN("Electric Cyan", AccentCyan),
    EMERALD("Emerald Pulse", AccentEmerald),
    AMBER("Amber Gold", AccentAmber),
    VIOLET("Cobalt Violet", AccentViolet),
    CORAL("Crimson Coral", AccentCoral),
    PLATINUM("Platinum", AccentPlatinum)
}

@Serializable
data class FavoriteFolder(
    val name: String,
    val path: String,
    val uriString: String? = null,
    val isSaf: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Serializable
data class RecentFileEntry(
    val name: String,
    val path: String,
    val uriString: String,
    val mimeType: String,
    val size: Long,
    val openedAt: Long = System.currentTimeMillis()
)

@Immutable
data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.PURE_AMOLED,
    val accentPreset: AccentPreset = AccentPreset.CYAN,
    val viewMode: ViewMode = ViewMode.LIST,
    val sortOption: SortOption = SortOption.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val showHiddenFiles: Boolean = false,
    val showFileExtensions: Boolean = true,
    val highRefreshRate: Boolean = true,
    val favorites: List<FavoriteFolder> = emptyList(),
    val recentFiles: List<RecentFileEntry> = emptyList(),
    val safTreeUris: List<String> = emptyList()
)

enum class ClipboardMode { COPY, CUT }

@Immutable
data class ClipboardState(
    val mode: ClipboardMode = ClipboardMode.COPY,
    val items: List<FileItem> = emptyList()
) {
    val isActive: Boolean get() = items.isNotEmpty()
}

@Immutable
data class FileProperties(
    val item: FileItem,
    val permissionsText: String,
    val itemCountInFolder: Int? = null,
    val folderTotalSize: Long? = null,
    val mediaWidth: Int? = null,
    val mediaHeight: Int? = null,
    val mediaDurationMs: Long? = null,
    val apkPackageName: String? = null,
    val apkVersionName: String? = null,
    val sha256: String? = null
)

sealed interface OperationState {
    data object Idle : OperationState
    data class Running(val title: String, val progress: Float, val detail: String) : OperationState
    data class Success(val message: String) : OperationState
    data class Error(val message: String) : OperationState
}
