package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppPreferences
import com.example.data.model.FavoriteFolder
import com.example.data.model.RecentFileEntry
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.MonoMetadataStyle
import com.example.util.FileFormatters

@Composable
fun FavoritesScreen(
    preferences: AppPreferences,
    onOpenFavorite: (FavoriteFolder) -> Unit,
    onRemoveFavorite: (FavoriteFolder) -> Unit,
    onOpenRecent: (RecentFileEntry) -> Unit,
    onClearRecents: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onyx = LocalOnyxColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(onyx.background)
            .testTag("favorites_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Column {
                Text(
                    text = "QUICK ACCESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = onyx.accent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Favorites & History",
                    style = MaterialTheme.typography.headlineMedium,
                    color = onyx.textPrimary
                )
            }
        }

        item(key = "bookmarks_title") {
            Text(
                text = "BOOKMARKED FOLDERS (${preferences.favorites.size})",
                style = MaterialTheme.typography.labelMedium,
                color = onyx.textTertiary
            )
        }

        if (preferences.favorites.isEmpty()) {
            item(key = "empty_bookmarks") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(onyx.cardSurface)
                        .border(0.8.dp, onyx.subtleBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_folder),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "No Bookmarks Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = onyx.textPrimary
                        )
                        Text(
                            text = "Tap the star icon in any folder to pin it here for instant access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = onyx.textSecondary
                        )
                    }
                }
            }
        } else {
            items(preferences.favorites, key = { it.path }) { fav ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(onyx.cardSurface)
                        .border(0.8.dp, onyx.subtleBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpenFavorite(fav) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(onyx.accentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = onyx.accent
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = fav.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = onyx.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = onyx.accent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = fav.path,
                            style = MonoMetadataStyle,
                            color = onyx.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onRemoveFavorite(fav) }) {
                        Icon(
                            imageVector = Icons.Rounded.BookmarkRemove,
                            contentDescription = "Remove bookmark",
                            tint = onyx.textSecondary
                        )
                    }
                }
            }
        }

        item(key = "recents_title") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENTLY OPENED (${preferences.recentFiles.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = onyx.textTertiary
                )
                if (preferences.recentFiles.isNotEmpty()) {
                    TextButton(onClick = onClearRecents) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            tint = onyx.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", color = onyx.accent)
                    }
                }
            }
        }

        if (preferences.recentFiles.isEmpty()) {
            item(key = "empty_recents") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(onyx.cardSurface)
                        .border(0.8.dp, onyx.subtleBorder, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Files you open in Onyx Files will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onyx.textSecondary
                    )
                }
            }
        } else {
            items(preferences.recentFiles, key = { "${it.path}_${it.openedAt}" }) { recent ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(onyx.cardSurface)
                        .border(0.8.dp, onyx.subtleBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpenRecent(recent) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(onyx.elevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.InsertDriveFile,
                            contentDescription = null,
                            tint = onyx.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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

        item(key = "bottom_pad") {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}
