package com.localtaskmanager.app.ui.addedit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localtaskmanager.app.data.local.dao.RecurrenceSeriesDao
import com.localtaskmanager.app.data.local.database.toEntity
import com.localtaskmanager.app.domain.model.AdvanceReminder
import com.localtaskmanager.app.domain.model.Category
import com.localtaskmanager.app.domain.model.Priority
import com.localtaskmanager.app.domain.model.RecurrenceFrequency
import com.localtaskmanager.app.domain.model.RecurrenceSeries
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.model.TaskStatus
import com.localtaskmanager.app.domain.repository.CategoryRepository
import com.localtaskmanager.app.domain.repository.PreferencesRepository
import com.localtaskmanager.app.domain.repository.TaskRepository
import com.localtaskmanager.app.domain.usecase.CreateTaskUseCase
import com.localtaskmanager.app.domain.usecase.UpdateTaskUseCase
import com.localtaskmanager.app.platform.reminders.ReminderScheduler
import com.localtaskmanager.app.platform.widgets.WidgetRefreshCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class AddEditTaskUiState(
    val taskId: String? = null,
    val title: String = "",
    val notes: String = "",
    val selectedCategory: Category? = null,
    val allCategories: List<Category> = emptyList(),
    val dueDate: LocalDate? = null,
    val scheduledTime: LocalTime? = null,
    val priority: Priority = Priority.NORMAL,
    val reminderEnabled: Boolean = false,
    val advanceReminder: AdvanceReminder = AdvanceReminder.AT_TIME,
    val recurrenceFrequency: RecurrenceFrequency? = null,
    val is24HourFormat: Boolean = false,
    val isEditing: Boolean = false,
    val titleError: String? = null,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true
)

class AddEditTaskViewModel(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val recurrenceSeriesDao: RecurrenceSeriesDao,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val initialTaskId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTaskUiState(taskId = initialTaskId))
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val categories = categoryRepository.getAllCategories()
            val is24Hour = preferencesRepository.is24HourFormatFlow.first()
            val defaultCatId = preferencesRepository.defaultCategoryIdFlow.first()
            val defaultAdvanceMin = preferencesRepository.defaultAdvanceReminderMinutesFlow.first()

            if (initialTaskId != null) {
                val existingTask = taskRepository.getTaskById(initialTaskId)
                if (existingTask != null) {
                    val matchingCategory = categories.firstOrNull { it.id == existingTask.categoryId }
                    val series = existingTask.recurrenceSeriesId?.let { recurrenceSeriesDao.getSeriesById(it) }

                    _uiState.update {
                        it.copy(
                            taskId = existingTask.id,
                            title = existingTask.title,
                            notes = existingTask.notes ?: "",
                            selectedCategory = matchingCategory,
                            allCategories = categories,
                            dueDate = existingTask.dueDate,
                            scheduledTime = existingTask.scheduledTime,
                            priority = existingTask.priority,
                            reminderEnabled = existingTask.reminderEnabled,
                            advanceReminder = existingTask.advanceReminder ?: AdvanceReminder.fromMinutes(defaultAdvanceMin),
                            recurrenceFrequency = series?.frequency?.let { freqStr -> RecurrenceFrequency.fromValue(freqStr) },
                            is24HourFormat = is24Hour,
                            isEditing = true,
                            isLoading = false
                        )
                    }
                    return@launch
                }
            }

            // New task defaults
            val defaultCat = categories.firstOrNull { it.id == defaultCatId } ?: categories.firstOrNull { it.isDefault }
            _uiState.update {
                it.copy(
                    selectedCategory = defaultCat,
                    allCategories = categories,
                    advanceReminder = AdvanceReminder.fromMinutes(defaultAdvanceMin),
                    is24HourFormat = is24Hour,
                    isLoading = false
                )
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, titleError = null) }
    }

    fun onNotesChanged(newNotes: String) {
        _uiState.update { it.copy(notes = newNotes) }
    }

    fun onCategorySelected(category: Category?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onDateSelected(date: LocalDate?) {
        _uiState.update {
            val reminderCanBeEnabled = date != null && it.scheduledTime != null
            it.copy(
                dueDate = date,
                reminderEnabled = if (reminderCanBeEnabled) true else if (date == null) false else it.reminderEnabled
            )
        }
    }

    fun onTimeSelected(time: LocalTime?) {
        _uiState.update {
            val effectiveDate = it.dueDate ?: if (time != null) LocalDate.now() else null
            it.copy(
                dueDate = effectiveDate,
                scheduledTime = time,
                reminderEnabled = time != null
            )
        }
    }

    fun onPrioritySelected(priority: Priority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun onReminderToggled(enabled: Boolean) {
        _uiState.update { it.copy(reminderEnabled = enabled) }
    }

    fun onAdvanceReminderSelected(advanceReminder: AdvanceReminder) {
        _uiState.update { it.copy(advanceReminder = advanceReminder) }
    }

    fun onRecurrenceSelected(frequency: RecurrenceFrequency?) {
        _uiState.update { it.copy(recurrenceFrequency = frequency) }
    }

    fun saveTask(context: Context) {
        val currentState = _uiState.value
        val trimmedTitle = currentState.title.trim()

        if (trimmedTitle.isEmpty()) {
            _uiState.update { it.copy(titleError = "Title cannot be empty") }
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()

            // If repeat rule specified, manage recurrence series
            var seriesId: String? = null
            if (currentState.recurrenceFrequency != null && currentState.dueDate != null) {
                val series = RecurrenceSeries(
                    id = UUID.randomUUID().toString(),
                    frequency = currentState.recurrenceFrequency,
                    interval = 1,
                    startDate = currentState.dueDate,
                    templateTitle = trimmedTitle,
                    templateNotes = currentState.notes.ifBlank { null },
                    templateCategoryId = currentState.selectedCategory?.id,
                    templateScheduledTime = currentState.scheduledTime,
                    templatePriority = currentState.priority,
                    templateReminderEnabled = currentState.reminderEnabled,
                    templateAdvanceReminder = currentState.advanceReminder,
                    isActive = true,
                    createdAtEpochMs = now
                )
                recurrenceSeriesDao.insertSeries(series.toEntity())
                seriesId = series.id
            }

            val task = Task(
                id = currentState.taskId ?: UUID.randomUUID().toString(),
                title = trimmedTitle,
                notes = currentState.notes.ifBlank { null },
                categoryId = currentState.selectedCategory?.id,
                category = currentState.selectedCategory,
                dueDate = currentState.dueDate,
                scheduledTime = currentState.scheduledTime,
                priority = currentState.priority,
                status = TaskStatus.OPEN,
                reminderEnabled = currentState.reminderEnabled,
                advanceReminder = currentState.advanceReminder,
                recurrenceSeriesId = seriesId,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )

            if (currentState.isEditing) {
                updateTaskUseCase(task)
            } else {
                createTaskUseCase(task)
            }

            // Reconcile alarm
            ReminderScheduler.scheduleRemindersForTask(context, task)

            // Refresh widgets
            WidgetRefreshCoordinator.refreshAllWidgets(context)

            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
