package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.ascendDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ascend_preferences"
)

data class UserPreferences(
    val displayName: String = "Explorer",
    val dailyFocusGoal: Int = 5,
    val focusSessionMinutes: Int = 25,
    val hasSeededInitialData: Boolean = false,
    val hasCompletedWelcome: Boolean = false,
    val hasCompletedRewardCounseling: Boolean = false,
    val lastBackupTimestamp: Long = 0L,
    val ghostsResolvedCount: Int = 0
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val DAILY_FOCUS_GOAL = intPreferencesKey("daily_focus_goal")
        val FOCUS_SESSION_MINUTES = intPreferencesKey("focus_session_minutes")
        val HAS_SEEDED_INITIAL_DATA = booleanPreferencesKey("has_seeded_initial_data")
        val HAS_COMPLETED_WELCOME = booleanPreferencesKey("has_completed_welcome")
        val HAS_COMPLETED_REWARD_COUNSELING = booleanPreferencesKey("has_completed_reward_counseling")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val GHOSTS_RESOLVED_COUNT = intPreferencesKey("ghosts_resolved_count")
    }

    val preferencesFlow: Flow<UserPreferences> = context.ascendDataStore.data.map { prefs ->
        UserPreferences(
            displayName = prefs[Keys.DISPLAY_NAME]?.takeIf { it.isNotBlank() } ?: "Explorer",
            dailyFocusGoal = (prefs[Keys.DAILY_FOCUS_GOAL] ?: 5).coerceIn(1, 15),
            focusSessionMinutes = prefs[Keys.FOCUS_SESSION_MINUTES] ?: 25,
            hasSeededInitialData = prefs[Keys.HAS_SEEDED_INITIAL_DATA] ?: false,
            hasCompletedWelcome = prefs[Keys.HAS_COMPLETED_WELCOME] ?: false,
            hasCompletedRewardCounseling = prefs[Keys.HAS_COMPLETED_REWARD_COUNSELING] ?: false,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TIMESTAMP] ?: 0L,
            ghostsResolvedCount = prefs[Keys.GHOSTS_RESOLVED_COUNT] ?: 0
        )
    }

    suspend fun getPreferencesOnce(): UserPreferences = preferencesFlow.first()

    suspend fun updateDisplayName(name: String) {
        val clean = name.trim().ifEmpty { "Explorer" }
        context.ascendDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = clean
        }
    }

    suspend fun updateDailyFocusGoal(goal: Int) {
        context.ascendDataStore.edit { prefs ->
            prefs[Keys.DAILY_FOCUS_GOAL] = goal.coerceIn(1, 15)
        }
    }

    suspend fun updateFocusSessionMinutes(minutes: Int) {
        context.ascendDataStore.edit { prefs ->
            prefs[Keys.FOCUS_SESSION_MINUTES] = minutes.coerceIn(10, 90)
        }
    }

    suspend fun markInitialSeeded() {
        context.ascendDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEDED_INITIAL_DATA] = true
        }
    }

    suspend fun completeWelcome(displayName: String?) {
        context.ascendDataStore.edit { prefs ->
            val clean = displayName?.trim()
            if (!clean.isNullOrEmpty()) {
                prefs[Keys.DISPLAY_NAME] = clean
            }
            prefs[Keys.HAS_COMPLETED_WELCOME] = true
        }
    }

    suspend fun setRewardCounselingCompleted(completed: Boolean) {
        context.ascendDataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_REWARD_COUNSELING] = completed
        }
    }

    suspend fun updateLastBackupTimestamp(timestamp: Long) {
        context.ascendDataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    suspend fun incrementGhostsResolved() {
        context.ascendDataStore.edit { prefs ->
            val current = prefs[Keys.GHOSTS_RESOLVED_COUNT] ?: 0
            prefs[Keys.GHOSTS_RESOLVED_COUNT] = current + 1
        }
    }

    suspend fun resetAllPreferences() {
        context.ascendDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = "Explorer"
            prefs[Keys.DAILY_FOCUS_GOAL] = 5
            prefs[Keys.FOCUS_SESSION_MINUTES] = 25
            prefs[Keys.HAS_SEEDED_INITIAL_DATA] = true
            prefs[Keys.HAS_COMPLETED_WELCOME] = true
            prefs[Keys.HAS_COMPLETED_REWARD_COUNSELING] = false
            prefs[Keys.LAST_BACKUP_TIMESTAMP] = 0L
            prefs[Keys.GHOSTS_RESOLVED_COUNT] = 0
        }
    }
}
