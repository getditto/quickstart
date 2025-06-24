package com.ditto.quickstart.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun RemoveTaskDialog(
    taskTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        title = { Text(text = "Delete task", style = MaterialTheme.typography.h6) },
        text = {
            Column {
                Text(text = "Confirm deletion of task:", style = MaterialTheme.typography.subtitle1)
                Text(text = taskTitle, style = MaterialTheme.typography.subtitle2)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Dismiss")
            }
        },
        onDismissRequest = onDismiss,
    )
}
