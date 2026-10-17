package com.localtaskmanager.app.ui.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.localtaskmanager.app.domain.model.Priority
import com.localtaskmanager.app.domain.model.TaskStatus
import com.localtaskmanager.app.ui.components.EmptyStateView
import com.localtaskmanager.app.ui.components.TaskItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTasksScreen(
    viewModel: TasksViewModel,
    onNavigateToEditTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("All Tasks (${uiState.tasks.size})") },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Tasks")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        TaskSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.displayName) },
                                onClick = {
                                    viewModel.onSortOrderChanged(order)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search tasks...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter Chips Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Filters
                FilterChip(
                    selected = uiState.selectedStatusFilter == null,
                    onClick = { viewModel.onStatusFilterChanged(null) },
                    label = { Text("All Status") }
                )
                FilterChip(
                    selected = uiState.selectedStatusFilter == TaskStatus.OPEN,
                    onClick = { viewModel.onStatusFilterChanged(TaskStatus.OPEN) },
                    label = { Text("Open") }
                )
                FilterChip(
                    selected = uiState.selectedStatusFilter == TaskStatus.COMPLETED,
                    onClick = { viewModel.onStatusFilterChanged(TaskStatus.COMPLETED) },
                    label = { Text("Completed") }
                )

                // Priority Filters
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = uiState.selectedPriorityFilter == p,
                        onClick = {
                            viewModel.onPriorityFilterChanged(
                                if (uiState.selectedPriorityFilter == p) null else p
                            )
                        },
                        label = { Text(p.displayName) }
                    )
                }

                // Category Filters
                uiState.allCategories.forEach { cat ->
                    FilterChip(
                        selected = uiState.selectedCategoryId == cat.id,
                        onClick = {
                            viewModel.onCategoryFilterChanged(
                                if (uiState.selectedCategoryId == cat.id) null else cat.id
                            )
                        },
                        label = { Text(cat.name) }
                    )
                }
            }

            // Task List
            if (uiState.tasks.isEmpty() && !uiState.isLoading) {
                EmptyStateView(
                    title = "No tasks found",
                    message = if (uiState.searchQuery.isNotEmpty()) "No tasks match your search or filter criteria" else "You haven't created any tasks yet"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.tasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            is24Hour = uiState.is24HourFormat,
                            onToggleCompletion = { viewModel.toggleTaskCompletion(context, it) },
                            onClick = { onNavigateToEditTask(it.id) },
                            onDelete = { viewModel.deleteTask(context, it) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}
