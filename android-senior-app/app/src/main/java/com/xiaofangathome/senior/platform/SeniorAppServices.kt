package com.xiaofangathome.senior.platform

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.xiaofangathome.senior.MainActivity
import com.xiaofangathome.senior.R
import com.xiaofangathome.senior.data.ChatMessage
import com.xiaofangathome.senior.data.ContactItem
import com.xiaofangathome.senior.data.FREQUENCY_DAILY
import com.xiaofangathome.senior.data.FREQUENCY_ONCE
import com.xiaofangathome.senior.data.FREQUENCY_WEEKLY
import com.xiaofangathome.senior.data.MemoryLayer
import com.xiaofangathome.senior.data.MemoryRetention
import com.xiaofangathome.senior.data.MockRepository
import com.xiaofangathome.senior.data.ReminderItem
import com.xiaofangathome.senior.data.ReminderStatus
import com.xiaofangathome.senior.data.SemanticMemoryItem
import com.xiaofangathome.senior.data.SemanticMemoryType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.UUID

private const val APP_PREFERENCES_NAME = "xiaofang_senior_app"
private const val REMINDERS_FILE_NAME = "reminders.json"
private const val CONTACTS_FILE_NAME = "contacts.json"
private const val CHAT_MESSAGES_FILE_NAME = "chat_messages.json"
private const val SEMANTIC_MEMORIES_FILE_NAME = "semantic_memories.json"
private const val LEGACY_REMINDERS_PREF_KEY = "reminders_json"
private const val LEGACY_CONTACTS_PREF_KEY = "contacts_json"
private const val LEGACY_CHAT_MESSAGES_PREF_KEY = "chat_messages_json"
private const val LEGACY_SEMANTIC_MEMORIES_PREF_KEY = "semantic_memories_json"
private const val LEGACY_FILE_MIGRATION_DONE_KEY = "legacy_file_migration_done_v1"
private const val MAX_CHAT_MESSAGE_COUNT = 24
private const val MAX_SEMANTIC_MEMORY_COUNT = 40
private const val ACTION_REMINDER_NOTIFICATION = "com.xiaofangathome.senior.REMINDER_NOTIFICATION"
private const val EXTRA_REMINDER_ID = "extra_reminder_id"
private const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
private const val EXTRA_REMINDER_BODY = "extra_reminder_body"

internal fun pendingIntentCanBeMissing(flags: Int): Boolean {
    return flags and PendingIntent.FLAG_NO_CREATE != 0
}

enum class CommunicationStyle(
    val displayName: String,
    val personaName: String,
    val systemPrompt: String,
) {
    PatientDelicate(
        displayName = "温和",
        personaName = "小芳",
        systemPrompt = """
            你是“小芳在家”的陪伴助手。
            语气温和、自然，不要过度关怀，不要刻意打听隐私。
            像熟悉的晚辈一样聊天，先接住对方的话，再顺着往下说。
        """.trimIndent(),
    ),
    ConfidentMature(
        displayName = "利落",
        personaName = "小芳",
        systemPrompt = """
            你是“小芳在家”的陪伴助手。
            语气自然、利落、真诚，不端着，也不说教。
            像熟人聊天一样接话，少模板，少盘问。
        """.trimIndent(),
    );

    companion object {
        fun fromStorage(raw: String?): CommunicationStyle {
            return when (raw) {
                PatientDelicate.name, "Warm", "patient_gentle" -> PatientDelicate
                ConfidentMature.name, "Brief", "confident_steady" -> ConfidentMature
                else -> PatientDelicate
            }
        }
    }
}

data class SeniorPreferencesSnapshot(
    val preferredName: String = "",
    val communicationStyle: CommunicationStyle = CommunicationStyle.PatientDelicate,
    val commonTopics: String = "",
    val tabooWords: String = "",
    val careServiceBaseUrl: String = "",
    val careServiceSeniorId: String = "",
    val hourlyLocationTrackingEnabled: Boolean = false,
)

