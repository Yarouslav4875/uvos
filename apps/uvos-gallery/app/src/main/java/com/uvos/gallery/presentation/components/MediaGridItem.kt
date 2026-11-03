package com.uvos.gallery.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uvos.gallery.domain.model.MediaItem
import com.uvos.gallery.domain.model.MediaType
import com.uvos.gallery.presentation.theme.UvosBackground
import com.uvos.gallery.presentation.theme.UvosBlueEncrypted
import com.uvos.gallery.presentation.theme.UvosDialIvory
import com.uvos.gallery.presentation.theme.UvosTileSurface

@Composable
fun MediaGridItem(
    item: MediaItem,
    onClick: () -> Unit,
    onThreatBadgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(UvosTileSurface)
            .clickable { onClick() }
    ) {
        if (item.isVaultItem) {
            // Vault item encrypted placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF14161E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Encrypted Vault Item",
                    tint = UvosBlueEncrypted,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            // Public unencrypted image preview
            AsyncImage(
                model = item.contentUri,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Video playback indicator icon
        if (item.mediaType == MediaType.VIDEO) {
            Icon(
                imageVector = Icons.Default.PlayCircleFilled,
                contentDescription = "Video",
                tint = UvosDialIvory.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
            )
        }

        // Bottom industrial HUD readout
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(UvosBackground.copy(alpha = 0.82f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = item.resolutionText,
                color = UvosDialIvory,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        // Upper-right hardware status lamp
        ThreatIndicatorBadge(
            threatLevel = item.threatLevel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
            size = 14.dp,
            onClick = onThreatBadgeClick
        )
    }
}
