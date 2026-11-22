package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ClaudeClassicApplication

class TaskReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ClaudeClassicApplication ?: return Result.failure()
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId <= 0L) return Result.success()

        val task = app.container.repository.getTaskById(taskId) ?: return Result.success()
        if (task.isCompleted || !task.remindEnabled) {
            return Result.success()
        }

        app.container.notificationManager.notifyTaskDue(
            taskId = task.id,
            taskTitle = task.title,
            promptPreview = task.promptText
        )
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "key_task_id"
    }
}
