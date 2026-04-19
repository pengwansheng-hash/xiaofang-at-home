package com.xiaofangathome.senior.data

data class ReminderItem(
    val id: String,
    val time: String,
    val title: String,
    val description: String,
    val status: ReminderStatus,
    val frequencyLabel: String = "每天",
    val weeklyDays: List<Int> = emptyList(),
    val voiceEnabled: Boolean = true,
    val alarmEnabled: Boolean = true,
)

enum class ReminderStatus {
    Completed,
    Current,
    Planned,
}

data class ContactItem(
    val id: String,
    val relation: String,
    val name: String,
    val phone: String,
)

data class ChatMessage(
    val id: String,
    val fromAi: Boolean,
    val content: String,
    val timeLabel: String? = null,
    val createdAt: Long = 0L,
)
