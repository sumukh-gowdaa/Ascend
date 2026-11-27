package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.AscendCoachService
import com.example.ai.configureAppCheck
import com.example.auth.GoogleAccountInfo
import com.example.auth.getSafeCurrentGoogleAccount
import com.example.auth.isFirebaseConfigured
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutGoogle
import com.example.auth.toGoogleAccountInfo
import com.example.data.local.AscendDatabase
import com.example.data.model.EisenhowerQuadrant
import com.example.data.model.TaskEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AscendRepository
import com.example.mcp.GoogleWorkspaceAuthHelper
import com.example.mcp.McpConnectorType
import com.example.notifications.ReminderScheduler
import com.example.ui.components.MoveQuadrantSheet
import com.example.ui.components.TaskEditorSheet
import com.example.ui.screens.CoachScreen
import com.example.ui.screens.GhostScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.McpConnectionsScreen
import com.example.ui.screens.PrioritiesScreen
import com.example.ui.screens.RewardCounselingScreen
import com.example.ui.screens.RewardsScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.AscendTheme
import com.example.ui.theme.AscendThemeTokens
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.AscendUiState
import com.example.ui.viewmodel.AscendViewModel
import com.example.ui.viewmodel.AscendViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

enum class AscendDestination(
    val route: String,
    val title: String,
    val shortLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    OVERVIEW("overview", "Overview", "Overview", Icons.Rounded.Home, Icons.Outlined.Home),
    PRIORITIES("priorities", "My priorities", "Priorities", Icons.Rounded.GridView, Icons.Outlined.GridView),
    GHOST("ghost", "Ghost tasks", "Ghost", Icons.Rounded.AutoAwesomeMotion, Icons.Outlined.AutoAwesomeMotion),
    REWARDS("rewards", "Reward zone", "Rewards", Icons.Rounded.EmojiEvents, Icons.Outlined.EmojiEvents),
    COACH("coach", "AI life coach", "Coach", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
    MCP("mcp", "MCP connections", "MCP", Icons.Rounded.Hub, Icons.Outlined.Hub)
}

private val CompactBottomBarDestinations = listOf(
    AscendDestination.OVERVIEW,
    AscendDestination.PRIORITIES,
    AscendDestination.GHOST,
    AscendDestination.REWARDS,
    AscendDestination.COACH
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)
        ReminderScheduler.ensureNotificationChannel(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val database = AscendDatabase.getInstance(applicationContext)
        val prefsRepository = UserPreferencesRepository(applicationContext)
        val repository = AscendRepository(applicationContext, database, prefsRepository)
        val coachService = AscendCoachService()
        val viewModelFactory = AscendViewModelFactory(repository, coachService)

        setContent {
            AscendTheme {
                val viewModel: AscendViewModel = viewModel(factory = viewModelFactory)
                AscendRootApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AscendRootApp(viewModel: AscendViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mcpState by viewModel.mcpState.collectAsStateWithLifecycle()
    val coachState by viewModel.coachState.collectAsStateWithLifecycle()
    val rewardCounselingAiState by viewModel.rewardCounselingAiState.collectAsStateWithLifecycle()
    val breakdownState by viewModel.ghostBreakdownState.collectAsStateWithLifecycle()
    val snackbarEvent by viewModel.snackbarEvent.collectAsStateWithLifecycle()
    val exportedJson by viewModel.lastExportedJson.collectAsStateWithLifecycle()

    var googleAccount by remember { mutableStateOf(getSafeCurrentGoogleAccount(context)) }
    var isAuthLoading by remember { mutableStateOf(false) }
    var authStatusMessage by remember { mutableStateOf<String?>(null) }

    var pendingOAuthConnector by rememberSaveable { mutableStateOf<McpConnectorType?>(null) }

    val workspaceAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        val connector = pendingOAuthConnector ?: return@rememberLauncherForActivityResult
        pendingOAuthConnector = null
        if (activityResult.resultCode == Activity.RESULT_OK) {
            GoogleWorkspaceAuthHelper.parseAuthorizationIntent(
                context = context,
                connector = connector,
                data = activityResult.data,
                onAccessTokenReady = { token ->
                    viewModel.onConnectorTokenReceived(connector, token)
                },
                onFailure = { errorMessage ->
                    viewModel.onConnectorAuthFailed(connector, errorMessage)
                }
            )
        } else {
            viewModel.onConnectorAuthFailed(
                connector,
                "${connector.displayName} OAuth consent was cancelled. Your offline workspace remains active."
            )
        }
    }

    val triggerConnectorAuth: (McpConnectorType) -> Unit = { connector ->
        viewModel.onConnectorAuthStarted(connector)
        GoogleWorkspaceAuthHelper.requestScopeAuthorization(
            context = context,
            connector = connector,
            onResolutionRequired = { intentSenderRequest ->
                pendingOAuthConnector = connector
                workspaceAuthLauncher.launch(intentSenderRequest)
            },
            onAccessTokenReady = { token ->
                viewModel.onConnectorTokenReceived(connector, token)
            },
            onFailure = { errorMessage ->
                viewModel.onConnectorAuthFailed(connector, errorMessage)
            }
        )
    }

    DisposableEffect(context) {
        if (!isFirebaseConfigured(context)) {
            onDispose { }
        } else {
            val auth = Firebase.auth
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                googleAccount = firebaseAuth.currentUser?.toGoogleAccountInfo()
            }
            auth.addAuthStateListener(listener)
            onDispose { auth.removeAuthStateListener(listener) }
        }
    }

    val triggerGoogleSignIn: () -> Unit = {
        isAuthLoading = true
        authStatusMessage = null
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = { account ->
                isAuthLoading = false
                googleAccount = account
                authStatusMessage = "Signed in as ${account.displayName} (${account.email.ifBlank { "Google account linked" }})."
                viewModel.onGoogleAccountLinked(account.displayName)
            },
            onAuthCancelled = { status ->
                isAuthLoading = false
                authStatusMessage = status
            },
            onAuthError = { status ->
                isAuthLoading = false
                authStatusMessage = status
            }
        )
    }

    val triggerGoogleSignOut: () -> Unit = {
        isAuthLoading = true
        signOutGoogle(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onSignOutComplete = { status ->
                isAuthLoading = false
                googleAccount = null
                authStatusMessage = status
                viewModel.disconnectAllWorkspaceConnectors()
            }
        )
    }

    var currentDestination by rememberSaveable { mutableStateOf(AscendDestination.OVERVIEW) }
    var skippedLaunchCounseling by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var showTaskEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var defaultEditorQuadrant by remember { mutableStateOf(EisenhowerQuadrant.DO_FIRST) }
    var movingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarEvent) {
        val event = snackbarEvent ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = event.message,
            actionLabel = if (event.undoTask != null) "Undo" else null,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed && event.undoTask != null) {
            viewModel.undoDropTask(event.undoTask)
        }
        viewModel.clearSnackbar()
    }

    if (!uiState.isLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    if (!uiState.preferences.hasCompletedWelcome) {
        WelcomeScreen(
            initialName = uiState.preferences.displayName,
            isAuthLoading = isAuthLoading,
            authStatusMessage = authStatusMessage,
            onStartOffline = { chosenName ->
                viewModel.completeWelcome(chosenName)
            },
            onGoogleSignIn = triggerGoogleSignIn
        )
        return
    }

    if (!uiState.preferences.hasCompletedRewardCounseling && !skippedLaunchCounseling) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.TopCenter
            ) {
                RewardCounselingScreen(
                    canCancel = false,
                    showSkipForNow = true,
                    aiState = rewardCounselingAiState,
                    onCancel = { skippedLaunchCounseling = true },
                    onGenerateWithGemini = { activities, cadence, treat ->
                        viewModel.generateAndSaveAiRewards(activities, cadence, treat) {
                            skippedLaunchCounseling = true
                        }
                    },
                    onSaveInstantOffline = { rewards ->
                        viewModel.savePersonalizedRewards(rewards)
                        skippedLaunchCounseling = true
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 780.dp)
                )
            }
        }
        return
    }

    if (drawerState.isOpen) {
        BackHandler {
            scope.launch { drawerState.close() }
        }
    } else if (currentDestination != AscendDestination.OVERVIEW) {
        BackHandler {
            currentDestination = AscendDestination.OVERVIEW
        }
    }

    val openNewTaskModal: (EisenhowerQuadrant) -> Unit = { quadrant ->
        editingTask = null
        defaultEditorQuadrant = quadrant
        showTaskEditor = true
    }

    val tokens = AscendThemeTokens.colors

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AscendDrawerContent(
                uiState = uiState,
                googleAccount = googleAccount,
                currentDestination = currentDestination,
                onSelectDestination = { dest ->
                    currentDestination = dest
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    Column {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("drawer_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Menu,
                                        contentDescription = "Open navigation menu",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = currentDestination.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            actions = {
                                Surface(
                                    onClick = { openNewTaskModal(EisenhowerQuadrant.DO_FIRST) },
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .testTag("top_new_task_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "New task",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        )
                        HorizontalDivider(thickness = 1.dp, color = tokens.cardBorder)
                    }
                },
                bottomBar = {
                    if (!useRail) {
                        AscendBottomNavigationBar(
                            currentDestination = currentDestination,
                            ghostCount = uiState.ghostTasks.size,
                            onSelectDestination = { currentDestination = it }
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        AscendNavigationRail(
                            currentDestination = currentDestination,
                            ghostCount = uiState.ghostTasks.size,
                            onSelectDestination = { currentDestination = it }
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Crossfade(
                            targetState = currentDestination,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 780.dp),
                            label = "ascend_destination_transition"
                        ) { destination ->
                            when (destination) {
                                AscendDestination.OVERVIEW -> HomeScreen(
                                    uiState = uiState,
                                    onOpenCoach = { currentDestination = AscendDestination.COACH },
                                    onOpenPriorities = { currentDestination = AscendDestination.PRIORITIES },
                                    onOpenGhost = { currentDestination = AscendDestination.GHOST },
                                    onOpenMcp = { currentDestination = AscendDestination.MCP },
                                    onOpenRewards = { currentDestination = AscendDestination.REWARDS },
                                    onToggleTask = viewModel::toggleTaskCompleted,
                                    onPostponeTask = { viewModel.postponeTask(it) },
                                    onMoveTask = { movingTask = it },
                                    onEditTask = {
                                        editingTask = it
                                        showTaskEditor = true
                                    },
                                    onDeleteTask = viewModel::dropTask
                                )

                                AscendDestination.PRIORITIES -> PrioritiesScreen(
                                    uiState = uiState,
                                    onAddTaskInQuadrant = openNewTaskModal,
                                    onToggleTask = viewModel::toggleTaskCompleted,
                                    onPostponeTask = { viewModel.postponeTask(it) },
                                    onMoveTask = { movingTask = it },
                                    onEditTask = {
                                        editingTask = it
                                        showTaskEditor = true
                                    },
                                    onDeleteTask = viewModel::dropTask
                                )

                                AscendDestination.GHOST -> GhostScreen(
                                    uiState = uiState,
                                    breakdownState = breakdownState,
                                    onRequestAiBreakdown = viewModel::requestAiGhostBreakdown,
                                    onResetBreakdownState = viewModel::resetGhostBreakdownState,
                                    onConfirmBreakdown = viewModel::applyGhostBreakdown,
                                    onCreateFiveMinuteStep = viewModel::ghostCreateFiveMinuteStep,
                                    onProtectFifteenMinutes = viewModel::ghostProtectFifteenMinutes,
                                    onRevisitLater = viewModel::ghostRevisitLater,
                                    onDropGhost = viewModel::dropTask
                                )

                                AscendDestination.REWARDS -> RewardsScreen(
                                    uiState = uiState,
                                    aiCounselingState = rewardCounselingAiState,
                                    onGenerateAiRewards = viewModel::generateAndSaveAiRewards,
                                    onSavePersonalizedRewards = viewModel::savePersonalizedRewards,
                                    onResetAiCounseling = viewModel::resetAiRewardCounseling,
                                    onUpsertReward = viewModel::upsertCustomReward,
                                    onDeleteReward = viewModel::deleteReward,
                                    onRedeemReward = viewModel::redeemReward
                                )

                                AscendDestination.COACH -> CoachScreen(
                                    workspaceState = uiState,
                                    mcpState = mcpState,
                                    coachState = coachState,
                                    onSendMessage = viewModel::sendCoachMessage,
                                    onClearHistory = viewModel::clearCoachHistory,
                                    onDismissError = viewModel::clearCoachError,
                                    onOpenMcpConnections = { currentDestination = AscendDestination.MCP }
                                )

                                AscendDestination.MCP -> McpConnectionsScreen(
                                    uiState = uiState,
                                    mcpState = mcpState,
                                    exportedJson = exportedJson,
                                    googleAccount = googleAccount,
                                    isAuthLoading = isAuthLoading,
                                    authStatusMessage = authStatusMessage,
                                    onConnectCalendar = { triggerConnectorAuth(McpConnectorType.CALENDAR) },
                                    onDisconnectCalendar = { viewModel.disconnectConnector(McpConnectorType.CALENDAR) },
                                    onRefreshCalendar = viewModel::refreshCalendarEvents,
                                    onConnectGmail = { triggerConnectorAuth(McpConnectorType.GMAIL) },
                                    onDisconnectGmail = { viewModel.disconnectConnector(McpConnectorType.GMAIL) },
                                    onRefreshGmail = viewModel::refreshRecentEmails,
                                    onGoogleSignIn = triggerGoogleSignIn,
                                    onGoogleSignOut = triggerGoogleSignOut,
                                    onSaveDisplayName = viewModel::updateDisplayName,
                                    onClearSampleData = viewModel::clearSampleData,
                                    onCreateLocalBackup = viewModel::createLocalBackup,
                                    onDismissExportedJson = viewModel::dismissExportedJson,
                                    onRestoreLocalBackup = viewModel::restoreLocalBackup,
                                    onResetAllData = viewModel::resetAllData
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTaskEditor) {
        TaskEditorSheet(
            initialTask = editingTask,
            defaultQuadrant = defaultEditorQuadrant,
            onDismiss = {
                showTaskEditor = false
                editingTask = null
            },
            onSave = { title, notes, category, quadrant, dueMillis, reminderEnabled, reminderTimestamp, reminderRecurrence, reminderCustomDays ->
                val existing = editingTask
                if (existing == null) {
                    viewModel.addTask(
                        title = title,
                        notes = notes,
                        category = category,
                        quadrant = quadrant,
                        dueAtMillis = dueMillis,
                        reminderEnabled = reminderEnabled,
                        reminderTimestamp = reminderTimestamp,
                        reminderRecurrence = reminderRecurrence,
                        reminderCustomDays = reminderCustomDays
                    )
                } else {
                    viewModel.updateTask(
                        existing.copy(
                            title = title,
                            notes = notes,
                            category = category.label,
                            quadrant = quadrant.name,
                            dueAtMillis = dueMillis,
                            reminderEnabled = reminderEnabled,
                            reminderTimestamp = reminderTimestamp,
                            reminderRecurrence = reminderRecurrence.name,
                            reminderCustomDays = reminderCustomDays.sorted().joinToString(",")
                        )
                    )
                }
                showTaskEditor = false
                editingTask = null
            }
        )
    }

    movingTask?.let { taskToMove ->
        MoveQuadrantSheet(
            task = taskToMove,
            onDismiss = { movingTask = null },
            onSelectQuadrant = { newQuadrant ->
                viewModel.moveTaskQuadrant(taskToMove, newQuadrant)
                movingTask = null
            }
        )
    }
}

@Composable
private fun AscendDrawerContent(
    uiState: AscendUiState,
    googleAccount: GoogleAccountInfo?,
    currentDestination: AscendDestination,
    onSelectDestination: (AscendDestination) -> Unit
) {
    val tokens = AscendThemeTokens.colors
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (googleAccount?.displayName ?: uiState.preferences.displayName)
                            .firstOrNull()?.uppercase() ?: "A",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ascend",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Level ${uiState.level} · ${uiState.levelTitle} · ${uiState.totalXp} XP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = tokens.cardBorder)
            Spacer(modifier = Modifier.height(6.dp))

            AscendDestination.entries.forEach { dest ->
                val selected = currentDestination == dest
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = dest.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = selected,
                    onClick = { onSelectDestination(dest) },
                    icon = {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = dest.title
                        )
                    },
                    badge = if (dest == AscendDestination.GHOST && uiState.ghostTasks.isNotEmpty()) {
                        {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = tokens.softRedBg,
                                border = BorderStroke(1.dp, tokens.softRed.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${uiState.ghostTasks.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = tokens.softRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    } else {
                        null
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("drawer_item_${dest.route}")
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = tokens.surfaceElevated,
                border = BorderStroke(1.dp, tokens.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = googleAccount?.displayName ?: uiState.preferences.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (googleAccount != null) {
                            "Google account linked"
                        } else {
                            "Offline-first Room workspace"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.mint
                    )
                }
            }
        }
    }
}

@Composable
private fun AscendBottomNavigationBar(
    currentDestination: AscendDestination,
    ghostCount: Int,
    onSelectDestination: (AscendDestination) -> Unit
) {
    val tokens = AscendThemeTokens.colors
    Box {
        HorizontalDivider(
            thickness = 1.dp,
            color = tokens.cardBorder
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets.navigationBars
        ) {
            CompactBottomBarDestinations.forEach { dest ->
                val selected = currentDestination == dest
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelectDestination(dest) },
                    modifier = Modifier.testTag("nav_tab_${dest.route}"),
                    icon = {
                        if (dest == AscendDestination.GHOST && ghostCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = tokens.softRed,
                                        contentColor = MaterialTheme.colorScheme.background
                                    ) {
                                        Text("$ghostCount")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.title
                                )
                            }
                        } else {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = dest.title
                            )
                        }
                    },
                    label = {
                        Text(
                            text = dest.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun AscendNavigationRail(
    currentDestination: AscendDestination,
    ghostCount: Int,
    onSelectDestination: (AscendDestination) -> Unit
) {
    val tokens = AscendThemeTokens.colors
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        AscendDestination.entries.forEach { dest ->
            val selected = currentDestination == dest
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectDestination(dest) },
                modifier = Modifier.testTag("nav_rail_${dest.route}"),
                icon = {
                    if (dest == AscendDestination.GHOST && ghostCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = tokens.softRed,
                                    contentColor = MaterialTheme.colorScheme.background
                                ) {
                                    Text("$ghostCount")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = dest.title
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = dest.title
                        )
                    }
                },
                label = {
                    Text(
                        text = dest.shortLabel,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
