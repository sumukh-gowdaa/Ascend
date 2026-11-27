package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RewardEntity
import com.example.ui.components.AscendCard
import com.example.ui.theme.AscendThemeTokens
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.RewardCounselingAiState

private data class OfflineRewardTemplate(
    val title: String,
    val description: String,
    val emoji: String,
    val xpCost: Int,
    val category: String
)

private enum class RechargeActivity(
    val label: String,
    val emoji: String,
    val quickBreak: OfflineRewardTemplate,
    val eveningWindDown: OfflineRewardTemplate,
    val weekendTreat: OfflineRewardTemplate
) {
    MOVIES_ANIME(
        label = "Movies/Anime",
        emoji = "🎬",
        quickBreak = OfflineRewardTemplate("1 anime episode break", "Watch one guilt-free episode with zero multitasking", "📺", 120, "Entertainment"),
        eveningWindDown = OfflineRewardTemplate("Cozy movie night", "Feature film + favorite snacks after wrapping up", "🎬", 210, "Entertainment"),
        weekendTreat = OfflineRewardTemplate("Cinema outing", "Big-screen IMAX trip with popcorn this weekend", "🍿", 340, "Milestone")
    ),
    GAMING(
        label = "Gaming",
        emoji = "🎮",
        quickBreak = OfflineRewardTemplate("15-min quick match", "Short guilt-free gaming sprint between study blocks", "🕹️", 120, "Play"),
        eveningWindDown = OfflineRewardTemplate("Evening game session", "Dedicated 1-hour campaign or co-op run", "🎮", 200, "Play"),
        weekendTreat = OfflineRewardTemplate("Weekend gaming marathon", "Uninterrupted evening + new indie DLC treat", "🏆", 360, "Milestone")
    ),
    MUSIC_PODCASTS(
        label = "Music/Podcasts",
        emoji = "🎧",
        quickBreak = OfflineRewardTemplate("15-min album reset", "Eyes closed with 3 favorite tracks and deep breaths", "🎧", 100, "Recharge"),
        eveningWindDown = OfflineRewardTemplate("Full album & podcast hour", "Unplugged listening session with zero screens", "🎵", 180, "Recharge"),
        weekendTreat = OfflineRewardTemplate("Live music or vinyl treat", "Acoustic lounge, concert ticket, or wishlist vinyl", "🎸", 340, "Milestone")
    ),
    READING(
        label = "Reading",
        emoji = "📚",
        quickBreak = OfflineRewardTemplate("Chapter break", "15–20 minutes of fiction with tea or coffee", "📖", 120, "Calm"),
        eveningWindDown = OfflineRewardTemplate("Evening reading hour", "Cozy uninterrupted chapter marathon before bed", "📚", 200, "Calm"),
        weekendTreat = OfflineRewardTemplate("Bookstore splurge", "Pick up a new hardcover + lazy reading afternoon", "✨", 320, "Milestone")
    ),
    CAFE_FOOD(
        label = "Café/Food treats",
        emoji = "☕",
        quickBreak = OfflineRewardTemplate("Specialty café treat", "Iced coffee, matcha, or a fresh bakery pastry", "☕", 140, "Treat"),
        eveningWindDown = OfflineRewardTemplate("Guilt-free takeout dinner", "Order your comfort meal instead of cooking", "🍜", 240, "Treat"),
        weekendTreat = OfflineRewardTemplate("Weekend brunch date", "Treat yourself to a favorite café or dessert spot", "🥐", 360, "Milestone")
    ),
    OUTDOOR_WALKS(
        label = "Outdoor walks",
        emoji = "🌿",
        quickBreak = OfflineRewardTemplate("15-min sunlight walk", "Phone-free reset walk outside for fresh air", "🌿", 100, "Wellness"),
        eveningWindDown = OfflineRewardTemplate("Golden-hour stroll", "Sunset park walk or relaxed evening bike ride", "🌅", 180, "Wellness"),
        weekendTreat = OfflineRewardTemplate("Weekend nature escape", "Half-day scenic trail, lake, or botanical garden", "⛰️", 340, "Adventure")
    ),
    SIDE_PROJECT_CODING(
        label = "Side-project/Coding time",
        emoji = "💻",
        quickBreak = OfflineRewardTemplate("15-min tinker sprint", "Play with a cool library, shader, or micro-idea", "⚡", 120, "Creative"),
        eveningWindDown = OfflineRewardTemplate("Guilt-free indie build hour", "Pure passion-project coding with zero deadlines", "💻", 220, "Creative"),
        weekendTreat = OfflineRewardTemplate("Weekend hack & upgrade", "New dev tool, plugin, or deep creative build day", "🛠️", 360, "Milestone")
    ),
    FRIENDS_HANGOUT(
        label = "Hanging out with friends",
        emoji = "🫶",
        quickBreak = OfflineRewardTemplate("Friend catch-up call", "15-min relaxed chat or voice notes with a close friend", "💬", 120, "Social"),
        eveningWindDown = OfflineRewardTemplate("Evening coffee or dinner", "Unplugged hangout with friends after finishing tasks", "🫶", 240, "Social"),
        weekendTreat = OfflineRewardTemplate("Weekend group adventure", "Bowling, trivia night, road trip, or hangout", "🎉", 380, "Milestone")
    )
}

