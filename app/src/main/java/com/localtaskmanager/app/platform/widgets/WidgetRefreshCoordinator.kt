package com.localtaskmanager.app.platform.widgets

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetRefreshCoordinator {

    suspend fun refreshAllWidgets(context: Context) {
        try {
            QuickAddWidget().updateAll(context)
        } catch (_: Exception) {
            // Widget not added on home screen or Glance internal error
        }

        try {
            UpcomingTasksWidget().updateAll(context)
        } catch (_: Exception) {
            // Widget not added on home screen or Glance internal error
        }
    }
}
