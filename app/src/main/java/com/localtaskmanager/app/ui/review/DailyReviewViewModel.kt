package com.localtaskmanager.app.ui.review

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.repository.PreferencesRepository
import com.localtaskmanager.app.domain.usecase.CompleteTaskUseCase
import com.localtaskmanager.app.domain.usecase.DailyReviewUseCase
import com.localtaskmanager.app.platform.reminders.ReminderScheduler
import com.localtaskmanager.app.platform.widgets.WidgetRefreshCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DailyReviewUiState(
    val reviewTasks: List<Task> = emptyList(),
    val is24HourFormat: Boolean = false,
    val isFinished: Boolean = false,
    val isLoading: Boolean = true
)

class DailyReviewViewModel(
    private val dailyReviewUseCase: DailyReviewUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyReviewUiState())
    val uiState: StateFlow<DailyReviewUiState> = _uiState.asStateFlow()

    init {
        loadReviewTasks()
    }

    fun loadReviewTasks() {
        viewModelScope.launch {
            val tasks = dailyReviewUseCase.getReviewTasks(LocalDate.now())
            val is24Hour = preferencesRepository.is24HourFormatFlow.first()
            _uiState.update {
                it.copy(
                    reviewTasks = tasks,
                    is24HourFormat = is24Hour,
                    isLoading = false,
                    isFinished = tasks.isEmpty()
                )
            }
        }
    }

    fun markComplete(context: Context, taskId: String) {
        viewModelScope.launch {
            completeTaskUseCase(taskId)
            ReminderScheduler.cancelRemindersForTask(context, taskId)
            WidgetRefreshCoordinator.refreshAllWidgets(context)
            removeTaskFromReview(taskId)
        }
    }

    fun moveToTomorrow(context: Context, taskId: String) {
        viewModelScope.launch {
            val updated = dailyReviewUseCase.moveTaskToTomorrow(taskId, LocalDate.now())
            if (updated != null) {
                ReminderScheduler.scheduleRemindersForTask(context, updated)
            }
            WidgetRefreshCoordinator.refreshAllWidgets(context)
            removeTaskFromReview(taskId)
        }
    }

    fun rescheduleToDate(context: Context, taskId: String, newDate: LocalDate) {
        viewModelScope.launch {
            val updated = dailyReviewUseCase.rescheduleTaskToDate(taskId, newDate)
            if (updated != null) {
                ReminderScheduler.scheduleRemindersForTask(context, updated)
            }
            WidgetRefreshCoordinator.refreshAllWidgets(context)
            removeTaskFromReview(taskId)
        }
    }

    fun keepPending(taskId: String) {
        removeTaskFromReview(taskId)
    }

    private fun removeTaskFromReview(taskId: String) {
        _uiState.update { state ->
            val updatedList = state.reviewTasks.filter { it.id != taskId }
            state.copy(
                reviewTasks = updatedList,
                isFinished = updatedList.isEmpty()
            )
        }
    }
}
