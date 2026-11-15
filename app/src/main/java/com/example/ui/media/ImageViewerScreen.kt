package com.example.ui.media

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Rotate90DegreesCw
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.data.model.FileItem
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.MonoMetadataStyle
import com.example.util.FileFormatters
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun ImageViewerScreen(
    images: List<FileItem>,
    initialIndex: Int,
    onClose: () -> Unit,
    onShare: (FileItem) -> Unit,
    onInfo: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onOpenExternal: (FileItem) -> Unit = {}
) {
    BackHandler { onClose() }

    if (images.isEmpty()) {
        onClose()
        return
    }

    val onyx = LocalOnyxColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val safeInitial = initialIndex.coerceIn(0, images.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeInitial, pageCount = { images.size })
    val thumbListState = rememberLazyListState()

    var showHud by remember { mutableStateOf(true) }
    var currentZoom by remember { mutableFloatStateOf(1f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var resetSignal by remember { mutableIntStateOf(0) }

    val currentItem = images.getOrNull(pagerState.currentPage) ?: images.first()
    val isCurrentZoomed = currentZoom > 1.05f

    LaunchedEffect(pagerState.currentPage) {
        currentZoom = 1f
        rotationDegrees = 0f
        if (images.size > 1) {
            runCatching { thumbListState.animateScrollToItem(pagerState.currentPage) }
        }
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
                rotationDegrees = if (page == pagerState.currentPage) rotationDegrees else 0f,
                resetSignal = if (page == pagerState.currentPage) resetSignal else 0,
                onTap = { showHud = !showHud },
                onScaleChanged = { scale ->
                    if (page == pagerState.currentPage) {
                        currentZoom = scale
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
                            colors = listOf(Color.Black.copy(alpha = 0.88f), Color.Transparent)
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
                if (isCurrentZoomed || rotationDegrees != 0f) {
                    Text(
                        text = String.format(Locale.US, "%.1fx", currentZoom),
                        style = MonoMetadataStyle.copy(fontWeight = FontWeight.Bold),
                        color = onyx.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable {
                                resetSignal++
                                rotationDegrees = 0f
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                IconButton(onClick = { rotationDegrees += 90f }) {
                    Icon(Icons.Rounded.Rotate90DegreesCw, contentDescription = "Rotate 90 degrees", tint = Color.White)
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

        // Bottom HUD: Filmstrip + Quick Action Pills + Metadata
        AnimatedVisibility(
            visible = showHud,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f))
                        )
                    )
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 12.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (images.size > 1) {
                    LazyRow(
                        state = thumbListState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(images, key = { idx, it -> "${idx}_${it.id}" }) { index, thumbItem ->
                            val selected = index == pagerState.currentPage
                            val thumbModel = remember(thumbItem.id) {
                                if (thumbItem.uriString.startsWith("content://")) Uri.parse(thumbItem.uriString) else File(thumbItem.path)
                            }
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(thumbModel)
                                    .size(110, 110)
                                    .precision(Precision.INEXACT)
                                    .build(),
                                contentDescription = thumbItem.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(if (selected) 52.dp else 46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (selected) 2.dp else 0.75.dp,
                                        color = if (selected) onyx.accent else Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        scope.launch { pagerState.animateScrollToPage(index) }
                                    }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = FileFormatters.formatFullDate(currentItem.lastModified),
                            style = MonoMetadataStyle,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        Text(
                            text = currentItem.extension.uppercase().ifEmpty { "IMG" },
                            style = MonoMetadataStyle,
                            color = onyx.accent
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageHudChip(
                            icon = Icons.Rounded.ZoomOutMap,
                            label = "Reset",
                            onClick = {
                                resetSignal++
                                rotationDegrees = 0f
                            }
                        )
                        ImageHudChip(
                            icon = Icons.Rounded.ContentCopy,
                            label = "Path",
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("File Path", currentItem.path))
                                Toast.makeText(context, "Path copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                        ImageHudChip(
                            icon = Icons.Rounded.OpenInNew,
                            label = "Open",
                            onClick = { onOpenExternal(currentItem) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageHudChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun ZoomableImagePage(
    item: FileItem,
    rotationDegrees: Float,
    resetSignal: Int,
    onTap: () -> Unit,
    onScaleChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(160),
        label = "zoomScale"
    )
    val animatedRotation by animateFloatAsState(
        targetValue = rotationDegrees,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "imageRotation"
    )

    LaunchedEffect(scale) {
        onScaleChanged(scale)
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
                    rotationZ = animatedRotation
                    translationX = if (scale > 1.05f) offset.x else 0f
                    translationY = if (scale > 1.05f) offset.y else 0f
                }
        )
    }
}
