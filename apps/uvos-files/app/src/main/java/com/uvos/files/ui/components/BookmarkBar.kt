package com.uvos.files.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.files.ui.theme.IndustrialAccent
import com.uvos.files.ui.theme.IndustrialTextPrimary
import com.uvos.files.ui.theme.IndustrialTextSecondary
import com.uvos.files.ui.theme.UvosBorder
import com.uvos.files.ui.theme.UvosCard

data class BookmarkItem(
    val label: String,
    val path: String
)

val DefaultBookmarks = listOf(
    BookmarkItem("ROOT (/)", "/"),
    BookmarkItem("STORAGE", "/sdcard"),
    BookmarkItem("DOWNLOADS", "/sdcard/Download"),
    BookmarkItem("DATA-APPS", "/data/app"),
    BookmarkItem("SYS-CONFIG", "/system/etc"),
    BookmarkItem("USB-OTG", "/storage")
)

@Composable
fun BookmarkBar(
    currentPath: String,
    onBookmarkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DefaultBookmarks.forEach { bookmark ->
            val isSelected = currentPath == bookmark.path ||
                    (bookmark.path != "/" && currentPath.startsWith(bookmark.path))

            FilterChip(
                selected = isSelected,
                onClick = { onBookmarkClick(bookmark.path) },
                label = {
                    Text(
                        text = bookmark.label,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) IndustrialAccent else IndustrialTextSecondary
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = UvosCard,
                    selectedContainerColor = IndustrialAccent.copy(alpha = 0.15f),
                    labelColor = IndustrialTextPrimary,
                    selectedLabelColor = IndustrialAccent
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = UvosBorder,
                    selectedBorderColor = IndustrialAccent,
                    enabled = true,
                    selected = isSelected
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}
