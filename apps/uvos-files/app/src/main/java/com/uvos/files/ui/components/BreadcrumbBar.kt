package com.uvos.files.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.files.ui.theme.IndustrialAccent
import com.uvos.files.ui.theme.IndustrialTextPrimary
import com.uvos.files.ui.theme.IndustrialTextSecondary
import com.uvos.files.ui.theme.LampGreen
import com.uvos.files.ui.theme.UvosCard

@Composable
fun BreadcrumbBar(
    currentPath: String,
    isRootActive: Boolean,
    onNavigate: (String) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = currentPath.split("/").filter { it.isNotEmpty() }
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(UvosCard, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onNavigateUp,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Up",
                tint = IndustrialTextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Mode badge: ROOT / SAF
        Box(
            modifier = Modifier
                .background(
                    if (isRootActive) IndustrialAccent.copy(alpha = 0.2f) else LampGreen.copy(alpha = 0.2f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = if (isRootActive) IndustrialAccent else LampGreen,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = if (isRootActive) "ROOT" else "SAF",
                    color = if (isRootActive) IndustrialAccent else LampGreen,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Path segments scrollable row
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "/",
                color = IndustrialAccent,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onNavigate("/") }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )

            var accumulatedPath = ""
            segments.forEachIndexed { index, segment ->
                accumulatedPath += "/$segment"
                val targetPath = accumulatedPath
                val isLast = index == segments.size - 1

                Text(
                    text = segment,
                    color = if (isLast) IndustrialTextPrimary else IndustrialTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { onNavigate(targetPath) }
                        .padding(horizontal = 3.dp, vertical = 4.dp)
                )

                if (!isLast) {
                    Text(
                        text = "/",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
