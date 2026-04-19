package com.xiaofangathome.senior

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xiaofangathome.senior.platform.ReminderDetailRouteExtra
import com.xiaofangathome.senior.ui.SeniorAppViewModel
import com.xiaofangathome.senior.ui.normalizePrimaryRoute
import com.xiaofangathome.senior.ui.components.SeniorBottomBar
import com.xiaofangathome.senior.ui.components.SeniorTopBar
import com.xiaofangathome.senior.ui.navigation.SeniorRoute
import com.xiaofangathome.senior.platform.hasBackgroundLocationPermission
import com.xiaofangathome.senior.platform.hasForegroundLocationPermission
import com.xiaofangathome.senior.ui.screens.BindingScreen
import com.xiaofangathome.senior.ui.screens.CompanionScreen
import com.xiaofangathome.senior.ui.screens.ContactsScreenV2
import com.xiaofangathome.senior.ui.screens.HelpRequestScreen
import com.xiaofangathome.senior.ui.screens.HomeScreen
import com.xiaofangathome.senior.ui.screens.OnboardingScreen
import com.xiaofangathome.senior.ui.screens.ReminderDetailScreen
import com.xiaofangathome.senior.ui.screens.ReminderEditorScreen
import com.xiaofangathome.senior.ui.screens.RemindersScreen
import com.xiaofangathome.senior.ui.screens.SettingsScreenV2
import com.xiaofangathome.senior.ui.theme.XiaofangTheme

