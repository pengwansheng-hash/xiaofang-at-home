package com.xiaofangathome.senior.data

import java.util.Calendar

const val FREQUENCY_DAILY = "每天"
const val FREQUENCY_WEEKLY = "每周"
const val FREQUENCY_ONCE = "单次"

fun normalizeWeeklyDays(
    frequencyLabel: String,
    weeklyDays: List<Int>,
): List<Int> {
    if (frequencyLabel != FREQUENCY_WEEKLY) return emptyList()
    return weeklyDays
        .filter { it in 1..7 }
        .distinct()
        .sorted()
        .ifEmpty { listOf(Calendar.MONDAY) }
}

fun formatWeeklyDaysLabel(weeklyDays: List<Int>): String {
    val dayLabels = mapOf(
        Calendar.MONDAY to "周一",
        Calendar.TUESDAY to "周二",
        Calendar.WEDNESDAY to "周三",
        Calendar.THURSDAY to "周四",
        Calendar.FRIDAY to "周五",
        Calendar.SATURDAY to "周六",
        Calendar.SUNDAY to "周日",
    )
    val labels = weeklyDays
        .distinct()
        .sortedWith(compareBy { if (it == Calendar.SUNDAY) 8 else it })
        .mapNotNull(dayLabels::get)
    return if (labels.isEmpty()) "每周一" else labels.joinToString("、")
}

fun formatReminderScheduleLabel(
    frequencyLabel: String,
    weeklyDays: List<Int>,
): String {
    return when (frequencyLabel) {
        FREQUENCY_WEEKLY -> formatWeeklyDaysLabel(weeklyDays)
        else -> frequencyLabel
    }
}

fun buildReminderDescription(
    frequencyLabel: String,
    weeklyDays: List<Int>,
    voiceEnabled: Boolean,
    alarmEnabled: Boolean,
): String {
    val scheduleLabel = formatReminderScheduleLabel(frequencyLabel, weeklyDays)
    val channels = buildList {
        if (voiceEnabled) add("语音播报")
        if (alarmEnabled) add("闹钟铃声")
    }.joinToString(" / ")
    return listOf(scheduleLabel, channels)
        .filter { it.isNotBlank() }
        .joinToString(" | ")
}

fun deriveReminderStatus(
    time: String,
    frequencyLabel: String,
    weeklyDays: List<Int>,
    completed: Boolean,
    now: Calendar = Calendar.getInstance(),
): ReminderStatus {
    if (completed) return ReminderStatus.Completed

    val hour = time.substringBefore(":").toIntOrNull() ?: 8
    val minute = time.substringAfter(":", "0").toIntOrNull() ?: 0
    val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val reminderMinutes = hour * 60 + minute

    return when (frequencyLabel) {
        FREQUENCY_ONCE -> if (reminderMinutes <= currentMinutes) ReminderStatus.Current else ReminderStatus.Planned
        FREQUENCY_WEEKLY -> {
            val today = now.get(Calendar.DAY_OF_WEEK)
            val normalizedDays = normalizeWeeklyDays(frequencyLabel, weeklyDays)
            if (today in normalizedDays && reminderMinutes <= currentMinutes) ReminderStatus.Current else ReminderStatus.Planned
        }
        else -> if (reminderMinutes <= currentMinutes) ReminderStatus.Current else ReminderStatus.Planned
    }
}

fun buildSingleReminderStateNote(
    time: String,
    status: ReminderStatus,
    frequencyLabel: String,
): String? {
    if (frequencyLabel != FREQUENCY_ONCE) return null
    if (time.isBlank()) return null
    return when (status) {
        ReminderStatus.Current -> "今天这个时间点需要处理"
        ReminderStatus.Planned -> "今天稍后会提醒"
        ReminderStatus.Completed -> "这次单次提醒已经处理"
    }
}
