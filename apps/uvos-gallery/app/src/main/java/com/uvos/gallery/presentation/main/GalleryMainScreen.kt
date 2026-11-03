package com.uvos.gallery.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.gallery.domain.model.MediaItem
import com.uvos.gallery.domain.model.ThreatLevel
import com.uvos.gallery.domain.repository.MediaRepository
import com.uvos.gallery.presentation.components.MediaGridItem
import com.uvos.gallery.presentation.theme.UvosBackground
import com.uvos.gallery.presentation.theme.UvosDialIvory
import com.uvos.gallery.presentation.theme.UvosRedDanger
import com.uvos.gallery.presentation.theme.UvosTileSurface
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GalleryMainScreen(
    mediaRepository: MediaRepository,
    onMediaSelected: (MediaItem) -> Unit,
    onInspectExif: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var mediaList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        mediaRepository.observePublicMedia().collectLatest { list ->
            mediaList = list
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UvosBackground)
            .padding(8.dp)
    ) {
        // Ретро-приборная панель безопасности галереи
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(UvosTileSurface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "UVOS ОФФЛАЙН ГАЛЕРЕЯ",
                    color = UvosDialIvory,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "ИНВАРИАНТ: 0% ТЕЛЕМЕТРИИ, EXIF SCRUBBER",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            val highThreatCount = mediaList.count { it.threatLevel == ThreatLevel.HIGH_RISK }
            Surface(
                color = if (highThreatCount > 0) UvosRedDanger else Color(0xFF27AE60),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = if (highThreatCount > 0) "GPS УТЕЧКИ: $highThreatCount" else "GPS ЧИСТО",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = UvosRedDanger)
            }
        } else if (mediaList.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "НЕТ ФОТО ИЛИ ВИДЕО В ХРАНИЛИЩЕ",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(2.dp)
            ) {
                items(mediaList, key = { it.id }) { item ->
                    MediaGridItem(
                        item = item,
                        onClick = { onMediaSelected(item) },
                        onThreatBadgeClick = { onInspectExif(item) }
                    )
                }
            }
        }
    }
}
