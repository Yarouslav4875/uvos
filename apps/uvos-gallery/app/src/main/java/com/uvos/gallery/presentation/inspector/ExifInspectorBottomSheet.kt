package com.uvos.gallery.presentation.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.gallery.domain.model.ExifSecurityReport
import com.uvos.gallery.presentation.theme.UvosBackground
import com.uvos.gallery.presentation.theme.UvosDialIvory
import com.uvos.gallery.presentation.theme.UvosGreenSterile
import com.uvos.gallery.presentation.theme.UvosMuted
import com.uvos.gallery.presentation.theme.UvosRedDanger
import com.uvos.gallery.presentation.theme.UvosTileSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifInspectorBottomSheet(
    report: ExifSecurityReport,
    onScrubRequested: () -> Unit,
    onMoveToVaultRequested: () -> Unit,
    onShareCleanRequested: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = UvosTileSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = UvosDialIvory) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "ИНСПЕКТОР БЕЗОПАСНОСТИ UVOS",
                color = UvosDialIvory,
                fontSize = 17.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Text(
                text = "АНАЛИЗАТОР ЦИФРОВОГО ОТПЕЧАТКА M4200",
                color = UvosMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(16.dp))

            // GPS Status readout
            val gpsValue = if (report.hasGpsCoordinates) {
                if (report.latitude != null && report.longitude != null) {
                    "УТЕЧКА [${String.format("%.4f", report.latitude)}, ${String.format("%.4f", report.longitude)}]"
                } else {
                    "ОБНАРУЖЕНЫ (УТЕЧКА)"
                }
            } else {
                "ОТСУТСТВУЮТ (СТЕРИЛЬНО)"
            }

            StatusRow(
                label = "GPS КООРДИНАТЫ:",
                value = gpsValue,
                isDanger = report.hasGpsCoordinates
            )

            // Device fingerprint
            StatusRow(
                label = "ОТПЕЧАТОК КАМЕРЫ:",
                value = report.deviceModel ?: "ОЧИЩЕНО",
                isDanger = report.deviceModel != null
            )

            // Capture timestamp
            StatusRow(
                label = "ВРЕМЯ СЪЕМКИ:",
                value = report.timestamp ?: "НЕ ОПРЕДЕЛЕНО",
                isDanger = false
            )

            // Lens & hardware serials
            if (report.lensModel != null) {
                StatusRow(
                    label = "МОДУЛЬ ОБЪЕКТИВА:",
                    value = report.lensModel,
                    isDanger = true
                )
            }

            if (report.serialNumber != null) {
                StatusRow(
                    label = "СЕРИЙНЫЙ НОМЕР МАТРИЦЫ:",
                    value = report.serialNumber,
                    isDanger = true
                )
            }

            if (report.software != null) {
                StatusRow(
                    label = "ВЕРСИЯ ПО КАМЕРЫ:",
                    value = report.software,
                    isDanger = false
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action 1: In-place sanitize
            Button(
                onClick = onScrubRequested,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UvosGreenSterile)
            ) {
                Text(
                    text = "ОЧИСТИТЬ EXIF / GPS НА МЕСТЕ",
                    color = UvosBackground,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action 2: Share sanitized copy via Air-gapped FileProvider
            Button(
                onClick = onShareCleanRequested,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2D3A))
            ) {
                Text(
                    text = "БЕЗОПАСНЫЙ ЭКСПОРТ (SHARE CLEAN)",
                    color = UvosDialIvory,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action 3: Move to crypto vault
            OutlinedButton(
                onClick = onMoveToVaultRequested,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = UvosRedDanger)
            ) {
                Text(
                    text = "ПЕРЕМЕСТИТЬ В КРИПТО-СЕЙФ (AES-256)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, isDanger: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = UvosMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = if (isDanger) UvosRedDanger else UvosGreenSterile,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
