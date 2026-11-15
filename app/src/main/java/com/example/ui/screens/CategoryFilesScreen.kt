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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.MonoMetadataStyle
import com.example.ui.viewmodel.CategoryUiState

@Composable
fun CategoryFilesScreen(
    categoryState: CategoryUiState,
    preferences: AppPreferences,
    isExpandedWidth: Boolean,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenSortSheet: () -> Unit,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onItemMoreClick: (FileItem) -> Unit,
    onClearSelection: () -> Unit,
    onCopySelected: (List<FileItem>) -> Unit,
    onShareSelected: (List<FileItem>) -> Unit,
    onPropertiesSingle: (FileItem) -> Unit,
    onDeleteSelected: (List<FileItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val onyx = LocalOnyxColors.current
    var showSearchBar by remember { mutableStateOf(false) }

    BackHandler {
        when {
            categoryState.selectedIds.isNotEmpty() -> onClearSelection()
            showSearchBar -> {
                showSearchBar = false
                onSearchChange("")
            }
            else -> onBack()
        }
    }

    val displayed = categoryState.filteredItems
    val selectedItems = remember(categoryState.selectedIds, displayed) {
        displayed.filter { it.id in categoryState.selectedIds }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(onyx.background)
            .testTag("category_files_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = onyx.textPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryState.category.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = onyx.textPrimary
                    )
                    Text(
                        text = "${displayed.size} files",
                        style = MonoMetadataStyle,
                        color = onyx.textSecondary
                    )
                }
                IconButton(onClick = {
                    showSearchBar = !showSearchBar
                    if (!showSearchBar) onSearchChange("")
                }) {
                    Icon(
                        imageVector = if (showSearchBar) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = "Search category",
                        tint = onyx.textPrimary
                    )
                }
                IconButton(onClick = onToggleViewMode) {
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
                IconButton(onClick = onOpenSortSheet) {
                    Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort", tint = onyx.textPrimary)
                }
            }

            AnimatedVisibility(visible = showSearchBar) {
                OutlinedTextField(
                    value = categoryState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Filter ${categoryState.category.label.lowercase()}...", color = onyx.textTertiary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = onyx.accent,
                        unfocusedBorderColor = onyx.hairlineBorder,
                        focusedTextColor = onyx.textPrimary,
                        unfocusedTextColor = onyx.textPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    categoryState.isLoading -> {
                        FileListSkeleton(
                            itemCount = 7,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    displayed.isEmpty() -> {
                        EmptyBrowserState(
                            imageRes = R.drawable.img_empty_search,
                            title = "No ${categoryState.category.label} Found",
                            subtitle = "Files matching this category will appear here automatically.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        AnimatedContent(
                            targetState = preferences.viewMode,
                            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(140)) },
                            label = "categoryViewMode"
                        ) { viewMode ->
                            if (viewMode == ViewMode.LIST) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 100.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(displayed, key = { it.id }) { item ->
                                        FileListRow(
                                            item = item,
                                            isSelected = item.id in categoryState.selectedIds,
                                            showExtension = preferences.showFileExtensions,
                                            onClick = { onItemClick(item) },
                                            onLongClick = { onItemLongClick(item) },
                                            onMoreClick = { onItemMoreClick(item) },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            } else {
                                val cols = if (isExpandedWidth) 4 else 2
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(cols),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 100.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(displayed, key = { it.id }) { item ->
                                        FileGridCard(
                                            item = item,
                                            isSelected = item.id in categoryState.selectedIds,
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

        AnimatedVisibility(
            visible = categoryState.selectedIds.isNotEmpty(),
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
                    .border(1.dp, onyx.glowBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SelectionActionButton(Icons.Rounded.ContentCopy, "Copy") { onCopySelected(selectedItems) }
                SelectionActionButton(Icons.Rounded.Share, "Share") { onShareSelected(selectedItems) }
                if (selectedItems.size == 1) {
                    SelectionActionButton(Icons.Rounded.Info, "Info") { onPropertiesSingle(selectedItems.first()) }
                }
                SelectionActionButton(Icons.Rounded.DeleteOutline, "Delete", tint = ErrorCrimson) {
                    onDeleteSelected(selectedItems)
                }
            }
        }
    }
}
