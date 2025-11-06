package com.ditto.quickstart.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ditto.quickstart.data.screenstate.TaskAddEditScreenState
import com.ditto.quickstart.ui.components.Loading
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun TaskAddEditScreen(
    state: TaskAddEditScreenState,
    onCancel: () -> Unit,
    onSubmit: (title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Loading()
        return
    }

    var updatedTaskTitle by remember { mutableStateOf(state.taskTitle) }

    val title = if (state.isEditing) {
        "Edit Task"
    } else {
        "Add Task"
    }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = updatedTaskTitle,
            placeholder = { Text(text = "Task Title") },
            onValueChange = { updatedTaskTitle = it },
        )
        Row {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) {
                Text(text = "Cancel")
            }
            Spacer(Modifier.width(16.dp))
            Button(onClick = { onSubmit(updatedTaskTitle) }) {
                Text(text = "Submit")
            }
        }
    }
}

@Preview
@Composable
private fun TaskEditScreenPreview() {
    TaskAddEditScreen(
        state = TaskAddEditScreenState(
            isLoading = false,
            isEditing = true,
            taskTitle = "",
        ),
        onCancel = {},
        onSubmit = {}
    )
}

@Preview
@Composable
private fun TaskAddScreenPreview() {
    TaskAddEditScreen(
        state = TaskAddEditScreenState(
            isLoading = false,
            isEditing = false,
            taskTitle = "",
        ),
        onCancel = {},
        onSubmit = {}
    )
}
