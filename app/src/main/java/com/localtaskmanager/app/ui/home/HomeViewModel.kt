package com.localtaskmanager.app.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.repository.PreferencesRepository
import com.localtaskmanager.app.domain.repository.TaskRepository
import com.localtaskmanager.app.domain.usecase.CompleteTaskUseCase
import com.localtaskmanager.app.domain.usecase.DeleteTaskUseCase
import com.localtaskmanager.app.domain.usecase.ReopenTaskUseCase
import com.localtaskmanager.app.platform.reminders.ReminderScheduler
import com.localtaskmanager.app.platform.widgets.WidgetRefreshCoordinator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val greeting: String = "",
    val currentDateText: String = "",
    val totalTodayTasksCount: Int = 0,
    val completedTodayTasksCount: Int = 0,
    val todayProgress: Float = 0f,
    val overdueTasks: List<Task> = emptyList(),
    val todayScheduledTasks: List<Task> = emptyList(),
    val todayAllDayTasks: List<Task> = emptyList(),
    val upcomingTasks: List<Task> = emptyList(),
    val completedRecentTasks: List<Task> = emptyList(),
    val is24HourFormat: Boolean = false,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val taskRepository: TaskRepository,
    private val preferencesRepository: PreferencesRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val reopenTaskUseCase: ReopenTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private fun getGreeting(hour: Int): String {
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        taskRepository.getAllTasksFlow(),
        preferencesRepository.is24HourFormatFlow
    ) { allTasks, is24Hour ->
        val today = LocalDate.now()
        val nowTime = LocalTime.now()
        val currentHour = nowTime.hour

        val greeting = getGreeting(currentHour)
        val dateText = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

        // Overdue open tasks
        val overdue = allTasks.filter { it.isOpen && it.isOverdue(today, nowTime) }

        // Tasks due today
        val todayOpenTasks = allTasks.filter { it.isOpen && it.isDueToday(today) && !it.isOverdue(today, nowTime) }
        val todayScheduled = todayOpenTasks.filter { it.scheduledTime != null }
            .sortedBy { it.scheduledTime }
        val todayAllDay = todayOpenTasks.filter { it.scheduledTime == null }

        // Upcoming tasks (due after today)
        val upcoming = allTasks.filter { it.isOpen && it.dueDate != null && it.dueDate.isAfter(today) }
            .sortedBy { it.dueDate }
            .take(5)

        // Today's total count & progress
        val tasksForTodayTotal = allTasks.filter {
            it.dueDate == today || (it.completedAtEpochMs != null && it.isCompleted && it.dueDate == today)
        }
        val completedToday = tasksForTodayTotal.count { it.isCompleted }
        val totalToday = tasksForTodayTotal.size
        val progress = if (totalToday > 0) completedToday.toFloat() / totalToday.toFloat() else 0f

        val completedRecent = allTasks.filter { it.isCompleted }.take(5)

        HomeUiState(
            greeting = greeting,
            currentDateText = dateText,
            totalTodayTasksCount = totalToday,
            completedTodayTasksCount = completedToday,
            todayProgress = progress,
            overdueTasks = overdue,
            todayScheduledTasks = todayScheduled,
            todayAllDayTasks = todayAllDay,
            upcomingTasks = upcoming,
            completedRecentTasks = completedRecent,
            is24HourFormat = is24Hour,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun toggleTaskCompletion(context: Context, task: Task) {
        viewModelScope.launch {
            if (task.isCompleted) {
                reopenTaskUseCase(task.id)
                ReminderScheduler.scheduleRemindersForTask(context, task.copy(status = com.localtaskmanager.app.domain.model.TaskStatus.OPEN))
            } else {
                completeTaskUseCase(task.id)
                ReminderScheduler.cancelRemindersForTask(context, task.id)
            }
            WidgetRefreshCoordinator.refreshAllWidgets(context)
        }
    }

    fun deleteTask(context: Context, task: Task) {
        viewModelScope.launch {
            deleteTaskUseCase(task.id)
            ReminderScheduler.cancelRemindersForTask(context, task.id)
            WidgetRefreshCoordinator.refreshAllWidgets(context)
        }
    }
}
