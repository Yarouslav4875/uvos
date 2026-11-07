package com.uvos.dashboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
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

/**
 * UVOS SystemShadeManager - контроллер приборной шторки параметров и уведомлений UnderVoltOS.
 *
 * Архитектура и принципы:
 * - Плавная анимация раскрытия и закрытия на базе сплайна Apple Spring (0.20, 1.0, 0.32, 1.0).
 * - Сетка быстрых приборных тумблеров (Сеть, Wi-Fi, Bluetooth, Изоляция, Фонарик, Радиотишина).
 * - Селектор профилей UVOS («СТЕРИЛЬНО» / «СВЯЗЬ» / «СВОЙ») с двухсторонней синхронизацией.
 * - Приборные линейные слайдеры яркости экрана и громкости акустики (SliderView).
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
    private val dragHandleBar: View = rootContainer.findViewById(R.id.shade_drag_handle_bar)

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
        setupToggles()
        setupProfileSelector()
        setupSliders()
        setupNotifications()
        setupGestures()
    }

    fun isShadeOpen(): Boolean = isOpen

    private fun setupClock() {
        val now = Date()
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEE, d MMMM", Locale("ru"))
        clockTimeText.text = timeFmt.format(now)
        clockDateText.text = "${dateFmt.format(now).uppercase()} • UVOS-CORE 2.0"
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
        // Тумблер 1: Мобильная сеть
        tileData.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            isDataOn = !isDataOn
            lampData.setOn(isDataOn)
            txtDataStatus.text = if (isDataOn) "4G LTE" else "ВЫКЛ"
            txtDataStatus.setTextColor(if (isDataOn) Color.parseColor("#E2E4EC") else Color.parseColor("#71768E"))
        }

        // Тумблер 2: Wi-Fi
        tileWifi.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            isWifiOn = !isWifiOn
            lampWifi.setOn(isWifiOn)
            txtWifiStatus.text = if (isWifiOn) "5 ГГЦ" else "ВЫКЛ"
            txtWifiStatus.setTextColor(if (isWifiOn) Color.parseColor("#E2E4EC") else Color.parseColor("#71768E"))
        }

        // Тумблер 3: Bluetooth
        tileBt.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            isBtOn = !isBtOn
            lampBt.setOn(isBtOn)
            txtBtStatus.text = if (isBtOn) "АКТИВЕН" else "ВЫКЛ"
            txtBtStatus.setTextColor(if (isBtOn) Color.parseColor("#E2E4EC") else Color.parseColor("#71768E"))
        }

        // Тумблер 4: Щит изоляции («Закрытая территория»)
        tileIsolation.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            isIsolationOn = !isIsolationOn
            lampIsolation.setOn(isIsolationOn)
            txtIsolationStatus.text = if (isIsolationOn) "АКТИВЕН" else "ОБЫЧНЫЙ"
            txtIsolationStatus.setTextColor(if (isIsolationOn) Color.parseColor("#E02020") else Color.parseColor("#71768E"))
        }

        // Тумблер 5: Фонарик
        tileTorch.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            isTorchOn = !isTorchOn
            lampTorch.setOn(isTorchOn)
            txtTorchStatus.text = if (isTorchOn) "ВКЛ" else "ВЫКЛ"
            txtTorchStatus.setTextColor(if (isTorchOn) Color.parseColor("#F59E0B") else Color.parseColor("#71768E"))
        }

        // Тумблер 6: Радиотишина (Стерильно)
        tileSterile.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            isSterileOn = !isSterileOn
            lampSterile.setOn(isSterileOn)
            txtSterileStatus.text = if (isSterileOn) "ТИШИНА" else "ДЕЖУРНЫЙ"
            txtSterileStatus.setTextColor(if (isSterileOn) Color.parseColor("#E02020") else Color.parseColor("#71768E"))
            if (isSterileOn) {
                syncProfile(ProfileType.STERILE)
            }
        }
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
        profileSelector.setSelectedIndex(idx, animate = true)
        updateProfileLabels(profile)
    }

    private fun updateProfileLabels(profile: ProfileType) {
        val name = when (profile) {
            ProfileType.STERILE -> "СТЕРИЛЬНО"
            ProfileType.COMM -> "СВЯЗЬ"
            ProfileType.CUSTOM -> "СВОЙ"
        }
        activeProfileLabel.text = "РЕЖИМ: $name"
    }

    private fun setupSliders() {
        sliderBrightness.valueFormatter = { p -> "${(p * 100).toInt()}%" }
        sliderBrightness.onProgressChanged = { p ->
            valBrightness.text = "${(p * 100).toInt()}%"
        }

        sliderVolume.valueFormatter = { p -> "${(p * 100).toInt()}%" }
        sliderVolume.onProgressChanged = { p ->
            valVolume.text = "${(p * 100).toInt()}%"
        }
    }

    private fun setupNotifications() {
        btnDismissSecurity.setOnClickListener {
            notifCardSecurity.visibility = View.GONE
            checkEmptyNotifications()
        }

        btnDismissNetwork.setOnClickListener {
            notifCardNetwork.visibility = View.GONE
            checkEmptyNotifications()
        }

        btnDismissPrivacy.setOnClickListener {
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
            closeShade(animate = true)
        }

        dragHandleBar.setOnClickListener {
            closeShade(animate = true)
        }

        // Свайп вверх по полоске закрытия
        var downY = 0f
        dragHandleBar.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.rawY
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
}
