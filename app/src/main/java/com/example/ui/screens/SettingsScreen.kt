package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccentPreset
import com.example.data.model.AppPreferences
import com.example.data.model.ThemeMode
import com.example.data.model.ViewMode
import com.example.ui.theme.LocalOnyxColors
import com.example.ui.theme.MonoMetadataStyle
import com.example.ui.theme.SuccessMint

private val SettingsCardShape = RoundedCornerShape(18.dp)
private val ThemeCardShape = RoundedCornerShape(16.dp)
private val SocialCardShape = RoundedCornerShape(14.dp)
private const val FOUNDER_NAME = "MAHMUD UDAY"
private const val FOUNDER_EMAIL = "mahmududay.info@gmail.com"
private const val FOUNDER_FACEBOOK_URL = "https://www.facebook.com/mahmududay.info"
private const val FOUNDER_WHATSAPP_NUMBER = "+8801680856340"
private const val FOUNDER_WHATSAPP_URL = "https://wa.me/8801680856340"

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
    val context = LocalContext.current

    val revealProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        revealProgress.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(onyx.background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(onyx.accentGlow.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.85f, 40f),
                        radius = size.width * 0.68f
                    ),
                    center = Offset(size.width * 0.85f, 40f),
                    radius = size.width * 0.68f
                )
            }
            .graphicsLayer {
                alpha = revealProgress.value
                translationY = (1f - revealProgress.value) * 14f
            }
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

        // Quick Dark / Light Mode Toggle + Theme Style Presets
        item(key = "theme_section") {
            Text(
                text = "DISPLAY THEME (DARK / LIGHT)",
                style = MaterialTheme.typography.labelMedium,
                color = onyx.textTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val isLight = preferences.themeMode == ThemeMode.LIGHT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ThemeCardShape)
                    .background(onyx.cardSurface)
                    .border(0.9.dp, onyx.subtleBorder, ThemeCardShape)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThemeQuickButton(
                    icon = Icons.Rounded.DarkMode,
                    label = "Dark Mode",
                    selected = !isLight,
                    onClick = {
                        if (isLight) onSelectThemeMode(ThemeMode.PURE_AMOLED)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_dark_mode_button")
                )
                ThemeQuickButton(
                    icon = Icons.Rounded.LightMode,
                    label = "Light Mode",
                    selected = isLight,
                    onClick = { onSelectThemeMode(ThemeMode.LIGHT) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_light_mode_button")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                            .clip(ThemeCardShape)
                            .background(if (selected) onyx.accentSoft else onyx.cardSurface)
                            .border(1.dp, border, ThemeCardShape)
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

        // Accent Swatches with luminous selected glow
        item(key = "accent_section") {
            Text(
                text = "GLOWING ACCENT COLOR",
                style = MaterialTheme.typography.labelMedium,
                color = onyx.textTertiary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SettingsCardShape)
                    .background(onyx.cardSurface)
                    .border(0.9.dp, onyx.subtleBorder, SettingsCardShape)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccentPreset.entries.forEach { preset ->
                    val selected = preset == preferences.accentPreset
                    val swatchScale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "swatchScale"
                    )
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .graphicsLayer {
                                scaleX = swatchScale
                                scaleY = swatchScale
                            }
                            .drawBehind {
                                if (selected) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(preset.color.copy(alpha = 0.55f), Color.Transparent),
                                            radius = size.width * 0.72f
                                        ),
                                        radius = size.width * 0.72f
                                    )
                                }
                            }
                            .clip(CircleShape)
                            .border(
                                width = if (selected) 2.2.dp else 0.dp,
                                color = if (selected) onyx.textPrimary else Color.Transparent,
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
                    .clip(SettingsCardShape)
                    .background(onyx.cardSurface)
                    .border(0.85.dp, onyx.subtleBorder, SettingsCardShape)
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
                    .clip(SettingsCardShape)
                    .background(onyx.cardSurface)
                    .border(0.85.dp, onyx.subtleBorder, SettingsCardShape)
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

        // Founder & Credits Section with Social Buttons
        item(key = "founder_credits_section") {
            Text(
                text = "FOUNDER & CREDITS",
                style = MaterialTheme.typography.labelMedium,
                color = onyx.accent
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SettingsCardShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                onyx.cardSurface,
                                onyx.elevatedSurface
                            )
                        )
                    )
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(onyx.accentGlow.copy(alpha = 0.42f), Color.Transparent),
                                center = Offset(size.width * 0.14f, size.height * 0.25f),
                                radius = size.width * 0.62f
                            ),
                            center = Offset(size.width * 0.14f, size.height * 0.25f),
                            radius = size.width * 0.62f
                        )
                    }
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(onyx.glowBorder, onyx.hairlineBorder, onyx.accent.copy(alpha = 0.35f))
                        ),
                        shape = SettingsCardShape
                    )
                    .padding(16.dp)
                    .testTag("founder_credits_section"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(onyx.accentGlow, onyx.accentSoft)
                                )
                            )
                            .border(1.2.dp, onyx.glowBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MU",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = onyx.accent
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FOUNDER & CREATOR",
                                style = MaterialTheme.typography.labelSmall,
                                color = onyx.accent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = onyx.accent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FOUNDER_NAME,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = onyx.textPrimary
                        )
                    }
                }

                HorizontalDivider(color = onyx.subtleBorder)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(onyx.accentSoft.copy(alpha = 0.65f))
                        .border(0.75.dp, onyx.glowBorder.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .clickable {
                            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$FOUNDER_EMAIL")
                            }
                            runCatching { context.startActivity(mailIntent) }.onFailure {
                                Toast.makeText(context, FOUNDER_EMAIL, Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("founder_email_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = "Email founder",
                        tint = onyx.accent,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contact Email",
                            style = MaterialTheme.typography.labelSmall,
                            color = onyx.textTertiary
                        )
                        Text(
                            text = FOUNDER_EMAIL,
                            style = MonoMetadataStyle,
                            color = onyx.textPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Founder Email", FOUNDER_EMAIL))
                            Toast.makeText(context, "Email copied: $FOUNDER_EMAIL", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy email",
                            tint = onyx.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Social Buttons: Facebook & WhatsApp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val fbColor = Color(0xFF1877F2)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(SocialCardShape)
                            .background(fbColor.copy(alpha = 0.14f))
                            .border(0.9.dp, fbColor.copy(alpha = 0.55f), SocialCardShape)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(FOUNDER_FACEBOOK_URL))
                                runCatching { context.startActivity(intent) }.onFailure {
                                    Toast.makeText(context, FOUNDER_FACEBOOK_URL, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                            .testTag("founder_facebook_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(fbColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "f",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Facebook",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = onyx.textPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "mahmududay.info",
                                style = MaterialTheme.typography.labelSmall,
                                color = onyx.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val waColor = Color(0xFF25D366)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(SocialCardShape)
                            .background(waColor.copy(alpha = 0.14f))
                            .border(0.9.dp, waColor.copy(alpha = 0.55f), SocialCardShape)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(FOUNDER_WHATSAPP_URL))
                                runCatching { context.startActivity(intent) }.onFailure {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("WhatsApp", FOUNDER_WHATSAPP_NUMBER))
                                    Toast.makeText(context, "WhatsApp copied: $FOUNDER_WHATSAPP_NUMBER", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                            .testTag("founder_whatsapp_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(waColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Chat,
                                contentDescription = "WhatsApp",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WhatsApp",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = onyx.textPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = FOUNDER_WHATSAPP_NUMBER,
                                style = MaterialTheme.typography.labelSmall,
                                color = onyx.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
                    .clip(SettingsCardShape)
                    .background(onyx.cardSurface)
                    .border(0.85.dp, onyx.subtleBorder, SettingsCardShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = onyx.accent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Orion Explorer  v1.0",
                            style = MaterialTheme.typography.titleMedium,
                            color = onyx.textPrimary
                        )
                        Text(
                            text = "Pure AMOLED & Light File Explorer & Media Suite",
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
private fun ThemeQuickButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onyx = LocalOnyxColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) onyx.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) onyx.onAccent else onyx.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) onyx.onAccent else onyx.textSecondary
        )
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
                checkedThumbColor = onyx.onAccent,
                checkedTrackColor = onyx.accent,
                uncheckedThumbColor = onyx.textSecondary,
                uncheckedTrackColor = onyx.elevatedSurface
            )
        )
    }
}
