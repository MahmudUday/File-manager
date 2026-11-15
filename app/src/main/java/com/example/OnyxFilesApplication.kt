package com.example

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FileOperationsRepository
import com.example.media.AudioPlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

class OnyxFilesApplication : Application(), ImageLoaderFactory {

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var fileOperationsRepository: FileOperationsRepository
        private set

    lateinit var audioPlayerController: AudioPlayerController
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = UserPreferencesRepository(this)
        fileOperationsRepository = FileOperationsRepository(this)
        audioPlayerController = AudioPlayerController(this)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun newImageLoader(): ImageLoader {
        val limitedIo = Dispatchers.IO.limitedParallelism(3)
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(VideoFrameDecoder.Factory())
            }
            .fetcherDispatcher(limitedIo)
            .decoderDispatcher(Dispatchers.IO.limitedParallelism(2))
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_thumb_cache"))
                    .maxSizeBytes(150L * 1024L * 1024L)
                    .build()
            }
            .crossfade(false)
            .build()
    }
}
