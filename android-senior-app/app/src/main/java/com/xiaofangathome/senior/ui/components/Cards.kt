package com.xiaofangathome.senior.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xiaofangathome.senior.data.ContactItem
import com.xiaofangathome.senior.data.ReminderItem
import com.xiaofangathome.senior.data.ReminderStatus
import com.xiaofangathome.senior.data.buildSingleReminderStateNote
import com.xiaofangathome.senior.ui.contactBadgeLabel
import com.xiaofangathome.senior.ui.theme.CurrentSoft
import com.xiaofangathome.senior.ui.theme.CurrentText
import com.xiaofangathome.senior.ui.theme.PlannedSoft
import com.xiaofangathome.senior.ui.theme.SuccessSoft
import com.xiaofangathome.senior.ui.theme.SuccessText

@Composable
fun HighlightReminderCard(
    time: String,
    title: String,
    onComplete: () -> Unit,
    onLater: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "今日重点",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                modifier = Modifier.size(60.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = time,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("我已完成")
                }
                OutlinedButton(
                    onClick = onLater,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("稍后提醒")
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    iconVoice: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                color = if (iconVoice) Color(0xFFEAF4FF) else Color(0xFFF6E9DE),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (iconVoice) Icons.Rounded.KeyboardVoice else Icons.Rounded.Call,
                        contentDescription = title,
                        tint = if (iconVoice) Color(0xFF3F7CD9) else MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun TodoListItem(item: ReminderItem) {
    TodoListItem(item = item, onClick = null)
}

@Composable
fun TodoListItem(item: ReminderItem, onClick: (() -> Unit)?) {
    val chip = when (item.status) {
        ReminderStatus.Completed -> Triple(SuccessSoft, SuccessText, "已完成")
        ReminderStatus.Current -> Triple(CurrentSoft, CurrentText, "待处理")
        ReminderStatus.Planned -> Triple(PlannedSoft, MaterialTheme.colorScheme.onSurfaceVariant, "未开始")
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.status == ReminderStatus.Completed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(width = 1.dp, height = 40.dp),
                ) {}
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (item.status == ReminderStatus.Completed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    val description = buildSingleReminderStateNote(
                        time = item.time,
                        status = item.status,
                        frequencyLabel = item.frequencyLabel,
                    )?.let { "$it | ${item.description}" } ?: item.description
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = chip.first) {
                Text(
                    text = chip.third,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = chip.second,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
fun EmergencyContactCard(item: ContactItem) {
    EmergencyContactCard(item = item, onCall = null, onDelete = null)
}

@Composable
fun EmergencyContactCard(
    item: ContactItem,
    onCall: (() -> Unit)?,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = contactBadgeLabel(item.relation, item.name),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = item.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (onDelete != null) {
                    Surface(
                        modifier = Modifier
                            .size(30.dp)
                            .clickable(onClick = onDelete),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "删除联系人",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { onCall?.invoke() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Rounded.Call, contentDescription = "拨打电话")
                Spacer(Modifier.width(8.dp))
                Text("拨打")
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NoticeCard(title: String, message: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun VoiceActionButton(
    isRecording: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(if (isRecording) 152.dp else 132.dp),
                shape = CircleShape,
                color = if (isRecording) Color(0x22E15B5B) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            ) {}
            Surface(
                modifier = Modifier.size(if (isRecording) 128.dp else 112.dp),
                shape = CircleShape,
                color = if (isRecording) Color(0x33E15B5B) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {}
            Surface(
                onClick = onClick,
                modifier = Modifier.size(if (isRecording) 96.dp else 88.dp),
                shape = CircleShape,
                color = if (isRecording) Color(0xFFE15B5B) else MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.KeyboardVoice,
                        contentDescription = if (isRecording) "停止语音" else "开始语音",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (isRecording) "点击结束" else "点击开始说话",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
