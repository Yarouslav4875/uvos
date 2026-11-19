package com.uvos.dashboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.view.View
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

/**
 * WallpaperManager - Управление обоями и фоновым слоем UnderVoltOS (UVOS).
 * Реализует:
 * 1. 3 системных процедурных пресета (Тактическая сетка, Карбон M4200, Радар-сталь) + сплошной AMOLED.
 * 2. Пользовательское фото через SAF (Storage Access Framework) с сохранением в локальное хранилище.
 * 3. Контроллер уровня затемнения (Dimming Level) в диапазоне 0-80%.
 * 4. Мгновенное применение к фону рабочего стола без перезапуска активности.
 */
class WallpaperManager(private val context: Context) {

    enum class WallpaperMode {
        SOLID_AMOLED,
        PRESET_TACTICAL_GRID,
        PRESET_CARBON_FIBER,
        PRESET_STEEL_RADAR,
        CUSTOM_PHOTO
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("uvos_wallpaper_prefs", Context.MODE_PRIVATE)
    private val keyMode = "wallpaper_mode"
    private val keyDimLevel = "wallpaper_dim_level"
    private val customFileName = "uvos_custom_wallpaper.png"

    private var cachedPresetBitmap: Bitmap? = null
    private var cachedPresetMode: WallpaperMode? = null
    private var cachedCustomBitmap: Bitmap? = null

    var onWallpaperChanged: (() -> Unit)? = null

    fun getWallpaperMode(): WallpaperMode {
        val name = prefs.getString(keyMode, WallpaperMode.PRESET_TACTICAL_GRID.name) ?: WallpaperMode.PRESET_TACTICAL_GRID.name
        return try {
            WallpaperMode.valueOf(name)
        } catch (_: Exception) {
            WallpaperMode.PRESET_TACTICAL_GRID
        }
    }

    fun setWallpaperMode(mode: WallpaperMode) {
        prefs.edit().putString(keyMode, mode.name).apply()
        onWallpaperChanged?.invoke()
    }

    fun getDimLevel(): Int {
        // По умолчанию 20% затемнения для читаемости шрифтов
        val level = prefs.getInt(keyDimLevel, 20)
        return level.coerceIn(0, 80)
    }

    fun setDimLevel(level: Int) {
        val clamped = level.coerceIn(0, 80)
        prefs.edit().putInt(keyDimLevel, clamped).apply()
        onWallpaperChanged?.invoke()
    }

    fun hasCustomWallpaper(): Boolean {
        val file = File(context.filesDir, customFileName)
        return file.exists() && file.length() > 0
    }

    fun saveCustomWallpaper(uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val tempFile = File(context.cacheDir, "temp_wall_saf.tmp")
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }

                // Декодируем с ограничением размера до 1080x2400 для экономии памяти
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(tempFile.absolutePath, options)

                val reqWidth = 1080
                val reqHeight = 2400
                var inSampleSize = 1
                if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                        inSampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath, decodeOptions)
                tempFile.delete()

