package com.localtaskmanager.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localtaskmanager.app.data.export.DataBackupManager
import com.localtaskmanager.app.data.export.ImportMode
import com.localtaskmanager.app.domain.model.AdvanceReminder
import com.localtaskmanager.app.domain.model.Category
import com.localtaskmanager.app.domain.repository.AppTheme
import com.localtaskmanager.app.domain.repository.CategoryRepository
import com.localtaskmanager.app.domain.repository.PreferencesRepository
import com.localtaskmanager.app.domain.repository.TaskRepository
import com.localtaskmanager.app.platform.reminders.ExactAlarmPermissionHelper
import com.localtaskmanager.app.platform.reminders.ReminderScheduler
import com.localtaskmanager.app.platform.widgets.WidgetRefreshCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val is24HourFormat: Boolean = false,
    val dailyReviewEnabled: Boolean = true,
    val dailyReviewHour: Int = 20,
    val dailyReviewMinute: Int = 0,
    val defaultCategoryId: String? = null,
    val defaultAdvanceReminderMinutes: Int = 0,
    val allCategories: List<Category> = emptyList(),
    val canScheduleExactAlarms: Boolean = true,
    val exportStatusMessage: String? = null,
    val importStatusMessage: String? = null,
    val isLoading: Boolean = true
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val categoryRepository: CategoryRepository,
    private val taskRepository: TaskRepository,
    private val dataBackupManager: DataBackupManager
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)

    private data class ReviewPrefs(
        val enabled: Boolean,
        val hour: Int,
        val minute: Int
    )

    private val reviewPrefsFlow = combine(
        preferencesRepository.dailyReviewEnabledFlow,
        preferencesRepository.dailyReviewHourFlow,
        preferencesRepository.dailyReviewMinuteFlow
    ) { enabled, hour, minute ->
        ReviewPrefs(enabled, hour, minute)
    }

    private data class MiscPrefs(
        val theme: AppTheme,
        val is24Hour: Boolean,
        val defaultCategoryId: String?,
        val defaultAdvanceMin: Int
    )

    private val miscPrefsFlow = combine(
        preferencesRepository.themeFlow,
        preferencesRepository.is24HourFormatFlow,
        preferencesRepository.defaultCategoryIdFlow,
        preferencesRepository.defaultAdvanceReminderMinutesFlow
    ) { theme, is24Hour, catId, advanceMin ->
        MiscPrefs(theme, is24Hour, catId, advanceMin)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        miscPrefsFlow,
        reviewPrefsFlow,
        categoryRepository.getAllCategoriesFlow(),
        _statusMessage
    ) { misc, review, categories, statusMsg ->
        SettingsUiState(
            theme = misc.theme,
            is24HourFormat = misc.is24Hour,
            dailyReviewEnabled = review.enabled,
            dailyReviewHour = review.hour,
            dailyReviewMinute = review.minute,
            defaultCategoryId = misc.defaultCategoryId,
            defaultAdvanceReminderMinutes = misc.defaultAdvanceMin,
            allCategories = categories,
            exportStatusMessage = statusMsg,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesRepository.setTheme(theme)
        }
    }

    fun setIs24HourFormat(is24Hour: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setIs24HourFormat(is24Hour)
        }
    }

    fun setDailyReviewEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDailyReviewEnabled(enabled)
            if (enabled) {
                val hour = uiState.value.dailyReviewHour
                val minute = uiState.value.dailyReviewMinute
                ReminderScheduler.scheduleDailyReviewAlarm(context, hour, minute)
            }
        }
    }

    fun setDailyReviewTime(context: Context, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setDailyReviewHour(hour)
            preferencesRepository.setDailyReviewMinute(minute)
            if (uiState.value.dailyReviewEnabled) {
                ReminderScheduler.scheduleDailyReviewAlarm(context, hour, minute)
            }
        }
    }

    fun setDefaultCategoryId(categoryId: String?) {
        viewModelScope.launch {
            preferencesRepository.setDefaultCategoryId(categoryId)
        }
    }

    fun setDefaultAdvanceReminderMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setDefaultAdvanceReminderMinutes(minutes)
        }
    }

    fun exportData(context: Context, targetUri: Uri) {
        viewModelScope.launch {
            try {
                val outputStream = context.contentResolver.openOutputStream(targetUri)
                if (outputStream == null) {
                    _statusMessage.value = "Failed to open export file destination"
                    return@launch
                }
                val count = dataBackupManager.exportData(outputStream).getOrThrow()
                _statusMessage.value = "Successfully exported $count tasks to JSON"
            } catch (e: Exception) {
                _statusMessage.value = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun importData(context: Context, sourceUri: Uri, mode: ImportMode) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                if (inputStream == null) {
                    _statusMessage.value = "Failed to open backup file"
                    return@launch
                }
                val count = dataBackupManager.importData(inputStream, mode).getOrThrow()

                // Reconcile alarms and refresh widgets after import!
                ReminderScheduler.reconcileAllReminders(context, taskRepository, preferencesRepository)
                WidgetRefreshCoordinator.refreshAllWidgets(context)

                _statusMessage.value = "Successfully imported $count tasks (${mode.name})"
            } catch (e: Exception) {
                _statusMessage.value = "Import failed: ${e.localizedMessage ?: "Invalid backup file"}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
