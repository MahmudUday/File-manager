package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AccentPreset
import com.example.data.model.AppPreferences
import com.example.data.model.ThemeMode
import com.example.data.model.ViewMode
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.MonoMetadataStyle
import com.example.ui.theme.SuccessMint

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    hasAllFilesAccess: Boolean,
    maxRefreshRateHz: Int,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectAccent: (AccentPreset) -> Unit,
    onToggleHiddenFiles: (Boolean) -> Unit,
    onToggleFileExtensions: (Boolean) -> Unit,
    onSelectViewMode: (ViewMode) -> Unit,
    onToggleHighRefreshRate: (Boolean) -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onMountSafDrive: () -> Unit,
    onRemoveSafDrive: (String) -> Unit,
    onClearRecentHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onyx = LocalOnyxColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(onyx.background)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Column {
                Text(
                    text = "CUSTOMIZATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = onyx.accent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = onyx.textPrimary
                )
            }
        }

        // Theme Mode
        item(key = "theme_section") {
            Text(
                text = "AMOLED APPEARANCE",
                style = MaterialTheme.typography.labelMedium,
                color = onyx.textTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val selected = mode == preferences.themeMode
                    val border by animateColorAsState(
                        targetValue = if (selected) onyx.accent else onyx.subtleBorder,
                        label = "themeBorder"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) onyx.accentSoft else onyx.cardSurface)
                            .border(1.dp, border, RoundedCornerShape(16.dp))
                            .clickable { onSelectThemeMode(mode) }
                            .padding(14.dp)
                            .testTag("theme_mode_${mode.name}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = onyx.textPrimary
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = onyx.textSecondary
                            )
                        }
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = onyx.accent)
                        }
                    }
                }
            }
        }

        // Accent Swatches
        item(key = "accent_section") {
            Text(
                text = "ACCENT COLOR",
                style = MaterialTheme.typography.labelMedium,
                color = onyx.textTertiary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(onyx.cardSurface)
                    .border(0.8.dp, onyx.subtleBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccentPreset.entries.forEach { preset ->
                    val selected = preset == preferences.accentPreset
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (selected) 2.5.dp else 0.dp,
                                color = if (selected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(preset.color)
                            .clickable { onSelectAccent(preset) }
                            .testTag("accent_swatch_${preset.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = preset.label,
                                tint = Color(0xFF05070B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Browsing Preferences
        item(key = "browsing_section") {
            Text(
                text = "FILE BROWSING",
                style = MaterialTheme.typography.labelMedium,
                color = onyx.textTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(onyx.cardSurface)
                    .border(0.8.dp, onyx.subtleBorder, RoundedCornerShape(18.dp))
            ) {
                SettingToggleRow(
                    icon = Icons.Rounded.Visibility,
                    title = "Show Hidden Files",
                    subtitle = "Display dotfiles and hidden system folders",
                    checked = preferences.showHiddenFiles,
                    onCheckedChange = onToggleHiddenFiles,
                    testTag = "toggle_hidden_files"
                )
                SettingToggleRow(
                    icon = Icons.Rounded.Extension,
                    title = "Show File Extensions",
                    subtitle = "Display suffixes like .mp4, .pdf, .zip",
                    checked = preferences.showFileExtensions,
                    onCheckedChange = onToggleFileExtensions,
                    testTag = "toggle_file_extensions"
                )
                SettingToggleRow(
                    icon = Icons.Rounded.GridView,
                    title = "Default Grid Layout",
                    subtitle = "Use visual card grid instead of compact list",
                    checked = preferences.viewMode == ViewMode.GRID,
                    onCheckedChange = { isGrid ->
                        onSelectViewMode(if (isGrid) ViewMode.GRID else ViewMode.LIST)
                    },
                    testTag = "toggle_grid_default"
                )
                SettingToggleRow(
                    icon = Icons.Rounded.Speed,
                    title = "120Hz Ultra-Smooth Mode",
                    subtitle = "Request max display refresh rate (${maxRefreshRateHz} Hz detected)",
                    checked = preferences.highRefreshRate,
                    onCheckedChange = onToggleHighRefreshRate,
                    testTag = "toggle_high_refresh_rate"
                )
            }
        }

        // Storage & Permissions
        item(key = "storage_perms") {
            Text(
                text = "STORAGE & PERMISSIONS",
                style = MaterialTheme.typography.labelMedium,
                color = onyx.textTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(onyx.cardSurface)
                    .border(0.8.dp, onyx.subtleBorder, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRequestAllFilesAccess)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = if (hasAllFilesAccess) SuccessMint else onyx.accent
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "All Files Access",
                            style = MaterialTheme.typography.titleMedium,
                            color = onyx.textPrimary
                        )
                        Text(
                            text = if (hasAllFilesAccess) "Granted — full storage control active" else "Tap to grant in Android Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasAllFilesAccess) SuccessMint else onyx.textSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onMountSafDrive)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Usb, contentDescription = null, tint = onyx.accent)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mount SD Card / SAF Folder",
                            style = MaterialTheme.typography.titleMedium,
                            color = onyx.textPrimary
                        )
                        Text(
                            text = "Grant persistent Storage Access Framework access to external drives",
                            style = MaterialTheme.typography.bodySmall,
                            color = onyx.textSecondary
                        )
                    }
                }

                preferences.safTreeUris.forEach { uri ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uri,
                            style = MonoMetadataStyle,
                            color = onyx.textSecondary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveSafDrive(uri) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Unmount", tint = onyx.textTertiary)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClearRecentHistory)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = onyx.textSecondary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Clear Recent Files History",
                            style = MaterialTheme.typography.titleMedium,
                            color = onyx.textPrimary
                        )
                        Text(
                            text = "Reset recently opened files list",
                            style = MaterialTheme.typography.bodySmall,
                            color = onyx.textSecondary
                        )
                    }
                }
            }
        }

        // App Information
        item(key = "about_section") {
            Text(
                text = "ABOUT",
                style = MaterialTheme.typography.labelMedium,
                color = onyx.textTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(onyx.cardSurface)
                    .border(0.8.dp, onyx.subtleBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = onyx.accent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Onyx Files  v1.0",
                            style = MaterialTheme.typography.titleMedium,
                            color = onyx.textPrimary
                        )
                        Text(
                            text = "Pure AMOLED File Manager & Media Suite",
                            style = MaterialTheme.typography.bodySmall,
                            color = onyx.textSecondary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Code, contentDescription = null, tint = onyx.textTertiary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Jetpack Compose • Material 3 • Media3 ExoPlayer • Coil • DataStore",
                        style = MonoMetadataStyle,
                        color = onyx.textTertiary
                    )
                }
            }
        }

        item(key = "bottom_pad") {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val onyx = LocalOnyxColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = onyx.accent, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = onyx.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = onyx.textSecondary)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF05070B),
                checkedTrackColor = onyx.accent,
                uncheckedThumbColor = onyx.textSecondary,
                uncheckedTrackColor = onyx.elevatedSurface
            )
        )
    }
}
