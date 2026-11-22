package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.launcher.ClaudeLauncher
import com.example.data.local.ConversationWithSummary
import com.example.data.local.MessageEntity
import com.example.data.local.MessageStatus
import com.example.data.local.ResponseLanguage
import com.example.data.preferences.AppSettings
import com.example.ui.components.ClaudeControlMark
import com.example.ui.components.ClaudeThinkingIndicator
import com.example.ui.components.FormattedMarkdownContent
import com.example.ui.components.GoldPillBadge
import com.example.ui.components.HairlineDivider
import com.example.ui.components.HebrewDateFormatter
import com.example.ui.components.MessageFlowDiagram
import com.example.ui.components.OfflineWarningBanner
import com.example.ui.components.luxuryPressScale
import com.example.ui.theme.CardSurfaceHigh
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ClaudeTerracotta
import com.example.ui.theme.ClaudeTerracottaSoft
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElevatedObsidian
import com.example.ui.theme.EmeraldGoldSuccess
import com.example.ui.theme.GoldBorderStrong
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GraphiteDivider
import com.example.ui.theme.MutedWarmGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftGoldContainer
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.UserBubbleDark
import com.example.ui.theme.VelvetCharcoal
import com.example.ui.theme.WarmIvory
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private val STARTER_PROMPTS = listOf(
    "כתוב לי טיוטה של מכתב רשמי מנומס ומרשים בעברית",
    "נתח אסטרטגיה לשיפור יעילות יומית והצג ב-5 נקודות",
    "תן לי רעיון לפרויקט תוכנה עם ארכיטקטורה נקייה",
    "הסבר לי בפשטות מושג מורכב מעולם הבינה המלאכותית"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    conversations: List<ConversationWithSummary>,
    activeConversationId: Long?,
    messages: List<MessageEntity>,
    settings: AppSettings,
    hasApiKey: Boolean,
    isOnline: Boolean,
    isSending: Boolean,
    streamingReplyText: String? = null,
    lastArrivedMessageId: Long?,
    lastOpenedPromptForClipboard: String? = null,
    showTabletSidebar: Boolean,
    onSelectConversation: (Long) -> Unit,
    onNewConversation: () -> Unit,
    onSendViaApi: (String) -> Unit,
    onPrepareDraftOnly: (String) -> Unit,
    onPrepareAndOpenFreeClaude: (String, Boolean) -> Unit,
    onOpenPreparedMessageInClaude: (String, Boolean) -> Unit,
    onMarkMessageSent: (Long) -> Unit,
    onSaveManualReply: (String) -> Unit,
    onCreateTaskFromReply: (title: String, prompt: String, dueAt: Long, remind: Boolean) -> Unit = { _, _, _, _ -> },
    onLatestMessageAnimated: (Long) -> Unit,
    onSelectResponseLanguage: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var composerText by rememberSaveable { mutableStateOf("") }
    var showConversationsSheet by rememberSaveable { mutableStateOf(false) }
    var showManualReplyDialog by remember { mutableStateOf(false) }
    var taskSourceReply by remember { mutableStateOf<MessageEntity?>(null) }
    var clipboardCandidate by remember { mutableStateOf<String?>(null) }
    var dismissedClipboardText by remember { mutableStateOf<String?>(null) }

    val paidReady = settings.isPaidApiReady && hasApiKey
    val activeTitle = conversations.firstOrNull { it.id == activeConversationId }?.title
        ?: stringResource(R.string.new_chat)

    DisposableEffect(lifecycleOwner, paidReady, activeConversationId, lastOpenedPromptForClipboard, messages) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !paidReady && activeConversationId != null && lastOpenedPromptForClipboard != null) {
                val clipText = runCatching {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
                }.getOrNull().orEmpty()

                val lastAssistantText = messages.lastOrNull { it.role == "assistant" }?.content?.trim().orEmpty()
                if (
                    clipText.length >= 12 &&
                    clipText != lastOpenedPromptForClipboard &&
                    clipText != lastAssistantText &&
                    clipText != dismissedClipboardText
                ) {
                    clipboardCandidate = clipText
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isImeVisible = imeBottomPx > 0

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        if (showTabletSidebar) {
            ConversationSidebarPane(
                conversations = conversations,
                activeConversationId = activeConversationId,
                onSelectConversation = onSelectConversation,
                onNewConversation = onNewConversation,
                modifier = Modifier
                    .width(288.dp)
                    .fillMaxHeight()
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(GraphiteDivider)
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .imePadding()
        ) {
            val isCompactIme = isImeVisible && maxHeight < 460.dp

            Column(modifier = Modifier.fillMaxSize()) {
            // Minimalist Studio Chat Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VelvetCharcoal)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClaudeControlMark(size = 34.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = WarmIvory,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (paidReady) {
                                "API פעיל · ${settings.selectedModel}"
                            } else {
                                "חשבון Claude חינמי · ללא עלות API"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (paidReady) EmeraldGoldSuccess else ChampagneGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val md = ClaudeLauncher.buildConversationMarkdown(activeTitle, messages)
                                val shared = ClaudeLauncher.shareText(context, activeTitle, md)
                                if (!shared) {
                                    Toast.makeText(context, "לא נמצאה אפליקציית שיתוף זמינה.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("share_conversation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "שתף שיחה כ-Markdown",
                                tint = ChampagneGold
                            )
                        }
                    }

                    if (activeConversationId != null) {
                        IconButton(
                            onClick = { showManualReplyDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("add_manual_reply_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "שמור תשובה מ-Claude",
                                tint = ChampagneGold
                            )
                        }
                    }

                    if (!showTabletSidebar) {
                        IconButton(
                            onClick = { showConversationsSheet = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_conversations_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = stringResource(R.string.open_conversations),
                                tint = MutedWarmGray
                            )
                        }
                    }

                    IconButton(
                        onClick = onNewConversation,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = stringResource(R.string.new_chat),
                            tint = GoldPrimary
                        )
                    }
                }

                // Response Language Selector Strip (collapses smoothly in compact landscape IME mode)
                AnimatedVisibility(visible = !isCompactIme) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "שפת תגובה",
                            tint = MutedWarmGray,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "שפת תגובה:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedWarmGray
                        )
                        listOf(
                            ResponseLanguage.AUTO to "זיהוי אוטומטי",
                            ResponseLanguage.HEBREW to "עברית",
                            ResponseLanguage.ENGLISH to "אנגלית"
                        ).forEach { (code, label) ->
                            val selected = settings.responseLanguage == code
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectResponseLanguage(code) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("chat_lang_$code"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SoftGoldContainer,
                                    selectedLabelColor = ChampagneGold,
                                    containerColor = ObsidianBlack,
                                    labelColor = MutedWarmGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = GraphiteDivider,
                                    selectedBorderColor = GoldPrimary
                                )
                            )
                        }
                    }
                }
                HairlineDivider()
            }

            OfflineWarningBanner(isOnline = isOnline)

            if (settings.isPaidApiReady && !hasApiKey) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SoftGoldContainer)
                        .border(1.dp, GoldBorderStrong)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = ChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.missing_api_key_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmIvory,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("missing_key_settings_button")
                    ) {
                        Text(
                            text = stringResource(R.string.go_to_settings),
                            color = ChampagneGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (!settings.isPaidApiReady && !isImeVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VelvetCharcoal)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "מצב חינמי: הכנה והעתקה אוטומטית לפתיחה בחשבון Claude הרשמי.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedWarmGray,
                        modifier = Modifier.weight(1f)
                    )
                    if (activeConversationId != null) {
                        TextButton(
                            onClick = { showManualReplyDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("banner_save_reply_button")
                        ) {
                            Text("שמור תשובה", color = ChampagneGold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                HairlineDivider()
            }

            val listState = rememberLazyListState()
            LaunchedEffect(imeBottomPx, messages.size, isSending, streamingReplyText?.length) {
                val totalCount = messages.size + if (isSending) 1 else 0
                if (totalCount > 0) {
                    if (imeBottomPx > 0) {
                        delay(90)
                    }
                    listState.animateScrollToItem(totalCount - 1)
                }
            }

            if (messages.isEmpty() && !isSending) {
                EmptyChatWelcome(
                    isPaidApiMode = paidReady,
                    isOnline = isOnline,
                    onPromptSelected = { prompt ->
                        if (paidReady && isOnline) {
                            onSendViaApi(prompt)
                        } else {
                            onPrepareAndOpenFreeClaude(prompt, false)
                        }
                    },
                    onOpenSettings = onNavigateToSettings,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .widthIn(max = 820.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isNewlyArrived = msg.id == lastArrivedMessageId && msg.role == "assistant" && !msg.isError
                            ClaudeConversationEntry(
                                message = msg,
                                isNewlyArrived = isNewlyArrived,
                                onArrivalFinished = { onLatestMessageAnimated(msg.id) },
                                onOpenInFreeClaude = { preferApp ->
                                    onOpenPreparedMessageInClaude(msg.content, preferApp)
                                },
                                onMarkSent = { onMarkMessageSent(msg.id) },
                                onCreateTask = { taskSourceReply = msg },
                                onRetry = {
                                    val previousUser = messages
                                        .takeWhile { it.id != msg.id }
                                        .lastOrNull { it.role == "user" }
                                    if (previousUser != null) {
                                        if (paidReady) {
                                            onSendViaApi(previousUser.content)
                                        } else {
                                            onOpenPreparedMessageInClaude(previousUser.content, false)
                                        }
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                        if (isSending) {
                            item(key = "streaming_or_thinking") {
                                if (!streamingReplyText.isNullOrBlank()) {
                                    StreamingAssistantPreview(text = streamingReplyText)
                                } else {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        ClaudeThinkingIndicator(
                                            label = stringResource(R.string.claude_thinking)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Smart Foreground Clipboard Reply Detection Banner (Free Mode)
            val detectedClip = clipboardCandidate
            if (detectedClip != null && !paidReady && activeConversationId != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VelvetCharcoal),
                    color = SoftGoldContainer,
                    border = BorderStroke(1.dp, GoldBorderStrong)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPasteGo,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "זוהתה תשובה חדשה בלוח ההעתקה",
                                style = MaterialTheme.typography.labelMedium,
                                color = ChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = detectedClip.take(70),
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmIvory,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = {
                                onSaveManualReply(detectedClip)
                                dismissedClipboardText = detectedClip
                                clipboardCandidate = null
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("clipboard_save_reply_button")
                        ) {
                            Text("שמור בשיחה", color = ChampagneGold, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                dismissedClipboardText = detectedClip
                                clipboardCandidate = null
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("clipboard_dismiss_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "התעלם",
                                tint = MutedWarmGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Clean Docked Studio Composer (zero dead gap above keyboard; inline controls flush with keyboard top)
            val composerTextDirection = when (settings.responseLanguage) {
                ResponseLanguage.HEBREW -> TextDirection.Rtl
                ResponseLanguage.ENGLISH -> TextDirection.Ltr
                else -> TextDirection.Content
            }
            val canSend = composerText.isNotBlank() && !isSending && (!paidReady || isOnline)
            val sendInteraction = remember { MutableInteractionSource() }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VelvetCharcoal)
            ) {
                HairlineDivider()
                Row(
                    modifier = Modifier
                        .widthIn(max = 820.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val toSave = composerText.trim()
                            if (toSave.isNotEmpty()) {
                                composerText = ""
                                onPrepareDraftOnly(toSave)
                            }
                        },
                        enabled = composerText.isNotBlank() && !isSending,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("prepare_draft_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "שמור כטיוטה לשליחה",
                            tint = if (composerText.isNotBlank() && !isSending) ChampagneGold else MutedWarmGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OutlinedTextField(
                        value = composerText,
                        onValueChange = { composerText = it },
                        placeholder = {
                            Text(
                                text = if (paidReady) {
                                    stringResource(R.string.chat_input_placeholder)
                                } else {
                                    "כתוב הודעה בעברית או באנגלית…"
                                },
                                color = MutedWarmGray,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = WarmIvory,
                            textDirection = composerTextDirection
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = true,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input"),
                        minLines = 1,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = GraphiteDivider,
                            focusedContainerColor = ElevatedObsidian,
                            unfocusedContainerColor = ElevatedObsidian,
                            focusedTextColor = WarmIvory,
                            unfocusedTextColor = WarmIvory,
                            cursorColor = ChampagneGold
                        )
                    )

                    Button(
                        onClick = {
                            val toSend = composerText.trim()
                            if (toSend.isNotEmpty() && !isSending) {
                                composerText = ""
                                if (paidReady) {
                                    onSendViaApi(toSend)
                                } else {
                                    onPrepareAndOpenFreeClaude(toSend, false)
                                }
                            }
                        },
                        interactionSource = sendInteraction,
                        enabled = canSend,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = ObsidianBlack,
                            disabledContainerColor = CardSurfaceHigh,
                            disabledContentColor = MutedWarmGray
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .luxuryPressScale(sendInteraction)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = if (paidReady) Icons.AutoMirrored.Filled.Send else Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = if (paidReady) "שלח הודעה" else "פתח ב-Claude",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (paidReady) "שלח" else "פתח",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            }
        }
    }

    if (showConversationsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showConversationsSheet = false },
            sheetState = sheetState,
            containerColor = VelvetCharcoal,
            contentColor = WarmIvory
        ) {
            ConversationSidebarPane(
                conversations = conversations,
                activeConversationId = activeConversationId,
                onSelectConversation = { id ->
                    onSelectConversation(id)
                    showConversationsSheet = false
                },
                onNewConversation = {
                    onNewConversation()
                    showConversationsSheet = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            )
        }
    }

    if (showManualReplyDialog) {
        var replyInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManualReplyDialog = false },
            containerColor = VelvetCharcoal,
            titleContentColor = ChampagneGold,
            textContentColor = WarmIvory,
            title = { Text("שמירת תשובה מ-Claude") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "הדבק כאן תשובה שקיבלת בחשבון החינמי של Claude כדי לשמור אותה בהיסטוריה ובסטטיסטיקות.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedWarmGray
                    )
                    OutlinedTextField(
                        value = replyInput,
                        onValueChange = { replyInput = it },
                        minLines = 3,
                        maxLines = 8,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = true,
                            keyboardType = KeyboardType.Text
                        ),
                        placeholder = { Text("הדבק את תשובת Claude…") },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = GraphiteDivider,
                            focusedTextColor = WarmIvory,
                            unfocusedTextColor = WarmIvory
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_reply_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (replyInput.isNotBlank()) {
                            onSaveManualReply(replyInput)
                            showManualReplyDialog = false
                        }
                    },
                    enabled = replyInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier.testTag("save_manual_reply_confirm")
                ) {
                    Text("שמור תשובה", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualReplyDialog = false }) {
                    Text("ביטול", color = MutedWarmGray)
                }
            }
        )
    }

    taskSourceReply?.let { replyMsg ->
        var taskTitle by remember(replyMsg.id) {
            mutableStateOf(
                replyMsg.content.lineSequence().firstOrNull { it.isNotBlank() }
                    ?.replace("#", "")?.trim()?.take(42)
                    ?.ifEmpty { "משימה מתשובת Claude" }
                    ?: "משימה מתשובת Claude"
            )
        }
        var taskPrompt by remember(replyMsg.id) { mutableStateOf(replyMsg.content.take(500)) }
        var dueAtMillis by remember(replyMsg.id) {
            mutableLongStateOf(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1))
        }
        AlertDialog(
            onDismissRequest = { taskSourceReply = null },
            containerColor = VelvetCharcoal,
            titleContentColor = ChampagneGold,
            textContentColor = WarmIvory,
            title = { Text("יצירת משימה מתשובה") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("כותרת המשימה") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = WarmIvory,
                            textDirection = TextDirection.Content
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = GraphiteDivider,
                            focusedTextColor = WarmIvory,
                            unfocusedTextColor = WarmIvory
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = taskPrompt,
                        onValueChange = { taskPrompt = it },
                        label = { Text("תוכן / הנחיה למשימה") },
                        minLines = 2,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            color = WarmIvory,
                            textDirection = TextDirection.Content
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = GraphiteDivider,
                            focusedTextColor = WarmIvory,
                            unfocusedTextColor = WarmIvory
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("מועד תזכורת:", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "בעוד 15 ד'" to TimeUnit.MINUTES.toMillis(15),
                            "בעוד שעה" to TimeUnit.HOURS.toMillis(1),
                            "בעוד 5 שע'" to TimeUnit.HOURS.toMillis(5),
                            "מחר" to TimeUnit.HOURS.toMillis(24)
                        ).forEach { (label, offset) ->
                            val now = System.currentTimeMillis()
                            FilterChip(
                                selected = kotlin.math.abs((dueAtMillis - now) - offset) < TimeUnit.MINUTES.toMillis(5),
                                onClick = { dueAtMillis = System.currentTimeMillis() + offset },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            onCreateTaskFromReply(taskTitle.trim(), taskPrompt.trim(), dueAtMillis, true)
                            taskSourceReply = null
                        }
                    },
                    enabled = taskTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBlack),
                    modifier = Modifier.testTag("confirm_create_task_from_reply")
                ) {
                    Text("צור משימה", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskSourceReply = null }) {
                    Text("ביטול", color = MutedWarmGray)
                }
            }
        )
    }
}

@Composable
private fun ConversationSidebarPane(
    conversations: List<ConversationWithSummary>,
    activeConversationId: Long?,
    onSelectConversation: (Long) -> Unit,
    onNewConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(VelvetCharcoal)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.open_conversations),
                style = MaterialTheme.typography.titleMedium,
                color = ChampagneGold
            )
            TextButton(
                onClick = onNewConversation,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("sidebar_new_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AddComment,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.new_chat), color = GoldPrimary)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        HairlineDivider()
        Spacer(modifier = Modifier.height(8.dp))

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_history_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedWarmGray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(conversations, key = { it.id }) { convo ->
                    val isSelected = convo.id == activeConversationId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(if (isSelected) SoftGoldContainer else Color.Transparent)
                            .clickable { onSelectConversation(convo.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("sidebar_conversation_${convo.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .height(28.dp)
                                    .background(GoldPrimary)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (convo.isScheduledThread) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = convo.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) ChampagneGold else WarmIvory,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = convo.lastMessagePreview ?: "…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedWarmGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    HairlineDivider()
                }
            }
        }
    }
}

@Composable
private fun EmptyChatWelcome(
    isPaidApiMode: Boolean,
    isOnline: Boolean,
    onPromptSelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 640.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ClaudeControlMark(size = 48.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.empty_chat_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = WarmIvory
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.empty_chat_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedWarmGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                MessageFlowDiagram(isPaidApiMode = isPaidApiMode, isOnline = isOnline)

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenSettings,
                    border = BorderStroke(1.dp, GraphiteDivider),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_state_configure_api_button")
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPaidApiMode) "הגדרות API ומודל" else "הגדרות חיבור (ברירת מחדל: חינמי)",
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "הצעות לפתיחה מהירה",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedWarmGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .border(1.dp, GraphiteDivider, MaterialTheme.shapes.small)
                        .background(VelvetCharcoal)
                ) {
                    STARTER_PROMPTS.forEachIndexed { index, prompt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPromptSelected(prompt) }
                                .padding(horizontal = 14.dp, vertical = 13.dp)
                                .testTag("starter_prompt_$index"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(ClaudeTerracotta, shape = RoundedCornerShape(1.dp))
                            )
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoftWhite,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MutedWarmGray,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        if (index < STARTER_PROMPTS.lastIndex) {
                            HairlineDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingAssistantPreview(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.small)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight()
                .background(ClaudeTerracotta)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClaudeControlMark(size = 22.dp)
                Text(
                    text = stringResource(R.string.role_claude),
                    style = MaterialTheme.typography.labelLarge,
                    color = ChampagneGold
                )
                GoldPillBadge(
                    text = "כותב בזמן אמת…",
                    containerColor = ClaudeTerracottaSoft,
                    contentColor = ClaudeTerracotta
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            FormattedMarkdownContent(
                text = text,
                textColor = WarmIvory
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClaudeConversationEntry(
    message: MessageEntity,
    isNewlyArrived: Boolean,
    onArrivalFinished: () -> Unit,
    onOpenInFreeClaude: (preferOfficialApp: Boolean) -> Unit,
    onMarkSent: () -> Unit,
    onCreateTask: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isUser = message.role == "user"
    val isPrepared = message.status == MessageStatus.PREPARED

    var visible by remember { mutableStateOf(false) }
    var highlightBurst by remember(isNewlyArrived) { mutableStateOf(isNewlyArrived) }

    LaunchedEffect(message.id) { visible = true }
    LaunchedEffect(isNewlyArrived) {
        if (isNewlyArrived) {
            delay(950)
            highlightBurst = false
            onArrivalFinished()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 6 },
        modifier = modifier
    ) {
        if (isUser) {
            // Compact user prompt block aligned cleanly on start side (right in RTL)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 580.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(UserBubbleDark)
                        .border(
                            width = 1.dp,
                            color = if (isPrepared) GoldPrimary.copy(alpha = 0.6f) else GraphiteDivider,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.role_user),
                            style = MaterialTheme.typography.labelLarge,
                            color = ChampagneGold
                        )
                        StatusBadgeForMessage(message.status, message.isError)
                        if (message.isScheduled) {
                            GoldPillBadge(text = stringResource(R.string.role_auto), icon = Icons.Default.Schedule)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = HebrewDateFormatter.formatTimeOnly(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedWarmGray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    FormattedMarkdownContent(
                        text = message.content,
                        textColor = WarmIvory
                    )

                    if (isPrepared) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onOpenInFreeClaude(false) },
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = ObsidianBlack
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_free_claude_${message.id}")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("פתח ב-Claude (חינם)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onOpenInFreeClaude(true) },
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, GraphiteDivider),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftWhite),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("share_claude_app_${message.id}")
                            ) {
                                Text("שתף לאפליקציה", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onMarkSent,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, EmeraldGoldSuccess.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGoldSuccess),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("mark_sent_${message.id}")
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("סמן כנשלחה", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(message.content))
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.copied_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("copy_message_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.copy_message),
                                tint = MutedWarmGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Clean Claude-style editorial response directly on canvas with subtle terracotta/gold accent bar
            val bgHighlight by animateColorAsState(
                targetValue = if (highlightBurst) SoftGoldContainer else if (message.isError) CrimsonContainer else Color.Transparent,
                animationSpec = tween(durationMillis = 500),
                label = "assistant_highlight"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(MaterialTheme.shapes.small)
                    .background(bgHighlight)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .fillMaxHeight()
                        .background(if (message.isError) CrimsonError else ClaudeTerracotta)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (message.isError) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = CrimsonError,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            ClaudeControlMark(size = 22.dp)
                        }
                        Text(
                            text = stringResource(R.string.role_claude),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (message.isError) CrimsonError else ChampagneGold
                        )
                        StatusBadgeForMessage(message.status, message.isError)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = HebrewDateFormatter.formatTimeOnly(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedWarmGray
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FormattedMarkdownContent(
                        text = message.content,
                        textColor = if (message.isError) CrimsonError else WarmIvory
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!message.isError && (message.inputTokens > 0 || message.outputTokens > 0)) {
                            Text(
                                text = stringResource(
                                    R.string.tokens_format,
                                    message.inputTokens,
                                    message.outputTokens
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedWarmGray
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (message.isError) {
                                TextButton(
                                    onClick = onRetry,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("retry_message_${message.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.retry_action),
                                        color = ChampagneGold
                                    )
                                }
                            }

                            if (!message.isError) {
                                IconButton(
                                    onClick = onCreateTask,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("create_task_from_msg_${message.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddTask,
                                        contentDescription = "צור משימה מתשובה זו",
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val shared = ClaudeLauncher.shareText(context, "תשובה מ-Claude", message.content)
                                        if (!shared) {
                                            Toast.makeText(context, "לא נמצאה אפליקציית שיתוף.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("share_message_${message.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "שתף תשובה",
                                        tint = MutedWarmGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(message.content))
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.copied_toast),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("copy_message_${message.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.copy_message),
                                    tint = MutedWarmGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadgeForMessage(status: String, isError: Boolean) {
    val (label, icon, container, content) = when {
        isError || status == MessageStatus.FAILED -> Quad(
            "נכשלה",
            Icons.Default.ErrorOutline,
            CrimsonContainer,
            CrimsonError
        )
        status == MessageStatus.PREPARED -> Quad(
            "מוכנה לשליחה",
            Icons.Default.PendingActions,
            ClaudeTerracottaSoft,
            ClaudeTerracotta
        )
        status == MessageStatus.RECEIVED -> Quad(
            "התקבלה",
            Icons.Default.CheckCircle,
            SuccessContainer,
            EmeraldGoldSuccess
        )
        else -> Quad(
            "נשלחה בפועל",
            Icons.Default.DoneAll,
            SoftGoldContainer,
            ChampagneGold
        )
    }
    GoldPillBadge(
        text = label,
        icon = icon,
        containerColor = container,
        contentColor = content
    )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
