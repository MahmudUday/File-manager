package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppPreferences
import com.example.data.model.FileItem
import com.example.data.model.ViewMode
import com.example.ui.components.FileGridCard
import com.example.ui.components.FileListRow
import com.example.ui.components.FileListSkeleton
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.viewmodel.BrowserUiState

private val PopupMenuShape = RoundedCornerShape(20.dp)
private val ActionBarShape = RoundedCornerShape(20.dp)
private val BreadcrumbShape = RoundedCornerShape(8.dp)

@Composable
fun FileBrowserScreen(
    browserState: BrowserUiState,
    preferences: AppPreferences,
    isExpandedWidth: Boolean,
    onNavigateBack: () -> Boolean,
    onBreadcrumbClick: (path: String, isSaf: Boolean) -> Unit,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onItemMoreClick: (FileItem) -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleHiddenFiles: () -> Unit,
    onOpenSortSheet: () -> Unit,
    onToggleBookmark: () -> Unit,
    onRefresh: () -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleDeepSearch: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCopySelected: (List<FileItem>) -> Unit,
    onCutSelected: (List<FileItem>) -> Unit,
    onDeleteSelected: (List<FileItem>) -> Unit,
    onZipSelected: (List<FileItem>) -> Unit,
    onShareSelected: (List<FileItem>) -> Unit,
    onRenameSingle: (FileItem) -> Unit,
    onPropertiesSingle: (FileItem) -> Unit,
    onCreateFolderClick: () -> Unit,
    onCreateFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onyx = LocalOnyxColors.current
    var showCreateMenu by remember { mutableStateOf(false) }

    val canNavigateUp = browserState.breadcrumbs.size > 1 || browserState.isSelectionMode || browserState.isSearchActive
    BackHandler(enabled = canNavigateUp || showCreateMenu) {
        if (showCreateMenu) {
            showCreateMenu = false
        } else {
            onNavigateBack()
        }
    }

    val isBookmarked = remember(preferences.favorites, browserState.currentPath) {
        preferences.favorites.any { it.path == browserState.currentPath }
    }

    val selectedItems = remember(browserState.selectedIds, browserState.displayedItems) {
        browserState.displayedItems.filter { it.id in browserState.selectedIds }
    }

    val breadcrumbListState = rememberLazyListState()
    LaunchedEffect(browserState.breadcrumbs.size) {
        if (browserState.breadcrumbs.isNotEmpty()) {
            breadcrumbListState.animateScrollToItem(browserState.breadcrumbs.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(onyx.background)
            .testTag("file_browser_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Adaptive Top Toolbar
            AnimatedContent(
                targetState = when {
                    browserState.isSelectionMode -> 2
                    browserState.isSearchActive -> 1
                    else -> 0
                },
                transitionSpec = {
                    (slideInVertically(tween(170)) { -it / 4 } + fadeIn(tween(160)))
                        .togetherWith(fadeOut(tween(130)))
                },
                label = "browserToolbarMode"
            ) { mode ->
                when (mode) {
                    2 -> {
                        // Selection Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onClearSelection) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear selection", tint = onyx.textPrimary)
                            }
                            Text(
                                text = "${browserState.selectedIds.size} selected",
                                style = MaterialTheme.typography.titleLarge,
                                color = onyx.accent,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onSelectAll) {
                                Icon(Icons.Rounded.SelectAll, contentDescription = "Select all", tint = onyx.textPrimary)
                            }
                        }
                    }

                    1 -> {
                        // Instant Search Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = browserState.searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    placeholder = { Text("Search files & folders...", color = onyx.textTertiary) },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Search, contentDescription = null, tint = onyx.accent)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { onToggleSearch(false) }) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Close search", tint = onyx.textSecondary)
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = onyx.accent,
                                        unfocusedBorderColor = onyx.hairlineBorder,
                                        focusedTextColor = onyx.textPrimary,
                                        unfocusedTextColor = onyx.textPrimary
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("browser_search_input")
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val deep = browserState.deepSearchEnabled
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (deep) onyx.accentSoft else onyx.cardSurface)
                                        .border(
                                            0.9.dp,
                                            if (deep) onyx.glowBorder else onyx.hairlineBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(onClick = onToggleDeepSearch)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (deep) "Deep Recursive Scan: ON" else "Current Folder Only",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (deep) onyx.accent else onyx.textSecondary
                                    )
                                }
                                if (browserState.isSearching) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(3.dp),
                                        color = onyx.accent,
                                        trackColor = onyx.hairlineBorder
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // Normal Browser Header with animated folder title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedVisibility(visible = browserState.breadcrumbs.size > 1) {
                                IconButton(onClick = { onNavigateBack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Up one folder",
                                        tint = onyx.textPrimary
                                    )
                                }
                            }
                            if (browserState.breadcrumbs.size <= 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            AnimatedContent(
                                targetState = (browserState.breadcrumbs.lastOrNull()?.label ?: "Files") to browserState.breadcrumbs.size,
                                transitionSpec = {
                                    val forward = targetState.second >= initialState.second
                                    (slideInVertically(tween(200, easing = FastOutSlowInEasing)) { if (forward) it / 2 else -it / 2 } +
                                        fadeIn(tween(180)))
                                        .togetherWith(
                                            slideOutVertically(tween(150)) { if (forward) -it / 2 else it / 2 } +
                                                fadeOut(tween(140))
                                        )
                                },
                                label = "folderTitleTransition",
                                modifier = Modifier.weight(1f)
                            ) { (title, _) ->
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = onyx.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onToggleSearch(true) },
                                modifier = Modifier.testTag("browser_search_button")
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = onyx.textPrimary)
                            }
                            IconButton(
                                onClick = onToggleViewMode,
                                modifier = Modifier.testTag("browser_view_toggle")
                            ) {
                                Icon(
                                    imageVector = if (preferences.viewMode == ViewMode.LIST) {
                                        Icons.Rounded.GridView
                                    } else {
                                        Icons.AutoMirrored.Rounded.ViewList
                                    },
                                    contentDescription = "Switch view mode",
                                    tint = onyx.textPrimary
                                )
                            }
                            IconButton(
                                onClick = onToggleHiddenFiles,
                                modifier = Modifier.testTag("browser_hidden_toggle")
                            ) {
                                Icon(
                                    imageVector = if (preferences.showHiddenFiles) {
                                        Icons.Rounded.Visibility
                                    } else {
                                        Icons.Rounded.VisibilityOff
                                    },
                                    contentDescription = if (preferences.showHiddenFiles) "Hide hidden files" else "Show hidden files",
                                    tint = if (preferences.showHiddenFiles) onyx.accent else onyx.textSecondary
                                )
                            }
                            IconButton(
                                onClick = onOpenSortSheet,
                                modifier = Modifier.testTag("browser_sort_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort files", tint = onyx.textPrimary)
                            }
                            IconButton(
                                onClick = onToggleBookmark,
                                modifier = Modifier.testTag("browser_bookmark_button")
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = "Bookmark folder",
                                    tint = if (isBookmarked) onyx.accent else onyx.textSecondary
                                )
                            }
                            IconButton(onClick = onRefresh) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = onyx.textSecondary)
                            }
                        }
                    }
                }
            }

            // Breadcrumbs Path Strip with item animation
            LazyRow(
                state = breadcrumbListState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(browserState.breadcrumbs, key = { idx, seg -> "${idx}_${seg.path}" }) { idx, seg ->
                    val isLast = idx == browserState.breadcrumbs.lastIndex
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.animateItem()
                    ) {
                        Text(
                            text = seg.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isLast) onyx.accent else onyx.textSecondary,
                            modifier = Modifier
                                .clip(BreadcrumbShape)
                                .then(
                                    if (isLast) {
                                        Modifier
                                            .background(onyx.accentSoft)
                                            .border(0.75.dp, onyx.glowBorder.copy(alpha = 0.55f), BreadcrumbShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable(enabled = !isLast) { onBreadcrumbClick(seg.path, seg.isSaf) }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                        if (!isLast) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = onyx.textTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Directory Content Area with directional folder drill-down motion
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = browserState.currentPath to browserState.breadcrumbs.size,
                    transitionSpec = {
                        val forward = targetState.second >= initialState.second
                        if (targetState.first == initialState.first) {
                            fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                        } else if (forward) {
                            (slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 4 } +
                                fadeIn(tween(210)) +
                                scaleIn(initialScale = 0.96f, animationSpec = tween(240, easing = FastOutSlowInEasing)))
                                .togetherWith(
                                    slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { -it / 5 } +
                                        fadeOut(tween(160))
                                )
                        } else {
                            (slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { -it / 4 } +
                                fadeIn(tween(210)) +
                                scaleIn(initialScale = 0.96f, animationSpec = tween(240, easing = FastOutSlowInEasing)))
                                .togetherWith(
                                    slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { it / 5 } +
                                        fadeOut(tween(160))
                                )
                        }
                    },
                    label = "folderNavigationTransition",
                    modifier = Modifier.fillMaxSize()
                ) { (activePath, _) ->
                    val displayed = browserState.displayedItems
                    val listState = remember(activePath) { LazyListState() }
                    val gridState = remember(activePath) { LazyGridState() }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            browserState.isLoading && displayed.isEmpty() -> {
                                FileListSkeleton(
                                    itemCount = 8,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }

                            browserState.errorMessage != null -> {
                                EmptyBrowserState(
                                    imageRes = R.drawable.img_empty_folder,
                                    title = "Folder Unavailable",
                                    subtitle = browserState.errorMessage,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            displayed.isEmpty() -> {
                                val isSearch = browserState.isSearchActive && browserState.searchQuery.isNotBlank()
                                EmptyBrowserState(
                                    imageRes = if (isSearch) R.drawable.img_empty_search else R.drawable.img_empty_folder,
                                    title = if (isSearch) "No Matching Files" else "This Folder Is Empty",
                                    subtitle = if (isSearch) "Try a different keyword or toggle deep recursive scan." else "Tap + below to create a new folder or file.",
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            else -> {
                                AnimatedContent(
                                    targetState = preferences.viewMode,
                                    transitionSpec = {
                                        (scaleIn(initialScale = 0.97f, animationSpec = tween(180)) + fadeIn(tween(180)))
                                            .togetherWith(fadeOut(tween(140)))
                                    },
                                    label = "viewModeSwitch"
                                ) { viewMode ->
                                    if (viewMode == ViewMode.LIST) {
                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(
                                                start = 14.dp,
                                                end = 14.dp,
                                                top = 6.dp,
                                                bottom = 110.dp
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(
                                                items = displayed,
                                                key = { it.id },
                                                contentType = { it.isDirectory }
                                            ) { item ->
                                                FileListRow(
                                                    item = item,
                                                    isSelected = item.id in browserState.selectedIds,
                                                    showExtension = preferences.showFileExtensions,
                                                    onClick = { onItemClick(item) },
                                                    onLongClick = { onItemLongClick(item) },
                                                    onMoreClick = { onItemMoreClick(item) },
                                                    modifier = Modifier.animateItem()
                                                )
                                            }
                                        }
                                    } else {
                                        val gridColumns = if (isExpandedWidth) 4 else 2
                                        LazyVerticalGrid(
                                            state = gridState,
                                            columns = GridCells.Fixed(gridColumns),
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(
                                                start = 14.dp,
                                                end = 14.dp,
                                                top = 6.dp,
                                                bottom = 110.dp
                                            ),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(
                                                items = displayed,
                                                key = { it.id },
                                                contentType = { it.isDirectory }
                                            ) { item ->
                                                FileGridCard(
                                                    item = item,
                                                    isSelected = item.id in browserState.selectedIds,
                                                    showExtension = preferences.showFileExtensions,
                                                    onClick = { onItemClick(item) },
                                                    onLongClick = { onItemLongClick(item) },
                                                    onMoreClick = { onItemMoreClick(item) },
                                                    modifier = Modifier.animateItem()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Scrim when animated create popup menu is expanded
        AnimatedVisibility(
            visible = showCreateMenu && !browserState.isSelectionMode,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(130)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showCreateMenu = false }
            )
        }

        // Create Folder / File FAB + Custom Glowing Animated Popup Menu
        if (!browserState.isSelectionMode) {
            val fabRotation by animateFloatAsState(
                targetValue = if (showCreateMenu) 45f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "fabRotate"
            )
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 18.dp)
            ) {
                AnimatedVisibility(
                    visible = showCreateMenu,
                    enter = scaleIn(
                        initialScale = 0.72f,
                        transformOrigin = TransformOrigin(1f, 1f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(tween(150)),
                    exit = scaleOut(
                        targetScale = 0.80f,
                        transformOrigin = TransformOrigin(1f, 1f),
                        animationSpec = tween(120)
                    ) + fadeOut(tween(110))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .width(214.dp)
                            .clip(PopupMenuShape)
                            .background(onyx.elevatedSurface)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(onyx.accentGlow.copy(alpha = 0.42f), Color.Transparent),
                                        center = Offset(size.width * 0.85f, size.height * 0.9f),
                                        radius = size.width * 0.85f
                                    ),
                                    center = Offset(size.width * 0.85f, size.height * 0.9f),
                                    radius = size.width * 0.85f
                                )
                            }
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(onyx.glowBorder, onyx.hairlineBorder)),
                                shape = PopupMenuShape
                            )
                            .padding(vertical = 6.dp)
                    ) {
                        PopupCreateOptionRow(
                            icon = Icons.Rounded.CreateNewFolder,
                            label = "New Folder",
                            tint = onyx.accent,
                            onClick = {
                                showCreateMenu = false
                                onCreateFolderClick()
                            }
                        )
                        HorizontalDivider(
                            color = onyx.subtleBorder,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        PopupCreateOptionRow(
                            icon = Icons.Rounded.NoteAdd,
                            label = "New Empty File",
                            tint = onyx.textPrimary,
                            onClick = {
                                showCreateMenu = false
                                onCreateFileClick()
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier.drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(onyx.accentGlow.copy(alpha = 0.60f), Color.Transparent),
                                radius = size.width * 0.85f
                            ),
                            radius = size.width * 0.85f
                        )
                    }
                ) {
                    FloatingActionButton(
                        onClick = { showCreateMenu = !showCreateMenu },
                        containerColor = onyx.accent,
                        contentColor = onyx.onAccent,
                        shape = CircleShape,
                        modifier = Modifier.testTag("create_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Create new",
                            modifier = Modifier.graphicsLayer { rotationZ = fabRotation }
                        )
                    }
                }
            }
        }

        // Multi-Select Contextual Action Bar
        AnimatedVisibility(
            visible = browserState.isSelectionMode,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(tween(160)),
            exit = slideOutVertically { it } + fadeOut(tween(130)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ActionBarShape)
                    .background(onyx.elevatedSurface)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(onyx.glowBorder, onyx.hairlineBorder, onyx.glowBorder)),
                        shape = ActionBarShape
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionActionButton(Icons.Rounded.ContentCopy, "Copy") { onCopySelected(selectedItems) }
                SelectionActionButton(Icons.Rounded.ContentCut, "Move") { onCutSelected(selectedItems) }
                SelectionActionButton(Icons.Rounded.FolderZip, "Zip") { onZipSelected(selectedItems) }
                if (selectedItems.none { it.isDirectory }) {
                    SelectionActionButton(Icons.Rounded.Share, "Share") { onShareSelected(selectedItems) }
                }
                if (selectedItems.size == 1) {
                    SelectionActionButton(Icons.Rounded.DriveFileRenameOutline, "Rename") { onRenameSingle(selectedItems.first()) }
                    SelectionActionButton(Icons.Rounded.Info, "Info") { onPropertiesSingle(selectedItems.first()) }
                }
                SelectionActionButton(Icons.Rounded.DeleteOutline, "Delete", tint = ErrorCrimson) {
                    onDeleteSelected(selectedItems)
                }
            }
        }
    }
}

@Composable
private fun PopupCreateOptionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val onyx = LocalOnyxColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(21.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = onyx.textPrimary
        )
    }
}

@Composable
fun SelectionActionButton(
    icon: ImageVector,
    label: String,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val onyx = LocalOnyxColors.current
    val resolvedTint = tint ?: onyx.textPrimary
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = resolvedTint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = resolvedTint)
    }
}

@Composable
fun EmptyBrowserState(
    imageRes: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val onyx = LocalOnyxColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, onyx.glowBorder.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = onyx.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = onyx.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
