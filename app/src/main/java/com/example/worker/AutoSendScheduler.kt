package com.example.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class AutoSendScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun scheduleEveryFiveHours(intervalHours: Int = 5) {
        val safeHours = intervalHours.coerceIn(1, 24).toLong()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<ScheduledPromptWorker>(safeHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(safeHours, TimeUnit.HOURS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun schedulePrompt(scheduleId: Long, intervalHours: Int) {
        val safeHours = intervalHours.coerceIn(1, 24).toLong()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<ScheduledPromptWorker>(safeHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(safeHours, TimeUnit.HOURS)
            .setInputData(workDataOf(ScheduledPromptWorker.KEY_SCHEDULE_ID to scheduleId))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            promptWorkName(scheduleId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelPromptSchedule(scheduleId: Long) {
        workManager.cancelUniqueWork(promptWorkName(scheduleId))
    }

    fun cancelSchedule() {
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
    }

    fun cancelAllWork() {
        workManager.cancelAllWork()
    }

    fun runImmediateVerification(scheduleId: Long = -1L) {
        val request = OneTimeWorkRequestBuilder<ScheduledPromptWorker>()
            .setInputData(
                workDataOf(
                    ScheduledPromptWorker.KEY_MANUAL_TEST to true,
                    ScheduledPromptWorker.KEY_SCHEDULE_ID to scheduleId
                )
            )
            .build()

        workManager.enqueue(request)
    }

    fun scheduleTaskReminder(taskId: Long, dueAtMillis: Long) {
        val delayMs = (dueAtMillis - System.currentTimeMillis()).coerceAtLeast(5_000L)
        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(TaskReminderWorker.KEY_TASK_ID to taskId))
            .build()

        workManager.enqueueUniqueWork(
            taskWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelTaskReminder(taskId: Long) {
        workManager.cancelUniqueWork(taskWorkName(taskId))
    }

    fun observeScheduleState(): Flow<WorkInfo.State?> {
        return workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_PERIODIC_WORK_NAME)
            .map { infos -> infos.firstOrNull()?.state }
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK_NAME = "claude_classic_auto_send_5h"
        private fun promptWorkName(id: Long) = "claude_schedule_prompt_$id"
        private fun taskWorkName(id: Long) = "claude_task_reminder_$id"
    }
}
