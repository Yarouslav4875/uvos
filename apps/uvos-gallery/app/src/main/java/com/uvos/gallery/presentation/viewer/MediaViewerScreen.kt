package com.uvos.gallery.presentation.viewer

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sanitizer
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.uvos.gallery.domain.model.MediaItem
import com.uvos.gallery.domain.model.MediaType
import com.uvos.gallery.presentation.components.ThreatIndicatorBadge
import com.uvos.gallery.presentation.theme.UvosBackground
import com.uvos.gallery.presentation.theme.UvosDialIvory
import com.uvos.gallery.presentation.theme.UvosGreenSterile
import com.uvos.gallery.presentation.theme.UvosMuted
import com.uvos.gallery.presentation.theme.UvosRedDanger
import com.uvos.gallery.presentation.theme.UvosTileSurface

@OptIn(UnstableApi::class)
@Composable
fun MediaViewerScreen(
    item: MediaItem,
    onBack: () -> Unit,
    onInspectExif: () -> Unit,
    onScrubInPlace: () -> Unit,
    onMoveToVault: () -> Unit,
    onShareClean: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UvosBackground)
    ) {
        // Main Media Display Area
        if (item.mediaType == MediaType.VIDEO) {
            val exoPlayer = remember(item.contentUri) {
                ExoPlayer.Builder(context).build().apply {
                    val mediaItem = ExoMediaItem.fromUri(item.contentUri)
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    exoPlayer.release()
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = item.contentUri,
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top M4200 Header HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(UvosBackground.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = UvosDialIvory
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = UvosDialIvory,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.resolutionText,
                    color = UvosMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            ThreatIndicatorBadge(
                threatLevel = item.threatLevel,
                size = 16.dp,
                onClick = onInspectExif
            )
        }

        // Bottom Instrument Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(UvosTileSurface.copy(alpha = 0.95f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewerActionButton(
                icon = Icons.Default.Info,
                label = "EXIF АНАЛИЗ",
                tint = UvosDialIvory,
                onClick = onInspectExif
            )

            ViewerActionButton(
                icon = Icons.Default.Sanitizer,
                label = "ОЧИСТИТЬ EXIF",
                tint = UvosGreenSterile,
                onClick = onScrubInPlace
            )

            ViewerActionButton(
                icon = Icons.Default.Share,
                label = "ПОДЕЛИТЬСЯ",
                tint = UvosDialIvory,
                onClick = onShareClean
            )

            if (!item.isVaultItem) {
                ViewerActionButton(
                    icon = Icons.Default.Lock,
                    label = "В СЕЙФ",
                    tint = UvosRedDanger,
                    onClick = onMoveToVault
                )
            }
        }
    }
}

@Composable
private fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            color = tint,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
