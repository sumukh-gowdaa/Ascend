package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CoachMessageEntity
import com.example.data.model.DailyActivityEntity
import com.example.data.model.HabitEntity
import com.example.data.model.RewardEntity
import com.example.data.model.TaskEntity
import com.example.data.model.UnlockedRewardEntity
import com.example.data.model.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueAtMillis ASC, id DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY id ASC")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE reminderEnabled = 1 AND isCompleted = 0")
    suspend fun getActiveReminderTasksOnce(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks WHERE isSample = 1")
    suspend fun deleteSampleTasks()

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun observeAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY id ASC")
    suspend fun getAllHabitsOnce(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE isSample = 1")
    suspend fun deleteSampleHabits()

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()
}

@Dao
interface DailyActivityDao {
    @Query("SELECT * FROM daily_activity ORDER BY dayKey DESC")
    fun observeAllActivity(): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity ORDER BY dayKey DESC")
    suspend fun getAllActivityOnce(): List<DailyActivityEntity>

    @Query("SELECT * FROM daily_activity WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getByDayKey(dayKey: String): DailyActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivity(activity: DailyActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivities(activities: List<DailyActivityEntity>)

    @Query("DELETE FROM daily_activity")
    suspend fun deleteAllActivity()
}

@Dao
interface CoachMessageDao {
    @Query("SELECT * FROM coach_messages ORDER BY createdAtMillis ASC, id ASC")
    fun observeAllMessages(): Flow<List<CoachMessageEntity>>

    @Query("SELECT * FROM coach_messages ORDER BY createdAtMillis ASC, id ASC")
    suspend fun getAllMessagesOnce(): List<CoachMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CoachMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CoachMessageEntity>)

    @Query("DELETE FROM coach_messages")
    suspend fun deleteAllMessages()
}

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE id = 1 LIMIT 1")
    fun observeProgress(): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_progress WHERE id = 1 LIMIT 1")
    suspend fun getProgressOnce(): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: UserProgressEntity)

    @Query("DELETE FROM user_progress")
    suspend fun deleteAllProgress()
}

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards ORDER BY xpCost ASC, id ASC")
    fun observeAllRewards(): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards ORDER BY xpCost ASC, id ASC")
    suspend fun getAllRewardsOnce(): List<RewardEntity>

    @Query("SELECT * FROM rewards WHERE id = :id LIMIT 1")
    suspend fun getRewardById(id: Long): RewardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(reward: RewardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRewards(rewards: List<RewardEntity>)

    @Update
    suspend fun updateReward(reward: RewardEntity)

    @Query("DELETE FROM rewards WHERE id = :id")
    suspend fun deleteRewardById(id: Long)

    @Query("DELETE FROM rewards")
    suspend fun deleteAllRewards()
}

@Dao
interface UnlockedRewardDao {
    @Query("SELECT * FROM unlocked_rewards ORDER BY lastUnlockedAtMillis DESC")
    fun observeUnlockedRewards(): Flow<List<UnlockedRewardEntity>>

    @Query("SELECT * FROM unlocked_rewards ORDER BY lastUnlockedAtMillis DESC")
    suspend fun getUnlockedRewardsOnce(): List<UnlockedRewardEntity>

    @Query("SELECT * FROM unlocked_rewards WHERE rewardId = :rewardId LIMIT 1")
    suspend fun getById(rewardId: String): UnlockedRewardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReward(reward: UnlockedRewardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRewards(rewards: List<UnlockedRewardEntity>)

    @Query("DELETE FROM unlocked_rewards")
    suspend fun deleteAllRewards()
}
