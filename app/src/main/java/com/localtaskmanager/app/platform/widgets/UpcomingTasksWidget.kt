package com.localtaskmanager.app.platform.widgets

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.localtaskmanager.app.MainActivity
import com.localtaskmanager.app.LocalTaskManagerApp
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.platform.reminders.ReminderScheduler
import com.localtaskmanager.app.ui.quickadd.QuickAddActivity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class UpcomingTasksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as LocalTaskManagerApp
        val allOpenTasks = app.appContainer.taskRepository.getOpenTasksSnapshot()

        val today = LocalDate.now()
        val nowTime = LocalTime.now()

        // Sort: Overdue first, then today & upcoming chronologically, then undated
        val sortedTasks = allOpenTasks.sortedWith(
            compareBy<Task> { !it.isOverdue(today, nowTime) }
                .thenBy { it.dueDate ?: LocalDate.MAX }
                .thenBy { it.scheduledTime ?: LocalTime.MAX }
        ).take(5)

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF181B22))
                        .cornerRadius(16.dp)
                        .padding(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStartActivity<MainActivity>()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upcoming Tasks",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color.White),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "${allOpenTasks.size} pending",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF9AA0A6)),
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    if (sortedTasks.isEmpty()) {
                        Column(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "All caught up! 🎉",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color(0xFFB0B7C3)),
                                    fontSize = 13.sp
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(8.dp))
                            Button(
                                text = "+ Add Task",
                                onClick = actionStartActivity<QuickAddActivity>(),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = androidx.glance.unit.ColorProvider(Color(0xFF2C3240)),
                                    contentColor = androidx.glance.unit.ColorProvider(Color.White)
                                )
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = GlanceModifier.fillMaxSize()
                        ) {
                            items(sortedTasks) { task ->
                                val isOverdue = task.isOverdue(today, nowTime)
                                val timeText = when {
                                    isOverdue -> "Overdue"
                                    task.dueDate != null && task.scheduledTime != null -> {
                                        task.scheduledTime.format(DateTimeFormatter.ofPattern("h:mm a"))
                                    }
                                    task.dueDate != null -> "Due today"
                                    else -> "No due date"
                                }

                                Row(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(Color(0xFF242933))
                                        .cornerRadius(8.dp)
                                        .padding(8.dp)
                                        .clickable(actionStartActivity<MainActivity>()),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Interactive Complete Action Button
                                    Button(
                                        text = "✓",
                                        onClick = actionRunCallback<CompleteTaskActionCallback>(
                                            actionParametersOf(CompleteTaskActionCallback.TASK_ID_KEY to task.id)
                                        ),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = androidx.glance.unit.ColorProvider(Color(0xFF333B4D)),
                                            contentColor = androidx.glance.unit.ColorProvider(Color(0xFF4CAF50))
                                        ),
                                        modifier = GlanceModifier.width(36.dp).height(36.dp)
                                    )

                                    Spacer(modifier = GlanceModifier.width(8.dp))

                                    Column(modifier = GlanceModifier.defaultWeight()) {
                                        Text(
                                            text = task.title,
                                            style = TextStyle(
                                                color = androidx.glance.unit.ColorProvider(Color.White),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = timeText,
                                            style = TextStyle(
                                                color = androidx.glance.unit.ColorProvider(
                                                    if (isOverdue) Color(0xFFFF5252) else Color(0xFF9AA0A6)
                                                ),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }

                                    task.category?.let { cat ->
                                        Spacer(modifier = GlanceModifier.width(4.dp))
                                        Text(
                                            text = cat.name,
                                            style = TextStyle(
                                                color = androidx.glance.unit.ColorProvider(Color(cat.colorArgb)),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class CompleteTaskActionCallback : ActionCallback {
    companion object {
        val TASK_ID_KEY = ActionParameters.Key<String>("task_id")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TASK_ID_KEY] ?: return
        val app = context.applicationContext as LocalTaskManagerApp

        // Complete in Room (Room is single source of truth!)
        app.appContainer.completeTaskUseCase(taskId)

        // Cancel alarms for completed task
        ReminderScheduler.cancelRemindersForTask(context, taskId)

        // Refresh all widget instances
        WidgetRefreshCoordinator.refreshAllWidgets(context)
    }
}

class UpcomingTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingTasksWidget()
}
