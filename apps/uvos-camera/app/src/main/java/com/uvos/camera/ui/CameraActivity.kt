package com.uvos.camera.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.uvos.camera.ui.components.OscilloscopeGridOverlay
import com.uvos.camera.ui.components.ShutterControl
import com.uvos.camera.ui.components.StatusLamp
import com.uvos.camera.ui.theme.AccentRed
import com.uvos.camera.ui.theme.BgDark
import com.uvos.camera.ui.theme.LampAmber
import com.uvos.camera.ui.theme.LampGreen
import com.uvos.camera.ui.theme.PanelDark
import com.uvos.camera.ui.theme.ReticleCyan
import com.uvos.camera.ui.theme.TextIvory
import com.uvos.camera.ui.theme.TextMuted
import com.uvos.camera.ui.theme.UvosCameraTheme

class CameraActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()
    private var hasCameraPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            UvosCameraTheme {
                if (hasCameraPermission) {
                    CameraScreen(
                        viewModel = viewModel,
                        onOpenGallery = { openGallery() }
                    )
                } else {
                    PermissionRequestScreen {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }
        }
    }

    private fun openGallery() {
        val galleryIntent = packageManager.getLaunchIntentForPackage("com.uvos.gallery")
        if (galleryIntent != null) {
            startActivity(galleryIntent)
        } else {
            val fallback = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
            }
            try {
                startActivity(fallback)
            } catch (_: Exception) {
                Toast.makeText(this, "Галерея недоступна", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onOpenGallery: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(uiState.statusNotification) {
        uiState.statusNotification?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearNotification()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // 1. CameraX Preview Layer
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { pView ->
                    previewViewRef = pView
                    viewModel.cameraManager.bindCamera(
                        lifecycleOwner = ctx as ComponentActivity,
                        previewView = pView
                    )
                }
            }
        )

        // 2. Oscilloscope / Instrument Reticle Overlay
        OscilloscopeGridOverlay()

        // 3. Top Instrument Header Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusLamp(
                    text = "EXIF PURGE: ON",
                    lampColor = LampGreen,
                    isActive = true
                )

                val flashLabel = when (uiState.flashMode) {
                    ImageCapture.FLASH_MODE_AUTO -> "FLASH: AUTO"
                    ImageCapture.FLASH_MODE_ON -> "FLASH: ON"
                    else -> "FLASH: OFF"
                }

                StatusLamp(
                    text = flashLabel,
                    lampColor = LampAmber,
                    isActive = uiState.flashMode != ImageCapture.FLASH_MODE_OFF,
                    modifier = Modifier.clickable { viewModel.toggleFlash() }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-status telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "OPTICAL SENSOR // CH-04",
                    color = ReticleCyan.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "ZERO AUDIO LOGGING",
                    color = AccentRed.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 4. Bottom Control Instrument Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .background(PanelDark.copy(alpha = 0.90f))
                .border(1.dp, Color(0xFF2E3240))
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Lens flip toggle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BgDark)
                        .border(1.dp, Color(0xFF3E4354), CircleShape)
                        .clickable {
                            val activity = context as? ComponentActivity
                            previewViewRef?.let { pView ->
                                if (activity != null) {
                                    viewModel.cameraManager.toggleLens(activity, pView)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FLIP",
                        color = TextIvory,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Center: Big Knurled Mechanical Shutter
                ShutterControl(
                    onClick = { viewModel.capturePhoto() }
                )

                // Right: Open Gallery Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgDark)
                        .border(1.dp, Color(0xFF3E4354), RoundedCornerShape(6.dp))
                        .clickable { onOpenGallery() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "АРХИВ",
                        color = TextIvory,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(onGrantClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ВИЗОР UVOS M4200",
                color = TextIvory,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Требуется прямое подключение к оптическому модулю устройства. Запись звука и гео-телеметрия аппаратно заблокированы.",
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "АКТИВИРОВАТЬ МОДУЛЬ",
                    color = TextIvory,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
