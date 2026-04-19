package com.xiaofangathome.senior.ui.screens

import android.widget.TimePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.xiaofangathome.senior.data.ChatMessage
import com.xiaofangathome.senior.data.ContactItem
import com.xiaofangathome.senior.data.FREQUENCY_DAILY
import com.xiaofangathome.senior.data.FREQUENCY_ONCE
import com.xiaofangathome.senior.data.FREQUENCY_WEEKLY
import com.xiaofangathome.senior.data.LocationSample
import com.xiaofangathome.senior.data.MemoryLayer
import com.xiaofangathome.senior.data.ReminderItem
import com.xiaofangathome.senior.data.ReminderStatus
import com.xiaofangathome.senior.data.SemanticMemoryItem
import com.xiaofangathome.senior.data.SemanticMemoryType
import com.xiaofangathome.senior.data.formatReminderScheduleLabel
import com.xiaofangathome.senior.platform.CommunicationStyle
import com.xiaofangathome.senior.ui.SeniorUiState
import com.xiaofangathome.senior.ui.contactBadgeLabel
import com.xiaofangathome.senior.ui.components.EmptyStateCard
import com.xiaofangathome.senior.ui.components.EmergencyContactCard
import com.xiaofangathome.senior.ui.components.HighlightReminderCard
import com.xiaofangathome.senior.ui.components.NoticeCard
import com.xiaofangathome.senior.ui.components.QuickActionCard
import com.xiaofangathome.senior.ui.components.TodoListItem
import com.xiaofangathome.senior.ui.components.VoiceActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    state: SeniorUiState,
    onOpenSettings: () -> Unit,
    onOpenCompanion: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenReminders: () -> Unit,
    onCompleteReminder: (String) -> Unit,
    onLaterReminder: (String) -> Unit,
) {
    val homeCurrentReminder = state.homeTasks.firstOrNull { it.status == ReminderStatus.Current }
        ?: state.homeTasks.firstOrNull()
        ?: state.reminders.firstOrNull { it.status == ReminderStatus.Current }
        ?: state.reminders.firstOrNull()
    val homeTodoReminders = state.homeTasks.ifEmpty { state.reminders }.take(3)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = buildGreetingMessage(state.preferredName),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = buildTodayLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            if (homeCurrentReminder != null) {
                HighlightReminderCard(
                    time = homeCurrentReminder.time,
                    title = homeCurrentReminder.title,
                    onComplete = { onCompleteReminder(homeCurrentReminder.id) },
                    onLater = { onLaterReminder(homeCurrentReminder.id) },
                )
            } else {
                EmptyStateCard(
                    title = "当前最重要提醒",
                    message = "今天还没有安排待办，可以去提醒页补一条计划。",
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    QuickActionCard(
                        title = "联系家人",
                        iconVoice = false,
                        onClick = onOpenContacts,
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    QuickActionCard(
                        title = "陪伴聊天",
                        iconVoice = true,
                        onClick = onOpenCompanion,
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "今日待办简表",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onOpenReminders) {
                    Text("查看全部")
                }
            }
        }
        if (homeTodoReminders.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "今天暂时没有待办",
                    message = "去提醒管理页加一条，首页就会自动带出来。",
                )
            }
        } else {
            items(homeTodoReminders, key = { it.id }) { reminder ->
                TodoListItem(
                    item = reminder,
                    onClick = onOpenReminders,
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
    return
    val currentReminder = state.homeTasks.firstOrNull { it.status == ReminderStatus.Current }
        ?: state.homeTasks.firstOrNull()
        ?: state.reminders.firstOrNull { it.status == ReminderStatus.Current }
        ?: state.reminders.firstOrNull()
    val todoReminders = state.homeTasks.ifEmpty { state.reminders }.take(3)

    PageList(title = "小芳在家", subtitle = buildTodayLabel()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = buildGreetingMessage(state.preferredName),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (todoReminders.isEmpty()) {
                            "今天先慢一点，有需要时再安排提醒。"
                        } else {
                            "先把今天最重要的几件事看一眼。"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onOpenSettings) {
                    Text("设置")
                }
            }
        }
        item {
            if (currentReminder != null) {
                HighlightReminderCard(
                    time = currentReminder.time,
                    title = currentReminder.title,
                    onComplete = { onCompleteReminder(currentReminder.id) },
                    onLater = { onLaterReminder(currentReminder.id) },
                )
            } else {
                EmptyStateCard(
                    title = "当前最重要提醒",
                    message = "今天还没有安排待办，可以去提醒页补一条计划。",
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    QuickActionCard(
                        title = "联系家人",
                        iconVoice = false,
                        onClick = onOpenContacts,
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    QuickActionCard(
                        title = "陪伴聊天",
                        iconVoice = true,
                        onClick = onOpenCompanion,
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle("今日待办简表")
                TextButton(onClick = onOpenReminders) {
                    Text("查看全部")
                }
            }
        }
        if (todoReminders.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "今天暂时没有待办",
                    message = "去提醒管理页加一条，首页就会自动带出来。",
                )
            }
        } else {
            items(todoReminders, key = { it.id }) { reminder ->
                TodoListItem(
                    item = reminder,
                    onClick = onOpenReminders,
                )
            }
        }
    }
    return
    PageList(title = "小芳在家", subtitle = buildTodayLabel()) {
        item {
            SectionCard {
                Text(
                    text = buildGreetingMessage(state.preferredName),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (state.homeTasks.isEmpty()) "今天没有待办压着你，可以轻松一点。" else "先把今天最要紧的几件事看一眼。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(label = "陪伴聊天", modifier = Modifier.weight(1f), onClick = onOpenCompanion)
                ActionButton(label = "联系家人", modifier = Modifier.weight(1f), onClick = onOpenContacts)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(label = "提醒管理", modifier = Modifier.weight(1f), onClick = onOpenReminders)
                ActionButton(label = "设置", modifier = Modifier.weight(1f), onClick = onOpenSettings)
            }
        }
        item {
            SectionTitle("今天待办")
        }
        if (state.homeTasks.isEmpty()) {
            item {
                SectionCard {
                    Text("今天暂时没有待办。", fontWeight = FontWeight.SemiBold)
                    Text("可以去提醒页给自己安排一条计划。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(state.homeTasks.take(4), key = { it.id }) { reminder ->
                ReminderRow(
                    reminder = reminder,
                    onComplete = { onCompleteReminder(reminder.id) },
                    onLater = { onLaterReminder(reminder.id) },
                    onClick = onOpenReminders,
                )
            }
        }
    }
}

@Composable
fun RemindersScreen(
    state: SeniorUiState,
    onOpenSettings: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenNewReminder: () -> Unit,
    onSyncReminders: () -> Unit,
) {
    val remindersRemainingCount = state.reminders.count { it.status != ReminderStatus.Completed }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ReminderOverviewCard(
                remainingCount = remindersRemainingCount,
                syncMessage = state.reminderSyncMessage,
                onOpenNewReminder = onOpenNewReminder,
                onSyncReminders = onSyncReminders,
            )
        }
        if (state.reminders.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "还没有提醒",
                    message = "先加一条，首页和陪伴页都会跟着更新。",
                )
            }
        } else {
            item {
                ReminderTimelineSection(
                    reminders = state.reminders,
                    onOpenDetail = onOpenDetail,
                )
            }
        }
        item {
            NoticeCard(
                title = "陪伴守护",
                message = buildReminderEncouragement(state.reminders),
            )
        }
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
    return
    val remainingCount = state.reminders.count { it.status != ReminderStatus.Completed }

    PageList(
        title = "提醒管理",
        subtitle = "今天还有 $remainingCount 件待处理",
    ) {
        item {
            ReminderOverviewCard(
                remainingCount = remainingCount,
                syncMessage = state.reminderSyncMessage,
                onOpenNewReminder = onOpenNewReminder,
                onSyncReminders = onSyncReminders,
            )
        }
        if (state.reminders.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "还没有提醒",
                    message = "先加一条，首页和陪伴页都会跟着更新。",
                )
            }
        } else {
            item {
                ReminderTimelineSection(
                    reminders = state.reminders,
                    onOpenDetail = onOpenDetail,
                )
            }
        }
        item {
            NoticeCard(
                title = "陪伴守护",
                message = buildReminderEncouragement(state.reminders),
            )
        }
        item {
            ActionButton(
                label = "打开设置",
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenSettings,
                filled = false,
            )
        }
    }
    return
    PageList(title = "提醒管理", subtitle = "${state.reminders.count { it.status != ReminderStatus.Completed }} 条待处理") {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(label = "新增提醒", modifier = Modifier.weight(1f), onClick = onOpenNewReminder)
                ActionButton(label = "同步提醒", modifier = Modifier.weight(1f), onClick = onSyncReminders)
            }
        }
        item {
            ActionButton(label = "打开设置", modifier = Modifier.fillMaxWidth(), onClick = onOpenSettings)
        }
        if (state.reminders.isEmpty()) {
            item {
                SectionCard {
                    Text("还没有提醒。", fontWeight = FontWeight.SemiBold)
                    Text("先加一条，后面就会在首页和聊天里带出来。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(state.reminders, key = { it.id }) { reminder ->
                ReminderRow(
                    reminder = reminder,
                    onComplete = {},
                    onLater = {},
                    onClick = { onOpenDetail(reminder.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorScreen(
    reminder: ReminderItem? = null,
    onBack: () -> Unit,
    onSave: (String, String, String, List<Int>, Boolean, Boolean) -> Unit,
) {
    var title by rememberSaveable(reminder?.id) { mutableStateOf(reminder?.title.orEmpty()) }
    var time by rememberSaveable(reminder?.id) { mutableStateOf(reminder?.time ?: "08:00") }
    var frequency by rememberSaveable(reminder?.id) { mutableStateOf(reminder?.frequencyLabel ?: FREQUENCY_DAILY) }
    var weeklyDays by rememberSaveable(reminder?.id) { mutableStateOf(reminder?.weeklyDays ?: emptyList()) }
    var voiceEnabled by rememberSaveable(reminder?.id) { mutableStateOf(reminder?.voiceEnabled ?: true) }
    var alarmEnabled by rememberSaveable(reminder?.id) { mutableStateOf(reminder?.alarmEnabled ?: true) }

    PageList(title = if (reminder == null) "新增提醒" else "编辑提醒", subtitle = "把提醒内容和时间定下来") {
        item {
            SectionCard {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("提醒内容") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                TimeField(time = time, onTimeChanged = { time = it })
                Spacer(modifier = Modifier.height(12.dp))
                Text("频率", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(FREQUENCY_DAILY, FREQUENCY_WEEKLY, FREQUENCY_ONCE).forEach { label ->
                        FilterChip(
                            selected = frequency == label,
                            onClick = { frequency = label },
                            label = { Text(label) },
                        )
                    }
                }
                if (frequency == FREQUENCY_WEEKLY) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("每周日期", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val labels: List<String> = listOf("一", "二", "三", "四", "五", "六", "日")
                        labels.forEachIndexed { index, label ->
                            val day = index + 1
                            FilterChip(
                                selected = weeklyDays.contains(day),
                                onClick = {
                                    weeklyDays = if (weeklyDays.contains(day)) {
                                        weeklyDays.filterNot { selectedDay -> selectedDay == day }
                                    } else {
                                        (weeklyDays + listOf(day)).sorted()
                                    }
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                SettingSwitchRow(
                    title = "语音播报",
                    checked = voiceEnabled,
                    onCheckedChange = { voiceEnabled = it },
                )
                SettingSwitchRow(
                    title = "铃声提醒",
                    checked = alarmEnabled,
                    onCheckedChange = { alarmEnabled = it },
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                    Text("返回")
                }
                Button(
                    onClick = { onSave(title.trim(), time, frequency, weeklyDays, voiceEnabled, alarmEnabled) },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    enabled = title.trim().isNotBlank(),
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
fun ReminderDetailScreen(
    reminder: ReminderItem?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onRequestHelp: () -> Unit,
) {
    PageList(title = "提醒详情", subtitle = reminder?.title ?: "未找到提醒") {
        item {
            SectionCard {
                if (reminder == null) {
                    Text("这条提醒已经不存在了。")
                } else {
                    InfoLine("时间", reminder.time)
                    InfoLine("频率", formatReminderScheduleLabel(reminder.frequencyLabel, reminder.weeklyDays))
                    InfoLine("状态", reminderStatusLabel(reminder.status))
                    InfoLine("说明", reminder.description)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(label = "返回", modifier = Modifier.weight(1f), onClick = onBack)
                ActionButton(label = "编辑", modifier = Modifier.weight(1f), onClick = onEdit, filled = false)
            }
        }
        if (reminder != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(label = "完成", modifier = Modifier.weight(1f), onClick = onComplete)
                    ActionButton(label = "稍后", modifier = Modifier.weight(1f), onClick = onSnooze, filled = false)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(label = "求助家人", modifier = Modifier.weight(1f), onClick = onRequestHelp, filled = false)
                    ActionButton(label = "删除", modifier = Modifier.weight(1f), onClick = onDelete, filled = false)
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    state: SeniorUiState,
    onDismissNotice: () -> Unit,
    onComplete: (String, CommunicationStyle, String, String, String, String, String, String) -> Unit,
) {
    var preferredName by rememberSaveable { mutableStateOf(state.preferredName) }
    var style by rememberSaveable { mutableStateOf(state.communicationStyle) }
    var commonTopics by rememberSaveable { mutableStateOf(state.commonTopics) }
    var relation by rememberSaveable { mutableStateOf("子女") }
    var contactName by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable { mutableStateOf("") }
    var reminderTitle by rememberSaveable { mutableStateOf("按时吃药") }
    var reminderTime by rememberSaveable { mutableStateOf("08:00") }

    PageList(title = "开始使用", subtitle = "先把最基础的信息补齐") {
        state.onboardingNotice?.takeIf { it.isNotBlank() }?.let { notice ->
            item {
                NoticeBlock(message = notice, onDismiss = onDismissNotice)
            }
        }
        item {
            SectionCard {
                OutlinedTextField(
                    value = preferredName,
                    onValueChange = { preferredName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("怎么称呼你") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("聊天风格", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = style == CommunicationStyle.PatientDelicate,
                        onClick = { style = CommunicationStyle.PatientDelicate },
                        label = { Text("温和") },
                    )
                    FilterChip(
                        selected = style == CommunicationStyle.ConfidentMature,
                        onClick = { style = CommunicationStyle.ConfidentMature },
                        label = { Text("利落") },
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = commonTopics,
                    onValueChange = { commonTopics = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("常聊话题") },
                    placeholder = { Text("比如做饭、散步、新闻") },
                )
            }
        }
        item {
            SectionCard {
                Text("首位联系人", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = relation, onValueChange = { relation = it }, modifier = Modifier.fillMaxWidth(), label = { Text("关系") })
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = contactName, onValueChange = { contactName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("姓名") })
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, modifier = Modifier.fillMaxWidth(), label = { Text("电话") })
            }
        }
        item {
            SectionCard {
                OutlinedTextField(
                    value = reminderTitle,
                    onValueChange = { reminderTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("第一条提醒") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                TimeField(time = reminderTime, onTimeChanged = { reminderTime = it })
            }
        }
        item {
            Button(
                onClick = {
                    onComplete(
                        preferredName.trim(),
                        style,
                        commonTopics.trim(),
                        relation.trim(),
                        contactName.trim(),
                        contactPhone.trim(),
                        reminderTitle.trim(),
                        reminderTime,
                    )
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                enabled = preferredName.trim().isNotBlank() && contactName.trim().isNotBlank() && contactPhone.trim().isNotBlank(),
            ) {
                Text("完成初始化")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanionScreen(
    state: SeniorUiState,
    onBack: () -> Unit,
    onStartVoiceInput: () -> Boolean,
    onStopVoiceInput: () -> Unit,
    onSendTextMessage: (String) -> Unit,
    onToggleAutoSpeak: () -> Unit,
) {
    var companionInputText by rememberSaveable { mutableStateOf("") }
    val companionListState = rememberLazyListState()
    val companionQuickReplies = remember(state.homeTasks, state.currentHelpRequest) {
        buildCompanionQuickReplies(state)
    }

    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            companionListState.animateScrollToItem(state.chatMessages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CompanionHeader(
            modelConnected = state.companionModelConnected,
            autoSpeak = state.companionAutoSpeak,
            onBack = onBack,
            onToggleAutoSpeak = onToggleAutoSpeak,
        )
        if (state.companionStatusMessage.isNotBlank()) {
            NoticeCard(
                title = "陪伴提示",
                message = state.companionStatusMessage,
            )
        }
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = companionListState,
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.chatMessages, key = { it.id }) { message ->
                    CompanionMessageBubble(message = message)
                }
            }
        }
        state.latestTranscript?.takeIf { it.isNotBlank() }?.let { transcript ->
            NoticeCard(
                title = "刚刚识别到的话",
                message = transcript,
            )
        }
        if (companionQuickReplies.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                companionQuickReplies.forEach { reply ->
                    FilterChip(
                        selected = false,
                        onClick = { onSendTextMessage(reply) },
                        label = { Text(reply) },
                    )
                }
            }
        }
        SectionCard {
            OutlinedTextField(
                value = companionInputText,
                onValueChange = { companionInputText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("想对小芳说点什么") },
                maxLines = 4,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VoiceActionButton(
                    isRecording = state.isRecordingVoice,
                    onClick = {
                        if (state.isRecordingVoice) {
                            onStopVoiceInput()
                        } else {
                            onStartVoiceInput()
                        }
                    },
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = if (state.isRecordingVoice) {
                            "正在听您说话，点一下结束。"
                        } else {
                            "也可以直接输入，发出去后小芳会继续陪您聊。"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            val clean = companionInputText.trim()
                            if (clean.isNotBlank()) {
                                onSendTextMessage(clean)
                                companionInputText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "发送",
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("发送消息")
                    }
                }
            }
        }
    }
    return
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeaderRow(title = "小芳聊天", subtitle = if (state.companionModelConnected) "已连接大模型" else "当前可能走本地回复", onBack = onBack)
        if (state.companionStatusMessage.isNotBlank()) {
            NoticeBlock(message = state.companionStatusMessage, onDismiss = null)
        }
        SectionCard(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.chatMessages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }
            }
        }
        SectionCard {
            SettingSwitchRow(
                title = "自动播报回复",
                checked = state.companionAutoSpeak,
                onCheckedChange = { onToggleAutoSpeak() },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("想对小芳说些什么") },
                    maxLines = 4,
                )
                IconButton(
                    onClick = {
                        if (state.isRecordingVoice) onStopVoiceInput() else onStartVoiceInput()
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                ) {
                    Icon(Icons.Rounded.KeyboardVoice, contentDescription = "语音")
                }
                IconButton(
                    onClick = {
                        val clean = inputText.trim()
                        if (clean.isNotBlank()) {
                            onSendTextMessage(clean)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "发送", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactsScreenV2(
    state: SeniorUiState,
    onOpenSettings: () -> Unit,
    onSaveProfile: (String, String) -> Unit,
    onDismissProfileNotice: () -> Unit,
    onStyleChange: (CommunicationStyle) -> Unit,
    onAddContact: (String, String, String) -> Unit,
    onRemoveContact: (String) -> Unit,
    onOpenBinding: () -> Unit,
    onCallContact: (ContactItem) -> Unit,
) {
    var restoredPreferredName by rememberSaveable(state.preferredName) { mutableStateOf(state.preferredName) }
    var restoredTabooWords by rememberSaveable(state.tabooWords) { mutableStateOf(state.tabooWords) }
    var restoredRelation by rememberSaveable { mutableStateOf("") }
    var restoredName by rememberSaveable { mutableStateOf("") }
    var restoredPhone by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        state.profileNotice?.takeIf { it.isNotBlank() }?.let { notice ->
            item { NoticeBlock(message = notice, onDismiss = onDismissProfileNotice) }
        }
        item {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "我希望被称呼为：",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SectionCard {
                    OutlinedTextField(
                        value = restoredPreferredName,
                        onValueChange = { restoredPreferredName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("称呼") },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = restoredTabooWords,
                        onValueChange = { restoredTabooWords = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("少聊或避开的话题") },
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "沟通方式：",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CommunicationStyleCard(
                        title = "温和",
                        subtitle = "像熟悉的晚辈一样慢慢聊",
                        selected = state.communicationStyle == CommunicationStyle.PatientDelicate,
                        onClick = { onStyleChange(CommunicationStyle.PatientDelicate) },
                    )
                    CommunicationStyleCard(
                        title = "利落",
                        subtitle = "表达直接一点，信息更清楚",
                        selected = state.communicationStyle == CommunicationStyle.ConfidentMature,
                        onClick = { onStyleChange(CommunicationStyle.ConfidentMature) },
                    )
                }
            }
        }
        item {
            Text(
                text = "紧急联系人",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (state.contacts.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "还没有联系人",
                    message = "先加一位家人，紧急时可以一键拨号。",
                )
            }
        } else {
            items(state.contacts, key = { it.id }) { contact ->
                EmergencyContactCard(
                    item = contact,
                    onCall = { onCallContact(contact) },
                    onDelete = { onRemoveContact(contact.id) },
                )
            }
        }
        item {
            OutlinedButton(
                onClick = onOpenBinding,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "绑定家人")
                Spacer(modifier = Modifier.width(8.dp))
                Text("绑定家人 / 新增联系人")
            }
        }
        item {
            SectionCard {
                Text(
                    text = "新增联系人",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = restoredRelation,
                    onValueChange = { restoredRelation = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("关系") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = restoredName,
                    onValueChange = { restoredName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("姓名") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = restoredPhone,
                    onValueChange = { restoredPhone = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("电话") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        onAddContact(restoredRelation.trim(), restoredName.trim(), restoredPhone.trim())
                        restoredRelation = ""
                        restoredName = ""
                        restoredPhone = ""
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    enabled = restoredRelation.trim().isNotBlank() &&
                        restoredName.trim().isNotBlank() &&
                        restoredPhone.trim().isNotBlank(),
                ) {
                    Text("添加新的联系人")
                }
            }
        }
        item {
            Button(
                onClick = { onSaveProfile(restoredPreferredName.trim(), restoredTabooWords.trim()) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text("保存修改")
            }
        }
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
    return
    var preferredNameDraft by rememberSaveable(state.preferredName) { mutableStateOf(state.preferredName) }
    var tabooWordsDraft by rememberSaveable(state.tabooWords) { mutableStateOf(state.tabooWords) }
    var contactRelationDraft by rememberSaveable { mutableStateOf("") }
    var contactNameDraft by rememberSaveable { mutableStateOf("") }
    var contactPhoneDraft by rememberSaveable { mutableStateOf("") }

    PageList(title = "家人与资料", subtitle = "保存称呼、沟通方式和紧急联系人") {
        state.profileNotice?.takeIf { it.isNotBlank() }?.let { notice ->
            item { NoticeBlock(message = notice, onDismiss = onDismissProfileNotice) }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "我希望被称呼为：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = preferredNameDraft.ifBlank { "小芳" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        TextButton(onClick = onOpenSettings) {
                            Text("设置")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = preferredNameDraft,
                        onValueChange = { preferredNameDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("称呼") },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tabooWordsDraft,
                        onValueChange = { tabooWordsDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("少聊或避开的话题") },
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "沟通方式：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CommunicationStyleCard(
                        title = "温和",
                        subtitle = "像熟悉的晚辈一样慢慢聊",
                        selected = state.communicationStyle == CommunicationStyle.PatientDelicate,
                        onClick = { onStyleChange(CommunicationStyle.PatientDelicate) },
                    )
                    CommunicationStyleCard(
                        title = "利落",
                        subtitle = "表达直接一点，信息更清楚",
                        selected = state.communicationStyle == CommunicationStyle.ConfidentMature,
                        onClick = { onStyleChange(CommunicationStyle.ConfidentMature) },
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle("紧急联系人")
                TextButton(onClick = onOpenBinding) {
                    Text("绑定家人")
                }
            }
        }
        if (state.contacts.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "还没有联系人",
                    message = "先加一位家人，紧急时可以一键拨号。",
                )
            }
        } else {
            items(state.contacts, key = { it.id }) { contact ->
                EmergencyContactCard(
                    item = contact,
                    onCall = { onCallContact(contact) },
                    onDelete = { onRemoveContact(contact.id) },
                )
            }
        }
        item {
            SectionCard {
                Text(
                    text = "新增联系人",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = contactRelationDraft,
                    onValueChange = { contactRelationDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("关系") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = contactNameDraft,
                    onValueChange = { contactNameDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("姓名") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = contactPhoneDraft,
                    onValueChange = { contactPhoneDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("电话") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        onAddContact(
                            contactRelationDraft.trim(),
                            contactNameDraft.trim(),
                            contactPhoneDraft.trim(),
                        )
                        contactRelationDraft = ""
                        contactNameDraft = ""
                        contactPhoneDraft = ""
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    enabled = contactRelationDraft.trim().isNotBlank() &&
                        contactNameDraft.trim().isNotBlank() &&
                        contactPhoneDraft.trim().isNotBlank(),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "新增联系人")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加新的联系人")
                }
            }
        }
        item {
            Button(
                onClick = { onSaveProfile(preferredNameDraft.trim(), tabooWordsDraft.trim()) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            ) {
                Text("保存资料")
            }
        }
    }
    return
    var preferredName by rememberSaveable(state.preferredName) { mutableStateOf(state.preferredName) }
    var tabooWords by rememberSaveable(state.tabooWords) { mutableStateOf(state.tabooWords) }
    var relation by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }

    PageList(title = "家人与资料", subtitle = "管理联系人和聊天边界") {
        state.profileNotice?.takeIf { it.isNotBlank() }?.let { notice ->
            item { NoticeBlock(message = notice, onDismiss = onDismissProfileNotice) }
        }
        item {
            SectionCard {
                OutlinedTextField(value = preferredName, onValueChange = { preferredName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("称呼") })
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tabooWords,
                    onValueChange = { tabooWords = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("少聊或避开的话题") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("聊天风格", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.communicationStyle == CommunicationStyle.PatientDelicate,
                        onClick = { onStyleChange(CommunicationStyle.PatientDelicate) },
                        label = { Text("温和") },
                    )
                    FilterChip(
                        selected = state.communicationStyle == CommunicationStyle.ConfidentMature,
                        onClick = { onStyleChange(CommunicationStyle.ConfidentMature) },
                        label = { Text("利落") },
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onSaveProfile(preferredName.trim(), tabooWords.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("保存资料")
                }
            }
        }
        item {
            SectionCard {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("家人绑定", fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onOpenBinding) { Text("查看绑定码") }
                }
                Text(
                    text = "当前绑定码：${state.bindingCode}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionTitle("联系人") }
        if (state.contacts.isEmpty()) {
            item {
                SectionCard {
                    Text("还没有联系人。")
                }
            }
        } else {
            items(state.contacts, key = { it.id }) { contact ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(contactBadgeLabel(contact.relation, contact.name), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${contact.relation} ${contact.name}", fontWeight = FontWeight.SemiBold)
                            Text(contact.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { onCallContact(contact) }) { Text("拨号") }
                        TextButton(onClick = { onRemoveContact(contact.id) }) { Text("删除") }
                    }
                }
            }
        }
        item {
            SectionCard {
                Text("新增联系人", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = relation, onValueChange = { relation = it }, modifier = Modifier.fillMaxWidth(), label = { Text("关系") })
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("姓名") })
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, modifier = Modifier.fillMaxWidth(), label = { Text("电话") })
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        onAddContact(relation.trim(), name.trim(), phone.trim())
                        relation = ""
                        name = ""
                        phone = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = relation.trim().isNotBlank() && name.trim().isNotBlank() && phone.trim().isNotBlank(),
                ) {
                    Text("添加联系人")
                }
            }
        }
        item {
            ActionButton(label = "打开设置", modifier = Modifier.fillMaxWidth(), onClick = onOpenSettings, filled = false)
        }
    }
}

@Composable
fun SettingsScreenV2(
    state: SeniorUiState,
    onBack: () -> Unit,
    onSaveServiceConnection: (String, String) -> Unit,
    onDismissSettingsNotice: () -> Unit,
    onRemoveSemanticMemory: (String) -> Unit,
    onToggleHourlyLocationTracking: (Boolean) -> Unit,
) {
    var baseUrl by rememberSaveable(state.careServiceBaseUrl) { mutableStateOf(state.careServiceBaseUrl) }
    var seniorId by rememberSaveable(state.careServiceSeniorId) { mutableStateOf(state.careServiceSeniorId) }
    val profileMemories = state.semanticMemories.filter { it.memoryLayer == MemoryLayer.Profile }
    val preferenceMemories = state.semanticMemories.filter { it.memoryLayer == MemoryLayer.Preference }
    val recentMemories = state.semanticMemories.filter { it.memoryLayer == MemoryLayer.RecentState }

    PageList(title = "设置", subtitle = "服务连接、记忆和位置采集", onBack = onBack) {
        state.settingsNotice?.takeIf { it.isNotBlank() }?.let { notice ->
            item { NoticeBlock(message = notice, onDismiss = onDismissSettingsNotice) }
        }
        item {
            SectionCard {
                OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("服务地址") })
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = seniorId, onValueChange = { seniorId = it }, modifier = Modifier.fillMaxWidth(), label = { Text("老人 ID") })
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onSaveServiceConnection(baseUrl.trim(), seniorId.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("保存连接设置")
                }
            }
        }
        item {
            SectionCard {
                SettingSwitchRow(
                    title = "按小时采集位置信息",
                    checked = state.hourlyLocationTrackingEnabled,
                    onCheckedChange = onToggleHourlyLocationTracking,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildLocationTrackingSummary(state.locationSamples),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionTitle("多层记忆") }
        item { MemoryGroup(title = "长期画像", memories = profileMemories, onRemoveSemanticMemory = onRemoveSemanticMemory) }
        item { MemoryGroup(title = "交流偏好", memories = preferenceMemories, onRemoveSemanticMemory = onRemoveSemanticMemory) }
        item { MemoryGroup(title = "近期状态", memories = recentMemories, onRemoveSemanticMemory = onRemoveSemanticMemory) }
    }
}

@Composable
fun BindingScreen(
    state: SeniorUiState,
    onBack: () -> Unit,
    onRefreshCode: () -> Unit,
    onCodeCopied: () -> Unit,
) {
    PageList(title = "家人绑定", subtitle = "让家人扫一扫或输入绑定码", onBack = onBack) {
        item {
            SectionCard(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("绑定码", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.bindingCode, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("更新时间：${state.bindingUpdatedAt}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        state.bindingNotice?.takeIf { it.isNotBlank() }?.let { notice ->
            item { NoticeBlock(message = notice, onDismiss = null) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(label = "刷新绑定码", modifier = Modifier.weight(1f), onClick = onRefreshCode)
                ActionButton(label = "标记已复制", modifier = Modifier.weight(1f), onClick = onCodeCopied, filled = false)
            }
        }
    }
}

@Composable
fun HelpRequestScreen(
    state: SeniorUiState,
    onBack: () -> Unit,
    onCallContact: (ContactItem) -> Unit,
) {
    val help = state.currentHelpRequest

    PageList(title = "联系家人", subtitle = help?.sourceLabel ?: "求助入口", onBack = onBack) {
        item {
            SectionCard {
                Text(help?.reason ?: "当前没有待处理的求助。", fontWeight = FontWeight.SemiBold)
                help?.recommendedContact?.let { contact ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("建议优先联系：${contact.relation} ${contact.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (!help?.contacts.isNullOrEmpty()) {
            items(help?.contacts ?: emptyList(), key = { it.id }) { contact ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${contact.relation} ${contact.name}", fontWeight = FontWeight.SemiBold)
                            Text(contact.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ActionButton(label = "拨号", modifier = Modifier.width(92.dp), onClick = { onCallContact(contact) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PageList(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeaderRow(title = title, subtitle = subtitle, onBack = onBack)
        }
        content()
    }
}

@Composable
private fun HeaderRow(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        onBack?.let {
            TextButton(onClick = it) { Text("返回") }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    filled: Boolean = true,
) {
    if (filled) {
        Button(onClick = onClick, modifier = modifier.heightIn(min = 52.dp)) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 52.dp)) { Text(label) }
    }
}

@Composable
private fun ReminderRow(
    reminder: ReminderItem,
    onComplete: () -> Unit,
    onLater: () -> Unit,
    onClick: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.title, fontWeight = FontWeight.SemiBold)
                Text(
                    "${reminder.time} · ${formatReminderScheduleLabel(reminder.frequencyLabel, reminder.weeklyDays)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(reminderStatusLabel(reminder.status), color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = onClick) { Text("详情") }
        }
        if (reminder.status != ReminderStatus.Completed) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onLater, modifier = Modifier.weight(1f)) { Text("稍后") }
                Button(onClick = onComplete, modifier = Modifier.weight(1f)) { Text("完成") }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val bubbleColor = if (message.fromAi) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
    val textColor = if (message.fromAi) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromAi) Arrangement.Start else Arrangement.End,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(modifier = Modifier.widthIn(max = 280.dp).padding(14.dp)) {
                Text(message.content, color = textColor)
                message.timeLabel?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = textColor.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun NoticeBlock(
    message: String,
    onDismiss: (() -> Unit)?,
) {
    SectionCard {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        onDismiss?.let {
            TextButton(onClick = it) { Text("知道了") }
        }
    }
}

@Composable
private fun ReminderOverviewCard(
    remainingCount: Int,
    syncMessage: String,
    onOpenNewReminder: () -> Unit,
    onSyncReminders: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "今天还有",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = remainingCount.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "件重要事",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = syncMessage.ifBlank { "按时作息，重要的提醒我会继续帮您记着。" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(
                    label = "新增提醒",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenNewReminder,
                )
                ActionButton(
                    label = "同步提醒",
                    modifier = Modifier.weight(1f),
                    onClick = onSyncReminders,
                    filled = false,
                )
            }
        }
    }
}

@Composable
private fun ReminderTimelineSection(
    reminders: List<ReminderItem>,
    onOpenDetail: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        reminders.forEach { reminder ->
            ReminderTimelineCard(
                reminder = reminder,
                onClick = { onOpenDetail(reminder.id) },
            )
        }
    }
}

@Composable
private fun ReminderTimelineCard(
    reminder: ReminderItem,
    onClick: () -> Unit,
) {
    val dotColor = when (reminder.status) {
        ReminderStatus.Completed -> Color(0xFF67A96B)
        ReminderStatus.Current -> MaterialTheme.colorScheme.primary
        ReminderStatus.Planned -> MaterialTheme.colorScheme.outline
    }
    val icon = when (reminder.status) {
        ReminderStatus.Completed -> Icons.Rounded.Check
        ReminderStatus.Current -> Icons.Rounded.Schedule
        ReminderStatus.Planned -> Icons.Rounded.ArrowForward
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(dotColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
            )
        }
        Card(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (reminder.status == ReminderStatus.Current) 4.dp else 1.dp,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = reminder.time,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (reminder.status == ReminderStatus.Completed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    ReminderStatusBadge(reminder.status)
                }
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = reminder.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("查看详情")
                }
            }
        }
    }
}

@Composable
private fun ReminderStatusBadge(status: ReminderStatus) {
    val (backgroundColor, textColor, label) = when (status) {
        ReminderStatus.Completed -> Triple(Color(0xFFF1F7F1), Color(0xFF4F8D55), "已完成")
        ReminderStatus.Current -> Triple(Color(0xFFFFEEE4), MaterialTheme.colorScheme.primary, "进行中")
        ReminderStatus.Planned -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "计划中",
        )
    }
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun CompanionHeader(
    modelConnected: Boolean,
    autoSpeak: Boolean,
    onBack: () -> Unit,
    onToggleAutoSpeak: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "芳",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "小芳聊天",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (modelConnected) Color(0xFF67A96B) else MaterialTheme.colorScheme.outline,
                                CircleShape,
                            ),
                    )
                    Text(
                        text = if (modelConnected) "已连接陪伴服务" else "当前使用本地回复",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        IconButton(onClick = onToggleAutoSpeak) {
            Icon(
                imageVector = if (autoSpeak) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                contentDescription = if (autoSpeak) "关闭自动播报" else "打开自动播报",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CompanionMessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromAi) Arrangement.Start else Arrangement.End,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.fromAi) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                },
            ),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.fromAi) 6.dp else 18.dp,
                bottomEnd = if (message.fromAi) 18.dp else 6.dp,
            ),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = message.content,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                message.timeLabel?.takeIf { it.isNotBlank() }?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunicationStyleCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 156.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MemoryGroup(
    title: String,
    memories: List<SemanticMemoryItem>,
    onRemoveSemanticMemory: (String) -> Unit,
) {
    SectionCard {
        Text(title, fontWeight = FontWeight.SemiBold)
        if (memories.isEmpty()) {
            Text("暂时没有内容。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                memories.forEach { memory ->
                    SemanticMemoryCard(memory = memory, onRemove = { onRemoveSemanticMemory(memory.id) })
                }
            }
        }
    }
}

@Composable
private fun SemanticMemoryCard(
    memory: SemanticMemoryItem,
    onRemove: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(semanticMemoryTypeLabel(memory.memoryType), fontWeight = FontWeight.SemiBold)
                Text(
                    text = memory.compressedSummary.ifBlank { memory.summary },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "证据 ${memory.evidenceCount} 次 · 最近确认 ${formatMemoryTime(memory.lastConfirmedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除记忆", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TimeField(
    time: String,
    onTimeChanged: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("时间", fontWeight = FontWeight.SemiBold)
        AndroidView(
            factory = { context ->
                TimePicker(context).apply {
                    setIs24HourView(true)
                    val hour = time.substringBefore(":").toIntOrNull() ?: 8
                    val minute = time.substringAfter(":", "0").toIntOrNull() ?: 0
                    this.hour = hour
                    this.minute = minute
                    setOnTimeChangedListener { _, selectedHour, selectedMinute ->
                        onTimeChanged("%02d:%02d".format(selectedHour, selectedMinute))
                    }
                }
            },
            update = { picker ->
                val hour = time.substringBefore(":").toIntOrNull() ?: 8
                val minute = time.substringAfter(":", "0").toIntOrNull() ?: 0
                if (picker.hour != hour) picker.hour = hour
                if (picker.minute != minute) picker.minute = minute
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun buildReminderEncouragement(reminders: List<ReminderItem>): String {
    val current = reminders.firstOrNull { it.status == ReminderStatus.Current }
    if (current != null) {
        return "当前最重要的是“${current.title}”，到点后首页和聊天页都会继续提醒您。"
    }
    val pendingCount = reminders.count { it.status != ReminderStatus.Completed }
    return if (pendingCount == 0) {
        "今天的提醒都处理完了，先安心休息一下。"
    } else {
        "今天还有 $pendingCount 条提醒，按顺序完成就好。"
    }
}

private fun buildCompanionQuickReplies(state: SeniorUiState): List<String> {
    val suggestions = mutableListOf<String>()
    state.homeTasks.firstOrNull()?.let { reminder ->
        suggestions += "帮我记一下${reminder.title}"
    }
    if (state.currentHelpRequest != null) {
        suggestions += "帮我联系家人"
    }
    suggestions += listOf(
        "今天感觉还不错",
        "陪我聊两句",
        "提醒我按时休息",
    )
    return suggestions.distinct().take(3)
}

private fun buildGreetingMessage(preferredName: String): String {
    val name = preferredName.trim().ifBlank { "你" }
    return "$name，今天想先从哪件事开始？"
}

private fun buildTodayLabel(): String {
    return SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA).format(Date())
}

private fun reminderStatusLabel(status: ReminderStatus): String {
    return when (status) {
        ReminderStatus.Completed -> "已完成"
        ReminderStatus.Current -> "进行中"
        ReminderStatus.Planned -> "待处理"
    }
}

private fun semanticMemoryTypeLabel(type: SemanticMemoryType): String {
    return when (type) {
        SemanticMemoryType.Preference -> "偏好"
        SemanticMemoryType.Routine -> "作息"
        SemanticMemoryType.Health -> "身体"
        SemanticMemoryType.Family -> "家人"
        SemanticMemoryType.Profile -> "画像"
        SemanticMemoryType.Experience -> "经历"
        SemanticMemoryType.Event -> "近况"
        SemanticMemoryType.Emotion -> "情绪"
    }
}

private fun buildLocationTrackingSummary(samples: List<LocationSample>): String {
    if (samples.isEmpty()) {
        return "开关打开后会每小时采一次当前位置，当前还没有采样记录。"
    }
    val latest = samples.maxByOrNull { it.sampledAt } ?: return "当前还没有采样记录。"
    return "最近一次采集：${formatMemoryTime(latest.sampledAt)}，累计 ${samples.size} 条样本。"
}

private fun formatMemoryTime(timestamp: Long): String {
    return SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(timestamp))
}
