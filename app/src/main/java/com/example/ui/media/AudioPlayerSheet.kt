package com.example.ui.media

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.R
import com.example.media.AudioPlaybackState
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.MonoMetadataStyle
import com.example.util.FileFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerSheet(
    state: AudioPlaybackState,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onSelectQueueIndex: (Int) -> Unit
) {
    val onyx = LocalOnyxColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val track = state.currentTrack ?: return

    val artBitmap = remember(track.id, track.embeddedArtwork) {
        track.embeddedArtwork?.let { bytes ->
            runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = onyx.background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse audio player",
                        tint = onyx.textPrimary
                    )
                }
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium,
                    color = onyx.accent
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Glowing Album Art
            Box(
                modifier = Modifier
                    .size(236.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(onyx.accentGlow.copy(alpha = 0.55f), Color.Transparent),
                                radius = size.width * 0.68f
                            ),
                            radius = size.width * 0.68f
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(214.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(onyx.cardSurface)
                        .border(1.2.dp, onyx.glowBorder, RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (artBitmap != null) {
                        Image(
                            bitmap = artBitmap,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_audio_artwork),
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                color = onyx.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${track.artist}  •  ${track.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = onyx.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            AudioWaveIndicator(isPlaying = state.isPlaying, color = onyx.accent)

            Spacer(modifier = Modifier.height(12.dp))

            // Quick utility pills: Speed & Sleep Timer
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioUtilityChip(
                    icon = Icons.Rounded.Speed,
                    label = "${state.playbackSpeed}x",
                    active = state.playbackSpeed != 1f,
                    onClick = onCycleSpeed
                )
                AudioUtilityChip(
                    icon = Icons.Rounded.Timer,
                    label = state.sleepTimerMinutes?.let { "${it}m" } ?: "Sleep Off",
                    active = state.sleepTimerMinutes != null,
                    onClick = onCycleSleepTimer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = state.progress,
                onValueChange = { fraction ->
                    val target = (fraction * state.durationMs.coerceAtLeast(1L)).toLong()
                    onSeek(target)
                },
                colors = SliderDefaults.colors(
                    thumbColor = onyx.accent,
                    activeTrackColor = onyx.accent,
                    inactiveTrackColor = onyx.hairlineBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = FileFormatters.formatDuration(state.positionMs),
                    style = MonoMetadataStyle,
                    color = onyx.textSecondary
                )
                Text(
                    text = FileFormatters.formatDuration(state.durationMs),
                    style = MonoMetadataStyle,
                    color = onyx.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transport controls (-10s, Prev, Play, Next, +10s)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.shuffleEnabled) onyx.accent else onyx.textTertiary
                    )
                }
                IconButton(onClick = { onSeekBy(-10_000L) }) {
                    Icon(
                        imageVector = Icons.Rounded.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = onyx.textSecondary
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = onyx.textPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(onyx.accent)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("full_audio_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = onyx.onAccent,
                        modifier = Modifier.size(34.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = onyx.textPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { onSeekBy(10_000L) }) {
                    Icon(
                        imageVector = Icons.Rounded.Forward10,
                        contentDescription = "Forward 10s",
                        tint = onyx.textSecondary
                    )
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) onyx.accent else onyx.textTertiary
                    )
                }
            }

            if (state.queue.size > 1) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "UP NEXT (${state.queue.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = onyx.textTertiary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(state.queue, key = { idx, t -> "${idx}_${t.id}" }) { index, qTrack ->
                        val active = index == state.currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) onyx.accentSoft else onyx.cardSurface)
                                .clickable { onSelectQueueIndex(index) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = if (active) onyx.accent else onyx.textTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = qTrack.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (active) onyx.accent else onyx.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = qTrack.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onyx.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = FileFormatters.formatDuration(qTrack.durationMs),
                                style = MonoMetadataStyle,
                                color = onyx.textTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioUtilityChip(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val onyx = LocalOnyxColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) onyx.accentSoft else onyx.cardSurface)
            .border(
                width = 0.85.dp,
                color = if (active) onyx.glowBorder else onyx.subtleBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) onyx.accent else onyx.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MonoMetadataStyle.copy(fontWeight = FontWeight.SemiBold),
            color = if (active) onyx.accent else onyx.textSecondary
        )
    }
}

@Composable
private fun AudioWaveIndicator(isPlaying: Boolean, color: Color) {
    val transition = rememberInfiniteTransition(label = "audioWave")
    val barHeights = List(7) { index ->
        val anim by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 360 + index * 75, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
        if (isPlaying) anim else 0.22f
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(22.dp)
    ) {
        barHeights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((20.dp * fraction).coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = 0.85f))
            )
        }
    }
}
