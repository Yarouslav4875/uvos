package com.uvos.dashboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.Executors
import android.graphics.Color
import android.media.AudioManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.uvos.dashboard.model.ProfileType
import com.uvos.dashboard.repository.RuleRepositoryImpl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * UVOS SystemShadeManager - контроллер приборной шторки параметров и уведомлений UnderVoltOS.
 *
 * Архитектура и принципы:
 * - Плавная анимация раскрытия и закрытия на базе сплайна Apple Spring (0.20, 1.0, 0.32, 1.0).
 * - Сетка быстрых приборных тумблеров (Сеть, Wi-Fi, Bluetooth, Изоляция, Фонарик, Радиотишина) с тактильным откликом.
 * - Цветовая индикация физических ламп LampView (Изумруд #10B981, Янтарь #F59E0B, Алый #FF3B30 / #E02020, Выкл #71768E).
 * - Селектор профилей UVOS («СТЕРИЛЬНО» / «СВЯЗЬ» / «СВОЙ») с двухсторонней синхронизацией.
 * - Приборные линейные слайдеры яркости экрана и громкости акустики (SliderView) с пошаговым тиканьем.
 * - Центр уведомлений и тревожный журнал безопасности с мгновенной очисткой.
 */
class SystemShadeManager(
    private val context: Context,
    private val rootContainer: ViewGroup,
    private val repository: RuleRepositoryImpl
) {

    private val shadeRoot: View = rootContainer.findViewById(R.id.system_shade_root)

    // Шапка шторки
    private val clockTimeText: TextView = rootContainer.findViewById(R.id.shade_clock_time)
    private val clockDateText: TextView = rootContainer.findViewById(R.id.shade_clock_date)
    private val batteryStatusText: TextView = rootContainer.findViewById(R.id.shade_battery_status)
    private val btnCloseShade: View = rootContainer.findViewById(R.id.btn_close_shade)
    private val btnShadeCustomization: View? = rootContainer.findViewById(R.id.btn_shade_customization)
    private val dragHandleBar: View = rootContainer.findViewById(R.id.shade_drag_handle_bar)

    var onCustomizationRequested: (() -> Unit)? = null

    // Быстрые тумблеры параметров
    private val tileData: View = rootContainer.findViewById(R.id.tile_quick_data)
    private val lampData: LampView = rootContainer.findViewById(R.id.lamp_quick_data)
    private val txtDataStatus: TextView = rootContainer.findViewById(R.id.txt_quick_data_status)

    private val tileWifi: View = rootContainer.findViewById(R.id.tile_quick_wifi)
    private val lampWifi: LampView = rootContainer.findViewById(R.id.lamp_quick_wifi)
    private val txtWifiStatus: TextView = rootContainer.findViewById(R.id.txt_quick_wifi_status)

    private val tileBt: View = rootContainer.findViewById(R.id.tile_quick_bt)
    private val lampBt: LampView = rootContainer.findViewById(R.id.lamp_quick_bt)
    private val txtBtStatus: TextView = rootContainer.findViewById(R.id.txt_quick_bt_status)

    private val tileIsolation: View = rootContainer.findViewById(R.id.tile_quick_isolation)
    private val lampIsolation: LampView = rootContainer.findViewById(R.id.lamp_quick_isolation)
    private val txtIsolationStatus: TextView = rootContainer.findViewById(R.id.txt_quick_isolation_status)

    private val tileTorch: View = rootContainer.findViewById(R.id.tile_quick_torch)
    private val lampTorch: LampView = rootContainer.findViewById(R.id.lamp_quick_torch)
    private val txtTorchStatus: TextView = rootContainer.findViewById(R.id.txt_quick_torch_status)

    private val tileSterile: View = rootContainer.findViewById(R.id.tile_quick_sterile)
    private val lampSterile: LampView = rootContainer.findViewById(R.id.lamp_quick_sterile)
    private val txtSterileStatus: TextView = rootContainer.findViewById(R.id.txt_quick_sterile_status)

    // Профиль безопасности
    private val profileSelector: SegmentedControl = rootContainer.findViewById(R.id.shade_profile_selector)
    private val activeProfileLabel: TextView = rootContainer.findViewById(R.id.shade_active_profile_label)

    // Слайдеры
    private val sliderBrightness: SliderView = rootContainer.findViewById(R.id.shade_slider_brightness)
    private val valBrightness: TextView = rootContainer.findViewById(R.id.shade_val_brightness)
    private val sliderVolume: SliderView = rootContainer.findViewById(R.id.shade_slider_volume)
    private val valVolume: TextView = rootContainer.findViewById(R.id.shade_val_volume)

    // Уведомления
    private val notificationsContainer: LinearLayout = rootContainer.findViewById(R.id.shade_notifications_container)
    private val emptyNotificationsView: View = rootContainer.findViewById(R.id.shade_empty_notifications)
    private val btnClearNotifications: View = rootContainer.findViewById(R.id.btn_clear_notifications)

    private val notifCardSecurity: View = rootContainer.findViewById(R.id.notif_card_security)
    private val notifCardNetwork: View = rootContainer.findViewById(R.id.notif_card_network)
    private val notifCardPrivacy: View = rootContainer.findViewById(R.id.notif_card_privacy)

    private val btnDismissSecurity: View = rootContainer.findViewById(R.id.btn_dismiss_notif_security)
    private val btnDismissNetwork: View = rootContainer.findViewById(R.id.btn_dismiss_notif_network)
    private val btnDismissPrivacy: View = rootContainer.findViewById(R.id.btn_dismiss_notif_privacy)

    // Палитра приборных индикаторов UnderVoltOS
    companion object {
        private const val COLOR_EMERALD = "#10B981"  // Изумрудный индикатор (Wi-Fi, BT, Мобильная сеть)
        private const val COLOR_AMBER = "#F59E0B"    // Тёплый янтарный индикатор (Фонарик)
        private const val COLOR_SCARLET = "#FF3B30"  // Алый сигнал тревоги / блокировки (Стерильно, Изоляция)
        private const val COLOR_MUTED = "#71768E"    // Выключенное состояние (приглушенный дежурный цвет)
    }

    private val torchController = TorchController(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    // Состояния тумблеров
    private var isDataOn = true
    private var isWifiOn = true
    private var isBtOn = true
    private var isIsolationOn = false
    private var isTorchOn = false
    private var isSterileOn = false

    private var isOpen = false
    private var currentAnimator: ValueAnimator? = null
    private val springInterpolator = PathInterpolator(0.20f, 1.0f, 0.32f, 1.0f)

    var onProfileChangedListener: ((ProfileType) -> Unit)? = null
    var onShadeStateChangedListener: ((Boolean) -> Unit)? = null

    init {
        setupClock()
        setupTorch()
        setupToggles()
        setupProfileSelector()
        setupSliders()
        setupNotifications()
        setupGestures()
    }

    private fun setupTorch() {
        torchController.onTorchStateChanged = { enabled ->
            isTorchOn = enabled
            updateTorchToggleUI()
        }
        isTorchOn = torchController.isTorchActive()
        updateTorchToggleUI()
    }

    fun isShadeOpen(): Boolean = isOpen

    private fun setupClock() {
        val now = Date()
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEE, d MMMM", Locale("ru"))
        clockTimeText.text = timeFmt.format(now)
        clockDateText.text = "${dateFmt.format(now).uppercase()} • UVOS-CORE 2.0"

        // Тактильный отклик часов и даты
        clockTimeText.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        clockDateText.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        batteryStatusText.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    fun updateTime(now: Date = Date()) {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEE, d MMMM", Locale("ru"))
        clockTimeText.text = timeFmt.format(now)
        clockDateText.text = "${dateFmt.format(now).uppercase()} • UVOS-CORE 2.0"
    }

    fun updateBattery(percent: Int, voltageV: Float) {
        batteryStatusText.text = String.format(Locale.US, "%d%% • %.2fV ⚡", percent, voltageV)
    }

    private fun setupToggles() {
        // Конфигурация цветовых схем ламп LampView
        lampData.setLampColors(COLOR_EMERALD, COLOR_MUTED)
        lampWifi.setLampColors(COLOR_EMERALD, COLOR_MUTED)
        lampBt.setLampColors(COLOR_EMERALD, COLOR_MUTED)
        lampIsolation.setLampColors(COLOR_SCARLET, COLOR_MUTED)
        lampTorch.setLampColors(COLOR_AMBER, COLOR_MUTED)
        lampSterile.setLampColors(COLOR_SCARLET, COLOR_MUTED)

        // Синхронизация начального состояния изоляции с профилем
        isIsolationOn = (repository.getActiveProfile() == ProfileType.STERILE)

        // Применение исходных состояний индикаторов
        updateDataToggleUI()
        updateWifiToggleUI()
        updateBtToggleUI()
        updateIsolationToggleUI()
        updateTorchToggleUI()
        updateSterileToggleUI()

        // Тумблер 1: Мобильная сеть (Изумрудный индикатор при включении)
        tileData.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            isDataOn = !isDataOn
            updateDataToggleUI()
            try {
                val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                if (e is ActivityNotFoundException || e is SecurityException) {
                    try {
                        val fallbackIntent = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallbackIntent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "Настройки сети недоступны", Toast.LENGTH_SHORT).show()
                    } catch (_: SecurityException) {
                        Toast.makeText(context, "Ошибка доступа к настройкам сети", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                    }
                }
            }
        }

        // Тумблер 2: Wi-Fi (Изумрудный индикатор #10B981)
        tileWifi.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            isWifiOn = !isWifiOn
            updateWifiToggleUI()
            try {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "Настройки Wi-Fi недоступны", Toast.LENGTH_SHORT).show()
            } catch (_: SecurityException) {
                Toast.makeText(context, "Ошибка доступа к настройкам Wi-Fi", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
            }
        }

        tileWifi.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            try {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "Настройки Wi-Fi недоступны", Toast.LENGTH_SHORT).show()
            } catch (_: SecurityException) {
                Toast.makeText(context, "Ошибка доступа к настройкам Wi-Fi", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
            }
            true
        }

        // Тумблер 3: Bluetooth (Изумрудный индикатор #10B981)
        tileBt.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            isBtOn = !isBtOn
            updateBtToggleUI()
            try {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "Настройки Bluetooth недоступны", Toast.LENGTH_SHORT).show()
            } catch (_: SecurityException) {
                Toast.makeText(context, "Ошибка доступа к настройкам Bluetooth", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
            }
        }

        // Тумблер 4: Щит изоляции («Закрытая территория» -> Алый сигнал #FF3B30)
        tileIsolation.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            try {
                isIsolationOn = !isIsolationOn
                updateIsolationToggleUI()

                val targetProfile = if (isIsolationOn) ProfileType.STERILE else ProfileType.COMM
                repository.setActiveProfile(targetProfile)
                syncProfile(targetProfile)
                onProfileChangedListener?.invoke(targetProfile)

                val msg = if (isIsolationOn) "Режим изоляции АКТИВИРОВАН (СТЕРИЛЬНО)" else "Режим изоляции ДЕАКТИВИРОВАН (СВЯЗЬ)"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            } catch (_: SecurityException) {
                Toast.makeText(context, "Ошибка безопасности при переключении изоляции", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка переключения режима изоляции: ", Toast.LENGTH_SHORT).show()
            }
        }

        // Тумблер 5: Фонарик (Тёплый янтарный индикатор #F59E0B)
        tileTorch.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            torchController.toggleTorch()
        }

        // Тумблер 6: Радиотишина (Стерильно -> Алый сигнал #FF3B30)
        tileSterile.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            isSterileOn = !isSterileOn
            updateSterileToggleUI()
            if (isSterileOn) {
                syncProfile(ProfileType.STERILE)
            }
        }
    }

    private fun updateDataToggleUI() {
        lampData.setOn(isDataOn)
        txtDataStatus.text = if (isDataOn) "4G LTE" else "ВЫКЛ"
        txtDataStatus.setTextColor(Color.parseColor(if (isDataOn) COLOR_EMERALD else COLOR_MUTED))
    }

    private fun updateWifiToggleUI() {
        lampWifi.setOn(isWifiOn)
        txtWifiStatus.text = if (isWifiOn) "5 ГГЦ" else "ВЫКЛ"
        txtWifiStatus.setTextColor(Color.parseColor(if (isWifiOn) COLOR_EMERALD else COLOR_MUTED))
    }

    private fun updateBtToggleUI() {
        lampBt.setOn(isBtOn)
        txtBtStatus.text = if (isBtOn) "АКТИВЕН" else "ВЫКЛ"
        txtBtStatus.setTextColor(Color.parseColor(if (isBtOn) COLOR_EMERALD else COLOR_MUTED))
    }

    private fun updateIsolationToggleUI() {
        lampIsolation.setOn(isIsolationOn)
        txtIsolationStatus.text = if (isIsolationOn) "АКТИВЕН" else "ОБЫЧНЫЙ"
        txtIsolationStatus.setTextColor(Color.parseColor(if (isIsolationOn) COLOR_SCARLET else COLOR_MUTED))
    }

    private fun updateTorchToggleUI() {
        lampTorch.setOn(isTorchOn)
        txtTorchStatus.text = if (isTorchOn) "ВКЛ" else "ВЫКЛ"
        txtTorchStatus.setTextColor(Color.parseColor(if (isTorchOn) COLOR_AMBER else COLOR_MUTED))
    }

    private fun updateSterileToggleUI() {
        lampSterile.setOn(isSterileOn)
        txtSterileStatus.text = if (isSterileOn) "ТИШИНА" else "ДЕЖУРНЫЙ"
        txtSterileStatus.setTextColor(Color.parseColor(if (isSterileOn) COLOR_SCARLET else COLOR_MUTED))
    }

    private fun setupProfileSelector() {
        val profileNames = listOf("СТЕРИЛЬНО", "СВЯЗЬ", "СВОЙ")
        val currentProfile = repository.getActiveProfile()
        val defaultIdx = when (currentProfile) {
            ProfileType.STERILE -> 0
            ProfileType.COMM -> 1
            ProfileType.CUSTOM -> 2
        }
        profileSelector.setItems(profileNames, defaultIdx)
        updateProfileLabels(currentProfile)

        profileSelector.onSegmentSelected = { idx ->
            profileSelector.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            val selected = when (idx) {
                0 -> ProfileType.STERILE
                1 -> ProfileType.COMM
                else -> ProfileType.CUSTOM
            }
            repository.setActiveProfile(selected)
            updateProfileLabels(selected)
            onProfileChangedListener?.invoke(selected)
        }
    }

    fun syncProfile(profile: ProfileType) {
        val idx = when (profile) {
            ProfileType.STERILE -> 0
            ProfileType.COMM -> 1
            ProfileType.CUSTOM -> 2
        }
        profileSelector.setSelectedIndex(idx, animate = true, notify = false)
        updateProfileLabels(profile)

        // Синхронизация тумблера изоляции со статусом профиля
        isIsolationOn = (profile == ProfileType.STERILE)
        updateIsolationToggleUI()
    }

    private fun updateProfileLabels(profile: ProfileType) {
        val name = when (profile) {
            ProfileType.STERILE -> "СТЕРИЛЬНО"
            ProfileType.COMM -> "СВЯЗЬ"
            ProfileType.CUSTOM -> "СВОЙ"
        }
        activeProfileLabel.text = "РЕЖИМ: $name"
    }

    /**
     * Троттлер IPC вызовов для 120Hz экрана.
     * Ограничивает частоту обращений к Binder (WindowManager/AudioService) не чаще 1 раза в 16 мс,
     * предотвращая перегрузку очередей IPC и просадки fps UI-потока при быстром скролле ползунка.
     * Гарантирует доставку последнего актуального значения (trailing edge) после завершения жеста.
     */
    private class IpcThrottler(
        private val minIntervalMs: Long = 16L,
        private val onDispatch: (Float) -> Unit
    ) {
        private val handler = Handler(Looper.getMainLooper())
        private var lastDispatchTime = 0L
        private var pendingValue: Float? = null
        private var hasScheduledTrailing = false

        private val trailingRunnable = Runnable {
            hasScheduledTrailing = false
            pendingValue?.let { value ->
                lastDispatchTime = SystemClock.elapsedRealtime()
                onDispatch(value)
                pendingValue = null
            }
        }

        fun post(value: Float) {
            val now = SystemClock.elapsedRealtime()
            val elapsed = now - lastDispatchTime

            if (elapsed >= minIntervalMs) {
                handler.removeCallbacks(trailingRunnable)
                hasScheduledTrailing = false
                lastDispatchTime = now
                pendingValue = null
                onDispatch(value)
            } else {
                pendingValue = value
                if (!hasScheduledTrailing) {
                    hasScheduledTrailing = true
                    val delay = minIntervalMs - elapsed
                    handler.postDelayed(trailingRunnable, delay)
                }
            }
        }
    }

    private val ipcExecutor = Executors.newSingleThreadExecutor()

    private val brightnessIpcThrottler = IpcThrottler(minIntervalMs = 16L) { p ->
        val activity = context as? Activity
        activity?.window?.let { win ->
            try {
                val lp = win.attributes
                lp.screenBrightness = p.coerceIn(0.01f, 1.0f)
                win.attributes = lp
            } catch (_: Exception) {}
        }
    }

    private val volumeIpcThrottler = IpcThrottler(minIntervalMs = 16L) { p ->
        ipcExecutor.execute {
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                if (maxVol > 0) {
                    val targetVolume = (p * maxVol).roundToInt().coerceIn(0, maxVol)
                    try {
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun setupSliders() {
        var lastBrightnessStep = -1
        sliderBrightness.valueFormatter = { p -> "${(p * 100).toInt()}%" }
        sliderBrightness.onProgressChanged = { p ->
            val pct = (p * 100).toInt()
            valBrightness.text = "$pct%"
            val step = pct / 5
            if (step != lastBrightnessStep) {
                lastBrightnessStep = step
                sliderBrightness.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            brightnessIpcThrottler.post(p)
        }

        var lastVolumeStep = -1
        sliderVolume.valueFormatter = { p -> "${(p * 100).toInt()}%" }
        sliderVolume.onProgressChanged = { p ->
            val pct = (p * 100).toInt()
            valVolume.text = "$pct%"
            val step = pct / 5
            if (step != lastVolumeStep) {
                lastVolumeStep = step
                sliderVolume.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            volumeIpcThrottler.post(p)
        }

        syncBrightnessAndVolume()
    }

    fun syncBrightnessAndVolume() {
        // 1. Яркость окна / системы
        val activity = context as? Activity
        val currentWinBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        val brightnessFraction = if (currentWinBrightness in 0f..1f) {
            currentWinBrightness
        } else {
            try {
                val sysBrightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                (sysBrightness / 255f).coerceIn(0.01f, 1f)
            } catch (_: Exception) {
                0.5f
            }
        }
        sliderBrightness.setProgress(brightnessFraction)
        valBrightness.text = "${(brightnessFraction * 100).toInt()}%"

        // 2. Громкость мультимедиа STREAM_MUSIC
        audioManager?.let { am ->
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val volFraction = if (maxVol > 0) (curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f) else 0f
            sliderVolume.setProgress(volFraction)
            valVolume.text = "${(volFraction * 100).toInt()}%"
        }
    }

    private fun setupNotifications() {
        btnDismissSecurity.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            notifCardSecurity.visibility = View.GONE
            checkEmptyNotifications()
        }

        btnDismissNetwork.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            notifCardNetwork.visibility = View.GONE
            checkEmptyNotifications()
        }

        btnDismissPrivacy.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            notifCardPrivacy.visibility = View.GONE
            checkEmptyNotifications()
        }

        btnClearNotifications.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            notifCardSecurity.visibility = View.GONE
            notifCardNetwork.visibility = View.GONE
            notifCardPrivacy.visibility = View.GONE
            checkEmptyNotifications()
        }
    }

    private fun checkEmptyNotifications() {
        val hasActiveCards = notifCardSecurity.visibility == View.VISIBLE ||
                notifCardNetwork.visibility == View.VISIBLE ||
                notifCardPrivacy.visibility == View.VISIBLE

        emptyNotificationsView.visibility = if (hasActiveCards) View.GONE else View.VISIBLE
    }

    private fun setupGestures() {
        btnCloseShade.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            closeShade(animate = true)
        }

        btnShadeCustomization?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            closeShade(animate = true)
            onCustomizationRequested?.invoke()
        }

        dragHandleBar.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            closeShade(animate = true)
        }

        // Свайп вверх по полоске закрытия с тактильной отдачей
        var downY = 0f
        dragHandleBar.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.rawY
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaY = downY - event.rawY
                    if (deltaY > 30f) {
                        v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        closeShade(animate = true)
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun openShade(animate: Boolean = true) {
        if (isOpen) return
        isOpen = true
        setupClock()
        syncProfile(repository.getActiveProfile())
        syncBrightnessAndVolume()

        currentAnimator?.cancel()
        shadeRoot.visibility = View.VISIBLE
        shadeRoot.bringToFront()

        if (!animate) {
            shadeRoot.translationY = 0f
            shadeRoot.alpha = 1f
            onShadeStateChangedListener?.invoke(true)
            return
        }

        val height = if (shadeRoot.height > 0) shadeRoot.height.toFloat() else 1200f
        shadeRoot.translationY = -height
        shadeRoot.alpha = 0.3f

        currentAnimator = ValueAnimator.ofFloat(-height, 0f).apply {
            duration = 340
            interpolator = springInterpolator
            addUpdateListener { va ->
                val currY = va.animatedValue as Float
                shadeRoot.translationY = currY
                shadeRoot.alpha = 0.3f + 0.7f * ((currY + height) / height).coerceIn(0f, 1f)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    shadeRoot.translationY = 0f
                    shadeRoot.alpha = 1f
                    onShadeStateChangedListener?.invoke(true)
                }
            })
            start()
        }
    }

    fun closeShade(animate: Boolean = true) {
        if (!isOpen) return
        isOpen = false

        currentAnimator?.cancel()

        if (!animate) {
            shadeRoot.visibility = View.GONE
            onShadeStateChangedListener?.invoke(false)
            return
        }

        val height = if (shadeRoot.height > 0) shadeRoot.height.toFloat() else 1200f

        currentAnimator = ValueAnimator.ofFloat(0f, -height).apply {
            duration = 260
            interpolator = springInterpolator
            addUpdateListener { va ->
                val currY = va.animatedValue as Float
                shadeRoot.translationY = currY
                shadeRoot.alpha = (1f - (abs(currY) / height)).coerceIn(0f, 1f)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    shadeRoot.visibility = View.GONE
                    shadeRoot.translationY = 0f
                    shadeRoot.alpha = 1f
                    onShadeStateChangedListener?.invoke(false)
                }
            })
            start()
        }
    }

    fun onDestroy() {
        torchController.release()
    }
}
