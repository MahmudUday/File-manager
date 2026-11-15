package com.example.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.model.ClipboardMode
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.FileTypeGroup
import com.example.data.model.OperationState
import com.example.ui.components.ClipboardFloatingBar
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.FileActionBottomSheet
import com.example.ui.components.FilePropertiesSheet
import com.example.ui.components.MiniAudioPlayerBar
import com.example.ui.components.OpenAsBottomSheet
import com.example.ui.components.OperationProgressDialog
import com.example.ui.components.SortBottomSheet
import com.example.ui.components.TextInputDialog
import com.example.data.model.ThemeMode
import com.example.ui.media.AudioPlayerSheet
import com.example.ui.media.ImageViewerScreen
import com.example.ui.media.VideoPlayerScreen
import com.example.ui.screens.CategoryFilesScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.FileBrowserScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.OnyxFilesTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.PermissionHelper
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object FilesRoute
@Serializable data object FavoritesRoute
@Serializable data object SettingsRoute
@Serializable data class CategoryRoute(val categoryName: String)

private data class TopTab(
    val label: String,
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopTabs = listOf(
    TopTab("Home", HomeRoute, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    TopTab("Files", FilesRoute, Icons.Rounded.Folder, Icons.Outlined.Folder, "nav_files"),
    TopTab("Favorites", FavoritesRoute, Icons.Rounded.Star, Icons.Outlined.StarOutline, "nav_favorites"),
    TopTab("Settings", SettingsRoute, Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun OnyxFilesApp(
    viewModel: MainViewModel,
    widthSizeClass: WindowWidthSizeClass,
    maxRefreshRateHz: Int = 60,
    onApplyRefreshRate: (Boolean) -> Unit = {},
    onApplySystemBarTheme: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val browserState by viewModel.browserState.collectAsStateWithLifecycle()
    val categoryState by viewModel.categoryState.collectAsStateWithLifecycle()
    val clipboard by viewModel.clipboard.collectAsStateWithLifecycle()
    val operationState by viewModel.operationState.collectAsStateWithLifecycle()
    val propertiesState by viewModel.propertiesState.collectAsStateWithLifecycle()
    val isLoadingProperties by viewModel.isLoadingProperties.collectAsStateWithLifecycle()
    val audioState by viewModel.audioController.state.collectAsStateWithLifecycle()

    val isExpandedWidth = widthSizeClass != WindowWidthSizeClass.Compact
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // Media viewer overlays
    var imageViewerData by remember { mutableStateOf<Pair<List<FileItem>, Int>?>(null) }
    var activeVideoItem by remember { mutableStateOf<FileItem?>(null) }

    // Modal sheets & dialogs
    var actionSheetItem by remember { mutableStateOf<FileItem?>(null) }
    var openAsTarget by remember { mutableStateOf<FileItem?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var deleteTargets by remember { mutableStateOf<List<FileItem>?>(null) }
    var zipTargets by remember { mutableStateOf<List<FileItem>?>(null) }

    // Apply 120Hz / preferred display refresh rate whenever user preference changes
    LaunchedEffect(preferences.highRefreshRate) {
        onApplyRefreshRate(preferences.highRefreshRate)
    }

    // Apply Light vs Dark status/navigation bar contrast dynamically
    LaunchedEffect(preferences.themeMode) {
        onApplySystemBarTheme(preferences.themeMode == ThemeMode.LIGHT)
    }

    // Refresh permissions lightly when user returns from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onApplyRefreshRate(preferences.highRefreshRate)
                viewModel.refreshOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val mediaPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissionsAndDashboard()
        viewModel.reloadCurrentDirectory()
    }

    val safTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.addSafTreeUri(uri.toString())
        }
    }

    // Request runtime media/notification permissions once if needed
    LaunchedEffect(Unit) {
        if (!PermissionHelper.hasAnyStorageAccess(context)) {
            mediaPermissionsLauncher.launch(PermissionHelper.requiredRuntimePermissions())
        }
    }

    // Snackbar feedback for completed/failed file operations
    LaunchedEffect(operationState) {
        when (val s = operationState) {
            is OperationState.Success -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.clearOperationState()
            }
            is OperationState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.clearOperationState()
            }
            else -> Unit
        }
    }

    val requestAllFilesAccess: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            runCatching { context.startActivity(intent) }.onFailure {
                context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            mediaPermissionsLauncher.launch(PermissionHelper.requiredRuntimePermissions())
        }
    }

    val navigateToTab: (Any) -> Unit = { route ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val shareItems: (List<FileItem>) -> Unit = { items ->
        runCatching {
            val intent = viewModel.createShareIntent(items)
            context.startActivity(Intent.createChooser(intent, "Share via"))
        }.onFailure {
            Toast.makeText(context, "Unable to share selected file(s)", Toast.LENGTH_SHORT).show()
        }
    }

    val openAsMime: (FileItem, String) -> Unit = { item, mimeType ->
        runCatching {
            viewModel.recordFileOpened(item)
            val intent = Intent.createChooser(viewModel.createOpenAsIntent(item, mimeType), "Open as...")
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "No app available to open ${item.name} as $mimeType", Toast.LENGTH_SHORT).show()
        }
    }

    val openExternal: (FileItem) -> Unit = { item ->
        runCatching {
            viewModel.recordFileOpened(item)
            context.startActivity(viewModel.createOpenExternalIntent(item))
        }.onFailure {
            openAsTarget = item
        }
    }

    val handleFileOpen: (FileItem, List<FileItem>) -> Unit = { item, siblingList ->
        if (item.isDirectory) {
            viewModel.openDirectory(item.path, isSaf = item.isSafDocument)
        } else if (item.extension.isBlank()) {
            openAsTarget = item
        } else {
            viewModel.recordFileOpened(item)
            when (item.typeGroup) {
                FileTypeGroup.IMAGE -> {
                    val images = siblingList.filter { it.typeGroup == FileTypeGroup.IMAGE }
                        .ifEmpty { listOf(item) }
                    val index = images.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                    imageViewerData = images to index
                }
                FileTypeGroup.VIDEO -> {
                    activeVideoItem = item
                }
                FileTypeGroup.AUDIO -> {
                    viewModel.playAudioFromList(item, siblingList)
                }
                FileTypeGroup.ARCHIVE -> {
                    actionSheetItem = item
                }
                else -> {
                    openExternal(item)
                }
            }
        }
    }

    OnyxFilesTheme(
        themeMode = preferences.themeMode,
        accentPreset = preferences.accentPreset
    ) {
        val onyx = LocalOnyxColors.current
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDest = navBackStackEntry?.destination

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(onyx.background)
        ) {
            Scaffold(
                containerColor = onyx.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ClipboardFloatingBar(
                            clipboard = clipboard,
                            onPasteHere = {
                                viewModel.pasteHere()
                                navigateToTab(FilesRoute)
                            },
                            onCancel = { viewModel.clearClipboard() }
                        )
                        MiniAudioPlayerBar(
                            state = audioState,
                            onExpand = { viewModel.audioController.setSheetExpanded(true) },
                            onTogglePlayPause = { viewModel.audioController.togglePlayPause() },
                            onNext = { viewModel.audioController.skipNext() },
                            onDismiss = { viewModel.audioController.stopAndDismiss() }
                        )
                        if (!isExpandedWidth) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                onyx.glowBorder,
                                                onyx.accent.copy(alpha = 0.72f),
                                                onyx.glowBorder,
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            NavigationBar(
                                containerColor = onyx.background,
                                tonalElevation = 0.dp
                            ) {
                                TopTabs.forEach { tab ->
                                    val selected = currentDest?.hasRoute(tab.route::class) == true
                                    val iconScale by animateFloatAsState(
                                        targetValue = if (selected) 1.15f else 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "navIconScale"
                                    )
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label,
                                                modifier = Modifier.graphicsLayer {
                                                    scaleX = iconScale
                                                    scaleY = iconScale
                                                }
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = onyx.accent,
                                            selectedTextColor = onyx.accent,
                                            indicatorColor = onyx.accentSoft,
                                            unselectedIconColor = onyx.textSecondary,
                                            unselectedTextColor = onyx.textSecondary
                                        ),
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedWidth) {
                        NavigationRail(
                            containerColor = onyx.background,
                            modifier = Modifier
                                .fillMaxHeight()
                                .windowInsetsPadding(WindowInsets.statusBars)
                        ) {
                            TopTabs.forEach { tab ->
                                val selected = currentDest?.hasRoute(tab.route::class) == true
                                val iconScale by animateFloatAsState(
                                    targetValue = if (selected) 1.15f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "railIconScale"
                                )
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                            modifier = Modifier.graphicsLayer {
                                                scaleX = iconScale
                                                scaleY = iconScale
                                            }
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = onyx.accent,
                                        selectedTextColor = onyx.accent,
                                        indicatorColor = onyx.accentSoft,
                                        unselectedIconColor = onyx.textSecondary,
                                        unselectedTextColor = onyx.textSecondary
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                        VerticalDivider(thickness = 0.85.dp, color = onyx.glowBorder.copy(alpha = 0.45f))
                    }

                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        enterTransition = {
                            fadeIn(tween(190, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.98f, animationSpec = tween(190, easing = FastOutSlowInEasing))
                        },
                        exitTransition = { fadeOut(tween(140, easing = FastOutSlowInEasing)) },
                        popEnterTransition = {
                            fadeIn(tween(190, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.98f, animationSpec = tween(190, easing = FastOutSlowInEasing))
                        },
                        popExitTransition = { fadeOut(tween(140, easing = FastOutSlowInEasing)) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .let {
                                if (isExpandedWidth) it else it.windowInsetsPadding(WindowInsets.statusBars)
                            }
                    ) {
                        composable<HomeRoute> {
                            HomeDashboardScreen(
                                dashboardState = dashboardState,
                                preferences = preferences,
                                isExpandedWidth = isExpandedWidth,
                                onVolumeClick = { volume ->
                                    viewModel.openVolume(volume)
                                    navigateToTab(FilesRoute)
                                },
                                onCategoryClick = { category ->
                                    viewModel.openCategory(category)
                                    navController.navigate(CategoryRoute(category.name))
                                },
                                onFavoriteClick = { fav ->
                                    viewModel.openDirectory(
                                        path = fav.uriString ?: fav.path,
                                        isSaf = fav.isSaf,
                                        rootLabel = fav.name,
                                        rootPath = fav.uriString ?: fav.path
                                    )
                                    navigateToTab(FilesRoute)
                                },
                                onRecentClick = { recent ->
                                    val ext = recent.name.substringAfterLast('.', "")
                                    val tempItem = FileItem(
                                        id = recent.path,
                                        name = recent.name,
                                        path = recent.path,
                                        uriString = recent.uriString,
                                        isDirectory = false,
                                        size = recent.size,
                                        lastModified = recent.openedAt,
                                        mimeType = recent.mimeType,
                                        extension = ext,
                                        isHidden = recent.name.startsWith("."),
                                        typeGroup = com.example.util.FileFormatters.classifyFileType(
                                            false,
                                            ext,
                                            recent.mimeType
                                        )
                                    )
                                    handleFileOpen(tempItem, listOf(tempItem))
                                },
                                onSearchClick = {
                                    viewModel.setSearchActive(true)
                                    navigateToTab(FilesRoute)
                                },
                                onRequestAllFilesAccess = requestAllFilesAccess,
                                onRequestMediaPermissions = {
                                    mediaPermissionsLauncher.launch(PermissionHelper.requiredRuntimePermissions())
                                },
                                onMountSafDrive = { safTreeLauncher.launch(null) }
                            )
                        }

                        composable<FilesRoute> {
                            FileBrowserScreen(
                                browserState = browserState,
                                preferences = preferences,
                                isExpandedWidth = isExpandedWidth,
                                onNavigateBack = { viewModel.navigateUpDirectory() },
                                onBreadcrumbClick = { path, isSaf ->
                                    viewModel.openDirectory(path, isSaf)
                                },
                                onItemClick = { item ->
                                    if (browserState.isSelectionMode) {
                                        viewModel.toggleSelection(item.id)
                                    } else {
                                        handleFileOpen(item, browserState.displayedItems)
                                    }
                                },
                                onItemLongClick = { item ->
                                    viewModel.toggleSelection(item.id)
                                },
                                onItemMoreClick = { item ->
                                    actionSheetItem = item
                                },
                                onToggleViewMode = { viewModel.toggleViewMode() },
                                onToggleHiddenFiles = {
                                    val next = !preferences.showHiddenFiles
                                    viewModel.setShowHiddenFiles(next)
                                    Toast.makeText(
                                        context,
                                        if (next) "Hidden files visible" else "Hidden files hidden",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onOpenSortSheet = { showSortSheet = true },
                                onToggleBookmark = { viewModel.toggleFavoriteCurrentFolder() },
                                onRefresh = { viewModel.reloadCurrentDirectory() },
                                onToggleSearch = { active -> viewModel.setSearchActive(active) },
                                onSearchQueryChange = { q -> viewModel.updateSearchQuery(q) },
                                onToggleDeepSearch = { viewModel.toggleDeepSearch() },
                                onSelectAll = { viewModel.selectAllDisplayed() },
                                onClearSelection = { viewModel.clearSelection() },
                                onCopySelected = { items -> viewModel.setClipboard(ClipboardMode.COPY, items) },
                                onCutSelected = { items -> viewModel.setClipboard(ClipboardMode.CUT, items) },
                                onDeleteSelected = { items -> deleteTargets = items },
                                onZipSelected = { items -> zipTargets = items },
                                onShareSelected = shareItems,
                                onRenameSingle = { item -> renameTarget = item },
                                onPropertiesSingle = { item -> viewModel.inspectProperties(item) },
                                onCreateFolderClick = { showCreateFolderDialog = true },
                                onCreateFileClick = { showCreateFileDialog = true }
                            )
                        }

                        composable<CategoryRoute>(
                            enterTransition = {
                                slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 4 } +
                                    fadeIn(tween(220)) +
                                    scaleIn(initialScale = 0.97f, animationSpec = tween(260, easing = FastOutSlowInEasing))
                            },
                            exitTransition = {
                                fadeOut(tween(160))
                            },
                            popEnterTransition = {
                                fadeIn(tween(180))
                            },
                            popExitTransition = {
                                slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 4 } +
                                    fadeOut(tween(180))
                            }
                        ) { backStackEntry ->
                            val route = backStackEntry.toRoute<CategoryRoute>()
                            val category = remember(route.categoryName) {
                                runCatching { FileCategory.valueOf(route.categoryName) }
                                    .getOrDefault(FileCategory.IMAGES)
                            }
                            LaunchedEffect(category) {
                                if (categoryState.category != category) {
                                    viewModel.openCategory(category)
                                }
                            }
                            CategoryFilesScreen(
                                categoryState = categoryState,
                                preferences = preferences,
                                isExpandedWidth = isExpandedWidth,
                                onBack = { navController.popBackStack() },
                                onSearchChange = { viewModel.updateCategorySearch(it) },
                                onToggleViewMode = { viewModel.toggleViewMode() },
                                onOpenSortSheet = { showSortSheet = true },
                                onItemClick = { item ->
                                    if (categoryState.selectedIds.isNotEmpty()) {
                                        viewModel.toggleCategorySelection(item.id)
                                    } else {
                                        handleFileOpen(item, categoryState.filteredItems)
                                    }
                                },
                                onItemLongClick = { item -> viewModel.toggleCategorySelection(item.id) },
                                onItemMoreClick = { item -> actionSheetItem = item },
                                onClearSelection = { viewModel.clearCategorySelection() },
                                onCopySelected = { items -> viewModel.setClipboard(ClipboardMode.COPY, items) },
                                onShareSelected = shareItems,
                                onPropertiesSingle = { item -> viewModel.inspectProperties(item) },
                                onDeleteSelected = { items -> deleteTargets = items }
                            )
                        }

                        composable<FavoritesRoute> {
                            FavoritesScreen(
                                preferences = preferences,
                                onOpenFavorite = { fav ->
                                    viewModel.openDirectory(
                                        path = fav.uriString ?: fav.path,
                                        isSaf = fav.isSaf,
                                        rootLabel = fav.name,
                                        rootPath = fav.uriString ?: fav.path
                                    )
                                    navigateToTab(FilesRoute)
                                },
                                onRemoveFavorite = { viewModel.removeFavorite(it) },
                                onOpenRecent = { recent ->
                                    val ext = recent.name.substringAfterLast('.', "")
                                    val item = FileItem(
                                        id = recent.path,
                                        name = recent.name,
                                        path = recent.path,
                                        uriString = recent.uriString,
                                        isDirectory = false,
                                        size = recent.size,
                                        lastModified = recent.openedAt,
                                        mimeType = recent.mimeType,
                                        extension = ext,
                                        isHidden = recent.name.startsWith("."),
                                        typeGroup = com.example.util.FileFormatters.classifyFileType(
                                            false,
                                            ext,
                                            recent.mimeType
                                        )
                                    )
                                    handleFileOpen(item, listOf(item))
                                },
                                onClearRecents = { viewModel.clearRecentFiles() }
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                preferences = preferences,
                                hasAllFilesAccess = dashboardState.hasAllFilesAccess,
                                maxRefreshRateHz = maxRefreshRateHz,
                                onSelectThemeMode = { viewModel.setThemeMode(it) },
                                onSelectAccent = { viewModel.setAccentPreset(it) },
                                onToggleHiddenFiles = { viewModel.setShowHiddenFiles(it) },
                                onToggleFileExtensions = { viewModel.setShowFileExtensions(it) },
                                onSelectViewMode = { viewModel.setViewMode(it) },
                                onToggleHighRefreshRate = { viewModel.setHighRefreshRate(it) },
                                onRequestAllFilesAccess = requestAllFilesAccess,
                                onMountSafDrive = { safTreeLauncher.launch(null) },
                                onRemoveSafDrive = { viewModel.removeSafTreeUri(it) },
                                onClearRecentHistory = { viewModel.clearRecentFiles() }
                            )
                        }
                    }
                }
            }

            // Fullscreen Built-in Image Viewer Overlay
            AnimatedVisibility(
                visible = imageViewerData != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(180))
            ) {
                imageViewerData?.let { (images, startIndex) ->
                    ImageViewerScreen(
                        images = images,
                        initialIndex = startIndex,
                        onClose = { imageViewerData = null },
                        onShare = { shareItems(listOf(it)) },
                        onInfo = { viewModel.inspectProperties(it) },
                        onDelete = { target ->
                            deleteTargets = listOf(target)
                            imageViewerData = null
                        }
                    )
                }
            }

            // Fullscreen Built-in Media3 Video Player Overlay
            AnimatedVisibility(
                visible = activeVideoItem != null,
                enter = slideInVertically { it / 6 } + fadeIn(tween(220)),
                exit = slideOutVertically { it / 6 } + fadeOut(tween(180))
            ) {
                activeVideoItem?.let { video ->
                    VideoPlayerScreen(
                        videoItem = video,
                        onClose = { activeVideoItem = null }
                    )
                }
            }

            // Expanded Audio Player Modal Sheet
            if (audioState.isSheetExpanded && audioState.currentTrack != null) {
                AudioPlayerSheet(
                    state = audioState,
                    onDismiss = { viewModel.audioController.setSheetExpanded(false) },
                    onTogglePlayPause = { viewModel.audioController.togglePlayPause() },
                    onNext = { viewModel.audioController.skipNext() },
                    onPrevious = { viewModel.audioController.skipPrevious() },
                    onSeek = { viewModel.audioController.seekTo(it) },
                    onToggleShuffle = { viewModel.audioController.toggleShuffle() },
                    onCycleRepeat = { viewModel.audioController.cycleRepeatMode() },
                    onSelectQueueIndex = { viewModel.audioController.playQueueIndex(it) }
                )
            }

            // Modals & Dialogs
            actionSheetItem?.let { item ->
                FileActionBottomSheet(
                    item = item,
                    onOpen = { handleFileOpen(item, browserState.displayedItems) },
                    onOpenExternal = { openExternal(item) },
                    onOpenAs = { openAsTarget = item },
                    onShare = { shareItems(listOf(item)) },
                    onCopy = { viewModel.setClipboard(ClipboardMode.COPY, listOf(item)) },
                    onCut = { viewModel.setClipboard(ClipboardMode.CUT, listOf(item)) },
                    onRename = { renameTarget = item },
                    onCompress = { zipTargets = listOf(item) },
                    onExtract = { viewModel.extractZip(item) },
                    onProperties = { viewModel.inspectProperties(item) },
                    onDelete = { deleteTargets = listOf(item) },
                    onDismiss = { actionSheetItem = null }
                )
            }

            openAsTarget?.let { target ->
                OpenAsBottomSheet(
                    item = target,
                    onSelectMimeType = { mime -> openAsMime(target, mime) },
                    onDismiss = { openAsTarget = null }
                )
            }

            if (showSortSheet) {
                SortBottomSheet(
                    currentOption = preferences.sortOption,
                    currentDirection = preferences.sortDirection,
                    onSelect = { opt, dir ->
                        viewModel.setSort(opt, dir)
                        showSortSheet = false
                    },
                    onDismiss = { showSortSheet = false }
                )
            }

            if (showCreateFolderDialog) {
                TextInputDialog(
                    title = "Create Folder",
                    initialValue = "",
                    placeholder = "Folder name",
                    confirmLabel = "Create",
                    onConfirm = { name ->
                        viewModel.createFolder(name)
                        showCreateFolderDialog = false
                    },
                    onDismiss = { showCreateFolderDialog = false }
                )
            }

            if (showCreateFileDialog) {
                TextInputDialog(
                    title = "Create File",
                    initialValue = "notes.txt",
                    placeholder = "File name",
                    confirmLabel = "Create",
                    onConfirm = { name ->
                        viewModel.createFile(name)
                        showCreateFileDialog = false
                    },
                    onDismiss = { showCreateFileDialog = false }
                )
            }

            renameTarget?.let { target ->
                TextInputDialog(
                    title = "Rename",
                    initialValue = target.name,
                    placeholder = "New name",
                    confirmLabel = "Rename",
                    onConfirm = { newName ->
                        viewModel.renameItem(target, newName)
                        renameTarget = null
                    },
                    onDismiss = { renameTarget = null }
                )
            }

            zipTargets?.let { targets ->
                val defaultArchive = if (targets.size == 1) {
                    "${targets.first().displayNameWithoutExtension}.zip"
                } else {
                    "Archive_${System.currentTimeMillis() / 1000}.zip"
                }
                TextInputDialog(
                    title = "Compress to ZIP",
                    initialValue = defaultArchive,
                    placeholder = "archive.zip",
                    confirmLabel = "Compress",
                    onConfirm = { archiveName ->
                        viewModel.compressItemsToZip(targets, archiveName)
                        zipTargets = null
                    },
                    onDismiss = { zipTargets = null }
                )
            }

            deleteTargets?.let { targets ->
                DeleteConfirmDialog(
                    count = targets.size,
                    firstName = targets.firstOrNull()?.name.orEmpty(),
                    onConfirm = {
                        viewModel.deleteItems(targets)
                        deleteTargets = null
                    },
                    onDismiss = { deleteTargets = null }
                )
            }

            propertiesState?.let { props ->
                FilePropertiesSheet(
                    properties = props,
                    isLoadingChecksum = isLoadingProperties,
                    onComputeChecksum = { viewModel.inspectProperties(props.item, includeChecksum = true) },
                    onDismiss = { viewModel.dismissProperties() }
                )
            }

            if (operationState is OperationState.Running) {
                OperationProgressDialog(operationState as OperationState.Running)
            }
        }
    }
}
