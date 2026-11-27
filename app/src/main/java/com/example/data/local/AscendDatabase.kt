package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CoachMessageEntity
import com.example.data.model.DailyActivityEntity
import com.example.data.model.HabitEntity
import com.example.data.model.RewardEntity
import com.example.data.model.TaskEntity
import com.example.data.model.UnlockedRewardEntity
import com.example.data.model.UserProgressEntity

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        DailyActivityEntity::class,
        CoachMessageEntity::class,
        UserProgressEntity::class,
        RewardEntity::class,
        UnlockedRewardEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AscendDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun coachMessageDao(): CoachMessageDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun rewardDao(): RewardDao
    abstract fun unlockedRewardDao(): UnlockedRewardDao

    companion object {
        @Volatile
        private var INSTANCE: AscendDatabase? = null

        fun getInstance(context: Context): AscendDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AscendDatabase::class.java,
                    "ascend_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
