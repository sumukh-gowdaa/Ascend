package com.example.mcp

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private const val TAG = "GoogleWorkspaceMcp"

enum class McpConnectorType(
    val displayName: String,
    val oauthScope: String
) {
    CALENDAR(
        displayName = "Google Calendar",
        oauthScope = "https://www.googleapis.com/auth/calendar.readonly"
    ),
    GMAIL(
        displayName = "Gmail",
        oauthScope = "https://www.googleapis.com/auth/gmail.readonly"
    )
}

data class CalendarEventItem(
    val id: String,
    val summary: String,
    val startTime: String,
    val endTime: String,
    val location: String = "",
    val isAllDay: Boolean = false
)

data class GmailMessageItem(
    val id: String,
    val subject: String,
    val from: String,
    val date: String,
    val snippet: String
)

data class McpWorkspaceState(
    val calendarConnected: Boolean = false,
    val gmailConnected: Boolean = false,
    val calendarLoading: Boolean = false,
    val gmailLoading: Boolean = false,
    val calendarEvents: List<CalendarEventItem> = emptyList(),
    val recentEmails: List<GmailMessageItem> = emptyList(),
    val calendarStatusMessage: String? = null,
    val gmailStatusMessage: String? = null,
    val lastCalendarSyncMillis: Long = 0L,
    val lastGmailSyncMillis: Long = 0L
)

class WorkspaceUnauthorizedException(message: String) : IOException(message)

/**
 * Initiates native Google Identity Services OAuth 2.0 scope authorization via AuthorizationClient.
 * Never logs or persists access tokens.
 */
object GoogleWorkspaceAuthHelper {

    fun requestScopeAuthorization(
        context: Context,
        connector: McpConnectorType,
        onResolutionRequired: (IntentSenderRequest) -> Unit,
        onAccessTokenReady: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val request = AuthorizationRequest.Builder()
                .setRequestedScopes(listOf(Scope(connector.oauthScope)))
                .build()

            Identity.getAuthorizationClient(context)
                .authorize(request)
                .addOnSuccessListener { result ->
                    handleAuthorizationResult(
                        connector = connector,
                        result = result,
                        onResolutionRequired = onResolutionRequired,
                        onAccessTokenReady = onAccessTokenReady,
                        onFailure = onFailure
                    )
                }
                .addOnFailureListener { error ->
                    Log.w(TAG, "${connector.displayName} authorization failed: ${error.javaClass.simpleName}")
                    val detail = when (error) {
                        is ApiException -> "Google Play Services OAuth error (code ${error.statusCode}): ${error.localizedMessage ?: "Authorization unavailable"}"
                        else -> "${error.javaClass.simpleName}: ${error.localizedMessage ?: "Unable to request authorization"}"
                    }
                    onFailure(detail)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error starting ${connector.displayName} authorization", e)
            onFailure("${e.javaClass.simpleName}: ${e.localizedMessage ?: "Unable to launch Google authorization"}")
        }
    }

    fun parseAuthorizationIntent(
        context: Context,
        connector: McpConnectorType,
        data: Intent?,
        onAccessTokenReady: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (data == null) {
            onFailure("${connector.displayName} authorization returned no data.")
            return
        }
        try {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
            val token = result.accessToken?.takeIf { it.isNotBlank() }
            if (token != null) {
                onAccessTokenReady(token)
            } else {
                onFailure("${connector.displayName} consent finished without an access token.")
            }
        } catch (e: ApiException) {
            Log.w(TAG, "${connector.displayName} consent resolution failed: status=${e.statusCode}")
            onFailure("${connector.displayName} OAuth error (code ${e.statusCode}): ${e.localizedMessage ?: "Permission denied or unavailable"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing ${connector.displayName} authorization result", e)
            onFailure("${e.javaClass.simpleName}: ${e.localizedMessage ?: "Failed to read OAuth result"}")
        }
    }

