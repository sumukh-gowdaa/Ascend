package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TaskCategory(val label: String) {
    STUDY("Study"),
    PERSONAL("Personal"),
    GROWTH("Growth"),
    WORK("Work"),
    HEALTH("Health");

    companion object {
        fun fromLabel(raw: String): TaskCategory {
            return entries.firstOrNull {
                it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true)
            } ?: PERSONAL
        }
    }
}

enum class EisenhowerQuadrant(
    val title: String,
    val subtitle: String,
    val modalLabel: String,
    val shortLabel: String,
    val guidance: String
) {
    DO_FIRST(
        title = "Do first",
        subtitle = "Urgent & Important",
        modalLabel = "Urgent — do first",
        shortLabel = "Do first",
        guidance = "High-impact priorities to tackle right now."
    ),
    SCHEDULE(
        title = "Schedule",
        subtitle = "Not Urgent & Important",
        modalLabel = "Important — schedule",
        shortLabel = "Schedule",
        guidance = "Deep work and meaningful goals to protect time for."
    ),
    DELEGATE(
        title = "Delegate",
        subtitle = "Urgent & Not Important",
        modalLabel = "Urgent — delegate",
        shortLabel = "Delegate",
        guidance = "Time-sensitive admin you can batch or delegate."
    ),
    ELIMINATE(
        title = "Maybe later",
        subtitle = "Low urgency & impact",
        modalLabel = "Maybe later",
        shortLabel = "Maybe later",
        guidance = "Optional ideas to park or let go without guilt."
    );

    companion object {
        fun fromName(name: String): EisenhowerQuadrant {
            return entries.firstOrNull { it.name == name } ?: DO_FIRST
        }
    }
}

enum class ReminderRecurrence(val label: String, val shortLabel: String) {
    ONE_TIME("One-time", "Once"),
    DAILY("Daily", "Daily"),
    WEEKDAYS("Weekdays (Mon–Fri)", "Weekdays"),
    WEEKLY("Weekly", "Weekly"),
    CUSTOM_DAYS("Custom days", "Custom");

    companion object {
        fun fromName(raw: String): ReminderRecurrence {
            return entries.firstOrNull { it.name == raw } ?: ONE_TIME
        }
    }
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val category: String = TaskCategory.PERSONAL.label,
    val quadrant: String = EisenhowerQuadrant.DO_FIRST.name,
    val dueAtMillis: Long,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val rescheduleCount: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isSample: Boolean = false,
    val parentTaskId: Long? = null,
    val reminderEnabled: Boolean = false,
    val reminderTimestamp: Long? = null,
    val reminderRecurrence: String = ReminderRecurrence.ONE_TIME.name,
    /** Comma-separated ISO weekdays (1=Mon .. 7=Sun) when recurrence is CUSTOM_DAYS */
    val reminderCustomDays: String = ""
) {
    val quadrantEnum: EisenhowerQuadrant
        get() = EisenhowerQuadrant.fromName(quadrant)

    val categoryEnum: TaskCategory
        get() = TaskCategory.fromLabel(category)

    val recurrenceEnum: ReminderRecurrence
        get() = ReminderRecurrence.fromName(reminderRecurrence)

    val customDaySet: Set<Int>
        get() = reminderCustomDays
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()

    fun overdueHours(nowMillis: Long = System.currentTimeMillis()): Long {
        if (isCompleted || nowMillis <= dueAtMillis) return 0L
        return (nowMillis - dueAtMillis) / (1000L * 60L * 60L)
    }

    fun isGhostTask(): Boolean = !isCompleted && rescheduleCount >= 2

    fun ghostReason(nowMillis: Long = System.currentTimeMillis()): String {
        val hours = overdueHours(nowMillis)
        return buildList {
            add("Postponed ${rescheduleCount}×")
            if (hours >= 24L) {
                val days = hours / 24L
                if (days >= 2L) add("Overdue ${days}d") else add("Overdue ${hours}h")
            }
        }.joinToString(" · ")
    }

    fun reminderBadgeLabel(): String? {
        if (!reminderEnabled || reminderTimestamp == null) return null
        val timeText = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(reminderTimestamp))
        val recLabel = when (recurrenceEnum) {
            ReminderRecurrence.ONE_TIME -> null
            ReminderRecurrence.CUSTOM_DAYS -> {
                val letters = listOf("M", "T", "W", "T", "F", "S", "S")
                customDaySet.sorted().joinToString(" ") { letters[(it - 1).coerceIn(0, 6)] }
                    .ifBlank { "Custom" }
            }
            else -> recurrenceEnum.shortLabel
        }
        return if (recLabel != null) "$timeText · $recLabel" else timeText
    }
}

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "Focus",
    val lastCompletedDayKey: String = "",
    val totalCompletions: Int = 0,
    val isSample: Boolean = false
)

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val dayKey: String,
    val tasksCompleted: Int = 0,
    val habitsCompleted: Int = 0,
    val focusMinutes: Int = 0,
    val goalMet: Boolean = false
)

@Entity(tableName = "coach_messages")
data class CoachMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isUser: Boolean,
    val text: String,
    val isError: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val id: Int = 1,
    val totalXp: Int = 220,
    val lifetimeXpEarned: Int = 220
) {
    val level: Int
        get() = (totalXp.coerceAtLeast(0) / 100) + 1

    val xpIntoCurrentLevel: Int
        get() = totalXp.coerceAtLeast(0) % 100

    val xpToNextLevel: Int
        get() = 100 - xpIntoCurrentLevel

    val levelTitle: String
        get() = when {
            level >= 10 -> "Luminary"
            level >= 7 -> "Architect"
            level >= 4 -> "Navigator"
            else -> "Explorer"
        }
}

@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val emoji: String = "🎁",
    val xpCost: Int = 150,
    val category: String = "Recharge",
    val redemptionCount: Int = 0,
    val lastUnlockedAtMillis: Long? = null,
    val isCustom: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "unlocked_rewards")
data class UnlockedRewardEntity(
    @PrimaryKey val rewardId: String,
    val title: String,
    val xpCost: Int,
    val redemptionCount: Int = 1,
    val lastUnlockedAtMillis: Long = System.currentTimeMillis()
)
