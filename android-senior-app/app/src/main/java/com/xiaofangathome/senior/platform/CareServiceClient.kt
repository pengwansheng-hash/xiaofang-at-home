package com.xiaofangathome.senior.platform

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.xiaofangathome.senior.data.RemoteCarePlan
import com.xiaofangathome.senior.data.RemoteCarePlanEvent
import com.xiaofangathome.senior.data.RemoteImportantContact
import com.xiaofangathome.senior.data.RemoteSemanticMemory
import com.xiaofangathome.senior.data.RemoteSeniorProfile
import com.xiaofangathome.senior.data.RemoteTopicBrief
import com.xiaofangathome.senior.data.SemanticMemoryItem
import com.xiaofangathome.senior.data.ServiceSyncSnapshot
import com.xiaofangathome.senior.platform.CommunicationStyle

private const val CARE_SERVICE_PREFS = "xiaofang_senior_app"
private const val CARE_SERVICE_BASE_URL_KEY = "care_service_base_url"
private const val CARE_SERVICE_SENIOR_ID_KEY = "care_service_senior_id"
private const val DEFAULT_CARE_SERVICE_BASE_URL = "http://10.0.2.2:3301"
private const val DEFAULT_CARE_SERVICE_SENIOR_ID = "senior-zhang"

data class CompanionReplyContextMessage(
    val role: String,
    val content: String,
)

data class CompanionReplyConversationContext(
    val preferredName: String? = null,
    val communicationStyle: String? = null,
    val personaPrompt: String? = null,
    val commonTopics: List<String> = emptyList(),
    val tabooTopics: List<String> = emptyList(),
    val emotionHint: String? = null,
    val memoryHighlights: List<String> = emptyList(),
    val reminderHint: String? = null,
    val contactHint: String? = null,
    val recentConversationHint: String? = null,
    val collectionHint: String? = null,
)

data class CompanionReplyServiceResult(
    val mode: String,
    val provider: String,
    val model: String,
    val reply: String,
    val summary: String,
)

data class CareServiceRuntimeResult(
    val provider: String,
    val configured: Boolean,
    val model: String?,
    val baseUrl: String?,
    val timeoutMs: Int,
)

