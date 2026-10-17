package com.localtaskmanager.app.ui.navigation

sealed class NavRoutes(val route: String) {
    data object Home : NavRoutes("home")
    data object AllTasks : NavRoutes("all_tasks")
    data object Calendar : NavRoutes("calendar")
    data object DailyReview : NavRoutes("daily_review")
    data object Settings : NavRoutes("settings")
    data object Categories : NavRoutes("categories")
    data object AddEditTask : NavRoutes("add_edit_task?taskId={taskId}") {
        fun createRoute(taskId: String? = null): String {
            return if (taskId != null) "add_edit_task?taskId=$taskId" else "add_edit_task"
        }
    }
}
