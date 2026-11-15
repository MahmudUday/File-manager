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
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppPreferences
import com.example.data.model.CategorySummary
import com.example.data.model.FavoriteFolder
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.StorageVolumeInfo
import com.example.ui.components.FileListRow
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.MonoMetadataStyle
import com.example.ui.theme.TypeApkColor
import com.example.ui.theme.TypeArchiveColor
import com.example.ui.theme.TypeAudioColor
import com.example.ui.theme.TypeDocumentColor
import com.example.ui.theme.TypeImageColor
import com.example.ui.theme.TypeVideoColor
import com.example.ui.theme.WarningGold
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.GlobalSearchUiState
import com.example.util.FileFormatters
import kotlin.math.cos
import kotlin.math.sin

private val CatCardShape = RoundedCornerShape(18.dp)
private val CatIconShape = RoundedCornerShape(11.dp)
private val BannerShape = RoundedCornerShape(22.dp)
private val QuickCardShape = RoundedCornerShape(14.dp)
private val SearchBarShape = RoundedCornerShape(18.dp)
private val DrivePillShape = RoundedCornerShape(14.dp)

@Composable
fun HomeDashboardScreen(
    dashboardState: DashboardUiState,
    globalSearchState: GlobalSearchUiState,
    preferences: AppPreferences,
    isExpandedWidth: Boolean,
    onVolumeClick: (StorageVolumeInfo) -> Unit,
    onCategoryClick: (FileCategory) -> Unit,
    onFavoriteClick: (FavoriteFolder) -> Unit,
    onGlobalSearchQueryChange: (String) -> Unit,
    onClearGlobalSearch: () -> Unit,
    onSearchResultClick: (FileItem, List<FileItem>) -> Unit,
    onSearchResultMoreClick: (FileItem) -> Unit,
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
        revealProgress.animateTo(1f, animationSpec = tween(240, easing = FastOutSlowInEasing))
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
                translationY = (1f - revealProgress.value) * 16f
            }
            .testTag("home_dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header + Global Search Bar
        item(key = "header_search") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                }

                // Interactive Global Search Bar directly in the header
                OutlinedTextField(
                    value = globalSearchState.query,
                    onValueChange = onGlobalSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Search files & folders globally...",
                            color = onyx.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Global search",
                            tint = onyx.accent
                        )
                    },
                    trailingIcon = {
                        if (globalSearchState.isActive) {
                            IconButton(
                                onClick = onClearGlobalSearch,
                                modifier = Modifier.testTag("home_global_search_clear")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = onyx.textSecondary
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = onyx.accent,
                        unfocusedBorderColor = onyx.glowBorder.copy(alpha = 0.55f),
                        focusedContainerColor = onyx.cardSurface,
                        unfocusedContainerColor = onyx.cardSurface,
                        focusedTextColor = onyx.textPrimary,
                        unfocusedTextColor = onyx.textPrimary,
                        cursorColor = onyx.accent
                    ),
                    shape = SearchBarShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_global_search_input")
                )

                if (globalSearchState.isSearching) {
                    LinearProgressIndicator(
                        color = onyx.accent,
                        trackColor = onyx.hairlineBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        if (globalSearchState.isActive) {
            item(key = "global_search_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (globalSearchState.isSearching) {
                            "SEARCHING STORAGE..."
                        } else {
                            "GLOBAL RESULTS (${globalSearchState.results.size})"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = onyx.accent
                    )
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelMedium,
                        color = onyx.textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onClearGlobalSearch)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (globalSearchState.results.isEmpty() && !globalSearchState.isSearching) {
                item(key = "global_search_empty") {
                    EmptyBrowserState(
                        imageRes = R.drawable.img_empty_search,
                        title = "No Matches Found",
                        subtitle = "No files or folders matched \"${globalSearchState.query}\".",
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                }
            } else {
                items(
                    items = globalSearchState.results,
                    key = { "gs_${it.id}" }
                ) { item ->
                    FileListRow(
                        item = item,
                        isSelected = false,
                        showExtension = preferences.showFileExtensions,
                        onClick = { onSearchResultClick(item, globalSearchState.results) },
                        onLongClick = { onSearchResultMoreClick(item) },
                        onMoreClick = { onSearchResultMoreClick(item) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        } else {
            // Round Animated Storage Orb + Quick Drive Pills (No duplicate large storage card list)
            item(key = "round_storage_hero") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AnimatedRoundStorageOrb(
                        usedBytes = usedBytes,
                        freeBytes = freeBytes,
                        totalBytes = totalBytes,
                        usedFraction = totalFraction,
                        driveCount = dashboardState.volumes.size,
                        onClick = {
                            dashboardState.volumes.firstOrNull()?.let(onVolumeClick)
                        }
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(dashboardState.volumes, key = { "pill_${it.id}" }) { volume ->
                            Row(
                                modifier = Modifier
                                    .clip(DrivePillShape)
                                    .background(onyx.cardSurface)
                                    .border(0.85.dp, onyx.glowBorder.copy(alpha = 0.55f), DrivePillShape)
                                    .clickable { onVolumeClick(volume) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("drive_pill_${volume.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when {
                                        volume.isSafMounted -> Icons.Rounded.Usb
                                        volume.isRemovable -> Icons.Rounded.SdStorage
                                        else -> Icons.Rounded.Smartphone
                                    },
                                    contentDescription = volume.name,
                                    tint = onyx.accent,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = volume.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = onyx.textPrimary
                                )
                            }
                        }

                        item(key = "mount_saf_pill") {
                            Row(
                                modifier = Modifier
                                    .clip(DrivePillShape)
                                    .background(onyx.accentSoft)
                                    .border(0.85.dp, onyx.glowBorder.copy(alpha = 0.65f), DrivePillShape)
                                    .clickable(onClick = onMountSafDrive)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("mount_saf_pill"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Mount SD or SAF drive",
                                    tint = onyx.accent,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mount SD / SAF",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = onyx.accent
                                )
                            }
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

            // Libraries Section
            item(key = "categories_header") {
                Text(
                    text = "LIBRARIES",
                    style = MaterialTheme.typography.labelMedium,
                    color = onyx.textTertiary
                )
            }

            item(key = "categories_grid") {
                val columns = if (isExpandedWidth) 4 else 2
                val rows = remember(columns) {
                    FileCategory.entries.chunked(columns)
                }
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
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AnimatedRoundStorageOrb(
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
        label = "roundStorageProgress"
    )

    val transition = rememberInfiniteTransition(label = "orbGlow")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulseAlpha"
    )
    val orbitRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbSpin"
    )

    val percentInt = (animatedFraction * 100f).toInt()

    Box(
        modifier = Modifier
            .size(238.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(onyx.accentGlow.copy(alpha = pulseAlpha), Color.Transparent),
                        radius = size.width * 0.62f
                    ),
                    radius = size.width * 0.62f
                )
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(onyx.elevatedSurface, onyx.cardSurface)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(onyx.glowBorder, onyx.hairlineBorder, onyx.accent.copy(alpha = 0.40f))
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .testTag("home_animated_storage_header"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val strokeWidth = 10.dp.toPx()
            val haloStroke = 20.dp.toPx()
            val innerArcInset = 18.dp.toPx()

            // Base circular track
            drawArc(
                color = onyx.hairlineBorder,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Outer glowing halo along filled arc
            drawArc(
                color = onyx.accent.copy(alpha = pulseAlpha * 0.55f),
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                style = Stroke(width = haloStroke, cap = StrokeCap.Round)
            )

            // Main bright arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(onyx.accent.copy(alpha = 0.72f), onyx.accent, onyx.accent.copy(alpha = 0.85f))
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Subtle orbiting inner ring accent
            drawArc(
                color = onyx.accent.copy(alpha = 0.25f),
                startAngle = orbitRotation,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = Offset(innerArcInset, innerArcInset),
                size = Size(size.width - innerArcInset * 2f, size.height - innerArcInset * 2f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = onyx.accent.copy(alpha = 0.16f),
                startAngle = orbitRotation + 180f,
                sweepAngle = 54f,
                useCenter = false,
                topLeft = Offset(innerArcInset, innerArcInset),
                size = Size(size.width - innerArcInset * 2f, size.height - innerArcInset * 2f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Luminous dot at tip of storage progress arc
            if (animatedFraction > 0.01f) {
                val angleRad = Math.toRadians((-90f + 360f * animatedFraction).toDouble())
                val radius = size.width / 2f
                val tipX = center.x + (radius * cos(angleRad)).toFloat()
                val tipY = center.y + (radius * sin(angleRad)).toFloat()
                drawCircle(
                    color = onyx.accentGlow,
                    radius = 9.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 26.dp)
        ) {
            Text(
                text = "STORAGE USED",
                style = MaterialTheme.typography.labelSmall,
                color = onyx.accent
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$percentInt%",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = onyx.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (totalBytes > 0L) {
                    "${FileFormatters.formatFileSize(freeBytes)} Free"
                } else {
                    "Scanning..."
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = onyx.accent,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (totalBytes > 0L) {
                    "${FileFormatters.formatFileSize(usedBytes)} / ${FileFormatters.formatFileSize(totalBytes)}"
                } else {
                    "$driveCount drive(s)"
                },
                style = MonoMetadataStyle,
                color = onyx.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
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
        FileCategory.IMAGES -> Icons.Rounded.Image to TypeImageColor
        FileCategory.VIDEOS -> Icons.Rounded.VideoFile to TypeVideoColor
        FileCategory.AUDIO -> Icons.Rounded.AudioFile to TypeAudioColor
        FileCategory.DOCUMENTS -> Icons.Rounded.Description to TypeDocumentColor
        FileCategory.DOWNLOADS -> Icons.Rounded.Download to accentColor
        FileCategory.APKS -> Icons.Rounded.Android to TypeApkColor
        FileCategory.ARCHIVES -> Icons.Rounded.FolderZip to TypeArchiveColor
        FileCategory.LARGE_FILES -> Icons.Rounded.CleaningServices to WarningGold
    }
}
