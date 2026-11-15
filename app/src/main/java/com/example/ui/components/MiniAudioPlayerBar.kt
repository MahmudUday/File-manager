package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.media.AudioPlaybackState
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.MonoMetadataStyle

private val MiniPlayerShape = RoundedCornerShape(18.dp)

@Composable
fun MiniAudioPlayerBar(
    state: AudioPlaybackState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onyx = LocalOnyxColors.current
    val track = state.currentTrack

    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        ) + scaleIn(initialScale = 0.94f) + fadeIn(tween(180)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(140)),
        modifier = modifier
    ) {
        if (track != null) {
            val artBitmap = remember(track.id, track.embeddedArtwork) {
                track.embeddedArtwork?.let { bytes ->
                    runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
                }
            }
            val infiniteTransition = rememberInfiniteTransition(label = "discSpin")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(9000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "discAngle"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(MiniPlayerShape)
                    .background(onyx.elevatedSurface)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(onyx.accentGlow.copy(alpha = 0.32f), Color.Transparent),
                                center = Offset(size.width * 0.12f, size.height * 0.5f),
                                radius = size.width * 0.45f
                            ),
                            center = Offset(size.width * 0.12f, size.height * 0.5f),
                            radius = size.width * 0.45f
                        )
                    }
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(onyx.glowBorder, onyx.hairlineBorder, onyx.glowBorder)
                        ),
                        shape = MiniPlayerShape
                    )
                    .clickable(onClick = onExpand)
                    .testTag("mini_audio_player")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .graphicsLayer {
                                rotationZ = if (state.isPlaying) rotation else 0f
                            }
                            .clip(CircleShape)
                            .border(1.2.dp, onyx.glowBorder, CircleShape)
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

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = onyx.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = MonoMetadataStyle,
                            color = onyx.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = onyx.accent
                        )
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.testTag("mini_player_next")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next track",
                            tint = onyx.textPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close player",
                            tint = onyx.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Glowing hairline progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(onyx.hairlineBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(state.progress)
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(onyx.accent.copy(alpha = 0.65f), onyx.accent)
                                )
                            )
                    )
                }
            }
        }
    }
}
