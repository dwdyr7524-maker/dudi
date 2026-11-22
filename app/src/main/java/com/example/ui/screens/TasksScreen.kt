package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TaskAlt
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.data.local.TaskEntity
import com.example.ui.components.GoldDivider
import com.example.ui.components.GoldPillBadge
import com.example.ui.components.HebrewDateFormatter
import com.example.ui.components.luxuryPressScale
import com.example.ui.theme.CardSurfaceHigh
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElevatedObsidian
import com.example.ui.theme.EmeraldGoldSuccess
import com.example.ui.theme.GoldHairline
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.MutedWarmGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftGoldContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.VelvetCharcoal
import com.example.ui.theme.WarmIvory
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    onSaveTask: (existingId: Long, title: String, promptText: String, dueAt: Long, remindEnabled: Boolean) -> Unit,
    onToggleCompleted: (TaskEntity) -> Unit,
    onDuplicateTask: (TaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onOpenTaskPromptInClaude: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) } // 0 = active/upcoming, 1 = completed
    var editorTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }

    val activeTasks = remember(tasks) { tasks.filter { !it.isCompleted } }
    val completedTasks = remember(tasks) { tasks.filter { it.isCompleted } }
    val displayedTasks = if (selectedFilter == 0) activeTasks else completedTasks

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
                        Column {
                            Text(
                                text = "ניהול משימות ותזכורות",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ChampagneGold
                            )
                            Text(
                                text = "${activeTasks.size} פעילות · ${completedTasks.size} הושלמו",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedWarmGray
                            )
                        }
                        val createInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                editorTask = null
                                showEditorDialog = true
                            },
                            interactionSource = createInteraction,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .luxuryPressScale(createInteraction)
                                .testTag("create_task_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = ObsidianBlack
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("משימה חדשה", fontWeight = FontWeight.Bold)
                        }
                    }
                    GoldDivider()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    label = { Text("עתידיות ופעילות (${activeTasks.size})") },
                    modifier = Modifier.testTag("filter_active_tasks"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SoftGoldContainer,
                        selectedLabelColor = ChampagneGold
                    )
                )
                FilterChip(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    label = { Text("הושלמו (${completedTasks.size})") },
                    modifier = Modifier.testTag("filter_completed_tasks"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SuccessContainer,
                        selectedLabelColor = EmeraldGoldSuccess
                    )
                )
            }

            if (displayedTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = GoldPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedFilter == 0) "אין משימות פעילות כרגע" else "טרם הושלמו משימות",
                            style = MaterialTheme.typography.titleLarge,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "צור משימה חדשה עם מועד ביצוע והודעה מוכנה ל-Claude.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedWarmGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedTasks, key = { it.id }) { task ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_card_${task.id}"),
                            shape = MaterialTheme.shapes.large,
                            color = if (task.isCompleted) ElevatedObsidian else MidnightNavy,
                            border = BorderStroke(
                                1.dp,
                                if (task.isCompleted) EmeraldGoldSuccess.copy(alpha = 0.45f) else GoldHairline
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onToggleCompleted(task) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("toggle_task_${task.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "סמן משימה כהושלמה",
                                            tint = if (task.isCompleted) EmeraldGoldSuccess else GoldPrimary
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (task.isCompleted) MutedWarmGray else WarmIvory,
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                        Text(
                                            text = "מועד ביצוע: ${HebrewDateFormatter.formatDateTime(task.dueAt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ChampagneGold
                                        )
                                    }
                                    if (task.remindEnabled && !task.isCompleted) {
                                        GoldPillBadge(
                                            text = "תזכורת",
                                            icon = Icons.Default.NotificationsActive
                                        )
                                    }
                                }

                                if (task.promptText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = ObsidianBlack.copy(alpha = 0.65f),
                                        border = BorderStroke(0.75.dp, GoldHairline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = task.promptText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = WarmIvory.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (task.promptText.isNotBlank()) {
                                        Button(
                                            onClick = { onOpenTaskPromptInClaude(task.promptText) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = GoldPrimary,
                                                contentColor = ObsidianBlack
                                            ),
                                            modifier = Modifier.testTag("run_task_claude_${task.id}")
                                        ) {
                                            Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("שלח ל-Claude", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            editorTask = task
                                            showEditorDialog = true
                                        },
                                        border = BorderStroke(1.dp, GoldHairline),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold),
                                        modifier = Modifier.testTag("edit_task_${task.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ערוך")
                                    }
                                    OutlinedButton(
                                        onClick = { onDuplicateTask(task) },
                                        border = BorderStroke(1.dp, GoldHairline),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmIvory),
                                        modifier = Modifier.testTag("duplicate_task_${task.id}")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("שכפל")
                                    }
                                    IconButton(
                                        onClick = { onDeleteTask(task.id) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("delete_task_${task.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "מחק משימה",
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

    if (showEditorDialog) {
        TaskEditorDialog(
            initialTask = editorTask,
            onDismiss = { showEditorDialog = false },
            onConfirm = { title, prompt, dueAt, remind ->
                onSaveTask(editorTask?.id ?: 0L, title, prompt, dueAt, remind)
                showEditorDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskEditorDialog(
    initialTask: TaskEntity?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, prompt: String, dueAt: Long, remind: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title.orEmpty()) }
    var prompt by remember { mutableStateOf(initialTask?.promptText.orEmpty()) }
    var dueAt by remember {
        mutableLongStateOf(initialTask?.dueAt ?: (System.currentTimeMillis() + TimeUnit.HOURS.toMillis(5)))
    }
    var remind by remember { mutableStateOf(initialTask?.remindEnabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelvetCharcoal,
        titleContentColor = ChampagneGold,
        textContentColor = WarmIvory,
        title = { Text(if (initialTask == null) "יצירת משימה חדשה" else "עריכת משימה") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("שם המשימה") },
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
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = GoldHairline,
                        focusedTextColor = WarmIvory,
                        unfocusedTextColor = WarmIvory
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("הודעה מוכנה ל-Claude (אופציונלי)") },
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
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = GoldHairline,
                        focusedTextColor = WarmIvory,
                        unfocusedTextColor = WarmIvory
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_prompt_input")
                )

                Text(
                    text = "מועד ביצוע: ${HebrewDateFormatter.formatDateTime(dueAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ChampagneGold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "בעוד 15 דק'" to TimeUnit.MINUTES.toMillis(15),
                        "בעוד שעה" to TimeUnit.HOURS.toMillis(1),
                        "בעוד 5 שעות" to TimeUnit.HOURS.toMillis(5),
                        "מחר (+24 שעות)" to TimeUnit.HOURS.toMillis(24)
                    ).forEach { (label, offset) ->
                        FilterChip(
                            selected = false,
                            onClick = { dueAt = System.currentTimeMillis() + offset },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CardSurfaceHigh,
                                labelColor = WarmIvory
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = remind,
                        onCheckedChange = { remind = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GoldPrimary,
                            checkmarkColor = ObsidianBlack
                        )
                    )
                    Text("הפעל תזכורת התראה במועד זה", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title, prompt, dueAt, remind) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = ObsidianBlack
                ),
                modifier = Modifier.testTag("save_task_dialog_button")
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
