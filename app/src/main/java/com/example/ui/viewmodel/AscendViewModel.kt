package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AscendCoachService
import com.example.ai.CoachToolCallbacks
import com.example.ai.CuratedRewardDraft
import com.example.data.model.DailyActivityEntity
import com.example.data.model.EisenhowerQuadrant
import com.example.data.model.ReminderRecurrence
import com.example.data.model.RewardEntity
import com.example.data.model.TaskCategory
import com.example.data.model.TaskEntity
import com.example.data.model.UserProgressEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.AscendRepository
import com.example.mcp.CalendarEventItem
import com.example.mcp.GmailMessageItem
import com.example.mcp.GoogleWorkspaceRestClient
import com.example.mcp.McpConnectorType
import com.example.mcp.McpWorkspaceState
import com.example.mcp.WorkspaceUnauthorizedException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

private const val TAG = "AscendViewModel"

data class WeekDayConsistency(
    val letter: String,
    val dayKey: String,
    val isActive: Boolean,
    val isToday: Boolean
)

data class AscendUiState(
    val isLoaded: Boolean = false,
    val tasks: List<TaskEntity> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val progress: UserProgressEntity = UserProgressEntity(),
    val rewards: List<RewardEntity> = emptyList(),
    val weeklyConsistency: List<WeekDayConsistency> = emptyList(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val ghostTasks: List<TaskEntity> = emptyList(),
    val sampleItemCount: Int = 0,
    val hasLocalBackup: Boolean = false
) {
    val activeTasks: List<TaskEntity>
        get() = tasks.filter { !it.isCompleted }

    val completedTasksCount: Int
        get() = tasks.count { it.isCompleted }

    val pendingTasksCount: Int
        get() = tasks.count { !it.isCompleted }

    val totalXp: Int
        get() = progress.totalXp

    val level: Int
        get() = progress.level

    val levelTitle: String
        get() = progress.levelTitle

    val xpToNextLevel: Int
        get() = progress.xpToNextLevel

    val xpLevelProgressFraction: Float
        get() = (progress.xpIntoCurrentLevel / 100f).coerceIn(0f, 1f)

    val totalRewardRedemptions: Int
        get() = rewards.sumOf { it.redemptionCount }

    fun tasksForQuadrant(quadrant: EisenhowerQuadrant): List<TaskEntity> =
        tasks.filter { it.quadrantEnum == quadrant }

    fun activeTasksForQuadrant(quadrant: EisenhowerQuadrant): List<TaskEntity> =
        tasks.filter { !it.isCompleted && it.quadrantEnum == quadrant }
}

data class CoachMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

sealed interface CoachRequestState {
    data object Idle : CoachRequestState
    data object Loading : CoachRequestState
    data class UsingTool(val statusLabel: String) : CoachRequestState
    data class Streaming(val partialText: String) : CoachRequestState
    data class Error(val message: String) : CoachRequestState
}

data class CoachUiState(
    val messages: List<CoachMessage> = emptyList(),
    val requestState: CoachRequestState = CoachRequestState.Idle
)

data class RewardCounselingAiState(
    val isGenerating: Boolean = false,
    val errorMessage: String? = null
)

sealed interface GhostBreakdownState {
    data object Idle : GhostBreakdownState
    data object Loading : GhostBreakdownState
    data class Ready(val steps: List<String>, val tip: String) : GhostBreakdownState
    data class Error(val message: String) : GhostBreakdownState
}

data class SnackbarEvent(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val undoTask: TaskEntity? = null
)

private data class CoreLocalBundle(
    val tasks: List<TaskEntity>,
    val activities: List<DailyActivityEntity>,
    val progress: UserProgressEntity,
    val rewards: List<RewardEntity>
)

class AscendViewModel(
    private val repository: AscendRepository,
    private val coachService: AscendCoachService,
    private val workspaceRestClient: GoogleWorkspaceRestClient = GoogleWorkspaceRestClient()
) : ViewModel() {

    private val _snackbarEvent = MutableStateFlow<SnackbarEvent?>(null)
    val snackbarEvent: StateFlow<SnackbarEvent?> = _snackbarEvent.asStateFlow()

    private val _lastExportedJson = MutableStateFlow<String?>(null)
    val lastExportedJson: StateFlow<String?> = _lastExportedJson.asStateFlow()

    private val _mcpState = MutableStateFlow(McpWorkspaceState())
    val mcpState: StateFlow<McpWorkspaceState> = _mcpState.asStateFlow()

    private val _rewardCounselingAiState = MutableStateFlow(RewardCounselingAiState())
    val rewardCounselingAiState: StateFlow<RewardCounselingAiState> = _rewardCounselingAiState.asStateFlow()

    // Kept strictly in memory — never persisted to disk or written to logs.
    @Volatile
    private var calendarAccessToken: String? = null

    @Volatile
    private var gmailAccessToken: String? = null

    private val _coachRequestState = MutableStateFlow<CoachRequestState>(CoachRequestState.Idle)

    val coachState: StateFlow<CoachUiState> = combine(
        repository.coachMessagesFlow,
        _coachRequestState
    ) { persistedMessages, requestState ->
        val uiMessages = if (persistedMessages.isEmpty()) {
            listOf(
                CoachMessage(
                    id = -1L,
                    isUser = false,
                    text = "I'm your Ascend AI Life Coach — synced with your Room tasks, XP level, Ghost Tasks, and live Google Calendar / Gmail MCP tools. Pick a prompt below or ask about your schedule, emails, or priorities."
                )
            )
        } else {
            persistedMessages.map { entity ->
                CoachMessage(
                    id = entity.id,
                    isUser = entity.isUser,
                    text = entity.text,
                    isStreaming = false,
                    isError = entity.isError
                )
            }
        }
        val finalMessages = if (requestState is CoachRequestState.Streaming) {
            uiMessages + CoachMessage(
                id = -2L,
                isUser = false,
                text = requestState.partialText,
                isStreaming = true
            )
        } else {
            uiMessages
        }
        CoachUiState(
            messages = finalMessages,
            requestState = requestState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CoachUiState()
    )

    private val _ghostBreakdownState = MutableStateFlow<GhostBreakdownState>(GhostBreakdownState.Idle)
    val ghostBreakdownState: StateFlow<GhostBreakdownState> = _ghostBreakdownState.asStateFlow()

    private val backupTrigger = MutableStateFlow(0)

    private val coreBundleFlow = combine(
        repository.tasksFlow,
        repository.activityFlow,
        repository.progressFlow,
        repository.rewardsFlow
    ) { tasks, activities, progress, rewards ->
        CoreLocalBundle(tasks, activities, progress, rewards)
    }

    val uiState: StateFlow<AscendUiState> = combine(
        coreBundleFlow,
        repository.preferencesFlow,
        backupTrigger
    ) { bundle, prefs, _ ->
        buildUiState(bundle, prefs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AscendUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialSeed()
        }
    }

    // -------------------------------------------------------------------------
    // Reward Zone Counseling & Personalized Rewards (Room + Gemini 3.8 Flash)
    // -------------------------------------------------------------------------

    fun resetAiRewardCounseling() {
        _rewardCounselingAiState.value = RewardCounselingAiState()
    }

    fun generateAndSaveAiRewards(
        rechargeActivities: List<String>,
        rewardCadence: String,
        specificTreat: String,
        onSaved: () -> Unit = {}
    ) {
        if (_rewardCounselingAiState.value.isGenerating) return
        _rewardCounselingAiState.value = RewardCounselingAiState(
            isGenerating = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val userName = uiState.value.preferences.displayName
                val xpHint = when {
                    "100–150" in rewardCadence -> "100–150 XP"
                    "300–400" in rewardCadence -> "300–400 XP"
                    else -> "180–250 XP"
                }
                val drafts = withTimeout(60_000L) {
                    coachService.generateRewardsFromCounselingAnswers(
                        userName = userName,
                        rechargeActivities = rechargeActivities,
                        rewardPacing = rewardCadence,
                        xpRangeHint = xpHint,
                        specificTreat = specificTreat
                    )
                }
                val entities = drafts.map { draft ->
                    RewardEntity(
                        title = draft.title,
                        description = draft.description,
                        emoji = draft.emoji,
                        xpCost = draft.xpCost,
                        category = draft.category,
                        isCustom = false
                    )
                }
                repository.replacePersonalizedRewards(entities)
                _rewardCounselingAiState.value = RewardCounselingAiState()
                showSnackbar("Gemini tailored ${entities.size} personalized rewards for your Reward Zone!")
                onSaved()
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini reward generation timed out", e)
                _rewardCounselingAiState.value = RewardCounselingAiState(
                    isGenerating = false,
                    errorMessage = "AI Error (${e.javaClass.simpleName}): ${e.localizedMessage ?: "Timed out after 60 seconds."}"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini reward generation failed", e)
                _rewardCounselingAiState.value = RewardCounselingAiState(
                    isGenerating = false,
                    errorMessage = "AI Error (${e.javaClass.simpleName}): ${e.localizedMessage ?: "Unable to reach Gemini AI."}"
                )
            }
        }
    }

    fun savePersonalizedRewards(rewards: List<RewardEntity>) {
        if (rewards.isEmpty()) return
        viewModelScope.launch {
            repository.replacePersonalizedRewards(rewards)
            _rewardCounselingAiState.value = RewardCounselingAiState()
            showSnackbar("Saved ${rewards.size} personalized rewards to your Reward Zone!")
        }
    }

    fun upsertCustomReward(reward: RewardEntity) {
        if (reward.title.isBlank()) return
        viewModelScope.launch {
            repository.upsertReward(reward)
            showSnackbar(if (reward.id > 0L) "Updated \"${reward.title}\"" else "Added \"${reward.title}\" to Reward Zone")
        }
    }

    fun deleteReward(reward: RewardEntity) {
        viewModelScope.launch {
            repository.deleteReward(reward.id)
            showSnackbar("Removed \"${reward.title}\"")
        }
    }

    fun redeemReward(reward: RewardEntity) {
        viewModelScope.launch {
            repository.redeemReward(reward)
                .onSuccess {
                    showSnackbar("Unlocked ${reward.emoji} ${reward.title}! (-${reward.xpCost} XP)")
                }
                .onFailure { err ->
                    showSnackbar(err.message ?: "Not enough XP yet")
                }
        }
    }

    // -------------------------------------------------------------------------
    // MCP Google Calendar & Gmail OAuth + REST State
    // -------------------------------------------------------------------------

    fun onConnectorAuthStarted(connector: McpConnectorType) {
        _mcpState.update { state ->
            when (connector) {
                McpConnectorType.CALENDAR -> state.copy(
                    calendarLoading = true,
                    calendarStatusMessage = "Requesting Google Calendar OAuth consent..."
                )

                McpConnectorType.GMAIL -> state.copy(
                    gmailLoading = true,
                    gmailStatusMessage = "Requesting Gmail OAuth consent..."
                )
            }
        }
    }

    fun onConnectorAuthFailed(connector: McpConnectorType, message: String) {
        _mcpState.update { state ->
            when (connector) {
                McpConnectorType.CALENDAR -> state.copy(
                    calendarLoading = false,
                    calendarStatusMessage = message
                )

                McpConnectorType.GMAIL -> state.copy(
                    gmailLoading = false,
                    gmailStatusMessage = message
                )
            }
        }
    }

    fun onConnectorTokenReceived(connector: McpConnectorType, accessToken: String) {
        when (connector) {
            McpConnectorType.CALENDAR -> {
                calendarAccessToken = accessToken
                _mcpState.update {
                    it.copy(
                        calendarConnected = true,
                        calendarLoading = true,
                        calendarStatusMessage = "Authorized. Fetching today & tomorrow's events..."
                    )
                }
                refreshCalendarEvents()
            }

            McpConnectorType.GMAIL -> {
                gmailAccessToken = accessToken
                _mcpState.update {
                    it.copy(
                        gmailConnected = true,
                        gmailLoading = true,
                        gmailStatusMessage = "Authorized. Fetching 5 most recent emails..."
                    )
                }
                refreshRecentEmails()
            }
        }
    }

    fun refreshCalendarEvents() {
        val token = calendarAccessToken
        if (token.isNullOrBlank()) {
            _mcpState.update {
                it.copy(
                    calendarConnected = false,
                    calendarLoading = false,
                    calendarStatusMessage = "Google Calendar is not connected."
                )
            }
            return
        }
        viewModelScope.launch {
            _mcpState.update { it.copy(calendarLoading = true) }
            val result = workspaceRestClient.fetchUpcomingCalendarEvents(token, daysAhead = 2)
            result.onSuccess { events ->
                _mcpState.update {
                    it.copy(
                        calendarConnected = true,
                        calendarLoading = false,
                        calendarEvents = events,
                        lastCalendarSyncMillis = System.currentTimeMillis(),
                        calendarStatusMessage = "Synced ${events.size} upcoming event(s) for today & tomorrow."
                    )
                }
                showSnackbar("Google Calendar connected (${events.size} events)")
            }.onFailure { err ->
                handleWorkspaceFetchError(McpConnectorType.CALENDAR, err)
            }
        }
    }

    fun refreshRecentEmails() {
        val token = gmailAccessToken
        if (token.isNullOrBlank()) {
            _mcpState.update {
                it.copy(
                    gmailConnected = false,
                    gmailLoading = false,
                    gmailStatusMessage = "Gmail is not connected."
                )
            }
            return
        }
        viewModelScope.launch {
            _mcpState.update { it.copy(gmailLoading = true) }
            val result = workspaceRestClient.fetchRecentEmails(token, maxResults = 5)
            result.onSuccess { emails ->
                _mcpState.update {
                    it.copy(
                        gmailConnected = true,
                        gmailLoading = false,
                        recentEmails = emails,
                        lastGmailSyncMillis = System.currentTimeMillis(),
                        gmailStatusMessage = "Synced ${emails.size} most recent Gmail message(s)."
                    )
                }
                showSnackbar("Gmail connected (${emails.size} recent emails)")
            }.onFailure { err ->
                handleWorkspaceFetchError(McpConnectorType.GMAIL, err)
            }
        }
    }

    fun disconnectConnector(connector: McpConnectorType) {
        when (connector) {
            McpConnectorType.CALENDAR -> {
                calendarAccessToken = null
                _mcpState.update {
                    it.copy(
                        calendarConnected = false,
                        calendarLoading = false,
                        calendarEvents = emptyList(),
                        lastCalendarSyncMillis = 0L,
                        calendarStatusMessage = "Disconnected from Google Calendar."
                    )
                }
                showSnackbar("Google Calendar disconnected")
            }

            McpConnectorType.GMAIL -> {
                gmailAccessToken = null
                _mcpState.update {
                    it.copy(
                        gmailConnected = false,
                        gmailLoading = false,
                        recentEmails = emptyList(),
                        lastGmailSyncMillis = 0L,
                        gmailStatusMessage = "Disconnected from Gmail."
                    )
                }
                showSnackbar("Gmail disconnected")
            }
        }
    }

    fun disconnectAllWorkspaceConnectors() {
        calendarAccessToken = null
        gmailAccessToken = null
        _mcpState.value = McpWorkspaceState()
    }

    private fun handleWorkspaceFetchError(connector: McpConnectorType, err: Throwable) {
        Log.e(TAG, "${connector.displayName} API request failed: ${err.javaClass.simpleName}")
        val unauthorized = err is WorkspaceUnauthorizedException
        val message = err.localizedMessage ?: "Unable to fetch ${connector.displayName} data."
        if (unauthorized) {
            when (connector) {
                McpConnectorType.CALENDAR -> calendarAccessToken = null
                McpConnectorType.GMAIL -> gmailAccessToken = null
            }
        }
        _mcpState.update { state ->
            when (connector) {
                McpConnectorType.CALENDAR -> state.copy(
                    calendarConnected = !unauthorized && calendarAccessToken != null,
                    calendarLoading = false,
                    calendarStatusMessage = message
                )

                McpConnectorType.GMAIL -> state.copy(
                    gmailConnected = !unauthorized && gmailAccessToken != null,
                    gmailLoading = false,
                    gmailStatusMessage = message
                )
            }
        }
    }

    private suspend fun executeCalendarTool(daysAhead: Int): JsonObject {
        val token = calendarAccessToken
        if (token.isNullOrBlank()) {
            return buildJsonObject {
                put("connected", JsonPrimitive(false))
                put(
                    "message",
                    JsonPrimitive("Google Calendar is not connected yet. Ask the user to open MCP connections and tap Connect on Google Calendar.")
                )
            }
        }

        val result = workspaceRestClient.fetchUpcomingCalendarEvents(token, daysAhead = daysAhead)
        return result.fold(
            onSuccess = { events ->
                _mcpState.update {
                    it.copy(
                        calendarConnected = true,
                        calendarEvents = events,
                        lastCalendarSyncMillis = System.currentTimeMillis(),
                        calendarStatusMessage = "Synced ${events.size} upcoming event(s) via AI Coach."
                    )
                }
                calendarEventsToJson(events, daysAhead)
            },
            onFailure = { err ->
                handleWorkspaceFetchError(McpConnectorType.CALENDAR, err)
                buildJsonObject {
                    put("connected", JsonPrimitive(calendarAccessToken != null))
                    put("error", JsonPrimitive(err.localizedMessage ?: "Failed to load Google Calendar events."))
                }
            }
        )
    }

    private suspend fun executeGmailTool(maxResults: Int): JsonObject {
        val token = gmailAccessToken
        if (token.isNullOrBlank()) {
            return buildJsonObject {
                put("connected", JsonPrimitive(false))
                put(
                    "message",
                    JsonPrimitive("Gmail is not connected yet. Ask the user to open MCP connections and tap Connect on Gmail.")
                )
            }
        }

        val result = workspaceRestClient.fetchRecentEmails(token, maxResults = maxResults)
        return result.fold(
            onSuccess = { emails ->
                _mcpState.update {
                    it.copy(
                        gmailConnected = true,
                        recentEmails = emails,
                        lastGmailSyncMillis = System.currentTimeMillis(),
                        gmailStatusMessage = "Synced ${emails.size} recent email(s) via AI Coach."
                    )
                }
                gmailMessagesToJson(emails)
            },
            onFailure = { err ->
                handleWorkspaceFetchError(McpConnectorType.GMAIL, err)
                buildJsonObject {
                    put("connected", JsonPrimitive(gmailAccessToken != null))
                    put("error", JsonPrimitive(err.localizedMessage ?: "Failed to load Gmail messages."))
                }
            }
        )
    }

    private fun calendarEventsToJson(events: List<CalendarEventItem>, daysAhead: Int): JsonObject =
        buildJsonObject {
            put("connected", JsonPrimitive(true))
            put("daysAhead", JsonPrimitive(daysAhead))
            put("count", JsonPrimitive(events.size))
            put(
                "events",
                JsonArray(
                    events.map { evt ->
                        buildJsonObject {
                            put("summary", JsonPrimitive(evt.summary))
                            put("start", JsonPrimitive(evt.startTime))
                            put("end", JsonPrimitive(evt.endTime))
                            put("isAllDay", JsonPrimitive(evt.isAllDay))
                            if (evt.location.isNotBlank()) {
                                put("location", JsonPrimitive(evt.location))
                            }
                        }
                    }
                )
            )
        }

    private fun gmailMessagesToJson(emails: List<GmailMessageItem>): JsonObject =
        buildJsonObject {
            put("connected", JsonPrimitive(true))
            put("count", JsonPrimitive(emails.size))
            put(
                "messages",
                JsonArray(
                    emails.map { msg ->
                        buildJsonObject {
                            put("subject", JsonPrimitive(msg.subject))
                            put("from", JsonPrimitive(msg.from))
                            put("date", JsonPrimitive(msg.date))
                            put("snippet", JsonPrimitive(msg.snippet))
                        }
                    }
                )
            )
        }

    // -------------------------------------------------------------------------
    // Core Local State, Tasks, Reminders, Ghosts, XP
    // -------------------------------------------------------------------------

    private fun buildUiState(
        bundle: CoreLocalBundle,
        prefs: UserPreferences
    ): AscendUiState {
        val tasks = bundle.tasks
        val activityMap = bundle.activities.associateBy { it.dayKey }

        val ghostTasks = tasks.filter { it.isGhostTask() }
            .sortedByDescending { it.rescheduleCount }

        val (currentStreak, bestStreak) = calculateStreaks(activityMap)

        val weeklyConsistency = AscendRepository.currentWeekDaySlots().map { (letter, key, isToday) ->
            val act = activityMap[key]
            val active = (act?.tasksCompleted ?: 0) + (act?.habitsCompleted ?: 0) > 0
            WeekDayConsistency(
                letter = letter,
                dayKey = key,
                isActive = active,
                isToday = isToday
            )
        }

        return AscendUiState(
            isLoaded = true,
            tasks = tasks,
            preferences = prefs,
            progress = bundle.progress,
            rewards = bundle.rewards,
            weeklyConsistency = weeklyConsistency,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            ghostTasks = ghostTasks,
            sampleItemCount = tasks.count { it.isSample },
            hasLocalBackup = repository.hasLocalBackupFile()
        )
    }

    private fun calculateStreaks(activityMap: Map<String, DailyActivityEntity>): Pair<Int, Int> {
        val todayEntry = activityMap[AscendRepository.dayKeyOffset(0)]
        val todayHasActivity = ((todayEntry?.tasksCompleted ?: 0) + (todayEntry?.habitsCompleted ?: 0)) > 0

        var current = 0
        var offset = if (todayHasActivity) 0 else -1
        while (offset >= -365) {
            val entry = activityMap[AscendRepository.dayKeyOffset(offset)]
            val active = ((entry?.tasksCompleted ?: 0) + (entry?.habitsCompleted ?: 0)) > 0
            if (active) {
                current++
                offset--
            } else {
                break
            }
        }

        var best = current
        var run = 0
        for (i in -60..0) {
            val entry = activityMap[AscendRepository.dayKeyOffset(i)]
            val active = ((entry?.tasksCompleted ?: 0) + (entry?.habitsCompleted ?: 0)) > 0
            if (active) {
                run++
                if (run > best) best = run
            } else {
                run = 0
            }
        }
        return current to best
    }

    fun addTask(
        title: String,
        notes: String,
        category: TaskCategory,
        quadrant: EisenhowerQuadrant,
        dueAtMillis: Long,
        reminderEnabled: Boolean = false,
        reminderTimestamp: Long? = null,
        reminderRecurrence: ReminderRecurrence = ReminderRecurrence.ONE_TIME,
        reminderCustomDays: Set<Int> = emptySet()
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addTask(
                title = title,
                notes = notes,
                category = category,
                quadrant = quadrant,
                dueAtMillis = dueAtMillis,
                reminderEnabled = reminderEnabled,
                reminderTimestamp = reminderTimestamp,
                reminderRecurrence = reminderRecurrence,
                reminderCustomDays = reminderCustomDays
            )
            val suffix = if (reminderEnabled) " · Reminder set" else ""
            showSnackbar("Added to ${quadrant.title}$suffix")
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
            val suffix = if (task.reminderEnabled) " · Reminder updated" else ""
            showSnackbar("Task updated$suffix")
        }
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            val isRecurring = task.reminderEnabled && task.recurrenceEnum != ReminderRecurrence.ONE_TIME
            val nowCompleted = repository.toggleTaskCompleted(task, uiState.value.preferences.dailyFocusGoal)
            if (nowCompleted) {
                if (isRecurring) {
                    showSnackbar("+${AscendRepository.TASK_COMPLETION_XP} XP · Next ${task.recurrenceEnum.shortLabel.lowercase()} reminder scheduled")
                } else {
                    showSnackbar("+${AscendRepository.TASK_COMPLETION_XP} XP · Completed \"${task.title.take(26)}\"")
                }
            } else {
                showSnackbar("Marked active (-${AscendRepository.TASK_COMPLETION_XP} XP)")
            }
        }
    }

    fun postponeTask(task: TaskEntity, daysForward: Int = 1) {
        viewModelScope.launch {
            val updated = repository.postponeTask(task, daysForward)
            val suffix = if (updated.rescheduleCount >= 2) " · Now in Ghost tasks" else ""
            showSnackbar("Postponed (${updated.rescheduleCount}×)$suffix")
        }
    }

    fun moveTaskQuadrant(task: TaskEntity, quadrant: EisenhowerQuadrant) {
        viewModelScope.launch {
            repository.moveTaskQuadrant(task, quadrant)
            showSnackbar("Moved to ${quadrant.title}")
        }
    }

    fun ghostCreateFiveMinuteStep(task: TaskEntity) {
        viewModelScope.launch {
            repository.createFiveMinuteStarter(task)
            showSnackbar("Created 5-minute starter in Do first")
        }
    }

    fun ghostProtectFifteenMinutes(task: TaskEntity) {
        viewModelScope.launch {
            repository.protectFifteenMinutes(task)
            showSnackbar("Protected 15 min tomorrow at 9:30 AM with reminder")
        }
    }

    fun ghostRevisitLater(task: TaskEntity) {
        postponeTask(task, daysForward = 3)
    }

    fun dropTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.dropTask(task)
            _snackbarEvent.value = SnackbarEvent(
                message = "Dropped \"${task.title.take(28)}\"",
                undoTask = task
            )
        }
    }

    fun undoDropTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.restoreDeletedTask(task)
            showSnackbar("Task restored")
        }
    }

    fun applyGhostBreakdown(ghostTask: TaskEntity, steps: List<String>) {
        viewModelScope.launch {
            repository.breakDownGhostTask(ghostTask, steps, uiState.value.preferences.dailyFocusGoal)
            _ghostBreakdownState.value = GhostBreakdownState.Idle
            showSnackbar("Split into ${steps.size} micro-steps in Do first")
        }
    }

    fun requestAiGhostBreakdown(task: TaskEntity) {
        viewModelScope.launch {
            _ghostBreakdownState.value = GhostBreakdownState.Loading
            try {
                val result = withTimeout(60_000L) {
                    coachService.generateGhostBreakdown(task)
                }
                _ghostBreakdownState.value = GhostBreakdownState.Ready(
                    steps = result.microSteps,
                    tip = result.coachingTip
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Ghost breakdown timed out", e)
                _ghostBreakdownState.value = GhostBreakdownState.Error(
                    "AI Error (${e.javaClass.simpleName}): ${e.localizedMessage ?: "Timed out after 60 seconds."}"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ghost breakdown failed", e)
                _ghostBreakdownState.value = GhostBreakdownState.Error(
                    "AI Error (${e.javaClass.simpleName}): ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun resetGhostBreakdownState() {
        _ghostBreakdownState.value = GhostBreakdownState.Idle
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            repository.updateDisplayName(name)
            showSnackbar("Display name saved")
        }
    }

    fun completeWelcome(displayName: String?) {
        viewModelScope.launch {
            repository.completeWelcome(displayName)
        }
    }

    fun onGoogleAccountLinked(googleName: String?) {
        viewModelScope.launch {
            val clean = googleName?.trim()?.takeIf { it.isNotEmpty() }
            repository.completeWelcome(clean)
        }
    }

    fun clearSampleData() {
        viewModelScope.launch {
            repository.clearSampleData()
            showSnackbar("Starter tasks cleared")
        }
    }

    fun createLocalBackup() {
        viewModelScope.launch {
            repository.createLocalBackup()
                .onSuccess { json ->
                    _lastExportedJson.value = json
                    backupTrigger.update { it + 1 }
                    showSnackbar("Local backup saved on device")
                }
                .onFailure { err ->
                    Log.e(TAG, "Backup failed", err)
                    showSnackbar("Backup failed: ${err.localizedMessage ?: "unknown error"}")
                }
        }
    }

    fun dismissExportedJson() {
        _lastExportedJson.value = null
    }

    fun restoreLocalBackup() {
        viewModelScope.launch {
            repository.restoreLocalBackup()
                .onSuccess { count ->
                    backupTrigger.update { it + 1 }
                    showSnackbar("Restored $count items from local backup")
                }
                .onFailure { err ->
                    Log.e(TAG, "Restore failed", err)
                    showSnackbar(err.localizedMessage ?: "Could not restore local backup")
                }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            backupTrigger.update { it + 1 }
            showSnackbar("All local data reset")
        }
    }

    fun sendCoachMessage(prompt: String) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) return

        val snapshot = buildWorkspaceContextSummary()
        val recentHistory = coachState.value.messages
            .filter { !it.isStreaming && !it.isError && it.id > 0L }
            .takeLast(6)
            .map { (if (it.isUser) "User" else "Coach") to it.text }

        viewModelScope.launch {
            repository.saveCoachMessage(isUser = true, text = cleanPrompt)
            _coachRequestState.value = CoachRequestState.Loading

            try {
                val finalReply = withTimeout(60_000L) {
                    coachService.sendCoachingWithTools(
                        userMessage = cleanPrompt,
                        workspaceContext = snapshot,
                        recentHistory = recentHistory,
                        callbacks = CoachToolCallbacks(
                            getCalendarEvents = { daysAhead -> executeCalendarTool(daysAhead) },
                            getRecentEmails = { maxResults -> executeGmailTool(maxResults) },
                            onToolActivity = { status ->
                                _coachRequestState.value = CoachRequestState.UsingTool(status)
                            }
                        )
                    )
                }

                if (finalReply.isNotEmpty()) {
                    repository.saveCoachMessage(isUser = false, text = finalReply)
                    _coachRequestState.value = CoachRequestState.Idle
                } else {
                    val emptyMsg = "AI Error (EmptyResponseException): Model returned an empty response."
                    repository.saveCoachMessage(isUser = false, text = emptyMsg, isError = true)
                    _coachRequestState.value = CoachRequestState.Error(emptyMsg)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Coach request timed out", e)
                val errorText = "AI Error (${e.javaClass.simpleName}): ${e.localizedMessage ?: "Timed out after 60 seconds."}"
                repository.saveCoachMessage(isUser = false, text = errorText, isError = true)
                _coachRequestState.value = CoachRequestState.Error(errorText)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Coach request failed", e)
                val errorText = "AI Error (${e.javaClass.simpleName}): ${e.localizedMessage ?: "Unknown error"}"
                repository.saveCoachMessage(isUser = false, text = errorText, isError = true)
                _coachRequestState.value = CoachRequestState.Error(errorText)
            }
        }
    }

    fun clearCoachHistory() {
        viewModelScope.launch {
            repository.clearCoachHistory()
            _coachRequestState.value = CoachRequestState.Idle
            showSnackbar("Coach conversation cleared")
        }
    }

    fun clearCoachError() {
        _coachRequestState.value = CoachRequestState.Idle
    }

    fun clearSnackbar() {
        _snackbarEvent.value = null
    }

    private fun showSnackbar(message: String) {
        _snackbarEvent.value = SnackbarEvent(message = message)
    }

    private fun buildWorkspaceContextSummary(): String {
        val state = uiState.value
        val mcp = _mcpState.value
        val now = System.currentTimeMillis()
        return buildString {
            appendLine("User: ${state.preferences.displayName}")
            appendLine("Level & XP: Level ${state.level} · ${state.levelTitle} (${state.totalXp} XP, ${state.xpToNextLevel} XP to Level ${state.level + 1})")
            appendLine("Current Streak: ${state.currentStreak} days (Best: ${state.bestStreak} days)")
            appendLine("Tasks Summary: ${state.pendingTasksCount} pending, ${state.completedTasksCount} completed")
            appendLine(
                "MCP Live Tools Status: Google Calendar=${if (mcp.calendarConnected) "CONNECTED" else "NOT_CONNECTED"}, " +
                    "Gmail=${if (mcp.gmailConnected) "CONNECTED" else "NOT_CONNECTED"}"
            )
            appendLine()
            appendLine("Active Tasks by Eisenhower Quadrant:")
            EisenhowerQuadrant.entries.forEach { quadrant ->
                val qTasks = state.activeTasksForQuadrant(quadrant)
                appendLine("- ${quadrant.title} (${quadrant.modalLabel}) [${qTasks.size}]:")
                if (qTasks.isEmpty()) {
                    appendLine("    (none)")
                } else {
                    qTasks.take(6).forEach { task ->
                        val due = AscendRepository.formatDueLabel(task.dueAtMillis, now)
                        val postponed = if (task.rescheduleCount > 0) ", postponed ${task.rescheduleCount}×" else ""
                        val reminder = task.reminderBadgeLabel()?.let { ", reminder: $it" } ?: ""
                        appendLine("    * [${task.category}] ${task.title} (Due: $due$postponed$reminder)")
                    }
                }
            }
            appendLine()
            appendLine("Ghost Tasks (postponed >= 2) [${state.ghostTasks.size}]:")
            if (state.ghostTasks.isEmpty()) {
                appendLine("- None")
            } else {
                state.ghostTasks.forEach { g ->
                    appendLine("- [${g.category}] ${g.title} (${g.ghostReason(now)})")
                }
            }
            val nextAffordable = state.rewards.firstOrNull { it.xpCost <= state.totalXp }
            if (nextAffordable != null) {
                appendLine("Affordable Personalized Reward Ready: ${nextAffordable.emoji} ${nextAffordable.title} (${nextAffordable.xpCost} XP)")
            }
        }
    }
}

class AscendViewModelFactory(
    private val repository: AscendRepository,
    private val coachService: AscendCoachService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AscendViewModel::class.java)) {
            return AscendViewModel(repository, coachService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
