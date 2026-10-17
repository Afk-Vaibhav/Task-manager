package com.localtaskmanager.app.platform.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.repository.PreferencesRepository
import com.localtaskmanager.app.domain.repository.TaskRepository
import com.localtaskmanager.app.platform.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

object ReminderScheduler {

    private const val TAG = "ReminderScheduler"

    const val ACTION_ALARM_TRIGGERED = "com.localtaskmanager.app.ACTION_ALARM_TRIGGERED"
    const val ACTION_DAILY_REVIEW_TRIGGERED = "com.localtaskmanager.app.ACTION_DAILY_REVIEW_TRIGGERED"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_ALARM_TYPE = "extra_alarm_type"
    const val EXTRA_ADVANCE_MINUTES = "extra_advance_minutes"

    const val ALARM_TYPE_DUE = "DUE"
    const val ALARM_TYPE_ADVANCE = "ADVANCE"
    const val ALARM_TYPE_RESCHEDULED = "RESCHEDULED"

    const val DAILY_REVIEW_REQUEST_CODE = 888888

    private fun getRequestCode(taskId: String, typeOffset: Int): Int {
        return abs(taskId.hashCode()) * 10 + typeOffset
    }

    fun scheduleRemindersForTask(context: Context, task: Task) {
        // 1. Cancel previous alarms first
        cancelRemindersForTask(context, task.id)

        // 2. Only open tasks can have alarms
        if (!task.isOpen) return

        val nowEpochMs = System.currentTimeMillis()

        // 3. One-time rescheduled reminder takes precedence
        if (task.snoozeUntilEpochMs != null) {
            if (task.snoozeUntilEpochMs > nowEpochMs) {
                scheduleAlarm(
                    context = context,
                    triggerEpochMs = task.snoozeUntilEpochMs,
                    requestCode = getRequestCode(task.id, 3),
                    taskId = task.id,
                    alarmType = ALARM_TYPE_RESCHEDULED
                )
            }
            return
        }

        // 4. Regular reminder
        if (task.reminderEnabled && task.dueDate != null && task.scheduledTime != null) {
            val dueEpochMs = ZonedDateTime.of(
                task.dueDate,
                task.scheduledTime,
                ZoneId.systemDefault()
            ).toInstant().toEpochMilli()

            val advanceMinutes = task.advanceReminder?.minutesBefore ?: 0

            // Advance alarm
            if (advanceMinutes > 0) {
                val advanceEpochMs = dueEpochMs - (advanceMinutes * 60 * 1000L)
                if (advanceEpochMs > nowEpochMs) {
                    scheduleAlarm(
                        context = context,
                        triggerEpochMs = advanceEpochMs,
                        requestCode = getRequestCode(task.id, 2),
                        taskId = task.id,
                        alarmType = ALARM_TYPE_ADVANCE,
                        advanceMinutes = advanceMinutes
                    )
                }
            }

            // Due alarm
            if (dueEpochMs > nowEpochMs) {
                scheduleAlarm(
                    context = context,
                    triggerEpochMs = dueEpochMs,
                    requestCode = getRequestCode(task.id, 1),
                    taskId = task.id,
                    alarmType = ALARM_TYPE_DUE
                )
            } else if (nowEpochMs - dueEpochMs < 60_000L) {
                // If the user selected the current minute and saved just after 00 seconds, alert immediately!
                NotificationHelper.showTaskReminderNotification(context, task)
            }
        }
    }

    fun cancelRemindersForTask(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val dueIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGERED
        }
        val duePending = PendingIntent.getBroadcast(
            context,
            getRequestCode(taskId, 1),
            dueIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        duePending?.let {
            alarmManager.cancel(it)
        }

        val advancePending = PendingIntent.getBroadcast(
            context,
            getRequestCode(taskId, 2),
            dueIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        advancePending?.let {
            alarmManager.cancel(it)
        }

        val snoozePending = PendingIntent.getBroadcast(
            context,
            getRequestCode(taskId, 3),
            dueIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        snoozePending?.let {
            alarmManager.cancel(it)
        }
    }

    fun scheduleTestAlarm(context: Context, delaySeconds: Int = 5) {
        val triggerEpochMs = System.currentTimeMillis() + (delaySeconds * 1000L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGERED
            putExtra("is_test_alarm", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999991,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (ExactAlarmPermissionHelper.canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            } catch (ignored: Exception) {
                Log.e(TAG, "Failed to schedule test alarm", ignored)
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        triggerEpochMs: Long,
        requestCode: Int,
        taskId: String,
        alarmType: String,
        advanceMinutes: Int = 0
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGERED
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_ALARM_TYPE, alarmType)
            if (advanceMinutes > 0) {
                putExtra(EXTRA_ADVANCE_MINUTES, advanceMinutes)
            }
        }
        // Log scheduling details for debugging
        android.util.Log.d(TAG, "Scheduling alarm: taskId=$taskId, type=$alarmType, triggerAt=${java.time.Instant.ofEpochMilli(triggerEpochMs)}")

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (ExactAlarmPermissionHelper.canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission unavailable, falling back to setAndAllowWhileIdle", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Failed to schedule alarm fallback", fallbackException)
            }
        }
    }

    fun scheduleDailyReviewAlarm(context: Context, hour: Int = 20, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = LocalDateTime.now()
        var targetTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        // If today's time has already passed, schedule for tomorrow
        if (!targetTime.isAfter(now)) {
            targetTime = targetTime.plusDays(1)
        }

        val triggerEpochMs = targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, DailyReviewAlarmReceiver::class.java).apply {
            action = ACTION_DAILY_REVIEW_TRIGGERED
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REVIEW_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (ExactAlarmPermissionHelper.canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule exact daily review alarm, using fallback", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerEpochMs,
                pendingIntent
            )
        }
    }

    suspend fun reconcileAllReminders(
        context: Context,
        taskRepository: TaskRepository,
        preferencesRepository: PreferencesRepository
    ) {
        try {
            val tasksWithReminders = taskRepository.getTasksWithActiveReminders()
            for (task in tasksWithReminders) {
                scheduleRemindersForTask(context, task)
            }

            val reviewEnabled = preferencesRepository.dailyReviewEnabledFlow.first()
            if (reviewEnabled) {
                val reviewHour = preferencesRepository.dailyReviewHourFlow.first()
                val reviewMinute = preferencesRepository.dailyReviewMinuteFlow.first()
                scheduleDailyReviewAlarm(context, reviewHour, reviewMinute)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during reminder reconciliation", e)
        }
    }
}
