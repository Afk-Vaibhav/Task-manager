package com.localtaskmanager.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.localtaskmanager.app.di.AppContainer
import com.localtaskmanager.app.ui.addedit.AddEditTaskScreen
import com.localtaskmanager.app.ui.addedit.AddEditTaskViewModel
import com.localtaskmanager.app.ui.calendar.CalendarScreen
import com.localtaskmanager.app.ui.calendar.CalendarViewModel
import com.localtaskmanager.app.ui.categories.CategoriesScreen
import com.localtaskmanager.app.ui.categories.CategoriesViewModel
import com.localtaskmanager.app.ui.home.HomeScreen
import com.localtaskmanager.app.ui.home.HomeViewModel
import com.localtaskmanager.app.ui.review.DailyReviewScreen
import com.localtaskmanager.app.ui.review.DailyReviewViewModel
import com.localtaskmanager.app.ui.settings.SettingsScreen
import com.localtaskmanager.app.ui.settings.SettingsViewModel
import com.localtaskmanager.app.ui.tasks.AllTasksScreen
import com.localtaskmanager.app.ui.tasks.TasksViewModel

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppNavigation(
    appContainer: AppContainer,
    startDestination: String = NavRoutes.Home.route,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem(NavRoutes.Home.route, "Home", Icons.Default.Home),
        BottomNavItem(NavRoutes.AllTasks.route, "Tasks", Icons.Default.CheckCircle),
        BottomNavItem(NavRoutes.Calendar.route, "Calendar", Icons.Default.CalendarMonth),
        BottomNavItem(NavRoutes.Categories.route, "Categories", Icons.Default.Category),
        BottomNavItem(NavRoutes.DailyReview.route, "Review", Icons.Default.RateReview),
        BottomNavItem(NavRoutes.Settings.route, "Settings", Icons.Default.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home Dashboard
            composable(NavRoutes.Home.route) {
                val homeViewModel = HomeViewModel(
                    taskRepository = appContainer.taskRepository,
                    preferencesRepository = appContainer.preferencesRepository,
                    completeTaskUseCase = appContainer.completeTaskUseCase,
                    reopenTaskUseCase = appContainer.reopenTaskUseCase,
                    deleteTaskUseCase = appContainer.deleteTaskUseCase
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAddTask = {
                        navController.navigate(NavRoutes.AddEditTask.createRoute(null))
                    },
                    onNavigateToEditTask = { taskId ->
                        navController.navigate(NavRoutes.AddEditTask.createRoute(taskId))
                    }
                )
            }

            // All Tasks
            composable(NavRoutes.AllTasks.route) {
                val tasksViewModel = TasksViewModel(
                    taskRepository = appContainer.taskRepository,
                    categoryRepository = appContainer.categoryRepository,
                    preferencesRepository = appContainer.preferencesRepository,
                    completeTaskUseCase = appContainer.completeTaskUseCase,
                    reopenTaskUseCase = appContainer.reopenTaskUseCase,
                    deleteTaskUseCase = appContainer.deleteTaskUseCase
                )
                AllTasksScreen(
                    viewModel = tasksViewModel,
                    onNavigateToEditTask = { taskId ->
                        navController.navigate(NavRoutes.AddEditTask.createRoute(taskId))
                    }
                )
            }

            // Calendar / Agenda
            composable(NavRoutes.Calendar.route) {
                val calendarViewModel = CalendarViewModel(
                    taskRepository = appContainer.taskRepository,
                    preferencesRepository = appContainer.preferencesRepository,
                    completeTaskUseCase = appContainer.completeTaskUseCase,
                    reopenTaskUseCase = appContainer.reopenTaskUseCase,
                    deleteTaskUseCase = appContainer.deleteTaskUseCase
                )
                CalendarScreen(
                    viewModel = calendarViewModel,
                    onNavigateToEditTask = { taskId ->
                        navController.navigate(NavRoutes.AddEditTask.createRoute(taskId))
                    }
                )
            }

            // Categories
            composable(NavRoutes.Categories.route) {
                val categoriesViewModel = CategoriesViewModel(
                    categoryRepository = appContainer.categoryRepository
                )
                CategoriesScreen(viewModel = categoriesViewModel)
            }

            // Daily Review
            composable(NavRoutes.DailyReview.route) {
                val reviewViewModel = DailyReviewViewModel(
                    dailyReviewUseCase = appContainer.dailyReviewUseCase,
                    completeTaskUseCase = appContainer.completeTaskUseCase,
                    preferencesRepository = appContainer.preferencesRepository
                )
                DailyReviewScreen(
                    viewModel = reviewViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Settings
            composable(NavRoutes.Settings.route) {
                val settingsViewModel = SettingsViewModel(
                    preferencesRepository = appContainer.preferencesRepository,
                    categoryRepository = appContainer.categoryRepository,
                    taskRepository = appContainer.taskRepository,
                    dataBackupManager = appContainer.dataBackupManager
                )
                SettingsScreen(viewModel = settingsViewModel)
            }

            // Add/Edit Task
            composable(
                route = NavRoutes.AddEditTask.route,
                arguments = listOf(
                    navArgument("taskId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")
                val addEditViewModel = AddEditTaskViewModel(
                    taskRepository = appContainer.taskRepository,
                    categoryRepository = appContainer.categoryRepository,
                    preferencesRepository = appContainer.preferencesRepository,
                    recurrenceSeriesDao = appContainer.database.recurrenceSeriesDao(),
                    createTaskUseCase = appContainer.createTaskUseCase,
                    updateTaskUseCase = appContainer.updateTaskUseCase,
                    initialTaskId = taskId
                )
                AddEditTaskScreen(
                    viewModel = addEditViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