interface SeniorPreferencesStore {
    fun load(): SeniorPreferencesSnapshot
    fun save(snapshot: SeniorPreferencesSnapshot)
}

interface ReminderRepository {
    fun loadAll(): List<ReminderItem>
    fun saveAll(reminders: List<ReminderItem>)
}

interface ContactRepository {
    fun loadAll(): List<ContactItem>
    fun saveAll(contacts: List<ContactItem>)
}

interface ChatMemoryRepository {
    fun loadRecent(): List<ChatMessage>
    fun saveRecent(messages: List<ChatMessage>)
}

interface SemanticMemoryRepository {
    fun loadAll(): List<SemanticMemoryItem>
    fun saveAll(memories: List<SemanticMemoryItem>)
}

data class ReminderSyncResult(
    val scheduledCount: Int,
    val message: String,
)

data class VoiceCaptureResult(
    val isRecording: Boolean,
    val message: String,
    val lastRecordingSummary: String? = null,
    val lastRecordingPath: String? = null,
)

data class VoicePlaybackResult(
    val isPlaying: Boolean,
    val message: String,
)

data class VoiceTranscriptionResult(
    val isListening: Boolean,
    val message: String,
    val transcript: String? = null,
)

interface ReminderScheduler {
    fun sync(reminders: List<ReminderItem>): ReminderSyncResult
}

interface VoiceCapability {
    fun startCapture(): VoiceCaptureResult
    fun stopCapture(): VoiceCaptureResult
    fun playRecording(recordingPath: String?, onPlaybackFinished: (VoicePlaybackResult) -> Unit): VoicePlaybackResult
    fun stopPlayback(): VoicePlaybackResult
    fun startTranscription(
        onResult: (VoiceTranscriptionResult) -> Unit,
        onLevelChange: (Float) -> Unit = {},
    ): VoiceTranscriptionResult
    fun stopTranscription(): VoiceTranscriptionResult
    fun speak(text: String): String
}

private class SharedPreferencesSeniorPreferencesStore(
    private val preferences: SharedPreferences,
) : SeniorPreferencesStore {
    override fun load(): SeniorPreferencesSnapshot {
        return SeniorPreferencesSnapshot(
            preferredName = preferences.getString("preferred_name", "").orEmpty(),
            communicationStyle = CommunicationStyle.fromStorage(
                preferences.getString("communication_style", CommunicationStyle.PatientDelicate.name),
            ),
            commonTopics = preferences.getString("common_topics", "").orEmpty(),
            tabooWords = preferences.getString("taboo_words", "").orEmpty(),
            careServiceBaseUrl = preferences.getString("care_service_base_url", "").orEmpty(),
            careServiceSeniorId = preferences.getString("care_service_senior_id", "").orEmpty(),
            hourlyLocationTrackingEnabled = preferences.getBoolean("hourly_location_tracking_enabled", false),
        )
    }

    override fun save(snapshot: SeniorPreferencesSnapshot) {
        preferences.edit()
            .putString("preferred_name", snapshot.preferredName)
            .putString("communication_style", snapshot.communicationStyle.name)
            .putString("common_topics", snapshot.commonTopics)
            .putString("taboo_words", snapshot.tabooWords)
            .putString("care_service_base_url", snapshot.careServiceBaseUrl)
            .putString("care_service_senior_id", snapshot.careServiceSeniorId)
            .putBoolean("hourly_location_tracking_enabled", snapshot.hourlyLocationTrackingEnabled)
            .apply()
    }
}

