package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

private val CatCardShape = RoundedCornerShape(18.dp)
private val CatIconShape = RoundedCornerShape(11.dp)
private val BannerShape = RoundedCornerShape(22.dp)
private val HeroStorageShape = RoundedCornerShape(24.dp)
private val QuickCardShape = RoundedCornerShape(14.dp)

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
        revealProgress.animateTo(1f, animationSpec = tween(260, easing = FastOutSlowInEasing))
    }

    val totalBytes = remember(dashboardState.volumes) {
        dashboardState.volumes.filter { it.totalBytes > 0L }.sumOf { it.totalBytes }
    }
    val usedBytes = remember(dashboardState.volumes) {
        dashboardState.volumes.filter { it.totalBytes > 0L }.sumOf { it.usedBytes }
    }
    val freeBytes = remember(totalBytes, usedBytes) {
        (totalBytes - usedBytes).coerceAtLeast(0L)
    }
    val totalFraction = remember(totalBytes, usedBytes) {
        if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(onyx.background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(onyx.accentGlow.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.85f, 40f),
                        radius = size.width * 0.72f
                    ),
                    center = Offset(size.width * 0.85f, 40f),
                    radius = size.width * 0.72f
                )
            }
            .graphicsLayer {
                alpha = revealProgress.value
                translationY = (1f - revealProgress.value) * 18f
            }
            .testTag("home_dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header + Animated Storage Hero Block
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ORION EXPLORER",
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
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(listOf(onyx.glowBorder, onyx.hairlineBorder)),
                                    shape = CircleShape
                                )
                                .testTag("home_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search files",
                                tint = onyx.accent
                            )
                        }
                    }
                }

                // Animated Storage Space Header Card
                AnimatedStorageHeaderCard(
                    usedBytes = usedBytes,
                    freeBytes = freeBytes,
                    totalBytes = totalBytes,
                    usedFraction = totalFraction,
                    driveCount = dashboardState.volumes.size,
                    onClick = {
                        dashboardState.volumes.firstOrNull()?.let(onVolumeClick)
                    }
                )
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
                        .clip(BannerShape)
                        .background(onyx.cardSurface)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(onyx.glowBorder, onyx.accent.copy(alpha = 0.25f))),
                            shape = BannerShape
                        )
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
                            text = "Allow Orion Explorer to browse, manage, compress, and move all internal & SD card folders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = onyx.textSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onRequestAllFilesAccess,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = onyx.accent,
                                    contentColor = onyx.onAccent
                                ),
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
                        .background(onyx.accentSoft)
                        .border(0.75.dp, onyx.glowBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onMountSafDrive)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
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
                onClick = { onVolumeClick(volume) },
                modifier = Modifier.animateItem()
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
                                .animateItem()
                                .clip(QuickCardShape)
                                .background(onyx.cardSurface)
                                .border(0.85.dp, onyx.glowBorder.copy(alpha = 0.55f), QuickCardShape)
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
                        .animateItem()
                        .fillMaxWidth()
                        .clip(QuickCardShape)
                        .background(onyx.cardSurface)
                        .border(0.75.dp, onyx.subtleBorder, QuickCardShape)
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
private fun AnimatedStorageHeaderCard(
    usedBytes: Long,
    freeBytes: Long,
    totalBytes: Long,
    usedFraction: Float,
    driveCount: Int,
    onClick: () -> Unit
) {
    val onyx = LocalOnyxColors.current
    var targetFraction by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(usedFraction) {
        targetFraction = usedFraction
    }
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "heroStorageProgress"
    )

    val pulseTransition = rememberInfiniteTransition(label = "heroGlowPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.56f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val sweepAngle by pulseTransition.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepLine"
    )

    val percentInt = (animatedFraction * 100f).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HeroStorageShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(onyx.cardSurface, onyx.elevatedSurface)
                )
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(onyx.accentGlow.copy(alpha = pulseAlpha), Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 0.45f),
                        radius = size.width * 0.65f
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.45f),
                    radius = size.width * 0.65f
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(onyx.glowBorder, onyx.hairlineBorder, onyx.accent.copy(alpha = 0.35f))
                ),
                shape = HeroStorageShape
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("home_animated_storage_header")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(66.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(66.dp)) {
                    val stroke = 6.dp.toPx()
                    val haloStroke = 12.dp.toPx()
                    drawArc(
                        color = onyx.hairlineBorder,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = onyx.accent.copy(alpha = pulseAlpha * 0.65f),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedFraction,
                        useCenter = false,
                        style = Stroke(width = haloStroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = onyx.accent,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedFraction,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentInt%",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = onyx.textPrimary
                    )
                    Text(
                        text = "USED",
                        style = MaterialTheme.typography.labelSmall,
                        color = onyx.accent
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = onyx.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TOTAL SPACE",
                        style = MaterialTheme.typography.labelSmall,
                        color = onyx.accent
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (totalBytes > 0L) {
                        "${FileFormatters.formatFileSize(freeBytes)} Free"
                    } else {
                        "Scanning storage..."
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = onyx.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (totalBytes > 0L) {
                        "${FileFormatters.formatFileSize(usedBytes)} of ${FileFormatters.formatFileSize(totalBytes)} • $driveCount drive(s)"
                    } else {
                        "$driveCount drive(s) mounted"
                    },
                    style = MonoMetadataStyle,
                    color = onyx.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Open storage",
                tint = onyx.accent
            )
        }

        if (totalBytes > 0L) {
            Spacer(modifier = Modifier.height(14.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                drawRoundRect(
                    color = onyx.hairlineBorder,
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
                val fillW = (size.width * animatedFraction).coerceAtLeast(size.height)
                drawRoundRect(
                    color = onyx.accentGlow,
                    topLeft = Offset(0f, -1.dp.toPx()),
                    size = Size(fillW, size.height + 2.dp.toPx()),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(onyx.accent.copy(alpha = 0.65f), onyx.accent)
                    ),
                    topLeft = Offset.Zero,
                    size = Size(fillW, size.height),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
                // Animated shimmer pulse traveling inside filled storage bar
                val shimmerX = fillW * sweepAngle
                if (shimmerX in 0f..fillW) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.65f),
                        radius = size.height * 0.7f,
                        center = Offset(shimmerX, size.height / 2f)
                    )
                }
            }
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
                if (scale != 1f) {
                    scaleX = scale
                    scaleY = scale
                }
            }
            .clip(CatCardShape)
            .background(onyx.cardSurface)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(28.dp.toPx(), 28.dp.toPx()),
                        radius = size.width * 0.75f
                    ),
                    center = Offset(28.dp.toPx(), 28.dp.toPx()),
                    radius = size.width * 0.75f
                )
            }
            .border(
                width = 0.9.dp,
                brush = Brush.linearGradient(
                    colors = listOf(tint.copy(alpha = 0.42f), onyx.subtleBorder)
                ),
                shape = CatCardShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
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
                    .clip(CatIconShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(tint.copy(alpha = 0.28f), tint.copy(alpha = 0.12f))
                        )
                    )
                    .border(0.7.dp, tint.copy(alpha = 0.42f), CatIconShape),
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
