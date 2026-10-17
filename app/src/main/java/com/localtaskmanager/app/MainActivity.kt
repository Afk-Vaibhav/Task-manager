package com.localtaskmanager.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.localtaskmanager.app.domain.repository.AppTheme
import com.localtaskmanager.app.platform.notifications.NotificationHelper
import com.localtaskmanager.app.ui.navigation.AppNavigation
import com.localtaskmanager.app.ui.navigation.NavRoutes
import com.localtaskmanager.app.ui.theme.LocalTaskManagerTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission result handled cleanly without crashing or blocking task management
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS runtime permission on Android 13+ if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val app = application as LocalTaskManagerApp

        // Check if opened from notification
        val openDailyReview = intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_DAILY_REVIEW, false)
        val initialTaskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID)

        val startRoute = when {
            openDailyReview -> NavRoutes.DailyReview.route
            initialTaskId != null -> NavRoutes.AddEditTask.createRoute(initialTaskId)
            else -> NavRoutes.Home.route
        }

        setContent {
            val themePreference by app.appContainer.preferencesRepository.themeFlow
                .collectAsState(initial = AppTheme.SYSTEM)

            LocalTaskManagerTheme(themePreference = themePreference) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        appContainer = app.appContainer,
                        startDestination = startRoute
                    )
                }
            }
        }
    }
}
