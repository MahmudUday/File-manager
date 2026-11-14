package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ClipboardMode
import com.example.data.model.ClipboardState
import com.example.data.model.FileItem
import com.example.data.model.FileProperties
import com.example.data.model.FileTypeGroup
import com.example.data.model.OperationState
import com.example.data.model.SortDirection
import com.example.data.model.SortOption
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.MonoMetadataStyle
import com.example.util.FileFormatters

@Composable
fun ClipboardFloatingBar(
    clipboard: ClipboardState,
    onPasteHere: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onyx = LocalOnyxColors.current
    AnimatedVisibility(
        visible = clipboard.isActive,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(onyx.elevatedSurface)
                .border(1.dp, onyx.accent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentPaste,
                contentDescription = null,
                tint = onyx.accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${if (clipboard.mode == ClipboardMode.CUT) "Move" else "Copy"} ${clipboard.items.size} item(s)",
                    style = MaterialTheme.typography.titleSmall,
                    color = onyx.textPrimary
                )
                Text(
                    text = "Navigate to destination and tap Paste",
                    style = MaterialTheme.typography.bodySmall,
                    color = onyx.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(
                onClick = onPasteHere,
                modifier = Modifier.testTag("paste_here_button")
            ) {
                Text("Paste Here", color = onyx.accent)
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cancel clipboard",
                    tint = onyx.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initialValue: String,
    placeholder: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val onyx = LocalOnyxColors.current
    var text by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = onyx.elevatedSurface,
        title = { Text(title, color = onyx.textPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text(placeholder, color = onyx.textTertiary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = onyx.accent,
                    unfocusedBorderColor = onyx.hairlineBorder,
                    focusedTextColor = onyx.textPrimary,
                    unfocusedTextColor = onyx.textPrimary,
                    cursorColor = onyx.accent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_text_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = onyx.accent)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = onyx.textSecondary)
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    count: Int,
    firstName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val onyx = LocalOnyxColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = onyx.elevatedSurface,
        title = { Text("Delete permanently?", color = onyx.textPrimary) },
        text = {
            Text(
                text = if (count == 1) {
                    "\"$firstName\" will be permanently removed from storage. This cannot be undone."
                } else {
                    "$count selected items will be permanently removed from storage. This cannot be undone."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = onyx.textSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = onyx.textSecondary)
            }
        }
    )
}

@Composable
fun OperationProgressDialog(state: OperationState.Running) {
    val onyx = LocalOnyxColors.current
    AlertDialog(
        onDismissRequest = {},
        containerColor = onyx.elevatedSurface,
        title = { Text(state.title, color = onyx.textPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { state.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = onyx.accent,
                    trackColor = onyx.hairlineBorder
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = state.detail,
                    style = MonoMetadataStyle,
                    color = onyx.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentOption: SortOption,
    currentDirection: SortDirection,
    onSelect: (SortOption, SortDirection) -> Unit,
    onDismiss: () -> Unit
) {
    val onyx = LocalOnyxColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = onyx.elevatedSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleLarge,
                color = onyx.textPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            SortOption.entries.forEach { option ->
                val selected = option == currentOption
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) onyx.accentSoft else Color.Transparent)
                        .clickable {
                            val nextDir = if (selected) {
                                if (currentDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                            } else {
                                currentDirection
                            }
                            onSelect(option, nextDir)
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) onyx.accent else onyx.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(
                            imageVector = if (currentDirection == SortDirection.ASCENDING) {
                                Icons.Rounded.ArrowUpward
                            } else {
                                Icons.Rounded.ArrowDownward
                            },
                            contentDescription = currentDirection.label,
                            tint = onyx.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = onyx.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionBottomSheet(
    item: FileItem,
    onOpen: () -> Unit,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onCompress: () -> Unit,
    onExtract: () -> Unit,
    onProperties: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val onyx = LocalOnyxColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = onyx.elevatedSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileThumbnail(item = item, size = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = onyx.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (item.isDirectory) "${item.childCount} items" else FileFormatters.formatFileSize(item.size),
                        style = MonoMetadataStyle,
                        color = onyx.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = onyx.hairlineBorder)
            Spacer(modifier = Modifier.height(8.dp))

            if (!item.isDirectory) {
                SheetActionRow(Icons.Rounded.OpenInNew, "Open With External App") {
                    onDismiss(); onOpenExternal()
                }
                SheetActionRow(Icons.Rounded.Share, "Share") {
                    onDismiss(); onShare()
                }
            } else {
                SheetActionRow(Icons.Rounded.OpenInNew, "Open Folder") {
                    onDismiss(); onOpen()
                }
            }

            if (item.extension.equals("zip", ignoreCase = true)) {
                SheetActionRow(Icons.Rounded.Unarchive, "Extract Archive Here", tint = onyx.accent) {
                    onDismiss(); onExtract()
                }
            }

            SheetActionRow(Icons.Rounded.ContentPaste, "Copy") {
                onDismiss(); onCopy()
            }
            SheetActionRow(Icons.Rounded.ContentPaste, "Move / Cut") {
                onDismiss(); onCut()
            }
            SheetActionRow(Icons.Rounded.DriveFileRenameOutline, "Rename") {
                onDismiss(); onRename()
            }
            if (!item.isSafDocument && item.typeGroup != FileTypeGroup.ARCHIVE) {
                SheetActionRow(Icons.Rounded.FolderZip, "Compress to ZIP") {
                    onDismiss(); onCompress()
                }
            }
            SheetActionRow(Icons.Rounded.Info, "Properties") {
                onDismiss(); onProperties()
            }
            SheetActionRow(Icons.Rounded.Delete, "Delete", tint = ErrorCrimson) {
                onDismiss(); onDelete()
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val onyx = LocalOnyxColors.current
    val resolved = tint ?: onyx.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = resolved, modifier = Modifier.size(21.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = resolved)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePropertiesSheet(
    properties: FileProperties,
    isLoadingChecksum: Boolean,
    onComputeChecksum: () -> Unit,
    onDismiss: () -> Unit
) {
    val onyx = LocalOnyxColors.current
    val item = properties.item

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = onyx.elevatedSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileThumbnail(item = item, size = 50.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = onyx.textPrimary
                    )
                    Text(
                        text = item.mimeType,
                        style = MonoMetadataStyle,
                        color = onyx.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = onyx.hairlineBorder)
            Spacer(modifier = Modifier.height(12.dp))

            PropertyRow("Location", item.path)
            PropertyRow(
                "Size",
                if (item.isDirectory) {
                    val folderBytes = properties.folderTotalSize ?: 0L
                    "${FileFormatters.formatFileSize(folderBytes)} (${properties.itemCountInFolder ?: item.childCount} items)"
                } else {
                    "${FileFormatters.formatFileSize(item.size)} (${item.size} bytes)"
                }
            )
            PropertyRow("Modified", FileFormatters.formatFullDate(item.lastModified))
            PropertyRow("Permissions", properties.permissionsText)

            if (item.extension.isNotEmpty()) {
                PropertyRow("Format", item.extension.uppercase())
            }
            if (properties.mediaWidth != null && properties.mediaHeight != null) {
                PropertyRow("Resolution", "${properties.mediaWidth} × ${properties.mediaHeight}")
            }
            if (properties.mediaDurationMs != null && properties.mediaDurationMs > 0L) {
                PropertyRow("Duration", FileFormatters.formatDuration(properties.mediaDurationMs))
            }
            if (properties.apkPackageName != null) {
                PropertyRow("Package", properties.apkPackageName)
                properties.apkVersionName?.let { PropertyRow("Version", it) }
            }

            if (!item.isDirectory && !item.isSafDocument) {
                Spacer(modifier = Modifier.height(8.dp))
                if (properties.sha256 != null) {
                    PropertyRow("SHA-256", properties.sha256)
                } else {
                    OutlinedButton(
                        onClick = onComputeChecksum,
                        enabled = !isLoadingChecksum,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isLoadingChecksum) "Calculating SHA-256..." else "Calculate SHA-256 Checksum",
                            color = onyx.accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    val onyx = LocalOnyxColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = onyx.textTertiary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MonoMetadataStyle, color = onyx.textPrimary)
    }
}
