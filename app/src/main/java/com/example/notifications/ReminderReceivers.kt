package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AscendDatabase
import com.example.data.model.ReminderRecurrence
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AscendRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "ReminderReceivers"

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val isSnooze = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_SNOOZE, false)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val database = AscendDatabase.getInstance(appContext)
                val prefsRepo = UserPreferencesRepository(appContext)
                val repository = AscendRepository(appContext, database, prefsRepo)

                when (action) {
                    ReminderScheduler.ACTION_FIRE_REMINDER -> {
                        handleFireReminder(appContext, database, taskId, isSnooze)
                    }

                    ReminderScheduler.ACTION_COMPLETE_TASK -> {
                        NotificationManagerCompat.from(appContext)
                            .cancel(ReminderScheduler.notificationIdForTask(taskId))
                        repository.completeTaskFromNotification(taskId)
                    }

                    ReminderScheduler.ACTION_SNOOZE_TASK -> {
                        NotificationManagerCompat.from(appContext)
                            .cancel(ReminderScheduler.notificationIdForTask(taskId))
                        ReminderScheduler.scheduleSnoozeReminder(appContext, taskId, delayMinutes = 15)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling reminder action=$action taskId=$taskId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleFireReminder(
        context: Context,
        database: AscendDatabase,
        taskId: Long,
        isSnooze: Boolean
    ) {
        val task = database.taskDao().getTaskById(taskId) ?: return
        if (task.isCompleted || !task.reminderEnabled) return

        ReminderScheduler.ensureNotificationChannel(context)

        val canPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (canPost) {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                ReminderScheduler.notificationIdForTask(taskId),
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val completeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderScheduler.ACTION_COMPLETE_TASK
                putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
            }
            val completePendingIntent = PendingIntent.getBroadcast(
                context,
                ReminderScheduler.notificationIdForTask(taskId) + 100_000,
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderScheduler.ACTION_SNOOZE_TASK
                putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                ReminderScheduler.notificationIdForTask(taskId) + 200_000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val subtitle = buildString {
                if (task.notes.isNotBlank()) {
                    append(task.notes.take(100))
                } else {
                    append("${task.quadrantEnum.title} · ${task.category}")
                    if (task.recurrenceEnum != ReminderRecurrence.ONE_TIME) {
                        append(" · ${task.recurrenceEnum.shortLabel}")
                    }
                }
            }

            val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_ascend)
                .setContentTitle(if (isSnooze) "Snoozed: ${task.title}" else task.title)
                .setContentText(subtitle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .addAction(
                    R.drawable.ic_notification_ascend,
                    "Complete (+20 XP)",
                    completePendingIntent
                )
                .addAction(
                    R.drawable.ic_notification_ascend,
                    "Snooze 15m",
                    snoozePendingIntent
                )
                .build()

            try {
                NotificationManagerCompat.from(context)
                    .notify(ReminderScheduler.notificationIdForTask(taskId), notification)
            } catch (se: SecurityException) {
                Log.w(TAG, "Missing POST_NOTIFICATIONS permission at notify time: ${se.message}")
            }
        }

        // If recurring (and not a one-off snooze fire), pre-arm the next occurrence automatically
        if (!isSnooze && task.recurrenceEnum != ReminderRecurrence.ONE_TIME && task.reminderTimestamp != null) {
            val nextMillis = ReminderScheduler.computeNextReminderTimestamp(
                baseTimestamp = task.reminderTimestamp,
                recurrence = task.recurrenceEnum,
                customDays = task.customDaySet,
                afterMillis = System.currentTimeMillis() + 60_000L
            )
            if (nextMillis != null) {
                val updated = task.copy(reminderTimestamp = nextMillis)
                database.taskDao().updateTask(updated)
                ReminderScheduler.scheduleTaskReminder(context, updated)
            }
        }
    }
}

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val allowedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
        if (action !in allowedActions) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val tasks = AscendDatabase.getInstance(appContext)
                    .taskDao()
                    .getActiveReminderTasksOnce()
                ReminderScheduler.rescheduleAllActiveReminders(appContext, tasks)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule reminders on $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
