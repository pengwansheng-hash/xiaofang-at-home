package com.xiaofangathome.senior.ui.navigation

sealed class SeniorRoute(val route: String, val baseRoute: String = route) {
    data object Onboarding : SeniorRoute("onboarding")
    data object Home : SeniorRoute("home")
    data object Reminders : SeniorRoute("reminders")
    data object ReminderNew : SeniorRoute("reminder_new")
    data object ReminderEdit : SeniorRoute("reminder_edit/{reminderId}", "reminder_edit") {
        const val ARG_REMINDER_ID = "reminderId"

        fun createRoute(reminderId: String): String = "$baseRoute/$reminderId"
    }
    data object ReminderDetail : SeniorRoute("reminder_detail/{reminderId}", "reminder_detail") {
        const val ARG_REMINDER_ID = "reminderId"

        fun createRoute(reminderId: String): String = "$baseRoute/$reminderId"
    }
    data object Companion : SeniorRoute("companion")
    data object Contacts : SeniorRoute("contacts")
    data object Settings : SeniorRoute("settings")
    data object Binding : SeniorRoute("binding")
    data object Help : SeniorRoute("help")
}
