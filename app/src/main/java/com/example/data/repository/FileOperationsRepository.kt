package com.example.data.repository

import android.app.usage.StorageStatsManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.CategorySummary
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.FileProperties
import com.example.data.model.FileTypeGroup
import com.example.data.model.SortDirection
import com.example.data.model.SortOption
import com.example.data.model.StorageVolumeInfo
import com.example.util.FileFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

class FileOperationsRepository(private val context: Context) {

    fun getStorageVolumes(safTreeUris: List<String>): List<StorageVolumeInfo> {
        val volumes = mutableListOf<StorageVolumeInfo>()
        val seenPaths = mutableSetOf<String>()

        val primaryRoot = Environment.getExternalStorageDirectory()
        if (primaryRoot != null && primaryRoot.exists()) {
            seenPaths.add(primaryRoot.absolutePath)
            val (total, free) = queryVolumeBytes(primaryRoot)
            volumes.add(
                StorageVolumeInfo(
                    id = "primary_internal",
                    name = "Internal Storage",
                    subtitle = primaryRoot.absolutePath,
                    path = primaryRoot.absolutePath,
                    uriString = null,
                    totalBytes = total,
                    freeBytes = free,
                    usedBytes = (total - free).coerceAtLeast(0L),
                    isRemovable = false,
                    isSafMounted = false
                )
            )
        }

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        storageManager?.storageVolumes?.forEachIndexed { idx, vol ->
            if (!vol.isPrimary && vol.isRemovable) {
                val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) vol.directory else null
                if (dir != null && dir.exists() && seenPaths.add(dir.absolutePath)) {
                    val (total, free) = queryVolumeBytes(dir)
                    volumes.add(
                        StorageVolumeInfo(
                            id = "sd_$idx",
                            name = vol.getDescription(context) ?: "SD Card",
                            subtitle = dir.absolutePath,
                            path = dir.absolutePath,
                            uriString = null,
                            totalBytes = total,
                            freeBytes = free,
                            usedBytes = (total - free).coerceAtLeast(0L),
                            isRemovable = true,
                            isSafMounted = false
                        )
                    )
                }
            }
        }

        // Fallback scan for secondary external dirs (e.g. /storage/XXXX-XXXX) on API 26-29
        context.getExternalFilesDirs(null).drop(1).forEachIndexed { index, appDir ->
            if (appDir != null) {
                val rootCandidate = appDir.absolutePath.substringBefore("/Android/data")
                val rootFile = File(rootCandidate)
                if (rootFile.exists() && rootFile.canRead() && seenPaths.add(rootFile.absolutePath)) {
                    val (total, free) = queryVolumeBytes(rootFile)
                    volumes.add(
                        StorageVolumeInfo(
                            id = "ext_$index",
                            name = "SD Card",
                            subtitle = rootFile.absolutePath,
                            path = rootFile.absolutePath,
                            uriString = null,
                            totalBytes = total,
                            freeBytes = free,
                            usedBytes = (total - free).coerceAtLeast(0L),
                            isRemovable = true,
                            isSafMounted = false
                        )
                    )
                }
            }
        }

        // User-mounted SAF DocumentTrees (external SD card or USB OTG)
        safTreeUris.forEachIndexed { idx, uriStr ->
            runCatching {
                val uri = Uri.parse(uriStr)
                val doc = DocumentFile.fromTreeUri(context, uri)
                if (doc != null && doc.exists()) {
                    volumes.add(
                        StorageVolumeInfo(
                            id = "saf_$idx",
                            name = doc.name ?: "Mounted Drive",
                            subtitle = "SAF Storage Access",
                            path = uriStr,
                            uriString = uriStr,
                            totalBytes = 0L,
                            freeBytes = 0L,
                            usedBytes = 0L,
                            isRemovable = true,
                            isSafMounted = true
                        )
                    )
                }
            }
        }

