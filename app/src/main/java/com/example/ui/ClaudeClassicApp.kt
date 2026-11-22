package com.example.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.components.ClaudeControlMark
import com.example.ui.components.HairlineDivider
import com.example.ui.components.InAppNotificationBanner
import com.example.ui.components.SplashRevealOverlay
import com.example.ui.navigation.AutomationRoute
import com.example.ui.navigation.ChatRoute
import com.example.ui.navigation.HistoryRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.NotificationsRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.StatsRoute
import com.example.ui.navigation.TasksRoute
import com.example.ui.screens.AutomationScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.GraphiteDivider
import com.example.ui.theme.MutedWarmGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftGoldContainer
import com.example.ui.theme.VelvetCharcoal
import com.example.ui.theme.WarmIvory
import com.example.ui.viewmodel.ClaudeViewModel

private data class TopNavItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val showBadge: Boolean = false
)

private val PHONE_NAV_ITEMS = listOf(
    TopNavItem(HomeRoute, "בית", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    TopNavItem(ChatRoute, "שיחה", Icons.Filled.Forum, Icons.Outlined.Forum, "nav_chat"),
    TopNavItem(TasksRoute, "משימות", Icons.Filled.TaskAlt, Icons.Outlined.TaskAlt, "nav_tasks"),
    TopNavItem(AutomationRoute, "אוטומציה", Icons.Filled.Schedule, Icons.Outlined.Schedule, "nav_automation"),
    TopNavItem(SettingsRoute, "הגדרות", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

private val TABLET_RAIL_ITEMS = listOf(
    TopNavItem(HomeRoute, "בית", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    TopNavItem(ChatRoute, "שיחה", Icons.Filled.Forum, Icons.Outlined.Forum, "nav_chat"),
    TopNavItem(HistoryRoute, "היסטוריה", Icons.Filled.History, Icons.Outlined.History, "nav_history"),
    TopNavItem(TasksRoute, "משימות", Icons.Filled.TaskAlt, Icons.Outlined.TaskAlt, "nav_tasks"),
    TopNavItem(AutomationRoute, "אוטומציה", Icons.Filled.Schedule, Icons.Outlined.Schedule, "nav_automation"),
    TopNavItem(NotificationsRoute, "התראות", Icons.Filled.Notifications, Icons.Outlined.Notifications, "nav_notifications", showBadge = true),
    TopNavItem(StatsRoute, "סטטיסטיקות", Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_stats"),
    TopNavItem(SettingsRoute, "הגדרות", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClaudeConnectApp(
    viewModel: ClaudeViewModel,
    intentTargetRoute: Any? = null,
    onIntentTargetConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    val maskedApiKey by viewModel.maskedApiKey.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val streamingReplyText by viewModel.streamingReplyText.collectAsStateWithLifecycle()
    val lastArrivedMessageId by viewModel.lastArrivedMessageId.collectAsStateWithLifecycle()
    val lastOpenedPromptForClipboard by viewModel.lastOpenedPromptForClipboard.collectAsStateWithLifecycle()
    val historyMatchingIds by viewModel.historyMatchingIds.collectAsStateWithLifecycle()
    val connectionTestState by viewModel.connectionTestState.collectAsStateWithLifecycle()
    val scheduleWorkState by viewModel.scheduleWorkState.collectAsStateWithLifecycle()
    val inAppAlert by viewModel.inAppAlert.collectAsStateWithLifecycle()
    val scheduledPrompts by viewModel.scheduledPrompts.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val actionLogs by viewModel.actionLogs.collectAsStateWithLifecycle()
    val dashboardStats by viewModel.dashboardStats.collectAsStateWithLifecycle()

    var showSplash by rememberSaveable { mutableStateOf(true) }
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(intentTargetRoute) {
        if (intentTargetRoute != null) {
            navigateTopLevel(intentTargetRoute)
            onIntentTargetConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack)
        ) {
            val useNavigationRail = maxWidth >= 600.dp
            val showTabletChatSidebar = maxWidth >= 840.dp

            Scaffold(
                containerColor = ObsidianBlack,
                contentWindowInsets = WindowInsets.systemBars,
                topBar = {
                    if (!useNavigationRail) {
                        Surface(color = VelvetCharcoal) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        ClaudeControlMark(size = 24.dp)
                                        Text(
                                            text = stringResource(R.string.app_name),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ChampagneGold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { navigateTopLevel(HistoryRoute) },
                                            modifier = Modifier.testTag("topbar_history_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.History,
                                                contentDescription = "היסטוריית שיחות",
                                                tint = if (currentDestination?.hasRoute(HistoryRoute::class) == true) ChampagneGold else MutedWarmGray
                                            )
                                        }
                                        IconButton(
                                            onClick = { navigateTopLevel(StatsRoute) },
                                            modifier = Modifier.testTag("topbar_stats_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.BarChart,
                                                contentDescription = "סטטיסטיקות",
                                                tint = if (currentDestination?.hasRoute(StatsRoute::class) == true) ChampagneGold else MutedWarmGray
                                            )
                                        }
                                        IconButton(
                                            onClick = { navigateTopLevel(NotificationsRoute) },
                                            modifier = Modifier.testTag("topbar_notifications_button")
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    if (unreadCount > 0) {
                                                        Badge(
                                                            containerColor = CrimsonError,
                                                            contentColor = WarmIvory
                                                        ) {
                                                            Text(unreadCount.coerceAtMost(99).toString())
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Notifications,
                                                    contentDescription = "מרכז התראות",
                                                    tint = if (currentDestination?.hasRoute(NotificationsRoute::class) == true) ChampagneGold else MutedWarmGray
                                                )
                                            }
                                        }
                                    }
                                }
                                HairlineDivider()
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!useNavigationRail && !isImeVisible) {
                        Column {
                            HairlineDivider()
                            NavigationBar(
                                containerColor = VelvetCharcoal,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                PHONE_NAV_ITEMS.forEach { item ->
                                    val selected = currentDestination?.hasRoute(item.route::class) == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(item.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ChampagneGold,
                                            selectedTextColor = ChampagneGold,
                                            indicatorColor = SoftGoldContainer,
                                            unselectedIconColor = MutedWarmGray,
                                            unselectedTextColor = MutedWarmGray
                                        ),
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = VelvetCharcoal,
                            header = {
                                Spacer(modifier = Modifier.height(10.dp))
                                ClaudeControlMark(size = 34.dp)
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TABLET_RAIL_ITEMS.forEach { item ->
                                val selected = currentDestination?.hasRoute(item.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (item.showBadge && unreadCount > 0) {
                                                    Badge(
                                                        containerColor = CrimsonError,
                                                        contentColor = WarmIvory
                                                    ) {
                                                        Text(unreadCount.coerceAtMost(99).toString())
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ChampagneGold,
                                        selectedTextColor = ChampagneGold,
                                        indicatorColor = SoftGoldContainer,
                                        unselectedIconColor = MutedWarmGray,
                                        unselectedTextColor = MutedWarmGray
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(GraphiteDivider)
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = HomeRoute,
                            enterTransition = {
                                fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 16 }
                            },
                            exitTransition = {
                                fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { it / 16 }
                            },
                            popEnterTransition = {
                                fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 16 }
                            },
                            popExitTransition = {
                                fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { -it / 16 }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable<HomeRoute> {
                                HomeScreen(
                                    settings = settings,
                                    stats = dashboardStats,
                                    hasApiKey = hasApiKey,
                                    isOnline = isOnline,
                                    isTablet = useNavigationRail,
                                    onOpenChat = { navigateTopLevel(ChatRoute) },
                                    onOpenTasks = { navigateTopLevel(TasksRoute) },
                                    onOpenAutomation = { navigateTopLevel(AutomationRoute) },
                                    onOpenNotifications = { navigateTopLevel(NotificationsRoute) },
                                    onOpenStats = { navigateTopLevel(StatsRoute) },
                                    onOpenSettings = { navigateTopLevel(SettingsRoute) },
                                    onOpenHistory = { navigateTopLevel(HistoryRoute) },
                                    onToggleFloatingBubble = { enable ->
                                        viewModel.toggleFloatingBubble(context, enable)
                                    }
                                )
                            }

                            composable<ChatRoute> {
                                ChatScreen(
                                    conversations = conversations,
                                    activeConversationId = activeConversationId,
                                    messages = messages,
                                    settings = settings,
                                    hasApiKey = hasApiKey,
                                    isOnline = isOnline,
                                    isSending = isSending,
                                    streamingReplyText = streamingReplyText,
                                    lastArrivedMessageId = lastArrivedMessageId,
                                    lastOpenedPromptForClipboard = lastOpenedPromptForClipboard,
                                    showTabletSidebar = showTabletChatSidebar,
                                    onSelectConversation = { id -> viewModel.selectConversation(id) },
                                    onNewConversation = { viewModel.startNewConversation() },
                                    onSendViaApi = { text -> viewModel.sendMessageViaApi(text) },
                                    onPrepareDraftOnly = { text -> viewModel.prepareDraftOnly(text) },
                                    onPrepareAndOpenFreeClaude = { text, preferApp ->
                                        viewModel.prepareAndOpenFreeClaude(context, text, preferApp)
                                    },
                                    onOpenPreparedMessageInClaude = { text, preferApp ->
                                        viewModel.openExistingPreparedMessageInClaude(context, text, preferApp)
                                    },
                                    onMarkMessageSent = { id -> viewModel.markMessageSent(id) },
                                    onSaveManualReply = { reply -> viewModel.saveManualReply(reply) },
                                    onCreateTaskFromReply = { title, prompt, dueAt, remind ->
                                        viewModel.saveTask(0L, title, prompt, dueAt, remind)
                                    },
                                    onLatestMessageAnimated = { id -> viewModel.markLatestMessageAnimated(id) },
                                    onSelectResponseLanguage = { lang -> viewModel.setResponseLanguage(lang) },
                                    onNavigateToSettings = { navigateTopLevel(SettingsRoute) }
                                )
                            }

                            composable<HistoryRoute> {
                                HistoryScreen(
                                    conversations = conversations,
                                    activeConversationId = activeConversationId,
                                    matchingConversationIds = historyMatchingIds,
                                    onSearchQueryChanged = { q -> viewModel.updateHistorySearchQuery(q) },
                                    onOpenConversation = { id ->
                                        viewModel.selectConversation(id)
                                        navigateTopLevel(ChatRoute)
                                    },
                                    onNewConversation = {
                                        viewModel.startNewConversation()
                                        navigateTopLevel(ChatRoute)
                                    },
                                    onRenameConversation = { id, title ->
                                        viewModel.renameConversation(id, title)
                                    },
                                    onDeleteConversation = { id ->
                                        viewModel.deleteConversation(id)
                                    }
                                )
                            }

                            composable<TasksRoute> {
                                TasksScreen(
                                    tasks = tasks,
                                    onSaveTask = { existingId, title, prompt, dueAt, remind ->
                                        viewModel.saveTask(existingId, title, prompt, dueAt, remind)
                                    },
                                    onToggleCompleted = { task -> viewModel.toggleTaskCompleted(task) },
                                    onDuplicateTask = { task -> viewModel.duplicateTask(task) },
                                    onDeleteTask = { id -> viewModel.deleteTask(id) },
                                    onOpenTaskPromptInClaude = { prompt ->
                                        if (settings.isPaidApiReady && hasApiKey) {
                                            viewModel.sendMessageViaApi(prompt)
                                            navigateTopLevel(ChatRoute)
                                        } else {
                                            viewModel.prepareAndOpenFreeClaude(context, prompt, false)
                                        }
                                    }
                                )
                            }

                            composable<AutomationRoute> {
                                AutomationScreen(
                                    settings = settings,
                                    conversations = conversations,
                                    scheduledPrompts = scheduledPrompts,
                                    scheduleWorkState = scheduleWorkState,
                                    hasApiKey = hasApiKey,
                                    isOnline = isOnline,
                                    onEnableAutoSend = { prompt, interval, startH, endH, convoId ->
                                        viewModel.enableAutoSend(prompt, interval, startH, endH, convoId)
                                    },
                                    onPauseAutoSend = { prompt, interval, startH, endH, convoId ->
                                        viewModel.pauseAutoSend(prompt, interval, startH, endH, convoId)
                                    },
                                    onResumeAutoSend = { prompt, interval, startH, endH, convoId ->
                                        viewModel.resumeAutoSend(prompt, interval, startH, endH, convoId)
                                    },
                                    onDisableAutoSend = { prompt, interval, startH, endH, convoId ->
                                        viewModel.disableAutoSend(prompt, interval, startH, endH, convoId)
                                    },
                                    onSavePromptOnly = { prompt, interval, startH, endH, convoId ->
                                        viewModel.saveAutoSendPromptOnly(prompt, interval, startH, endH, convoId)
                                    },
                                    onRunNow = { prompt, interval, startH, endH, convoId ->
                                        viewModel.runAutoSendNow(prompt, interval, startH, endH, convoId)
                                    },
                                    onSaveCustomSchedule = { id, title, prompt, interval, startH, endH, enabled, targetConvoId ->
                                        viewModel.saveCustomSchedule(id, title, prompt, interval, startH, endH, enabled, targetConvoId)
                                    },
                                    onToggleCustomSchedule = { schedule ->
                                        viewModel.toggleCustomSchedule(schedule)
                                    },
                                    onDeleteCustomSchedule = { id ->
                                        viewModel.deleteCustomSchedule(id)
                                    },
                                    onRunCustomScheduleNow = { id ->
                                        viewModel.runCustomScheduleNow(id)
                                    },
                                    onUpdateNotificationPreferences = { replies, errors, taskReminders ->
                                        viewModel.updateNotificationPreferences(replies, errors, taskReminders)
                                    }
                                )
                            }

                            composable<NotificationsRoute> {
                                NotificationsScreen(
                                    notifications = notifications,
                                    onMarkRead = { id -> viewModel.markNotificationRead(id) },
                                    onMarkAllRead = { viewModel.markAllNotificationsRead() },
                                    onDeleteNotification = { id -> viewModel.deleteNotification(id) },
                                    onClearAll = { viewModel.clearAllNotifications() },
                                    onOpenConversation = { convoId ->
                                        viewModel.selectConversation(convoId)
                                        navigateTopLevel(ChatRoute)
                                    },
                                    onOpenTasks = { navigateTopLevel(TasksRoute) }
                                )
                            }

                            composable<StatsRoute> {
                                StatsScreen(
                                    stats = dashboardStats,
                                    actionLogs = actionLogs,
                                    selectedModel = settings.selectedModel,
                                    monthlyBudgetUsd = settings.monthlyBudgetUsd,
                                    onUpdateMonthlyBudgetUsd = { cap -> viewModel.setMonthlyBudgetUsd(cap) }
                                )
                            }

                            composable<SettingsRoute> {
                                SettingsScreen(
                                    settings = settings,
                                    hasApiKey = hasApiKey,
                                    maskedApiKey = maskedApiKey,
                                    isOnline = isOnline,
                                    connectionTestState = connectionTestState,
                                    onSelectFreeMode = { viewModel.selectFreeConnectionMode() },
                                    onApprovePaidApiMode = { viewModel.approvePaidApiMode() },
                                    onSaveApiKey = { key -> viewModel.saveApiKey(key) },
                                    onClearApiKey = { viewModel.clearApiKey() },
                                    onRevealApiKey = { viewModel.revealApiKeyForEditing() },
                                    onSaveConnectionSettings = { baseUrl, version, model, sysPrompt, maxTok, temp ->
                                        viewModel.saveConnectionSettings(
                                            baseUrl,
                                            version,
                                            model,
                                            sysPrompt,
                                            maxTok,
                                            temp
                                        )
                                    },
                                    onTestConnection = { baseUrl, version, model ->
                                        viewModel.testConnection(baseUrl, version, model)
                                    },
                                    onToggleFloatingBubble = { enable ->
                                        viewModel.toggleFloatingBubble(context, enable)
                                    },
                                    onResetBubblePosition = {
                                        viewModel.resetBubblePosition(context)
                                    },
                                    onSelectResponseLanguage = { lang ->
                                        viewModel.setResponseLanguage(lang)
                                    },
                                    onExportBackupJson = { viewModel.exportBackupJson() },
                                    onRestoreBackupJson = { json -> viewModel.restoreBackupJson(json) },
                                    onStopAllActivity = { viewModel.stopAllActivity(context) },
                                    onClearAllLocalData = { viewModel.clearAllLocalData(context) }
                                )
                            }
                        }

                        InAppNotificationBanner(
                            alert = inAppAlert,
                            onDismiss = { viewModel.dismissAlert() },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }

        if (showSplash) {
            SplashRevealOverlay(
                onFinished = { showSplash = false }
            )
        }
    }
}
