package com.localtaskmanager.app.platform.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.localtaskmanager.app.LocalTaskManagerApp
import com.localtaskmanager.app.platform.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra("is_test_alarm", false)) {
            android.util.Log.d("AlarmReceiver", "Test alarm triggered, posting notification")
            NotificationHelper.sendTestNotification(context)
            return
        }

        val taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID) ?: return
        val alarmType = intent.getStringExtra(ReminderScheduler.EXTRA_ALARM_TYPE) ?: ReminderScheduler.ALARM_TYPE_DUE
        val advanceMinutes = intent.getIntExtra(ReminderScheduler.EXTRA_ADVANCE_MINUTES, 0)

        val pendingResult = goAsync()
        val app = context.applicationContext as LocalTaskManagerApp

        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("AlarmReceiver", "Alarm triggered for taskId=$taskId, type=$alarmType")
                // Re-verify task state from Room to avoid stale alarms
                val task = app.appContainer.taskRepository.getTaskById(taskId)
                if (task == null || !task.isOpen) {
                    android.util.Log.d("AlarmReceiver", "Task not found or not open, skipping notification")
                    return@launch
                }

                // If this was a rescheduled reminder, clear the snooze timestamp so it doesn't loop
                if (alarmType == ReminderScheduler.ALARM_TYPE_RESCHEDULED) {
                    app.appContainer.taskRepository.clearSnooze(taskId)
                }

                val reminderLabel = when (alarmType) {
                    ReminderScheduler.ALARM_TYPE_ADVANCE -> "Due in $advanceMinutes minutes"
                    ReminderScheduler.ALARM_TYPE_RESCHEDULED -> "Rescheduled reminder"
                    else -> null
                }

                // Show notification with EXACTLY TWO ACTIONS: Done and Reschedule
                NotificationHelper.showTaskReminderNotification(context, task, reminderLabel)
            } catch (e: Throwable) {
                android.util.Log.e("AlarmReceiver", "Failed to process alarm receiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