        return volumes
    }

    private fun queryVolumeBytes(dir: File): Pair<Long, Long> {
        return try {
            if (dir.absolutePath == Environment.getExternalStorageDirectory()?.absolutePath) {
                val statsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                if (statsManager != null) {
                    val total = statsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
                    val free = statsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
                    if (total > 0L) return total to free
                }
            }
            val stat = StatFs(dir.absolutePath)
            stat.totalBytes to stat.availableBytes
        } catch (_: Exception) {
            0L to 0L
        }
    }

    /**
     * Streams directory items incrementally for ultra-smooth loading in huge directories (thousands of files).
     */
    fun listDirectoryStream(
        path: String,
        isSafUri: Boolean,
        showHidden: Boolean,
        sortOption: SortOption,
        sortDirection: SortDirection
    ): Flow<List<FileItem>> = flow {
        if (isSafUri) {
            val treeUri = Uri.parse(path)
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: DocumentFile.fromSingleUri(context, treeUri)
            val children = rootDoc?.listFiles()?.toList().orEmpty()
            val mapped = children.mapNotNull { doc ->
                coroutineContext.ensureActive()
                val name = doc.name ?: return@mapNotNull null
                val hidden = name.startsWith(".")
                if (!showHidden && hidden) return@mapNotNull null
                val isDir = doc.isDirectory
                val ext = if (isDir) "" else name.substringAfterLast('.', "")
                val mime = doc.type ?: if (isDir) "resource/folder" else FileFormatters.getMimeType(ext)
                val docSize = if (isDir) 0L else doc.length()
                val docMod = doc.lastModified()
                FileItem(
                    id = doc.uri.toString(),
                    name = name,
                    path = doc.uri.toString(),
                    uriString = doc.uri.toString(),
                    isDirectory = isDir,
                    size = docSize,
                    lastModified = docMod,
                    mimeType = mime,
                    extension = ext,
                    isHidden = hidden,
                    childCount = 0,
                    isSafDocument = true,
                    typeGroup = FileFormatters.classifyFileType(isDir, ext, mime),
                    formattedSize = if (isDir) "Folder" else FileFormatters.formatFileSize(docSize),
                    formattedDate = FileFormatters.formatDateSmart(docMod)
                )
            }
            emit(sortItems(mapped, sortOption, sortDirection))
            return@flow
        }

        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            emit(emptyList())
            return@flow
        }

        val rawFiles = dir.listFiles() ?: emptyArray()
        val total = rawFiles.size

        // For directories <= 400 items, emit once immediately after mapping & sorting
        if (total <= 400) {
            val items = rawFiles.mapNotNull { file ->
                coroutineContext.ensureActive()
                mapFileToItem(file, showHidden)
            }
            emit(sortItems(items, sortOption, sortDirection))
        } else {
            // Incremental chunked loading for huge directories (thousands of files)
            val accumulated = ArrayList<FileItem>(total)
            val chunkSize = 250
            rawFiles.forEachIndexed { index, file ->
                coroutineContext.ensureActive()
                mapFileToItem(file, showHidden)?.let { accumulated.add(it) }
                if ((index + 1) % chunkSize == 0 || index == total - 1) {
                    emit(sortItems(accumulated, sortOption, sortDirection))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun mapFileToItem(file: File, showHidden: Boolean): FileItem? {
        val name = file.name
        val hidden = file.isHidden || name.startsWith(".")
        if (!showHidden && hidden) return null
        val isDir = file.isDirectory
        val ext = if (isDir) "" else file.extension
        val mime = if (isDir) "resource/folder" else FileFormatters.getMimeType(ext)
        val fileSize = if (isDir) 0L else file.length()
        val mod = file.lastModified()
        return FileItem(
            id = file.absolutePath,
            name = name,
            path = file.absolutePath,
            uriString = Uri.fromFile(file).toString(),
            isDirectory = isDir,
            size = fileSize,
            lastModified = mod,
            mimeType = mime,
            extension = ext,
            isHidden = hidden,
            childCount = 0,
            isSafDocument = false,
            typeGroup = FileFormatters.classifyFileType(isDir, ext, mime),
            formattedSize = if (isDir) "Folder" else FileFormatters.formatFileSize(fileSize),
            formattedDate = FileFormatters.formatDateSmart(mod)
        )
    }

    fun sortItems(
        items: List<FileItem>,
        sortOption: SortOption,
        direction: SortDirection
    ): List<FileItem> {
        val comparator = Comparator<FileItem> { a, b ->
            if (a.isDirectory != b.isDirectory) {
                return@Comparator if (a.isDirectory) -1 else 1
            }
            val cmp = when (sortOption) {
                SortOption.NAME -> a.name.lowercase(Locale.ROOT).compareTo(b.name.lowercase(Locale.ROOT))
                SortOption.SIZE -> a.size.compareTo(b.size)
                SortOption.DATE -> a.lastModified.compareTo(b.lastModified)
                SortOption.TYPE -> a.extension.lowercase(Locale.ROOT).compareTo(b.extension.lowercase(Locale.ROOT))
                    .let { if (it == 0) a.name.lowercase(Locale.ROOT).compareTo(b.name.lowercase(Locale.ROOT)) else it }
            }
            if (direction == SortDirection.ASCENDING) cmp else -cmp
        }
        return items.sortedWith(comparator)
    }

    suspend fun getCategorySummaries(): List<CategorySummary> = withContext(Dispatchers.IO) {
        coroutineScope {
            val imagesDef = async { countMediaStoreSummary(MediaStore.Images.Media.EXTERNAL_CONTENT_URI) }
            val videosDef = async { countMediaStoreSummary(MediaStore.Video.Media.EXTERNAL_CONTENT_URI) }
            val audioDef = async { countMediaStoreSummary(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) }
            val downloadsDef = async {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null && downloadsDir.exists()) {
                    val files = downloadsDir.listFiles() ?: emptyArray()
                    var cnt = 0
                    var bytes = 0L
                    for (f in files) {
                        if (!f.name.startsWith(".")) {
                            cnt++
                            if (f.isFile) bytes += f.length()
                        }
                    }
                    cnt to bytes
                } else 0 to 0L
            }
            val filesDef = async { countFilesTableCategories() }

            val (imgCount, imgBytes) = imagesDef.await()
            val (vidCount, vidBytes) = videosDef.await()
            val (audCount, audBytes) = audioDef.await()
            val (dlCount, dlBytes) = downloadsDef.await()
            val fileStats = filesDef.await()

            listOf(
                CategorySummary(FileCategory.RECENT, fileStats.recentCount, fileStats.recentBytes),
                CategorySummary(FileCategory.IMAGES, imgCount, imgBytes),
                CategorySummary(FileCategory.VIDEOS, vidCount, vidBytes),
                CategorySummary(FileCategory.AUDIO, audCount, audBytes),
                CategorySummary(FileCategory.DOCUMENTS, fileStats.docCount, fileStats.docBytes),
                CategorySummary(FileCategory.DOWNLOADS, dlCount, dlBytes),
                CategorySummary(FileCategory.APKS, fileStats.apkCount, fileStats.apkBytes),
                CategorySummary(FileCategory.ARCHIVES, fileStats.archiveCount, fileStats.archiveBytes)
            )
        }
    }

    private data class FilesTableStats(
        val recentCount: Int = 0,
        val recentBytes: Long = 0L,
        val docCount: Int = 0,
        val docBytes: Long = 0L,
        val apkCount: Int = 0,
        val apkBytes: Long = 0L,
        val archiveCount: Int = 0,
        val archiveBytes: Long = 0L
    )

    private fun countMediaStoreSummary(uri: Uri): Pair<Int, Long> {
        var count = 0
        var total = 0L
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                while (cursor.moveToNext()) {
                    count++
                    total += cursor.getLong(sizeCol)
                }
            }
        }
        return count to total
    }

    private fun countFilesTableCategories(): FilesTableStats {
        var recentCnt = 0; var recentBytes = 0L
        var docCnt = 0; var docBytes = 0L
        var apkCnt = 0; var apkBytes = 0L
        var arcCnt = 0; var arcBytes = 0L

        val filesUri = MediaStore.Files.getContentUri("external")
        val cutoffSec = (System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000) / 1000L
        val docExts = FileFormatters.DOCUMENT_EXTENSIONS + FileFormatters.CODE_EXTENSIONS

        runCatching {
            context.contentResolver.query(
                filesUri,
                arrayOf(
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED
                ),
                "${MediaStore.Files.FileColumns.SIZE} > 0",
                null,
                null
            )?.use { cursor ->
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    if (name.startsWith(".")) continue
                    val size = cursor.getLong(sizeCol)
                    val dateSec = cursor.getLong(dateCol)
                    if (dateSec >= cutoffSec && recentCnt < 120) {
                        recentCnt++
                        recentBytes += size
                    }
                    val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    when {
                        ext == "apk" -> { apkCnt++; apkBytes += size }
                        ext in FileFormatters.ARCHIVE_EXTENSIONS -> { arcCnt++; arcBytes += size }
                        ext in docExts -> { docCnt++; docBytes += size }
                    }
                }
            }
        }
        return FilesTableStats(recentCnt, recentBytes, docCnt, docBytes, apkCnt, apkBytes, arcCnt, arcBytes)
    }

    suspend fun queryCategoryFiles(
        category: FileCategory,
        showHidden: Boolean,
        limit: Int = 2000
    ): List<FileItem> = withContext(Dispatchers.IO) {
        when (category) {
            FileCategory.IMAGES -> queryMediaStore(
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                limit = limit,
                showHidden = showHidden
            )
            FileCategory.VIDEOS -> queryMediaStore(
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                limit = limit,
                showHidden = showHidden
            )
            FileCategory.AUDIO -> queryMediaStore(
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                limit = limit,
                showHidden = showHidden
            )
            FileCategory.DOWNLOADS -> {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null && downloadsDir.exists()) {
                    downloadsDir.listFiles()
                        ?.mapNotNull { mapFileToItem(it, showHidden) }
                        ?.sortedByDescending { it.lastModified }
                        ?: emptyList()
                } else emptyList()
            }
            FileCategory.DOCUMENTS -> queryFilesByExtensions(
                extensions = FileFormatters.DOCUMENT_EXTENSIONS + FileFormatters.CODE_EXTENSIONS,
                showHidden = showHidden,
                limit = limit
            )
            FileCategory.APKS -> queryFilesByExtensions(
                extensions = setOf("apk"),
                showHidden = showHidden,
                limit = limit
            )
            FileCategory.ARCHIVES -> queryFilesByExtensions(
                extensions = FileFormatters.ARCHIVE_EXTENSIONS,
                showHidden = showHidden,
                limit = limit
            )
            FileCategory.RECENT -> queryRecentMediaStoreFiles(showHidden = showHidden, limit = 120)
        }
    }

    private fun queryMediaStore(uri: Uri, limit: Int, showHidden: Boolean): List<FileItem> {
        val result = mutableListOf<FileItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )
        runCatching {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                while (cursor.moveToNext() && result.size < limit) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val hidden = name.startsWith(".")
                    if (!showHidden && hidden) continue
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val size = cursor.getLong(sizeCol)
                    val dateSec = cursor.getLong(dateCol)
                    val contentUri = ContentUris.withAppendedId(uri, id).toString()
                    val ext = name.substringAfterLast('.', "")
                    val mime = cursor.getString(mimeCol) ?: FileFormatters.getMimeType(ext)
                    val lastModMs = dateSec * 1000L
                    result.add(
                        FileItem(
                            id = if (path.isNotEmpty()) path else contentUri,
                            name = name,
                            path = path.ifEmpty { contentUri },
                            uriString = if (path.isNotEmpty() && File(path).canRead()) Uri.fromFile(File(path)).toString() else contentUri,
                            isDirectory = false,
                            size = size,
                            lastModified = lastModMs,
                            mimeType = mime,
                            extension = ext,
                            isHidden = hidden,
                            typeGroup = FileFormatters.classifyFileType(false, ext, mime),
                            formattedSize = FileFormatters.formatFileSize(size),
                            formattedDate = FileFormatters.formatDateSmart(lastModMs)
                        )
                    )
                }
            }
        }
        return result
    }

    private fun queryRecentMediaStoreFiles(showHidden: Boolean, limit: Int): List<FileItem> {
        val filesUri = MediaStore.Files.getContentUri("external")
        val result = mutableListOf<FileItem>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val cutoffSec = (System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000) / 1000L
        runCatching {
            context.contentResolver.query(
                filesUri,
                projection,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} >= ? AND ${MediaStore.Files.FileColumns.SIZE} > 0",
                arrayOf(cutoffSec.toString()),
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext() && result.size < limit) {
                    val id = cursor.getLong(idCol)
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val name = cursor.getString(nameCol) ?: path.substringAfterLast('/')
                    if (name.isEmpty()) continue
                    val hidden = name.startsWith(".")
                    if (!showHidden && hidden) continue
                    val ext = name.substringAfterLast('.', "")
                    val mime = cursor.getString(mimeCol) ?: FileFormatters.getMimeType(ext)
                    val contentUri = ContentUris.withAppendedId(filesUri, id).toString()
                    val fSize = cursor.getLong(sizeCol)
                    val lastModMs = cursor.getLong(dateCol) * 1000L
                    result.add(
                        FileItem(
                            id = if (path.isNotEmpty()) path else contentUri,
                            name = name,
                            path = path.ifEmpty { contentUri },
                            uriString = if (path.isNotEmpty() && File(path).canRead()) Uri.fromFile(File(path)).toString() else contentUri,
                            isDirectory = false,
                            size = fSize,
                            lastModified = lastModMs,
                            mimeType = mime,
                            extension = ext,
                            isHidden = hidden,
                            typeGroup = FileFormatters.classifyFileType(false, ext, mime),
                            formattedSize = FileFormatters.formatFileSize(fSize),
                            formattedDate = FileFormatters.formatDateSmart(lastModMs)
                        )
                    )
                }
            }
        }
        return result
    }

    private fun queryFilesByExtensions(
        extensions: Set<String>,
        showHidden: Boolean,
        limit: Int
    ): List<FileItem> {
        val result = mutableListOf<FileItem>()
        val seen = HashSet<String>()
        val filesUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        runCatching {
            context.contentResolver.query(
                filesUri,
                projection,
                "${MediaStore.Files.FileColumns.SIZE} > 0",
                null,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext() && result.size < limit) {
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val name = cursor.getString(nameCol) ?: path.substringAfterLast('/')
                    val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    if (ext !in extensions) continue
                    val hidden = name.startsWith(".")
                    if (!showHidden && hidden) continue
                    val id = cursor.getLong(idCol)
                    val uniqueKey = path.ifEmpty { id.toString() }
                    if (!seen.add(uniqueKey)) continue
                    val mime = cursor.getString(mimeCol) ?: FileFormatters.getMimeType(ext)
                    val contentUri = ContentUris.withAppendedId(filesUri, id).toString()
                    val fSize = cursor.getLong(sizeCol)
                    val lastModMs = cursor.getLong(dateCol) * 1000L
                    result.add(
                        FileItem(
                            id = uniqueKey,
                            name = name,
                            path = path.ifEmpty { contentUri },
                            uriString = if (path.isNotEmpty() && File(path).canRead()) Uri.fromFile(File(path)).toString() else contentUri,
                            isDirectory = false,
                            size = fSize,
                            lastModified = lastModMs,
                            mimeType = mime,
                            extension = ext,
                            isHidden = hidden,
                            typeGroup = FileFormatters.classifyFileType(false, ext, mime),
                            formattedSize = FileFormatters.formatFileSize(fSize),
                            formattedDate = FileFormatters.formatDateSmart(lastModMs)
                        )
                    )
                }
            }
        }

        // Quick scan in Downloads only for newly added unindexed files (e.g. APKs/Archives)
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloads != null && downloads.canRead() && result.size < limit) {
            downloads.listFiles()?.forEach { child ->
                if (!child.isDirectory) {
                    val ext = child.extension.lowercase(Locale.ROOT)
                    if (ext in extensions && seen.add(child.absolutePath)) {
                        mapFileToItem(child, showHidden)?.let { result.add(it) }
                    }
                }
            }
        }
        return result.sortedByDescending { it.lastModified }
    }

    fun searchFiles(
        query: String,
        rootPath: String,
        isSafUri: Boolean,
        showHidden: Boolean,
        deepSearch: Boolean
    ): Flow<List<FileItem>> = flow {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val matches = mutableListOf<FileItem>()
        if (isSafUri) {
            val doc = DocumentFile.fromTreeUri(context, Uri.parse(rootPath))
            doc?.listFiles()?.forEach { child ->
                coroutineContext.ensureActive()
                val name = child.name ?: return@forEach
                if (!showHidden && name.startsWith(".")) return@forEach
                if (name.lowercase(Locale.ROOT).contains(trimmed)) {
                    val isDir = child.isDirectory
                    val ext = if (isDir) "" else name.substringAfterLast('.', "")
                    val mime = child.type ?: if (isDir) "resource/folder" else FileFormatters.getMimeType(ext)
                    matches.add(
                        FileItem(
                            id = child.uri.toString(),
                            name = name,
                            path = child.uri.toString(),
                            uriString = child.uri.toString(),
                            isDirectory = isDir,
                            size = if (isDir) 0L else child.length(),
                            lastModified = child.lastModified(),
                            mimeType = mime,
                            extension = ext,
                            isHidden = name.startsWith("."),
                            isSafDocument = true,
                            typeGroup = FileFormatters.classifyFileType(isDir, ext, mime)
                        )
                    )
                }
            }
            emit(matches)
            return@flow
        }

        val startDir = File(rootPath)
        if (!startDir.exists() || !startDir.isDirectory) {
            emit(emptyList())
            return@flow
        }

        if (!deepSearch) {
            startDir.listFiles()?.forEach { file ->
                coroutineContext.ensureActive()
                if (file.name.lowercase(Locale.ROOT).contains(trimmed)) {
                    mapFileToItem(file, showHidden)?.let { matches.add(it) }
                }
            }
            emit(sortItems(matches, SortOption.NAME, SortDirection.ASCENDING))
            return@flow
        }

        val queue = ArrayDeque<File>()
        queue.add(startDir)
        var scanned = 0
        while (queue.isNotEmpty() && matches.size < 300) {
            coroutineContext.ensureActive()
            val dir = queue.removeFirst()
            val children = dir.listFiles() ?: continue
            for (child in children) {
                coroutineContext.ensureActive()
                val hidden = child.isHidden || child.name.startsWith(".")
                if (!showHidden && hidden) continue
                if (child.name.lowercase(Locale.ROOT).contains(trimmed)) {
                    mapFileToItem(child, showHidden)?.let { matches.add(it) }
                    if (matches.size % 25 == 0) emit(matches.toList())
                }
                if (child.isDirectory && child.name != "Android") {
                    queue.add(child)
                }
                scanned++
                if (scanned > 15_000) break
            }
        }
        emit(sortItems(matches, SortOption.NAME, SortDirection.ASCENDING))
    }.flowOn(Dispatchers.IO)

    suspend fun createFolder(parentPath: String, isSafUri: Boolean, name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = name.trim()
                require(clean.isNotEmpty() && !clean.contains('/')) { "Invalid folder name" }
                if (isSafUri) {
                    val parent = DocumentFile.fromTreeUri(context, Uri.parse(parentPath))
                        ?: error("Cannot access SAF folder")
                    check(parent.createDirectory(clean) != null) { "Failed to create folder" }
                } else {
                    val target = File(parentPath, clean)
                    check(!target.exists()) { "Folder already exists" }
                    check(target.mkdirs()) { "Permission denied creating folder" }
                }
            }
        }

    suspend fun createEmptyFile(parentPath: String, isSafUri: Boolean, name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = name.trim()
                require(clean.isNotEmpty() && !clean.contains('/')) { "Invalid file name" }
                if (isSafUri) {
                    val parent = DocumentFile.fromTreeUri(context, Uri.parse(parentPath))
                        ?: error("Cannot access SAF folder")
                    val ext = clean.substringAfterLast('.', "")
                    check(parent.createFile(FileFormatters.getMimeType(ext), clean) != null) { "Failed to create file" }
                } else {
                    val target = File(parentPath, clean)
                    check(!target.exists()) { "File already exists" }
                    check(target.createNewFile()) { "Permission denied creating file" }
                }
            }
        }

    suspend fun renameItem(item: FileItem, newName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = newName.trim()
                require(clean.isNotEmpty() && !clean.contains('/')) { "Invalid name" }
                if (item.isSafDocument) {
                    val doc = DocumentFile.fromSingleUri(context, Uri.parse(item.uriString))
                        ?: error("Cannot access SAF document")
                    check(doc.renameTo(clean)) { "Failed to rename document" }
                } else {
                    val src = File(item.path)
                    val dst = File(src.parentFile, clean)
                    check(!dst.exists()) { "An item named '$clean' already exists" }
                    check(src.renameTo(dst)) { "Rename failed (check storage permissions)" }
                }
            }
        }

    suspend fun deleteItems(items: List<FileItem>): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                var deleted = 0
                for (item in items) {
                    if (item.isSafDocument) {
                        val doc = DocumentFile.fromSingleUri(context, Uri.parse(item.uriString))
                        if (doc?.delete() == true) deleted++
                    } else {
                        val file = File(item.path)
                        if (file.exists() && file.deleteRecursively()) deleted++
                    }
                }
                check(deleted > 0) { "No files could be deleted (check storage permissions)" }
                deleted
            }
        }

    fun pasteItems(
        items: List<FileItem>,
        destinationDirPath: String,
        isDestinationSaf: Boolean,
        isMove: Boolean
    ): Flow<Pair<Float, String>> = flow {
        val totalCount = items.size.coerceAtLeast(1)
        items.forEachIndexed { idx, item ->
            coroutineContext.ensureActive()
            emit((idx.toFloat() / totalCount.toFloat()) to "${if (isMove) "Moving" else "Copying"} ${item.name}")

            if (!item.isSafDocument && !isDestinationSaf) {
                val src = File(item.path)
                val destDir = File(destinationDirPath)
                require(destDir.exists() && destDir.isDirectory) { "Destination folder unavailable" }
                require(!destDir.absolutePath.startsWith(src.absolutePath + File.separator) && destDir.absolutePath != src.absolutePath) {
                    "Cannot copy or move a folder into itself"
                }

                val target = resolveConflictFile(destDir, src.name)
                if (isMove) {
                    if (!src.renameTo(target)) {
                        copyFileRecursive(src, target)
                        src.deleteRecursively()
                    }
                } else {
                    copyFileRecursive(src, target)
                }
            } else {
                // Cross-SAF or SAF copy fallback via streams
                val destParent = if (isDestinationSaf) {
                    DocumentFile.fromTreeUri(context, Uri.parse(destinationDirPath))
                        ?: error("Cannot access SAF destination")
                } else null

                if (destParent != null && !item.isDirectory) {
                    val created = destParent.createFile(item.mimeType, item.name)
                        ?: error("Cannot create SAF file")
                    val input = if (item.isSafDocument) {
                        context.contentResolver.openInputStream(Uri.parse(item.uriString))
                    } else {
                        FileInputStream(File(item.path))
                    }
                    input?.use { inp ->
                        context.contentResolver.openOutputStream(created.uri)?.use { out ->
                            inp.copyTo(out, bufferSize = 64 * 1024)
                        }
                    }
                    if (isMove) {
                        if (item.isSafDocument) {
                            DocumentFile.fromSingleUri(context, Uri.parse(item.uriString))?.delete()
                        } else {
                            File(item.path).delete()
                        }
                    }
                } else if (!isDestinationSaf && item.isSafDocument && !item.isDirectory) {
                    val target = resolveConflictFile(File(destinationDirPath), item.name)
                    context.contentResolver.openInputStream(Uri.parse(item.uriString))?.use { inp ->
                        FileOutputStream(target).use { out ->
                            inp.copyTo(out, bufferSize = 64 * 1024)
                        }
                    }
                    if (isMove) {
                        DocumentFile.fromSingleUri(context, Uri.parse(item.uriString))?.delete()
                    }
                }
            }
            emit(((idx + 1).toFloat() / totalCount.toFloat()) to "Processed ${item.name}")
        }
    }.flowOn(Dispatchers.IO)

    private fun resolveConflictFile(destDir: File, originalName: String): File {
        var candidate = File(destDir, originalName)
        if (!candidate.exists()) return candidate
        val base = originalName.substringBeforeLast('.', originalName)
        val ext = if (originalName.contains('.')) ".${originalName.substringAfterLast('.')}" else ""
        var counter = 1
        while (candidate.exists()) {
            candidate = File(destDir, "$base ($counter)$ext")
            counter++
        }
        return candidate
    }

    private fun copyFileRecursive(src: File, dst: File) {
        if (src.isDirectory) {
            if (!dst.exists()) dst.mkdirs()
            src.listFiles()?.forEach { child ->
                copyFileRecursive(child, File(dst, child.name))
            }
        } else {
            dst.parentFile?.mkdirs()
            FileInputStream(src).use { input ->
                FileOutputStream(dst).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }
        }
    }

    fun compressToZip(
        items: List<FileItem>,
        destinationDirPath: String,
        archiveName: String
    ): Flow<Pair<Float, String>> = flow {
        val cleanName = archiveName.trim().let { if (it.endsWith(".zip", ignoreCase = true)) it else "$it.zip" }
        val destFile = resolveConflictFile(File(destinationDirPath), cleanName)
        val filesToZip = mutableListOf<Pair<File, String>>()

        for (item in items) {
            val file = File(item.path)
            if (file.exists()) {
                collectZipEntries(file, file.name, filesToZip)
            }
        }

        val total = filesToZip.size.coerceAtLeast(1)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(destFile))).use { zos ->
            val buffer = ByteArray(64 * 1024)
            filesToZip.forEachIndexed { idx, (file, entryName) ->
                coroutineContext.ensureActive()
                emit((idx.toFloat() / total.toFloat()) to "Compressing ${file.name}")
                if (file.isDirectory) {
                    val dirEntry = if (entryName.endsWith("/")) entryName else "$entryName/"
                    zos.putNextEntry(ZipEntry(dirEntry))
                    zos.closeEntry()
                } else {
                    zos.putNextEntry(ZipEntry(entryName))
                    BufferedInputStream(FileInputStream(file)).use { bis ->
                        var count: Int
                        while (bis.read(buffer).also { count = it } != -1) {
                            zos.write(buffer, 0, count)
                        }
                    }
                    zos.closeEntry()
                }
            }
        }
        emit(1f to "Created ${destFile.name}")
    }.flowOn(Dispatchers.IO)

    private fun collectZipEntries(file: File, entryPath: String, out: MutableList<Pair<File, String>>) {
        if (file.isDirectory) {
            out.add(file to "$entryPath/")
            file.listFiles()?.forEach { child ->
                collectZipEntries(child, "$entryPath/${child.name}", out)
            }
        } else {
            out.add(file to entryPath)
        }
    }

    fun extractZip(
        zipItem: FileItem,
        destinationDirPath: String
    ): Flow<Pair<Float, String>> = flow {
        val folderName = zipItem.name.substringBeforeLast('.', zipItem.name)
        val targetRoot = resolveConflictFile(File(destinationDirPath), folderName)
        targetRoot.mkdirs()
        val canonicalTarget = targetRoot.canonicalPath

        val inputStream = if (zipItem.isSafDocument || zipItem.uriString.startsWith("content://")) {
            context.contentResolver.openInputStream(Uri.parse(zipItem.uriString))
        } else {
            FileInputStream(File(zipItem.path))
        } ?: error("Cannot open archive")

        var count = 0
        inputStream.use { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zis ->
                val buffer = ByteArray(64 * 1024)
                var entry = zis.nextEntry
                while (entry != null) {
                    coroutineContext.ensureActive()
                    val outFile = File(targetRoot, entry.name)
                    // ZipSlip protection
                    val canonicalOut = outFile.canonicalPath
                    require(canonicalOut.startsWith(canonicalTarget + File.separator) || canonicalOut == canonicalTarget) {
                        "Unsafe ZIP path detected: ${entry.name}"
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                            var read: Int
                            while (zis.read(buffer).also { read = it } != -1) {
                                bos.write(buffer, 0, read)
                            }
                        }
                    }
                    count++
                    emit(((count % 20) / 20f).coerceIn(0.1f, 0.95f) to "Extracting ${entry.name}")
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        emit(1f to "Extracted $count entries to ${targetRoot.name}")
    }.flowOn(Dispatchers.IO)

    suspend fun getFileProperties(item: FileItem, includeChecksum: Boolean = false): FileProperties =
        withContext(Dispatchers.IO) {
            val file = if (!item.isSafDocument && !item.path.startsWith("content://")) File(item.path) else null
            val perms = if (file != null) {
                buildString {
                    append(if (file.canRead()) "R" else "-")
                    append(if (file.canWrite()) "W" else "-")
                    append(if (file.canExecute()) "X" else "-")
                }
            } else "SAF"

            var folderCount: Int? = null
            var folderSize: Long? = null
            if (item.isDirectory && file != null) {
                var cnt = 0
                var total = 0L
                file.walkTopDown().maxDepth(4).forEach {
                    if (it != file) {
                        cnt++
                        if (it.isFile) total += it.length()
                    }
                }
                folderCount = cnt
                folderSize = total
            }

            var width: Int? = null
            var height: Int? = null
            var durationMs: Long? = null
            var apkPkg: String? = null
            var apkVer: String? = null

            if (item.typeGroup == FileTypeGroup.IMAGE && file != null) {
                runCatching {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                    if (opts.outWidth > 0 && opts.outHeight > 0) {
                        width = opts.outWidth
                        height = opts.outHeight
                    }
                }
            } else if ((item.typeGroup == FileTypeGroup.AUDIO || item.typeGroup == FileTypeGroup.VIDEO) && file != null) {
                runCatching {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(file.absolutePath)
                    durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                    height = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                    mmr.release()
                }
            } else if (item.typeGroup == FileTypeGroup.APK && file != null) {
                runCatching {
                    val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getPackageArchiveInfo(
                            file.absolutePath,
                            PackageManager.PackageInfoFlags.of(0)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                    }
                    apkPkg = pkgInfo?.packageName
                    apkVer = pkgInfo?.versionName
                }
            }

            val checksum = if (includeChecksum && !item.isDirectory && file != null && file.length() <= 250L * 1024 * 1024) {
                runCatching {
                    val digest = MessageDigest.getInstance("SHA-256")
                    FileInputStream(file).use { fis ->
                        val buf = ByteArray(64 * 1024)
                        var read: Int
                        while (fis.read(buf).also { read = it } != -1) {
                            digest.update(buf, 0, read)
                        }
                    }
                    digest.digest().joinToString("") { "%02x".format(it) }
                }.getOrNull()
            } else null

            FileProperties(
                item = item,
                permissionsText = perms,
                itemCountInFolder = folderCount,
                folderTotalSize = folderSize,
                mediaWidth = width,
                mediaHeight = height,
                mediaDurationMs = durationMs,
                apkPackageName = apkPkg,
                apkVersionName = apkVer,
                sha256 = checksum
            )
        }

    fun resolveContentUri(item: FileItem): Uri {
        return if (item.uriString.startsWith("content://")) {
            Uri.parse(item.uriString)
        } else {
            val file = File(item.path)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }

    fun createShareIntent(items: List<FileItem>): Intent {
        return if (items.size == 1) {
            val item = items.first()
            val uri = resolveContentUri(item)
            Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val uris = ArrayList<Uri>(items.map { resolveContentUri(it) })
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    fun createOpenExternalIntent(item: FileItem): Intent {
        return createOpenAsIntent(item, item.mimeType)
    }

    fun createOpenAsIntent(item: FileItem, mimeType: String): Intent {
        val uri = resolveContentUri(item)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
