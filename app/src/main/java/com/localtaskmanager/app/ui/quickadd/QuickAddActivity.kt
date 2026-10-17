package com.localtaskmanager.app.ui.quickadd

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localtaskmanager.app.LocalTaskManagerApp
import com.localtaskmanager.app.domain.model.Category
import com.localtaskmanager.app.domain.model.Priority
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.model.TaskStatus
import com.localtaskmanager.app.platform.reminders.ReminderScheduler
import com.localtaskmanager.app.platform.widgets.WidgetRefreshCoordinator
import com.localtaskmanager.app.ui.theme.LocalTaskManagerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class QuickAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as LocalTaskManagerApp

        setContent {
            LocalTaskManagerTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    QuickAddContent(
                        onSave = { title, category, date, time, priority, reminder ->
                            val task = Task(
                                id = UUID.randomUUID().toString(),
                                title = title.trim(),
                                categoryId = category?.id,
                                category = category,
                                dueDate = date,
                                scheduledTime = time,
                                priority = priority,
                                status = TaskStatus.OPEN,
                                reminderEnabled = reminder && date != null && time != null,
                                createdAtEpochMs = System.currentTimeMillis(),
                                updatedAtEpochMs = System.currentTimeMillis()
                            )

                            // Insert into Room
                            val scope = (application as LocalTaskManagerApp)
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                app.appContainer.taskRepository.insertTask(task)
                                ReminderScheduler.scheduleRemindersForTask(this@QuickAddActivity, task)
                                WidgetRefreshCoordinator.refreshAllWidgets(this@QuickAddActivity)
                            }
                            finish()
                        },
                        onCancel = { finish() },
                        categoriesProvider = { app.appContainer.categoryRepository.getAllCategories() },
                        is24HourProvider = { app.appContainer.preferencesRepository.is24HourFormatFlow.first() }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAddContent(
    onSave: (String, Category?, LocalDate?, LocalTime?, Priority, Boolean) -> Unit,
    onCancel: () -> Unit,
    categoriesProvider: suspend () -> List<Category>,
    is24HourProvider: suspend () -> Boolean
) {
    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var dueDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var scheduledTime by remember { mutableStateOf<LocalTime?>(null) }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    var reminderEnabled by remember { mutableStateOf(false) }
    var is24Hour by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        categories = categoriesProvider()
        selectedCategory = categories.firstOrNull { it.isDefault }
        is24Hour = is24HourProvider()
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Quick Add Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                placeholder = { Text("What needs to be done?") },
                isError = titleError != null,
                supportingText = {
                    titleError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            // Date Quick Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = dueDate == LocalDate.now(),
                    onClick = { dueDate = LocalDate.now() },
                    label = { Text("Today") }
                )
                FilterChip(
                    selected = dueDate == LocalDate.now().plusDays(1),
                    onClick = { dueDate = LocalDate.now().plusDays(1) },
                    label = { Text("Tomorrow") }
                )
                FilterChip(
                    selected = dueDate == null,
                    onClick = { dueDate = null },
                    label = { Text("No Date") }
                )
            }

            // Priority Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val trimmed = title.trim()
                        if (trimmed.isEmpty()) {
                            titleError = "Task title cannot be empty"
                        } else {
                            onSave(trimmed, selectedCategory, dueDate, scheduledTime, priority, reminderEnabled)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}
