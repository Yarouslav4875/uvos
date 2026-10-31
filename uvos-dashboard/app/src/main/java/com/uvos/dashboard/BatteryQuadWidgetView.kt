package com.uvos.dashboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * UVOS M4200 Micro Edition 4x1 Battery & Peripheral Widget
 *
 * Полностью автоматический виджет:
 * - Слот 0: ХОСТ (Телефон), модель определяется автоматически через Build.MODEL.
 * - Слоты 1, 2, 3: Автоматическое подключение любых Bluetooth-устройств (наушники, часы, колонки, геймпады и др.).
 * - Без ручных кнопок: при обнаружении Bluetooth-устройства вольтметр плавно оживает, циферблат освещается,
 *   стрелка поднимается до реального уровня заряда.
 * - При отключении стрелка опускается на механический упор нуля (-52°), циферблат гаснет (#13151D), светодиод отключается.
 * - Автоопределение имён и векторных иконок для каждого типа устройств.
 */
class BatteryQuadWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    enum class DeviceType {
        PHONE, HEADPHONES, WATCH, SPEAKER, GAMEPAD, POWERBANK, GENERIC_BT, EMPTY_SLOT
    }

    data class BatterySlot(
        val slotIndex: Int,
        var deviceName: String,
        var percent: Int,
        var isConnected: Boolean,
        var subtitle: String,
        var deviceType: DeviceType,
        var currentNeedleAngle: Float = -52f,
        var targetNeedleAngle: Float = -52f,
        var animator: ValueAnimator? = null
    )

    private val hostModelName = Build.MODEL.take(13).uppercase()

    private val slots = mutableListOf(
        BatterySlot(
            slotIndex = 0,
            deviceName = if (hostModelName.isNotEmpty()) hostModelName else "ТЕЛЕФОН",
            percent = 84,
            isConnected = true,
            subtitle = "АКБ 4.12V",
            deviceType = DeviceType.PHONE
        ),
        BatterySlot(
            slotIndex = 1,
            deviceName = "СЛОТ 2",
            percent = 0,
            isConnected = false,
            subtitle = "НЕТ СВЯЗИ",
            deviceType = DeviceType.EMPTY_SLOT
        ),
        BatterySlot(
            slotIndex = 2,
            deviceName = "СЛОТ 3",
            percent = 0,
            isConnected = false,
            subtitle = "НЕТ СВЯЗИ",
            deviceType = DeviceType.EMPTY_SLOT
        ),
        BatterySlot(
            slotIndex = 3,
            deviceName = "СЛОТ 4",
            percent = 0,
            isConnected = false,
            subtitle = "НЕТ СВЯЗИ",
            deviceType = DeviceType.EMPTY_SLOT
        )
    )

    var onSlotClickListener: ((slotIndex: Int, slot: BatterySlot) -> Unit)? = null

    // --- Палитра цветов UVOS M4200 ---
    private val colorWidgetBorder = Color.parseColor("#242838")
    private val colorCaseBgTop = Color.parseColor("#151824")
    private val colorCaseBgBottom = Color.parseColor("#0D0E15")
    private val colorCaseBorder = Color.parseColor("#2F3345")
    private val colorDialOn = Color.parseColor("#E8E4D8")
    private val colorDialOff = Color.parseColor("#13151D")
    private val colorRedDanger = Color.parseColor("#E02020")
    private val colorRedDangerDim = Color.parseColor("#441111")
    private val colorLedAlert = Color.parseColor("#FF2222")
    private val colorLedDim = Color.parseColor("#7A1010")
    private val colorLedOff = Color.parseColor("#180808")
    private val colorInk = Color.parseColor("#1A1A1A")
    private val colorInkDim = Color.parseColor("#25293A")
    private val colorGreenActive = Color.parseColor("#44CC66")
    private val colorScrewHead = Color.parseColor("#5A5F72")
    private val colorScrewSlot = Color.parseColor("#101117")

    // --- Кисти рисования ---
    private val widgetBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val widgetBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorWidgetBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }
    private val slotCasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val slotCaseBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorCaseBorder
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    private val dialBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dialInnerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = Color.parseColor("#20000000")
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val screwPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorScrewHead
        style = Paint.Style.FILL
    }
    private val screwSlotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorScrewSlot
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    private val ledHousingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1C24")
        style = Paint.Style.FILL
    }
    private val ledCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val needleLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorRedDanger
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    private val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#252834")
        style = Paint.Style.FILL
    }
    private val pivotCapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val readoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }
    private val slotTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#9AA0B8")
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        letterSpacing = 0.04f
    }
    private val slotTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    private val scaleNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val w = width.toFloat()
            val slotWidth = w / 4f
            val clickedIndex = (e.x / slotWidth).toInt().coerceIn(0, 3)
            val slot = slots[clickedIndex]
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onSlotClickListener?.invoke(clickedIndex, slot)
            performClick()
            return true
        }
    })

    init {
        isClickable = true
        isFocusable = true

        slots.forEach { slot ->
            val angle = if (slot.isConnected) calculateAngle(slot.percent) else -52f
            slot.currentNeedleAngle = angle
            slot.targetNeedleAngle = angle
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun calculateAngle(percent: Int): Float {
        val pct = percent.coerceIn(0, 100)
        return -52f + (pct * 1.04f)
    }

    /**
     * Обновить данные хост-телефона от BatteryManager
     */
    fun setHostBattery(percent: Int, voltage: Float, customHostName: String? = null) {
        val slot = slots[0]
        slot.percent = percent.coerceIn(0, 100)
        slot.isConnected = true
        if (!customHostName.isNullOrBlank()) {
            slot.deviceName = customHostName.take(13).uppercase()
        } else if (slot.deviceName.isBlank()) {
            slot.deviceName = if (hostModelName.isNotEmpty()) hostModelName else "ТЕЛЕФОН"
        }
        slot.subtitle = String.format("АКБ %.2fV", voltage)
        slot.deviceType = DeviceType.PHONE
        animateNeedle(0, calculateAngle(percent))
        invalidate()
    }

    /**
     * Автоматически обновить слоты периферии (слоты 1, 2, 3) на основе реально подключённых Bluetooth-устройств.
     */
    fun setConnectedBluetoothDevices(devices: List<BluetoothDeviceManager.ConnectedDeviceInfo>) {
        for (i in 0 until 3) {
            val slotIdx = i + 1
            val slot = slots[slotIdx]

            if (i < devices.size) {
                val dev = devices[i]
                slot.deviceName = dev.name
                slot.percent = dev.batteryPercent.coerceIn(0, 100)
                slot.isConnected = true
                slot.subtitle = dev.subtitle
                slot.deviceType = dev.deviceType
                animateNeedle(slotIdx, calculateAngle(slot.percent))
            } else {
                // Слот свободен (устройство не подключено)
                slot.deviceName = "СЛОТ ${slotIdx + 1}"
                slot.percent = 0
                slot.isConnected = false
                slot.subtitle = "НЕТ СВЯЗИ"
                slot.deviceType = DeviceType.EMPTY_SLOT
                animateNeedle(slotIdx, -52f)
            }
        }
        invalidate()
    }

    private fun animateNeedle(slotIndex: Int, targetAngle: Float) {
        val slot = slots[slotIndex]
        slot.animator?.cancel()
        slot.targetNeedleAngle = targetAngle

        val anim = ValueAnimator.ofFloat(slot.currentNeedleAngle, targetAngle).apply {
            duration = 650
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener { va ->
                slot.currentNeedleAngle = va.animatedValue as Float
                invalidate()
            }
        }
        slot.animator = anim
        anim.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val desiredHeight = (118f * density).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // 1. Внешний контейнер виджета со скруглением 22dp
        val corner = 22f * density
        val widgetRect = RectF(1f, 1f, w - 1f, h - 1f)
        widgetBgPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            colorCaseBgTop, colorCaseBgBottom, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(widgetRect, corner, corner, widgetBgPaint)
        canvas.drawRoundRect(widgetRect, corner, corner, widgetBorderPaint)

        // 2. Отрисовка 4 слотов М4200
        val padH = 8f * density
        val padTop = 9f * density
        val padBottom = 8f * density
        val availableW = w - (padH * 2)
        val slotGap = 6f * density
        val slotW = (availableW - (slotGap * 3)) / 4f

        for (i in 0 until 4) {
            val slotLeft = padH + i * (slotW + slotGap)
            val slot = slots[i]
            drawMeterSlot(canvas, slotLeft, padTop, slotW, h - padTop - padBottom, slot)
        }
    }

    private fun drawMeterSlot(
        canvas: Canvas,
        x: Float,
        y: Float,
        slotW: Float,
        slotH: Float,
        slot: BatterySlot
    ) {
        val meterH = slotW * (220f / 320f)
        val meterRect = RectF(x, y, x + slotW, y + meterH)
        val s = slotW / 320f

        // А. Корпус вольтметра
        val meterCorner = 14f * s
        slotCasePaint.shader = LinearGradient(
            x, y, x, y + meterH,
            Color.parseColor("#222533"), Color.parseColor("#101117"), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(meterRect, meterCorner, meterCorner, slotCasePaint)
        canvas.drawRoundRect(meterRect, meterCorner, meterCorner, slotCaseBorderPaint)

        // Б. Окно циферблата (x=32, y=24, w=256, h=162)
        val dialX = x + (32f * s)
        val dialY = y + (24f * s)
        val dialW = 256f * s
        val dialH = 162f * s
        val dialCorner = 8f * s
        val dialRect = RectF(dialX, dialY, dialX + dialW, dialY + dialH)

        dialBgPaint.color = if (slot.isConnected) colorDialOn else colorDialOff
        canvas.drawRoundRect(dialRect, dialCorner, dialCorner, dialBgPaint)
        canvas.drawRoundRect(dialRect, dialCorner, dialCorner, dialInnerShadowPaint)

        // В. Центр дуги и радиус (cx=160, cy=195, R=145)
        val cx = x + (160f * s)
        val cy = y + (195f * s)
        val rArc = 145f * s

        val arcRect = RectF(cx - rArc, cy - rArc, cx + rArc, cy + rArc)
        val canvasStart = 218f
        val totalSweep = 104f

        val dangerSweep = totalSweep * 0.20f
        val safeSweep = totalSweep * 0.80f

        arcPaint.strokeWidth = 3.8f * s
        arcPaint.color = if (slot.isConnected) colorRedDanger else colorRedDangerDim
        canvas.drawArc(arcRect, canvasStart, dangerSweep, false, arcPaint)

        arcPaint.strokeWidth = 2f * s
        arcPaint.color = if (slot.isConnected) colorInk else colorInkDim
        canvas.drawArc(arcRect, canvasStart + dangerSweep, safeSweep, false, arcPaint)

        // Г. Радиальные засечки внутрь шкалы
        tickPaint.color = if (slot.isConnected) colorInk else colorInkDim
        for (idx in 0..10) {
            val frac = idx / 10f
            val angDeg = canvasStart + (totalSweep * frac)
            val rad = Math.toRadians(angDeg.toDouble())
            val isMajor = (idx % 2 == 0)
            val tickLen = if (isMajor) 10f * s else 6f * s
            tickPaint.strokeWidth = if (isMajor) 2f * s else 1.2f * s

            val outerX = cx + rArc * cos(rad).toFloat()
            val outerY = cy + rArc * sin(rad).toFloat()
            val innerX = cx + (rArc - tickLen) * cos(rad).toFloat()
            val innerY = cy + (rArc - tickLen) * sin(rad).toFloat()
            canvas.drawLine(outerX, outerY, innerX, innerY, tickPaint)
        }

        // Числа 0, 50, 100
        scaleNumberPaint.textSize = 9.5f * s
        scaleNumberPaint.color = if (slot.isConnected) Color.parseColor("#444444") else Color.parseColor("#3A3E4E")
        canvas.drawText("0", dialX + (28f * s), dialY + (86f * s), scaleNumberPaint)
        canvas.drawText("50", cx, dialY + (44f * s), scaleNumberPaint)
        canvas.drawText("100", dialX + dialW - (28f * s), dialY + (86f * s), scaleNumberPaint)

        // Д. Автоопределяемая иконка устройства над цифровым значением
        drawDeviceIcon(canvas, cx, dialY + (52f * s), 20f * s, slot)

        // Е. Цифровое значение процента / "ОТКЛ"
        readoutPaint.textSize = 19f * s
        if (slot.isConnected) {
            val isLow = slot.percent <= 20
            readoutPaint.color = if (isLow) colorRedDanger else Color.parseColor("#111111")
            canvas.drawText("${slot.percent}%", cx, dialY + (94f * s), readoutPaint)
        } else {
            readoutPaint.color = Color.parseColor("#4B5168")
            canvas.drawText("ОТКЛ", cx, dialY + (94f * s), readoutPaint)
        }

        // Ж. Подстрочный текст под значением (вольтаж / "НЕТ СВЯЗИ")
        subTextPaint.textSize = 7.5f * s
        subTextPaint.color = if (slot.isConnected) Color.parseColor("#555555") else Color.parseColor("#3B3F52")
        canvas.drawText(slot.subtitle, cx, dialY + (108f * s), subTextPaint)

        // З. Сигнальный рубиновый светодиод в верхнем правом углу (cx=264, cy=38)
        val ledCx = x + (264f * s)
        val ledCy = y + (38f * s)
        val ledR = 4.5f * s
        canvas.drawCircle(ledCx, ledCy, ledR + (2f * s), ledHousingPaint)

        val ledColor = when {
            !slot.isConnected -> colorLedOff
            slot.percent <= 20 -> colorLedAlert
            else -> colorLedDim
        }
        ledCorePaint.color = ledColor
        canvas.drawCircle(ledCx, ledCy, ledR, ledCorePaint)

        // И. Стрелка вольтметра с фиксатором на механическом нуле при отключении (-52°)
        canvas.save()
        canvas.rotate(slot.currentNeedleAngle, cx, cy)

        val needleLen = 100f * s
        val needlePath = Path().apply {
            moveTo(cx - (2f * s), cy)
            lineTo(cx, cy - needleLen)
            lineTo(cx + (2f * s), cy)
            close()
        }
        needlePaint.color = if (slot.isConnected) Color.parseColor("#1A1A1A") else Color.parseColor("#252834")
        canvas.drawPath(needlePath, needlePaint)

        if (slot.isConnected) {
            canvas.drawLine(cx, cy - (15f * s), cx, cy - needleLen, needleLinePaint)
        }
        canvas.restore()

        // Колпачок оси стрелки и винт нулевой юстировки
        canvas.drawCircle(cx, cy, 8f * s, pivotPaint)
        canvas.drawCircle(cx, cy, 3.5f * s, pivotCapPaint)

        // К. 4 угловых винта корпуса (строго снаружи стекла циферблата!)
        val screwR = 5f * s
        drawScrew(canvas, x + (18f * s), y + (18f * s), screwR, 35f)
        drawScrew(canvas, x + slotW - (18f * s), y + (18f * s), screwR, 120f)
        drawScrew(canvas, x + (18f * s), y + meterH - (18f * s), screwR, 80f)
        drawScrew(canvas, x + slotW - (18f * s), y + meterH - (18f * s), screwR, 15f)

        // Л. Нижняя плашка автоопределяемой подписи слота
        val labelTop = y + meterH + (6f * density)
        slotTitlePaint.textSize = 9.2f * scaledDensity
        val displayTitle = if (slot.deviceName.length > 12) slot.deviceName.take(11) + "…" else slot.deviceName
        canvas.drawText(displayTitle, cx, labelTop + (9f * density), slotTitlePaint)

        slotTagPaint.textSize = 8f * scaledDensity
        if (slot.isConnected) {
            slotTagPaint.color = if (slot.percent <= 20) colorRedDanger else colorGreenActive
            canvas.drawText("АКТИВЕН ${slot.percent}%", cx, labelTop + (19f * density), slotTagPaint)
        } else {
            slotTagPaint.color = Color.parseColor("#555A6E")
            canvas.drawText("ОТКЛЮЧЕН", cx, labelTop + (19f * density), slotTagPaint)
        }
    }

    private fun drawDeviceIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, slot: BatterySlot) {
        iconPaint.color = if (slot.isConnected) Color.parseColor("#222222") else Color.parseColor("#444960")
        val half = size / 2f

        when (slot.deviceType) {
            DeviceType.PHONE -> {
                val pw = size * 0.55f
                val ph = size * 0.9f
                val rect = RectF(cx - pw / 2f, cy - ph / 2f, cx + pw / 2f, cy + ph / 2f)
                canvas.drawRoundRect(rect, 2.5f * density, 2.5f * density, iconPaint)
                canvas.drawLine(cx - pw / 4f, cy + ph / 2f - (2.5f * density), cx + pw / 4f, cy + ph / 2f - (2.5f * density), iconPaint)
            }
            DeviceType.HEADPHONES -> {
                val arcRect = RectF(cx - half + 2, cy - half, cx + half - 2, cy + half)
                canvas.drawArc(arcRect, 180f, 180f, false, iconPaint)
                val cupW = 3f * density
                val cupH = 6f * density
                canvas.drawRect(cx - half, cy, cx - half + cupW, cy + cupH, iconPaint)
                canvas.drawRect(cx + half - cupW, cy, cx + half, cy + cupH, iconPaint)
            }
            DeviceType.WATCH -> {
                val ww = size * 0.7f
                val wh = size * 0.75f
                val rect = RectF(cx - ww / 2f, cy - wh / 2f, cx + ww / 2f, cy + wh / 2f)
                canvas.drawRoundRect(rect, 3f * density, 3f * density, iconPaint)
                canvas.drawLine(cx - ww / 3f, cy - wh / 2f - (3f * density), cx + ww / 3f, cy - wh / 2f - (3f * density), iconPaint)
                canvas.drawLine(cx - ww / 3f, cy + wh / 2f + (3f * density), cx + ww / 3f, cy + wh / 2f + (3f * density), iconPaint)
            }
            DeviceType.SPEAKER -> {
                val sw = size * 0.8f
                val sh = size * 0.8f
                val rect = RectF(cx - sw / 2f, cy - sh / 2f, cx + sw / 2f, cy + sh / 2f)
                canvas.drawRoundRect(rect, 3f * density, 3f * density, iconPaint)
                canvas.drawCircle(cx, cy, 4f * density, iconPaint)
            }
            DeviceType.GAMEPAD -> {
                val gw = size * 0.9f
                val gh = size * 0.6f
                val rect = RectF(cx - gw / 2f, cy - gh / 2f, cx + gw / 2f, cy + gh / 2f)
                canvas.drawRoundRect(rect, 4f * density, 4f * density, iconPaint)
                canvas.drawCircle(cx - (4f * density), cy, 1.5f * density, iconPaint)
                canvas.drawCircle(cx + (4f * density), cy, 1.5f * density, iconPaint)
            }
            DeviceType.POWERBANK -> {
                val bw = size * 0.85f
                val bh = size * 0.65f
                val rect = RectF(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f)
                canvas.drawRoundRect(rect, 2f * density, 2f * density, iconPaint)
                canvas.drawLine(cx + bw / 2f, cy - (3f * density), cx + bw / 2f + (2f * density), cy - (3f * density), iconPaint)
                canvas.drawLine(cx + bw / 2f, cy + (3f * density), cx + bw / 2f + (2f * density), cy + (3f * density), iconPaint)
                val boltPath = Path().apply {
                    moveTo(cx - 2, cy - (3f * density))
                    lineTo(cx + 2, cy)
                    lineTo(cx - 1, cy)
                    lineTo(cx + 2, cy + (3f * density))
                }
                canvas.drawPath(boltPath, iconPaint)
            }
            DeviceType.GENERIC_BT, DeviceType.EMPTY_SLOT -> {
                // Стилизованная руна Bluetooth
                val hHalf = size * 0.42f
                val wQuarter = size * 0.22f
                val btPath = Path().apply {
                    moveTo(cx, cy - hHalf)
                    lineTo(cx, cy + hHalf)
                    moveTo(cx - wQuarter, cy - (hHalf * 0.5f))
                    lineTo(cx + wQuarter, cy + (hHalf * 0.5f))
                    lineTo(cx, cy + hHalf)
                    moveTo(cx - wQuarter, cy + (hHalf * 0.5f))
                    lineTo(cx + wQuarter, cy - (hHalf * 0.5f))
                    lineTo(cx, cy - hHalf)
                }
                canvas.drawPath(btPath, iconPaint)
            }
        }
    }

    private fun drawScrew(canvas: Canvas, cx: Float, cy: Float, r: Float, angleDeg: Float) {
        canvas.drawCircle(cx, cy, r, screwPaint)
        val rad = Math.toRadians(angleDeg.toDouble())
        val dx = r * 0.72f * cos(rad).toFloat()
        val dy = r * 0.72f * sin(rad).toFloat()
        canvas.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, screwSlotPaint)
    }
}
