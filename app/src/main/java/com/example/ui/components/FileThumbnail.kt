package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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

private val ListRowShape = RoundedCornerShape(16.dp)
private val GridCardShape = RoundedCornerShape(18.dp)
private val ThumbDefaultShape = RoundedCornerShape(13.dp)
private val ThumbGridShape = RoundedCornerShape(15.dp)

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
    val shape = remember(cornerRadius) {
        when (cornerRadius) {
            13.dp -> ThumbDefaultShape
            15.dp -> ThumbGridShape
            else -> RoundedCornerShape(cornerRadius)
        }
    }

    val showThumbnail = item.typeGroup == FileTypeGroup.IMAGE || item.typeGroup == FileTypeGroup.VIDEO

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                if (item.isDirectory) {
                    Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = 0.24f), tint.copy(alpha = 0.10f))
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = 0.14f), onyx.elevatedSurface)
                    )
                }
            )
            .border(
                width = 0.85.dp,
                color = if (item.isDirectory) tint.copy(alpha = 0.42f) else tint.copy(alpha = 0.24f),
                shape = shape
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
                        .background(Color.Black.copy(alpha = 0.68f))
                        .border(0.7.dp, tint.copy(alpha = 0.6f), CircleShape),
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

    // Lightweight GPU-only scroll entrance micro-animation
    val appearProgress = remember(item.id) { Animatable(0f) }
    LaunchedEffect(item.id) {
        appearProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 145, easing = FastOutSlowInEasing)
        )
    }

    val selectScale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "rowScale"
    )
    val bgColor = if (isSelected) onyx.accentSoft else onyx.cardSurface
    val borderColor = if (isSelected) onyx.accent.copy(alpha = 0.80f) else onyx.subtleBorder

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
                val p = appearProgress.value
                val combinedScale = selectScale * (0.965f + 0.035f * p)
                scaleX = combinedScale
                scaleY = combinedScale
                translationY = (1f - p) * 14f
                alpha = (0.78f + 0.22f * p) * (if (item.isHidden) 0.68f else 1f)
            }
            .clip(ListRowShape)
            .background(bgColor)
            .border(width = if (isSelected) 1.dp else 0.8.dp, color = borderColor, shape = ListRowShape)
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

    val appearProgress = remember(item.id) { Animatable(0f) }
    LaunchedEffect(item.id) {
        appearProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 155, easing = FastOutSlowInEasing)
        )
    }

    val selectScale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "gridScale"
    )
    val bgColor = if (isSelected) onyx.accentSoft else onyx.cardSurface
    val borderColor = if (isSelected) onyx.accent.copy(alpha = 0.80f) else onyx.subtleBorder

    val displayName = if (showExtension) item.name else item.displayNameWithoutExtension
    val metaText = item.formattedSize.ifEmpty {
        if (item.isDirectory) "Folder" else FileFormatters.formatFileSize(item.size)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val p = appearProgress.value
                val combinedScale = selectScale * (0.955f + 0.045f * p)
                scaleX = combinedScale
                scaleY = combinedScale
                translationY = (1f - p) * 16f
                alpha = (0.78f + 0.22f * p) * (if (item.isHidden) 0.68f else 1f)
            }
            .clip(GridCardShape)
            .background(bgColor)
            .border(width = if (isSelected) 1.dp else 0.8.dp, color = borderColor, shape = GridCardShape)
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
