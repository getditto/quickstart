package live.ditto.quickstart.tasks.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import live.ditto.quickstart.tasks.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(navController: NavController, taskId: String?) {
    val editScreenViewModel: EditScreenViewModel = viewModel()
    editScreenViewModel.setupWithTask(id = taskId)

    val topBarTitle = if (taskId == null) "New Task" else "Edit Task"

    val body: String by editScreenViewModel.body.observeAsState("")
    val isCompleted: Boolean by editScreenViewModel.isCompleted.observeAsState(initial = false)
    val canDelete: Boolean by editScreenViewModel.canDelete.observeAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.blue_700)
                )
            )
        },
        content = { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EditForm(
                    canDelete = canDelete,
                    body = body,
                    onBodyTextChange = { editScreenViewModel.body.value = it },
                    isCompleted = isCompleted,
                    onIsCompletedChanged = { editScreenViewModel.isCompleted.value = it },
                    onSaveButtonClicked = {
                        editScreenViewModel.save()
                        navController.popBackStack()
                    },
                    onDeleteButtonClicked = {
                        editScreenViewModel.delete()
                        navController.popBackStack()
                    }
                )
            }
        }
    )
}
