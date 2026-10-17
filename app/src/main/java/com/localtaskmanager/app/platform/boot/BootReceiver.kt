package com.localtaskmanager.app.platform.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.localtaskmanager.app.LocalTaskManagerApp
import com.localtaskmanager.app.platform.reminders.ReminderScheduler
import com.localtaskmanager.app.platform.widgets.WidgetRefreshCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED
        ) {
            val pendingResult = goAsync()
            val app = context.applicationContext as LocalTaskManagerApp

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Reconcile all alarms with current database state
                    ReminderScheduler.reconcileAllReminders(
                        context = context,
                        taskRepository = app.appContainer.taskRepository,
                        preferencesRepository = app.appContainer.preferencesRepository
                    )

                    // Refresh Glance widgets
                    WidgetRefreshCoordinator.refreshAllWidgets(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
