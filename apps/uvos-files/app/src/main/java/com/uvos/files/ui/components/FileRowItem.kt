package com.uvos.files.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.files.domain.model.FileItem
import com.uvos.files.domain.model.FileType
import com.uvos.files.ui.theme.IndustrialAccent
import com.uvos.files.ui.theme.IndustrialSelected
import com.uvos.files.ui.theme.IndustrialTextPrimary
import com.uvos.files.ui.theme.IndustrialTextSecondary
import com.uvos.files.ui.theme.LampGreen
import com.uvos.files.ui.theme.LampYellow
import com.uvos.files.ui.theme.UvosBorder
import com.uvos.files.ui.theme.UvosCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FileRowItem(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOpenHexViewer: (FileItem) -> Unit,
    onOpenTextEditor: (FileItem) -> Unit,
    onCalculateChecksum: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) IndustrialSelected else UvosCard,
                RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        val icon = when {
            item.isDirectory -> Icons.Default.Folder
            item.fileType == FileType.BLOCK_DEVICE || item.fileType == FileType.CHARACTER_DEVICE -> Icons.Default.Memory
            item.isConfigFile -> Icons.Default.Settings
            item.fileType == FileType.SYMLINK -> Icons.Default.Code
            else -> Icons.Default.Description
        }

        val iconTint = when {
            item.isDirectory -> LampYellow
            item.isConfigFile -> LampGreen
            item.fileType == FileType.SYMLINK -> IndustrialAccent
            else -> IndustrialTextSecondary
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Name and Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                color = IndustrialTextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Permissions & Owner
                val permStr = item.permissions?.toFormattedString() ?: "----------"
                Text(
                    text = "$permStr ${item.owner}:${item.group}",
                    color = IndustrialTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                // Size
                Text(
                    text = item.formattedSize,
                    color = if (item.isDirectory) LampYellow else IndustrialAccent,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )

                // Date
                Text(
                    text = dateFormat.format(Date(item.lastModifiedEpochMs)),
                    color = Color.DarkGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Actions Menu
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = IndustrialTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(UvosCard)
            ) {
                if (!item.isDirectory) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Hex Viewer / Editor",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = IndustrialTextPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Memory, null, tint = IndustrialAccent, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenHexViewer(item)
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                "Text / Config Editor",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = IndustrialTextPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Code, null, tint = LampGreen, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenTextEditor(item)
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                "Calculate Checksum (SHA/MD5)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = IndustrialTextPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Fingerprint, null, tint = LampYellow, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onCalculateChecksum(item)
                        }
                    )
                }

                DropdownMenuItem(
                    text = {
                        Text(
                            "Delete",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = IndustrialAccent
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete(item)
                    }
                )
            }
        }
    }
}