class CareServiceClient(
    private val context: Context,
) {
    fun fetchRuntimeStatus(baseUrlOverride: String? = null): CareServiceRuntimeResult? {
        return retryRequest {
            val connection = openConnection("/api/ai/runtime", "GET", baseUrlOverride) ?: return@retryRequest null

            try {
                val responseCode = connection.responseCode
                val responseText = readBody(connection, responseCode) ?: return@retryRequest null
                if (responseCode !in 200..299) {
                    return@retryRequest null
                }

                val json = JSONObject(responseText)
                CareServiceRuntimeResult(
                    provider = json.optString("provider", "unknown"),
                    configured = json.optBoolean("configured", false),
                    model = json.optString("model").trim().ifBlank { null },
                    baseUrl = json.optString("baseUrl").trim().ifBlank { null },
                    timeoutMs = json.optInt("timeoutMs", 0),
                )
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }
    }

    fun fetchServiceSyncSnapshot(): ServiceSyncSnapshot? {
        return retryRequest {
            val connection = openConnection("/api/seniors/${resolveSeniorId()}/sync-packet", "GET") ?: return@retryRequest null

            try {
                val responseCode = connection.responseCode
                val responseText = readBody(connection, responseCode) ?: return@retryRequest null
                if (responseCode !in 200..299) {
                    return@retryRequest null
                }

                val json = JSONObject(responseText)
                ServiceSyncSnapshot(
                    seniorId = json.optString("seniorId", resolveSeniorId()),
                    profile = json.optJSONObject("profile")?.let { profile ->
                        RemoteSeniorProfile(
                            seniorId = profile.optString("seniorId", resolveSeniorId()),
                            preferredName = profile.optString("preferredName"),
                            relationLabel = profile.optString("relationLabel"),
                            interests = readStringList(profile.optJSONArray("interests")),
                            hobbies = readStringList(profile.optJSONArray("hobbies")),
                            tabooTopics = readStringList(profile.optJSONArray("tabooTopics")),
                            communicationStyle = CommunicationStyle.fromStorage(
                                profile.optString("communicationStyle", CommunicationStyle.PatientDelicate.name),
                            ),
                            routineSummary = profile.optString("routineSummary"),
                            personaTags = readStringList(profile.optJSONArray("personaTags")),
                            importantContacts = readImportantContacts(profile.optJSONArray("importantContacts")),
                            updatedAt = profile.optString("updatedAt"),
                        )
                    },
                    carePlans = readCarePlans(json.optJSONArray("carePlans")),
                    latestEvents = readCarePlanEvents(json.optJSONArray("latestEvents")),
                    topicBriefs = readTopicBriefs(json.optJSONArray("topicBriefs")),
                    semanticMemories = readSemanticMemories(json.optJSONArray("semanticMemories")),
                    generatedAt = json.optString("generatedAt"),
                )
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }
    }

    fun fetchCompanionReply(
        userText: String,
        recentMessages: List<CompanionReplyContextMessage>,
        conversationContext: CompanionReplyConversationContext? = null,
    ): CompanionReplyServiceResult? {
        val requestBody = JSONObject().apply {
            put("seniorId", resolveSeniorId())
            put("userText", userText)
            put("recentMessages", JSONArray().apply {
                recentMessages.forEach { message ->
                    put(
                        JSONObject().apply {
                            put("role", message.role)
                            put("content", message.content)
                        },
                    )
                }
            })
            conversationContext?.let { context ->
                put("conversationContext", JSONObject().apply {
                    put("preferredName", context.preferredName.orEmpty())
                    put("communicationStyle", context.communicationStyle.orEmpty())
                    put("personaPrompt", context.personaPrompt.orEmpty())
                    put("commonTopics", JSONArray(context.commonTopics))
                    put("tabooTopics", JSONArray(context.tabooTopics))
                    put("emotionHint", context.emotionHint.orEmpty())
                    put("memoryHighlights", JSONArray(context.memoryHighlights))
                    put("reminderHint", context.reminderHint.orEmpty())
                    put("contactHint", context.contactHint.orEmpty())
                    put("recentConversationHint", context.recentConversationHint.orEmpty())
                    put("collectionHint", context.collectionHint.orEmpty())
                })
            }
        }

        return retryRequest(attempts = 1, delayMs = 0L) {
            val connection = openConnection("/api/ai/companion-reply", "POST") ?: return@retryRequest null

            try {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(requestBody.toString())
                }

                val responseCode = connection.responseCode
                val responseText = readBody(connection, responseCode) ?: return@retryRequest null
                if (responseCode !in 200..299) {
                    return@retryRequest null
                }

                val json = JSONObject(responseText)
                val reply = json.optString("reply").trim()
                if (reply.isBlank()) {
                    return@retryRequest null
                }

                CompanionReplyServiceResult(
                    mode = json.optString("mode", "fallback"),
                    provider = json.optString("provider", "unknown"),
                    model = json.optString("model", "unknown"),
                    reply = reply,
                    summary = json.optString("summary", "").trim(),
                )
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }
    }

    fun syncSemanticMemories(memories: List<SemanticMemoryItem>): Boolean {
        val connection = openConnection("/api/seniors/${resolveSeniorId()}/semantic-memories", "PUT") ?: return false

        return try {
            val body = JSONObject().apply {
                put("semanticMemories", JSONArray().apply {
                    memories.sortedByDescending { it.lastAccessedAt }.take(40).forEach { memory ->
                        put(
                            JSONObject().apply {
                                put("id", memory.id)
                                put("memoryType", memory.memoryType.name)
                                put("memoryLayer", memory.memoryLayer.name)
                                put("retention", memory.retention.name)
                                put("title", memory.title)
                                put("summary", memory.summary)
                                put("compressedSummary", memory.compressedSummary)
                                put("keywords", JSONArray(memory.keywords))
                                put("sourceText", memory.sourceText)
                                put("confidence", memory.confidence)
                                put("createdAt", memory.createdAt)
                                put("updatedAt", memory.updatedAt)
                                put("sourceCount", memory.sourceCount)
                                put("evidenceCount", memory.evidenceCount)
                                put("lastAccessedAt", memory.lastAccessedAt)
                                put("lastConfirmedAt", memory.lastConfirmedAt)
                                put("expiresAt", memory.expiresAt)
                            },
                        )
                    }
                })
            }

            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
            }

            val responseCode = connection.responseCode
            readBody(connection, responseCode) ?: return false
            if (responseCode !in 200..299) {
                return false
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(
        path: String,
        method: String,
        baseUrlOverride: String? = null,
    ): HttpURLConnection? {
        return try {
            val baseUrl = resolveBaseUrl(baseUrlOverride).trimEnd('/')
            (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 6_000
                readTimeout = 8_500
                doInput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                if (method == "POST" || method == "PUT") {
                    doOutput = true
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveBaseUrl(baseUrlOverride: String? = null): String {
        val overrideBaseUrl = baseUrlOverride?.trim()?.takeIf { it.isNotBlank() }
        if (overrideBaseUrl != null) {
            return overrideBaseUrl
        }

        val preferences = context.getSharedPreferences(CARE_SERVICE_PREFS, Context.MODE_PRIVATE)
        return preferences.getString(CARE_SERVICE_BASE_URL_KEY, DEFAULT_CARE_SERVICE_BASE_URL)
            .orEmpty()
            .trim()
            .ifBlank { DEFAULT_CARE_SERVICE_BASE_URL }
    }

    private fun resolveSeniorId(): String {
        val preferences = context.getSharedPreferences(CARE_SERVICE_PREFS, Context.MODE_PRIVATE)
        return preferences.getString(CARE_SERVICE_SENIOR_ID_KEY, DEFAULT_CARE_SERVICE_SENIOR_ID)
            .orEmpty()
            .trim()
            .ifBlank { DEFAULT_CARE_SERVICE_SENIOR_ID }
    }

    private fun readBody(connection: HttpURLConnection, responseCode: Int): String? {
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            reader.readText()
        }
    }

    private fun <T> retryRequest(
        attempts: Int = 2,
        delayMs: Long = 250L,
        request: () -> T?,
    ): T? {
        repeat(attempts) { index ->
            val result = request()
            if (result != null) {
                return result
            }
            if (index < attempts - 1) {
                runCatching { Thread.sleep(delayMs) }
            }
        }
        return null
    }

    private fun readStringList(array: JSONArray?): List<String> {
        if (array == null) {
            return emptyList()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val value = array.optString(index).trim()
                if (value.isNotBlank()) {
                    add(value)
                }
            }
        }
    }

    private fun readImportantContacts(array: JSONArray?): List<RemoteImportantContact> {
        if (array == null) {
            return emptyList()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    RemoteImportantContact(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        relation = item.optString("relation"),
                        phone = item.optString("phone"),
                        priority = item.optInt("priority", 0),
                    ),
                )
            }
        }
    }

    private fun readCarePlans(array: JSONArray?): List<RemoteCarePlan> {
        if (array == null) {
            return emptyList()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    RemoteCarePlan(
                        planId = item.optString("planId"),
                        seniorId = item.optString("seniorId"),
                        title = item.optString("title"),
                        schedule = item.optString("schedule"),
                        frequency = item.optString("frequency"),
                        channel = item.optString("channel"),
                        confirmRequired = item.optBoolean("confirmRequired", true),
                        status = item.optString("status"),
                        updatedAt = item.optString("updatedAt"),
                    ),
                )
            }
        }
    }

    private fun readCarePlanEvents(array: JSONArray?): List<RemoteCarePlanEvent> {
        if (array == null) {
            return emptyList()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    RemoteCarePlanEvent(
                        eventId = item.optString("eventId"),
                        planId = item.optString("planId"),
                        eventType = item.optString("eventType"),
                        payloadSummary = item.optJSONObject("payload")?.toString().orEmpty(),
                        createdAt = item.optString("createdAt"),
                    ),
                )
            }
        }
    }

    private fun readTopicBriefs(array: JSONArray?): List<RemoteTopicBrief> {
        if (array == null) {
            return emptyList()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    RemoteTopicBrief(
                        topicId = item.optString("topicId"),
                        title = item.optString("title"),
                        summary = item.optString("summary"),
                        sourceName = item.optString("sourceName"),
                        generatedAt = item.optString("generatedAt"),
                    ),
                )
            }
        }
    }

    private fun readSemanticMemories(array: JSONArray?): List<RemoteSemanticMemory> {
        if (array == null) {
            return emptyList()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    RemoteSemanticMemory(
                        seniorId = item.optString("seniorId"),
                        id = item.optString("id"),
                        memoryType = item.optString("memoryType"),
                        memoryLayer = item.optString("memoryLayer", "Profile"),
                        retention = item.optString("retention", "LongTerm"),
                        title = item.optString("title"),
                        summary = item.optString("summary"),
                        compressedSummary = item.optString("compressedSummary"),
                        keywords = readStringList(item.optJSONArray("keywords")),
                        sourceText = item.optString("sourceText"),
                        confidence = item.optDouble("confidence", 0.5),
                        createdAt = item.optLong("createdAt", 0L),
                        updatedAt = item.optLong("updatedAt", 0L),
                        sourceCount = item.optInt("sourceCount", 1),
                        evidenceCount = item.optInt("evidenceCount", item.optInt("sourceCount", 1)),
                        lastAccessedAt = item.optLong("lastAccessedAt", 0L),
                        lastConfirmedAt = item.optLong("lastConfirmedAt", item.optLong("updatedAt", 0L)),
                        expiresAt = item.optLong("expiresAt").takeIf { it > 0L },
                    ),
                )
            }
        }
    }
}
