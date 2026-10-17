package com.localtaskmanager.app.ui.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localtaskmanager.app.domain.model.Category
import com.localtaskmanager.app.domain.model.Priority
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.model.TaskStatus
import com.localtaskmanager.app.domain.repository.CategoryRepository
import com.localtaskmanager.app.domain.repository.PreferencesRepository
import com.localtaskmanager.app.domain.repository.TaskRepository
import com.localtaskmanager.app.domain.usecase.CompleteTaskUseCase
import com.localtaskmanager.app.domain.usecase.DeleteTaskUseCase
import com.localtaskmanager.app.domain.usecase.ReopenTaskUseCase
import com.localtaskmanager.app.platform.reminders.ReminderScheduler
import com.localtaskmanager.app.platform.widgets.WidgetRefreshCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

enum class TaskSortOrder(val displayName: String) {
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    TITLE("Title"),
    CREATED("Creation Date")
}

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: TaskStatus? = null, // null = all
    val selectedPriorityFilter: Priority? = null, // null = all
    val selectedCategoryId: String? = null, // null = all
    val sortOrder: TaskSortOrder = TaskSortOrder.DUE_DATE,
    val is24HourFormat: Boolean = false,
    val isLoading: Boolean = true
)

class TasksViewModel(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val reopenTaskUseCase: ReopenTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow<TaskStatus?>(null)
    private val _priorityFilter = MutableStateFlow<Priority?>(null)
    private val _categoryFilter = MutableStateFlow<String?>(null)
    private val _sortOrder = MutableStateFlow(TaskSortOrder.DUE_DATE)

    private data class FilterState(
        val query: String,
        val status: TaskStatus?,
        val priority: Priority?,
        val categoryId: String?,
        val sortOrder: TaskSortOrder
    )

    private val filterStateFlow = combine(
        _searchQuery,
        _statusFilter,
        _priorityFilter,
        combine(_categoryFilter, _sortOrder) { cat, sort -> cat to sort }
    ) { query, status, priority, (category, sort) ->
        FilterState(query, status, priority, category, sort)
    }

    val uiState: StateFlow<TasksUiState> = combine(
        taskRepository.getAllTasksFlow(),
        categoryRepository.getAllCategoriesFlow(),
        preferencesRepository.is24HourFormatFlow,
        filterStateFlow
    ) { allTasks, categories, is24Hour, filter ->
        val today = LocalDate.now()
        val nowTime = LocalTime.now()

        val filtered = allTasks.filter { task ->
            // Search query filter
            val matchesQuery = filter.query.isBlank() ||
                task.title.contains(filter.query, ignoreCase = true) ||
                (task.notes != null && task.notes.contains(filter.query, ignoreCase = true))

            // Status filter
            val matchesStatus = filter.status == null || task.status == filter.status

            // Priority filter
            val matchesPriority = filter.priority == null || task.priority == filter.priority

            // Category filter
            val matchesCategory = filter.categoryId == null || task.categoryId == filter.categoryId

            matchesQuery && matchesStatus && matchesPriority && matchesCategory
        }.let { list ->
            when (filter.sortOrder) {
                TaskSortOrder.DUE_DATE -> list.sortedWith(
                    compareBy<Task> { !it.isOverdue(today, nowTime) }
                        .thenBy { it.dueDate ?: LocalDate.MAX }
                        .thenBy { it.scheduledTime ?: LocalTime.MAX }
                )
                TaskSortOrder.PRIORITY -> list.sortedByDescending { it.priority.value }
                TaskSortOrder.TITLE -> list.sortedBy { it.title.lowercase() }
                TaskSortOrder.CREATED -> list.sortedByDescending { it.createdAtEpochMs }
            }
        }

        TasksUiState(
            tasks = filtered,
            allCategories = categories,
            searchQuery = filter.query,
            selectedStatusFilter = filter.status,
            selectedPriorityFilter = filter.priority,
            selectedCategoryId = filter.categoryId,
            sortOrder = filter.sortOrder,
            is24HourFormat = is24Hour,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterChanged(status: TaskStatus?) {
        _statusFilter.value = status
    }

    fun onPriorityFilterChanged(priority: Priority?) {
        _priorityFilter.value = priority
    }

    fun onCategoryFilterChanged(categoryId: String?) {
        _categoryFilter.value = categoryId
    }

    fun onSortOrderChanged(sortOrder: TaskSortOrder) {
        _sortOrder.value = sortOrder
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
