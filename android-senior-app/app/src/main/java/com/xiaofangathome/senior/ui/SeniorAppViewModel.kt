package com.xiaofangathome.senior.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.xiaofangathome.senior.data.ChatMessage
import com.xiaofangathome.senior.data.CompanionContextRecall
import com.xiaofangathome.senior.data.CompanionMemoryEngine
import com.xiaofangathome.senior.data.ContactItem
import com.xiaofangathome.senior.data.FREQUENCY_DAILY
import com.xiaofangathome.senior.data.FREQUENCY_ONCE
import com.xiaofangathome.senior.data.FREQUENCY_WEEKLY
import com.xiaofangathome.senior.data.MockRepository
import com.xiaofangathome.senior.data.LocationSample
import com.xiaofangathome.senior.data.MemoryLayer
import com.xiaofangathome.senior.data.MemoryRetention
import com.xiaofangathome.senior.data.SemanticMemoryItem
import com.xiaofangathome.senior.data.SemanticMemoryType
import com.xiaofangathome.senior.data.ReminderDraft
import com.xiaofangathome.senior.data.ReminderItem
import com.xiaofangathome.senior.data.ReminderStatus
import com.xiaofangathome.senior.data.RemoteSemanticMemory
import com.xiaofangathome.senior.data.ServiceSyncSnapshot
import com.xiaofangathome.senior.data.buildReminderDescription
import com.xiaofangathome.senior.data.deriveReminderStatus
import com.xiaofangathome.senior.data.normalizeWeeklyDays
import com.xiaofangathome.senior.platform.CareServiceClient
import com.xiaofangathome.senior.platform.CompanionReplyConversationContext
import com.xiaofangathome.senior.platform.CompanionReplyContextMessage
import com.xiaofangathome.senior.platform.CommunicationStyle
import com.xiaofangathome.senior.platform.SeniorAppServices
import com.xiaofangathome.senior.platform.VoiceTranscriptionResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class HelpRequestState(
    val sourceLabel: String,
    val reason: String,
    val recommendedContact: ContactItem?,
    val contacts: List<ContactItem>,
    val lastDialedContactId: String? = null,
)

data class SeniorUiState(
    val requiresOnboarding: Boolean,
    val preferredName: String,
    val communicationStyle: CommunicationStyle,
    val commonTopics: String,
    val tabooWords: String,
    val careServiceBaseUrl: String,
    val careServiceSeniorId: String,
    val homeTasks: List<ReminderItem>,
    val reminders: List<ReminderItem>,
    val contacts: List<ContactItem>,
    val chatMessages: List<ChatMessage>,
    val semanticMemories: List<SemanticMemoryItem>,
    val reminderSyncMessage: String,
    val companionStatusMessage: String,
    val companionModelConnected: Boolean = false,
    val companionAutoSpeak: Boolean = true,
    val isRecordingVoice: Boolean = false,
    val isPlayingVoice: Boolean = false,
    val isTranscribingVoice: Boolean = false,
    val companionVoiceLevel: Float = 0f,
    val lastRecordingSummary: String? = null,
    val lastRecordingPath: String? = null,
    val latestTranscript: String? = null,
    val latestReminderDraft: ReminderDraft? = null,
    val currentHelpRequest: HelpRequestState? = null,
    val bindingCode: String,
    val bindingUpdatedAt: String,
    val bindingNotice: String? = null,
    val onboardingNotice: String? = null,
    val profileNotice: String? = null,
    val serviceConnectionNotice: String? = null,
    val settingsNotice: String? = null,
    val hourlyLocationTrackingEnabled: Boolean = false,
    val locationSamples: List<LocationSample> = emptyList(),
)

class SeniorAppViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val preferencesStore by lazy {
        SeniorAppServices.ensureInitialized(application.applicationContext)
        SeniorAppServices.preferencesStore
    }
    private val reminderRepository by lazy {
        SeniorAppServices.ensureInitialized(application.applicationContext)
        SeniorAppServices.reminderRepository
    }
    private val reminderScheduler by lazy {
        SeniorAppServices.ensureInitialized(application.applicationContext)
        SeniorAppServices.reminderScheduler
    }
    private val contactRepository by lazy {
        SeniorAppServices.ensureInitialized(application.applicationContext)
        SeniorAppServices.contactRepository
    }
    private val chatMemoryRepository by lazy {
        SeniorAppServices.ensureInitialized(application.applicationContext)
        SeniorAppServices.chatMemoryRepository
    }
    private val semanticMemoryRepository by lazy {
        SeniorAppServices.ensureInitialized(application.applicationContext)
        SeniorAppServices.semanticMemoryRepository
    }
    private val locationSampleRepository by lazy {
        SeniorAppServices.ensureInitialized(application.applicationContext)
        SeniorAppServices.locationSampleRepository
    }
    private val locationTrackingScheduler by lazy {
        SeniorAppServices.ensureInitialized(application.applicationContext)
        SeniorAppServices.locationTrackingScheduler
    }
    private val voiceCapability by lazy {
        SeniorAppServices.ensureInitialized(application.applicationContext)
        SeniorAppServices.voiceCapability
    }
    private val careServiceClient by lazy {
        CareServiceClient(application.applicationContext)
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    var uiState by mutableStateOf(createInitialState())
        private set

    init {
        syncReminderPlan()
        hydrateServiceSnapshot()
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        uiState = uiState.copy(
            companionStatusMessage = if (granted) {
                "现在可以按住说话了，松开后我会把内容整理成一句话。"
            } else {
                "没有麦克风权限，暂时还不能用语音和我说话。"
            },
        )
    }

    fun toggleCompanionAutoSpeak() {
        val enabled = !uiState.companionAutoSpeak
        uiState = uiState.copy(
            companionAutoSpeak = enabled,
            companionStatusMessage = if (enabled) {
                "自动播报已经打开。"
            } else {
                "自动播报已经关闭。"
            },
        )
    }

    fun savePreferenceProfile(
        preferredName: String,
        tabooWords: String,
    ) {
        val snapshot = preferencesStore.load().copy(
            preferredName = preferredName.trim(),
            tabooWords = tabooWords.trim(),
        )
        preferencesStore.save(snapshot)
        uiState = uiState.copy(
            preferredName = snapshot.preferredName,
            tabooWords = snapshot.tabooWords,
            profileNotice = "资料已经保存。",
        )
    }

    fun saveServiceConnectionSettings(
        baseUrl: String,
        seniorId: String,
    ) {
        val snapshot = preferencesStore.load().copy(
            careServiceBaseUrl = baseUrl.trim(),
            careServiceSeniorId = seniorId.trim(),
        )
        preferencesStore.save(snapshot)
        uiState = uiState.copy(
            careServiceBaseUrl = snapshot.careServiceBaseUrl,
            careServiceSeniorId = snapshot.careServiceSeniorId,
            serviceConnectionNotice = "服务连接设置已经保存，正在测试连接……",
        )
        Thread {
            val runtime = careServiceClient.fetchRuntimeStatus(snapshot.careServiceBaseUrl)
            val notice = buildServiceConnectionNotice(snapshot, runtime)
            mainHandler.post {
                uiState = uiState.copy(
                    serviceConnectionNotice = notice,
                    companionModelConnected = runtime?.configured == true,
                )
            }
            if (runtime?.configured == true) {
                hydrateServiceSnapshot()
            }
        }.start()
    }

    fun completeOnboarding(
        preferredName: String,
        style: CommunicationStyle,
        commonTopics: String,
        relation: String,
        contactName: String,
        contactPhone: String,
        reminderTitle: String,
        reminderTime: String,
    ) {
        val cleanPreferredName = preferredName.trim()
        val cleanRelation = relation.trim()
        val cleanContactName = contactName.trim()
        val cleanContactPhone = contactPhone.trim()
        val cleanReminderTitle = reminderTitle.trim()
        val cleanCommonTopics = commonTopics.trim()
        val normalizedTime = normalizeTime(reminderTime)

        val missingLabel = when {
            cleanPreferredName.isBlank() -> "先告诉我怎么称呼您。"
            cleanRelation.isBlank() || cleanContactName.isBlank() || cleanContactPhone.isBlank() -> "先补一位家人的关系、姓名和电话。"
            cleanReminderTitle.isBlank() -> "先记下一件今天最重要的事。"
            else -> null
        }
        if (missingLabel != null) {
            uiState = uiState.copy(onboardingNotice = missingLabel)
            return
        }

        val snapshot = preferencesStore.load().copy(
            preferredName = cleanPreferredName,
            communicationStyle = style,
            commonTopics = cleanCommonTopics,
        )
        preferencesStore.save(snapshot)

        val updatedContacts = if (uiState.contacts.isEmpty()) {
            listOf(
                ContactItem(
                    id = System.currentTimeMillis().toString(),
                    relation = cleanRelation,
                    name = cleanContactName,
                    phone = cleanContactPhone,
                ),
            )
        } else {
            uiState.contacts
        }
        contactRepository.saveAll(updatedContacts)

        val updatedReminders = if (uiState.reminders.isEmpty()) {
            listOf(
                ReminderItem(
                    id = "${System.currentTimeMillis()}_onboarding",
                    time = normalizedTime,
                    title = cleanReminderTitle,
                    description = buildReminderDescription(FREQUENCY_DAILY, emptyList(), true, true),
                    status = ReminderStatus.Planned,
                    frequencyLabel = FREQUENCY_DAILY,
                    weeklyDays = emptyList(),
                    voiceEnabled = true,
                    alarmEnabled = true,
                ),
            )
        } else {
            uiState.reminders
        }
        reminderRepository.saveAll(updatedReminders)
        reminderScheduler.sync(updatedReminders)

        val refreshedReminders = sortReminders(refreshReminderStatuses(updatedReminders))
        uiState = uiState.copy(
            requiresOnboarding = needsOnboarding(snapshot, updatedContacts, refreshedReminders),
            preferredName = snapshot.preferredName,
            communicationStyle = snapshot.communicationStyle,
            commonTopics = snapshot.commonTopics,
            contacts = updatedContacts,
            reminders = refreshedReminders,
            homeTasks = buildHomeTasks(refreshedReminders),
            reminderSyncMessage = "首次设置已完成，提醒已经同步到本地。",
            onboardingNotice = null,
            profileNotice = "首次设置已完成。",
        )
    }

    fun setCommunicationStyle(style: CommunicationStyle) {
        val snapshot = preferencesStore.load().copy(communicationStyle = style)
        preferencesStore.save(snapshot)
        uiState = uiState.copy(
            communicationStyle = style,
            companionStatusMessage = when (style) {
                CommunicationStyle.PatientDelicate -> "小芳已切换到${style.displayName}，会更像${style.personaName}那样陪您说话。"
                CommunicationStyle.ConfidentMature -> "小芳已切换到${style.displayName}，会更像${style.personaName}那样陪您聊天。"
            },
            profileNotice = "沟通风格已切换为${style.displayName}。",
        )
    }

    fun findReminder(reminderId: String?): ReminderItem? {
        return uiState.reminders.firstOrNull { it.id == reminderId }
    }

    fun markReminderCompleted(reminderId: String) {
        saveReminders(
            uiState.reminders.map { reminder ->
                if (reminder.id == reminderId) reminder.copy(status = ReminderStatus.Completed) else reminder
            },
            "这条提醒已经记为完成。",
        )
    }

    fun snoozeReminder(reminderId: String) {
        val target = findReminder(reminderId) ?: return
        saveReminders(
            uiState.reminders.map { reminder ->
                if (reminder.id == reminderId) {
                    reminder.copy(
                        time = shiftTimeByMinutes(target.time, 30),
                        status = ReminderStatus.Planned,
                    )
                } else {
                    reminder
                }
            },
            "我已经帮您顺延 30 分钟。",
        )
    }

    fun syncReminderPlan() {
        val refreshed = refreshReminderStatuses(uiState.reminders)
        reminderRepository.saveAll(refreshed)
        val result = reminderScheduler.sync(refreshed)
        uiState = uiState.copy(
            reminders = sortReminders(refreshed),
            homeTasks = buildHomeTasks(refreshed),
            reminderSyncMessage = if (result.scheduledCount > 0) {
                "提醒计划已经同步到本地，一共安排了 ${result.scheduledCount} 条提醒。"
            } else {
                "当前还没有可同步的提醒。"
            },
        )
    }

    fun addReminder(
        title: String,
        time: String,
        frequencyLabel: String,
        weeklyDays: List<Int>,
        voiceEnabled: Boolean,
        alarmEnabled: Boolean,
    ) {
        val normalizedFrequency = normalizeFrequency(frequencyLabel)
        val reminder = ReminderItem(
            id = System.currentTimeMillis().toString(),
            time = normalizeTime(time),
            title = title.ifBlank { "新的提醒" },
            description = buildReminderDescription(normalizedFrequency, weeklyDays, voiceEnabled, alarmEnabled),
            status = ReminderStatus.Planned,
            frequencyLabel = normalizedFrequency,
            weeklyDays = normalizeWeeklyDays(normalizedFrequency, weeklyDays),
            voiceEnabled = voiceEnabled,
            alarmEnabled = alarmEnabled,
        )
        saveReminders(uiState.reminders + reminder, "已经新增提醒。")
    }

    fun updateReminder(
        reminderId: String,
        title: String,
        time: String,
        frequencyLabel: String,
        weeklyDays: List<Int>,
        voiceEnabled: Boolean,
        alarmEnabled: Boolean,
    ) {
        val normalizedFrequency = normalizeFrequency(frequencyLabel)
        saveReminders(
            uiState.reminders.map { reminder ->
                if (reminder.id == reminderId) {
                    reminder.copy(
                        title = title.ifBlank { reminder.title },
                        time = normalizeTime(time),
                        description = buildReminderDescription(normalizedFrequency, weeklyDays, voiceEnabled, alarmEnabled),
                        frequencyLabel = normalizedFrequency,
                        weeklyDays = normalizeWeeklyDays(normalizedFrequency, weeklyDays),
                        voiceEnabled = voiceEnabled,
                        alarmEnabled = alarmEnabled,
                    )
                } else {
                    reminder
                }
            },
            "提醒已经更新。",
        )
    }

    fun deleteReminder(reminderId: String) {
        saveReminders(uiState.reminders.filterNot { it.id == reminderId }, "提醒已经删除。")
    }

    fun notifyMicrophonePermissionNeeded() {
        uiState = uiState.copy(
            companionStatusMessage = "需要先打开麦克风权限，我才能听见您说话。",
        )
    }

    fun startCompanionListening(): Boolean {
        voiceCapability.stopPlayback()
        uiState = uiState.copy(
            companionStatusMessage = "正在打开语音识别……",
            latestTranscript = null,
            companionVoiceLevel = 0f,
        )
        val result = voiceCapability.startTranscription(
            onResult = ::handleCompanionVoiceResult,
            onLevelChange = { level ->
                uiState = uiState.copy(companionVoiceLevel = level)
            },
        )
        uiState = uiState.copy(
            isRecordingVoice = result.isListening,
            isTranscribingVoice = result.isListening,
            isPlayingVoice = false,
            companionVoiceLevel = if (result.isListening) uiState.companionVoiceLevel else 0f,
            companionStatusMessage = if (result.isListening) {
                "我在听，您松开后我就整理成一句话。"
            } else {
                result.message
            },
        )
        return result.isListening
    }

    fun stopCompanionListening() {
        val wasListening = uiState.isRecordingVoice || uiState.isTranscribingVoice
        uiState = uiState.copy(
            companionStatusMessage = if (wasListening) {
                "正在结束语音输入……"
            } else {
                uiState.companionStatusMessage
            },
            companionVoiceLevel = 0f,
        )
        val result = voiceCapability.stopTranscription()
        uiState = uiState.copy(
            isRecordingVoice = false,
            isTranscribingVoice = false,
            companionVoiceLevel = 0f,
            companionStatusMessage = if (wasListening) {
                "我在整理刚才的话，马上回复您。"
            } else {
                result.message
            },
        )
    }

    fun sendCompanionText(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        appendCompanionConversation(userText = cleanText, fromVoice = false)
    }

    fun beginHelpRequest(
        sourceLabel: String,
        reason: String,
    ) {
        uiState = uiState.copy(
            currentHelpRequest = HelpRequestState(
                sourceLabel = sourceLabel,
                reason = reason,
                recommendedContact = uiState.contacts.firstOrNull(),
                contacts = uiState.contacts,
            ),
        )
    }

    fun clearHelpRequest() {
        uiState = uiState.copy(currentHelpRequest = null)
    }

    fun markHelpDialed(contactId: String) {
        val request = uiState.currentHelpRequest ?: return
        uiState = uiState.copy(currentHelpRequest = request.copy(lastDialedContactId = contactId))
    }

    fun refreshBindingCode() {
        uiState = uiState.copy(
            bindingCode = createBindingCode(),
            bindingUpdatedAt = currentBindingTimeLabel(),
            bindingNotice = "新的绑定码已经准备好了。",
        )
    }

    fun markBindingCodeCopied() {
        uiState = uiState.copy(bindingNotice = "绑定码已经复制，可以发给家里人了。")
    }
    fun addContact(
        relation: String,
        name: String,
        phone: String,
    ) {
        val cleanRelation = relation.trim()
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        if (cleanRelation.isBlank() || cleanName.isBlank() || cleanPhone.isBlank()) {
            uiState = uiState.copy(profileNotice = "请把关系、姓名和手机号填写完整。")
            return
        }
        val updatedContacts = uiState.contacts + ContactItem(
            id = System.currentTimeMillis().toString(),
            relation = cleanRelation,
            name = cleanName,
            phone = cleanPhone,
        )
        contactRepository.saveAll(updatedContacts)
        uiState = uiState.copy(
            contacts = updatedContacts,
            profileNotice = "常用联系人已经新增。",
        )
    }

    fun removeContact(contactId: String) {
        val updatedContacts = uiState.contacts.filterNot { it.id == contactId }
        contactRepository.saveAll(updatedContacts)
        val currentRequest = uiState.currentHelpRequest
        uiState = uiState.copy(
            contacts = updatedContacts,
            currentHelpRequest = currentRequest?.copy(
                contacts = updatedContacts,
                recommendedContact = updatedContacts.firstOrNull(),
                lastDialedContactId = currentRequest.lastDialedContactId
                    ?.takeIf { dialedId -> updatedContacts.any { it.id == dialedId } },
            ),
            profileNotice = "常用联系人已经删除。",
        )
    }

    fun clearProfileNotice() {
        if (uiState.profileNotice == null) return
        uiState = uiState.copy(profileNotice = null)
    }

    fun clearSettingsNotice() {
        if (uiState.settingsNotice == null) return
        uiState = uiState.copy(settingsNotice = null)
    }

    fun removeSemanticMemory(memoryId: String) {
        val updatedMemories = uiState.semanticMemories.filterNot { it.id == memoryId }
        semanticMemoryRepository.saveAll(updatedMemories)
        uiState = uiState.copy(
            semanticMemories = updatedMemories,
            settingsNotice = "这条多层记忆已经删除。",
        )
        Thread {
            careServiceClient.syncSemanticMemories(updatedMemories)
        }.start()
    }

    fun setHourlyLocationTrackingEnabled(enabled: Boolean) {
        val snapshot = preferencesStore.load().copy(hourlyLocationTrackingEnabled = enabled)
        preferencesStore.save(snapshot)
        val schedulerMessage = locationTrackingScheduler.sync(enabled)
        uiState = uiState.copy(
            hourlyLocationTrackingEnabled = enabled,
            settingsNotice = schedulerMessage,
            locationSamples = locationSampleRepository.loadAll(),
        )
    }

    fun onLocationTrackingPermissionDenied() {
        uiState = uiState.copy(
            settingsNotice = "需要定位权限并允许后台访问，才能按小时记录当前位置。",
        )
    }

    fun clearOnboardingNotice() {
        if (uiState.onboardingNotice == null) return
        uiState = uiState.copy(onboardingNotice = null)
    }

    private fun createInitialState(): SeniorUiState {
        val snapshot = preferencesStore.load()
        val storedReminders = reminderRepository.loadAll().ifEmpty { MockRepository.reminders }
        val contacts = contactRepository.loadAll()
        val reminders = sortReminders(refreshReminderStatuses(storedReminders))
        return SeniorUiState(
            requiresOnboarding = needsOnboarding(snapshot, contacts, reminders),
            preferredName = snapshot.preferredName,
            communicationStyle = snapshot.communicationStyle,
            commonTopics = snapshot.commonTopics,
            tabooWords = snapshot.tabooWords,
            careServiceBaseUrl = snapshot.careServiceBaseUrl,
            careServiceSeniorId = snapshot.careServiceSeniorId,
            homeTasks = buildHomeTasks(reminders),
            reminders = reminders,
            contacts = contacts,
            chatMessages = chatMemoryRepository.loadRecent().ifEmpty { MockRepository.chatMessages },
            semanticMemories = semanticMemoryRepository.loadAll(),
            reminderSyncMessage = "提醒计划会同步到本地通知。",
            companionStatusMessage = buildCompanionCheckInMessage(
                chatMessages = chatMemoryRepository.loadRecent().ifEmpty { MockRepository.chatMessages },
                semanticMemories = semanticMemoryRepository.loadAll(),
            ) ?: "按住说话，或者直接打字告诉我。",
            companionModelConnected = false,
            companionAutoSpeak = true,
            bindingCode = createBindingCode(),
            bindingUpdatedAt = currentBindingTimeLabel(),
            hourlyLocationTrackingEnabled = snapshot.hourlyLocationTrackingEnabled,
            locationSamples = locationSampleRepository.loadAll(),
        )
    }

    private fun saveReminders(
        reminders: List<ReminderItem>,
        syncMessage: String,
    ) {
        val refreshed = refreshReminderStatuses(reminders)
        reminderRepository.saveAll(refreshed)
        reminderScheduler.sync(refreshed)
        uiState = uiState.copy(
            reminders = sortReminders(refreshed),
            homeTasks = buildHomeTasks(refreshed),
            reminderSyncMessage = syncMessage,
        )
    }

    private fun refreshReminderStatuses(reminders: List<ReminderItem>): List<ReminderItem> {
        return reminders.map { reminder ->
            reminder.copy(
                status = deriveReminderStatus(
                    time = reminder.time,
                    frequencyLabel = reminder.frequencyLabel,
                    weeklyDays = reminder.weeklyDays,
                    completed = reminder.status == ReminderStatus.Completed,
                ),
            )
        }
    }

    private fun sortReminders(reminders: List<ReminderItem>): List<ReminderItem> {
        return reminders.sortedWith(
            compareBy<ReminderItem>({ statusOrder(it.status) }, { timeToMinutes(it.time) }, { it.title }),
        )
    }

    private fun buildHomeTasks(reminders: List<ReminderItem>): List<ReminderItem> {
        val active = reminders.filter { it.status != ReminderStatus.Completed }
        return if (active.isNotEmpty()) active.take(3) else reminders.take(3)
    }

    private fun handleCompanionVoiceResult(result: VoiceTranscriptionResult) {
        val transcript = result.transcript?.trim().orEmpty()
        if (transcript.isBlank()) {
            uiState = uiState.copy(
                isRecordingVoice = false,
                isTranscribingVoice = false,
                companionVoiceLevel = 0f,
                latestTranscript = null,
                companionStatusMessage = result.message.ifBlank { "这次没有听清楚，我们再说一次。" },
            )
            return
        }
        appendCompanionConversation(userText = transcript, fromVoice = true)
    }

    private fun appendCompanionConversation(
        userText: String,
        fromVoice: Boolean,
    ) {
        val snapshot = uiState
        val timeLabel = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date())
        val currentTime = System.currentTimeMillis()
        val patientDelicate = snapshot.communicationStyle == CommunicationStyle.PatientDelicate
        val useFastLocalReply = CompanionMemoryEngine.shouldUseFastLocalReplyV2(userText)
        val recalledMemories = CompanionMemoryEngine.recallRelevantMemories(
            userText = userText,
            semanticMemories = snapshot.semanticMemories,
            touchedAt = currentTime,
        )
        val emotionHint = CompanionMemoryEngine.buildEmotionHintV2(
            userText = userText,
            patientDelicate = patientDelicate,
        )
        val collectionHint = CompanionMemoryEngine.buildCollectionHintV2(
            userText = userText,
            semanticMemories = snapshot.semanticMemories,
            patientDelicate = patientDelicate,
        )
        val recall = CompanionContextRecall(
            semanticMemories = recalledMemories,
            profileHint = CompanionMemoryEngine.buildProfileHint(
                userText = userText,
                commonTopics = snapshot.commonTopics,
                tabooWords = snapshot.tabooWords,
                patientDelicate = patientDelicate,
            ),
            taskHint = CompanionMemoryEngine.buildTaskHint(
                userText = userText,
                reminders = snapshot.reminders,
                patientDelicate = patientDelicate,
            ),
            recentHint = CompanionMemoryEngine.buildRecentHint(
                userText = userText,
                chatMessages = snapshot.chatMessages,
                patientDelicate = patientDelicate,
            ),
            emotionHint = emotionHint,
            collectionHint = collectionHint,
        )
        val conversationContext = buildCompanionReplyContext(
            snapshot = snapshot,
            recalledMemories = recalledMemories,
            recentHint = recall.recentHint,
            emotionHint = recall.emotionHint,
            collectionHint = collectionHint,
        )
        val replyDraft = CompanionMemoryEngine.buildReplyDraftV2(
            userText = userText,
            patientDelicate = patientDelicate,
            recall = recall,
        )
        val localReply = CompanionMemoryEngine.composeReply(replyDraft)
        val recentMessages = snapshot.chatMessages.takeLast(4).map { message ->
            CompanionReplyContextMessage(
                role = if (message.fromAi) "assistant" else "user",
                content = message.content,
            )
        }
        val userMessage = ChatMessage(
            id = "user_$currentTime",
            fromAi = false,
            content = userText,
            timeLabel = timeLabel,
            createdAt = currentTime,
        )
        val optimisticMessages = snapshot.chatMessages + userMessage

        uiState = snapshot.copy(
            chatMessages = optimisticMessages,
            companionStatusMessage = "",
            isRecordingVoice = false,
            isTranscribingVoice = false,
            companionVoiceLevel = 0f,
        )
        chatMemoryRepository.saveRecent(optimisticMessages)

        Thread {
            val serviceResult = if (useFastLocalReply) {
                null
            } else {
                careServiceClient.fetchCompanionReply(
                    userText = userText,
                    recentMessages = recentMessages,
                    conversationContext = conversationContext,
                )
            }
            val useServiceReply = !useFastLocalReply && serviceResult?.mode == "live" && serviceResult.reply.isNotBlank()
            val reply = if (useServiceReply) serviceResult!!.reply else localReply
            val extractedMemories = if (!useFastLocalReply && CompanionMemoryEngine.shouldExtractSemanticMemory(userText)) {
                CompanionMemoryEngine.extractSemanticMemories(userText, currentTime)
            } else {
                emptyList()
            }
            val assistantMessage = ChatMessage(
                id = "ai_${currentTime + 1}",
                fromAi = true,
                content = reply,
                timeLabel = timeLabel,
                createdAt = currentTime + 1,
            )
            val updatedMessages = optimisticMessages + assistantMessage
            val updatedMemories = mergeSemanticMemories(
                existing = snapshot.semanticMemories,
                additions = extractedMemories,
                recalled = recalledMemories,
                touchedAt = currentTime,
            )
            chatMemoryRepository.saveRecent(updatedMessages)
            semanticMemoryRepository.saveAll(updatedMemories)

            if (snapshot.companionAutoSpeak) {
                voiceCapability.speak(reply)
            }

            mainHandler.post {
                val currentState = uiState
                uiState = currentState.copy(
                    chatMessages = updatedMessages,
                    semanticMemories = updatedMemories,
                    latestTranscript = if (fromVoice) userText else null,
                    latestReminderDraft = null,
                    isRecordingVoice = false,
                    isTranscribingVoice = false,
                    companionVoiceLevel = 0f,
                    companionModelConnected = if (useFastLocalReply) currentState.companionModelConnected else useServiceReply,
                    companionStatusMessage = "",
                )
            }

            if (extractedMemories.isNotEmpty() || recalledMemories.isNotEmpty()) {
                careServiceClient.syncSemanticMemories(updatedMemories)
            }
        }.start()
    }

    private fun hydrateServiceSnapshot() {
        Thread {
            val runtime = careServiceClient.fetchRuntimeStatus()
            val snapshot = careServiceClient.fetchServiceSyncSnapshot()
            if (snapshot == null) {
                mainHandler.post {
                    val currentState = uiState
                    uiState = currentState.copy(
                        companionModelConnected = runtime?.configured == true,
                    )
                }
                return@Thread
            }
            val remoteMemories = snapshot.semanticMemories.mapNotNull { it.toLocalMemory() }
            if (remoteMemories.isEmpty()) {
                mainHandler.post {
                    val currentState = uiState
                    uiState = currentState.copy(
                        companionModelConnected = runtime?.configured == true,
                    )
                }
                return@Thread
            }

            val localMemories = semanticMemoryRepository.loadAll()
            val mergedMemories = mergeServiceSemanticMemories(localMemories, remoteMemories)
            semanticMemoryRepository.saveAll(mergedMemories)

            mainHandler.post {
                val currentState = uiState
                val refreshedStatus = buildCompanionCheckInMessage(currentState.chatMessages, mergedMemories)
                uiState = currentState.copy(
                    semanticMemories = mergedMemories,
                    companionModelConnected = runtime?.configured == true,
                    companionStatusMessage = if (shouldRefreshCompanionStatus(currentState.companionStatusMessage)) {
                        refreshedStatus ?: currentState.companionStatusMessage
                    } else {
                        currentState.companionStatusMessage
                    },
                )
            }
        }.start()
    }

    private fun buildCompanionCheckInMessage(
        chatMessages: List<ChatMessage>,
        semanticMemories: List<SemanticMemoryItem>,
    ): String? {
        val lastUserMessage = chatMessages.lastOrNull { !it.fromAi && it.createdAt > 0L }
        if (lastUserMessage != null) {
            val hoursSince = ((System.currentTimeMillis() - lastUserMessage.createdAt).coerceAtLeast(0L) / 3_600_000L)
            if (hoursSince >= 72L) {
                return "好几天没跟您聊天了，我在这儿，想聊的时候叫我。"
            }
            if (hoursSince >= 24L) {
                return "今天如果想找人说话，我一直在。"
            }
        }

        val lowMoodMemory = semanticMemories.lastOrNull { memory ->
            val text = listOf(memory.title, memory.summary, memory.compressedSummary).joinToString(" ")
            listOf("低落", "孤单", "寂寞", "难过", "担心", "烦", "想家").any { text.contains(it) }
        }
        if (lowMoodMemory != null) {
            return "我记得您前几次提到心情有点低，我会陪着您。"
        }

        return null
    }

    private fun shouldRefreshCompanionStatus(currentStatus: String): Boolean {
        return currentStatus.isBlank() ||
            currentStatus.contains("按住说话") ||
            currentStatus.contains("我在这儿") ||
            currentStatus.contains("没跟您聊天") ||
            currentStatus.contains("一直在")
    }

    private fun mergeServiceSemanticMemories(
        local: List<SemanticMemoryItem>,
        remote: List<SemanticMemoryItem>,
    ): List<SemanticMemoryItem> {
        val merged = linkedMapOf<String, SemanticMemoryItem>()
        (local + remote).sortedByDescending { it.lastAccessedAt }.forEach { memory ->
            val current = merged[memory.id]
            if (current == null) {
                merged[memory.id] = memory
            } else if (memory.updatedAt > current.updatedAt || memory.lastAccessedAt > current.lastAccessedAt) {
                merged[memory.id] = memory
            }
        }
        return merged.values.sortedByDescending { it.lastAccessedAt }.take(40)
    }

    private fun RemoteSemanticMemory.toLocalMemory(): SemanticMemoryItem? {
        val memoryType = runCatching { SemanticMemoryType.valueOf(this.memoryType) }.getOrNull() ?: return null
        return SemanticMemoryItem(
            id = id,
            memoryType = memoryType,
            memoryLayer = when (memoryLayer) {
                "profile", MemoryLayer.Profile.name -> MemoryLayer.Profile
                "preference", MemoryLayer.Preference.name -> MemoryLayer.Preference
                "recent_state", MemoryLayer.RecentState.name -> MemoryLayer.RecentState
                else -> MemoryLayer.Profile
            },
            retention = when (retention) {
                "short_term", MemoryRetention.ShortTerm.name -> MemoryRetention.ShortTerm
                else -> MemoryRetention.LongTerm
            },
            title = title,
            summary = summary,
            compressedSummary = compressedSummary,
            keywords = keywords,
            sourceText = sourceText,
            confidence = confidence,
            createdAt = createdAt,
            updatedAt = updatedAt,
            sourceCount = sourceCount,
            evidenceCount = evidenceCount,
            lastAccessedAt = lastAccessedAt,
            lastConfirmedAt = lastConfirmedAt,
            expiresAt = expiresAt,
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun buildCompanionReplyContext(
        snapshot: SeniorUiState,
        recalledMemories: List<SemanticMemoryItem>,
        recentHint: String?,
        emotionHint: String?,
        collectionHint: String?,
    ): CompanionReplyConversationContext {
        return CompanionReplyConversationContext(
            preferredName = snapshot.preferredName.ifBlank { null },
            communicationStyle = when (snapshot.communicationStyle) {
                CommunicationStyle.PatientDelicate -> "patient_gentle"
                CommunicationStyle.ConfidentMature -> "confident_steady"
            },
            personaPrompt = null,
            commonTopics = splitTopics(snapshot.commonTopics).take(4),
            tabooTopics = splitTopics(snapshot.tabooWords).take(4),
            emotionHint = emotionHint,
            memoryHighlights = recalledMemories.mapNotNull { memory ->
                memory.compressedSummary.takeIf { it.isNotBlank() } ?: memory.summary.takeIf { it.isNotBlank() }
            }.take(2),
            reminderHint = snapshot.reminders.firstOrNull { it.status != ReminderStatus.Completed }
                ?.let { "${it.time} 的 ${it.title}" },
            contactHint = snapshot.contacts.take(2)
                .joinToString("、") { "${it.relation}${it.name}" }
                .takeIf { it.isNotBlank() },
            recentConversationHint = recentHint,
            collectionHint = null,
        )
    }

    private fun splitTopics(rawText: String): List<String> {
        return rawText
            .split("、", "，", ",", " ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun mergeSemanticMemories(
        existing: List<SemanticMemoryItem>,
        additions: List<SemanticMemoryItem>,
        recalled: List<SemanticMemoryItem>,
        touchedAt: Long,
    ): List<SemanticMemoryItem> {
        return CompanionMemoryEngine.mergeSemanticMemories(
            existing = existing,
            additions = additions,
            recalled = recalled,
            touchedAt = touchedAt,
        )
    }

    private fun normalizeFrequency(frequencyLabel: String): String {
        return when (frequencyLabel) {
            FREQUENCY_ONCE, FREQUENCY_WEEKLY -> frequencyLabel
            else -> FREQUENCY_DAILY
        }
    }

    private fun normalizeTime(rawTime: String): String {
        val hour = rawTime.substringBefore(":").toIntOrNull() ?: 8
        val minute = rawTime.substringAfter(":", "0").toIntOrNull() ?: 0
        return "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    private fun shiftTimeByMinutes(time: String, offsetMinutes: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.substringBefore(":").toIntOrNull() ?: 8)
            set(Calendar.MINUTE, time.substringAfter(":", "0").toIntOrNull() ?: 0)
            add(Calendar.MINUTE, offsetMinutes)
        }
        return "%02d:%02d".format(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }

    private fun statusOrder(status: ReminderStatus): Int {
        return when (status) {
            ReminderStatus.Current -> 0
            ReminderStatus.Planned -> 1
            ReminderStatus.Completed -> 2
        }
    }

    private fun timeToMinutes(time: String): Int {
        val hour = time.substringBefore(":").toIntOrNull() ?: 0
        val minute = time.substringAfter(":", "0").toIntOrNull() ?: 0
        return hour * 60 + minute
    }

    private fun createBindingCode(): String {
        return buildString { repeat(6) { append(Random.nextInt(0, 10)) } }
    }

    private fun currentBindingTimeLabel(): String {
        return SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date())
    }

    private fun needsOnboarding(
        snapshot: com.xiaofangathome.senior.platform.SeniorPreferencesSnapshot,
        contacts: List<ContactItem>,
        reminders: List<ReminderItem>,
    ): Boolean {
        return snapshot.preferredName.isBlank() || contacts.isEmpty() || reminders.isEmpty()
    }
}
