package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.data.model.ReminderRecurrence
import com.example.data.model.TaskEntity
import java.util.Calendar

private const val TAG = "ReminderScheduler"

object ReminderScheduler {

    const val CHANNEL_ID = "ascend_reminders"
    const val CHANNEL_NAME = "Task & Habit Reminders"
    const val CHANNEL_DESCRIPTION = "High-priority reminders for your Ascend tasks and recurring habits."

    const val ACTION_FIRE_REMINDER = "com.example.ascend.ACTION_FIRE_REMINDER"
    const val ACTION_COMPLETE_TASK = "com.example.ascend.ACTION_COMPLETE_TASK"
    const val ACTION_SNOOZE_TASK = "com.example.ascend.ACTION_SNOOZE_TASK"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_IS_SNOOZE = "extra_is_snooze"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    enableLights(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun notificationIdForTask(taskId: Long): Int = (taskId.toInt() and 0x0FFFFFFF) + 10_000

    fun computeNextReminderTimestamp(
        baseTimestamp: Long,
        recurrence: ReminderRecurrence,
        customDays: Set<Int>,
        afterMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (recurrence == ReminderRecurrence.ONE_TIME) {
            return if (baseTimestamp > afterMillis) baseTimestamp else null
        }

        val baseCal = Calendar.getInstance().apply { timeInMillis = baseTimestamp }
        val targetHour = baseCal.get(Calendar.HOUR_OF_DAY)
        val targetMinute = baseCal.get(Calendar.MINUTE)

        val candidate = Calendar.getInstance().apply {
            timeInMillis = maxOf(baseTimestamp, afterMillis)
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (candidate.timeInMillis <= afterMillis) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }

        return when (recurrence) {
            ReminderRecurrence.ONE_TIME -> null
            ReminderRecurrence.DAILY -> candidate.timeInMillis
            ReminderRecurrence.WEEKDAYS -> {
                for (i in 0..7) {
                    val dow = candidate.get(Calendar.DAY_OF_WEEK)
                    if (dow in Calendar.MONDAY..Calendar.FRIDAY && candidate.timeInMillis > afterMillis) {
                        return candidate.timeInMillis
                    }
                    candidate.add(Calendar.DAY_OF_YEAR, 1)
                }
                candidate.timeInMillis
            }
            ReminderRecurrence.WEEKLY -> {
                val weeklyCal = Calendar.getInstance().apply {
                    timeInMillis = baseTimestamp
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                while (weeklyCal.timeInMillis <= afterMillis) {
                    weeklyCal.add(Calendar.WEEK_OF_YEAR, 1)
                }
                weeklyCal.timeInMillis
            }
            ReminderRecurrence.CUSTOM_DAYS -> {
                val validDays = customDays.filter { it in 1..7 }.toSet()
                if (validDays.isEmpty()) {
                    candidate.timeInMillis
                } else {
                    for (i in 0..14) {
                        val isoDay = calendarDayToIsoDay(candidate.get(Calendar.DAY_OF_WEEK))
                        if (isoDay in validDays && candidate.timeInMillis > afterMillis) {
                            return candidate.timeInMillis
                        }
                        candidate.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    candidate.timeInMillis
                }
            }
        }
    }

    fun scheduleTaskReminder(context: Context, task: TaskEntity) {
        if (task.id <= 0L) return
        if (!task.reminderEnabled || task.isCompleted || task.reminderTimestamp == null) {
            cancelTaskReminder(context, task.id)
            return
        }

        ensureNotificationChannel(context)
        val now = System.currentTimeMillis()
        val triggerAt = if (task.reminderTimestamp > now) {
            task.reminderTimestamp
        } else {
            computeNextReminderTimestamp(
                baseTimestamp = task.reminderTimestamp,
                recurrence = task.recurrenceEnum,
                customDays = task.customDaySet,
                afterMillis = now
            )
        }

        if (triggerAt == null) {
            cancelTaskReminder(context, task.id)
            return
        }

        scheduleAlarmInternal(
            context = context,
            taskId = task.id,
            triggerAtMillis = triggerAt,
            isSnooze = false
        )
    }

    fun scheduleSnoozeReminder(context: Context, taskId: Long, delayMinutes: Int = 15) {
        if (taskId <= 0L) return
        ensureNotificationChannel(context)
        val triggerAt = System.currentTimeMillis() + (delayMinutes * 60L * 1000L)
        scheduleAlarmInternal(
            context = context,
            taskId = taskId,
            triggerAtMillis = triggerAt,
            isSnooze = true
        )
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        if (taskId <= 0L) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val pendingIntent = buildAlarmPendingIntent(
            context = context,
            taskId = taskId,
            isSnooze = false,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        try {
            NotificationManagerCompat.from(context).cancel(notificationIdForTask(taskId))
        } catch (_: Exception) {
            // Safe ignore if notification service unavailable
        }
    }

    fun rescheduleAllActiveReminders(context: Context, tasks: List<TaskEntity>) {
        ensureNotificationChannel(context)
        tasks.filter { it.reminderEnabled && !it.isCompleted && it.reminderTimestamp != null }
            .forEach { task -> scheduleTaskReminder(context, task) }
    }

    private fun scheduleAlarmInternal(
        context: Context,
        taskId: Long,
        triggerAtMillis: Long,
        isSnooze: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(
            context = context,
            taskId = taskId,
            isSnooze = isSnooze,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        val canUseExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canUseExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (se: SecurityException) {
            Log.w(TAG, "Exact alarm denied, falling back to inexact idle alarm: ${se.message}")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for task $taskId", e)
        }
    }

    private fun buildAlarmPendingIntent(
        context: Context,
        taskId: Long,
        isSnooze: Boolean,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationIdForTask(taskId),
            intent,
            flags
        )
    }

    /** Converts java.util.Calendar.DAY_OF_WEEK (Sun=1, Mon=2 .. Sat=7) into ISO (Mon=1 .. Sun=7). */
    fun calendarDayToIsoDay(calendarDow: Int): Int {
        return when (calendarDow) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }
}
