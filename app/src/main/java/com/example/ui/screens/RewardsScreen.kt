package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RewardEntity
import com.example.ui.components.AscendCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.AscendThemeTokens
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.AscendUiState
import com.example.ui.viewmodel.RewardCounselingAiState

private val EDITOR_EMOJIS = listOf("🎁", "🎬", "🎮", "☕", "📚", "🎧", "🌿", "🍜", "🎨", "🫶", "✈️", "🏆")

@Composable
fun RewardsScreen(
    uiState: AscendUiState,
    aiCounselingState: RewardCounselingAiState,
    onGenerateAiRewards: (
        rechargeActivities: List<String>,
        rewardCadence: String,
        specificTreat: String,
        onSaved: () -> Unit
    ) -> Unit,
    onSavePersonalizedRewards: (List<RewardEntity>) -> Unit,
    onResetAiCounseling: () -> Unit,
    onUpsertReward: (RewardEntity) -> Unit,
    onDeleteReward: (RewardEntity) -> Unit,
    onRedeemReward: (RewardEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var forceShowCounseling by rememberSaveable { mutableStateOf(false) }
    var showRewardEditor by remember { mutableStateOf(false) }
    var editingReward by remember { mutableStateOf<RewardEntity?>(null) }

    val shouldShowCounseling = !uiState.preferences.hasCompletedRewardCounseling ||
        uiState.rewards.isEmpty() ||
        forceShowCounseling

    if (shouldShowCounseling) {
        RewardCounselingScreen(
            canCancel = uiState.preferences.hasCompletedRewardCounseling && uiState.rewards.isNotEmpty(),
            showSkipForNow = false,
            aiState = aiCounselingState,
            onCancel = { forceShowCounseling = false },
            onGenerateWithGemini = { activities, cadence, treat ->
                onGenerateAiRewards(activities, cadence, treat) {
                    forceShowCounseling = false
                }
            },
            onSaveInstantOffline = { curated ->
                onSavePersonalizedRewards(curated)
                forceShowCounseling = false
            },
            modifier = modifier
        )
        return
    }

    val tokens = AscendThemeTokens.colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.xpLevelProgressFraction,
        animationSpec = spring(),
        label = "xp_wallet_progress"
    )
    val rewardPairs = uiState.rewards.chunked(2)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "rewards_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "PERSONALIZED XP STORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.gold
                    )
                    Text(
                        text = "Reward zone",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Surface(
                    onClick = {
                        onResetAiCounseling()
                        forceShowCounseling = true
                    },
                    shape = RoundedCornerShape(999.dp),
                    color = tokens.surfaceElevated,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("retake_counseling_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Retake Counseling",
                            tint = tokens.gold,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Retake Counseling",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // XP Wallet Hero Card
        item(key = "xp_wallet_card") {
            AscendCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("xp_wallet_card"),
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.24f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.82f, size.height * 0.25f),
                                    radius = size.width * 0.65f
                                )
                            )
                        }
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = tokens.gold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "XP WALLET",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.gold
                                    )
                                }

                                Text(
                                    text = "${uiState.totalXp} XP",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "${uiState.xpToNextLevel} XP to your next level",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                tokens.surfaceElevated
                                            )
                                        )
                                    )
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .testTag("level_circular_badge"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "LEVEL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${uiState.level}",
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = tokens.cardBorderStrong,
                                strokeCap = StrokeCap.Round
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Level ${uiState.level} · ${uiState.levelTitle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${uiState.progress.xpIntoCurrentLevel} / 100 XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Personalized Rewards Section Header + Add Custom Reward Action
        item(key = "redeem_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeaderRow(
                    title = "Your rewards (${uiState.rewards.size})",
                    trailingText = "${uiState.totalRewardRedemptions} unlocked",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "add_custom_reward_row") {
            OutlinedButton(
                onClick = {
                    editingReward = null
                    showRewardEditor = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_custom_reward_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, tokens.cardBorderStrong)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add custom reward", color = MaterialTheme.colorScheme.onSurface)
            }
        }

        rewardPairs.forEachIndexed { index, pair ->
            item(key = "reward_row_$index") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { reward ->
                        PersonalizedRewardCard(
                            reward = reward,
                            canAfford = uiState.totalXp >= reward.xpCost,
                            missingXp = (reward.xpCost - uiState.totalXp).coerceAtLeast(0),
                            onRedeem = { onRedeemReward(reward) },
                            onEdit = {
                                editingReward = reward
                                showRewardEditor = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item(key = "rewards_bottom_spacer") {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showRewardEditor) {
        RewardEditorSheet(
            initialReward = editingReward,
            onDismiss = {
                showRewardEditor = false
                editingReward = null
            },
            onSave = { updated ->
                onUpsertReward(updated)
                showRewardEditor = false
                editingReward = null
            },
            onDelete = editingReward?.let { toDelete ->
                {
                    onDeleteReward(toDelete)
                    showRewardEditor = false
                    editingReward = null
                }
            }
        )
    }
}

@Composable
private fun PersonalizedRewardCard(
    reward: RewardEntity,
    canAfford: Boolean,
    missingXp: Int,
    onRedeem: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = AscendThemeTokens.colors

    AscendCard(
        modifier = modifier.testTag("reward_card_${reward.id}"),
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = if (reward.redemptionCount > 0) tokens.mint.copy(alpha = 0.55f) else tokens.cardBorder,
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.surfaceElevated)
                        .border(1.dp, tokens.cardBorderStrong, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = reward.emoji, fontSize = 20.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (reward.redemptionCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = tokens.mintBg,
                            border = BorderStroke(1.dp, tokens.mint.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "Unlocked",
                                    tint = tokens.mint,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${reward.redemptionCount}×",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.mint
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_reward_${reward.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit reward",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = reward.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = reward.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onRedeem,
                enabled = canAfford,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("redeem_button_${reward.id}"),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) MaterialTheme.colorScheme.primary else tokens.surfaceElevated,
                    contentColor = if (canAfford) MaterialTheme.colorScheme.onPrimary else tokens.textMuted,
                    disabledContainerColor = tokens.surfaceElevated,
                    disabledContentColor = tokens.textMuted
                )
            ) {
                Text(
                    text = if (canAfford) "Redeem · ${reward.xpCost} XP" else "Need $missingXp XP",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RewardEditorSheet(
    initialReward: RewardEntity?,
    onDismiss: () -> Unit,
    onSave: (RewardEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tokens = AscendThemeTokens.colors

    var title by remember(initialReward) { mutableStateOf(initialReward?.title ?: "") }
    var description by remember(initialReward) { mutableStateOf(initialReward?.description ?: "") }
    var emoji by remember(initialReward) { mutableStateOf(initialReward?.emoji ?: "🎁") }
    var xpCost by remember(initialReward) { mutableIntStateOf(initialReward?.xpCost ?: 180) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.surfaceElevated,
        contentColor = MaterialTheme.colorScheme.onSurface
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialReward == null) "New custom reward" else "Edit reward",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete reward",
                            tint = tokens.softRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove", color = tokens.softRed)
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Reward title") },
                placeholder = { Text("e.g., Iced latte & comic chapter") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reward_editor_title"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = tokens.cardBorderStrong
                )
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Why this feels like a great break...") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = tokens.cardBorderStrong
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ICON",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EDITOR_EMOJIS.forEach { item ->
                        val selected = emoji == item
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else tokens.cardBorderStrong,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { emoji = item },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = item, fontSize = 20.sp)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "XP COST",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100, 120, 150, 180, 220, 250, 300, 350, 400).forEach { cost ->
                        val selected = xpCost == cost
                        Surface(
                            onClick = { xpCost = cost },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else tokens.cardBorderStrong
                            )
                        ) {
                            Text(
                                text = "$cost XP",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val base = initialReward ?: RewardEntity(
                        title = title.trim(),
                        description = description.trim().ifEmpty { "Personal guilt-free reward" },
                        emoji = emoji,
                        xpCost = xpCost,
                        category = "Personal",
                        isCustom = true
                    )
                    onSave(
                        base.copy(
                            title = title.trim(),
                            description = description.trim().ifEmpty { "Personal guilt-free reward" },
                            emoji = emoji,
                            xpCost = xpCost
                        )
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("reward_editor_save_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (initialReward == null) "Save Custom Reward" else "Update Reward")
            }
        }
    }
}
