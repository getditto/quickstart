package com.ditto.quickstart.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun TopBar(
    isLoading: Boolean,
    appId: String,
    appToken: String,
    isSyncEnabled: Boolean,
    onSyncChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colors.primarySurface
    ) {
        Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Ditto Tasks", style = MaterialTheme.typography.h5)
                Spacer(modifier = Modifier.weight(1f))
                TopBarAnimatedVisibility(visible = !isLoading) {
                    SyncButton(
                        checked = isSyncEnabled,
                        onCheckedChange = { checked ->
                            onSyncChange(checked)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            TopBarAnimatedVisibility(visible = !isLoading) {
                Row {
                    Text(
                        text = "App Id:",
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = appId,
                        style = MaterialTheme.typography.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row {
                    Text(
                        text = "App Token:",
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = appToken,
                        style = MaterialTheme.typography.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun RowScope.TopBarAnimatedVisibility(
    visible: Boolean,
    content: @Composable RowScope.() -> Unit
) = AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
    Row { content() }
}


@Composable
internal fun ColumnScope.TopBarAnimatedVisibility(
    visible: Boolean,
    content: @Composable ColumnScope.() -> Unit
) = AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut()
) {
    Column { content() }
}


@Composable
internal fun SyncButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Sync", style = MaterialTheme.typography.subtitle1)
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