private abstract class JsonFileRepository(
    context: Context,
    fileName: String,
) {
    private val legacyPreferences = context.getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE)
    protected val file = File(context.filesDir, fileName)

    protected fun readArray(): JSONArray {
        if (!file.exists()) return JSONArray()
        val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrNull().orEmpty()
        if (raw.isBlank()) return JSONArray()
        return runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
    }

    protected fun writeArray(array: JSONArray) {
        file.parentFile?.mkdirs()
        file.writeText(array.toString(), Charsets.UTF_8)
    }

    protected fun readArrayWithLegacyFallback(legacyKey: String): JSONArray {
        val current = readArray()
        if (current.length() > 0) return current

        val legacyRaw = legacyPreferences.getString(legacyKey, null)?.trim().orEmpty()
        if (!legacyRaw.startsWith("[")) return current

        val legacy = runCatching { JSONArray(legacyRaw) }.getOrNull() ?: return current
        if (legacy.length() == 0) return current

        writeArray(legacy)
        return legacy
    }
}

private class FileReminderRepository(
    context: Context,
) : JsonFileRepository(context, REMINDERS_FILE_NAME), ReminderRepository {
    override fun loadAll(): List<ReminderItem> {
        val array = readArrayWithLegacyFallback(LEGACY_REMINDERS_PREF_KEY)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    ReminderItem(
                        id = item.optString("id"),
                        time = item.optString("time"),
                        title = item.optString("title"),
                        description = item.optString("description"),
                        status = runCatching { ReminderStatus.valueOf(item.optString("status")) }.getOrDefault(ReminderStatus.Planned),
                        frequencyLabel = item.optString("frequencyLabel", FREQUENCY_DAILY),
                        weeklyDays = readIntList(item.optJSONArray("weeklyDays")),
                        voiceEnabled = item.optBoolean("voiceEnabled", true),
                        alarmEnabled = item.optBoolean("alarmEnabled", true),
                    ),
                )
            }
        }
    }

    override fun saveAll(reminders: List<ReminderItem>) {
        val array = JSONArray()
        reminders.forEach { reminder ->
            array.put(
                JSONObject()
                    .put("id", reminder.id)
                    .put("time", reminder.time)
                    .put("title", reminder.title)
                    .put("description", reminder.description)
                    .put("status", reminder.status.name)
                    .put("frequencyLabel", reminder.frequencyLabel)
                    .put("weeklyDays", JSONArray(reminder.weeklyDays))
                    .put("voiceEnabled", reminder.voiceEnabled)
                    .put("alarmEnabled", reminder.alarmEnabled),
            )
        }
        writeArray(array)
    }
}

private class FileContactRepository(
    context: Context,
) : JsonFileRepository(context, CONTACTS_FILE_NAME), ContactRepository {
    override fun loadAll(): List<ContactItem> {
        val array = readArrayWithLegacyFallback(LEGACY_CONTACTS_PREF_KEY)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    ContactItem(
                        id = item.optString("id"),
                        relation = item.optString("relation"),
                        name = item.optString("name"),
                        phone = item.optString("phone"),
                    ),
                )
            }
        }
    }

    override fun saveAll(contacts: List<ContactItem>) {
        val array = JSONArray()
        contacts.forEach { contact ->
            array.put(
                JSONObject()
                    .put("id", contact.id)
                    .put("relation", contact.relation)
                    .put("name", contact.name)
                    .put("phone", contact.phone),
            )
        }
        writeArray(array)
    }
}

private class FileChatMemoryRepository(
    context: Context,
) : JsonFileRepository(context, CHAT_MESSAGES_FILE_NAME), ChatMemoryRepository {
    override fun loadRecent(): List<ChatMessage> {
        val array = readArrayWithLegacyFallback(LEGACY_CHAT_MESSAGES_PREF_KEY)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    ChatMessage(
                        id = item.optString("id"),
                        fromAi = item.optBoolean("fromAi"),
                        content = item.optString("content"),
                        timeLabel = item.optString("timeLabel").ifBlank { null },
                        createdAt = item.optLong("createdAt", 0L),
                    ),
                )
            }
        }.sortedBy { it.createdAt }
    }

    override fun saveRecent(messages: List<ChatMessage>) {
        val array = JSONArray()
        messages.takeLast(MAX_CHAT_MESSAGE_COUNT).forEach { message ->
            array.put(
                JSONObject()
                    .put("id", message.id)
                    .put("fromAi", message.fromAi)
                    .put("content", message.content)
                    .put("timeLabel", message.timeLabel)
                    .put("createdAt", message.createdAt),
            )
        }
        writeArray(array)
    }
}

