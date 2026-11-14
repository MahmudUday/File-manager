package com.example.ui.media

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.FileItem
import com.example.ui.theme.MonoMetadataStyle
import com.example.util.FileFormatters
import java.io.File

@Composable
fun ImageViewerScreen(
    images: List<FileItem>,
    initialIndex: Int,
    onClose: () -> Unit,
    onShare: (FileItem) -> Unit,
    onInfo: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit
) {
    BackHandler { onClose() }

    if (images.isEmpty()) {
        onClose()
        return
    }

    val safeInitial = initialIndex.coerceIn(0, images.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeInitial, pageCount = { images.size })
    var showHud by remember { mutableStateOf(true) }
    var isCurrentZoomed by remember { mutableStateOf(false) }

    val currentItem = images.getOrNull(pagerState.currentPage) ?: images.first()

    LaunchedEffect(pagerState.currentPage) {
        isCurrentZoomed = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("image_viewer_screen")
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isCurrentZoomed,
            key = { index -> images.getOrNull(index)?.id ?: index },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = images[page]
            ZoomableImagePage(
                item = item,
                onTap = { showHud = !showHud },
                onZoomChanged = { zoomed ->
                    if (page == pagerState.currentPage) {
                        isCurrentZoomed = zoomed
                    }
                }
            )
        }

        // Top HUD
        AnimatedVisibility(
            visible = showHud,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("image_viewer_back")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${pagerState.currentPage + 1} / ${images.size}  •  ${FileFormatters.formatFileSize(currentItem.size)}",
                        style = MonoMetadataStyle,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
                IconButton(onClick = { onInfo(currentItem) }) {
                    Icon(Icons.Rounded.Info, contentDescription = "Image details", tint = Color.White)
                }
                IconButton(onClick = { onShare(currentItem) }) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share image", tint = Color.White)
                }
                IconButton(onClick = { onDelete(currentItem) }) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete image", tint = Color.White)
                }
            }
        }

        // Bottom metadata bar
        AnimatedVisibility(
            visible = showHud,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = FileFormatters.formatFullDate(currentItem.lastModified),
                    style = MonoMetadataStyle,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Text(
                    text = currentItem.extension.uppercase().ifEmpty { "IMG" },
                    style = MonoMetadataStyle,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun ZoomableImagePage(
    item: FileItem,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(160),
        label = "zoomScale"
    )

    LaunchedEffect(scale) {
        onZoomChanged(scale > 1.05f)
    }

    val model = remember(item.id, item.uriString) {
        if (item.uriString.startsWith("content://")) Uri.parse(item.uriString) else File(item.path)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.6f
                        }
                    }
                )
            }
            .pointerInput(item.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.count { it.pressed }
                        if (pointerCount >= 2 || scale > 1.05f) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
                            scale = nextScale
                            offset = if (nextScale > 1.05f) {
                                offset + panChange
                            } else {
                                Offset.Zero
                            }
                            if (pointerCount >= 2 || scale > 1.05f) {
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    translationX = if (scale > 1.05f) offset.x else 0f
                    translationY = if (scale > 1.05f) offset.y else 0f
                }
        )
    }
}
