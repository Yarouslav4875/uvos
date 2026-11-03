package com.uvos.music.core.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Studio Custom Vector Icons to avoid 35MB external icon library dependency.
 */
object StudioIcons {

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "StudioPause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 19f)
                horizontalLineTo(10f)
                verticalLineTo(5f)
                horizontalLineTo(6f)
                verticalLineTo(19f)
                close()
                moveTo(14f, 5f)
                verticalLineTo(19f)
                horizontalLineTo(18f)
                verticalLineTo(5f)
                horizontalLineTo(14f)
                close()
            }
        }.build()
    }

    val SkipNext: ImageVector by lazy {
        ImageVector.Builder(
            name = "StudioSkipNext",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 18f)
                lineTo(14.5f, 12f)
                lineTo(6f, 6f)
                verticalLineTo(18f)
                close()
                moveTo(16f, 6f)
                verticalLineTo(18f)
                horizontalLineTo(18f)
                verticalLineTo(6f)
                horizontalLineTo(16f)
                close()
            }
        }.build()
    }

    val SkipPrevious: ImageVector by lazy {
        ImageVector.Builder(
            name = "StudioSkipPrevious",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 6f)
                horizontalLineTo(8f)
                verticalLineTo(18f)
                horizontalLineTo(6f)
                verticalLineTo(6f)
                close()
                moveTo(9.5f, 12f)
                lineTo(18f, 18f)
                verticalLineTo(6f)
                lineTo(9.5f, 12f)
                close()
            }
        }.build()
    }

    val Shuffle: ImageVector by lazy {
        ImageVector.Builder(
            name = "StudioShuffle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(10.59f, 9.17f)
                lineTo(5.41f, 4f)
                lineTo(4f, 5.41f)
                lineTo(9.17f, 10.59f)
                lineTo(10.59f, 9.17f)
                close()
                moveTo(14.5f, 4f)
                lineTo(16.54f, 6.04f)
                lineTo(4f, 18.59f)
                lineTo(5.41f, 20f)
                lineTo(17.96f, 7.46f)
                lineTo(20f, 9.5f)
                verticalLineTo(4f)
                horizontalLineTo(14.5f)
                close()
                moveTo(14.83f, 13.41f)
                lineTo(13.42f, 14.82f)
                lineTo(16.55f, 17.95f)
                lineTo(14.5f, 20f)
                horizontalLineTo(20f)
                verticalLineTo(14.5f)
                lineTo(17.96f, 16.54f)
                lineTo(14.83f, 13.41f)
                close()
            }
        }.build()
    }

    val Repeat: ImageVector by lazy {
        ImageVector.Builder(
            name = "StudioRepeat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7f, 7f)
                horizontalLineTo(17f)
                verticalLineTo(10f)
                lineTo(21f, 6f)
                lineTo(17f, 2f)
                verticalLineTo(5f)
                horizontalLineTo(5f)
                verticalLineTo(11f)
                horizontalLineTo(7f)
                verticalLineTo(7f)
                close()
                moveTo(17f, 17f)
                horizontalLineTo(7f)
                verticalLineTo(14f)
                lineTo(3f, 18f)
                lineTo(7f, 22f)
                verticalLineTo(19f)
                horizontalLineTo(19f)
                verticalLineTo(13f)
                horizontalLineTo(17f)
                verticalLineTo(17f)
                close()
            }
        }.build()
    }

    val RepeatOne: ImageVector by lazy {
        ImageVector.Builder(
            name = "StudioRepeatOne",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7f, 7f)
                horizontalLineTo(17f)
                verticalLineTo(10f)
                lineTo(21f, 6f)
                lineTo(17f, 2f)
                verticalLineTo(5f)
                horizontalLineTo(5f)
                verticalLineTo(11f)
                horizontalLineTo(7f)
                verticalLineTo(7f)
                close()
                moveTo(17f, 17f)
                horizontalLineTo(7f)
                verticalLineTo(14f)
                lineTo(3f, 18f)
                lineTo(7f, 22f)
                verticalLineTo(19f)
                horizontalLineTo(19f)
                verticalLineTo(13f)
                horizontalLineTo(17f)
                verticalLineTo(17f)
                close()
                moveTo(13f, 15f)
                verticalLineTo(9f)
                horizontalLineTo(12f)
                lineTo(10f, 10f)
                verticalLineTo(11f)
                horizontalLineTo(11.5f)
                verticalLineTo(15f)
                horizontalLineTo(13f)
                close()
            }
        }.build()
    }

    val Equalizer: ImageVector by lazy {
        ImageVector.Builder(
            name = "StudioEqualizer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(10f, 20f)
                horizontalLineTo(14f)
                verticalLineTo(4f)
                horizontalLineTo(10f)
                verticalLineTo(20f)
                close()
                moveTo(4f, 20f)
                horizontalLineTo(8f)
                verticalLineTo(12f)
                horizontalLineTo(4f)
                verticalLineTo(20f)
                close()
                moveTo(16f, 9f)
                verticalLineTo(20f)
                horizontalLineTo(20f)
                verticalLineTo(9f)
                horizontalLineTo(16f)
                close()
            }
        }.build()
    }

    val QueueMusic: ImageVector by lazy {
        ImageVector.Builder(
            name = "StudioQueueMusic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(15f, 6f)
                horizontalLineTo(3f)
                verticalLineTo(8f)
                horizontalLineTo(15f)
                verticalLineTo(6f)
                close()
                moveTo(15f, 10f)
                horizontalLineTo(3f)
                verticalLineTo(12f)
                horizontalLineTo(15f)
                verticalLineTo(10f)
                close()
                moveTo(3f, 16f)
                horizontalLineTo(11f)
                verticalLineTo(14f)
                horizontalLineTo(3f)
                verticalLineTo(16f)
                close()
                moveTo(17f, 6f)
                verticalLineTo(14.18f)
                curveTo(16.69f, 14.07f, 16.35f, 14f, 16f, 14f)
                curveTo(14.34f, 14f, 13f, 15.34f, 13f, 17f)
                curveTo(13f, 18.66f, 14.34f, 20f, 16f, 20f)
                curveTo(17.66f, 20f, 19f, 18.66f, 19f, 17f)
                verticalLineTo(8f)
                horizontalLineTo(22f)
                verticalLineTo(6f)
                horizontalLineTo(17f)
                close()
            }
        }.build()
    }

    val RestartAlt: ImageVector by lazy {
        ImageVector.Builder(
            name = "StudioRestartAlt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 5f)
                verticalLineTo(1f)
                lineTo(7f, 6f)
                lineTo(12f, 11f)
                verticalLineTo(7f)
                curveTo(15.31f, 7f, 18f, 9.69f, 18f, 13f)
                curveTo(18f, 16.31f, 15.31f, 19f, 12f, 19f)
                curveTo(8.69f, 19f, 6f, 16.31f, 6f, 13f)
                horizontalLineTo(4f)
                curveTo(4f, 17.42f, 7.58f, 21f, 12f, 21f)
                curveTo(16.42f, 21f, 20f, 17.42f, 20f, 13f)
                curveTo(20f, 8.58f, 16.42f, 5f, 12f, 5f)
                close()
            }
        }.build()
    }

    val GraphicEq: ImageVector by lazy {
        ImageVector.Builder(
            name = "StudioGraphicEq",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7f, 18f)
                horizontalLineTo(9f)
                verticalLineTo(6f)
                horizontalLineTo(7f)
                verticalLineTo(18f)
                close()
                moveTo(11f, 22f)
                horizontalLineTo(13f)
                verticalLineTo(2f)
                horizontalLineTo(11f)
                verticalLineTo(22f)
                close()
                moveTo(3f, 14f)
                horizontalLineTo(5f)
                verticalLineTo(10f)
                horizontalLineTo(3f)
                verticalLineTo(14f)
                close()
                moveTo(15f, 18f)
                horizontalLineTo(17f)
                verticalLineTo(6f)
                horizontalLineTo(15f)
                verticalLineTo(18f)
                close()
                moveTo(19f, 10f)
                verticalLineTo(14f)
                horizontalLineTo(21f)
                verticalLineTo(10f)
                horizontalLineTo(19f)
                close()
            }
        }.build()
    }
}
