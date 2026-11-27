package com.example.ai

import com.example.data.model.TaskEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject

data class GhostBreakdownResult(
    val microSteps: List<String>,
    val coachingTip: String
)

data class CuratedRewardDraft(
    val title: String,
    val description: String,
    val emoji: String,
    val xpCost: Int,
    val category: String
)

data class CoachToolCallbacks(
    val getCalendarEvents: suspend (daysAhead: Int) -> JsonObject,
    val getRecentEmails: suspend (maxResults: Int) -> JsonObject,
    val onToolActivity: (String) -> Unit
)

class AscendCoachService {

    private val getCalendarEventsDeclaration by lazy {
        FunctionDeclaration(
            name = TOOL_GET_CALENDAR_EVENTS,
            description = "Fetches the user's real Google Calendar events for today and tomorrow from their primary calendar. " +
                "Call this whenever the user asks about their schedule, meetings, availability, deadlines, or planning their day.",
            parameters = mapOf(
                "daysAhead" to Schema.integer(
                    description = "Number of days starting from today to retrieve events for (default 2 for today and tomorrow, max 7)."
                )
            ),
            optionalParameters = listOf("daysAhead")
        )
    }

    private val getRecentEmailsDeclaration by lazy {
        FunctionDeclaration(
            name = TOOL_GET_RECENT_EMAILS,
            description = "Fetches the user's most recent Gmail message subjects, senders, dates, and snippets. " +
                "Call this whenever the user asks about emails, inbox messages, deadlines in email, follow-ups, or action items from Gmail.",
            parameters = mapOf(
                "maxResults" to Schema.integer(
                    description = "Maximum number of recent emails to fetch (default 5, max 10)."
                )
            ),
            optionalParameters = listOf("maxResults")
        )
    }

