package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.ui.components.CoachSparklePill
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.TaskRowCard
import com.example.ui.theme.AscendThemeTokens
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.AscendUiState

@Composable
fun HomeScreen(
    uiState: AscendUiState,
    onOpenCoach: () -> Unit,
    onOpenPriorities: () -> Unit,
    onOpenGhost: () -> Unit,
    onOpenMcp: () -> Unit,
    onOpenRewards: () -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onPostponeTask: (TaskEntity) -> Unit,
    onMoveTask: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = AscendThemeTokens.colors

    val todaysFocusTasks = remember(uiState.tasks) {
        uiState.activeTasks
            .sortedWith(
                compareBy<TaskEntity> { it.quadrantEnum.ordinal }
                    .thenBy { it.dueAtMillis }
            )
            .take(4)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header: "Make room for what matters." + Level X · Explorer badge
        item(key = "overview_header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WELCOME BACK, ${uiState.preferences.displayName.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        onClick = onOpenRewards,
                        shape = RoundedCornerShape(999.dp),
                        color = tokens.goldBg,
                        border = BorderStroke(1.dp, tokens.gold.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("overview_level_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MilitaryTech,
                                contentDescription = "Current level",
                                tint = tokens.gold,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Level ${uiState.level} · ${uiState.levelTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.gold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(
                    text = "Make room for what matters.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Your Consistency weekly tracker (M T W T F S S)
        item(key = "consistency_tracker") {
            AscendCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overview_consistency_card"),
                borderColor = tokens.cardBorderStrong
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
                        Text(
                            text = "Your Consistency",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val activeDaysCount = uiState.weeklyConsistency.count { it.isActive }
                        Text(
                            text = "$activeDaysCount / 7 days active",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.mint
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        uiState.weeklyConsistency.forEach { day ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                day.isActive -> tokens.mint.copy(alpha = 0.22f)
                                                day.isToday -> MaterialTheme.colorScheme.primaryContainer
                                                else -> tokens.surfaceElevated
                                            }
                                        )
                                        .border(
                                            width = if (day.isToday) 1.5.dp else 1.dp,
                                            color = when {
                                                day.isToday -> MaterialTheme.colorScheme.primary
                                                day.isActive -> tokens.mint
                                                else -> tokens.cardBorder
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (day.isActive) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "${day.letter} active",
                                            tint = tokens.mint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (day.isToday) MaterialTheme.colorScheme.primary else tokens.textMuted)
                                        )
                                    }
                                }
                                Text(
                                    text = day.letter,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4-Card Stats Grid
        item(key = "overview_stats_grid") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OverviewStatCard(
                        title = "Tasks in focus",
                        value = "${uiState.pendingTasksCount}",
                        subtitle = "Pending",
                        accent = tokens.skyBlue,
                        icon = Icons.Rounded.TrackChanges,
                        onClick = onOpenPriorities,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_tasks_in_focus")
                    )
                    OverviewStatCard(
                        title = "Completed",
                        value = "${uiState.completedTasksCount}",
                        subtitle = "Tasks done",
                        accent = tokens.mint,
                        icon = Icons.Rounded.CheckCircle,
                        onClick = onOpenPriorities,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_completed")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OverviewStatCard(
                        title = "Experience points",
                        value = "${uiState.totalXp} XP",
                        subtitle = "Level ${uiState.level} · ${uiState.xpToNextLevel} to next",
                        accent = MaterialTheme.colorScheme.primary,
                        icon = Icons.Rounded.Bolt,
                        onClick = onOpenRewards,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_xp")
                    )
                    OverviewStatCard(
                        title = "Current streak",
                        value = "${uiState.currentStreak}d",
                        subtitle = "Best ${uiState.bestStreak}d",
                        accent = tokens.gold,
                        icon = Icons.Rounded.LocalFireDepartment,
                        onClick = onOpenRewards,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_streak")
                    )
                }
            }
        }

        // Today's focus (Top 4 pending tasks)
        item(key = "todays_focus_header") {
            SectionHeaderRow(
                title = "Today's focus",
                trailingText = "All priorities",
                onTrailingClick = onOpenPriorities
            )
        }

        if (todaysFocusTasks.isEmpty()) {
            item(key = "todays_focus_empty") {
                AscendCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = tokens.mint,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "All caught up for today",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap + New task above to plan your next win and earn +20 XP.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(todaysFocusTasks, key = { it.id }) { task ->
                TaskRowCard(
                    task = task,
                    onToggleComplete = { onToggleTask(task) },
                    onPostpone = { onPostponeTask(task) },
                    onMoveQuadrant = { onMoveTask(task) },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task) }
                )
            }
        }

        // AI Companion Spotlight Card
        item(key = "ai_companion_spotlight") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                tokens.deepPurple.copy(alpha = 0.6f),
                                tokens.mint.copy(alpha = 0.35f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clickable(onClick = onOpenCoach)
                    .testTag("open_ascend_companion_card")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ascend_companion_bg_1795614806603),
                    contentDescription = "AI Companion spotlight artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoachSparklePill(label = "AI COMPANION")

                    Text(
                        text = "Need clarity on what to tackle next?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Your AI life coach reads your tasks, Level ${uiState.level} XP, and ${uiState.ghostTasks.size} ghost task${if (uiState.ghostTasks.size == 1) "" else "s"} to help you focus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Ask AI life coach",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Your Personal Radar (Ghost tasks + MCP connections)
        item(key = "personal_radar") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeaderRow(title = "Your personal radar")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadarLinkCard(
                        title = "Ghost tasks",
                        subtitle = if (uiState.ghostTasks.isEmpty()) {
                            "0 lingering tasks"
                        } else {
                            "${uiState.ghostTasks.size} postponed 2+ times"
                        },
                        accent = tokens.softRed,
                        icon = Icons.Rounded.AutoAwesomeMotion,
                        onClick = onOpenGhost,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("radar_ghost_card")
                    )

                    RadarLinkCard(
                        title = "MCP connections",
                        subtitle = "Calendar, Gmail & tools",
                        accent = tokens.skyBlue,
                        icon = Icons.Rounded.Hub,
                        onClick = onOpenMcp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("radar_mcp_card")
                    )
                }
            }
        }

        item(key = "overview_bottom_spacer") {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun OverviewStatCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = AscendThemeTokens.colors
    AscendCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = tokens.cardBorder,
        cornerRadius = 18.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RadarLinkCard(
    title: String,
    subtitle: String,
    accent: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = AscendThemeTokens.colors
    AscendCard(
        modifier = modifier,
        containerColor = tokens.surfaceElevated,
        borderColor = accent.copy(alpha = 0.35f),
        cornerRadius = 18.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Open $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