private class FileSemanticMemoryRepository(
    context: Context,
) : JsonFileRepository(context, SEMANTIC_MEMORIES_FILE_NAME), SemanticMemoryRepository {
    override fun loadAll(): List<SemanticMemoryItem> {
        val array = readArrayWithLegacyFallback(LEGACY_SEMANTIC_MEMORIES_PREF_KEY)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    SemanticMemoryItem(
                        id = item.optString("id"),
                        memoryType = runCatching { SemanticMemoryType.valueOf(item.optString("memoryType")) }.getOrDefault(SemanticMemoryType.Profile),
                        memoryLayer = runCatching { MemoryLayer.valueOf(item.optString("memoryLayer")) }.getOrDefault(MemoryLayer.Profile),
                        retention = runCatching { MemoryRetention.valueOf(item.optString("retention")) }.getOrDefault(MemoryRetention.LongTerm),
                        title = item.optString("title"),
                        summary = item.optString("summary"),
                        compressedSummary = item.optString("compressedSummary"),
                        keywords = readStringList(item.optJSONArray("keywords")),
                        sourceText = item.optString("sourceText"),
                        confidence = item.optDouble("confidence", 0.5),
                        createdAt = item.optLong("createdAt", 0L),
                        updatedAt = item.optLong("updatedAt", item.optLong("createdAt", 0L)),
                        sourceCount = item.optInt("sourceCount", 1),
                        evidenceCount = item.optInt("evidenceCount", item.optInt("sourceCount", 1)),
                        lastAccessedAt = item.optLong("lastAccessedAt", item.optLong("createdAt", 0L)),
                        lastConfirmedAt = item.optLong("lastConfirmedAt", item.optLong("updatedAt", item.optLong("createdAt", 0L))),
                        expiresAt = item.takeIf { !it.isNull("expiresAt") }?.optLong("expiresAt"),
                    ),
                )
            }
        }.sortedByDescending { it.lastAccessedAt }
    }

    override fun saveAll(memories: List<SemanticMemoryItem>) {
        val array = JSONArray()
        memories.take(MAX_SEMANTIC_MEMORY_COUNT).forEach { memory ->
            array.put(
                JSONObject()
                    .put("id", memory.id)
                    .put("memoryType", memory.memoryType.name)
                    .put("memoryLayer", memory.memoryLayer.name)
                    .put("retention", memory.retention.name)
                    .put("title", memory.title)
                    .put("summary", memory.summary)
                    .put("compressedSummary", memory.compressedSummary)
                    .put("keywords", JSONArray(memory.keywords))
                    .put("sourceText", memory.sourceText)
                    .put("confidence", memory.confidence)
                    .put("createdAt", memory.createdAt)
                    .put("updatedAt", memory.updatedAt)
                    .put("sourceCount", memory.sourceCount)
                    .put("evidenceCount", memory.evidenceCount)
                    .put("lastAccessedAt", memory.lastAccessedAt)
                    .put("lastConfirmedAt", memory.lastConfirmedAt)
                    .put("expiresAt", memory.expiresAt),
            )
        }
        writeArray(array)
    }
}