                if (bitmap != null) {
                    val targetFile = File(context.filesDir, customFileName)
                    FileOutputStream(targetFile).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
                    }
                    cachedCustomBitmap?.recycle()
                    cachedCustomBitmap = bitmap
                    setWallpaperMode(WallpaperMode.CUSTOM_PHOTO)
                    true
                } else {
                    false
                }
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun getWallpaperBitmap(targetWidth: Int = 1080, targetHeight: Int = 2400): Bitmap? {
        val mode = getWallpaperMode()
        return when (mode) {
            WallpaperMode.SOLID_AMOLED -> null
            WallpaperMode.CUSTOM_PHOTO -> {
                if (cachedCustomBitmap == null || cachedCustomBitmap?.isRecycled == true) {
                    val file = File(context.filesDir, customFileName)
                    if (file.exists()) {
                        cachedCustomBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    }
                }
                cachedCustomBitmap
            }
            WallpaperMode.PRESET_TACTICAL_GRID -> {
                if (cachedPresetBitmap == null || cachedPresetMode != mode || cachedPresetBitmap?.isRecycled == true) {
                    cachedPresetBitmap?.recycle()
                    cachedPresetBitmap = createTacticalGridBitmap(targetWidth, targetHeight)
                    cachedPresetMode = mode
                }
                cachedPresetBitmap
            }
            WallpaperMode.PRESET_CARBON_FIBER -> {
                if (cachedPresetBitmap == null || cachedPresetMode != mode || cachedPresetBitmap?.isRecycled == true) {
                    cachedPresetBitmap?.recycle()
                    cachedPresetBitmap = createCarbonFiberBitmap(targetWidth, targetHeight)
                    cachedPresetMode = mode
                }
                cachedPresetBitmap
            }
            WallpaperMode.PRESET_STEEL_RADAR -> {
                if (cachedPresetBitmap == null || cachedPresetMode != mode || cachedPresetBitmap?.isRecycled == true) {
                    cachedPresetBitmap?.recycle()
                    cachedPresetBitmap = createSteelRadarBitmap(targetWidth, targetHeight)
                    cachedPresetMode = mode
                }
                cachedPresetBitmap
            }
        }
    }

    fun applyTo(wallpaperImageView: ImageView?, dimOverlayView: View?, containerView: View?) {
        val mode = getWallpaperMode()
        val dimPercent = getDimLevel()

        if (wallpaperImageView == null || dimOverlayView == null) return

        if (mode == WallpaperMode.SOLID_AMOLED) {
            wallpaperImageView.setImageDrawable(null)
            wallpaperImageView.visibility = View.GONE
            dimOverlayView.visibility = View.GONE
            containerView?.setBackgroundColor(Color.parseColor("#040406"))
        } else {
            val bitmap = getWallpaperBitmap()
            if (bitmap != null) {
                wallpaperImageView.setImageBitmap(bitmap)
                wallpaperImageView.visibility = View.VISIBLE
                containerView?.setBackgroundColor(Color.TRANSPARENT)
            } else {
                wallpaperImageView.visibility = View.GONE
                containerView?.setBackgroundColor(Color.parseColor("#040406"))
            }

            // Уровень затемнения (0-80%)
            dimOverlayView.visibility = View.VISIBLE
            dimOverlayView.setBackgroundColor(Color.BLACK)
            dimOverlayView.alpha = dimPercent / 100f
        }
    }

    // ==========================================
    // ПРОЦЕДУРНЫЕ ПРЕСЕТЫ СИСТЕМЫ UVOS (120HZ COMPLIANT)
    // ==========================================

    private fun createTacticalGridBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Глубокий тёмно-графитовый фон
        canvas.drawColor(Color.parseColor("#07080D"))

        val paintMinor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#101724")
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
        }
        val paintMajor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B273A")
            strokeWidth = 2.0f
            style = Paint.Style.STROKE
        }
        val paintCross = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E02020")
            strokeWidth = 2.0f
            alpha = 80
            style = Paint.Style.STROKE
        }

        val step = 48
        val majorStep = step * 5

        // Тонкая сетка
        var x = 0f
        while (x <= width) {
            val p = if ((x.toInt() % majorStep) == 0) paintMajor else paintMinor
            canvas.drawLine(x, 0f, x, height.toFloat(), p)
            x += step
        }

        var y = 0f
        while (y <= height) {
            val p = if ((y.toInt() % majorStep) == 0) paintMajor else paintMinor
            canvas.drawLine(0f, y, width.toFloat(), y, p)
            y += step
        }

        // Тактические перекрестия на ключевых узлах
        x = majorStep.toFloat()
        while (x < width) {
            y = majorStep.toFloat()
            while (y < height) {
                val s = 10f
                canvas.drawLine(x - s, y, x + s, y, paintCross)
                canvas.drawLine(x, y - s, x, y + s, paintCross)
                y += majorStep
            }
            x += majorStep
        }

        return bitmap
    }

    private fun createCarbonFiberBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Базовый титановый тёмный фон
        canvas.drawColor(Color.parseColor("#0A0B10"))

        val p1 = Paint().apply { color = Color.parseColor("#12141C") }
        val p2 = Paint().apply { color = Color.parseColor("#1A1D27") }
        val p3 = Paint().apply { color = Color.parseColor("#0F1117") }

        val tileSize = 24f
        var y = 0f
        var row = 0
        while (y < height) {
            var x = 0f
            var col = 0
            while (x < width) {
                val paint = when ((row + col) % 3) {
                    0 -> p1
                    1 -> p2
                    else -> p3
                }
                canvas.drawRect(x, y, x + tileSize, y + tileSize, paint)
                x += tileSize
                col++
            }
            y += tileSize
            row++
        }

        // Диагональные световые волокна
        val fiberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#222735")
            strokeWidth = 1.0f
            alpha = 70
        }
        var d = -height.toFloat()
        while (d < width + height) {
            canvas.drawLine(d, 0f, d + height, height.toFloat(), fiberPaint)
            d += 36f
        }

        return bitmap
    }

    private fun createSteelRadarBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Глубокий радарный фон
        canvas.drawColor(Color.parseColor("#080B12"))

        val centerX = width / 2f
        val centerY = height * 0.45f

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#142236")
            strokeWidth = 1.8f
            style = Paint.Style.STROKE
        }
        val mainAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F3554")
            strokeWidth = 2.0f
            style = Paint.Style.STROKE
        }
        val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E02020")
            style = Paint.Style.FILL
        }

        // Концентрические кольца дальности
        val maxRadius = width * 0.46f
        val ringCount = 5
        for (i in 1..ringCount) {
            val r = (maxRadius / ringCount) * i
            canvas.drawCircle(centerX, centerY, r, ringPaint)
        }

        // Основные оси координат
        canvas.drawLine(centerX - maxRadius - 20, centerY, centerX + maxRadius + 20, centerY, mainAxisPaint)
        canvas.drawLine(centerX, centerY - maxRadius - 20, centerX, centerY + maxRadius + 20, mainAxisPaint)

        // Азимутальные лучи (45 и 135 градусов)
        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#121D2C")
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
        }
        for (angle in listOf(45.0, 135.0, 225.0, 315.0)) {
            val rad = Math.toRadians(angle)
            val endX = centerX + (maxRadius * cos(rad)).toFloat()
            val endY = centerY + (maxRadius * sin(rad)).toFloat()
            canvas.drawLine(centerX, centerY, endX, endY, rayPaint)
        }

        // Центр радара
        canvas.drawCircle(centerX, centerY, 4f, centerDotPaint)

        return bitmap
    }
}
