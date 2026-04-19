package com.xiaofangathome.senior.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xiaofangathome.senior.ui.navigation.SeniorRoute

@Composable
fun SeniorTopBar(
    title: String = "小芳在家",
    subtitle: String? = null,
    showBack: Boolean = false,
    showSettings: Boolean = true,
    showLeadingAvatar: Boolean = true,
    leadingAvatarText: String = "芳",
    trailingAvatarText: String? = null,
    onBack: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showBack) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onBack?.invoke() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else if (showLeadingAvatar) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = leadingAvatarText.ifBlank { "芳" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (trailingAvatarText != null) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = trailingAvatarText,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        } else if (showSettings) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onSettingsClick?.invoke() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
fun SeniorBottomBar(
    activeRoute: String,
    onNavigate: (String) -> Unit,
) {
    val items = listOf(
        BottomNavItem(SeniorRoute.Home.route, "首页", Icons.Rounded.Home),
        BottomNavItem(SeniorRoute.Reminders.route, "提醒", Icons.Rounded.Notifications),
        BottomNavItem(SeniorRoute.Contacts.route, "家人", Icons.Rounded.People),
    )

    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            items.forEach { item ->
                val active = item.route == activeRoute
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        )
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (active) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(
                        text = item.label,
                        color = if (active) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)
