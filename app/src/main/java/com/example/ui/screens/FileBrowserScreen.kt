package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
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
    BackHandler(enabled = canNavigateUp) {
        onNavigateBack()
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
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
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
                                            0.8.dp,
                                            if (deep) onyx.accent else onyx.hairlineBorder,
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
                        // Normal Browser Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (browserState.breadcrumbs.size > 1) {
                                IconButton(onClick = { onNavigateBack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Up one folder",
                                        tint = onyx.textPrimary
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = browserState.breadcrumbs.lastOrNull()?.label ?: "Files",
                                style = MaterialTheme.typography.titleLarge,
                                color = onyx.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
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

            // Breadcrumbs Path Strip
            LazyRow(
                state = breadcrumbListState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(browserState.breadcrumbs, key = { idx, seg -> "${idx}_${seg.path}" }) { idx, seg ->
                    val isLast = idx == browserState.breadcrumbs.lastIndex
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = seg.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isLast) onyx.accent else onyx.textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isLast) { onBreadcrumbClick(seg.path, seg.isSaf) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
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

            // Directory Content Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val displayed = browserState.displayedItems
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
                                fadeIn(tween(180)) togetherWith fadeOut(tween(150))
                            },
                            label = "viewModeSwitch"
                        ) { viewMode ->
                            if (viewMode == ViewMode.LIST) {
                                LazyColumn(
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
                                            onMoreClick = { onItemMoreClick(item) }
                                        )
                                    }
                                }
                            } else {
                                val gridColumns = if (isExpandedWidth) 4 else 2
                                LazyVerticalGrid(
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
                                            onMoreClick = { onItemMoreClick(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Create Folder / File FAB
        if (!browserState.isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 18.dp)
            ) {
                FloatingActionButton(
                    onClick = { showCreateMenu = true },
                    containerColor = onyx.accent,
                    contentColor = Color(0xFF05070B),
                    shape = CircleShape,
                    modifier = Modifier.testTag("create_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Create new")
                }

                DropdownMenu(
                    expanded = showCreateMenu,
                    onDismissRequest = { showCreateMenu = false },
                    containerColor = onyx.elevatedSurface
                ) {
                    DropdownMenuItem(
                        text = { Text("New Folder", color = onyx.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, tint = onyx.accent) },
                        onClick = {
                            showCreateMenu = false
                            onCreateFolderClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Empty File", color = onyx.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.NoteAdd, contentDescription = null, tint = onyx.textPrimary) },
                        onClick = {
                            showCreateMenu = false
                            onCreateFileClick()
                        }
                    )
                }
            }
        }

        // Multi-Select Contextual Action Bar
        AnimatedVisibility(
            visible = browserState.isSelectionMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(onyx.elevatedSurface)
                    .border(1.dp, onyx.hairlineBorder, RoundedCornerShape(20.dp))
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
