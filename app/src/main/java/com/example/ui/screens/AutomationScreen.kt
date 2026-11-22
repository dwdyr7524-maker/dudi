package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import com.example.R
import com.example.data.local.ConversationWithSummary
import com.example.data.local.ScheduledPromptEntity
import com.example.data.preferences.AppSettings
import com.example.notifications.ClaudeNotificationManager
import com.example.ui.components.GoldDivider
import com.example.ui.components.GoldPillBadge
import com.example.ui.components.HebrewDateFormatter
import com.example.ui.components.LuxuryObsidianCard
import com.example.ui.components.OfflineWarningBanner
import com.example.ui.components.luxuryPressScale
import com.example.ui.theme.CardSurfaceHigh
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElevatedObsidian
import com.example.ui.theme.EmeraldGoldSuccess
import com.example.ui.theme.GoldHairline
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InfoBlueAccent
import com.example.ui.theme.InfoBlueContainer
import com.example.ui.theme.MutedWarmGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftGoldContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.VelvetCharcoal
import com.example.ui.theme.WarmIvory

private val INTERVAL_OPTIONS = listOf(1, 2, 3, 5, 8, 12, 24)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutomationScreen(
    settings: AppSettings,
    conversations: List<ConversationWithSummary>,
    scheduledPrompts: List<ScheduledPromptEntity>,
    scheduleWorkState: WorkInfo.State?,
    hasApiKey: Boolean,
    isOnline: Boolean,
    onEnableAutoSend: (prompt: String, intervalHours: Int, activeStartHour: Int, activeEndHour: Int, targetConversationId: Long) -> Unit,
    onPauseAutoSend: (prompt: String, intervalHours: Int, activeStartHour: Int, activeEndHour: Int, targetConversationId: Long) -> Unit,
    onResumeAutoSend: (prompt: String, intervalHours: Int, activeStartHour: Int, activeEndHour: Int, targetConversationId: Long) -> Unit,
    onDisableAutoSend: (prompt: String, intervalHours: Int, activeStartHour: Int, activeEndHour: Int, targetConversationId: Long) -> Unit,
    onSavePromptOnly: (prompt: String, intervalHours: Int, activeStartHour: Int, activeEndHour: Int, targetConversationId: Long) -> Unit,
    onRunNow: (prompt: String, intervalHours: Int, activeStartHour: Int, activeEndHour: Int, targetConversationId: Long) -> Unit,
    onSaveCustomSchedule: (existingId: Long, title: String, promptText: String, intervalHours: Int, activeStartHour: Int, activeEndHour: Int, isEnabled: Boolean, targetConversationId: Long) -> Unit,
    onToggleCustomSchedule: (ScheduledPromptEntity) -> Unit,
    onDeleteCustomSchedule: (Long) -> Unit,
    onRunCustomScheduleNow: (Long) -> Unit,
    onUpdateNotificationPreferences: (notifyReplies: Boolean, notifyErrors: Boolean, notifyTasks: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationManager = remember(context) { ClaudeNotificationManager(context) }

    var hasNotifPermission by remember { mutableStateOf(notificationManager.hasNotificationPermission()) }
    var promptInput by rememberSaveable(settings.autoSendPrompt) { mutableStateOf(settings.autoSendPrompt) }
    var intervalHours by rememberSaveable(settings.autoSendIntervalHours) { mutableIntStateOf(settings.autoSendIntervalHours) }
    var activeStartHour by rememberSaveable(settings.activeStartHour) { mutableIntStateOf(settings.activeStartHour) }
    var activeEndHour by rememberSaveable(settings.activeEndHour) { mutableIntStateOf(settings.activeEndHour) }
    var targetConversationId by rememberSaveable(settings.autoSendConversationId) {
        mutableLongStateOf(settings.autoSendConversationId)
    }

    var showConfirmActivationDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduledPromptEntity?>(null) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    val paidReady = settings.isPaidApiReady && hasApiKey

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted || notificationManager.hasNotificationPermission()
    }

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
                .imePadding()
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
                                text = "מערכת אוטומציה מתקדמת",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ChampagneGold
                            )
                            Text(
                                text = "תזמון ברירת מחדל כל 5 שעות · מרווחים ושעות פעילות מותאמות",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedWarmGray
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val statusBadge = when {
                            !settings.autoSendEnabled -> stringResource(R.string.automation_status_inactive)
                            settings.autoSendPaused -> stringResource(R.string.automation_status_paused)
                            else -> "פעיל · כל $intervalHours שע'"
                        }
                        GoldPillBadge(
                            text = statusBadge,
                            icon = Icons.Default.Schedule,
                            containerColor = when {
                                settings.autoSendEnabled && !settings.autoSendPaused -> SuccessContainer
                                settings.autoSendPaused -> SoftGoldContainer
                                else -> CardSurfaceHigh
                            },
                            contentColor = when {
                                settings.autoSendEnabled && !settings.autoSendPaused -> EmeraldGoldSuccess
                                settings.autoSendPaused -> ChampagneGold
                                else -> MutedWarmGray
                            }
                        )
                    }
                    GoldDivider()
                }
            }

            OfflineWarningBanner(isOnline = isOnline)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Honest Operating Mode & WorkManager Timing Disclaimer
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = InfoBlueContainer.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, InfoBlueAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = InfoBlueAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (paidReady) {
                                        "מצב API מאושר: התזמון ישלח את ההודעה אוטומטית ל-Anthropic וישמור תשובה."
                                    } else {
                                        "מצב חשבון Claude חינמי: התזמון מכין את ההודעה ושולח לך תזכורת נוחה לשליחה בחינם ללא עלויות API."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarmIvory,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "הערת דיוק: WorkManager של אנדרואיד מכבד את אופטימיזציית הסוללה של המכשיר, ולכן תזמון הרקע עשוי לפעול בטווח משוער ולא על הדקה המדויקת.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedWarmGray
                                )
                            }
                        }
                    }
                }

                // Primary Automation Toggle + Pause / Resume Controls
                item {
                    LuxuryObsidianCard(highlighted = settings.autoSendEnabled && !settings.autoSendPaused) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "הפעלת תזמון ראשי (ברירת מחדל: 5 שעות)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ChampagneGold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "שומר על מניעת שליחות כפולות, נותר פעיל גם לאחר הפעלה מחדש של המכשיר.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedWarmGray
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = settings.autoSendEnabled,
                                onCheckedChange = { shouldEnable ->
                                    if (shouldEnable) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        showConfirmActivationDialog = true
                                    } else {
                                        onDisableAutoSend(
                                            promptInput,
                                            intervalHours,
                                            activeStartHour,
                                            activeEndHour,
                                            targetConversationId
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("automation_master_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ObsidianBlack,
                                    checkedTrackColor = GoldPrimary,
                                    uncheckedThumbColor = MutedWarmGray,
                                    uncheckedTrackColor = CardSurfaceHigh
                                )
                            )
                        }

                        if (settings.autoSendEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (settings.autoSendPaused) {
                                    val resumeInteraction = remember { MutableInteractionSource() }
                                    Button(
                                        onClick = {
                                            onResumeAutoSend(
                                                promptInput,
                                                intervalHours,
                                                activeStartHour,
                                                activeEndHour,
                                                targetConversationId
                                            )
                                        },
                                        interactionSource = resumeInteraction,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .luxuryPressScale(resumeInteraction)
                                            .testTag("resume_automation_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GoldPrimary,
                                            contentColor = ObsidianBlack
                                        )
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.automation_resume),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    val pauseInteraction = remember { MutableInteractionSource() }
                                    OutlinedButton(
                                        onClick = {
                                            onPauseAutoSend(
                                                promptInput,
                                                intervalHours,
                                                activeStartHour,
                                                activeEndHour,
                                                targetConversationId
                                            )
                                        },
                                        interactionSource = pauseInteraction,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .luxuryPressScale(pauseInteraction)
                                            .testTag("pause_automation_button"),
                                        border = BorderStroke(1.dp, GoldPrimary),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold)
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.automation_pause))
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        onDisableAutoSend(
                                            promptInput,
                                            intervalHours,
                                            activeStartHour,
                                            activeEndHour,
                                            targetConversationId
                                        )
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("stop_automation_button"),
                                    border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.7f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.automation_stop))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        GoldDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("הרצה הבאה (משוערת)", style = MaterialTheme.typography.labelSmall, color = MutedWarmGray)
                                Text(
                                    text = if (settings.autoSendEnabled && !settings.autoSendPaused && settings.nextAutoSendTimestamp > 0L) {
                                        HebrewDateFormatter.formatDateTime(settings.nextAutoSendTimestamp)
                                    } else {
                                        "לא מתוזמן"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ChampagneGold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("מצב WorkManager", style = MaterialTheme.typography.labelSmall, color = MutedWarmGray)
                                Text(
                                    text = if (scheduleWorkState != null) {
                                        mapWorkStateToHebrew(scheduleWorkState, settings.autoSendPaused, intervalHours)
                                    } else {
                                        "ממתין להפעלה"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarmIvory
                                )
                            }
                        }
                    }
                }

                // Interval, Active Hours, Prompt & Target Editor
                item {
                    LuxuryObsidianCard {
                        Text(
                            text = "מרווח זמן ושעות פעילות",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "כל כמה שעות לפעול (ברירת מחדל: 5 שעות)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedWarmGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            INTERVAL_OPTIONS.forEach { hours ->
                                val selected = intervalHours == hours
                                FilterChip(
                                    selected = selected,
                                    onClick = { intervalHours = hours },
                                    label = { Text("כל $hours שע'") },
                                    modifier = Modifier.testTag("interval_chip_$hours"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SoftGoldContainer,
                                        selectedLabelColor = ChampagneGold,
                                        containerColor = ObsidianBlack,
                                        labelColor = MutedWarmGray
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "חלון שעות פעילות מותר: ${formatHour(activeStartHour)} – ${formatHour(activeEndHour)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HourStepper(
                                label = "התחלה",
                                hour = activeStartHour,
                                onDecrease = { activeStartHour = (activeStartHour + 23) % 24 },
                                onIncrease = { activeStartHour = (activeStartHour + 1) % 24 },
                                modifier = Modifier.weight(1f)
                            )
                            HourStepper(
                                label = "סיום",
                                hour = activeEndHour,
                                onDecrease = { activeEndHour = (activeEndHour + 23) % 24 },
                                onIncrease = { activeEndHour = (activeEndHour + 1) % 24 },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.automation_prompt_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = WarmIvory,
                                textDirection = TextDirection.Content
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrectEnabled = true,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Default
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("automation_prompt_input"),
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = GoldHairline,
                                focusedContainerColor = ObsidianBlack,
                                unfocusedContainerColor = ObsidianBlack,
                                focusedTextColor = WarmIvory,
                                unfocusedTextColor = WarmIvory
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "יעד שמירת ההודעות",
                            style = MaterialTheme.typography.labelLarge,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        TargetConversationOption(
                            label = "שיחה ייעודית קבועה לאוטומציה",
                            selected = targetConversationId <= 0L,
                            onClick = { targetConversationId = -1L },
                            testTag = "target_convo_dedicated"
                        )

                        conversations.take(4).forEach { convo ->
                            TargetConversationOption(
                                label = convo.title,
                                selected = targetConversationId == convo.id,
                                onClick = { targetConversationId = convo.id },
                                testTag = "target_convo_${convo.id}"
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val saveInteraction = remember { MutableInteractionSource() }
                            Button(
                                onClick = {
                                    onSavePromptOnly(
                                        promptInput,
                                        intervalHours,
                                        activeStartHour,
                                        activeEndHour,
                                        targetConversationId
                                    )
                                },
                                interactionSource = saveInteraction,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .luxuryPressScale(saveInteraction)
                                    .testTag("save_automation_prompt_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = ObsidianBlack
                                )
                            ) {
                                Text(
                                    text = "שמור הגדרות תזמון",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val runNowInteraction = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = {
                                    onRunNow(
                                        promptInput,
                                        intervalHours,
                                        activeStartHour,
                                        activeEndHour,
                                        targetConversationId
                                    )
                                },
                                enabled = !paidReady || isOnline,
                                interactionSource = runNowInteraction,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .luxuryPressScale(runNowInteraction)
                                    .testTag("run_automation_now_button"),
                                border = BorderStroke(1.dp, GoldPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ChampagneGold
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "הפעל בדיקה כעת")
                            }
                        }
                    }
                }

                // Multiple Scheduled Prompts Section
                item {
                    LuxuryObsidianCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "הודעות מתוזמנות נוספות (${scheduledPrompts.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ChampagneGold
                                )
                                Text(
                                    text = "הוסף מספר תזמונים נפרדים עם מרווחים ושעות שונות.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedWarmGray
                                )
                            }
                            Button(
                                onClick = {
                                    editingSchedule = null
                                    showScheduleDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = ObsidianBlack
                                ),
                                modifier = Modifier.testTag("add_custom_schedule_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("הוסף תזמון", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (scheduledPrompts.isEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "טרם נוספו תזמונים נוספים. התזמון הראשי למעלה זמין תמיד.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedWarmGray
                            )
                        }
                    }
                }

                items(scheduledPrompts, key = { it.id }) { schedule ->
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = ElevatedObsidian,
                        border = BorderStroke(
                            1.dp,
                            if (schedule.isEnabled && !schedule.isPaused) GoldPrimary else GoldHairline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_schedule_card_${schedule.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = schedule.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ChampagneGold
                                    )
                                    Text(
                                        text = "כל ${schedule.intervalHours} שעות · ${formatHour(schedule.activeStartHour)}–${formatHour(schedule.activeEndHour)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedWarmGray
                                    )
                                }
                                Switch(
                                    checked = schedule.isEnabled && !schedule.isPaused,
                                    onCheckedChange = { onToggleCustomSchedule(schedule) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ObsidianBlack,
                                        checkedTrackColor = GoldPrimary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = schedule.promptText,
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmIvory
                            )
                            val targetLabel = if (schedule.targetConversationId > 0L) {
                                conversations.firstOrNull { it.id == schedule.targetConversationId }?.title ?: "שיחה שמורה"
                            } else {
                                "שיחה ייעודית קבועה לאוטומציה"
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "יעד: $targetLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampagneGold
                            )
                            if (schedule.lastStatus.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "אחרון: ${schedule.lastStatus}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InfoBlueAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onRunCustomScheduleNow(schedule.id) },
                                    border = BorderStroke(1.dp, GoldPrimary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("הפעל כעת")
                                }
                                OutlinedButton(
                                    onClick = {
                                        editingSchedule = schedule
                                        showScheduleDialog = true
                                    },
                                    border = BorderStroke(1.dp, GoldHairline),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmIvory)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ערוך")
                                }
                                IconButton(
                                    onClick = { onDeleteCustomSchedule(schedule.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "מחק תזמון",
                                        tint = CrimsonError
                                    )
                                }
                            }
                        }
                    }
                }

                // Notification Preferences & Android 13+ Permission Card
                item {
                    LuxuryObsidianCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = ChampagneGold
                            )
                            Text(
                                text = stringResource(R.string.notifications_section_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = ChampagneGold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (hasNotifPermission) SuccessContainer else SoftGoldContainer,
                                border = BorderStroke(
                                    1.dp,
                                    if (hasNotifPermission) EmeraldGoldSuccess.copy(alpha = 0.5f) else GoldPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (hasNotifPermission) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = if (hasNotifPermission) EmeraldGoldSuccess else ChampagneGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (hasNotifPermission) {
                                                stringResource(R.string.notification_permission_granted)
                                            } else {
                                                stringResource(R.string.notification_permission_missing)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = WarmIvory
                                        )
                                    }

                                    if (!hasNotifPermission) {
                                        TextButton(
                                            onClick = {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("grant_notifications_button")
                                        ) {
                                            Text(
                                                text = stringResource(R.string.grant_notification_permission),
                                                color = ChampagneGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        NotificationToggleRow(
                            label = stringResource(R.string.notify_replies_label),
                            checked = settings.notifyOnReplies,
                            onCheckedChange = { checked ->
                                onUpdateNotificationPreferences(
                                    checked,
                                    settings.notifyOnErrors,
                                    settings.notifyTaskReminders
                                )
                            },
                            testTag = "notify_replies_switch"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NotificationToggleRow(
                            label = stringResource(R.string.notify_errors_label),
                            checked = settings.notifyOnErrors,
                            onCheckedChange = { checked ->
                                onUpdateNotificationPreferences(
                                    settings.notifyOnReplies,
                                    checked,
                                    settings.notifyTaskReminders
                                )
                            },
                            testTag = "notify_errors_switch"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NotificationToggleRow(
                            label = "תזכורות למשימות ותזמוני שליחה",
                            checked = settings.notifyTaskReminders,
                            onCheckedChange = { checked ->
                                onUpdateNotificationPreferences(
                                    settings.notifyOnReplies,
                                    settings.notifyOnErrors,
                                    checked
                                )
                            },
                            testTag = "notify_tasks_switch"
                        )
                    }
                }

                // Last Run Status Card
                item {
                    LuxuryObsidianCard {
                        Text(
                            text = stringResource(R.string.automation_last_run_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (settings.lastAutoSendTimestamp <= 0L) {
                            Text(
                                text = stringResource(R.string.automation_never_run),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedWarmGray
                            )
                        } else {
                            Text(
                                text = HebrewDateFormatter.formatDateTime(settings.lastAutoSendTimestamp),
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = settings.lastAutoSendStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmIvory
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConfirmActivationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmActivationDialog = false },
            containerColor = VelvetCharcoal,
            titleContentColor = ChampagneGold,
            textContentColor = WarmIvory,
            title = { Text(stringResource(R.string.automation_confirm_title)) },
            text = {
                Text(
                    if (paidReady) {
                        "האפליקציה תשלח את ההודעה שהגדרת אל Anthropic API ברקע אחת ל-$intervalHours שעות בין ${formatHour(activeStartHour)} ל-${formatHour(activeEndHour)}, ותציג התראה עם התשובה. ניתן לעצור בכל רגע."
                    } else {
                        "האפליקציה תכין עבורך את ההודעה אחת ל-$intervalHours שעות בין ${formatHour(activeStartHour)} ל-${formatHour(activeEndHour)} ותקפיץ תזכורת נוחה לפתיחה בחשבון החינמי של Claude — ללא חיובי API. להמשיך?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmActivationDialog = false
                        onEnableAutoSend(
                            promptInput,
                            intervalHours,
                            activeStartHour,
                            activeEndHour,
                            targetConversationId
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier.testTag("confirm_enable_automation_button")
                ) {
                    Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmActivationDialog = false }) {
                    Text(stringResource(R.string.cancel), color = MutedWarmGray)
                }
            }
        )
    }

    if (showScheduleDialog) {
        ScheduleEditorDialog(
            initial = editingSchedule,
            conversations = conversations,
            onDismiss = { showScheduleDialog = false },
            onConfirm = { title, prompt, interval, startH, endH, enabled, targetId ->
                onSaveCustomSchedule(editingSchedule?.id ?: 0L, title, prompt, interval, startH, endH, enabled, targetId)
                showScheduleDialog = false
            }
        )
    }
}

@Composable
private fun HourStepper(
    label: String,
    hour: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = ObsidianBlack,
        border = BorderStroke(1.dp, GoldHairline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MutedWarmGray)
                Text(formatHour(hour), style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
            }
            Row {
                TextButton(onClick = onDecrease) { Text("−", color = GoldPrimary) }
                TextButton(onClick = onIncrease) { Text("+", color = GoldPrimary) }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = WarmIvory,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBlack,
                checkedTrackColor = GoldPrimary
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleEditorDialog(
    initial: ScheduledPromptEntity?,
    conversations: List<ConversationWithSummary>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, prompt: String, interval: Int, startHour: Int, endHour: Int, enabled: Boolean, targetConversationId: Long) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var prompt by remember { mutableStateOf(initial?.promptText.orEmpty()) }
    var interval by remember { mutableIntStateOf(initial?.intervalHours ?: 5) }
    var startHour by remember { mutableIntStateOf(initial?.activeStartHour ?: 8) }
    var endHour by remember { mutableIntStateOf(initial?.activeEndHour ?: 22) }
    var enabled by remember { mutableStateOf(initial?.isEnabled ?: true) }
    var targetConvoId by remember { mutableLongStateOf(initial?.targetConversationId ?: -1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelvetCharcoal,
        titleContentColor = ChampagneGold,
        textContentColor = WarmIvory,
        title = { Text(if (initial == null) "תזמון נוסף חדש" else "עריכת תזמון") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("שם התזמון") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = WarmIvory,
                        textDirection = TextDirection.Content
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = GoldHairline,
                        focusedTextColor = WarmIvory,
                        unfocusedTextColor = WarmIvory
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("תוכן ההודעה ל-Claude") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = WarmIvory,
                        textDirection = TextDirection.Content
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default
                    ),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = GoldHairline,
                        focusedTextColor = WarmIvory,
                        unfocusedTextColor = WarmIvory
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("מרווח בשעות:", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 5, 8, 12, 24).forEach { h ->
                        FilterChip(
                            selected = interval == h,
                            onClick = { interval = h },
                            label = { Text("$h שע'") }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HourStepper(
                        label = "מ־",
                        hour = startHour,
                        onDecrease = { startHour = (startHour + 23) % 24 },
                        onIncrease = { startHour = (startHour + 1) % 24 },
                        modifier = Modifier.weight(1f)
                    )
                    HourStepper(
                        label = "עד",
                        hour = endHour,
                        onDecrease = { endHour = (endHour + 23) % 24 },
                        onIncrease = { endHour = (endHour + 1) % 24 },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("יעד שמירת הודעות:", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                TargetConversationOption(
                    label = "שיחה ייעודית קבועה לאוטומציה",
                    selected = targetConvoId <= 0L,
                    onClick = { targetConvoId = -1L },
                    testTag = "dialog_target_dedicated"
                )
                conversations.take(4).forEach { convo ->
                    TargetConversationOption(
                        label = convo.title,
                        selected = targetConvoId == convo.id,
                        onClick = { targetConvoId = convo.id },
                        testTag = "dialog_target_${convo.id}"
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GoldPrimary,
                            checkmarkColor = ObsidianBlack
                        )
                    )
                    Text("הפעל תזמון זה מיד עם השמירה", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && prompt.isNotBlank()) {
                        onConfirm(title, prompt, interval, startHour, endHour, enabled, targetConvoId)
                    }
                },
                enabled = title.isNotBlank() && prompt.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBlack)
            ) {
                Text("שמור", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ביטול", color = MutedWarmGray)
            }
        }
    )
}

@Composable
private fun TargetConversationOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = GoldPrimary,
                unselectedColor = MutedWarmGray
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) ChampagneGold else WarmIvory
        )
    }
}

private fun formatHour(hour: Int): String = "${hour.toString().padStart(2, '0')}:00"

private fun mapWorkStateToHebrew(state: WorkInfo.State, paused: Boolean, intervalHours: Int): String {
    if (paused) return "מושהה זמנית"
    return when (state) {
        WorkInfo.State.ENQUEUED -> "ממתין בתור (כל $intervalHours שע')"
        WorkInfo.State.RUNNING -> "רץ כעת"
        WorkInfo.State.SUCCEEDED -> "הסתיים בהצלחה"
        WorkInfo.State.FAILED -> "נכשל"
        WorkInfo.State.BLOCKED -> "ממתין לחיבור רשת"
        WorkInfo.State.CANCELLED -> "בוטל"
    }
}
