package com.example.ui.media

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.FileItem
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.MonoMetadataStyle
import com.example.util.FileFormatters
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoItem: FileItem,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val onyx = LocalOnyxColors.current

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    val exoPlayer = remember(videoItem.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoItem.uriString)))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var showUnlockPill by remember { mutableStateOf(false) }

    var currentPos by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }

    var gestureOverlayLabel by remember { mutableStateOf<String?>(null) }
    var gestureOverlayIsBrightness by remember { mutableStateOf(true) }
    var gestureFraction by remember { mutableFloatStateOf(-1f) }

    var brightnessLevel by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
        )
    }

    BackHandler {
        if (isLocked) {
            showUnlockPill = true
        } else {
            onClose()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = exoPlayer.duration.takeIf { it > 0L } ?: duration
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val lp = act.window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                act.window.attributes = lp
            }
        }
    }

    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val d = exoPlayer.duration
            if (d > 0L) duration = d
            delay(250L)
        }
    }

    LaunchedEffect(showControls, isPlaying, isLocked) {
        if (showControls && isPlaying && !isLocked) {
            delay(3500L)
            showControls = false
        }
    }

    LaunchedEffect(showUnlockPill) {
        if (showUnlockPill) {
            delay(2500L)
            showUnlockPill = false
        }
    }

    LaunchedEffect(gestureOverlayLabel) {
        if (gestureOverlayLabel != null) {
            delay(1100L)
            gestureOverlayLabel = null
        }
    }

    val resizeLabel = when (resizeMode) {
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "FILL"
        AspectRatioFrameLayout.RESIZE_MODE_FILL -> "STRETCH"
        else -> "FIT"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_screen")
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture capture layer (left = brightness + -10s, right = volume + +10s)
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pointerInput(isLocked) {
                        detectTapGestures(
                            onTap = {
                                if (isLocked) {
                                    showUnlockPill = !showUnlockPill
                                } else {
                                    showControls = !showControls
                                }
                            },
                            onDoubleTap = {
                                if (!isLocked) {
                                    val next = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(next)
                                    currentPos = next
                                    gestureOverlayIsBrightness = true
                                    gestureFraction = -1f
                                    gestureOverlayLabel = "-10s"
                                }
                            }
                        )
                    }
                    .pointerInput(isLocked) {
                        if (!isLocked) {
                            detectVerticalDragGestures { _, dragAmount ->
                                val delta = -dragAmount / size.height.toFloat()
                                brightnessLevel = (brightnessLevel + delta).coerceIn(0.05f, 1f)
                                activity?.let { act ->
                                    val lp = act.window.attributes
                                    lp.screenBrightness = brightnessLevel
                                    act.window.attributes = lp
                                }
                                gestureOverlayIsBrightness = true
                                gestureFraction = brightnessLevel
                                gestureOverlayLabel = "Brightness ${(brightnessLevel * 100).toInt()}%"
                            }
                        }
                    }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pointerInput(isLocked) {
                        detectTapGestures(
                            onTap = {
                                if (isLocked) {
                                    showUnlockPill = !showUnlockPill
                                } else {
                                    showControls = !showControls
                                }
                            },
                            onDoubleTap = {
                                if (!isLocked) {
                                    val next = (exoPlayer.currentPosition + 10_000L).coerceAtMost(duration.coerceAtLeast(0L))
                                    exoPlayer.seekTo(next)
                                    currentPos = next
                                    gestureOverlayIsBrightness = false
                                    gestureFraction = -1f
                                    gestureOverlayLabel = "+10s"
                                }
                            }
                        )
                    }
                    .pointerInput(isLocked) {
                        if (!isLocked) {
                            var volAccum = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
                            detectVerticalDragGestures { _, dragAmount ->
                                val delta = -dragAmount / size.height.toFloat()
                                volAccum = (volAccum + delta).coerceIn(0f, 1f)
                                val targetVol = (volAccum * maxVolume).toInt().coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                gestureOverlayIsBrightness = false
                                gestureFraction = volAccum
                                gestureOverlayLabel = "Volume ${(volAccum * 100).toInt()}%"
                            }
                        }
                    }
            )
        }

        // Gesture HUD Pill with progress meter
        AnimatedVisibility(
            visible = gestureOverlayLabel != null,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.80f))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (gestureOverlayIsBrightness) Icons.Rounded.BrightnessHigh else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = onyx.accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = gestureOverlayLabel.orEmpty(),
                        style = MonoMetadataStyle.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
                if (gestureFraction >= 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { gestureFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .width(140.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = onyx.accent,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }
        }

        // Floating Unlock Pill when Screen Lock is enabled
        AnimatedVisibility(
            visible = isLocked && showUnlockPill,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .clickable {
                        isLocked = false
                        showUnlockPill = false
                        showControls = true
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.LockOpen, contentDescription = "Unlock screen", tint = onyx.accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tap to Unlock Controls", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }

        // Player Controls HUD
        AnimatedVisibility(
            visible = showControls && !isLocked,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("video_player_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = videoItem.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${FileFormatters.formatFileSize(videoItem.size)}  •  $resizeLabel",
                            style = MonoMetadataStyle,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "${playbackSpeed}x",
                        style = MonoMetadataStyle,
                        color = onyx.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable {
                                val nextSpeed = when (playbackSpeed) {
                                    1f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2f
                                    2f -> 0.75f
                                    else -> 1f
                                }
                                playbackSpeed = nextSpeed
                                exoPlayer.setPlaybackSpeed(nextSpeed)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    IconButton(onClick = {
                        isMuted = !isMuted
                        exoPlayer.volume = if (isMuted) 0f else 1f
                    }) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = if (isMuted) onyx.accent else Color.White
                        )
                    }
                    IconButton(onClick = {
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }) {
                        Icon(Icons.Rounded.AspectRatio, contentDescription = "Toggle aspect ratio", tint = Color.White)
                    }
                    IconButton(onClick = {
                        activity?.let { act ->
                            val isLandscape = act.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            act.requestedOrientation = if (isLandscape) {
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }
                        }
                    }) {
                        Icon(Icons.Rounded.ScreenRotation, contentDescription = "Rotate screen", tint = Color.White)
                    }
                }

                // Center Transport Buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)) },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(30.dp))
                    }

                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(onyx.accent)
                            .testTag("video_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF05070B),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(duration.coerceAtLeast(0L)))
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }

                // Bottom Seekbar + Loop & Lock Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f))
                            )
                        )
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    val safeDuration = duration.coerceAtLeast(1L)
                    Slider(
                        value = (currentPos.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f),
                        onValueChange = { fraction ->
                            val target = (fraction * safeDuration).toLong()
                            currentPos = target
                            exoPlayer.seekTo(target)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = onyx.accent,
                            activeTrackColor = onyx.accent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${FileFormatters.formatDuration(currentPos)} / ${FileFormatters.formatDuration(duration)}",
                            style = MonoMetadataStyle,
                            color = Color.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    isLooping = !isLooping
                                    exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLooping) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                    contentDescription = "Loop video",
                                    tint = if (isLooping) onyx.accent else Color.White.copy(alpha = 0.78f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    isLocked = true
                                    showControls = false
                                    showUnlockPill = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Lock screen controls",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
