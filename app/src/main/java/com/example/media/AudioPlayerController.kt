package com.example.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.FileItem
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class AudioTrackInfo(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val uriString: String,
    val path: String,
    val durationMs: Long,
    val embeddedArtwork: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioTrackInfo) return false
        return id == other.id && title == other.title && artist == other.artist && album == other.album && uriString == other.uriString
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        return result
    }
}

@Immutable
data class AudioPlaybackState(
    val currentTrack: AudioTrackInfo? = null,
    val queue: List<AudioTrackInfo> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1f,
    val sleepTimerMinutes: Int? = null,
    val isSheetExpanded: Boolean = false
) {
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

class AudioPlayerController(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null

    fun ensureConnected(onConnected: (() -> Unit)? = null) {
        val existing = mediaController
        if (existing != null) {
            onConnected?.invoke()
            return
        }
        val token = SessionToken(context, ComponentName(context, AudioPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    mediaController = controller
                    attachListener(controller)
                    syncFromController(controller)
                    onConnected?.invoke()
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun attachListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionTicker() else stopPositionTicker()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncFromController(controller)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                syncFromController(controller)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _state.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _state.update { it.copy(repeatMode = repeatMode) }
            }
        })
    }

    private fun syncFromController(controller: MediaController) {
        val index = controller.currentMediaItemIndex
        val duration = controller.duration.takeIf { it > 0L } ?: 0L
        _state.update { current ->
            val track = current.queue.getOrNull(index) ?: controller.currentMediaItem?.let { item ->
                AudioTrackInfo(
                    id = item.mediaId,
                    title = item.mediaMetadata.title?.toString() ?: "Audio Track",
                    artist = item.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                    album = item.mediaMetadata.albumTitle?.toString() ?: "Local Storage",
                    uriString = item.localConfiguration?.uri?.toString() ?: "",
                    path = "",
                    durationMs = duration
                )
            }
            current.copy(
                currentTrack = track,
                currentIndex = index.coerceAtLeast(0),
                isPlaying = controller.isPlaying,
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = if (duration > 0L) duration else (track?.durationMs ?: 0L),
                shuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode
            )
        }
        if (controller.isPlaying) startPositionTicker()
    }

    private fun startPositionTicker() {
        if (positionJob?.isActive == true) return
        positionJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0L } ?: _state.value.durationMs
                    _state.update { it.copy(positionMs = pos, durationMs = dur, isPlaying = controller.isPlaying) }
                }
                delay(250L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionJob?.cancel()
        positionJob = null
    }

    fun playTracks(tracks: List<FileItem>, startIndex: Int, expandSheet: Boolean = false) {
        if (tracks.isEmpty()) return
        scope.launch {
            val enriched = withContext(Dispatchers.IO) {
                tracks.map { extractTrackInfo(it) }
            }
            val safeIndex = startIndex.coerceIn(0, enriched.lastIndex)
            _state.update {
                it.copy(
                    queue = enriched,
                    currentIndex = safeIndex,
                    currentTrack = enriched[safeIndex],
                    durationMs = enriched[safeIndex].durationMs,
                    positionMs = 0L,
                    isSheetExpanded = expandSheet
                )
            }
            ensureConnected {
                val controller = mediaController ?: return@ensureConnected
                val mediaItems = enriched.map { track ->
                    val metadataBuilder = MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                    if (track.embeddedArtwork != null) {
                        metadataBuilder.setArtworkData(track.embeddedArtwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    }
                    MediaItem.Builder()
                        .setMediaId(track.id)
                        .setUri(Uri.parse(track.uriString))
                        .setMediaMetadata(metadataBuilder.build())
                        .build()
                }
                controller.setMediaItems(mediaItems, safeIndex, 0L)
                controller.prepare()
                controller.play()
            }
        }
    }

    private fun extractTrackInfo(file: FileItem): AudioTrackInfo {
        var title = file.displayNameWithoutExtension
        var artist = "Unknown Artist"
        var album = "Local Audio"
        var duration = 0L
        var art: ByteArray? = null

        runCatching {
            val mmr = MediaMetadataRetriever()
            val uri = Uri.parse(file.uriString)
            if (uri.scheme == "content") {
                mmr.setDataSource(context, uri)
            } else {
                mmr.setDataSource(file.path)
            }
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let { title = it }
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let { artist = it }
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }?.let { album = it }
            duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            art = mmr.embeddedPicture
            mmr.release()
        }

        return AudioTrackInfo(
            id = file.id,
            title = title,
            artist = artist,
            album = album,
            uriString = file.uriString,
            path = file.path,
            durationMs = duration,
            embeddedArtwork = art
        )
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0L)
            }
            controller.play()
        }
    }

    fun skipNext() {
        mediaController?.let {
            if (it.hasNextMediaItem()) it.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
        mediaController?.let {
            if (it.currentPosition > 3500L) {
                it.seekTo(0L)
            } else if (it.hasPreviousMediaItem()) {
                it.seekToPreviousMediaItem()
            } else {
                it.seekTo(0L)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs.coerceAtLeast(0L))
        _state.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun playQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekToDefaultPosition(index)
            controller.play()
        }
    }

    fun toggleShuffle() {
        mediaController?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        mediaController?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        mediaController?.setPlaybackSpeed(clamped)
        _state.update { it.copy(playbackSpeed = clamped) }
    }

    fun cycleSleepTimer() {
        val nextMinutes = when (_state.value.sleepTimerMinutes) {
            null -> 15
            15 -> 30
            30 -> 60
            else -> null
        }
        sleepTimerJob?.cancel()
        _state.update { it.copy(sleepTimerMinutes = nextMinutes) }
        if (nextMinutes != null) {
            sleepTimerJob = scope.launch {
                var remaining = nextMinutes
                while (remaining > 0 && isActive) {
                    delay(60_000L)
                    remaining--
                    _state.update { it.copy(sleepTimerMinutes = if (remaining > 0) remaining else null) }
                }
                mediaController?.pause()
            }
        }
    }

    fun seekBy(deltaMs: Long) {
        val controller = mediaController ?: return
        val dur = controller.duration.takeIf { it > 0L } ?: _state.value.durationMs
        val target = (controller.currentPosition + deltaMs).coerceIn(0L, dur.coerceAtLeast(0L))
        seekTo(target)
    }

    fun setSheetExpanded(expanded: Boolean) {
        _state.update { it.copy(isSheetExpanded = expanded) }
    }

    fun stopAndDismiss() {
        sleepTimerJob?.cancel()
        mediaController?.stop()
        mediaController?.clearMediaItems()
        stopPositionTicker()
        _state.value = AudioPlaybackState()
    }
}
