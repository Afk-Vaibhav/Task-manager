package com.localtaskmanager.app

import android.app.Application
import com.localtaskmanager.app.di.AppContainer
import com.localtaskmanager.app.platform.notifications.NotificationChannels
import com.localtaskmanager.app.platform.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocalTaskManagerApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        NotificationChannels.createChannels(this)

        // Reconcile reminders on app launch in the background
        CoroutineScope(Dispatchers.IO).launch {
            ReminderScheduler.reconcileAllReminders(
                context = this@LocalTaskManagerApp,
                taskRepository = appContainer.taskRepository,
                preferencesRepository = appContainer.preferencesRepository
            )
        }
    }
}