    private fun handleAuthorizationResult(
        connector: McpConnectorType,
        result: AuthorizationResult,
        onResolutionRequired: (IntentSenderRequest) -> Unit,
        onAccessTokenReady: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (result.hasResolution()) {
            val pendingIntent = result.pendingIntent
            if (pendingIntent != null) {
                onResolutionRequired(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            } else {
                onFailure("${connector.displayName} requires consent, but Play Services returned no PendingIntent.")
            }
            return
        }

        val token = result.accessToken?.takeIf { it.isNotBlank() }
        if (token != null) {
            onAccessTokenReady(token)
        } else {
            onFailure("${connector.displayName} authorization succeeded without an OAuth access token.")
        }
    }
}

/**
 * Lightweight OkHttp + org.json REST client for read-only Google Calendar and Gmail APIs.
 * Restricts outbound requests to official HTTPS Google API endpoints and validates message IDs.
 */
class GoogleWorkspaceRestClient {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchUpcomingCalendarEvents(
        accessToken: String,
        daysAhead: Int = 2
    ): Result<List<CalendarEventItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val clampedDays = daysAhead.coerceIn(1, 7)
            val (timeMinIso, timeMaxIso) = buildDateWindowIso(clampedDays)

            val url = CALENDAR_EVENTS_ENDPOINT.toHttpUrl().newBuilder()
                .addQueryParameter("singleEvents", "true")
                .addQueryParameter("orderBy", "startTime")
                .addQueryParameter("maxResults", "25")
                .addQueryParameter("timeMin", timeMinIso)
                .addQueryParameter("timeMax", timeMaxIso)
                .build()

            val body = executeAuthorizedGet(url.toString(), accessToken)
            val root = JSONObject(body)
            val items = root.optJSONArray("items") ?: return@runCatching emptyList()

