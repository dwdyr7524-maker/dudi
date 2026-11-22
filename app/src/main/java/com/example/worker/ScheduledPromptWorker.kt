package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ClaudeClassicApplication
import com.example.data.local.ActionType
import com.example.data.local.ConnectionMode
import com.example.data.local.ScheduledPromptEntity
import com.example.data.repository.SendMessageResult
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ScheduledPromptWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ClaudeClassicApplication ?: return Result.failure()
        val container = app.container
        val repo = container.repository
        val settings = container.settingsDataStore.getCurrentSettings()
        val isManualTest = inputData.getBoolean(KEY_MANUAL_TEST, false)
        val scheduleId = inputData.getLong(KEY_SCHEDULE_ID, -1L)
        val now = System.currentTimeMillis()

        // If triggered for a specific ScheduledPromptEntity
        if (scheduleId > 0L) {
            val schedule = repo.getScheduledPromptById(scheduleId) ?: return Result.success()
            if ((!schedule.isEnabled || schedule.isPaused) && !isManualTest) {
                return Result.success()
            }

            if (!isManualTest && !isWithinActiveHours(schedule.activeStartHour, schedule.activeEndHour)) {
                val nextRun = now + TimeUnit.HOURS.toMillis(1)
                repo.updateScheduledPrompt(
                    schedule.copy(
                        nextRunAt = nextRun,
                        lastStatus = "ממתין לשעות הפעילות (${schedule.activeStartHour}:00–${schedule.activeEndHour}:00)"
                    )
                )
                return Result.success()
            }

            // Duplicate-send guard: avoid running twice inside 75% of configured interval
            val minSpacingMs = (TimeUnit.HOURS.toMillis(schedule.intervalHours.toLong()) * 0.75).toLong()
            val slotKey = "${schedule.id}_${now / minSpacingMs.coerceAtLeast(60_000L)}"
            if (!isManualTest && (schedule.lastExecutionKey == slotKey || (schedule.lastRunAt > 0L && now - schedule.lastRunAt < minSpacingMs))) {
                return Result.success()
            }

            return executeForPrompt(
                repo = repo,
                notificationManager = container.notificationManager,
                isPaidMode = settings.isPaidApiReady,
                promptText = schedule.promptText,
                promptTitle = schedule.title,
                targetConversationId = schedule.targetConversationId.takeIf { it > 0L },
                intervalHours = schedule.intervalHours,
                isManualTest = isManualTest,
                onUpdate = { nextRun, status ->
                    repo.updateScheduledPrompt(
                        schedule.copy(
                            lastRunAt = now,
                            nextRunAt = nextRun,
                            lastStatus = status,
                            lastExecutionKey = if (isManualTest) schedule.lastExecutionKey else slotKey
                        )
                    )
                }
            )
        }

        // Legacy / primary global schedule fallback
        if ((!settings.autoSendEnabled || settings.autoSendPaused) && !isManualTest) {
            return Result.success()
        }
        if (!isManualTest && !isWithinActiveHours(settings.activeStartHour, settings.activeEndHour)) {
            val nextRun = now + TimeUnit.HOURS.toMillis(1)
            repo.recordAutoSendRun(now, nextRun, "ממתין לחלון שעות הפעילות")
            return Result.success()
        }
        val minSpacingMs = (TimeUnit.HOURS.toMillis(settings.autoSendIntervalHours.toLong()) * 0.75).toLong()
        if (!isManualTest && settings.lastAutoSendTimestamp > 0L && now - settings.lastAutoSendTimestamp < minSpacingMs) {
            return Result.success()
        }

        return executeForPrompt(
            repo = repo,
            notificationManager = container.notificationManager,
            isPaidMode = settings.isPaidApiReady,
            promptText = settings.autoSendPrompt,
            promptTitle = "תזמון ראשי",
            targetConversationId = settings.autoSendConversationId.takeIf { it > 0L },
            intervalHours = settings.autoSendIntervalHours,
            isManualTest = isManualTest,
            onUpdate = { nextRun, status ->
                repo.recordAutoSendRun(now, nextRun, status)
            }
        )
    }

    private suspend fun executeForPrompt(
        repo: com.example.data.repository.ClaudeRepository,
        notificationManager: com.example.notifications.ClaudeNotificationManager,
        isPaidMode: Boolean,
        promptText: String,
        promptTitle: String,
        targetConversationId: Long?,
        intervalHours: Int,
        isManualTest: Boolean,
        onUpdate: suspend (nextRun: Long, status: String) -> Unit
    ): Result {
        val now = System.currentTimeMillis()
        val nextRun = now + TimeUnit.HOURS.toMillis(intervalHours.toLong())

        // Zero-cost default: Prepare message locally and notify user to open & send in free Claude
        if (!isPaidMode) {
            val (convoId, _) = repo.prepareMessageForFreeClaude(
                conversationId = targetConversationId,
                promptText = promptText,
                isScheduled = true
            )
            repo.logAction(
                actionType = ActionType.REMINDER_FIRED,
                mode = ConnectionMode.FREE_ACCOUNT,
                summary = "הוכנה הודעה והוצגה תזכורת: $promptTitle"
            )
            notificationManager.notifySendReminder(
                conversationId = convoId,
                promptTitle = promptTitle,
                promptPreview = promptText
            )
            onUpdate(nextRun, "תזכורת נשלחה · ההודעה הוכנה לשליחה חינמית")
            return Result.success()
        }

        // Paid API mode (explicitly approved)
        return when (val result = repo.sendMessage(
            conversationId = targetConversationId,
            promptText = promptText,
            isScheduled = true,
            notifyEvenIfForeground = true
        )) {
            is SendMessageResult.Success -> {
                onUpdate(nextRun, "נשלח ב-API · ${result.outputTokens} אסימוני פלט")
                Result.success()
            }
            is SendMessageResult.Failure -> {
                if (result.isTransient && runAttemptCount < 2 && !isManualTest) {
                    onUpdate(nextRun, "תקלה זמנית — מנסה שוב אוטומטית")
                    Result.retry()
                } else {
                    onUpdate(nextRun, "שגיאה: ${result.errorMessage}")
                    Result.failure()
                }
            }
        }
    }

    private fun isWithinActiveHours(startHour: Int, endHour: Int): Boolean {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (startHour <= endHour) {
            currentHour in startHour..endHour
        } else {
            currentHour >= startHour || currentHour <= endHour
        }
    }

    companion object {
        const val KEY_MANUAL_TEST = "key_manual_test"
        const val KEY_SCHEDULE_ID = "key_schedule_id"
    }
}
