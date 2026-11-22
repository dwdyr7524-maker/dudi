package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.AppNotificationEntity
import com.example.data.local.NotificationType
import com.example.ui.components.GoldDivider
import com.example.ui.components.GoldPillBadge
import com.example.ui.components.HebrewDateFormatter
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElevatedObsidian
import com.example.ui.theme.EmeraldGoldSuccess
import com.example.ui.theme.GoldBorderStrong
import com.example.ui.theme.GoldHairline
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InfoBlueAccent
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.MutedWarmGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.VelvetCharcoal
import com.example.ui.theme.WarmIvory

@Composable
fun NotificationsScreen(
    notifications: List<AppNotificationEntity>,
    onMarkRead: (Long) -> Unit,
    onMarkAllRead: () -> Unit,
    onDeleteNotification: (Long) -> Unit,
    onClearAll: () -> Unit,
    onOpenConversation: (Long) -> Unit,
    onOpenTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unreadCount = notifications.count { !it.isRead }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxSize()
        ) {
            Surface(color = VelvetCharcoal) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "מרכז התראות",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ChampagneGold
                            )
                            Text(
                                text = if (unreadCount > 0) "$unreadCount התראות חדשות שלא נקראו" else "כל ההתראות נקראו",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedWarmGray
                            )
                        }
                        Row {
                            if (unreadCount > 0) {
                                TextButton(
                                    onClick = onMarkAllRead,
                                    modifier = Modifier.testTag("mark_all_notifications_read")
                                ) {
                                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("סמן הכל כנקרא", color = GoldPrimary)
                                }
                            }
                            if (notifications.isNotEmpty()) {
                                TextButton(
                                    onClick = onClearAll,
                                    modifier = Modifier.testTag("clear_all_notifications")
                                ) {
                                    Text("מחק הכל", color = CrimsonError)
                                }
                            }
                        }
                    }
                    GoldDivider()
                }
            }

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = GoldPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "אין התראות כרגע",
                            style = MaterialTheme.typography.titleLarge,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "הודעות חדשות, תזכורות שליחה, משימות ותקלות חיבור יופיעו כאן.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedWarmGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        val style = resolveNotificationStyle(item.type)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .clickable {
                                    onMarkRead(item.id)
                                    if (item.relatedConversationId > 0L) {
                                        onOpenConversation(item.relatedConversationId)
                                    } else if (item.relatedTaskId > 0L) {
                                        onOpenTasks()
                                    }
                                }
                                .testTag("notification_card_${item.id}"),
                            shape = MaterialTheme.shapes.large,
                            color = if (item.isRead) ElevatedObsidian else MidnightNavy,
                            border = BorderStroke(
                                width = if (item.isRead) 0.75.dp else 1.25.dp,
                                color = if (item.isRead) GoldHairline else GoldBorderStrong
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(style.accent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = style.icon,
                                        contentDescription = null,
                                        tint = style.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        GoldPillBadge(
                                            text = style.label,
                                            contentColor = style.accent,
                                            containerColor = style.accent.copy(alpha = 0.12f)
                                        )
                                        if (!item.isRead) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(GoldPrimary)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = WarmIvory
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedWarmGray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = HebrewDateFormatter.formatDateTime(item.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedWarmGray.copy(alpha = 0.8f)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteNotification(item.id) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("delete_notification_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "מחק התראה",
                                        tint = CrimsonError
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class NotificationVisualStyle(
    val label: String,
    val icon: ImageVector,
    val accent: Color
)

private fun resolveNotificationStyle(type: String): NotificationVisualStyle {
    return when (type) {
        NotificationType.NEW_MESSAGE -> NotificationVisualStyle("הודעה חדשה", Icons.Default.MarkChatUnread, ChampagneGold)
        NotificationType.SEND_REMINDER -> NotificationVisualStyle("תזכורת לשליחה", Icons.Default.Schedule, InfoBlueAccent)
        NotificationType.CONNECTION_ERROR -> NotificationVisualStyle("תקלה בחיבור", Icons.Default.WifiOff, CrimsonError)
        NotificationType.TASK_DUE -> NotificationVisualStyle("משימה שהגיעה זמנה", Icons.Default.TaskAlt, GoldPrimary)
        NotificationType.ACTION_SUCCESS -> NotificationVisualStyle("הצלחת פעולה", Icons.Default.CheckCircle, EmeraldGoldSuccess)
        NotificationType.ACTION_FAILURE -> NotificationVisualStyle("כישלון פעולה", Icons.Default.ErrorOutline, CrimsonError)
        else -> NotificationVisualStyle("התראה", Icons.Default.Notifications, ChampagneGold)
    }
}
