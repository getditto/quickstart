package com.ditto.quickstart.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ditto.quickstart.data.Task
import com.ditto.quickstart.data.screenstate.TaskListScreenState
import com.ditto.quickstart.ui.components.Loading

@Composable
fun TaskListScreen(
    state: TaskListScreenState,
    onCheck: (Task, Boolean) -> Unit,
    onEdit: (Task) -> Unit,
    onRemove: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        Loading()
        return // Don't show tasks list while loading
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(state.tasks) { task ->
            TaskItem(
                task = task,
                onCheck = { onCheck(task, it) },
                onEdit = { onEdit(task) },
                onRemove = { onRemove(task) }
            )
        }
    }
}

@Composable
private fun TaskItem(
    task: Task,
    onCheck: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.done,
            onCheckedChange = onCheck,
        )
        val titleTextDecoration = if (task.done) {
            TextDecoration.LineThrough
        } else {
            MaterialTheme.typography.body1.textDecoration
        }

        Text(
            text = task.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.body1.copy(textDecoration = titleTextDecoration),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEdit) {
            Icon(
                painter = rememberVectorPainter(Icons.Default.Edit),
                contentDescription = "edit"
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onRemove) {
            Icon(
                painter = rememberVectorPainter(Icons.Default.Delete),
                contentDescription = "remove"
            )
        }
        Spacer(Modifier.width(16.dp))
    }
}
