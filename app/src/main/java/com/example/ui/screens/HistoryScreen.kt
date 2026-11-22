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
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ConversationWithSummary
import com.example.ui.components.GoldDivider
import com.example.ui.components.GoldPillBadge
import com.example.ui.components.HebrewDateFormatter
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElevatedObsidian
import com.example.ui.theme.EmeraldGoldSuccess
import com.example.ui.theme.GoldHairline
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MutedWarmGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftGoldContainer
import com.example.ui.theme.VelvetCharcoal
import com.example.ui.theme.WarmIvory

@Composable
fun HistoryScreen(
    conversations: List<ConversationWithSummary>,
    activeConversationId: Long?,
    matchingConversationIds: Set<Long>? = null,
    onSearchQueryChanged: (String) -> Unit = {},
    onOpenConversation: (Long) -> Unit,
    onNewConversation: () -> Unit,
    onRenameConversation: (Long, String) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ConversationWithSummary?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ConversationWithSummary?>(null) }

    LaunchedEffect(searchQuery) {
        onSearchQueryChanged(searchQuery)
    }

    val filteredConversations = remember(conversations, searchQuery, matchingConversationIds) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            conversations
        } else {
            conversations.filter {
                (matchingConversationIds != null && it.id in matchingConversationIds) ||
                    it.title.contains(q, ignoreCase = true) ||
                    (it.lastMessagePreview?.contains(q, ignoreCase = true) == true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
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
                                text = stringResource(R.string.history_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = ChampagneGold
                            )
                            Text(
                                text = "${conversations.size} שיחות שמורות",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedWarmGray
                            )
                        }
                        Button(
                            onClick = onNewConversation,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("history_new_chat_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = ObsidianBlack
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.new_chat),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    GoldDivider()
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_history_hint),
                        color = MutedWarmGray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = GoldPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "נקה חיפוש",
                                tint = MutedWarmGray
                            )
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = WarmIvory,
                    textDirection = TextDirection.Content
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrectEnabled = true,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = GoldHairline,
                    focusedContainerColor = ElevatedObsidian,
                    unfocusedContainerColor = ElevatedObsidian,
                    focusedTextColor = WarmIvory,
                    unfocusedTextColor = WarmIvory
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("history_search_input")
            )

            if (filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = GoldPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.empty_history_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.empty_history_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedWarmGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredConversations, key = { it.id }) { convo ->
                        val isActive = convo.id == activeConversationId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .clickable { onOpenConversation(convo.id) }
                                .testTag("history_card_${convo.id}"),
                            shape = MaterialTheme.shapes.large,
                            color = if (isActive) SoftGoldContainer else ElevatedObsidian,
                            border = BorderStroke(
                                width = if (isActive) 1.25.dp else 1.dp,
                                color = if (isActive) GoldPrimary else GoldHairline
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = convo.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ChampagneGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (convo.isScheduledThread) {
                                        GoldPillBadge(
                                            text = stringResource(R.string.role_auto),
                                            icon = Icons.Default.Schedule
                                        )
                                    }
                                    val qTrimmed = searchQuery.trim()
                                    val matchedInBodyOnly = qTrimmed.isNotEmpty() &&
                                        matchingConversationIds?.contains(convo.id) == true &&
                                        !convo.title.contains(qTrimmed, ignoreCase = true) &&
                                        convo.lastMessagePreview?.contains(qTrimmed, ignoreCase = true) != true
                                    if (matchedInBodyOnly) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        GoldPillBadge(
                                            text = "בתוך ההודעות",
                                            contentColor = EmeraldGoldSuccess
                                        )
                                    }
                                }

                                if (!convo.lastMessagePreview.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = convo.lastMessagePreview,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WarmIvory.copy(alpha = 0.85f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.messages_count_format, convo.messageCount)} · ${HebrewDateFormatter.formatDateTime(convo.updatedAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedWarmGray
                                    )

                                    Row {
                                        IconButton(
                                            onClick = {
                                                renameTarget = convo
                                                renameText = convo.title
                                            },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("rename_conversation_${convo.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = stringResource(R.string.rename_conversation),
                                                tint = GoldPrimary
                                            )
                                        }
                                        IconButton(
                                            onClick = { deleteTarget = convo },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("delete_conversation_${convo.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = stringResource(R.string.delete_conversation),
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
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = VelvetCharcoal,
            titleContentColor = ChampagneGold,
            textContentColor = WarmIvory,
            title = { Text(text = stringResource(R.string.rename_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(stringResource(R.string.conversation_name_label)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = WarmIvory,
                        textDirection = TextDirection.Content
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_conversation_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = GoldHairline,
                        focusedTextColor = WarmIvory,
                        unfocusedTextColor = WarmIvory
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameConversation(target.id, renameText)
                        renameTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier.testTag("rename_confirm_button")
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.cancel), color = MutedWarmGray)
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = VelvetCharcoal,
            titleContentColor = ChampagneGold,
            textContentColor = WarmIvory,
            title = { Text(text = stringResource(R.string.delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.delete_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConversation(target.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonError,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier.testTag("delete_confirm_button")
                ) {
                    Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel), color = MutedWarmGray)
                }
            }
        )
    }
}