    private val coachModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(
                Tool.functionDeclarations(
                    listOf(
                        getCalendarEventsDeclaration,
                        getRecentEmailsDeclaration
                    )
                )
            ),
            systemInstruction = content {
                text(
                    "You are Ascend AI Life Coach, a calm, sharp, and encouraging productivity coach inside the Ascend Android app. " +
                        "You help users prioritize with the Eisenhower Matrix, overcome procrastination on Ghost Tasks " +
                        "(tasks postponed 2+ times), protect deep focus blocks, and align their tasks with their real schedule and inbox. " +
                        "You have two live function calling tools: `getCalendarEvents` (today & tomorrow's Google Calendar events) and " +
                        "`getRecentEmails` (the 5 most recent Gmail subjects and snippets). " +
                        "Whenever the user asks about their schedule, calendar, meetings, availability, deadlines, or emails, ALWAYS call the matching tool(s) first so you answer with real data. " +
                        "If a tool reports connected=false, briefly explain that the user can tap Connect on the MCP connections screen, and still help using their Room tasks. " +
                        "Keep replies concise, structured, practical, and warm — under 200 words unless asked for a detailed plan."
                )
            }
        )
    }

    private val ghostBreakdownSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "microSteps" to Schema.array(
                    items = Schema.string(description = "A concrete 5 to 15 minute starter action"),
                    description = "Exactly 3 small, friction-free micro-steps to complete the stalled task"
                ),
                "coachingTip" to Schema.string(
                    description = "One short, encouraging sentence explaining how to build momentum on this task"
                )
            )
        )
    }

    private val breakdownModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = ghostBreakdownSchema
            }
        )
    }

    private val curatedRewardsSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "rewards" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "title" to Schema.string(description = "Short, appetizing reward title (2-4 words)"),
                            "description" to Schema.string(description = "Warm 1-sentence description (under 70 characters)"),
                            "emoji" to Schema.string(description = "A single expressive Unicode emoji representing this reward"),
                            "xpCost" to Schema.integer(description = "XP cost between 100 and 400 in increments of 20 or 25"),
                            "category" to Schema.string(description = "Short category label like Recharge, Treat, Creative, Social, or Adventure")
                        )
                    ),
                    description = "6 to 8 personalized guilt-free rewards spanning quick wins (100-160 XP), evening treats (180-260 XP), and milestones (280-400 XP)"
                )
            )
        )
    }

    private val curatedRewardsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = curatedRewardsSchema
            }
        )
    }

    suspend fun sendCoachingWithTools(
        userMessage: String,
        workspaceContext: String,
        recentHistory: List<Pair<String, String>>,
        callbacks: CoachToolCallbacks
    ): String {
        val chatHistory = recentHistory.takeLast(6).map { (role, text) ->
            content(role = if (role == "User") "user" else "model") {
                text(text)
            }
        }

        val chat = coachModel.startChat(history = chatHistory)

        val enrichedPrompt = buildString {
            appendLine("=== CURRENT ASCEND WORKSPACE SNAPSHOT ===")
            appendLine(workspaceContext)
            appendLine("\n=== USER MESSAGE ===")
            append(userMessage)
        }

        var response = chat.sendMessage(enrichedPrompt)
        var toolRounds = 0

        while (response.functionCalls.isNotEmpty() && toolRounds < MAX_TOOL_ROUNDS) {
            toolRounds++
            val responseParts = response.functionCalls.map { call ->
                val resultJson: JsonObject = when (call.name) {
                    TOOL_GET_CALENDAR_EVENTS -> {
                        callbacks.onToolActivity("Fetching Google Calendar events...")
                        val daysAhead = call.args["daysAhead"]?.jsonPrimitive?.intOrNull ?: 2
                        callbacks.getCalendarEvents(daysAhead.coerceIn(1, 7))
                    }

                    TOOL_GET_RECENT_EMAILS -> {
                        callbacks.onToolActivity("Fetching recent Gmail messages...")
                        val maxResults = call.args["maxResults"]?.jsonPrimitive?.intOrNull ?: 5
                        callbacks.getRecentEmails(maxResults.coerceIn(1, 10))
                    }

                    else -> {
                        buildJsonObject {
                            put("error", JsonPrimitive("Unknown tool requested: ${call.name}"))
                        }
                    }
                }

                FunctionResponsePart(
                    name = call.name,
                    response = resultJson
                )
            }

            callbacks.onToolActivity("Synthesizing plan with live data...")
            response = chat.sendMessage(
                content(role = "function") {
                    responseParts.forEach { part(it) }
                }
            )
        }

        return response.text?.trim().orEmpty()
    }

    suspend fun generateRewardsFromCounselingAnswers(
        userName: String,
        rechargeActivities: List<String>,
        rewardPacing: String,
        xpRangeHint: String,
        specificTreat: String
    ): List<CuratedRewardDraft> {
        val prompt = buildString {
            appendLine("Design 7 deeply personalized, guilt-free XP rewards for $userName based strictly on their Reward Zone Counseling answers.")
            appendLine("Rules:")
            appendLine("- Return between 6 and 8 concrete, appetizing rewards.")
            appendLine("- Center most XP costs around their preferred pacing bracket ($xpRangeHint), with 1-2 nearby stretch or quick options for variety.")
            appendLine("- Every reward must directly reflect their chosen recharge activities or specific treat.")
            if (specificTreat.isNotBlank()) {
                appendLine("- IMPORTANT: Feature their specific personal treat (\"$specificTreat\") as one of the rewards with a fitting emoji and XP cost.")
            }
            appendLine("- Make each title punchy (2-4 words), each emoji unique, and each description warm and under 65 characters.")
            appendLine()
            appendLine("=== USER COUNSELING ANSWERS ===")
            appendLine("Q1 (Recharge activities): ${rechargeActivities.joinToString(", ")}")
            appendLine("Q2 (Preferred reward cadence): $rewardPacing ($xpRangeHint)")
            appendLine("Q3 (Specific treat/hobby/guilty pleasure): ${specificTreat.ifBlank { "None specified" }}")
        }
        val response = curatedRewardsModel.generateContent(prompt)
        val rawJson = response.text ?: throw IllegalStateException("Gemini AI returned an empty reward catalog.")
        val root = JSONObject(rawJson)
        val arr = root.getJSONArray("rewards")
        val parsed = (0 until arr.length()).mapNotNull { idx ->
            val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
            val title = obj.optString("title").trim()
            if (title.isEmpty()) return@mapNotNull null
            CuratedRewardDraft(
                title = title,
                description = obj.optString("description").trim().ifEmpty { "A guilt-free reward you earned." },
                emoji = obj.optString("emoji").trim().ifEmpty { "🎁" },
                xpCost = obj.optInt("xpCost", 180).coerceIn(80, 450),
                category = obj.optString("category").trim().ifEmpty { "Recharge" }
            )
        }.take(8)
        if (parsed.isEmpty()) {
            throw IllegalStateException("Gemini AI did not return any valid rewards.")
        }
        return parsed
    }

    suspend fun generateGhostBreakdown(task: TaskEntity): GhostBreakdownResult {
        val prompt = buildString {
            appendLine("Break down this stalled productivity task into 3 tiny, ultra-actionable micro-steps (5-15 mins each) so the user can immediately regain momentum.")
            appendLine("Task Title: ${task.title}")
            if (task.notes.isNotBlank()) {
                appendLine("Notes: ${task.notes}")
            }
            appendLine("Current Quadrant: ${task.quadrantEnum.title} (${task.quadrantEnum.subtitle})")
            appendLine("Times Rescheduled: ${task.rescheduleCount}")
        }
        val response = breakdownModel.generateContent(prompt)
        val rawJson = response.text ?: throw IllegalStateException("AI Coach returned an empty breakdown.")
        val json = JSONObject(rawJson)
        val stepsJson = json.getJSONArray("microSteps")
        val steps = (0 until stepsJson.length())
            .map { stepsJson.getString(it).trim() }
            .filter { it.isNotEmpty() }
            .take(4)
        val tip = json.optString("coachingTip", "Start with Step 1 for just 5 minutes to break inertia.")
        if (steps.isEmpty()) {
            throw IllegalStateException("AI Coach could not generate steps for this task.")
        }
        return GhostBreakdownResult(microSteps = steps, coachingTip = tip)
    }

    companion object {
        const val TOOL_GET_CALENDAR_EVENTS = "getCalendarEvents"
        const val TOOL_GET_RECENT_EMAILS = "getRecentEmails"
        private const val MAX_TOOL_ROUNDS = 4
    }
}
