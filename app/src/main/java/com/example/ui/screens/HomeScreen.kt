package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ResponseLanguage
import com.example.data.preferences.AppSettings
import com.example.ui.components.ClaudeControlMark
import com.example.ui.components.GoldPillBadge
import com.example.ui.components.HairlineDivider
import com.example.ui.components.HebrewDateFormatter
import com.example.ui.components.MessageFlowDiagram
import com.example.ui.components.OfflineWarningBanner
import com.example.ui.theme.CardSurfaceHigh
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ClaudeTerracotta
import com.example.ui.theme.ClaudeTerracottaSoft
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElevatedObsidian
import com.example.ui.theme.EmeraldGoldSuccess
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GraphiteDivider
import com.example.ui.theme.MutedWarmGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftGoldContainer
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.VelvetCharcoal
import com.example.ui.theme.WarmIvory
import com.example.ui.viewmodel.DashboardStats

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    settings: AppSettings,
    stats: DashboardStats,
    hasApiKey: Boolean,
    isOnline: Boolean,
    isTablet: Boolean,
    onOpenChat: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenAutomation: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleFloatingBubble: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasOverlayPermission = Settings.canDrawOverlays(context)
    val paidReady = settings.isPaidApiReady && hasApiKey

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 1120.dp)
                .fillMaxSize()
        ) {
            OfflineWarningBanner(isOnline = isOnline)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Studio Header Workspace Strip (Asymmetric Executive Banner)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(VelvetCharcoal)
                            .border(1.dp, GraphiteDivider, MaterialTheme.shapes.medium)
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ClaudeControlMark(size = 46.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "שלום, ברוך הבא למרכז הבקרה",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = ChampagneGold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "שליטה מאוחדת על שיחות, תזמונים, משימות והתראות בעברית מלאה.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedWarmGray
                                )
                            }
                            Button(
                                onClick = onOpenChat,
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = ObsidianBlack
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("home_primary_chat_cta")
                            ) {
                                Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("פתח שיחה", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HairlineDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val connectionText = when {
                                !isOnline -> "ללא חיבור רשת"
                                paidReady -> "מחובר · Anthropic API"
                                settings.isPaidApiReady && !hasApiKey -> "API ממתין למפתח"
                                else -> "מצב חשבון Claude חינמי"
                            }
                            GoldPillBadge(
                                text = connectionText,
                                icon = Icons.Default.CheckCircle,
                                containerColor = if (isOnline) SuccessContainer else CardSurfaceHigh,
                                contentColor = if (isOnline) EmeraldGoldSuccess else CrimsonError
                            )

                            val autoText = when {
                                !settings.autoSendEnabled -> "אוטומציה כבויה"
                                settings.autoSendPaused -> "אוטומציה מושהית"
                                else -> "אוטומציה · כל ${settings.autoSendIntervalHours} שע'"
                            }
                            GoldPillBadge(
                                text = autoText,
                                icon = Icons.Default.Schedule,
                                containerColor = if (settings.autoSendEnabled && !settings.autoSendPaused) SoftGoldContainer else CardSurfaceHigh,
                                contentColor = if (settings.autoSendEnabled && !settings.autoSendPaused) ChampagneGold else MutedWarmGray
                            )

                            GoldPillBadge(
                                text = "שפת תגובה: ${ResponseLanguage.resolveLabel(settings.responseLanguage)}",
                                containerColor = CardSurfaceHigh,
                                contentColor = SoftWhite
                            )
                        }
                    }
                }

                // Inline Architectural Metrics Strip (replaces 5 heavy rounded cards)
                item {
                    val metrics = listOf(
                        Triple("הוכנו", stats.preparedCount.toString(), SoftWhite),
                        Triple("נשלחו", stats.sentCount.toString(), GoldPrimary),
                        Triple("תשובות", stats.repliesCount.toString(), ClaudeTerracotta),
                        Triple("משימות פתוחות", stats.activeTasksCount.toString(), ChampagneGold),
                        Triple("התראות חדשות", stats.unreadNotificationsCount.toString(), if (stats.unreadNotificationsCount > 0) CrimsonError else MutedWarmGray)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(VelvetCharcoal)
                            .border(1.dp, GraphiteDivider, MaterialTheme.shapes.medium)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "מדדי מערכת בזמן אמת",
                            style = MaterialTheme.typography.titleSmall,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isTablet) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                metrics.forEachIndexed { index, (label, value, accent) ->
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = accent,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedWarmGray
                                        )
                                    }
                                    if (index < metrics.lastIndex) {
                                        VerticalDivider(
                                            modifier = Modifier.fillMaxHeight(),
                                            thickness = 1.dp,
                                            color = GraphiteDivider
                                        )
                                    }
                                }
                            }
                        } else {
                            FlowRow(
                                maxItemsInEachRow = 3,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                metrics.forEach { (label, value, accent) ->
                                    Column(modifier = Modifier.widthIn(min = 88.dp)) {
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = accent,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedWarmGray
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HairlineDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("פעולה אחרונה", style = MaterialTheme.typography.labelSmall, color = MutedWarmGray)
                                Text(
                                    text = if (stats.lastActionTimestamp > 0L) {
                                        HebrewDateFormatter.formatDateTime(stats.lastActionTimestamp)
                                    } else {
                                        "טרם בוצעה פעולה"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WarmIvory
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("הרצה מתוזמנת הבאה", style = MaterialTheme.typography.labelSmall, color = MutedWarmGray)
                                Text(
                                    text = if (stats.nextScheduledTimestamp > 0L) {
                                        HebrewDateFormatter.formatDateTime(stats.nextScheduledTimestamp)
                                    } else {
                                        "אין תזמון פעיל"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ChampagneGold
                                )
                            }
                        }
                    }
                }

                // Asymmetric Tablet Split Workspace (Primary Routing + Control List)
                item {
                    if (isTablet) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1.25f),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                MessageFlowDiagram(isPaidApiMode = paidReady, isOnline = isOnline)
                                FloatingBubbleControlStrip(
                                    enabled = settings.floatingBubbleEnabled,
                                    hasOverlayPermission = hasOverlayPermission,
                                    onToggleFloatingBubble = onToggleFloatingBubble,
                                    onRequestPermission = {
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    }
                                )
                            }
                            NavigationDirectoryList(
                                stats = stats,
                                intervalHours = settings.autoSendIntervalHours,
                                onOpenChat = onOpenChat,
                                onOpenTasks = onOpenTasks,
                                onOpenAutomation = onOpenAutomation,
                                onOpenNotifications = onOpenNotifications,
                                onOpenStats = onOpenStats,
                                onOpenHistory = onOpenHistory,
                                onOpenSettings = onOpenSettings,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            MessageFlowDiagram(isPaidApiMode = paidReady, isOnline = isOnline)
                            FloatingBubbleControlStrip(
                                enabled = settings.floatingBubbleEnabled,
                                hasOverlayPermission = hasOverlayPermission,
                                onToggleFloatingBubble = onToggleFloatingBubble,
                                onRequestPermission = {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                }
                            )
                            NavigationDirectoryList(
                                stats = stats,
                                intervalHours = settings.autoSendIntervalHours,
                                onOpenChat = onOpenChat,
                                onOpenTasks = onOpenTasks,
                                onOpenAutomation = onOpenAutomation,
                                onOpenNotifications = onOpenNotifications,
                                onOpenStats = onOpenStats,
                                onOpenHistory = onOpenHistory,
                                onOpenSettings = onOpenSettings
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingBubbleControlStrip(
    enabled: Boolean,
    hasOverlayPermission: Boolean,
    onToggleFloatingBubble: (Boolean) -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(VelvetCharcoal)
            .border(
                1.dp,
                if (enabled && hasOverlayPermission) GoldPrimary.copy(alpha = 0.55f) else GraphiteDivider,
                MaterialTheme.shapes.medium
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(ClaudeTerracottaSoft)
                        .border(1.dp, ClaudeTerracotta.copy(alpha = 0.45f), MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BubbleChart,
                        contentDescription = null,
                        tint = ClaudeTerracotta,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "כפתור צף חכם מעל אפליקציות",
                        style = MaterialTheme.typography.titleSmall,
                        color = WarmIvory
                    )
                    Text(
                        text = if (hasOverlayPermission) {
                            "גרירה חופשית, שליחה מהירה ופתיחת האפליקציה מכל מסך"
                        } else {
                            "נדרשת הרשאת הצגה מעל אפליקציות אחרות"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedWarmGray
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = enabled && hasOverlayPermission,
                onCheckedChange = { wantEnable ->
                    if (wantEnable && !hasOverlayPermission) {
                        onRequestPermission()
                    } else {
                        onToggleFloatingBubble(wantEnable)
                    }
                },
                modifier = Modifier.testTag("home_floating_bubble_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianBlack,
                    checkedTrackColor = GoldPrimary
                )
            )
        }
        if (!hasOverlayPermission) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onRequestPermission,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardSurfaceHigh,
                    contentColor = ChampagneGold
                ),
                border = BorderStroke(1.dp, GraphiteDivider),
                modifier = Modifier.testTag("home_grant_overlay_button")
            ) {
                Text("הענק הרשאת כפתור צף בהגדרות אנדרואיד")
            }
        }
    }
}

@Composable
private fun NavigationDirectoryList(
    stats: DashboardStats,
    intervalHours: Int,
    onOpenChat: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenAutomation: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shortcuts = listOf(
        ShortcutDef("שיחה והכנה ל-Claude", Icons.Default.Forum, onOpenChat, "shortcut_chat"),
        ShortcutDef("ניהול משימות ותזכורות", Icons.Default.TaskAlt, onOpenTasks, "shortcut_tasks"),
        ShortcutDef("אוטומציה כל $intervalHours שעות", Icons.Default.Schedule, onOpenAutomation, "shortcut_automation"),
        ShortcutDef("מרכז התראות (${stats.unreadNotificationsCount})", Icons.Default.Notifications, onOpenNotifications, "shortcut_notifications"),
        ShortcutDef("סטטיסטיקות ופעילות", Icons.Default.BarChart, onOpenStats, "shortcut_stats"),
        ShortcutDef("היסטוריית שיחות", Icons.Default.History, onOpenHistory, "shortcut_history"),
        ShortcutDef("הגדרות, שפה ותקינות", Icons.Default.Settings, onOpenSettings, "shortcut_settings")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(VelvetCharcoal)
            .border(1.dp, GraphiteDivider, MaterialTheme.shapes.medium)
    ) {
        Text(
            text = "ניווט מהיר",
            style = MaterialTheme.typography.titleSmall,
            color = ChampagneGold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        HairlineDivider()
        shortcuts.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = item.onClick)
                    .padding(horizontal = 16.dp, vertical = 13.dp)
                    .testTag(item.tag),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmIvory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MutedWarmGray,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (index < shortcuts.lastIndex) {
                HairlineDivider()
            }
        }
    }
}

private data class ShortcutDef(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val tag: String
)