class MainActivity : ComponentActivity() {
    private var pendingReminderId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingReminderId = intent?.getStringExtra(ReminderDetailRouteExtra.KEY_REMINDER_ID)
        setContent {
            XiaofangTheme {
                RequestNotificationPermission()
                SeniorAppRoot(
                    pendingReminderId = pendingReminderId,
                    onReminderRouteConsumed = { pendingReminderId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingReminderId = intent.getStringExtra(ReminderDetailRouteExtra.KEY_REMINDER_ID)
    }
}

@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun SeniorAppRoot(
    pendingReminderId: String?,
    onReminderRouteConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val viewModel: SeniorAppViewModel = viewModel()
    val state = viewModel.uiState
    val context = LocalContext.current
    var shouldStartVoiceAfterPermission by remember { mutableStateOf(false) }
    var shouldEnableLocationTrackingAfterPermission by remember { mutableStateOf(false) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: if (state.requiresOnboarding) {
        SeniorRoute.Onboarding.route
    } else {
        SeniorRoute.Home.route
    }
    val normalizedCurrentRoute = normalizePrimaryRoute(currentRoute)
    val showTopBar = normalizedCurrentRoute == SeniorRoute.Home.route ||
        normalizedCurrentRoute == SeniorRoute.Reminders.route ||
        normalizedCurrentRoute == SeniorRoute.Contacts.route
    val showBottomBar = !currentRoute.startsWith(SeniorRoute.ReminderDetail.baseRoute) &&
        !currentRoute.startsWith(SeniorRoute.ReminderEdit.baseRoute) &&
        currentRoute != SeniorRoute.Companion.route &&
        currentRoute != SeniorRoute.Onboarding.route &&
        currentRoute != SeniorRoute.ReminderNew.route &&
        currentRoute != SeniorRoute.Settings.route &&
        currentRoute != SeniorRoute.Binding.route &&
        currentRoute != SeniorRoute.Help.route

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.onMicrophonePermissionResult(granted)
            if (granted && shouldStartVoiceAfterPermission) {
                viewModel.startCompanionListening()
            }
            shouldStartVoiceAfterPermission = false
        },
    )
    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted && shouldEnableLocationTrackingAfterPermission) {
                viewModel.setHourlyLocationTrackingEnabled(true)
            } else if (shouldEnableLocationTrackingAfterPermission) {
                viewModel.onLocationTrackingPermissionDenied()
            }
            shouldEnableLocationTrackingAfterPermission = false
        },
    )
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            val foregroundGranted = result.values.any { it } || hasForegroundLocationPermission(context)
            if (!foregroundGranted) {
                if (shouldEnableLocationTrackingAfterPermission) {
                    viewModel.onLocationTrackingPermissionDenied()
                }
                shouldEnableLocationTrackingAfterPermission = false
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission(context)) {
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                if (shouldEnableLocationTrackingAfterPermission) {
                    viewModel.setHourlyLocationTrackingEnabled(true)
                }
                shouldEnableLocationTrackingAfterPermission = false
            }
        },
    )

    LaunchedEffect(pendingReminderId) {
        if (!pendingReminderId.isNullOrBlank() && viewModel.findReminder(pendingReminderId) != null) {
            navController.navigate(SeniorRoute.ReminderDetail.createRoute(pendingReminderId)) {
                launchSingleTop = true
            }
            onReminderRouteConsumed()
        }
    }

    LaunchedEffect(state.requiresOnboarding, currentRoute) {
        if (state.requiresOnboarding && currentRoute != SeniorRoute.Onboarding.route) {
            navController.navigate(SeniorRoute.Onboarding.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        } else if (!state.requiresOnboarding && currentRoute == SeniorRoute.Onboarding.route) {
            navController.navigate(SeniorRoute.Home.route) {
                popUpTo(SeniorRoute.Onboarding.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (showTopBar) {
                SeniorTopBar(
                    title = "小芳在家",
                    showBack = false,
                    showSettings = true,
                    showLeadingAvatar = true,
                    leadingAvatarText = state.preferredName.trim().take(1).ifBlank { "芳" },
                    onSettingsClick = { navController.navigate(SeniorRoute.Settings.route) },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                SeniorBottomBar(activeRoute = normalizedCurrentRoute) { route ->
                    if (route == SeniorRoute.Home.route) {
                        if (normalizedCurrentRoute != SeniorRoute.Home.route) {
                            val popped = navController.popBackStack(SeniorRoute.Home.route, false)
                            if (!popped) {
                                navController.navigate(SeniorRoute.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    } else {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (state.requiresOnboarding) {
                SeniorRoute.Onboarding.route
            } else {
                SeniorRoute.Home.route
            },
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(SeniorRoute.Onboarding.route) {
                OnboardingScreen(
                    state = state,
                    onDismissNotice = viewModel::clearOnboardingNotice,
                    onComplete = { preferredName, style, commonTopics, relation, contactName, contactPhone, reminderTitle, reminderTime ->
                        viewModel.completeOnboarding(
                            preferredName = preferredName,
                            style = style,
                            commonTopics = commonTopics,
                            relation = relation,
                            contactName = contactName,
                            contactPhone = contactPhone,
                            reminderTitle = reminderTitle,
                            reminderTime = reminderTime,
                        )
                    },
                )
            }
            composable(SeniorRoute.Home.route) {
                HomeScreen(
                    state = state,
                    onOpenSettings = { navController.navigate(SeniorRoute.Settings.route) },
                    onOpenCompanion = { navController.navigate(SeniorRoute.Companion.route) },
                    onOpenContacts = { navController.navigate(SeniorRoute.Contacts.route) },
                    onOpenReminders = { navController.navigate(SeniorRoute.Reminders.route) },
                    onCompleteReminder = viewModel::markReminderCompleted,
                    onLaterReminder = viewModel::snoozeReminder,
                )
            }
            composable(SeniorRoute.Reminders.route) {
                RemindersScreen(
                    state = state,
                    onOpenSettings = { navController.navigate(SeniorRoute.Settings.route) },
                    onOpenDetail = { reminderId ->
                        navController.navigate(SeniorRoute.ReminderDetail.createRoute(reminderId))
                    },
                    onOpenNewReminder = { navController.navigate(SeniorRoute.ReminderNew.route) },
                    onSyncReminders = viewModel::syncReminderPlan,
                )
            }
            composable(SeniorRoute.ReminderNew.route) {
                ReminderEditorScreen(
                    onBack = { navController.popBackStack() },
                    onSave = { title, time, frequencyLabel, weeklyDays, voiceEnabled, alarmEnabled ->
                        viewModel.addReminder(title, time, frequencyLabel, weeklyDays, voiceEnabled, alarmEnabled)
                        navController.popBackStack()
                    },
                )
            }
            composable(
                route = SeniorRoute.ReminderDetail.route,
                arguments = listOf(
                    navArgument(SeniorRoute.ReminderDetail.ARG_REMINDER_ID) { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val reminderId = backStackEntry.arguments?.getString(SeniorRoute.ReminderDetail.ARG_REMINDER_ID)
                ReminderDetailScreen(
                    reminder = viewModel.findReminder(reminderId),
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        reminderId?.let { navController.navigate(SeniorRoute.ReminderEdit.createRoute(it)) }
                    },
                    onDelete = {
                        reminderId?.let { viewModel.deleteReminder(it) }
                        navController.popBackStack()
                    },
                    onComplete = {
                        reminderId?.let { viewModel.markReminderCompleted(it) }
                        navController.popBackStack()
                    },
                    onSnooze = {
                        reminderId?.let { viewModel.snoozeReminder(it) }
                        navController.popBackStack()
                    },
                    onRequestHelp = {
                        val reminderTitle = viewModel.findReminder(reminderId)?.title ?: "当前提醒"
                        viewModel.beginHelpRequest(
                            sourceLabel = "提醒确认",
                            reason = "这条“${reminderTitle}”提醒现在需要家人帮忙确认。",
                        )
                        navController.navigate(SeniorRoute.Help.route)
                    },
                )
            }
            composable(
                route = SeniorRoute.ReminderEdit.route,
                arguments = listOf(
                    navArgument(SeniorRoute.ReminderEdit.ARG_REMINDER_ID) { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val reminderId = backStackEntry.arguments?.getString(SeniorRoute.ReminderEdit.ARG_REMINDER_ID)
                ReminderEditorScreen(
                    reminder = viewModel.findReminder(reminderId),
                    onBack = { navController.popBackStack() },
                    onSave = { title, time, frequencyLabel, weeklyDays, voiceEnabled, alarmEnabled ->
                        reminderId?.let {
                            viewModel.updateReminder(it, title, time, frequencyLabel, weeklyDays, voiceEnabled, alarmEnabled)
                        }
                        navController.popBackStack()
                    },
                )
            }
            composable(SeniorRoute.Companion.route) {
                CompanionScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onStartVoiceInput = {
                        if (hasPermission(context, Manifest.permission.RECORD_AUDIO)) {
                            shouldStartVoiceAfterPermission = false
                            viewModel.startCompanionListening()
                        } else {
                            shouldStartVoiceAfterPermission = true
                            viewModel.notifyMicrophonePermissionNeeded()
                            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            false
                        }
                    },
                    onStopVoiceInput = viewModel::stopCompanionListening,
                    onSendTextMessage = viewModel::sendCompanionText,
                    onToggleAutoSpeak = viewModel::toggleCompanionAutoSpeak,
                )
            }
            composable(SeniorRoute.Contacts.route) {
                ContactsScreenV2(
                    state = state,
                    onOpenSettings = { navController.navigate(SeniorRoute.Settings.route) },
                    onSaveProfile = viewModel::savePreferenceProfile,
                    onDismissProfileNotice = viewModel::clearProfileNotice,
                    onStyleChange = viewModel::setCommunicationStyle,
                    onAddContact = viewModel::addContact,
                    onRemoveContact = viewModel::removeContact,
                    onOpenBinding = { navController.navigate(SeniorRoute.Binding.route) },
                    onCallContact = { contact ->
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${contact.phone}")
                        }
                        context.startActivity(intent)
                    },
                )
            }
            composable(SeniorRoute.Settings.route) {
                SettingsScreenV2(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onSaveServiceConnection = viewModel::saveServiceConnectionSettings,
                    onDismissSettingsNotice = viewModel::clearSettingsNotice,
                    onRemoveSemanticMemory = viewModel::removeSemanticMemory,
                    onToggleHourlyLocationTracking = { enabled ->
                        if (!enabled) {
                            shouldEnableLocationTrackingAfterPermission = false
                            viewModel.setHourlyLocationTrackingEnabled(false)
                        } else if (hasForegroundLocationPermission(context) && hasBackgroundLocationPermission(context)) {
                            shouldEnableLocationTrackingAfterPermission = false
                            viewModel.setHourlyLocationTrackingEnabled(true)
                        } else {
                            shouldEnableLocationTrackingAfterPermission = true
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        }
                    },
                )
            }
            composable(SeniorRoute.Binding.route) {
                BindingScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onRefreshCode = viewModel::refreshBindingCode,
                    onCodeCopied = viewModel::markBindingCodeCopied,
                )
            }
            composable(SeniorRoute.Help.route) {
                HelpRequestScreen(
                    state = state,
                    onBack = {
                        viewModel.clearHelpRequest()
                        navController.popBackStack()
                    },
                    onCallContact = { contact ->
                        viewModel.markHelpDialed(contact.id)
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${contact.phone}")
                        }
                        context.startActivity(intent)
                    },
                )
            }
        }
    }
}

private fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
