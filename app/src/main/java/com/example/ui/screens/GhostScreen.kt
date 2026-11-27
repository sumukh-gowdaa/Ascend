package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.TaskEntity
import com.example.ui.components.AscendCard
import com.example.ui.components.CategoryPill
import com.example.ui.components.QuadrantBadge
import com.example.ui.theme.AscendThemeTokens
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.AscendUiState
import com.example.ui.viewmodel.GhostBreakdownState

private enum class GhostReflection {
    TOO_BIG,
    NO_TIME,
    NOT_IMPORTANT
}

@Composable
fun GhostScreen(
    uiState: AscendUiState,
    breakdownState: GhostBreakdownState,
    onRequestAiBreakdown: (TaskEntity) -> Unit,
    onResetBreakdownState: () -> Unit,
    onConfirmBreakdown: (TaskEntity, List<String>) -> Unit,
    onCreateFiveMinuteStep: (TaskEntity) -> Unit,
    onProtectFifteenMinutes: (TaskEntity) -> Unit,
    onRevisitLater: (TaskEntity) -> Unit,
    onDropGhost: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = AscendThemeTokens.colors
    var breakdownTarget by remember { mutableStateOf<TaskEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "ghost_header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "GHOST DETECTOR · POSTPONED 2+ TIMES",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.softRed
                )
                Text(
                    text = "Ghost tasks",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tasks you've postponed twice or more show up here so you can reflect without guilt and pick a clear next move.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Visual Detector Status Banner
        item(key = "ghost_hero_card") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        1.dp,
                        if (uiState.ghostTasks.isNotEmpty()) tokens.softRed.copy(alpha = 0.5f) else tokens.cardBorder,
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ghost_detector_1795614813038),
                    contentDescription = "Ghost tasks visual banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.35f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${uiState.ghostTasks.size}",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (uiState.ghostTasks.isNotEmpty()) tokens.softRed else tokens.mint
                        )
                        Column {
                            Text(
                                text = if (uiState.ghostTasks.isEmpty()) "All clear" else "Ghost tasks detected",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.preferences.ghostsResolvedCount} resolved all-time",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = if (uiState.ghostTasks.isEmpty()) {
                            "No tasks have been postponed 2+ times."
                        } else {
                            "Tap a reflection chip below to unblock or release each task."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (uiState.ghostTasks.isEmpty()) {
            item(key = "ghost_empty_state") {
                AscendCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = tokens.mint.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircleOutline,
                            contentDescription = null,
                            tint = tokens.mint,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "No ghost tasks right now",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Whenever an incomplete task is postponed 2 or more times, it automatically surfaces here for reflection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.ghostTasks, key = { it.id }) { ghost ->
                GhostReflectionCard(
                    task = ghost,
                    onCreateFiveMinuteStep = { onCreateFiveMinuteStep(ghost) },
                    onOpenAiBreakdown = {
                        onResetBreakdownState()
                        breakdownTarget = ghost
                    },
                    onProtectFifteenMinutes = { onProtectFifteenMinutes(ghost) },
                    onDrop = { onDropGhost(ghost) },
                    onRevisitLater = { onRevisitLater(ghost) }
                )
            }
        }

        item(key = "ghost_bottom_spacer") {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    breakdownTarget?.let { target ->
        GhostBreakdownSheet(
            task = target,
            breakdownState = breakdownState,
            onRequestAi = { onRequestAiBreakdown(target) },
            onDismiss = {
                breakdownTarget = null
                onResetBreakdownState()
            },
            onConfirmSteps = { steps ->
                onConfirmBreakdown(target, steps)
                breakdownTarget = null
            }
        )
    }
}

@Composable
private fun GhostReflectionCard(
    task: TaskEntity,
    onCreateFiveMinuteStep: () -> Unit,
    onOpenAiBreakdown: () -> Unit,
    onProtectFifteenMinutes: () -> Unit,
    onDrop: () -> Unit,
    onRevisitLater: () -> Unit
) {
    val tokens = AscendThemeTokens.colors
    var activeReflection by remember(task.id) { mutableStateOf<GhostReflection?>(null) }

    AscendCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ghost_task_card_${task.id}"),
        borderColor = tokens.softRed.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(tokens.softRedBg)
                        .border(1.dp, tokens.softRed.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = tokens.softRed,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = task.ghostReason(),
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.softRed
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryPill(category = task.category)
                    QuadrantBadge(quadrant = task.quadrantEnum)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (task.notes.isNotBlank()) {
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "WHY IS THIS STALLING?",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textMuted
            )

            // 4 Reflection Action Chips (2x2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReflectionChipButton(
                        label = "It's too big",
                        icon = Icons.Rounded.Splitscreen,
                        accent = MaterialTheme.colorScheme.primary,
                        selected = activeReflection == GhostReflection.TOO_BIG,
                        onClick = {
                            activeReflection = if (activeReflection == GhostReflection.TOO_BIG) null else GhostReflection.TOO_BIG
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ghost_chip_too_big_${task.id}")
                    )
                    ReflectionChipButton(
                        label = "No time for it",
                        icon = Icons.Outlined.Schedule,
                        accent = tokens.skyBlue,
                        selected = activeReflection == GhostReflection.NO_TIME,
                        onClick = {
                            activeReflection = if (activeReflection == GhostReflection.NO_TIME) null else GhostReflection.NO_TIME
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ghost_chip_no_time_${task.id}")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReflectionChipButton(
                        label = "Not important now",
                        icon = Icons.Outlined.Archive,
                        accent = tokens.softRed,
                        selected = activeReflection == GhostReflection.NOT_IMPORTANT,
                        onClick = {
                            activeReflection = if (activeReflection == GhostReflection.NOT_IMPORTANT) null else GhostReflection.NOT_IMPORTANT
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ghost_chip_not_important_${task.id}")
                    )
                    ReflectionChipButton(
                        label = "Revisit later",
                        icon = Icons.Outlined.EventRepeat,
                        accent = tokens.gold,
                        selected = false,
                        onClick = onRevisitLater,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ghost_chip_revisit_${task.id}")
                    )
                }
            }

            // Guided Action Panel for the chosen reflection
            AnimatedVisibility(visible = activeReflection != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = tokens.surfaceElevated,
                    border = BorderStroke(1.dp, tokens.cardBorderStrong),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (activeReflection) {
                            GhostReflection.TOO_BIG -> {
                                Text(
                                    text = "Start with a 5-minute first step: open what you need and complete just the very first tiny piece of \"${task.title}\".",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            activeReflection = null
                                            onCreateFiveMinuteStep()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Add 5-min step", maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            activeReflection = null
                                            onOpenAiBreakdown()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("AI Split", maxLines = 1)
                                    }
                                }
                            }

                            GhostReflection.NO_TIME -> {
                                Text(
                                    text = "Protect a dedicated 15-minute block tomorrow at 9:30 AM and move this task to Schedule so it stops competing with today.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Button(
                                    onClick = {
                                        activeReflection = null
                                        onProtectFifteenMinutes()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = tokens.skyBlue,
                                        contentColor = MaterialTheme.colorScheme.background
                                    )
                                ) {
                                    Text("Protect 15 min tomorrow")
                                }
                            }

                            GhostReflection.NOT_IMPORTANT -> {
                                Text(
                                    text = "Letting go of low-value tasks creates room for what actually matters. Drop/archive this task cleanly? (Undo available)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Button(
                                    onClick = {
                                        activeReflection = null
                                        onDrop()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = tokens.softRed,
                                        contentColor = MaterialTheme.colorScheme.background
                                    )
                                ) {
                                    Text("Drop / archive task")
                                }
                            }

                            null -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReflectionChipButton(
    label: String,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = AscendThemeTokens.colors
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.2f) else tokens.surfaceElevated,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent else tokens.cardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GhostBreakdownSheet(
    task: TaskEntity,
    breakdownState: GhostBreakdownState,
    onRequestAi: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmSteps: (List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tokens = AscendThemeTokens.colors
    var step1 by remember(task.id) { mutableStateOf("Spend 5 minutes outlining: ${task.title.take(28)}") }
    var step2 by remember(task.id) { mutableStateOf("Complete core first pass") }
    var step3 by remember(task.id) { mutableStateOf("Review and wrap up") }

    LaunchedEffect(breakdownState) {
        if (breakdownState is GhostBreakdownState.Ready) {
            step1 = breakdownState.steps.getOrElse(0) { step1 }
            step2 = breakdownState.steps.getOrElse(1) { step2 }
            step3 = breakdownState.steps.getOrElse(2) { step3 }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.surfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Break down ghost task",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Split \"${task.title}\" into 3 small steps in Do first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                onClick = onRequestAi,
                enabled = breakdownState !is GhostBreakdownState.Loading,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_breakdown_trigger_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (breakdownState is GhostBreakdownState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI Coach is drafting steps...",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate steps with AI Coach",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (breakdownState is GhostBreakdownState.Ready) {
                Text(
                    text = "Coach tip: ${breakdownState.tip}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.mint
                )
            } else if (breakdownState is GhostBreakdownState.Error) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = breakdownState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = step1,
                onValueChange = { step1 = it },
                label = { Text("Step 1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = tokens.cardBorderStrong
                )
            )
            OutlinedTextField(
                value = step2,
                onValueChange = { step2 = it },
                label = { Text("Step 2") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = tokens.cardBorderStrong
                )
            )
            OutlinedTextField(
                value = step3,
                onValueChange = { step3 = it },
                label = { Text("Step 3") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = tokens.cardBorderStrong
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, tokens.cardBorderStrong)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val validSteps = listOf(step1, step2, step3).map { it.trim() }.filter { it.isNotEmpty() }
                        if (validSteps.isNotEmpty()) onConfirmSteps(validSteps)
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("confirm_breakdown_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create steps")
                }
            }
        }
    }
}
