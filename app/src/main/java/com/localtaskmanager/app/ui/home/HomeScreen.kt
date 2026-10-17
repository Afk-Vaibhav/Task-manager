package com.localtaskmanager.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localtaskmanager.app.platform.notifications.NotificationPermissionHelper
import com.localtaskmanager.app.ui.components.EmptyStateView
import com.localtaskmanager.app.ui.components.TaskItemCard

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddTask: () -> Unit,
    onNavigateToEditTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Greeting and Date
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = uiState.currentDateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.greeting,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Notification Permission Banner (if permission not granted)
            if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notifications are turned off",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Turn on notifications to get reminded when tasks are due.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { NotificationPermissionHelper.openNotificationSettings(context) }
                            ) {
                                Text("Enable", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Daily Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${uiState.completedTodayTasksCount} of ${uiState.totalTodayTasksCount} completed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { uiState.todayProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            strokeCap = StrokeCap.Round,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }

            // Overdue Tasks Section
            if (uiState.overdueTasks.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Overdue (${uiState.overdueTasks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                items(uiState.overdueTasks, key = { "overdue_${it.id}" }) { task ->
                    TaskItemCard(
                        task = task,
                        is24Hour = uiState.is24HourFormat,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(context, it) },
                        onClick = { onNavigateToEditTask(it.id) },
                        onDelete = { viewModel.deleteTask(context, it) }
                    )
                }
            }

            // Today's Scheduled Tasks
            if (uiState.todayScheduledTasks.isNotEmpty() || uiState.todayAllDayTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(uiState.todayScheduledTasks, key = { "today_sched_${it.id}" }) { task ->
                    TaskItemCard(
                        task = task,
                        is24Hour = uiState.is24HourFormat,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(context, it) },
                        onClick = { onNavigateToEditTask(it.id) },
                        onDelete = { viewModel.deleteTask(context, it) }
                    )
                }

                items(uiState.todayAllDayTasks, key = { "today_allday_${it.id}" }) { task ->
                    TaskItemCard(
                        task = task,
                        is24Hour = uiState.is24HourFormat,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(context, it) },
                        onClick = { onNavigateToEditTask(it.id) },
                        onDelete = { viewModel.deleteTask(context, it) }
                    )
                }
            }

            // Upcoming Tasks Section
            if (uiState.upcomingTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "Upcoming",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(uiState.upcomingTasks, key = { "upcoming_${it.id}" }) { task ->
                    TaskItemCard(
                        task = task,
                        is24Hour = uiState.is24HourFormat,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(context, it) },
                        onClick = { onNavigateToEditTask(it.id) },
                        onDelete = { viewModel.deleteTask(context, it) }
                    )
                }
            }

            // Empty state if no tasks at all today or upcoming
            if (!uiState.isLoading &&
                uiState.overdueTasks.isEmpty() &&
                uiState.todayScheduledTasks.isEmpty() &&
                uiState.todayAllDayTasks.isEmpty() &&
                uiState.upcomingTasks.isEmpty()
            ) {
                item {
                    EmptyStateView(
                        title = "No tasks planned",
                        message = "Enjoy your free time or add a task to get started!",
                        actionButtonText = "+ Add First Task",
                        onActionClick = onNavigateToAddTask
                    )
                }
            }

            // Completed Recently Section
            if (uiState.completedRecentTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "Completed Recently",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(uiState.completedRecentTasks, key = { "completed_${it.id}" }) { task ->
                    TaskItemCard(
                        task = task,
                        is24Hour = uiState.is24HourFormat,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(context, it) },
                        onClick = { onNavigateToEditTask(it.id) },
                        onDelete = { viewModel.deleteTask(context, it) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}