private class AlarmManagerReminderScheduler(
    private val context: Context,
) : ReminderScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun sync(reminders: List<ReminderItem>): ReminderSyncResult {
        reminders.forEach { reminder ->
            buildPendingIntent(reminder, PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
        }

        var scheduledCount = 0
        reminders
            .filter { it.status != ReminderStatus.Completed }
            .filter { it.alarmEnabled || it.voiceEnabled }
            .forEach { reminder ->
                val triggerAtMillis = computeNextTrigger(reminder) ?: return@forEach
                val pendingIntent =
                    buildPendingIntent(reminder, PendingIntent.FLAG_UPDATE_CURRENT)
                        ?: error("Expected reminder PendingIntent to be created for ${reminder.id}")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
                scheduledCount += 1
            }

        val message = if (scheduledCount > 0) {
            "已同步 $scheduledCount 条提醒到本地通知。"
        } else {
            "当前没有需要调度的提醒。"
        }
        return ReminderSyncResult(scheduledCount = scheduledCount, message = message)
    }

    private fun buildPendingIntent(reminder: ReminderItem, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ACTION_REMINDER_NOTIFICATION
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(EXTRA_REMINDER_BODY, reminder.description)
        }
        if (pendingIntentCanBeMissing(flags)) {
            return PendingIntent.getBroadcast(
                context,
                reminder.id.hashCode(),
                intent,
                flags or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        return PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun computeNextTrigger(reminder: ReminderItem): Long? {
        val hour = reminder.time.substringBefore(":").toIntOrNull() ?: return null
        val minute = reminder.time.substringAfter(":", "0").toIntOrNull() ?: return null
        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
        }

        return when (reminder.frequencyLabel) {
            FREQUENCY_WEEKLY -> {
                val weeklyDays = reminder.weeklyDays.ifEmpty { listOf(now.get(Calendar.DAY_OF_WEEK)) }
                repeat(8) { offset ->
                    val current = (candidate.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, offset)
                    }
                    val targetDay = toWeeklyDay(current.get(Calendar.DAY_OF_WEEK))
                    if (weeklyDays.contains(targetDay) && current.timeInMillis > now.timeInMillis) {
                        return current.timeInMillis
                    }
                }
                null
            }
            FREQUENCY_ONCE -> {
                if (candidate.timeInMillis > now.timeInMillis) candidate.timeInMillis else null
            }
            else -> {
                if (candidate.timeInMillis <= now.timeInMillis) {
                    candidate.add(Calendar.DAY_OF_YEAR, 1)
                }
                candidate.timeInMillis
            }
        }
    }

    private fun toWeeklyDay(calendarDay: Int): Int {
        return when (calendarDay) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }
}

class AndroidVoiceCapability(
    context: Context,
) : VoiceCapability, TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var ttsReady = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var transcriptionCallback: ((VoiceTranscriptionResult) -> Unit)? = null
    private var levelCallback: ((Float) -> Unit)? = null
    private var isListening = false
    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINA.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts?.language = Locale.CHINA
        }
    }

    override fun startCapture(): VoiceCaptureResult {
        return VoiceCaptureResult(
            isRecording = false,
            message = "当前版本建议直接使用系统语音识别或打字。",
        )
    }

    override fun stopCapture(): VoiceCaptureResult {
        return VoiceCaptureResult(
            isRecording = false,
            message = "录音能力当前未启用。",
        )
    }

    override fun playRecording(
        recordingPath: String?,
        onPlaybackFinished: (VoicePlaybackResult) -> Unit,
    ): VoicePlaybackResult {
        val result = VoicePlaybackResult(
            isPlaying = false,
            message = "录音回放当前未启用。",
        )
        onPlaybackFinished(result)
        return result
    }

    override fun stopPlayback(): VoicePlaybackResult {
        return VoicePlaybackResult(
            isPlaying = false,
            message = "当前没有正在播放的语音。",
        )
    }

    override fun startTranscription(
        onResult: (VoiceTranscriptionResult) -> Unit,
        onLevelChange: (Float) -> Unit,
    ): VoiceTranscriptionResult {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            return VoiceTranscriptionResult(
                isListening = false,
                message = "当前设备暂不支持系统语音识别。",
            )
        }
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return VoiceTranscriptionResult(
                isListening = false,
                message = "需要先打开麦克风权限。",
            )
        }
        if (isListening) {
            return VoiceTranscriptionResult(
                isListening = true,
                message = "已经在听了。",
            )
        }

        ensureSpeechRecognizer()
        transcriptionCallback = onResult
        levelCallback = onLevelChange
        isListening = true
        levelCallback?.invoke(0.15f)
        return try {
            speechRecognizer?.startListening(recognizerIntent)
            VoiceTranscriptionResult(
                isListening = true,
                message = "开始收听了，说完后点一下停止。",
            )
        } catch (_: Exception) {
            isListening = false
            VoiceTranscriptionResult(
                isListening = false,
                message = "语音识别暂时没打开成功。",
            )
        }
    }

    override fun stopTranscription(): VoiceTranscriptionResult {
        runCatching { speechRecognizer?.stopListening() }
        isListening = false
        levelCallback?.invoke(0f)
        return VoiceTranscriptionResult(
            isListening = false,
            message = "已停止收听。",
            transcript = null,
        )
    }

    override fun speak(text: String): String {
        if (!ttsReady) {
            return "当前设备暂时无法播报。"
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "xiaofang_tts")
        return "已开始播报。"
    }

    private fun ensureSpeechRecognizer() {
        if (speechRecognizer != null) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        levelCallback?.invoke(0.2f)
                    }

                    override fun onBeginningOfSpeech() {
                        levelCallback?.invoke(0.35f)
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = (rmsdB / 12f).coerceIn(0f, 1f)
                        levelCallback?.invoke(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() {
                        isListening = false
                        levelCallback?.invoke(0f)
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        levelCallback?.invoke(0f)
                        mainHandler.post {
                            transcriptionCallback?.invoke(
                                VoiceTranscriptionResult(
                                    isListening = false,
                                    message = "这次没有听清楚，我们再说一次。",
                                    transcript = null,
                                ),
                            )
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        levelCallback?.invoke(0f)
                        val transcript = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                        mainHandler.post {
                            transcriptionCallback?.invoke(
                                VoiceTranscriptionResult(
                                    isListening = false,
                                    message = if (transcript.isNullOrBlank()) "这次没有听清楚，我们再说一次。" else "识别完成。",
                                    transcript = transcript,
                                ),
                            )
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) = Unit

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                },
            )
        }
    }
}

class ReminderNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER_NOTIFICATION) return

        SeniorAppServices.ensureInitialized(context.applicationContext)
        SeniorAppServices.createNotificationChannel(context.applicationContext)

        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID).orEmpty()
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE).orEmpty().ifBlank { "提醒" }
        val body = intent.getStringExtra(EXTRA_REMINDER_BODY).orEmpty().ifBlank { "有一条新的提醒需要处理。" }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(ReminderDetailRouteExtra.KEY_REMINDER_ID, reminderId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, SeniorAppServices.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)
        }

        SeniorAppServices.reminderScheduler.sync(SeniorAppServices.reminderRepository.loadAll())
    }
}

class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SeniorAppServices.ensureInitialized(context.applicationContext)
        SeniorAppServices.reminderScheduler.sync(SeniorAppServices.reminderRepository.loadAll())
        SeniorAppServices.locationTrackingScheduler.sync(
            SeniorAppServices.preferencesStore.load().hourlyLocationTrackingEnabled,
        )
    }
}

object SeniorAppServices {
    const val NOTIFICATION_CHANNEL_ID = "senior_reminders"

    private var initialized = false
    private lateinit var appContext: Context

    lateinit var preferencesStore: SeniorPreferencesStore
        private set
    lateinit var reminderRepository: ReminderRepository
        private set
    lateinit var contactRepository: ContactRepository
        private set
    lateinit var chatMemoryRepository: ChatMemoryRepository
        private set
    lateinit var semanticMemoryRepository: SemanticMemoryRepository
        private set
    lateinit var reminderScheduler: ReminderScheduler
        private set
    lateinit var locationSampleRepository: LocationSampleRepository
        private set
    lateinit var locationTrackingScheduler: LocationTrackingScheduler
        private set
    lateinit var voiceCapability: VoiceCapability
        private set

    fun initialize(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        val preferences = appContext.getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE)
        migrateLegacyJsonStoresIfNeeded(preferences, appContext)

