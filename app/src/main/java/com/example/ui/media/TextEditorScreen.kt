package com.example.ui.media

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileItem
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.MonoMetadataStyle
import com.example.util.FileFormatters

@Composable
fun TextEditorScreen(
    item: FileItem,
    onLoadText: suspend (FileItem) -> Result<String>,
    onSaveText: (FileItem, String) -> Unit,
    onClose: () -> Unit
) {
    val onyx = LocalOnyxColors.current
    val context = LocalContext.current

    var isLoading by remember(item.id) { mutableStateOf(true) }
    var loadError by remember(item.id) { mutableStateOf<String?>(null) }
    var originalText by remember(item.id) { mutableStateOf("") }
    var currentText by remember(item.id) { mutableStateOf("") }
    var isEditing by remember(item.id) { mutableStateOf(false) }
    var showSearch by remember(item.id) { mutableStateOf(false) }
    var searchQuery by remember(item.id) { mutableStateOf("") }
    var wordWrap by remember(item.id) { mutableStateOf(true) }

    LaunchedEffect(item.id) {
        isLoading = true
        loadError = null
        onLoadText(item)
            .onSuccess { text ->
                originalText = text
                currentText = text
                isLoading = false
            }
            .onFailure { err ->
                loadError = err.message ?: "Unable to read file"
                isLoading = false
            }
    }

    BackHandler {
        if (showSearch) {
            showSearch = false
            searchQuery = ""
        } else {
            onClose()
        }
    }

    val isDirty = currentText != originalText
    val lines = remember(currentText) { currentText.split('\n') }
    val matchCount = remember(currentText, searchQuery) {
        if (searchQuery.isBlank()) 0 else {
            Regex.escape(searchQuery).toRegex(RegexOption.IGNORE_CASE).findAll(currentText).count()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(onyx.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .testTag("text_editor_screen")
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(onyx.cardSurface)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose, modifier = Modifier.testTag("text_editor_back")) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close editor", tint = onyx.textPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isDirty) "${item.name} *" else item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isDirty) onyx.accent else onyx.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${lines.size} lines  •  ${currentText.length} chars  •  ${FileFormatters.formatFileSize(item.size)}",
                    style = MonoMetadataStyle,
                    color = onyx.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { showSearch = !showSearch }) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Find in file",
                    tint = if (showSearch) onyx.accent else onyx.textPrimary
                )
            }
            IconButton(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText(item.name, currentText))
                    Toast.makeText(context, "Copied text to clipboard", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy all", tint = onyx.textPrimary)
            }
            IconButton(
                onClick = { isEditing = !isEditing },
                modifier = Modifier.testTag("text_editor_mode_toggle")
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Rounded.Visibility else Icons.Rounded.Edit,
                    contentDescription = if (isEditing) "Read mode" else "Edit mode",
                    tint = if (isEditing) onyx.accent else onyx.textPrimary
                )
            }
            if (isDirty) {
                IconButton(
                    onClick = {
                        onSaveText(item, currentText)
                        originalText = currentText
                    },
                    modifier = Modifier.testTag("text_editor_save")
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = "Save file", tint = onyx.accent)
                }
            }
        }

        // Collapsible Search Bar
        AnimatedVisibility(visible = showSearch) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(onyx.cardSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Find in file...", color = onyx.textTertiary) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            searchQuery = ""
                            showSearch = false
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close find", tint = onyx.textSecondary)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = onyx.accent,
                        unfocusedBorderColor = onyx.hairlineBorder,
                        focusedTextColor = onyx.textPrimary,
                        unfocusedTextColor = onyx.textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "$matchCount",
                        style = MonoMetadataStyle,
                        color = onyx.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(onyx.accentSoft)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Mode & Wrap Status Pill Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(onyx.elevatedSurface)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEditing) "EDITING MODE" else "READER VIEW",
                style = MaterialTheme.typography.labelSmall,
                color = if (isEditing) onyx.accent else onyx.textTertiary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.extension.uppercase().ifEmpty { "TXT" },
                    style = MonoMetadataStyle,
                    color = onyx.textSecondary
                )
                if (!isEditing) {
                    Text(
                        text = if (wordWrap) "Wrap: ON" else "Wrap: OFF",
                        style = MonoMetadataStyle,
                        color = onyx.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(onyx.accentSoft)
                            .clickable { wordWrap = !wordWrap }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = onyx.subtleBorder)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        color = onyx.accent,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                loadError != null -> {
                    Text(
                        text = loadError.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = onyx.textSecondary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                isEditing -> {
                    val vScroll = rememberScrollState()
                    BasicTextField(
                        value = currentText,
                        onValueChange = { currentText = it },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            color = onyx.textPrimary
                        ),
                        cursorBrush = SolidColor(onyx.accent),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(vScroll)
                            .padding(14.dp)
                            .testTag("text_editor_field")
                    )
                }

                else -> {
                    val hScroll = rememberScrollState()
                    val codeModifier = if (wordWrap) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxSize()
                            .horizontalScroll(hScroll)
                    }
                    LazyColumn(
                        modifier = codeModifier.padding(vertical = 8.dp)
                    ) {
                        itemsIndexed(lines) { index, lineText ->
                            val highlighted = remember(lineText, searchQuery, onyx.accent, onyx.onAccent) {
                                buildHighlightedLine(
                                    line = lineText,
                                    query = searchQuery,
                                    highlightBg = onyx.accent.copy(alpha = 0.35f)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 1.5.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 19.sp,
                                        color = onyx.textTertiary,
                                        textAlign = TextAlign.End
                                    ),
                                    modifier = Modifier.width(38.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = highlighted,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        color = onyx.textPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildHighlightedLine(
    line: String,
    query: String,
    highlightBg: androidx.compose.ui.graphics.Color
): AnnotatedString {
    if (query.isBlank() || line.isEmpty()) return AnnotatedString(line)
    val regex = Regex.escape(query).toRegex(RegexOption.IGNORE_CASE)
    return buildAnnotatedString {
        append(line)
        regex.findAll(line).forEach { match ->
            addStyle(
                style = SpanStyle(background = highlightBg, fontWeight = FontWeight.Bold),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
    }
}
