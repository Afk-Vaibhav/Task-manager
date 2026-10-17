package com.localtaskmanager.app.ui.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.model.TaskStatus
import com.localtaskmanager.app.domain.repository.PreferencesRepository
import com.localtaskmanager.app.domain.repository.TaskRepository
import com.localtaskmanager.app.domain.usecase.CompleteTaskUseCase
import com.localtaskmanager.app.domain.usecase.DeleteTaskUseCase
import com.localtaskmanager.app.domain.usecase.ReopenTaskUseCase
import com.localtaskmanager.app.platform.reminders.ReminderScheduler
import com.localtaskmanager.app.platform.widgets.WidgetRefreshCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasksForDate: List<Task> = emptyList(),
    val is24HourFormat: Boolean = false,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val taskRepository: TaskRepository,
    private val preferencesRepository: PreferencesRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val reopenTaskUseCase: ReopenTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedDate.flatMapLatest { date -> taskRepository.getTasksForDateFlow(date) },
        _selectedDate,
        preferencesRepository.is24HourFormatFlow
    ) { tasks, date, is24Hour ->
        val sorted = tasks.sortedWith(
            compareBy<Task> { it.isCompleted }
                .thenBy { it.scheduledTime ?: java.time.LocalTime.MAX }
        )
        CalendarUiState(
            selectedDate = date,
            tasksForDate = sorted,
            is24HourFormat = is24Hour,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleTaskCompletion(context: Context, task: Task) {
        viewModelScope.launch {
            if (task.isCompleted) {
                reopenTaskUseCase(task.id)
                ReminderScheduler.scheduleRemindersForTask(context, task.copy(status = TaskStatus.OPEN))
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
