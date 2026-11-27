package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.EisenhowerQuadrant
import com.example.data.model.TaskCategory
import com.example.data.model.TaskEntity
import com.example.ui.components.AscendCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.TaskRowCard
import com.example.ui.components.quadrantAccentColor
import com.example.ui.components.quadrantContainerColor
import com.example.ui.theme.AscendThemeTokens
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.AscendUiState

@Composable
fun PrioritiesScreen(
    uiState: AscendUiState,
    onAddTaskInQuadrant: (EisenhowerQuadrant) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onPostponeTask: (TaskEntity) -> Unit,
    onMoveTask: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = AscendThemeTokens.colors
    var selectedQuadrant by remember { mutableStateOf<EisenhowerQuadrant?>(null) }
    var selectedCategory by remember { mutableStateOf<TaskCategory?>(null) }
    var showCompleted by remember { mutableStateOf(true) }

    val filteredTasks = remember(uiState.tasks, selectedQuadrant, selectedCategory, showCompleted) {
        uiState.tasks
            .filter { selectedQuadrant == null || it.quadrantEnum == selectedQuadrant }
            .filter { selectedCategory == null || it.categoryEnum == selectedCategory }
            .filter { showCompleted || !it.isCompleted }
            .sortedWith(
                compareBy<TaskEntity> { it.isCompleted }
                    .thenBy { it.quadrantEnum.ordinal }
                    .thenBy { it.dueAtMillis }
            )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "priorities_header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "EISENHOWER MATRIX · +20 XP PER COMPLETION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "My priorities",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Interactive 4-Quadrant Eisenhower Matrix displaying tasks inside each quadrant
        item(key = "matrix_grid") {
            val rows = EisenhowerQuadrant.entries.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { quadrant ->
                            val isSelected = selectedQuadrant == quadrant
                            val accent = quadrantAccentColor(quadrant)
                            val bg = quadrantContainerColor(quadrant)
                            val activeTasks = uiState.activeTasksForQuadrant(quadrant)

                            Surface(
                                onClick = {
                                    selectedQuadrant = if (isSelected) null else quadrant
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("matrix_quadrant_${quadrant.name.lowercase()}"),
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) bg else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) accent else tokens.cardBorder
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(accent)
                                            )
                                            Text(
                                                text = quadrant.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = "${activeTasks.size}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = accent
                                        )
                                    }

                                    Text(
                                        text = quadrant.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (activeTasks.isEmpty()) {
                                        Surface(
                                            onClick = { onAddTaskInQuadrant(quadrant) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = tokens.surfaceElevated
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Add,
                                                    contentDescription = "Add task to ${quadrant.title}",
                                                    tint = accent,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Add here",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            activeTasks.take(2).forEach { task ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(tokens.surfaceElevated.copy(alpha = 0.9f))
                                                        .clickable { onToggleTask(task) }
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clip(CircleShape)
                                                            .border(1.5.dp, accent, CircleShape)
                                                    )
                                                    Text(
                                                        text = task.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                            if (activeTasks.size > 2) {
                                                Text(
                                                    text = "+${activeTasks.size - 2} more",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = accent
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category & Status Filter Row
        item(key = "matrix_filter_controls") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "filter_all") {
                    FilterChip(
                        selected = selectedQuadrant == null && selectedCategory == null,
                        onClick = {
                            selectedQuadrant = null
                            selectedCategory = null
                        },
                        label = { Text("All (${uiState.tasks.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                item(key = "filter_active") {
                    FilterChip(
                        selected = !showCompleted,
                        onClick = { showCompleted = !showCompleted },
                        label = { Text(if (showCompleted) "Hide done" else "Pending only") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                items(TaskCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = if (selectedCategory == cat) null else cat
                        },
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        item(key = "matrix_list_header") {
            SectionHeaderRow(
                title = selectedQuadrant?.title ?: "All tasks",
                trailingText = "${filteredTasks.size} shown"
            )
        }

        if (filteredTasks.isEmpty()) {
            item(key = "matrix_empty") {
                AscendCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GridView,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "No matching tasks",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap + New task or clear filters to see everything in your matrix.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                TaskRowCard(
                    task = task,
                    onToggleComplete = { onToggleTask(task) },
                    onPostpone = { onPostponeTask(task) },
                    onMoveQuadrant = { onMoveTask(task) },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task) },
                    showQuadrantBadge = selectedQuadrant == null
                )
            }
        }

        item(key = "priorities_bottom_spacer") {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
