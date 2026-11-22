package com.example.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ClaudeClassicApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val app = context.applicationContext as? ClaudeClassicApplication ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = app.container
                val settings = container.settingsDataStore.getCurrentSettings()
                if (settings.autoSendEnabled && !settings.autoSendPaused) {
                    container.autoSendScheduler.scheduleEveryFiveHours(settings.autoSendIntervalHours)
                }

                val schedules = container.repository.getScheduledPromptsSync()
                for (schedule in schedules) {
                    if (schedule.isEnabled && !schedule.isPaused) {
                        container.autoSendScheduler.schedulePrompt(schedule.id, schedule.intervalHours)
                    }
                }

                val now = System.currentTimeMillis()
                val tasks = container.repository.getTasksSync()
                for (task in tasks) {
                    if (!task.isCompleted && task.remindEnabled && task.dueAt > now) {
                        container.autoSendScheduler.scheduleTaskReminder(task.id, task.dueAt)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
