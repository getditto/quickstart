package live.ditto.quickstart.tasks.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import live.ditto.quickstart.tasks.BuildConfig
import live.ditto.quickstart.tasks.R
import live.ditto.quickstart.tasks.data.Task
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksListScreen(navController: NavController) {
    val tasksListViewModel: TasksListScreenViewModel = viewModel()
    val tasks: List<Task> by tasksListViewModel.tasks.observeAsState(emptyList())
    val syncEnabled: Boolean by tasksListViewModel.syncEnabled.observeAsState(true)
    val memoryUsage: Long by tasksListViewModel.memoryUsage.observeAsState(0)
    val dataGenerationStatus: String by tasksListViewModel.dataGenerationStatus.observeAsState("")

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteDialogTaskId by remember { mutableStateOf("") }
    var showMemoryTestDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Ditto Tasks",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "App ID: ${BuildConfig.DITTO_APP_ID}",
                                style = TextStyle(fontSize = 10.sp)
                            )
                            Text(
                                text = "Token: ${BuildConfig.DITTO_PLAYGROUND_TOKEN}",
                                style = TextStyle(fontSize = 10.sp)
                            )
                            Text(
                                text = "Memory: ${memoryUsage / (1024 * 1024)}MB",
                                style = TextStyle(fontSize = 12.sp),
                                color = Color.Yellow
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.blue_700),
                    titleContentColor = Color.White
                ),
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showMemoryTestDialog = true }) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Memory Test",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "Sync",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 10.dp),
                            color = Color.White
                        )
                        Switch(
                            checked = syncEnabled,
                            onCheckedChange = { isChecked ->
                                tasksListViewModel.setSyncEnabled(isChecked)
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                icon = { Icon(Icons.Filled.Add, "", tint = Color.White) },
                text = { Text(text = "New Task", color = Color.White) },
                onClick = { navController.navigate("tasks/edit") },
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                containerColor = colorResource(id = R.color.blue_500)
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Show data generation status if available
                if (dataGenerationStatus.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = dataGenerationStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                TasksList(
                    tasks = tasks,
                    onToggle = { tasksListViewModel.toggle(it) },
                    onClickEdit = {
                        navController.navigate("tasks/edit/${it}")
                    },
                    onClickDelete = {
                        deleteDialogTaskId = it
                        showDeleteDialog = true
                    }
                )
            }
        }
    )

    // Alert displayed if user taps a Delete icon for a list item
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Confirm Deletion",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(text = "Are you sure you want to delete this item?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        tasksListViewModel.delete(deleteDialogTaskId)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Memory test dialog
    if (showMemoryTestDialog) {
        AlertDialog(
            onDismissRequest = { showMemoryTestDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Memory Test",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Memory Leak Test",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "Test memory leak issue SDKS-1463",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Current Memory: ${memoryUsage / (1024 * 1024)}MB",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = { tasksListViewModel.generateLargeDataset() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text("1. Generate Large Dataset (~10MB)")
                    }
                    Button(
                        onClick = { tasksListViewModel.closeQueryResultOnly() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text("2. Close Query Result Only (Leaks)")
                    }
                    Button(
                        onClick = { tasksListViewModel.closeQueryResultAndItems() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text("3. Close Result + Items (Frees Memory)")
                    }
                    Button(
                        onClick = { tasksListViewModel.clearTestData() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text("4. Clear Test Data")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showMemoryTestDialog = false }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun TasksList(
    tasks: List<Task>,
    onToggle: ((taskId: String) -> Unit)? = null,
    onClickEdit: ((taskId: String) -> Unit)? = null,
    onClickDelete: ((taskId: String) -> Unit)? = null,
) {
    LazyColumn {
        items(tasks) { task ->
            TaskRow(
                task = task,
                onToggle = { onToggle?.invoke(it._id) },
                onClickEdit = { onClickEdit?.invoke(it._id) },
                onClickDelete = { onClickDelete?.invoke(it._id) }
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_3
)
@Composable
fun TasksListPreview() {
    TasksList(
        tasks = listOf(
            Task(UUID.randomUUID().toString(), "Get Milk", true, false),
            Task(UUID.randomUUID().toString(), "Get Oats", false, false),
            Task(UUID.randomUUID().toString(), "Get Berries", true, false),
        )
    )
}
