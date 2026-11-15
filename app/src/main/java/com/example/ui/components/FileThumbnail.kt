package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.data.model.FileItem
import com.example.data.model.FileTypeGroup
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.MonoMetadataStyle
import com.example.ui.theme.TypeApkColor
import com.example.ui.theme.TypeArchiveColor
import com.example.ui.theme.TypeAudioColor
import com.example.ui.theme.TypeCodeColor
import com.example.ui.theme.TypeDefaultColor
import com.example.ui.theme.TypeDocumentColor
import com.example.ui.theme.TypeImageColor
import com.example.ui.theme.TypeVideoColor
import com.example.util.FileFormatters
import java.io.File

@Composable
fun fileTypeColor(typeGroup: FileTypeGroup, accent: Color): Color {
    return when (typeGroup) {
        FileTypeGroup.FOLDER -> accent
        FileTypeGroup.IMAGE -> TypeImageColor
        FileTypeGroup.VIDEO -> TypeVideoColor
        FileTypeGroup.AUDIO -> TypeAudioColor
        FileTypeGroup.DOCUMENT -> TypeDocumentColor
        FileTypeGroup.ARCHIVE -> TypeArchiveColor
        FileTypeGroup.APK -> TypeApkColor
        FileTypeGroup.CODE -> TypeCodeColor
        FileTypeGroup.OTHER -> TypeDefaultColor
    }
}

fun fileTypeIcon(typeGroup: FileTypeGroup): ImageVector {
    return when (typeGroup) {
        FileTypeGroup.FOLDER -> Icons.Rounded.Folder
        FileTypeGroup.IMAGE -> Icons.Rounded.Image
        FileTypeGroup.VIDEO -> Icons.Rounded.VideoFile
        FileTypeGroup.AUDIO -> Icons.Rounded.AudioFile
        FileTypeGroup.DOCUMENT -> Icons.Rounded.Description
        FileTypeGroup.ARCHIVE -> Icons.Rounded.FolderZip
        FileTypeGroup.APK -> Icons.Rounded.Android
        FileTypeGroup.CODE -> Icons.Rounded.Code
        FileTypeGroup.OTHER -> Icons.Rounded.InsertDriveFile
    }
}

@Composable
fun FileThumbnail(
    item: FileItem,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    cornerRadius: Dp = 13.dp
) {
    val onyx = LocalOnyxColors.current
    val tint = fileTypeColor(item.typeGroup, onyx.accent)
    val context = LocalContext.current

    val showThumbnail = item.typeGroup == FileTypeGroup.IMAGE || item.typeGroup == FileTypeGroup.VIDEO

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (item.isDirectory) tint.copy(alpha = 0.13f) else onyx.elevatedSurface
            )
            .border(
                width = 0.75.dp,
                color = if (item.isDirectory) tint.copy(alpha = 0.25f) else onyx.subtleBorder,
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showThumbnail) {
            val request = remember(item.id, item.uriString) {
                val dataModel = if (item.uriString.startsWith("content://")) {
                    Uri.parse(item.uriString)
                } else {
                    File(item.path)
                }
                ImageRequest.Builder(context)
                    .data(dataModel)
                    .size(160, 160)
                    .precision(Precision.INEXACT)
                    .crossfade(false)
                    .memoryCacheKey("thumb_${item.id}_${item.lastModified}")
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (item.typeGroup == FileTypeGroup.VIDEO) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else {
            Icon(
                imageVector = fileTypeIcon(item.typeGroup),
                contentDescription = item.name,
                tint = tint,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListRow(
    item: FileItem,
    isSelected: Boolean,
    showExtension: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onyx = LocalOnyxColors.current
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "rowScale"
    )
    val bgColor = if (isSelected) onyx.accentSoft else onyx.cardSurface
    val borderColor = if (isSelected) onyx.accent.copy(alpha = 0.72f) else onyx.subtleBorder

    val displayName = if (showExtension) item.name else item.displayNameWithoutExtension
    val metaText = remember(item.id, item.formattedSize, item.formattedDate) {
        val sizePart = item.formattedSize.ifEmpty {
            if (item.isDirectory) "Folder" else FileFormatters.formatFileSize(item.size)
        }
        val datePart = item.formattedDate.ifEmpty {
            FileFormatters.formatDateSmart(item.lastModified)
        }
        "$sizePart  •  $datePart"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                if (scale != 1f) {
                    scaleX = scale
                    scaleY = scale
                }
                if (item.isHidden) alpha = 0.68f
            }
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(width = 0.8.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("file_row_${item.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            FileThumbnail(item = item, size = 46.dp, cornerRadius = 13.dp)
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = onyx.accent,
                    modifier = Modifier
                        .size(20.dp)
                        .background(onyx.background, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                color = onyx.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = metaText,
                style = MonoMetadataStyle,
                color = onyx.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onMoreClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("file_more_${item.name}")
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "File options for ${item.name}",
                tint = onyx.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridCard(
    item: FileItem,
    isSelected: Boolean,
    showExtension: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onyx = LocalOnyxColors.current
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "gridScale"
    )
    val bgColor = if (isSelected) onyx.accentSoft else onyx.cardSurface
    val borderColor = if (isSelected) onyx.accent.copy(alpha = 0.75f) else onyx.subtleBorder

    val displayName = if (showExtension) item.name else item.displayNameWithoutExtension
    val metaText = item.formattedSize.ifEmpty {
        if (item.isDirectory) "Folder" else FileFormatters.formatFileSize(item.size)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                if (scale != 1f) {
                    scaleX = scale
                    scaleY = scale
                }
                if (item.isHidden) alpha = 0.68f
            }
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(width = 0.8.dp, color = borderColor, shape = RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(10.dp)
            .testTag("file_grid_${item.name}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.18f),
            contentAlignment = Alignment.Center
        ) {
            FileThumbnail(
                item = item,
                size = 86.dp,
                cornerRadius = 15.dp,
                modifier = Modifier.fillMaxSize()
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = onyx.accent,
                    modifier = Modifier
                        .size(22.dp)
                        .background(onyx.background, CircleShape)
                )
            }
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(18.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .padding(2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.titleSmall,
            color = onyx.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = metaText,
            style = MonoMetadataStyle,
            color = onyx.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
