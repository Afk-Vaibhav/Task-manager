package com.localtaskmanager.app.domain.repository

import kotlinx.coroutines.flow.Flow

enum class AppTheme(val value: String) {
    SYSTEM("SYSTEM"),
    LIGHT("LIGHT"),
    DARK("DARK")
}

interface PreferencesRepository {
    val themeFlow: Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)

    val is24HourFormatFlow: Flow<Boolean>
    suspend fun setIs24HourFormat(is24Hour: Boolean)

    val dailyReviewEnabledFlow: Flow<Boolean>
    suspend fun setDailyReviewEnabled(enabled: Boolean)

    val dailyReviewHourFlow: Flow<Int>
    suspend fun setDailyReviewHour(hour: Int)

    val dailyReviewMinuteFlow: Flow<Int>
    suspend fun setDailyReviewMinute(minute: Int)

    val defaultCategoryIdFlow: Flow<String?>
    suspend fun setDefaultCategoryId(categoryId: String?)

    val defaultAdvanceReminderMinutesFlow: Flow<Int>
    suspend fun setDefaultAdvanceReminderMinutes(minutes: Int)
}