            val events = mutableListOf<CalendarEventItem>()
            for (i in 0 until items.length()) {
                val obj = items.optJSONObject(i) ?: continue
                if (obj.optString("status") == "cancelled") continue

                val startObj = obj.optJSONObject("start")
                val endObj = obj.optJSONObject("end")
                val startDateTime = startObj?.optString("dateTime").orEmpty()
                val startDate = startObj?.optString("date").orEmpty()
                val endDateTime = endObj?.optString("dateTime").orEmpty()
                val endDate = endObj?.optString("date").orEmpty()

                val isAllDay = startDateTime.isBlank() && startDate.isNotBlank()
                val startLabel = if (isAllDay) startDate else formatIsoToDisplay(startDateTime)
                val endLabel = if (isAllDay) endDate else formatIsoToDisplay(endDateTime)

                events += CalendarEventItem(
                    id = obj.optString("id", "evt_$i"),
                    summary = obj.optString("summary").ifBlank { "(No title)" },
                    startTime = startLabel.ifBlank { "Scheduled" },
                    endTime = endLabel,
                    location = obj.optString("location").trim(),
                    isAllDay = isAllDay
                )
            }
            events
        }
    }

    suspend fun fetchRecentEmails(
        accessToken: String,
        maxResults: Int = 5
    ): Result<List<GmailMessageItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val clampedLimit = maxResults.coerceIn(1, 10)
            val listUrl = GMAIL_MESSAGES_ENDPOINT.toHttpUrl().newBuilder()
                .addQueryParameter("maxResults", clampedLimit.toString())
                .build()

            val listBody = executeAuthorizedGet(listUrl.toString(), accessToken)
            val listJson = JSONObject(listBody)
            val messagesArray = listJson.optJSONArray("messages") ?: return@runCatching emptyList()

            val emails = mutableListOf<GmailMessageItem>()
            val count = minOf(messagesArray.length(), clampedLimit)
            for (i in 0 until count) {
                val msgStub = messagesArray.optJSONObject(i) ?: continue
                val msgId = msgStub.optString("id").trim()
                if (!VALID_GMAIL_ID_REGEX.matches(msgId)) continue

                val detailUrl = "$GMAIL_MESSAGES_ENDPOINT/$msgId".toHttpUrl().newBuilder()
                    .addQueryParameter("format", "metadata")
                    .addQueryParameter("metadataHeaders", "Subject")
                    .addQueryParameter("metadataHeaders", "From")
                    .addQueryParameter("metadataHeaders", "Date")
                    .build()

                val detailBody = executeAuthorizedGet(detailUrl.toString(), accessToken)
                val detailJson = JSONObject(detailBody)
                val payload = detailJson.optJSONObject("payload")
                val headers = payload?.optJSONArray("headers")

                var subject = ""
                var from = ""
                var date = ""
                if (headers != null) {
                    for (h in 0 until headers.length()) {
                        val header = headers.optJSONObject(h) ?: continue
                        when (header.optString("name").lowercase(Locale.US)) {
                            "subject" -> subject = header.optString("value").trim()
                            "from" -> from = header.optString("value").trim()
                            "date" -> date = header.optString("value").trim()
                        }
                    }
                }

                val snippet = decodeHtmlEntities(detailJson.optString("snippet").trim())
                emails += GmailMessageItem(
                    id = msgId,
                    subject = subject.ifBlank { "(No subject)" },
                    from = cleanSenderDisplay(from),
                    date = date,
                    snippet = snippet.ifBlank { "No preview snippet available." }
                )
            }
            emails
        }
    }

    private fun executeAuthorizedGet(url: String, accessToken: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                return rawBody
            }

            val apiMessage = extractGoogleApiError(rawBody, response.code)
            if (response.code == 401) {
                throw WorkspaceUnauthorizedException("OAuth token expired or revoked (HTTP 401): $apiMessage. Tap Connect to refresh.")
            }
            throw IOException("Google API HTTP ${response.code}: $apiMessage")
        }
    }

    private fun extractGoogleApiError(rawBody: String, httpCode: Int): String {
        return try {
            val json = JSONObject(rawBody)
            val errObj = json.optJSONObject("error")
            errObj?.optString("message")?.takeIf { it.isNotBlank() }
                ?: json.optString("error_description").takeIf { it.isNotBlank() }
                ?: "Request failed with HTTP $httpCode"
        } catch (_: Exception) {
            "Request failed with HTTP $httpCode"
        }
    }

    private fun buildDateWindowIso(daysAhead: Int): Pair<String, String> {
        val localCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = localCal.timeInMillis
        localCal.add(Calendar.DAY_OF_YEAR, daysAhead)
        val endMillis = localCal.timeInMillis

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return isoFormat.format(startMillis) to isoFormat.format(endMillis)
    }

    private fun formatIsoToDisplay(iso: String): String {
        if (iso.isBlank()) return ""
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (pattern in patterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) {
                    parser.timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = parser.parse(iso) ?: continue
                val formatter = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())
                return formatter.format(parsed)
            } catch (_: Exception) {
                // Try next ISO-8601 variant
            }
        }
        return iso
    }

    private fun cleanSenderDisplay(rawFrom: String): String {
        if (rawFrom.isBlank()) return "Unknown sender"
        val angleIndex = rawFrom.indexOf('<')
        val display = if (angleIndex > 0) {
            rawFrom.substring(0, angleIndex).trim().trim('"')
        } else {
            rawFrom.trim()
        }
        return display.ifBlank { rawFrom }
    }

    private fun decodeHtmlEntities(input: String): String {
        return input
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
    }

    companion object {
        private const val CALENDAR_EVENTS_ENDPOINT =
            "https://www.googleapis.com/calendar/v3/calendars/primary/events"
        private const val GMAIL_MESSAGES_ENDPOINT =
            "https://www.googleapis.com/gmail/v1/users/me/messages"
        private val VALID_GMAIL_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,128}$")
    }
}
