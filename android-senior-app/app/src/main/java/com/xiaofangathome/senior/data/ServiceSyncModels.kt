package com.xiaofangathome.senior.data

import com.xiaofangathome.senior.platform.CommunicationStyle

data class RemoteImportantContact(
    val id: String,
    val name: String,
    val relation: String,
    val phone: String,
    val priority: Int,
)

data class RemoteSeniorProfile(
    val seniorId: String,
    val preferredName: String,
    val relationLabel: String,
    val interests: List<String>,
    val hobbies: List<String>,
    val tabooTopics: List<String>,
    val communicationStyle: CommunicationStyle,
    val routineSummary: String,
    val personaTags: List<String>,
    val importantContacts: List<RemoteImportantContact>,
    val updatedAt: String,
)

data class RemoteCarePlan(
    val planId: String,
    val seniorId: String,
    val title: String,
    val schedule: String,
    val frequency: String,
    val channel: String,
    val confirmRequired: Boolean,
    val status: String,
    val updatedAt: String,
)

data class RemoteCarePlanEvent(
    val eventId: String,
    val planId: String,
    val eventType: String,
    val payloadSummary: String,
    val createdAt: String,
)

data class RemoteTopicBrief(
    val topicId: String,
    val title: String,
    val summary: String,
    val sourceName: String,
    val generatedAt: String,
)

data class RemoteSemanticMemory(
    val seniorId: String,
    val id: String,
    val memoryType: String,
    val memoryLayer: String,
    val retention: String,
    val title: String,
    val summary: String,
    val compressedSummary: String,
    val keywords: List<String>,
    val sourceText: String,
    val confidence: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val sourceCount: Int,
    val evidenceCount: Int,
    val lastAccessedAt: Long,
    val lastConfirmedAt: Long,
    val expiresAt: Long?,
)

data class ServiceSyncSnapshot(
    val seniorId: String,
    val profile: RemoteSeniorProfile?,
    val carePlans: List<RemoteCarePlan>,
    val latestEvents: List<RemoteCarePlanEvent>,
    val topicBriefs: List<RemoteTopicBrief>,
    val semanticMemories: List<RemoteSemanticMemory>,
    val generatedAt: String,
)

data class ServiceStorageMigrationPlan(
    val profileFieldsMovingToService: List<String>,
    val reminderFieldsMovingToService: List<String>,
    val localFallbacksToKeep: List<String>,
    val nextAction: String,
)

fun buildServiceStorageMigrationPlan(
    preferredName: String,
    communicationStyle: CommunicationStyle,
    commonTopics: String,
    tabooWords: String,
    reminders: List<ReminderItem>,
    contacts: List<ContactItem>,
): ServiceStorageMigrationPlan {
    val profileFields = buildList {
        add("称呼")
        add("沟通风格")
        if (commonTopics.isNotBlank()) add("常聊话题")
        if (tabooWords.isNotBlank()) add("禁忌话题")
        if (contacts.isNotEmpty()) add("重要联系人")
        if (preferredName.isNotBlank()) add("老人画像标签")
    }

    val reminderFields = buildList {
        if (reminders.isNotEmpty()) {
            add("正式提醒计划")
            add("提醒频率与时间")
            add("语音/铃声通道")
            add("确认要求")
        }
    }

    val localFallbacks = listOf(
        "短期聊天记忆",
        "本地提醒调度",
        "最近画像缓存",
        "拨号求助能力",
    )

    val nextAction = if (reminders.isEmpty()) {
        "先把老人首个提醒和基础画像补齐，再准备同步到服务端。"
    } else if (contacts.isEmpty()) {
        "先补至少 1 位重要联系人，再把画像和提醒主档迁到服务端。"
    } else {
        "当前已经具备迁移到“服务端主存储 + 本地缓存”路线的最小字段。"
    }

    return ServiceStorageMigrationPlan(
        profileFieldsMovingToService = profileFields.distinct(),
        reminderFieldsMovingToService = reminderFields.distinct(),
        localFallbacksToKeep = localFallbacks,
        nextAction = when (communicationStyle) {
            CommunicationStyle.PatientDelicate -> "$nextAction 当前仍应保留耐心细腻型的本地降级回复。"
            CommunicationStyle.ConfidentMature -> "$nextAction 当前仍应保留自信成熟型的本地降级回复。"
        },
    )
}
