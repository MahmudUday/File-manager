package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.VideoFile
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

private val DialogCardShape = RoundedCornerShape(24.dp)
private val FloatingBarShape = RoundedCornerShape(16.dp)
private val SheetRowShape = RoundedCornerShape(12.dp)

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
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        ) + scaleIn(initialScale = 0.94f) + fadeIn(tween(180)),
        exit = slideOutVertically { it } + fadeOut(tween(140)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(FloatingBarShape)
                .background(onyx.elevatedSurface)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(onyx.accentGlow.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.5f),
                            radius = size.width * 0.45f
                        ),
                        center = Offset(size.width * 0.1f, size.height * 0.5f),
                        radius = size.width * 0.45f
                    )
                }
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(onyx.glowBorder, onyx.accent.copy(alpha = 0.35f), onyx.glowBorder)
                    ),
                    shape = FloatingBarShape
                )
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
private fun AnimatedGlowDialogCard(
    borderColor: Color? = null,
    glowColor: Color? = null,
    content: @Composable () -> Unit
) {
    val onyx = LocalOnyxColors.current
    val resolvedBorder = borderColor ?: onyx.glowBorder
    val resolvedGlow = glowColor ?: onyx.accentGlow

    val scaleAnim = remember { Animatable(0.88f) }
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
        )
    }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(160, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                alpha = alphaAnim.value
            }
            .clip(DialogCardShape)
            .background(onyx.elevatedSurface)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(resolvedGlow.copy(alpha = 0.36f), Color.Transparent),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 0.85f
                    ),
                    center = Offset(size.width * 0.5f, 0f),
                    radius = size.width * 0.85f
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(resolvedBorder, onyx.hairlineBorder, resolvedBorder.copy(alpha = 0.45f))
                ),
                shape = DialogCardShape
            )
            .padding(22.dp)
    ) {
        content()
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

    Dialog(onDismissRequest = onDismiss) {
        AnimatedGlowDialogCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = onyx.textPrimary)
                Spacer(modifier = Modifier.height(14.dp))
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_text_input")
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = onyx.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                        enabled = text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = onyx.accent,
                            contentColor = onyx.onAccent
                        )
                    ) {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    count: Int,
    firstName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val onyx = LocalOnyxColors.current
    Dialog(onDismissRequest = onDismiss) {
        AnimatedGlowDialogCard(
            borderColor = ErrorCrimson.copy(alpha = 0.65f),
            glowColor = ErrorCrimson.copy(alpha = 0.30f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Delete permanently?", style = MaterialTheme.typography.titleLarge, color = onyx.textPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (count == 1) {
                        "\"$firstName\" will be permanently removed from storage. This cannot be undone."
                    } else {
                        "$count selected items will be permanently removed from storage. This cannot be undone."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = onyx.textSecondary
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = onyx.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson, contentColor = Color.White),
                        modifier = Modifier.testTag("confirm_delete_button")
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun OperationProgressDialog(state: OperationState.Running) {
    val onyx = LocalOnyxColors.current
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        AnimatedGlowDialogCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(state.title, style = MaterialTheme.typography.titleLarge, color = onyx.textPrimary)
                Spacer(modifier = Modifier.height(14.dp))
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
        }
    }
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
        val enterAnim = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            enterAnim.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = enterAnim.value
                    translationY = (1f - enterAnim.value) * 24f
                }
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
                        .clip(SheetRowShape)
                        .background(if (selected) onyx.accentSoft else Color.Transparent)
                        .border(
                            width = if (selected) 0.9.dp else 0.dp,
                            color = if (selected) onyx.glowBorder else Color.Transparent,
                            shape = SheetRowShape
                        )
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
    onOpenAs: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onCopyPath: () -> Unit,
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
        val enterAnim = remember { Animatable(0f) }
        LaunchedEffect(item.id) {
            enterAnim.animateTo(1f, tween(210, easing = FastOutSlowInEasing))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = enterAnim.value
                    translationY = (1f - enterAnim.value) * 26f
                }
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
            HorizontalDivider(color = onyx.glowBorder.copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(8.dp))

            if (!item.isDirectory) {
                if (item.extension.isBlank()) {
                    SheetActionRow(Icons.Rounded.Tune, "Open As...", tint = onyx.accent) {
                        onDismiss(); onOpenAs()
                    }
                    SheetActionRow(Icons.Rounded.OpenInNew, "Open With External App") {
                        onDismiss(); onOpenExternal()
                    }
                } else {
                    SheetActionRow(Icons.Rounded.OpenInNew, "Open With External App") {
                        onDismiss(); onOpenExternal()
                    }
                    SheetActionRow(Icons.Rounded.Tune, "Open As...") {
                        onDismiss(); onOpenAs()
                    }
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
            SheetActionRow(Icons.Rounded.ContentCopy, "Copy Path") {
                onDismiss(); onCopyPath()
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
            .clip(SheetRowShape)
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
        val enterAnim = remember { Animatable(0f) }
        LaunchedEffect(item.id) {
            enterAnim.animateTo(1f, tween(210, easing = FastOutSlowInEasing))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = enterAnim.value
                    translationY = (1f - enterAnim.value) * 24f
                }
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
            HorizontalDivider(color = onyx.glowBorder.copy(alpha = 0.45f))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenAsBottomSheet(
    item: FileItem,
    onOpenTextEditor: () -> Unit,
    onSelectMimeType: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val onyx = LocalOnyxColors.current
    val options = remember {
        listOf(
            Triple(Icons.Rounded.Description, "Text / Code (External)", "text/plain"),
            Triple(Icons.Rounded.Image, "Image", "image/*"),
            Triple(Icons.Rounded.AudioFile, "Audio", "audio/*"),
            Triple(Icons.Rounded.VideoFile, "Video", "video/*"),
            Triple(Icons.Rounded.PictureAsPdf, "PDF / Document", "application/pdf"),
            Triple(Icons.Rounded.Apps, "Any App (*/*)", "*/*")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = onyx.elevatedSurface
    ) {
        val enterAnim = remember { Animatable(0f) }
        LaunchedEffect(item.id) {
            enterAnim.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = enterAnim.value
                    translationY = (1f - enterAnim.value) * 24f
                }
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileThumbnail(item = item, size = 42.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Open As",
                        style = MaterialTheme.typography.titleLarge,
                        color = onyx.textPrimary
                    )
                    Text(
                        text = item.name,
                        style = MonoMetadataStyle,
                        color = onyx.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = onyx.glowBorder.copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SheetRowShape)
                    .clickable {
                        onDismiss()
                        onOpenTextEditor()
                    }
                    .padding(horizontal = 10.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Code, contentDescription = "Built-in Text Editor", tint = onyx.accent, modifier = Modifier.size(21.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Text(text = "Built-in Text / Code Viewer", style = MaterialTheme.typography.bodyLarge, color = onyx.accent, modifier = Modifier.weight(1f))
                Text(text = "INTERNAL", style = MonoMetadataStyle, color = onyx.accent)
            }

            options.forEach { (icon, label, mime) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SheetRowShape)
                        .clickable {
                            onDismiss()
                            onSelectMimeType(mime)
                        }
                        .padding(horizontal = 10.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = label, tint = onyx.accent, modifier = Modifier.size(21.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = label, style = MaterialTheme.typography.bodyLarge, color = onyx.textPrimary, modifier = Modifier.weight(1f))
                    Text(text = mime, style = MonoMetadataStyle, color = onyx.textTertiary)
                }
            }
        }
    }
}
