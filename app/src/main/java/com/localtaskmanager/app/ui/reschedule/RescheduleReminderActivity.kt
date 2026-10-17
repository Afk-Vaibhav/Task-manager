package com.localtaskmanager.app.ui.reschedule

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Schedule
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localtaskmanager.app.LocalTaskManagerApp
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.platform.notifications.NotificationHelper
import com.localtaskmanager.app.platform.reminders.ReminderScheduler
import com.localtaskmanager.app.platform.widgets.WidgetRefreshCoordinator
import com.localtaskmanager.app.ui.theme.LocalTaskManagerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RescheduleReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID)
        val notificationId = intent.getIntExtra(
            NotificationHelper.EXTRA_NOTIFICATION_ID,
            if (taskId != null) NotificationHelper.getNotificationIdForTask(taskId) else -1
        )

        if (taskId == null) {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val app = application as LocalTaskManagerApp

        setContent {
            LocalTaskManagerTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RescheduleContent(
                            taskId = taskId,
                            onSetReminder = { hours, minutes ->
                                val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                                scope.launch {
                                    val result = app.appContainer.rescheduleReminderUseCase(
                                        taskId = taskId,
                                        hours = hours,
                                        minutes = minutes
                                    )

                                    if (result.isSuccess) {
                                        val updatedTask = result.getOrThrow()
                                        // Schedule the one-time snooze alarm
                                        ReminderScheduler.scheduleRemindersForTask(this@RescheduleReminderActivity, updatedTask)
                                        // Dismiss old notification
                                        NotificationHelper.dismissNotification(this@RescheduleReminderActivity, notificationId)
                                        // Refresh widgets
                                        WidgetRefreshCoordinator.refreshAllWidgets(this@RescheduleReminderActivity)

                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            Toast.makeText(
                                                this@RescheduleReminderActivity,
                                                "Reminder rescheduled for $hours hr $minutes min from now",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            finish()
                                        }
                                    } else {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            Toast.makeText(
                                                this@RescheduleReminderActivity,
                                                result.exceptionOrNull()?.message ?: "Failed to reschedule",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            onCancel = { finish() },
                            taskProvider = { app.appContainer.taskRepository.getTaskById(taskId) },
                            is24HourProvider = { app.appContainer.preferencesRepository.is24HourFormatFlow.first() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RescheduleContent(
    taskId: String,
    onSetReminder: (Int, Int) -> Unit,
    onCancel: () -> Unit,
    taskProvider: suspend () -> Task?,
    is24HourProvider: suspend () -> Boolean
) {
    var task by remember { mutableStateOf<Task?>(null) }
    var is24Hour by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    var showHoursMenu by remember { mutableStateOf(false) }
    var showMinutesMenu by remember { mutableStateOf(false) }
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(0) }

    LaunchedEffect(taskId) {
        task = taskProvider()
        is24Hour = is24HourProvider()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Reschedule Reminder",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Task title confirmation
            Column {
                Text(
                    text = "Task",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = task?.title ?: "Loading...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "Remind me in",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            // Hour and Minute pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hours Dropdown
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hours",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHoursMenu = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = String.format("%02d", hours), fontWeight = FontWeight.Bold)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showHoursMenu,
                        onDismissRequest = { showHoursMenu = false }
                    ) {
                        (0..23).forEach { h ->
                            DropdownMenuItem(
                                text = { Text("$h hours") },
                                onClick = {
                                    hours = h
                                    errorMessage = null
                                    showHoursMenu = false
                                }
                            )
                        }
                    }
                }

                // Minutes Dropdown
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Minutes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMinutesMenu = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = String.format("%02d", minutes), fontWeight = FontWeight.Bold)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showMinutesMenu,
                        onDismissRequest = { showMinutesMenu = false }
                    ) {
                        listOf(5, 10, 15, 20, 30, 45, 50, 55).forEach { m ->
                            DropdownMenuItem(
                                text = { Text("$m minutes") },
                                onClick = {
                                    minutes = m
                                    errorMessage = null
                                    showMinutesMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Calculated target reminder time preview
            val totalMinutes = (hours * 60) + minutes
            val targetInstant = Instant.now().plusSeconds(totalMinutes * 60L)
            val targetDateTime = LocalDateTime.ofInstant(targetInstant, ZoneId.systemDefault())
            val pattern = if (is24Hour) "HH:mm" else "h:mm a"
            val targetTimeFormatted = targetDateTime.format(DateTimeFormatter.ofPattern(pattern))

            val durationDesc = when {
                hours > 0 && minutes > 0 -> "$hours hr $minutes min from now"
                hours > 0 -> "$hours hr from now"
                else -> "$minutes min from now"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (totalMinutes > 0) "$durationDesc (at $targetTimeFormatted)" else "Select a valid duration",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Action Buttons: Cancel and Set Reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (hours == 0 && minutes == 0) {
                            errorMessage = "Please choose a delay greater than 0"
                        } else {
                            onSetReminder(hours, minutes)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Set Reminder")
                }
            }
        }
    }
}
