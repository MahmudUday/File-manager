package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.StorageVolumeInfo
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.MonoMetadataStyle
import com.example.util.FileFormatters

@Composable
fun StorageVolumeCard(
    volume: StorageVolumeInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onyx = LocalOnyxColors.current
    var targetProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(volume.usedFraction) {
        targetProgress = volume.usedFraction
    }
    val animatedFraction by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "storageProgress"
    )

    val percentText = remember(volume.usedFraction) {
        "${(volume.usedFraction * 100).toInt()}%"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        onyx.cardSurface,
                        onyx.elevatedSurface.copy(alpha = 0.72f)
                    )
                )
            )
            .border(1.dp, onyx.hairlineBorder, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("storage_card_${volume.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(onyx.accentSoft)
                    .border(0.8.dp, onyx.accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        volume.isSafMounted -> Icons.Rounded.Usb
                        volume.isRemovable -> Icons.Rounded.SdStorage
                        else -> Icons.Rounded.Smartphone
                    },
                    contentDescription = volume.name,
                    tint = onyx.accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = volume.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = onyx.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (volume.totalBytes > 0L) {
                        "${FileFormatters.formatFileSize(volume.usedBytes)} / ${FileFormatters.formatFileSize(volume.totalBytes)}"
                    } else {
                        volume.subtitle
                    },
                    style = MonoMetadataStyle,
                    color = onyx.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (volume.totalBytes > 0L) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(44.dp)) {
                        val stroke = 4.dp.toPx()
                        drawArc(
                            color = onyx.hairlineBorder,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = onyx.accent,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedFraction,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = percentText,
                        style = MaterialTheme.typography.labelSmall,
                        color = onyx.textPrimary
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Open drive",
                    tint = onyx.textSecondary
                )
            }
        }

        if (volume.totalBytes > 0L) {
            Spacer(modifier = Modifier.height(14.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
            ) {
                drawRoundRect(
                    color = onyx.hairlineBorder,
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
                val fillW = (size.width * animatedFraction).coerceAtLeast(size.height)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            onyx.accent.copy(alpha = 0.72f),
                            onyx.accent
                        )
                    ),
                    topLeft = Offset.Zero,
                    size = Size(fillW, size.height),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Used ${FileFormatters.formatFileSize(volume.usedBytes)}",
                    style = MonoMetadataStyle,
                    color = onyx.textTertiary
                )
                Text(
                    text = "Free ${FileFormatters.formatFileSize(volume.freeBytes)}",
                    style = MonoMetadataStyle,
                    color = onyx.accent
                )
            }
        }
    }
}
