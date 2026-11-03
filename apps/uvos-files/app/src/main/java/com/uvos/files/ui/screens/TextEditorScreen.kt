package com.uvos.files.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.files.ui.theme.IndustrialAccent
import com.uvos.files.ui.theme.IndustrialTextPrimary
import com.uvos.files.ui.theme.IndustrialTextSecondary
import com.uvos.files.ui.theme.LampGreen
import com.uvos.files.ui.theme.UvosBg
import com.uvos.files.ui.theme.UvosCard
import com.uvos.files.ui.theme.UvosPanel
import com.uvos.files.ui.viewmodel.TextEditorViewModel

@Composable
fun TextEditorScreen(
    viewModel: TextEditorViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UvosBg)
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(UvosPanel, RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", tint = IndustrialTextPrimary)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.fileItem?.name ?: "Config Editor",
                    color = IndustrialTextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.lineCount} lines | ${state.metadata?.permissions?.toFormattedString() ?: "0644"} ${state.metadata?.owner ?: "root"}",
                    color = IndustrialTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = { viewModel.saveContent() },
                enabled = state.isModified && !state.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IndustrialAccent,
                    disabledContainerColor = UvosCard
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (state.isModified) "Save *" else "Saved", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Editor area with line numbers
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(UvosPanel, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = IndustrialAccent
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // Line numbers column
                    val lineNumbersText = (1..state.lineCount).joinToString("\n")
                    Text(
                        text = lineNumbersText,
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .background(UvosCard.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Text field
                    BasicTextField(
                        value = state.content,
                        onValueChange = { viewModel.updateContent(it) },
                        textStyle = TextStyle(
                            color = IndustrialTextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        ),
                        cursorBrush = SolidColor(IndustrialAccent),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Status bar
        state.statusMessage?.let {
            Text(
                text = it,
                color = LampGreen,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        state.errorMessage?.let {
            Text(
                text = it,
                color = IndustrialAccent,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