private enum class RewardPacingOption(
    val title: String,
    val xpRange: String,
    val subtitle: String,
    val promptSummary: String,
    val defaultCustomXp: Int
) {
    SMALL_BREAKS(
        title = "Frequent small 15-min breaks",
        xpRange = "100–150 XP",
        subtitle = "Fast daily wins after finishing 5–7 focused tasks",
        promptSummary = "Frequent small 15-min breaks (100–150 XP)",
        defaultCustomXp = 140
    ),
    EVENING_WINDDOWNS(
        title = "Balanced evening wind-downs",
        xpRange = "180–250 XP",
        subtitle = "Satisfying end-of-day guilt-free treats",
        promptSummary = "Balanced evening wind-downs (180–250 XP)",
        defaultCustomXp = 220
    ),
    BIG_WEEKEND_TREATS(
        title = "Big weekend treats",
        xpRange = "300–400 XP",
        subtitle = "High-anticipation milestones saved for the weekend",
        promptSummary = "Big weekend treats (300–400 XP)",
        defaultCustomXp = 340
    )
}

private val Q3_SUGGESTIONS = listOf(
    "Iced coffee & pastry",
    "2 episodes of anime",
    "1 hour guilt-free gaming",
    "New book or vinyl"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RewardCounselingScreen(
    canCancel: Boolean,
    showSkipForNow: Boolean = false,
    aiState: RewardCounselingAiState,
    onCancel: () -> Unit,
    onGenerateWithGemini: (
        rechargeActivities: List<String>,
        rewardCadence: String,
        specificTreat: String
    ) -> Unit,
    onSaveInstantOffline: (List<RewardEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = AscendThemeTokens.colors
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    val selectedActivities = remember { mutableStateListOf<RechargeActivity>() }
    var selectedPacing by remember { mutableStateOf(RewardPacingOption.EVENING_WINDDOWNS) }
    var specificTreat by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reward_counseling_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Welcome Header + Progress Indicator (Question 1 of 4 -> 4 of 4)
        item(key = "counseling_header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "REWARD ZONE COUNSELING",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.gold
                        )
                        Text(
                            text = "Let's personalize your Reward Zone — tell me what actually motivates you.",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (canCancel) {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.testTag("counseling_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close counseling",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (showSkipForNow) {
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.testTag("counseling_skip_button")
                        ) {
                            Text(
                                text = "Later",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Step progress bar + "Question X of 4"
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question $currentStep of 4",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("counseling_step_label")
                        )
                        Text(
                            text = "${currentStep * 25}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..4).forEach { idx ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (idx <= currentStep) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            tokens.cardBorderStrong
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Active Question Card (1 -> 4)
        item(key = "counseling_question_body") {
            AnimatedContent(
                targetState = currentStep,
                label = "counseling_question_step"
            ) { step ->
                when (step) {
                    1 -> {
                        AscendCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "What kind of breaks or activities genuinely recharge you?",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Select all that apply — your Reward Zone will be built exclusively around what energizes you.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RechargeActivity.entries.forEach { activity ->
                                        val isSelected = activity in selectedActivities
                                        Surface(
                                            onClick = {
                                                if (isSelected) {
                                                    selectedActivities.remove(activity)
                                                } else {
                                                    selectedActivities.add(activity)
                                                }
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                tokens.surfaceElevated
                                            },
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    tokens.cardBorderStrong
                                                }
                                            ),
                                            modifier = Modifier.testTag("counseling_q1_${activity.name.lowercase()}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                                            ) {
                                                Text(text = activity.emoji, fontSize = 16.sp)
                                                Text(
                                                    text = activity.label,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (selectedActivities.isEmpty()) {
                                    Text(
                                        text = "Pick at least 1 recharge activity to continue.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.gold
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        AscendCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "How do you prefer to reward yourself after finishing tasks?",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Every completed task earns +20 XP. Pick your ideal reward rhythm:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                RewardPacingOption.entries.forEach { option ->
                                    val isSelected = selectedPacing == option
                                    Surface(
                                        onClick = { selectedPacing = option },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            tokens.surfaceElevated
                                        },
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                tokens.cardBorder
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("counseling_q2_${option.name.lowercase()}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = option.title,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    text = option.xpRange,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = tokens.gold
                                                )
                                                Text(
                                                    text = option.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Selected pacing",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        AscendCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "What is one specific treat, hobby, or guilty pleasure you want to earn guilt-free?",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "We'll make sure this signature reward is included in your personal store.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = specificTreat,
                                    onValueChange = { specificTreat = it },
                                    label = { Text("Your guilt-free reward") },
                                    placeholder = { Text("e.g., \"Iced coffee & pastry\" or \"2 episodes of anime\"") },
                                    minLines = 2,
                                    maxLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("counseling_q3_input"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = tokens.cardBorderStrong,
                                        focusedContainerColor = tokens.surfaceElevated,
                                        unfocusedContainerColor = tokens.surfaceElevated
                                    )
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "QUICK IDEAS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Q3_SUGGESTIONS.forEach { idea ->
                                            Surface(
                                                onClick = { specificTreat = idea },
                                                shape = RoundedCornerShape(999.dp),
                                                color = tokens.surfaceElevated,
                                                border = BorderStroke(1.dp, tokens.cardBorderStrong)
                                            ) {
                                                Text(
                                                    text = idea,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        AscendCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = tokens.gold.copy(alpha = 0.55f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Would you like Gemini AI to tailor 6–8 custom rewards from your answers, or generate them instantly offline?",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Compact summary of Q1–Q3 answers
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = tokens.surfaceElevated,
                                    border = BorderStroke(1.dp, tokens.cardBorderStrong),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "YOUR MOTIVATION PROFILE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tokens.gold
                                        )
                                        Text(
                                            text = "Recharges: ${selectedActivities.joinToString(" · ") { "${it.emoji} ${it.label}" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Pacing: ${selectedPacing.title} (${selectedPacing.xpRange})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (specificTreat.isNotBlank()) {
                                            Text(
                                                text = "Signature treat: \"${specificTreat.trim()}\"",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = tokens.mint
                                            )
                                        }
                                    }
                                }

                                if (aiState.isGenerating) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.5.dp,
                                            color = tokens.gold
                                        )
                                        Text(
                                            text = "Gemini AI is crafting 6–8 custom rewards from your answers...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (!aiState.errorMessage.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ErrorOutline,
                                                contentDescription = "AI error",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = aiState.errorMessage,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            onGenerateWithGemini(
                                                selectedActivities.map { it.label },
                                                selectedPacing.promptSummary,
                                                specificTreat.trim()
                                            )
                                        },
                                        enabled = !aiState.isGenerating,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("counseling_generate_ai_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "✨ Generate with Gemini AI",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val offlineRewards = buildInstantOfflineRewards(
                                                activities = selectedActivities.toList(),
                                                pacing = selectedPacing,
                                                specificTreat = specificTreat.trim()
                                            )
                                            onSaveInstantOffline(offlineRewards)
                                        },
                                        enabled = !aiState.isGenerating,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("counseling_build_offline_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = tokens.surfaceElevated,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = BorderStroke(1.dp, tokens.mint.copy(alpha = 0.65f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Bolt,
                                            contentDescription = null,
                                            tint = tokens.mint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "⚡ Build My Rewards Instantly",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Step Navigation Buttons
        item(key = "counseling_nav_bar") {
            if (currentStep < 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("counseling_back_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, tokens.cardBorderStrong)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Button(
                        onClick = { currentStep++ },
                        enabled = currentStep != 1 || selectedActivities.isNotEmpty(),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp)
                            .testTag("counseling_next_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = if (currentStep == 3 && specificTreat.isBlank()) "Skip & Continue" else "Next",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { currentStep = 3 },
                    enabled = !aiState.isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("counseling_edit_answers_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, tokens.cardBorderStrong)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit answers", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        item(key = "counseling_bottom_spacer") {
            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}

private fun buildInstantOfflineRewards(
    activities: List<RechargeActivity>,
    pacing: RewardPacingOption,
    specificTreat: String
): List<RewardEntity> {
    val chosen = activities.ifEmpty { listOf(RechargeActivity.CAFE_FOOD, RechargeActivity.MOVIES_ANIME) }
    val result = mutableListOf<RewardEntity>()
    val seenTitles = mutableSetOf<String>()

    fun addIfUnique(entity: RewardEntity) {
        if (seenTitles.add(entity.title.lowercase())) {
            result += entity
        }
    }

    // Include Q3 personal signature treat prominently first
    if (specificTreat.isNotBlank()) {
        addIfUnique(
            RewardEntity(
                title = specificTreat.take(40),
                description = "Your signature guilt-free reward",
                emoji = inferEmojiForTreat(specificTreat),
                xpCost = pacing.defaultCustomXp,
                category = "Personal",
                isCustom = true
            )
        )
    }

    // Primary tier aligned tightly with Q2 pacing
    chosen.forEach { act ->
        val primaryTemplate = when (pacing) {
            RewardPacingOption.SMALL_BREAKS -> act.quickBreak
            RewardPacingOption.EVENING_WINDDOWNS -> act.eveningWindDown
            RewardPacingOption.BIG_WEEKEND_TREATS -> act.weekendTreat
        }
        addIfUnique(primaryTemplate.toEntity())
    }

    // Secondary tier from the user's selected Q1 activities so the store has a rich 6-8 item ladder
    chosen.forEach { act ->
        if (result.size >= 7) return@forEach
        val secondaryTemplate = when (pacing) {
            RewardPacingOption.SMALL_BREAKS -> act.eveningWindDown
            RewardPacingOption.EVENING_WINDDOWNS -> act.quickBreak
            RewardPacingOption.BIG_WEEKEND_TREATS -> act.eveningWindDown
        }
        addIfUnique(secondaryTemplate.toEntity())
    }

    // Tertiary tier if the user only picked 1-2 activities, keeping all rewards tied to their Q1 picks
    chosen.forEach { act ->
        if (result.size >= 6) return@forEach
        val tertiaryTemplate = when (pacing) {
            RewardPacingOption.SMALL_BREAKS -> act.weekendTreat
            RewardPacingOption.EVENING_WINDDOWNS -> act.weekendTreat
            RewardPacingOption.BIG_WEEKEND_TREATS -> act.quickBreak
        }
        addIfUnique(tertiaryTemplate.toEntity())
    }

    // Guarantee at least 6 rewards if only 1 activity was picked
    val bonusFallbacks = listOf(
        RewardEntity(
            title = "Zero-guilt recharge break",
            description = "Unplug completely for 20 minutes with zero screens",
            emoji = "✨",
            xpCost = 120,
            category = "Recharge"
        ),
        RewardEntity(
            title = "Evening favorite snack",
            description = "Treat yourself to a favorite drink or dessert",
            emoji = "🍨",
            xpCost = 190,
            category = "Treat"
        ),
        RewardEntity(
            title = "Slow weekend morning",
            description = "No alarms, slow breakfast, and your favorite playlist",
            emoji = "☀️",
            xpCost = 320,
            category = "Milestone"
        )
    )
    for (fallback in bonusFallbacks) {
        if (result.size >= 6) break
        addIfUnique(fallback)
    }

    return result.take(8).sortedBy { it.xpCost }
}

private fun OfflineRewardTemplate.toEntity(): RewardEntity = RewardEntity(
    title = title,
    description = description,
    emoji = emoji,
    xpCost = xpCost,
    category = category,
    isCustom = false
)

private fun inferEmojiForTreat(text: String): String {
    val lower = text.lowercase()
    return when {
        "coffee" in lower || "latte" in lower || "tea" in lower || "matcha" in lower -> "☕"
        "anime" in lower || "movie" in lower || "show" in lower || "episode" in lower -> "🎬"
        "game" in lower || "gaming" in lower || "xbox" in lower || "ps5" in lower -> "🎮"
        "book" in lower || "read" in lower || "manga" in lower -> "📚"
        "music" in lower || "vinyl" in lower || "podcast" in lower -> "🎧"
        "pizza" in lower || "pastry" in lower || "cake" in lower || "ice cream" in lower || "sushi" in lower || "boba" in lower -> "🥐"
        "walk" in lower || "hike" in lower || "park" in lower -> "🌿"
        "code" in lower || "coding" in lower || "project" in lower -> "💻"
        "friend" in lower || "dinner" in lower || "hangout" in lower -> "🫶"
        else -> "🎁"
    }
}
