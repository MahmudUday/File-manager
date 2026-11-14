package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.example.data.model.FileTypeGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object FileFormatters {

    private val fullDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    }
    private val shortDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    }
    private val timeOnlyFormat = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) {
            "$bytes B"
        } else {
            String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "00:00"
        val totalSeconds = durationMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatDateSmart(timestampMs: Long): String {
        if (timestampMs <= 0L) return "Unknown"
        val now = System.currentTimeMillis()
        val diff = now - timestampMs
        return when {
            diff in 0 until 60_000L -> "Just now"
            diff in 60_000L until 3_600_000L -> "${diff / 60_000L}m ago"
            diff in 3_600_000L until 86_400_000L -> "Today, ${timeOnlyFormat.get()?.format(Date(timestampMs)) ?: ""}"
            else -> shortDateFormat.get()?.format(Date(timestampMs)) ?: ""
        }
    }

    fun formatFullDate(timestampMs: Long): String {
        if (timestampMs <= 0L) return "Unknown"
        return fullDateFormat.get()?.format(Date(timestampMs)) ?: ""
    }

    fun getMimeType(extension: String): String {
        if (extension.isEmpty()) return "application/octet-stream"
        val lower = extension.lowercase(Locale.ROOT)
        return when (lower) {
            "mkv" -> "video/x-matroska"
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            "m4a" -> "audio/mp4"
            "ogg", "opus" -> "audio/ogg"
            "webp" -> "image/webp"
            "heic", "heif" -> "image/heic"
            "apk" -> "application/vnd.android.package-archive"
            "zip" -> "application/zip"
            "rar" -> "application/vnd.rar"
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"
            "json" -> "application/json"
            "md" -> "text/markdown"
            "kt", "java", "py", "js", "ts", "xml", "yml", "yaml", "gradle", "kts", "sh", "c", "cpp", "h" -> "text/plain"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(lower) ?: "application/octet-stream"
        }
    }

    fun classifyFileType(isDirectory: Boolean, extension: String, mimeType: String): FileTypeGroup {
        if (isDirectory) return FileTypeGroup.FOLDER
        val ext = extension.lowercase(Locale.ROOT)
        val mime = mimeType.lowercase(Locale.ROOT)
        return when {
            ext in IMAGE_EXTENSIONS || mime.startsWith("image/") -> FileTypeGroup.IMAGE
            ext in VIDEO_EXTENSIONS || mime.startsWith("video/") -> FileTypeGroup.VIDEO
            ext in AUDIO_EXTENSIONS || mime.startsWith("audio/") -> FileTypeGroup.AUDIO
            ext == "apk" || mime == "application/vnd.android.package-archive" -> FileTypeGroup.APK
            ext in ARCHIVE_EXTENSIONS -> FileTypeGroup.ARCHIVE
            ext in CODE_EXTENSIONS -> FileTypeGroup.CODE
            ext in DOCUMENT_EXTENSIONS || mime.startsWith("text/") || mime.contains("pdf") || mime.contains("document") -> FileTypeGroup.DOCUMENT
            else -> FileTypeGroup.OTHER
        }
    }

    val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif")
    val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "mov", "avi", "m4v", "3gp", "ts", "flv")
    val AUDIO_EXTENSIONS = setOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "opus", "wma", "mid")
    val DOCUMENT_EXTENSIONS = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "md", "epub")
    val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "jar")
    val CODE_EXTENSIONS = setOf("kt", "kts", "java", "xml", "json", "js", "ts", "py", "html", "css", "c", "cpp", "h", "sh", "yaml", "yml", "sql", "gradle")
}

object PermissionHelper {
    fun hasAllFilesAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasMediaPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasAnyStorageAccess(context: Context): Boolean {
        return hasAllFilesAccess(context) || hasMediaPermissions(context)
    }

    fun requiredRuntimePermissions(): Array<String> {
        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }.toTypedArray()
    }
}
