package com.localtaskmanager.app.platform.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.localtaskmanager.app.MainActivity
import com.localtaskmanager.app.R
import com.localtaskmanager.app.domain.model.Priority
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.ui.reschedule.RescheduleReminderActivity
import kotlin.math.abs

object NotificationHelper {

    const val ACTION_DONE = "com.localtaskmanager.app.ACTION_DONE"
    const val ACTION_RESCHEDULE = "com.localtaskmanager.app.ACTION_RESCHEDULE"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_OPEN_DAILY_REVIEW = "extra_open_daily_review"

    fun getNotificationIdForTask(taskId: String): Int {
        return abs(taskId.hashCode())
    }

    fun showTaskReminderNotification(
        context: Context,
        task: Task,
        reminderContext: String? = null
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = getNotificationIdForTask(task.id)

        // 1. Content Intent: Tapping notification body opens MainActivity with the task ID
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action One: Done (Explicit PendingIntent to DoneActionReceiver)
        val doneIntent = Intent(context, DoneActionReceiver::class.java).apply {
            action = ACTION_DONE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action Two: Reschedule (Direct Activity PendingIntent to RescheduleReminderActivity)
        val rescheduleIntent = Intent(context, RescheduleReminderActivity::class.java).apply {
            action = ACTION_RESCHEDULE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val reschedulePendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 2,
            rescheduleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            reminderContext != null -> "Reminder: ${task.title}"
            task.category != null -> "${task.category.name}: ${task.title}"
            else -> "Task Reminder"
        }

        val contentText = buildString {
            if (reminderContext != null) {
                append(reminderContext)
            } else {
                append(task.title)
            }
            if (!task.notes.isNullOrBlank()) {
                append(" • ")
                append(task.notes)
            }
            if (task.priority == Priority.HIGH) {
                append(" [High Priority]")
            }
        }

        val builder = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_task_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            // EXACTLY TWO ACTIONS: Done and Reschedule
            .addAction(0, "Done", donePendingIntent)
            .addAction(0, "Reschedule", reschedulePendingIntent)

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Failed to post task reminder notification", e)
        }
    }

    fun dismissNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    fun sendTestNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val testNotificationId = 777777

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            testNotificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_task_notification)
            .setContentTitle("🔔 Test Notification: Task Manager")
            .setContentText("Notifications and alerts are working properly on your phone!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Great news! Notifications and sounds are configured properly. Your task reminders will alert you on time."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        try {
            notificationManager.notify(testNotificationId, builder.build())
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Failed to send test notification", e)
        }
    }

    fun showDailyReviewNotification(context: Context, pendingCount: Int) {
        if (pendingCount <= 0) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val reviewNotificationId = 999999

        val reviewIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_DAILY_REVIEW, true)
        }
        val reviewPendingIntent = PendingIntent.getActivity(
            context,
            reviewNotificationId,
            reviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (pendingCount == 1) {
            "You have 1 pending task to review for today."
        } else {
            "You have $pendingCount pending tasks to review for today."
        }

        val builder = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_DAILY_REVIEW)
            .setSmallIcon(R.drawable.ic_task_notification)
            .setContentTitle("Daily Task Review")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(reviewPendingIntent)

        try {
            notificationManager.notify(reviewNotificationId, builder.build())
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Failed to post daily review notification", e)
        }
    }
}
