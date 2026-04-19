package com.xiaofangathome.senior.data

data class ReminderDraft(
    val sourceText: String,
    val title: String,
    val time: String,
    val frequencyLabel: String,
    val weeklyDays: List<Int> = emptyList(),
    val note: String,
)
