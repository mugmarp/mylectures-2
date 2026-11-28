package com.lectures.features.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import com.lectures.ui.components.onEscapeKey

@Composable
internal fun AccountDeleteCloudDataDialog(
    preferDesktopDialogs: Boolean,
    isDeleting: Boolean,
    alsoClearLocal: Boolean,
    onToggleClearLocal: (Boolean) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .then(
                if (preferDesktopDialogs) {
                    Modifier.widthIn(min = 400.dp, max = 520.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp).widthIn(max = 440.dp)
                }
            )
            .onEscapeKey { if (!isDeleting) onDismiss() },
        containerColor = ext.surfaceCard,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(ext.dangerBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.DeleteForever,
                    contentDescription = null,
                    tint = ext.dangerIcon,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "Delete Cloud Backup Data?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "This permanently deletes your synced lecture notes, coursework tasks, custom events, and saved preferences from Cloud Firestore and disconnects your Google account from this device.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ext.surfaceBase,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .desktopHoverable()
                        .desktopHandCursor(enabled = !isDeleting)
                        .clickable(enabled = !isDeleting) { onToggleClearLocal(!alsoClearLocal) }
                        .testTag("delete_cloud_clear_local_row")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = alsoClearLocal,
                            onCheckedChange = { if (!isDeleting) onToggleClearLocal(it) },
                            enabled = !isDeleting,
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("delete_cloud_clear_local_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Also clear local copies from this device",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (alsoClearLocal) {
                                    "Removes synced records, attachments, and alarms from this phone too."
                                } else {
                                    "Unchecked: keeps your notes and tasks offline in Guest mode."
                                },
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = ext.textMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor(enabled = !isDeleting)
                    .testTag("dialog_delete_cloud_confirm_btn")
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deleting...", fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        if (alsoClearLocal) "Delete Cloud & Local" else "Delete Cloud Backup",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor(enabled = !isDeleting)
                    .testTag("dialog_delete_cloud_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}
