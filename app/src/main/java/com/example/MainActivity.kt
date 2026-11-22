package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.notifications.ClaudeNotificationManager
import com.example.ui.ClaudeConnectApp
import com.example.ui.navigation.ChatRoute
import com.example.ui.navigation.TasksRoute
import com.example.ui.theme.ClaudeClassicTheme
import com.example.ui.viewmodel.ClaudeViewModel

class MainActivity : ComponentActivity() {

    private var intentTargetRoute by mutableStateOf<Any?>(null)

    private val viewModel: ClaudeViewModel by viewModels {
        val container = (application as ClaudeClassicApplication).container
        ClaudeViewModel.Factory(
            repository = container.repository,
            autoSendScheduler = container.autoSendScheduler,
            networkMonitor = container.networkMonitor,
            settingsDataStore = container.settingsDataStore
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        handleNotificationIntent(intent)

        setContent {
            ClaudeClassicTheme {
                ClaudeConnectApp(
                    viewModel = viewModel,
                    intentTargetRoute = intentTargetRoute,
                    onIntentTargetConsumed = { intentTargetRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val conversationId = intent?.getLongExtra(
            ClaudeNotificationManager.EXTRA_CONVERSATION_ID,
            -1L
        ) ?: -1L
        val taskId = intent?.getLongExtra(
            ClaudeNotificationManager.EXTRA_TASK_ID,
            -1L
        ) ?: -1L

        if (conversationId > 0L) {
            viewModel.selectConversation(conversationId)
            intentTargetRoute = ChatRoute
        } else if (taskId > 0L) {
            intentTargetRoute = TasksRoute
        }
    }
}
