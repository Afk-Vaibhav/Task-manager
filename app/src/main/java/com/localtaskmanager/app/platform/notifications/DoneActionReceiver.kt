package com.localtaskmanager.app.platform.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.localtaskmanager.app.LocalTaskManagerApp
import com.localtaskmanager.app.platform.reminders.ReminderScheduler
import com.localtaskmanager.app.platform.widgets.WidgetRefreshCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoneActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID) ?: return
        val notificationId = intent.getIntExtra(
            NotificationHelper.EXTRA_NOTIFICATION_ID,
            NotificationHelper.getNotificationIdForTask(taskId)
        )

        val pendingResult = goAsync()
        val app = context.applicationContext as LocalTaskManagerApp

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Mark task completed and generate next recurring occurrence if applicable
                app.appContainer.completeTaskUseCase(taskId)

                // 2. Cancel pending alarms for this task
                ReminderScheduler.cancelRemindersForTask(context, taskId)

                // 3. Dismiss notification
                NotificationHelper.dismissNotification(context, notificationId)

                // 4. Refresh both widgets
                WidgetRefreshCoordinator.refreshAllWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
