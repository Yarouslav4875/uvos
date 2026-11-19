package com.uvos.dashboard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * UVOS Dark Glassmorphism Tile Drawable.
 *
 * Implements the signature UnderVoltOS high-contrast dark frosted glass aesthetic:
 * 1. Base Layer: Multi-stop deep graphite frosted glass body (#1A1C24 -> #0E0E12)
 * 2. Specular Rim: Ultra-sharp directional metallic bevel (top-left light sheen -> bottom-right deep shadow)
 * 3. Neon Core: Ambient underglow and rim highlights in vibrant Crimson Neon (#FF3B30 / #E02020)
 * 4. Micro-shelf: Inner light dispersion shelf for physical glass thickness
 * 5. State-aware: Interactive touch feedback (pressed, focused, normal)
 */
class GlassTileDrawable(
    private var cornerRadiusDp: Float = 18f,
    private var hasNeonGlow: Boolean = true,
    private var neonColor: Int = Color.parseColor("#FF3B30")
) : Drawable() {

    private val boundsF = RectF()
    private val innerBoundsF = RectF()
    private var density: Float = 1f

    // --- Paint instances (Allocated once for 120 FPS zero-allocation rendering) ---
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val specularBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val neonGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val shelfHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private var isPressedState = false
    private var isFocusedState = false

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        if (bounds.isEmpty) return

        density = if (bounds.width() > 0) bounds.width() / 64f else 2.5f
        val strokeWidth = 1.35f * density
        val halfStroke = strokeWidth / 2f

        boundsF.set(bounds)
        innerBoundsF.set(
            bounds.left + halfStroke,
            bounds.top + halfStroke,
            bounds.right - halfStroke,
            bounds.bottom - halfStroke
        )

        val w = boundsF.width()
        val h = boundsF.height()

        // 1. Frosted Glass Body Gradient (Strict reference standard: rgba(26,28,36,0.70))
        val topGlass = if (isPressedState) Color.argb(210, 26, 28, 36) else Color.argb(178, 26, 28, 36)
        val midGlass = if (isPressedState) Color.argb(225, 20, 22, 29) else Color.argb(195, 20, 22, 29)
        val botGlass = if (isPressedState) Color.argb(245, 14, 15, 20) else Color.argb(220, 14, 15, 20)

        glassPaint.shader = LinearGradient(
            boundsF.left, boundsF.top,
            boundsF.right, boundsF.bottom,
            intArrayOf(topGlass, midGlass, botGlass),
            floatArrayOf(0.0f, 0.45f, 1.0f),
            Shader.TileMode.CLAMP
        )

        // 2. Specular Bevel / Rim Gradient (Top-Left mirror reflection to Bottom-Right shadow)
        val rimHighlight = if (isPressedState) Color.argb(110, 255, 255, 255) else Color.argb(90, 255, 255, 255)
        val rimMid = Color.argb(30, 255, 255, 255)
        val rimShadow = Color.argb(120, 0, 0, 0)

        specularBorderPaint.strokeWidth = strokeWidth
        specularBorderPaint.shader = LinearGradient(
            boundsF.left, boundsF.top,
            boundsF.right, boundsF.bottom,
            intArrayOf(rimHighlight, rimMid, rimShadow),
            floatArrayOf(0.0f, 0.35f, 1.0f),
            Shader.TileMode.CLAMP
        )

        // 3. Crimson Neon Accent Shader
        val neonAlpha = if (isPressedState) 220 else (if (hasNeonGlow) 130 else 0)
        val neonColorWithAlpha = Color.argb(
            neonAlpha,
            Color.red(neonColor),
            Color.green(neonColor),
            Color.blue(neonColor)
        )
        val neonFade = Color.argb(0, Color.red(neonColor), Color.green(neonColor), Color.blue(neonColor))

        neonGlowPaint.strokeWidth = 2.4f * density
        neonGlowPaint.shader = RadialGradient(
            boundsF.centerX(), boundsF.bottom,
            w * 0.75f,
            intArrayOf(neonColorWithAlpha, neonFade),
            floatArrayOf(0.0f, 1.0f),
            Shader.TileMode.CLAMP
        )

        // 4. Glass Shelf Inner Highlight (Thin light catching shelf across the upper 20%)
        shelfHighlightPaint.strokeWidth = 1.0f * density
        shelfHighlightPaint.shader = LinearGradient(
            boundsF.left + w * 0.15f, boundsF.top + 3f * density,
            boundsF.right - w * 0.15f, boundsF.top + 3f * density,
            intArrayOf(Color.TRANSPARENT, Color.argb(60, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0.0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )
    }

    override fun draw(canvas: Canvas) {
        val cr = cornerRadiusDp * density

        // A. Draw Frosted Glass Body
        canvas.drawRoundRect(boundsF, cr, cr, glassPaint)

        // B. Draw Neon Ambient Rim Glow (Bottom / Edge)
        if (hasNeonGlow || isPressedState) {
            canvas.drawRoundRect(innerBoundsF, cr, cr, neonGlowPaint)
        }

        // C. Draw Specular Outer Bevel
        canvas.drawRoundRect(innerBoundsF, cr, cr, specularBorderPaint)

        // D. Draw Subtle Physical Glass Shelf
        val shelfY = boundsF.top + 3f * density
        val shelfMargin = boundsF.width() * 0.18f
        canvas.drawLine(
            boundsF.left + shelfMargin, shelfY,
            boundsF.right - shelfMargin, shelfY,
            shelfHighlightPaint
        )
    }

    override fun isStateful(): Boolean = true

    override fun onStateChange(state: IntArray): Boolean {
        var pressed = false
        var focused = false
        for (s in state) {
            if (s == android.R.attr.state_pressed) pressed = true
            if (s == android.R.attr.state_focused || s == android.R.attr.state_selected) focused = true
        }

        if (pressed != isPressedState || focused != isFocusedState) {
            isPressedState = pressed
            isFocusedState = focused
            onBoundsChange(bounds)
            invalidateSelf()
            return true
        }
        return false
    }

    fun setCornerRadius(radiusDp: Float) {
        cornerRadiusDp = radiusDp
        invalidateSelf()
    }

    fun setNeonColor(color: Int) {
        neonColor = color
        onBoundsChange(bounds)
        invalidateSelf()
    }

    fun setNeonEnabled(enabled: Boolean) {
        hasNeonGlow = enabled
        onBoundsChange(bounds)
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        glassPaint.alpha = alpha
        specularBorderPaint.alpha = alpha
        neonGlowPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        glassPaint.colorFilter = colorFilter
        specularBorderPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        /**
         * Генератор стеклянных иконок: композиция подложки GlassTileDrawable и векторной иконки
         */
        fun createGlassTileBitmap(
            sizePx: Int = 192,
            cornerRadiusDp: Float = 18f,
            hasNeonGlow: Boolean = true,
            neonColor: Int = Color.parseColor("#FF3B30"),
            foregroundIcon: Drawable? = null
        ): Bitmap {
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val tile = GlassTileDrawable(
                cornerRadiusDp = cornerRadiusDp,
                hasNeonGlow = hasNeonGlow,
                neonColor = neonColor
            )
            tile.setBounds(0, 0, sizePx, sizePx)
            tile.draw(canvas)

            if (foregroundIcon != null) {
                val padding = (sizePx * 0.22f).toInt()
                foregroundIcon.setBounds(padding, padding, sizePx - padding, sizePx - padding)
                foregroundIcon.draw(canvas)
            }

            return bitmap
        }
    }
}
