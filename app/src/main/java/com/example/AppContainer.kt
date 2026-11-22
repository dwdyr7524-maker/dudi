package com.example

import android.content.Context
import com.example.data.local.ClaudeDatabase
import com.example.data.network.NetworkMonitor
import com.example.data.preferences.SettingsDataStore
import com.example.data.remote.AnthropicClientFactory
import com.example.data.repository.ClaudeRepository
import com.example.data.security.SecureApiKeyStore
import com.example.notifications.ClaudeNotificationManager
import com.example.worker.AutoSendScheduler

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: ClaudeDatabase by lazy { ClaudeDatabase.getInstance(appContext) }
    val apiKeyStore: SecureApiKeyStore by lazy { SecureApiKeyStore(appContext) }
    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(appContext) }
    val clientFactory: AnthropicClientFactory by lazy { AnthropicClientFactory() }
    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(appContext) }
    val notificationManager: ClaudeNotificationManager by lazy {
        ClaudeNotificationManager(appContext, database.controlCenterDao()).apply { ensureChannels() }
    }
    val autoSendScheduler: AutoSendScheduler by lazy { AutoSendScheduler(appContext) }

    val repository: ClaudeRepository by lazy {
        ClaudeRepository(
            chatDao = database.chatDao(),
            controlCenterDao = database.controlCenterDao(),
            apiKeyStore = apiKeyStore,
            settingsDataStore = settingsDataStore,
            clientFactory = clientFactory,
            notificationManager = notificationManager
        )
    }
}
