package com.uvos.notes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.notes.ui.theme.UVOSAmber
import com.uvos.notes.ui.theme.UVOSBorder
import com.uvos.notes.ui.theme.UVOSCyan
import com.uvos.notes.ui.theme.UVOSGreen
import com.uvos.notes.ui.theme.UVOSPanel
import com.uvos.notes.ui.theme.UVOSRed
import com.uvos.notes.ui.theme.UVOSTextMuted
import com.uvos.notes.ui.theme.UVOSTextPrimary
import com.uvos.notes.ui.theme.UVOSTextSecondary

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class CheckListItem(val lineIndex: Int, val isChecked: Boolean, val text: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

object MarkdownParser {

    fun parse(markdown: String): List<MarkdownBlock> {
        val lines = markdown.lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var inCodeBlock = false
        var codeLanguage = ""
        val codeBuilder = StringBuilder()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    blocks.add(MarkdownBlock.CodeBlock(codeLanguage, codeBuilder.toString().trimEnd()))
                    codeBuilder.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                    codeLanguage = line.trim().removePrefix("```").trim()
                }
                i++
                continue
            }

            if (inCodeBlock) {
                if (codeBuilder.isNotEmpty()) codeBuilder.append("\n")
                codeBuilder.append(line)
                i++
                continue
            }

            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> {
                    // Blank lines separate paragraphs
                }
                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    blocks.add(MarkdownBlock.Divider)
                }
                trimmed.startsWith("#### ") -> {
                    blocks.add(MarkdownBlock.Header(4, trimmed.removePrefix("#### ").trim()))
                }
                trimmed.startsWith("### ") -> {
                    blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### ").trim()))
                }
                trimmed.startsWith("## ") -> {
                    blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## ").trim()))
                }
                trimmed.startsWith("# ") -> {
                    blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# ").trim()))
                }
                trimmed.startsWith("> ") -> {
                    blocks.add(MarkdownBlock.Blockquote(trimmed.removePrefix("> ").trim()))
                }
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                    val text = trimmed.substring(6).trim()
                    blocks.add(MarkdownBlock.CheckListItem(i, false, text))
                }
                trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ||
                trimmed.startsWith("* [x] ") || trimmed.startsWith("* [X] ") -> {
                    val text = trimmed.substring(6).trim()
                    blocks.add(MarkdownBlock.CheckListItem(i, true, text))
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    blocks.add(MarkdownBlock.BulletItem(trimmed.substring(2).trim()))
                }
                Regex("^\\d+\\.\\s+.*").matches(trimmed) -> {
                    val dotIndex = trimmed.indexOf('.')
                    val num = trimmed.substring(0, dotIndex)
                    val text = trimmed.substring(dotIndex + 1).trim()
                    blocks.add(MarkdownBlock.NumberedItem(num, text))
                }
                else -> {
                    blocks.add(MarkdownBlock.Paragraph(line))
                }
            }
            i++
        }

        if (inCodeBlock) {
            blocks.add(MarkdownBlock.CodeBlock(codeLanguage, codeBuilder.toString().trimEnd()))
        }

        return blocks
    }
}

@Composable
fun MarkwonRenderer(
    markdown: String,
    modifier: Modifier = Modifier,
    onToggleChecklist: ((lineIndex: Int, newChecked: Boolean) -> Unit)? = null
) {
    val blocks = MarkdownParser.parse(markdown)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (blocks.isEmpty()) {
            Text(
                text = "(Пустая заметка)",
                color = UVOSTextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp)
            )
        }

        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Header -> {
                    RenderHeader(block)
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(block.text),
                        fontSize = 15.sp,
                        color = UVOSTextPrimary,
                        lineHeight = 22.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    RenderCodeBlock(block)
                }
                is MarkdownBlock.Blockquote -> {
                    RenderBlockquote(block)
                }
                is MarkdownBlock.CheckListItem -> {
                    RenderCheckListItem(block, onToggleChecklist)
                }
                is MarkdownBlock.BulletItem -> {
                    RenderBulletItem(block)
                }
                is MarkdownBlock.NumberedItem -> {
                    RenderNumberedItem(block)
                }
                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = UVOSBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun RenderHeader(header: MarkdownBlock.Header) {
    val (size, color, weight) = when (header.level) {
        1 -> Triple(22.sp, UVOSAmber, FontWeight.Bold)
        2 -> Triple(19.sp, UVOSCyan, FontWeight.Bold)
        3 -> Triple(17.sp, UVOSGreen, FontWeight.SemiBold)
        else -> Triple(15.sp, UVOSTextPrimary, FontWeight.SemiBold)
    }

    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = formatInlineMarkdown(header.text),
            fontSize = size,
            color = color,
            fontWeight = weight,
            fontFamily = FontFamily.Monospace
        )
        if (header.level <= 2) {
            HorizontalDivider(
                color = color.copy(alpha = 0.4f),
                thickness = if (header.level == 1) 2.dp else 1.dp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun RenderCodeBlock(block: MarkdownBlock.CodeBlock) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF13141B))
            .border(1.dp, Color(0xFF282B3B), RoundedCornerShape(6.dp))
    ) {
        if (block.language.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1D28))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = block.language.uppercase(),
                    color = UVOSAmber,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "M4200 SYNTAX",
                    color = UVOSTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = block.code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Color(0xFF80CBC4),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun RenderBlockquote(block: MarkdownBlock.Blockquote) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(28.dp)
                .background(UVOSAmber, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = formatInlineMarkdown(block.text),
            color = UVOSTextSecondary,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun RenderCheckListItem(
    item: MarkdownBlock.CheckListItem,
    onToggleChecklist: ((lineIndex: Int, newChecked: Boolean) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onToggleChecklist != null) {
                onToggleChecklist?.invoke(item.lineIndex, !item.isChecked)
            }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = if (item.isChecked) "Done" else "Pending",
            tint = if (item.isChecked) UVOSGreen else UVOSTextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = formatInlineMarkdown(item.text),
            color = if (item.isChecked) UVOSTextMuted else UVOSTextPrimary,
            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun RenderBulletItem(item: MarkdownBlock.BulletItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = UVOSAmber,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Text(
            text = formatInlineMarkdown(item.text),
            color = UVOSTextPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun RenderNumberedItem(item: MarkdownBlock.NumberedItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${item.number}.",
            color = UVOSCyan,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = formatInlineMarkdown(item.text),
            color = UVOSTextPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

private fun formatInlineMarkdown(text: String) = buildAnnotatedString {
    var i = 0
    while (i < text.size()) {
        if (i + 1 < text.size() && text[i] == '*' && text[i + 1] == '*') {
            val end = text.indexOf("**", i + 2)
            if (end != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = UVOSTextPrimary)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }
        if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end != -1) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0xFF202330),
                        color = Color(0xFFFFCC80)
                    )
                ) {
                    append(" ${text.substring(i + 1, end)} ")
                }
                i = end + 1
                continue
            }
        }
        if (text[i] == '*' || text[i] == '_') {
            val end = text.indexOf(text[i], i + 1)
            if (end != -1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        if (i + 1 < text.size() && text[i] == '~' && text[i + 1] == '~') {
            val end = text.indexOf("~~", i + 2)
            if (end != -1) {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = UVOSTextMuted)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }

        append(text[i])
        i++
    }
}

private fun String.size() = this.length
