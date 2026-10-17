package com.localtaskmanager.app.platform.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.localtaskmanager.app.LocalTaskManagerApp
import com.localtaskmanager.app.platform.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class DailyReviewAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as LocalTaskManagerApp

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reviewTasks = app.appContainer.dailyReviewUseCase.getReviewTasks(LocalDate.now())
                if (reviewTasks.isNotEmpty()) {
                    NotificationHelper.showDailyReviewNotification(context, reviewTasks.size)
                }

                // Schedule next day's review alarm
                val enabled = app.appContainer.preferencesRepository.dailyReviewEnabledFlow.first()
                if (enabled) {
                    val hour = app.appContainer.preferencesRepository.dailyReviewHourFlow.first()
                    val minute = app.appContainer.preferencesRepository.dailyReviewMinuteFlow.first()
                    ReminderScheduler.scheduleDailyReviewAlarm(context, hour, minute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
