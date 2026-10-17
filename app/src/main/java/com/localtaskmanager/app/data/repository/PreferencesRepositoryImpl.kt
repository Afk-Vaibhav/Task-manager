package com.localtaskmanager.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.localtaskmanager.app.domain.repository.AppTheme
import com.localtaskmanager.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class PreferencesRepositoryImpl(private val context: Context) : PreferencesRepository {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("app_theme")
        val IS_24_HOUR = booleanPreferencesKey("is_24_hour_format")
        val DAILY_REVIEW_ENABLED = booleanPreferencesKey("daily_review_enabled")
        val DAILY_REVIEW_HOUR = intPreferencesKey("daily_review_hour")
        val DAILY_REVIEW_MINUTE = intPreferencesKey("daily_review_minute")
        val DEFAULT_CATEGORY_ID = stringPreferencesKey("default_category_id")
        val DEFAULT_ADVANCE_REMINDER_MINUTES = intPreferencesKey("default_advance_reminder_minutes")
    }

    override val themeFlow: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        val raw = prefs[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.value
        AppTheme.entries.firstOrNull { it.value == raw } ?: AppTheme.SYSTEM
    }

    override suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME] = theme.value
        }
    }

    override val is24HourFormatFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.IS_24_HOUR] ?: false
    }

    override suspend fun setIs24HourFormat(is24Hour: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_24_HOUR] = is24Hour
        }
    }

    override val dailyReviewEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DAILY_REVIEW_ENABLED] ?: true
    }

    override suspend fun setDailyReviewEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DAILY_REVIEW_ENABLED] = enabled
        }
    }

    override val dailyReviewHourFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DAILY_REVIEW_HOUR] ?: 20 // 8:00 PM default
    }

    override suspend fun setDailyReviewHour(hour: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DAILY_REVIEW_HOUR] = hour
        }
    }

    override val dailyReviewMinuteFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DAILY_REVIEW_MINUTE] ?: 0
    }

    override suspend fun setDailyReviewMinute(minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DAILY_REVIEW_MINUTE] = minute
        }
    }

    override val defaultCategoryIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DEFAULT_CATEGORY_ID] ?: "cat_personal"
    }

    override suspend fun setDefaultCategoryId(categoryId: String?) {
        context.dataStore.edit { prefs ->
            if (categoryId != null) {
                prefs[PreferencesKeys.DEFAULT_CATEGORY_ID] = categoryId
            } else {
                prefs.remove(PreferencesKeys.DEFAULT_CATEGORY_ID)
            }
        }
    }

    override val defaultAdvanceReminderMinutesFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DEFAULT_ADVANCE_REMINDER_MINUTES] ?: 0
    }

    override suspend fun setDefaultAdvanceReminderMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEFAULT_ADVANCE_REMINDER_MINUTES] = minutes
        }
    }
}