        preferencesStore = SharedPreferencesSeniorPreferencesStore(preferences)
        reminderRepository = FileReminderRepository(appContext)
        contactRepository = FileContactRepository(appContext)
        chatMemoryRepository = FileChatMemoryRepository(appContext)
        semanticMemoryRepository = FileSemanticMemoryRepository(appContext)
        reminderScheduler = AlarmManagerReminderScheduler(appContext)
        locationSampleRepository = FileLocationSampleRepository(appContext)
        locationTrackingScheduler = AlarmManagerLocationTrackingScheduler(appContext, preferences)
        voiceCapability = AndroidVoiceCapability(appContext)

        createNotificationChannel(appContext)
        seedDefaultsIfNeeded()
        reminderScheduler.sync(reminderRepository.loadAll())
        locationTrackingScheduler.sync(preferencesStore.load().hourlyLocationTrackingEnabled)
        initialized = true
    }

    fun ensureInitialized(context: Context) {
        if (!initialized) {
            initialize(context)
        }
    }

    private fun migrateLegacyJsonStoresIfNeeded(
        preferences: SharedPreferences,
        context: Context,
    ) {
        if (preferences.getBoolean(LEGACY_FILE_MIGRATION_DONE_KEY, false)) {
            return
        }

        migrateLegacyJsonFile(
            preferences = preferences,
            legacyKey = LEGACY_REMINDERS_PREF_KEY,
            targetFile = File(context.filesDir, REMINDERS_FILE_NAME),
        )
        migrateLegacyJsonFile(
            preferences = preferences,
            legacyKey = LEGACY_CONTACTS_PREF_KEY,
            targetFile = File(context.filesDir, CONTACTS_FILE_NAME),
        )
        migrateLegacyJsonFile(
            preferences = preferences,
            legacyKey = LEGACY_CHAT_MESSAGES_PREF_KEY,
            targetFile = File(context.filesDir, CHAT_MESSAGES_FILE_NAME),
        )
        migrateLegacyJsonFile(
            preferences = preferences,
            legacyKey = LEGACY_SEMANTIC_MEMORIES_PREF_KEY,
            targetFile = File(context.filesDir, SEMANTIC_MEMORIES_FILE_NAME),
        )

        preferences.edit().putBoolean(LEGACY_FILE_MIGRATION_DONE_KEY, true).apply()
    }

    private fun migrateLegacyJsonFile(
        preferences: SharedPreferences,
        legacyKey: String,
        targetFile: File,
    ) {
        migrateLegacyJsonFileIfNeeded(
            legacyJson = preferences.getString(legacyKey, null)?.trim().orEmpty(),
            targetFile = targetFile,
        )
    }

    internal fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "小芳在家提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "用于日常提醒和重要提示。"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun seedDefaultsIfNeeded() {
        if (reminderRepository.loadAll().isEmpty()) {
            reminderRepository.saveAll(MockRepository.reminders)
        }
        if (chatMemoryRepository.loadRecent().isEmpty()) {
            chatMemoryRepository.saveRecent(MockRepository.chatMessages)
        }
    }
}

private fun readStringList(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val value = array.optString(index).trim()
            if (value.isNotBlank()) {
                add(value)
            }
        }
    }
}

internal fun migrateLegacyJsonFileIfNeeded(
    legacyJson: String,
    targetFile: File,
): Boolean {
    val payload = legacyJson.trim()
    if (payload.isBlank()) return false
    if (!payload.startsWith("[")) return false

    val currentRaw = runCatching { targetFile.readText(Charsets.UTF_8) }.getOrNull().orEmpty().trim()
    if (currentRaw.isNotBlank()) return false

    targetFile.parentFile?.mkdirs()
    targetFile.writeText(payload, Charsets.UTF_8)
    return true
}

private fun readIntList(array: JSONArray?): List<Int> {
    if (array == null) return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val value = array.optInt(index, -1)
            if (value > 0) {
                add(value)
            }
        }
    }
}

object ReminderDetailRouteExtra {
    const val KEY_REMINDER_ID = "reminder_id"
}
