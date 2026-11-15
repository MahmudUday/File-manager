package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppPreferences
import com.example.data.model.CategorySummary
import com.example.data.model.FavoriteFolder
import com.example.data.model.FileCategory
import com.example.data.model.RecentFileEntry
import com.example.data.model.StorageVolumeInfo
import com.example.ui.components.StorageVolumeCard
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.MonoMetadataStyle
import com.example.ui.theme.TypeApkColor
import com.example.ui.theme.TypeArchiveColor
import com.example.ui.theme.TypeAudioColor
import com.example.ui.theme.TypeDocumentColor
import com.example.ui.theme.TypeImageColor
import com.example.ui.theme.TypeVideoColor
import com.example.ui.viewmodel.DashboardUiState
import com.example.util.FileFormatters

@Composable
fun HomeDashboardScreen(
    dashboardState: DashboardUiState,
    preferences: AppPreferences,
    isExpandedWidth: Boolean,
    onVolumeClick: (StorageVolumeInfo) -> Unit,
    onCategoryClick: (FileCategory) -> Unit,
    onFavoriteClick: (FavoriteFolder) -> Unit,
    onRecentClick: (RecentFileEntry) -> Unit,
    onSearchClick: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onRequestMediaPermissions: () -> Unit,
    onMountSafDrive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onyx = LocalOnyxColors.current
    val summaryMap = remember(dashboardState.categorySummaries) {
        dashboardState.categorySummaries.associateBy { it.category }
    }
    val revealProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        revealProgress.animateTo(1f, animationSpec = tween(280, easing = FastOutSlowInEasing))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(onyx.background)
            .graphicsLayer {
                alpha = revealProgress.value
                translationY = (1f - revealProgress.value) * 18f
            }
            .testTag("home_dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ONYX FILES",
                        style = MaterialTheme.typography.labelSmall,
                        color = onyx.accent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Storage Hub",
                        style = MaterialTheme.typography.headlineMedium,
                        color = onyx.textPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(onyx.cardSurface)
                            .border(1.dp, onyx.hairlineBorder, CircleShape)
                            .testTag("home_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search files",
                            tint = onyx.textPrimary
                        )
                    }
                }
            }
        }

        // Storage Permission Banner when full access is not yet granted
        item(key = "permission_banner") {
            AnimatedVisibility(
                visible = !dashboardState.hasAllFilesAccess,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(onyx.cardSurface)
                        .border(1.dp, onyx.accent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_storage_vault),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.35f,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        onyx.background.copy(alpha = 0.92f),
                                        onyx.background.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = onyx.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enable Full Storage Access",
                                style = MaterialTheme.typography.titleMedium,
                                color = onyx.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Allow Onyx Files to browse, manage, compress, and move all internal & SD card folders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = onyx.textSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onRequestAllFilesAccess,
                                colors = ButtonDefaults.buttonColors(containerColor = onyx.accent),
                                modifier = Modifier.testTag("grant_all_files_button")
                            ) {
                                Text("Allow All Files")
                            }
                            if (!dashboardState.hasAnyStorageAccess) {
                                OutlinedButton(onClick = onRequestMediaPermissions) {
                                    Text("Media Only", color = onyx.textPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Storage Volumes (Internal + SD Card + SAF Drives)
        item(key = "storage_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STORAGE DRIVES",
                    style = MaterialTheme.typography.labelMedium,
                    color = onyx.textTertiary
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onMountSafDrive)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = onyx.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mount SD / SAF",
                        style = MaterialTheme.typography.labelMedium,
                        color = onyx.accent
                    )
                }
            }
        }

        items(dashboardState.volumes, key = { it.id }) { volume ->
            StorageVolumeCard(
                volume = volume,
                onClick = { onVolumeClick(volume) }
            )
        }

        // Categories Section
        item(key = "categories_header") {
            Text(
                text = "LIBRARIES",
                style = MaterialTheme.typography.labelMedium,
                color = onyx.textTertiary
            )
        }

        item(key = "categories_grid") {
            val columns = if (isExpandedWidth) 4 else 2
            val categories = FileCategory.entries
            val rows = categories.chunked(columns)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { category ->
                            CategoryCard(
                                category = category,
                                summary = summaryMap[category],
                                accentColor = onyx.accent,
                                onClick = { onCategoryClick(category) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(columns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Quick Bookmarks
        if (preferences.favorites.isNotEmpty()) {
            item(key = "favorites_header") {
                Text(
                    text = "BOOKMARKED FOLDERS",
                    style = MaterialTheme.typography.labelMedium,
                    color = onyx.textTertiary
                )
            }
            item(key = "favorites_row") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(preferences.favorites, key = { it.path }) { fav ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(onyx.cardSurface)
                                .border(0.8.dp, onyx.hairlineBorder, RoundedCornerShape(14.dp))
                                .clickable { onFavoriteClick(fav) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = onyx.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = fav.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = onyx.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // Recently Opened Files
        if (preferences.recentFiles.isNotEmpty()) {
            item(key = "recent_opened_header") {
                Text(
                    text = "RECENTLY OPENED",
                    style = MaterialTheme.typography.labelMedium,
                    color = onyx.textTertiary
                )
            }
            items(preferences.recentFiles.take(5), key = { "${it.path}_${it.openedAt}" }) { recent ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(onyx.cardSurface)
                        .border(0.75.dp, onyx.subtleBorder, RoundedCornerShape(14.dp))
                        .clickable { onRecentClick(recent) }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = onyx.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recent.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = onyx.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${FileFormatters.formatFileSize(recent.size)}  •  ${FileFormatters.formatDateSmart(recent.openedAt)}",
                            style = MonoMetadataStyle,
                            color = onyx.textSecondary
                        )
                    }
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun CategoryCard(
    category: FileCategory,
    summary: CategorySummary?,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onyx = LocalOnyxColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "catScale"
    )

    val (icon, tint) = categoryVisuals(category, accentColor)
    val countText = summary?.let { "${it.count} items" } ?: "—"
    val sizeText = summary?.takeIf { it.totalBytes > 0L }?.let { FileFormatters.formatFileSize(it.totalBytes) } ?: ""

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(onyx.cardSurface)
            .border(0.85.dp, onyx.subtleBorder, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .padding(14.dp)
            .testTag("category_card_${category.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.label,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (sizeText.isNotEmpty()) {
                Text(
                    text = sizeText,
                    style = MonoMetadataStyle,
                    color = onyx.textTertiary
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = category.label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = onyx.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = countText,
            style = MonoMetadataStyle,
            color = onyx.textSecondary
        )
    }
}

private fun categoryVisuals(category: FileCategory, accentColor: Color): Pair<ImageVector, Color> {
    return when (category) {
        FileCategory.RECENT -> Icons.Rounded.History to accentColor
        FileCategory.IMAGES -> Icons.Rounded.Image to TypeImageColor
        FileCategory.VIDEOS -> Icons.Rounded.VideoFile to TypeVideoColor
        FileCategory.AUDIO -> Icons.Rounded.AudioFile to TypeAudioColor
        FileCategory.DOCUMENTS -> Icons.Rounded.Description to TypeDocumentColor
        FileCategory.DOWNLOADS -> Icons.Rounded.Download to accentColor
        FileCategory.APKS -> Icons.Rounded.Android to TypeApkColor
        FileCategory.ARCHIVES -> Icons.Rounded.FolderZip to TypeArchiveColor
    }
}
