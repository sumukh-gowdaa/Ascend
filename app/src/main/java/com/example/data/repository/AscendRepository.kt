package com.example.data.repository

import android.content.Context
import com.example.data.local.AscendDatabase
import com.example.data.model.CoachMessageEntity
import com.example.data.model.DailyActivityEntity
import com.example.data.model.EisenhowerQuadrant
import com.example.data.model.HabitEntity
import com.example.data.model.ReminderRecurrence
import com.example.data.model.RewardEntity
import com.example.data.model.TaskCategory
import com.example.data.model.TaskEntity
import com.example.data.model.UserProgressEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AscendRepository(
    private val context: Context,
    private val database: AscendDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val taskDao = database.taskDao()
    private val habitDao = database.habitDao()
    private val activityDao = database.dailyActivityDao()
    private val coachMessageDao = database.coachMessageDao()
    private val progressDao = database.userProgressDao()
    private val rewardDao = database.rewardDao()
    private val unlockedRewardDao = database.unlockedRewardDao()

    val tasksFlow: Flow<List<TaskEntity>> = taskDao.observeAllTasks()
    val habitsFlow: Flow<List<HabitEntity>> = habitDao.observeAllHabits()
    val activityFlow: Flow<List<DailyActivityEntity>> = activityDao.observeAllActivity()
    val coachMessagesFlow: Flow<List<CoachMessageEntity>> = coachMessageDao.observeAllMessages()
    val progressFlow: Flow<UserProgressEntity> = progressDao.observeProgress().map { it ?: UserProgressEntity() }
    val rewardsFlow: Flow<List<RewardEntity>> = rewardDao.observeAllRewards()
    val preferencesFlow: Flow<UserPreferences> = preferencesRepository.preferencesFlow

    private val backupFile: File
        get() = File(context.filesDir, "ascend_local_backup.json")

    fun hasLocalBackupFile(): Boolean = backupFile.exists() && backupFile.length() > 0L

    suspend fun ensureInitialSeed() {
        val existingProgress = progressDao.getProgressOnce()
        if (existingProgress == null) {
            progressDao.upsertProgress(UserProgressEntity(id = 1, totalXp = 220, lifetimeXpEarned = 220))
        }

        val prefs = preferencesRepository.getPreferencesOnce()
        if (prefs.hasSeededInitialData) {
            // Re-arm any active reminders cleanly on cold start
            val activeReminders = taskDao.getActiveReminderTasksOnce()
            ReminderScheduler.rescheduleAllActiveReminders(context, activeReminders)
            return
        }

        val existingTasks = taskDao.getAllTasksOnce()
        if (existingTasks.isEmpty()) {
            val now = System.currentTimeMillis()
            val oneHour = 60L * 60L * 1000L
            val oneDay = 24L * oneHour

            val starterTasks = listOf(
                TaskEntity(
                    title = "Finish algorithms problem set",
                    notes = "Complete dynamic programming problems 3 & 4 and verify edge cases.",
                    category = TaskCategory.STUDY.label,
                    quadrant = EisenhowerQuadrant.DO_FIRST.name,
                    dueAtMillis = now + 3L * oneHour,
                    isCompleted = false,
                    rescheduleCount = 0,
                    createdAtMillis = now - 5L * oneHour,
                    isSample = true
                ),
                TaskEntity(
                    title = "Outline Q4 career growth roadmap",
                    notes = "Draft key skill milestones and schedule mentor check-in.",
                    category = TaskCategory.GROWTH.label,
                    quadrant = EisenhowerQuadrant.SCHEDULE.name,
                    dueAtMillis = now + oneDay,
                    isCompleted = false,
                    rescheduleCount = 0,
                    createdAtMillis = now - 10L * oneHour,
                    isSample = true
                ),
                TaskEntity(
                    title = "Book annual health checkup",
                    notes = "Call clinic reception or submit portal appointment request.",
                    category = TaskCategory.HEALTH.label,
                    quadrant = EisenhowerQuadrant.DELEGATE.name,
                    dueAtMillis = now - 28L * oneHour,
                    isCompleted = false,
                    rescheduleCount = 2,
                    createdAtMillis = now - 3L * oneDay,
                    isSample = true
                ),
                TaskEntity(
                    title = "Organize cloud receipts & archive",
                    notes = "Sort invoices into folders and tag reimbursable items.",
                    category = TaskCategory.PERSONAL.label,
                    quadrant = EisenhowerQuadrant.ELIMINATE.name,
                    dueAtMillis = now - 50L * oneHour,
                    isCompleted = false,
                    rescheduleCount = 3,
                    createdAtMillis = now - 5L * oneDay,
                    isSample = true
                )
            )
            taskDao.insertTasks(starterTasks)
        }

        val existingActivity = activityDao.getAllActivityOnce()
        if (existingActivity.isEmpty()) {
            val seedHistory = listOf(
                DailyActivityEntity(dayKey = dayKeyOffset(-3), tasksCompleted = 3, habitsCompleted = 1, focusMinutes = 60, goalMet = true),
                DailyActivityEntity(dayKey = dayKeyOffset(-2), tasksCompleted = 4, habitsCompleted = 1, focusMinutes = 80, goalMet = true),
                DailyActivityEntity(dayKey = dayKeyOffset(-1), tasksCompleted = 2, habitsCompleted = 1, focusMinutes = 45, goalMet = true),
                DailyActivityEntity(dayKey = todayDayKey(), tasksCompleted = 1, habitsCompleted = 0, focusMinutes = 25, goalMet = false)
            )
            activityDao.upsertActivities(seedHistory)
        }

        preferencesRepository.markInitialSeeded()
    }

    suspend fun addTask(
        title: String,
        notes: String,
        category: TaskCategory,
        quadrant: EisenhowerQuadrant,
        dueAtMillis: Long,
        reminderEnabled: Boolean = false,
        reminderTimestamp: Long? = null,
        reminderRecurrence: ReminderRecurrence = ReminderRecurrence.ONE_TIME,
        reminderCustomDays: Set<Int> = emptySet(),
        parentTaskId: Long? = null
    ): Long {
        val normalizedTimestamp = if (reminderEnabled) {
            reminderTimestamp ?: dueAtMillis
        } else {
            null
        }
        val task = TaskEntity(
            title = title.trim(),
            notes = notes.trim(),
            category = category.label,
            quadrant = quadrant.name,
            dueAtMillis = dueAtMillis,
            isCompleted = false,
            rescheduleCount = 0,
            createdAtMillis = System.currentTimeMillis(),
            isSample = false,
            parentTaskId = parentTaskId,
            reminderEnabled = reminderEnabled,
            reminderTimestamp = normalizedTimestamp,
            reminderRecurrence = reminderRecurrence.name,
            reminderCustomDays = reminderCustomDays.sorted().joinToString(",")
        )
        val insertedId = taskDao.insertTask(task)
        if (reminderEnabled && normalizedTimestamp != null) {
            ReminderScheduler.scheduleTaskReminder(context, task.copy(id = insertedId))
        }
        return insertedId
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
        if (task.reminderEnabled && !task.isCompleted && task.reminderTimestamp != null) {
            ReminderScheduler.scheduleTaskReminder(context, task)
        } else {
            ReminderScheduler.cancelTaskReminder(context, task.id)
        }
    }

    suspend fun restoreDeletedTask(task: TaskEntity) {
        val restoredId = taskDao.insertTask(task)
        if (task.reminderEnabled && !task.isCompleted && task.reminderTimestamp != null) {
            ReminderScheduler.scheduleTaskReminder(context, task.copy(id = restoredId))
        }
    }

    suspend fun moveTaskQuadrant(task: TaskEntity, targetQuadrant: EisenhowerQuadrant) {
        taskDao.updateTask(task.copy(quadrant = targetQuadrant.name))
    }

    /**
     * Toggles task completion and awards +20 XP on completion (or reverts 20 XP if unchecked).
     * For recurring tasks being completed, schedules the next occurrence automatically.
     * Returns true if the task was completed/advanced.
     */
    suspend fun toggleTaskCompleted(task: TaskEntity, dailyGoal: Int): Boolean {
        val now = System.currentTimeMillis()
        val nowCompleted = !task.isCompleted
        val wasGhost = task.isGhostTask()

        if (nowCompleted && task.reminderEnabled && task.recurrenceEnum != ReminderRecurrence.ONE_TIME && task.reminderTimestamp != null) {
            val nextReminder = ReminderScheduler.computeNextReminderTimestamp(
                baseTimestamp = task.reminderTimestamp,
                recurrence = task.recurrenceEnum,
                customDays = task.customDaySet,
                afterMillis = now + 60_000L
            )
            if (nextReminder != null) {
                val rolledTask = task.copy(
                    dueAtMillis = nextReminder,
                    reminderTimestamp = nextReminder,
                    isCompleted = false,
                    completedAtMillis = now,
                    rescheduleCount = 0
                )
                taskDao.updateTask(rolledTask)
                ReminderScheduler.scheduleTaskReminder(context, rolledTask)
                adjustXp(delta = TASK_COMPLETION_XP)
                if (wasGhost) preferencesRepository.incrementGhostsResolved()
                recordTodayTaskDelta(delta = 1, dailyGoal = dailyGoal)
                return true
            }
        }

        val updated = task.copy(
            isCompleted = nowCompleted,
            completedAtMillis = if (nowCompleted) now else null
        )
        taskDao.updateTask(updated)
        if (nowCompleted) {
            ReminderScheduler.cancelTaskReminder(context, task.id)
        } else if (updated.reminderEnabled && updated.reminderTimestamp != null) {
            ReminderScheduler.scheduleTaskReminder(context, updated)
        }

        adjustXp(delta = if (nowCompleted) TASK_COMPLETION_XP else -TASK_COMPLETION_XP)
        if (nowCompleted && wasGhost) {
            preferencesRepository.incrementGhostsResolved()
        }
        recordTodayTaskDelta(delta = if (nowCompleted) 1 else -1, dailyGoal = dailyGoal)
        return nowCompleted
    }

    /**
     * Invoked directly from the notification shade's "Complete (+20 XP)" action button.
     */
    suspend fun completeTaskFromNotification(taskId: Long) {
        val task = taskDao.getTaskById(taskId) ?: return
        if (task.isCompleted) return
        val prefs = preferencesRepository.getPreferencesOnce()
        toggleTaskCompleted(task, prefs.dailyFocusGoal)
    }

    suspend fun postponeTask(task: TaskEntity, daysForward: Int = 1): TaskEntity {
        val oneDay = 24L * 60L * 60L * 1000L
        val baseTime = maxOf(System.currentTimeMillis(), task.dueAtMillis)
        val nextDue = baseTime + (daysForward * oneDay)
        val updated = task.copy(
            dueAtMillis = nextDue,
            rescheduleCount = task.rescheduleCount + 1,
            reminderTimestamp = if (task.reminderEnabled && task.reminderTimestamp != null) {
                maxOf(System.currentTimeMillis(), task.reminderTimestamp) + (daysForward * oneDay)
            } else {
                task.reminderTimestamp
            }
        )
        taskDao.updateTask(updated)
        if (updated.reminderEnabled && updated.reminderTimestamp != null) {
            ReminderScheduler.scheduleTaskReminder(context, updated)
        }
        return updated
    }

    suspend fun protectFifteenMinutes(task: TaskEntity): TaskEntity {
        val oneDay = 24L * 60L * 60L * 1000L
        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis() + oneDay
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val prefix = "Protected 15-min focus block."
        val updatedNotes = if (task.notes.contains(prefix)) task.notes else {
            if (task.notes.isBlank()) prefix else "$prefix\n${task.notes}"
        }
        val updated = task.copy(
            quadrant = EisenhowerQuadrant.SCHEDULE.name,
            dueAtMillis = cal.timeInMillis,
            notes = updatedNotes,
            rescheduleCount = 0,
            reminderEnabled = true,
            reminderTimestamp = cal.timeInMillis
        )
        taskDao.updateTask(updated)
        ReminderScheduler.scheduleTaskReminder(context, updated)
        preferencesRepository.incrementGhostsResolved()
        return updated
    }

    suspend fun createFiveMinuteStarter(ghostTask: TaskEntity): Long {
        val now = System.currentTimeMillis()
        val twoHours = 2L * 60L * 60L * 1000L
        val starter = TaskEntity(
            title = "5-min start: ${ghostTask.title}",
            notes = "Micro-step to break inertia on \"${ghostTask.title}\". Just open it and do the first 5 minutes.",
            category = ghostTask.category,
            quadrant = EisenhowerQuadrant.DO_FIRST.name,
            dueAtMillis = now + twoHours,
            isCompleted = false,
            rescheduleCount = 0,
            createdAtMillis = now,
            isSample = false,
            parentTaskId = ghostTask.id
        )
        val id = taskDao.insertTask(starter)
        taskDao.updateTask(ghostTask.copy(rescheduleCount = 0, dueAtMillis = now + 24L * 60L * 60L * 1000L))
        preferencesRepository.incrementGhostsResolved()
        return id
    }

    suspend fun dropTask(task: TaskEntity) {
        val wasGhost = task.isGhostTask()
        ReminderScheduler.cancelTaskReminder(context, task.id)
        taskDao.deleteTaskById(task.id)
        if (wasGhost) {
            preferencesRepository.incrementGhostsResolved()
        }
    }

    suspend fun breakDownGhostTask(
        ghostTask: TaskEntity,
        microSteps: List<String>,
        dailyGoal: Int
    ) {
        val cleanSteps = microSteps.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanSteps.isEmpty()) return

        val now = System.currentTimeMillis()
        val sixHours = 6L * 60L * 60L * 1000L
        val createdTasks = cleanSteps.mapIndexed { index, stepTitle ->
            TaskEntity(
                title = stepTitle,
                notes = "Step ${index + 1} from: ${ghostTask.title}",
                category = ghostTask.category,
                quadrant = EisenhowerQuadrant.DO_FIRST.name,
                dueAtMillis = now + ((index + 1) * sixHours),
                isCompleted = false,
                rescheduleCount = 0,
                createdAtMillis = now,
                isSample = false,
                parentTaskId = ghostTask.id
            )
        }
        taskDao.insertTasks(createdTasks)
        ReminderScheduler.cancelTaskReminder(context, ghostTask.id)
        taskDao.deleteTaskById(ghostTask.id)
        preferencesRepository.incrementGhostsResolved()
        recordTodayTaskDelta(delta = 0, dailyGoal = dailyGoal)
    }

    // -------------------------------------------------------------------------
    // Personalized Reward Zone (Room RewardEntity)
    // -------------------------------------------------------------------------

    suspend fun replacePersonalizedRewards(rewards: List<RewardEntity>) {
        rewardDao.deleteAllRewards()
        if (rewards.isNotEmpty()) {
            rewardDao.insertRewards(rewards)
        }
        preferencesRepository.setRewardCounselingCompleted(true)
    }

    suspend fun upsertReward(reward: RewardEntity): Long {
        return if (reward.id > 0L) {
            rewardDao.updateReward(reward)
            reward.id
        } else {
            rewardDao.insertReward(reward)
        }
    }

    suspend fun deleteReward(rewardId: Long) {
        rewardDao.deleteRewardById(rewardId)
    }

    suspend fun redeemReward(reward: RewardEntity): Result<RewardEntity> {
        val current = progressDao.getProgressOnce() ?: UserProgressEntity()
        if (current.totalXp < reward.xpCost) {
            val needed = reward.xpCost - current.totalXp
            return Result.failure(IllegalStateException("Need $needed more XP to unlock ${reward.title}"))
        }
        val updatedProgress = current.copy(totalXp = (current.totalXp - reward.xpCost).coerceAtLeast(0))
        progressDao.upsertProgress(updatedProgress)

        val updatedReward = reward.copy(
            redemptionCount = reward.redemptionCount + 1,
            lastUnlockedAtMillis = System.currentTimeMillis()
        )
        rewardDao.updateReward(updatedReward)
        return Result.success(updatedReward)
    }

    private suspend fun adjustXp(delta: Int) {
        val current = progressDao.getProgressOnce() ?: UserProgressEntity()
        val nextTotal = (current.totalXp + delta).coerceAtLeast(0)
        val nextLifetime = if (delta > 0) current.lifetimeXpEarned + delta else current.lifetimeXpEarned
        progressDao.upsertProgress(
            current.copy(
                totalXp = nextTotal,
                lifetimeXpEarned = nextLifetime
            )
        )
    }

    suspend fun clearSampleData() {
        val allTasks = taskDao.getAllTasksOnce()
        allTasks.filter { it.isSample }.forEach { ReminderScheduler.cancelTaskReminder(context, it.id) }
        taskDao.deleteSampleTasks()
        habitDao.deleteSampleHabits()
    }

    suspend fun saveCoachMessage(
        isUser: Boolean,
        text: String,
        isError: Boolean = false
    ): Long {
        return coachMessageDao.insertMessage(
            CoachMessageEntity(
                isUser = isUser,
                text = text,
                isError = isError,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearCoachHistory() {
        coachMessageDao.deleteAllMessages()
    }

    suspend fun resetAllData() {
        taskDao.getAllTasksOnce().forEach { ReminderScheduler.cancelTaskReminder(context, it.id) }
        taskDao.deleteAllTasks()
        habitDao.deleteAllHabits()
        activityDao.deleteAllActivity()
        coachMessageDao.deleteAllMessages()
        rewardDao.deleteAllRewards()
        unlockedRewardDao.deleteAllRewards()
        progressDao.upsertProgress(UserProgressEntity(id = 1, totalXp = 0, lifetimeXpEarned = 0))
        preferencesRepository.resetAllPreferences()
        withContext(Dispatchers.IO) {
            if (backupFile.exists()) backupFile.delete()
        }
    }

    suspend fun updateDisplayName(name: String) = preferencesRepository.updateDisplayName(name)
    suspend fun completeWelcome(displayName: String?) = preferencesRepository.completeWelcome(displayName)
    suspend fun setRewardCounselingCompleted(completed: Boolean) = preferencesRepository.setRewardCounselingCompleted(completed)

    suspend fun createLocalBackup(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val tasks = taskDao.getAllTasksOnce()
            val activities = activityDao.getAllActivityOnce()
            val coachMessages = coachMessageDao.getAllMessagesOnce()
            val progress = progressDao.getProgressOnce() ?: UserProgressEntity()
            val rewards = rewardDao.getAllRewardsOnce()
            val prefs = preferencesRepository.getPreferencesOnce()

            val root = JSONObject().apply {
                put("version", 4)
                put("exportedAtMillis", System.currentTimeMillis())
                put("preferences", JSONObject().apply {
                    put("displayName", prefs.displayName)
                    put("dailyFocusGoal", prefs.dailyFocusGoal)
                    put("focusSessionMinutes", prefs.focusSessionMinutes)
                    put("ghostsResolvedCount", prefs.ghostsResolvedCount)
                    put("hasCompletedRewardCounseling", prefs.hasCompletedRewardCounseling)
                })
                put("progress", JSONObject().apply {
                    put("totalXp", progress.totalXp)
                    put("lifetimeXpEarned", progress.lifetimeXpEarned)
                })
                put("tasks", JSONArray().apply {
                    tasks.forEach { t ->
                        put(JSONObject().apply {
                            put("title", t.title)
                            put("notes", t.notes)
                            put("category", t.category)
                            put("quadrant", t.quadrant)
                            put("dueAtMillis", t.dueAtMillis)
                            put("isCompleted", t.isCompleted)
                            put("completedAtMillis", t.completedAtMillis ?: JSONObject.NULL)
                            put("rescheduleCount", t.rescheduleCount)
                            put("createdAtMillis", t.createdAtMillis)
                            put("isSample", t.isSample)
                            put("reminderEnabled", t.reminderEnabled)
                            put("reminderTimestamp", t.reminderTimestamp ?: JSONObject.NULL)
                            put("reminderRecurrence", t.reminderRecurrence)
                            put("reminderCustomDays", t.reminderCustomDays)
                        })
                    }
                })
                put("activities", JSONArray().apply {
                    activities.forEach { a ->
                        put(JSONObject().apply {
                            put("dayKey", a.dayKey)
                            put("tasksCompleted", a.tasksCompleted)
                            put("habitsCompleted", a.habitsCompleted)
                            put("focusMinutes", a.focusMinutes)
                            put("goalMet", a.goalMet)
                        })
                    }
                })
                put("rewards", JSONArray().apply {
                    rewards.forEach { r ->
                        put(JSONObject().apply {
                            put("title", r.title)
                            put("description", r.description)
                            put("emoji", r.emoji)
                            put("xpCost", r.xpCost)
                            put("category", r.category)
                            put("redemptionCount", r.redemptionCount)
                            put("lastUnlockedAtMillis", r.lastUnlockedAtMillis ?: JSONObject.NULL)
                            put("isCustom", r.isCustom)
                            put("createdAtMillis", r.createdAtMillis)
                        })
                    }
                })
                put("coachMessages", JSONArray().apply {
                    coachMessages.forEach { m ->
                        put(JSONObject().apply {
                            put("isUser", m.isUser)
                            put("text", m.text)
                            put("isError", m.isError)
                            put("createdAtMillis", m.createdAtMillis)
                        })
                    }
                })
            }

            val jsonText = root.toString(2)
            backupFile.writeText(jsonText)
            preferencesRepository.updateLastBackupTimestamp(System.currentTimeMillis())
            jsonText
        }
    }

    suspend fun restoreLocalBackup(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (!backupFile.exists()) {
                throw IllegalStateException("No local backup file found yet.")
            }
            val json = JSONObject(backupFile.readText())
            val tasksArray = json.optJSONArray("tasks") ?: JSONArray()
            val activitiesArray = json.optJSONArray("activities") ?: JSONArray()
            val rewardsArray = json.optJSONArray("rewards") ?: JSONArray()
            val coachMessagesArray = json.optJSONArray("coachMessages") ?: JSONArray()

            val tasks = (0 until tasksArray.length()).map { i ->
                val obj = tasksArray.getJSONObject(i)
                TaskEntity(
                    title = obj.getString("title"),
                    notes = obj.optString("notes", ""),
                    category = obj.optString("category", TaskCategory.PERSONAL.label),
                    quadrant = obj.optString("quadrant", EisenhowerQuadrant.DO_FIRST.name),
                    dueAtMillis = obj.optLong("dueAtMillis", System.currentTimeMillis()),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    completedAtMillis = if (obj.isNull("completedAtMillis")) null else obj.optLong("completedAtMillis"),
                    rescheduleCount = obj.optInt("rescheduleCount", 0),
                    createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                    isSample = obj.optBoolean("isSample", false),
                    reminderEnabled = obj.optBoolean("reminderEnabled", false),
                    reminderTimestamp = if (obj.isNull("reminderTimestamp")) null else obj.optLong("reminderTimestamp"),
                    reminderRecurrence = obj.optString("reminderRecurrence", ReminderRecurrence.ONE_TIME.name),
                    reminderCustomDays = obj.optString("reminderCustomDays", "")
                )
            }
            val activities = (0 until activitiesArray.length()).map { i ->
                val obj = activitiesArray.getJSONObject(i)
                DailyActivityEntity(
                    dayKey = obj.getString("dayKey"),
                    tasksCompleted = obj.optInt("tasksCompleted", 0),
                    habitsCompleted = obj.optInt("habitsCompleted", 0),
                    focusMinutes = obj.optInt("focusMinutes", 0),
                    goalMet = obj.optBoolean("goalMet", false)
                )
            }
            val rewards = (0 until rewardsArray.length()).map { i ->
                val obj = rewardsArray.getJSONObject(i)
                RewardEntity(
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    emoji = obj.optString("emoji", "🎁"),
                    xpCost = obj.optInt("xpCost", 150),
                    category = obj.optString("category", "Recharge"),
                    redemptionCount = obj.optInt("redemptionCount", 0),
                    lastUnlockedAtMillis = if (obj.isNull("lastUnlockedAtMillis")) null else obj.optLong("lastUnlockedAtMillis"),
                    isCustom = obj.optBoolean("isCustom", false),
                    createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                )
            }
            val coachMessages = (0 until coachMessagesArray.length()).map { i ->
                val obj = coachMessagesArray.getJSONObject(i)
                CoachMessageEntity(
                    isUser = obj.optBoolean("isUser", false),
                    text = obj.optString("text", ""),
                    isError = obj.optBoolean("isError", false),
                    createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                )
            }

            taskDao.getAllTasksOnce().forEach { ReminderScheduler.cancelTaskReminder(context, it.id) }
            taskDao.deleteAllTasks()
            activityDao.deleteAllActivity()
            rewardDao.deleteAllRewards()
            coachMessageDao.deleteAllMessages()

            if (tasks.isNotEmpty()) taskDao.insertTasks(tasks)
            if (activities.isNotEmpty()) activityDao.upsertActivities(activities)
            if (rewards.isNotEmpty()) rewardDao.insertRewards(rewards)
            if (coachMessages.isNotEmpty()) coachMessageDao.insertMessages(coachMessages)

            ReminderScheduler.rescheduleAllActiveReminders(context, taskDao.getActiveReminderTasksOnce())

            json.optJSONObject("progress")?.let { p ->
                progressDao.upsertProgress(
                    UserProgressEntity(
                        id = 1,
                        totalXp = p.optInt("totalXp", 0),
                        lifetimeXpEarned = p.optInt("lifetimeXpEarned", 0)
                    )
                )
            }

            json.optJSONObject("preferences")?.let { p ->
                preferencesRepository.updateDisplayName(p.optString("displayName", "Explorer"))
                preferencesRepository.updateDailyFocusGoal(p.optInt("dailyFocusGoal", 5))
                preferencesRepository.updateFocusSessionMinutes(p.optInt("focusSessionMinutes", 25))
                if (p.has("hasCompletedRewardCounseling")) {
                    preferencesRepository.setRewardCounselingCompleted(p.optBoolean("hasCompletedRewardCounseling", rewards.isNotEmpty()))
                }
            }

            tasks.size + rewards.size
        }
    }

    private suspend fun recordTodayTaskDelta(delta: Int, dailyGoal: Int) {
        val today = todayDayKey()
        val current = activityDao.getByDayKey(today) ?: DailyActivityEntity(dayKey = today)
        val newCount = (current.tasksCompleted + delta).coerceAtLeast(0)
        val newMinutes = (current.focusMinutes + if (delta > 0) 20 else 0).coerceAtLeast(0)
        val goalMet = newCount >= dailyGoal
        activityDao.upsertActivity(
            current.copy(
                tasksCompleted = newCount,
                focusMinutes = newMinutes,
                goalMet = goalMet
            )
        )
    }

    companion object {
        const val TASK_COMPLETION_XP = 20

        fun todayDayKey(): String = dayKeyOffset(0)

        fun dayKeyOffset(daysOffset: Int): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, daysOffset)
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        }

        fun currentWeekDaySlots(): List<Triple<String, String, Boolean>> {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayKey = todayDayKey()
            val cal = Calendar.getInstance()
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val offsetToMonday = if (dow == Calendar.SUNDAY) -6 else (Calendar.MONDAY - dow)
            cal.add(Calendar.DAY_OF_YEAR, offsetToMonday)

            val labels = listOf("M", "T", "W", "T", "F", "S", "S")
            return labels.mapIndexed { idx, letter ->
                val slotCal = (cal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, idx)
                }
                val key = fmt.format(slotCal.time)
                Triple(letter, key, key == todayKey)
            }
        }

        fun formatDueLabel(dueAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
            val todayKey = dayKeyFromMillis(nowMillis)
            val tomorrowKey = dayKeyOffset(1)
            val dueKey = dayKeyFromMillis(dueAtMillis)
            return when {
                dueAtMillis < nowMillis - 24L * 60L * 60L * 1000L -> {
                    val hours = ((nowMillis - dueAtMillis) / (60L * 60L * 1000L)).coerceAtLeast(1L)
                    if (hours >= 48L) "${hours / 24L}d overdue" else "${hours}h overdue"
                }
                dueKey == todayKey -> "Today"
                dueKey == tomorrowKey -> "Tomorrow"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dueAtMillis))
            }
        }

        fun dayKeyFromMillis(millis: Long): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
        }
    }
}
