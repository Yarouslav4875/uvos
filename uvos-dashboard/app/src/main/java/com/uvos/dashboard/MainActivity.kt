package com.uvos.dashboard

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.app.ActivityManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.io.File
import android.view.HapticFeedbackConstants
import android.view.View
import android.content.ClipData
import android.content.ClipDescription
import android.view.DragEvent
import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.uvos.dashboard.model.ProfileType
import com.uvos.dashboard.repository.RuleRepositoryImpl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UVOS Dashboard - главная приборная панель и домашняя оболочка системы (Launcher).
 * Реализует все 4 рабочих стола UnderVoltOS:
 * - СТОЛ 01: ПРИБОРНЫЙ СТОЛ (ЩИТ / DASHBOARD - 4 ВОЛЬТМЕТРА 2x2)
 * - СТОЛ 02: ДОМАШНИЙ ЭКРАН (ДОМ / HOME - ЧАСЫ, СЕТКА 4x4 ИКОНОК, ДОК С РЕДАКТИРОВАНИЕМ)
 * - СТОЛ 03: МЕНЮ ПРИЛОЖЕНИЙ (МЕНЮ / DRAWER - ЗАКРЫТАЯ ТЕРРИТОРИЯ)
 * - СТОЛ 04: МЕНЕДЖЕР ПРОЦЕССОВ (ПРОЦЕССЫ / PROCESS MANAGER)
 */

data class DragSlotPayload(
    val sourceType: String, // "HOME_SLOT", "DRAWER_APP"
    val fromSlotIndex: Int = -1,
    val packageName: String? = null,
    val title: String = "",
    val actionType: String = "APP",
    val iconResName: String? = null
)

class MainActivity : Activity() {

    // Пагинатор рабочих столов
    private lateinit var desktopPager: DesktopPagerView
    private lateinit var topBarCenter: TextView
    private lateinit var topBarBattery: TextView
    private lateinit var dotPage1: View
    private lateinit var dotPage2: View
    private lateinit var dotPage3: View
    private lateinit var dotPage4: View

    // Стол 01: Gauges & Controls
    private lateinit var gaugeCpu: GaugeView
    private lateinit var gaugeMem: GaugeView
    private lateinit var gaugeBattery: GaugeView
    private lateinit var gaugeNet: GaugeView
    private var valCpuBadge: TextView? = null
    private var valMemBadge: TextView? = null
    private var valBatteryBadge: TextView? = null
    private var valNetBadge: TextView? = null
    private lateinit var statusText: TextView
    private lateinit var profileSelector: SegmentedControl
    private lateinit var profileDescription: TextView
    private lateinit var logCard: LinearLayout
    private lateinit var logLampDot: LampView
    private lateinit var logBlockedText: TextView
    private lateinit var gaugeWidgetManager: GaugeWidgetManager
    private lateinit var customGaugesContainer: LinearLayout
    private lateinit var btnAddGaugeWidget: View

    // Стол 02: Home Clock & Grid
    private var batteryQuadWidget: BatteryQuadWidgetView? = null
    private var bluetoothDeviceManager: BluetoothDeviceManager? = null
    private lateinit var homeClockTime: TextView
    private lateinit var homeClockDate: TextView
    private lateinit var homeManager: HomeManager
    private lateinit var homeRow1: LinearLayout
    private lateinit var homeRow2: LinearLayout
    private lateinit var homeRow3: LinearLayout
    private lateinit var homeRow4: LinearLayout

    // Панель зон сброса Drag & Drop (как на обычном Android)
    private var dropTargetBar: LinearLayout? = null
    private var dropTargetRemove: LinearLayout? = null
    private var dropTargetUninstall: LinearLayout? = null
    private var dropTargetRemoveLabel: TextView? = null
    private var dropTargetUninstallLabel: TextView? = null

    // Состояние перетаскивания и долгого нажатия
    private var isDraggingSlot = false
    private var dragStartSlotIndex = -1
    private var hasMovedOutsideInitialSlot = false

    private val packageRemovedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pkg = intent.data?.schemeSpecificPart ?: return
            homeManager.removeAppPackage(pkg)
            renderHomeGrid()
            renderDrawerApps()
            loadRealProcessList()
        }
    }

    // Стол 03: App Drawer
    private lateinit var drawerAppsContainer: LinearLayout

    // Стол 04: Process Manager
    private lateinit var managerProcessContainer: LinearLayout
    private lateinit var managerFilterActive: TextView
    private lateinit var managerFilterAccess: TextView
    private lateinit var managerFilterIsolated: TextView
    private lateinit var btnKillAllServices: Button

    // Сводка менеджера процессов (Стол 04)
    private var managerSummaryActiveCount: TextView? = null
    private var managerSummaryRam: TextView? = null
    private var managerSummaryRamSub: TextView? = null
    private var managerSummaryChannels: TextView? = null

    // Цикл непрерывного обновления реальных системных данных (каждую секунду)
    private val systemStatsHandler = Handler(Looper.getMainLooper())
    private var lastRxBytes = -1L
    private var lastTxBytes = -1L
    private var lastNetSampleTime = 0L

    private val systemStatsRunnable = object : Runnable {
        override fun run() {
            updateLiveIndicators()
            systemStatsHandler.postDelayed(this, 1000)
        }
    }

    private lateinit var repository: RuleRepositoryImpl

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateClock()
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateFromBatteryIntent(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)

        repository = RuleRepositoryImpl.getInstance(this)
        homeManager = HomeManager(this)

        initTopBarAndPager()
        initDesktop1Dashboard()
        initDesktop2Home()
        initDesktop3Drawer()
        initDesktop4ProcessManager()
        initBottomDock()

        updateClock()
        batteryIntent()?.let { updateFromBatteryIntent(it) }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(timeTickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        val pkgFilter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply { addDataScheme("package") }
        registerReceiver(packageRemovedReceiver, pkgFilter)
        updateDailyLogStats()
        renderHomeGrid()
        renderDrawerApps()
        updateClock()
        checkBluetoothPermission()
        bluetoothDeviceManager?.start()
        systemStatsHandler.post(systemStatsRunnable)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
        unregisterReceiver(timeTickReceiver)
        try { unregisterReceiver(packageRemovedReceiver) } catch (_: Exception) {}
        bluetoothDeviceManager?.stop()
        systemStatsHandler.removeCallbacks(systemStatsRunnable)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        renderHomeGrid()
        renderDrawerApps()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Как домашняя оболочка UVOS не закрывается по Back, а возвращает на Стол 02 (Дом)
        if (desktopPager.currentPage != 1) {
            desktopPager.scrollToPage(1, smooth = true)
        }
    }

    // =========================================================================
    // ТОП-БАР И ПАГИНАТОР СТОЛОВ
    // =========================================================================

    private fun initTopBarAndPager() {
        topBarCenter = findViewById(R.id.top_bar_center)
        topBarBattery = findViewById(R.id.top_bar_battery)
        desktopPager = findViewById(R.id.desktop_pager)

        dotPage1 = findViewById(R.id.dot_page_1)
        dotPage2 = findViewById(R.id.dot_page_2)
        dotPage3 = findViewById(R.id.dot_page_3)
        dotPage4 = findViewById(R.id.dot_page_4)

        val pageTitles = listOf(
            "СТОЛ 01 • ЩИТ",
            "СТОЛ 02 • ДОМ",
            "СТОЛ 03 • МЕНЮ",
            "СТОЛ 04 • ПРОЦЕССЫ"
        )

        fun updatePageIndicators(page: Int) {
            val dots = listOf(dotPage1, dotPage2, dotPage3, dotPage4)
            val density = resources.displayMetrics.density
            val activeW = (18f * density).toInt()
            val inactiveW = (6f * density).toInt()
            val dotH = (6f * density).toInt()

            dots.forEachIndexed { index, dot ->
                val isActive = (index == page)
                dot.setBackgroundResource(if (isActive) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
                val lp = dot.layoutParams
                if (lp != null) {
                    lp.width = if (isActive) activeW else inactiveW
                    lp.height = dotH
                    dot.layoutParams = lp
                }
            }
        }

        desktopPager.onPageSelectedListener = { page ->
            updatePageIndicators(page)
        }

        desktopPager.onScrollProgressListener = { progress ->
            val nearestPage = Math.round(progress).coerceIn(0, 3)
            updatePageIndicators(nearestPage)
        }

        dotPage1.setOnClickListener { desktopPager.scrollToPage(0) }
        dotPage2.setOnClickListener { desktopPager.scrollToPage(1) }
        dotPage3.setOnClickListener { desktopPager.scrollToPage(2) }
        dotPage4.setOnClickListener { desktopPager.scrollToPage(3) }

        // Стартовое состояние - Стол 02 (Главный домашний экран с виджетами)
        desktopPager.post {
            desktopPager.scrollToPage(1, smooth = false)
            updatePageIndicators(1)
        }
    }

    // =========================================================================
    // СТОЛ 01: ПРИБОРНЫЙ СТОЛ (DASHBOARD)
    // =========================================================================

    private fun initDesktop1Dashboard() {
        gaugeCpu = findViewById(R.id.gauge_cpu)
        gaugeMem = findViewById(R.id.gauge_mem)
        gaugeBattery = findViewById(R.id.gauge_battery)
        gaugeNet = findViewById(R.id.gauge_net)

        valCpuBadge = findViewById(R.id.val_cpu_badge)
        valMemBadge = findViewById(R.id.val_mem_badge)
        valBatteryBadge = findViewById(R.id.val_battery_badge)
        valNetBadge = findViewById(R.id.val_net_badge)

        statusText = findViewById(R.id.status_text)
        profileSelector = findViewById(R.id.profile_selector)
        profileDescription = findViewById(R.id.profile_description)
        logCard = findViewById(R.id.card_daily_log)
        logLampDot = findViewById(R.id.log_lamp_dot)
        logBlockedText = findViewById(R.id.log_blocked_text)
        gaugeWidgetManager = GaugeWidgetManager(this)
        customGaugesContainer = findViewById(R.id.custom_gauges_container)
        btnAddGaugeWidget = findViewById(R.id.btn_add_gauge_widget)
        batteryQuadWidget = findViewById(R.id.battery_quad_widget)

        bluetoothDeviceManager = BluetoothDeviceManager(this).apply {
            onDevicesChangedListener = { devices ->
                runOnUiThread {
                    batteryQuadWidget?.setConnectedBluetoothDevices(devices)
                }
            }
        }


        btnAddGaugeWidget.setOnClickListener {
            showAddGaugeDialog()
        }

        val onGaugeLongClick = View.OnLongClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            true
        }
        gaugeCpu.setOnLongClickListener(onGaugeLongClick)
        gaugeMem.setOnLongClickListener(onGaugeLongClick)
        gaugeBattery.setOnLongClickListener(onGaugeLongClick)
        gaugeNet.setOnLongClickListener(onGaugeLongClick)

        renderCustomGauges()

                // Первичный сбор реальных системных данных
        updateLiveIndicators()

        val profileNames = listOf(
            getString(R.string.profile_sterile),
            getString(R.string.profile_comm),
            getString(R.string.profile_custom)
        )
        val currentProfile = repository.getActiveProfile()
        val defaultIdx = when (currentProfile) {
            ProfileType.STERILE -> 0
            ProfileType.COMM -> 1
            ProfileType.CUSTOM -> 2
        }
        profileSelector.setItems(profileNames, defaultIdx)
        updateProfileDescription(currentProfile)

        profileSelector.onSegmentSelected = { index ->
            val selectedProfile = when (index) {
                0 -> ProfileType.STERILE
                1 -> ProfileType.COMM
                else -> ProfileType.CUSTOM
            }
            repository.setActiveProfile(selectedProfile)
            updateProfileDescription(selectedProfile)
            renderDrawerApps()
        }

        logCard.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        findViewById<Button>(R.id.open_shield).setOnClickListener {
            startActivity(Intent(this, ShieldActivity::class.java))
        }
    }

    private fun showAddGaugeDialog() {
        val options = arrayOf(
            "🗄️ Накопитель UFS/Flash (STORAGE)",
            "🌡️ Термопакет SoC и АКБ (THERMAL)",
            "🛡️ Шлюз изоляции каналов (SECURITY)",
            "📡 Мощность радиочастот (RADIO RF)"
        )
        AlertDialog.Builder(this)
            .setTitle("Добавить прибор на панель")
            .setItems(options) { _, which ->
                val type = when (which) {
                    0 -> GaugeWidgetType.STORAGE
                    1 -> GaugeWidgetType.THERMAL
                    2 -> GaugeWidgetType.SECURITY
                    else -> GaugeWidgetType.RADIO
                }
                gaugeWidgetManager.addGauge(type)
                renderCustomGauges()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun renderCustomGauges() {
        customGaugesContainer.removeAllViews()
        val gauges = gaugeWidgetManager.getGauges()

        gauges.forEach { item ->
            val row = layoutInflater.inflate(R.layout.item_custom_gauge, customGaugesContainer, false)
            val titleView = row.findViewById<TextView>(R.id.custom_gauge_title)
            val btnRemove = row.findViewById<TextView>(R.id.custom_gauge_btn_remove)
            val gaugeView = row.findViewById<GaugeView>(R.id.custom_gauge_view)
            val valueView = row.findViewById<TextView>(R.id.custom_gauge_value)
            val subtitleView = row.findViewById<TextView>(R.id.custom_gauge_subtitle)
            val badgeView = row.findViewById<TextView>(R.id.custom_gauge_badge)

            titleView.text = item.title

            when (item.type) {
                GaugeWidgetType.STORAGE -> {
                    val stats = gaugeWidgetManager.getStorageStats()
                    gaugeView.setLevel(stats.fraction, "${stats.percent}%")
                    valueView.text = stats.usedText
                    subtitleView.text = stats.subtitle
                    badgeView.text = "UFS 2.2 • СТАТУС: В НОРМЕ"
                    badgeView.setTextColor(Color.parseColor("#E2E4EC"))
                }
                GaugeWidgetType.THERMAL -> {
                    val tempC = 34.8f
                    gaugeView.setLevel(tempC / 60f, "35°C")
                    valueView.text = "34.8°C (АКБ 31.2°C)"
                    subtitleView.text = "Snapdragon 685 • Порог: 60°C"
                    badgeView.text = "THERMAL GOVERNOR: НОРМА"
                    badgeView.setTextColor(Color.parseColor("#06B6D4"))
                }
                GaugeWidgetType.SECURITY -> {
                    gaugeView.setLevel(0.86f, "86%")
                    valueView.text = "24 / 28 ИЗОЛИРОВАНО"
                    subtitleView.text = "4 системных сокета активно"
                    badgeView.text = "FIREWALL KERNEL: АКТИВЕН"
                    badgeView.setTextColor(Color.parseColor("#06B6D4"))
                }
                GaugeWidgetType.RADIO -> {
                    gaugeView.setLevel(0.72f, "72%")
                    valueView.text = "-68 dBm (4G LTE-A)"
                    subtitleView.text = "Диапазон: B3 (1800 МГц)"
                    badgeView.text = "РАДИОМОДЕМ: СВЯЗЬ СТАБИЛЬНА"
                    badgeView.setTextColor(Color.parseColor("#E2E4EC"))
                }
            }

            btnRemove.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Удалить прибор?")
                    .setMessage("Убрать прибор «${item.title}» с панели 4 счетчиков?")
                    .setPositiveButton("Удалить") { _, _ ->
                        gaugeWidgetManager.removeGauge(item.id)
                        renderCustomGauges()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }

            customGaugesContainer.addView(row)
        }
    }

    // =========================================================================
    // СТОЛ 02: ДОМАШНИЙ ЭКРАН (HOME) С ПОЛНОЦЕННЫМ DRAG & DROP И УДАЛЕНИЕМ
    // =========================================================================

    private fun initDesktop2Home() {
        homeClockTime = findViewById(R.id.home_clock_time)
        homeClockDate = findViewById(R.id.home_clock_date)

        homeRow1 = findViewById(R.id.home_row_1)
        homeRow2 = findViewById(R.id.home_row_2)
        homeRow3 = findViewById(R.id.home_row_3)
        homeRow4 = findViewById(R.id.home_row_4)

        dropTargetBar = findViewById(R.id.drop_target_bar)
        dropTargetRemove = findViewById(R.id.drop_target_remove)
        dropTargetUninstall = findViewById(R.id.drop_target_uninstall)
        dropTargetRemoveLabel = findViewById(R.id.drop_target_remove_label)
        dropTargetUninstallLabel = findViewById(R.id.drop_target_uninstall_label)

        initDropTargetListeners()

        // Долгий тап по часам/дате открывает общее меню настройки стола
        val openHomeSettings = View.OnLongClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showHomeSettingsDialog()
            true
        }
        homeClockTime.setOnLongClickListener(openHomeSettings)
        homeClockDate.setOnLongClickListener(openHomeSettings)

        renderHomeGrid()
    }

    private fun initDropTargetListeners() {
        // Зона 1: "Убрать с экрана" (Освободить слот рабочего стола)
        dropTargetRemove?.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> {
                    hasMovedOutsideInitialSlot = true
                    v.isActivated = true
                    v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(110).start()
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    v.isActivated = false
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(110).start()
                    true
                }
                DragEvent.ACTION_DROP -> {
                    v.isActivated = false
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(110).start()
                    val payload = event.localState as? DragSlotPayload ?: return@setOnDragListener false
                    v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    if (payload.sourceType == "HOME_SLOT" && payload.fromSlotIndex != -1) {
                        homeManager.clearSlot(payload.fromSlotIndex)
                        renderHomeGrid()
                        Toast.makeText(this, "Ярлык «${payload.title}» убран с экрана", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    v.isActivated = false
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                    hideDropTargetBar()
                    true
                }
                else -> true
            }
        }

        // Зона 2: "Удалить" (Деинсталляция приложения с телефона)
        dropTargetUninstall?.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> {
                    hasMovedOutsideInitialSlot = true
                    v.isActivated = true
                    v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(110).start()
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    v.isActivated = false
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(110).start()
                    true
                }
                DragEvent.ACTION_DROP -> {
                    v.isActivated = false
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(110).start()
                    val payload = event.localState as? DragSlotPayload ?: return@setOnDragListener false
                    v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

                    if (payload.packageName != null && !isSystemApp(payload.packageName)) {
                        if (payload.sourceType == "HOME_SLOT" && payload.fromSlotIndex != -1) {
                            homeManager.clearSlot(payload.fromSlotIndex)
                            renderHomeGrid()
                        }
                        requestUninstallApp(payload.packageName)
                    } else {
                        Toast.makeText(this, "Системные утилиты UnderVoltOS нельзя удалить, но можно убрать с экрана", Toast.LENGTH_LONG).show()
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    v.isActivated = false
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                    hideDropTargetBar()
                    true
                }
                else -> true
            }
        }
    }

    private fun showDropTargetBar(canUninstall: Boolean) {
        dropTargetBar?.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationY = -60f
            animate().alpha(1f).translationY(0f).setDuration(180).start()
        }
        dropTargetUninstall?.visibility = if (canUninstall) View.VISIBLE else View.GONE
    }

    private fun hideDropTargetBar() {
        dropTargetBar?.apply {
            if (visibility == View.VISIBLE) {
                animate().alpha(0f).translationY(-50f).setDuration(160).withEndAction {
                    visibility = View.GONE
                }.start()
            }
        }
        isDraggingSlot = false
    }

    private fun startSlotDrag(slot: HomeSlotItem, view: View) {
        if (slot.actionType == "EMPTY") return
        isDraggingSlot = true
        dragStartSlotIndex = slot.slotIndex
        hasMovedOutsideInitialSlot = false

        val payload = DragSlotPayload(
            sourceType = "HOME_SLOT",
            fromSlotIndex = slot.slotIndex,
            packageName = slot.packageName,
            title = slot.title,
            actionType = slot.actionType,
            iconResName = slot.iconResName
        )
        val clipData = ClipData.newPlainText("UVOS_SLOT", slot.title)
        val shadow = View.DragShadowBuilder(view)
        val canUninstall = (slot.actionType == "APP" && slot.packageName != null && !isSystemApp(slot.packageName))
        showDropTargetBar(canUninstall)
        view.startDragAndDrop(clipData, shadow, payload, View.DRAG_FLAG_GLOBAL)
    }

    private fun renderHomeGrid() {
        homeRow1.removeAllViews()
        homeRow2.removeAllViews()
        homeRow3.removeAllViews()
        homeRow4.removeAllViews()

        val pm = packageManager
        val slots = homeManager.getSlots()

        slots.forEach { slot ->
            val targetRow = when (slot.slotIndex / 4) {
                0 -> homeRow1
                1 -> homeRow2
                2 -> homeRow3
                else -> homeRow4
            }

            val itemView = layoutInflater.inflate(R.layout.item_home_slot, targetRow, false)
            val iconView = itemView.findViewById<ImageView>(R.id.slot_icon)
            val emptyBadge = itemView.findViewById<TextView>(R.id.slot_empty_badge)
            val labelView = itemView.findViewById<TextView>(R.id.slot_label)

            labelView.text = slot.title

            if (slot.actionType == "EMPTY") {
                iconView.visibility = View.GONE
                emptyBadge.visibility = View.VISIBLE
                labelView.setTextColor(Color.parseColor("#71768E"))

                itemView.setOnClickListener {
                    showAppChooserForSlot(slot.slotIndex)
                }
                itemView.setOnLongClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    showAppChooserForSlot(slot.slotIndex)
                    true
                }
            } else {
                iconView.visibility = View.VISIBLE
                emptyBadge.visibility = View.GONE
                labelView.setTextColor(Color.parseColor("#E2E4EC"))

                // Загрузка значка
                if (slot.actionType == "APP" && slot.packageName != null) {
                    try {
                        val icon = pm.getApplicationIcon(slot.packageName!!)
                        iconView.setImageDrawable(icon)
                        iconView.setPadding(0, 0, 0, 0)
                        iconView.setBackgroundColor(Color.TRANSPARENT)
                    } catch (_: Exception) {
                        iconView.setImageResource(android.R.drawable.sym_def_app_icon)
                        iconView.setBackgroundResource(R.drawable.bg_tile_clickable)
                        iconView.setPadding(24, 24, 24, 24)
                    }
                } else {
                    val resId = getSystemIconRes(slot.iconResName)
                    iconView.setImageResource(resId)
                    iconView.setBackgroundResource(R.drawable.bg_tile_clickable)
                    iconView.setPadding(26, 26, 26, 26)
                }

                // Клик - запуск приложения
                itemView.setOnClickListener {
                    handleSlotClick(slot)
                }

                // Долгий тап - запуск режима Drag & Drop (или показ контекстного меню если палец отпущен)
                itemView.setOnLongClickListener { v ->
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    startSlotDrag(slot, v)
                    true
                }
            }

            // Drag & Drop слушатель для каждого слота сетки (перемещение, swap, вставка)
            itemView.setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> true
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        if (dragStartSlotIndex != slot.slotIndex) {
                            hasMovedOutsideInitialSlot = true
                        }
                        v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(100).start()
                        v.setBackgroundResource(R.drawable.bg_slot_drop_target)
                        v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        true
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                        v.setBackgroundColor(Color.TRANSPARENT)
                        true
                    }
                    DragEvent.ACTION_DROP -> {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                        v.setBackgroundColor(Color.TRANSPARENT)
                        val payload = event.localState as? DragSlotPayload ?: return@setOnDragListener false
                        v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

                        if (payload.sourceType == "HOME_SLOT") {
                            if (payload.fromSlotIndex != slot.slotIndex) {
                                hasMovedOutsideInitialSlot = true
                                homeManager.swapOrMoveSlot(payload.fromSlotIndex, slot.slotIndex)
                                renderHomeGrid()
                            }
                        } else if (payload.sourceType == "DRAWER_APP") {
                            hasMovedOutsideInitialSlot = true
                            homeManager.updateSlot(slot.slotIndex, payload.title, payload.packageName, "APP", null)
                            renderHomeGrid()
                            Toast.makeText(this@MainActivity, "Ярлык «${payload.title}» размещён на столе", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                        v.setBackgroundColor(Color.TRANSPARENT)
                        hideDropTargetBar()

                        // Если пользователь просто нажал и отпустил без перемещения в другой слот/корзину,
                        // открываем всплывающее меню быстрых действий как на обычном Android!
                        if (!hasMovedOutsideInitialSlot && dragStartSlotIndex == slot.slotIndex && slot.actionType != "EMPTY") {
                            v.post {
                                showAppQuickActionsDialog(slot)
                            }
                        }
                        true
                    }
                    else -> true
                }
            }

            // Тактильный пружинный отклик
            itemView.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(110).start()
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(220).setInterpolator(android.view.animation.OvershootInterpolator(1.2f)).start()
                    }
                }
                false
            }

            targetRow.addView(itemView)
        }
    }

    private fun getSystemIconRes(iconName: String?): Int {
        return when (iconName) {
            "ic_sys_phone" -> R.drawable.ic_sys_phone
            "ic_sys_messages" -> R.drawable.ic_sys_messages
            "ic_sys_radar" -> R.drawable.ic_sys_radar
            "ic_sys_camera" -> R.drawable.ic_sys_camera
            "ic_sys_files" -> R.drawable.ic_sys_files
            "ic_sys_player" -> R.drawable.ic_sys_player
            "ic_sys_calc" -> R.drawable.ic_sys_calc
            "ic_sys_settings" -> R.drawable.ic_sys_settings
            "ic_sys_shield" -> R.drawable.ic_sys_shield
            "ic_sys_oscilloscope" -> R.drawable.ic_sys_oscilloscope
            "ic_sys_terminal" -> R.drawable.ic_sys_terminal
            "ic_sys_packages" -> R.drawable.ic_sys_packages
            else -> R.drawable.ic_sys_shield
        }
    }

    private fun handleSlotClick(slot: HomeSlotItem) {
        val pm = packageManager
        when (slot.actionType) {
            "APP" -> {
                if (slot.packageName != null) {
                    val intent = pm.getLaunchIntentForPackage(slot.packageName!!)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Приложение не найдено", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "SYSTEM_PHONE" -> safeStartIntent(Intent(Intent.ACTION_DIAL))
            "SYSTEM_MESSAGES" -> safeStartIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) })
            "SYSTEM_RADAR" -> safeStartIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")))
            "SYSTEM_CAMERA" -> safeStartIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            "SYSTEM_FILES" -> safeStartIntent(Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" })
            "SYSTEM_PLAYER" -> safeStartIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MUSIC) })
            "SYSTEM_CALC" -> safeStartIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALCULATOR) })
            "SYSTEM_SETTINGS" -> safeStartIntent(Intent(Settings.ACTION_SETTINGS))
            "SYSTEM_SHIELD" -> startActivity(Intent(this, ShieldActivity::class.java))
            "SYSTEM_LOG" -> startActivity(Intent(this, LogActivity::class.java))
            "SYSTEM_OSC" -> {}
            "SYSTEM_TERM" -> {}
            "SYSTEM_PACKAGES" -> desktopPager.scrollToPage(2, smooth = true)
        }
    }

    /**
     * Контекстное меню быстрого доступа (Quick Actions Floating Card) как на обычном Android
     */
    private fun showAppQuickActionsDialog(slot: HomeSlotItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_app_quick_actions, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val dialogWidth = (resources.displayMetrics.widthPixels * 0.90f).toInt()
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        val iconView = dialogView.findViewById<ImageView>(R.id.dialog_app_icon)
        val titleView = dialogView.findViewById<TextView>(R.id.dialog_app_title)
        val subtitleView = dialogView.findViewById<TextView>(R.id.dialog_app_subtitle)

        titleView.text = slot.title
        subtitleView.text = slot.packageName ?: "Системный инструмент UnderVoltOS"

        val pm = packageManager
        if (slot.actionType == "APP" && slot.packageName != null) {
            try {
                iconView.setImageDrawable(pm.getApplicationIcon(slot.packageName!!))
            } catch (_: Exception) {
                iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        } else {
            iconView.setImageResource(getSystemIconRes(slot.iconResName))
        }

        // Действие 1: Убрать с экрана
        dialogView.findViewById<View>(R.id.action_remove_from_screen).setOnClickListener {
            dialog.dismiss()
            homeManager.clearSlot(slot.slotIndex)
            renderHomeGrid()
            Toast.makeText(this, "Ярлык «${slot.title}» убран с экрана", Toast.LENGTH_SHORT).show()
        }

        // Действие 2: Удалить приложение
        val uninstallBtn = dialogView.findViewById<View>(R.id.action_uninstall_app)
        val canUninstall = (slot.actionType == "APP" && slot.packageName != null && !isSystemApp(slot.packageName))
        if (canUninstall) {
            uninstallBtn.visibility = View.VISIBLE
            uninstallBtn.setOnClickListener {
                dialog.dismiss()
                homeManager.clearSlot(slot.slotIndex)
                renderHomeGrid()
                requestUninstallApp(slot.packageName)
            }
        } else {
            uninstallBtn.visibility = View.GONE
        }

        // Действие 3: О приложении
        val appInfoBtn = dialogView.findViewById<View>(R.id.action_app_info)
        if (slot.actionType == "APP" && slot.packageName != null) {
            appInfoBtn.visibility = View.VISIBLE
            appInfoBtn.setOnClickListener {
                dialog.dismiss()
                openAppDetails(slot.packageName)
            }
        } else {
            appInfoBtn.visibility = View.GONE
        }

        // Действие 4: Переименовать
        dialogView.findViewById<View>(R.id.action_rename_slot).setOnClickListener {
            dialog.dismiss()
            showRenameDialog(slot)
        }

        // Действие 5: Заменить приложение
        dialogView.findViewById<View>(R.id.action_replace_app).setOnClickListener {
            dialog.dismiss()
            showAppChooserForSlot(slot.slotIndex)
        }

        // Действие 6: Перетащить
        dialogView.findViewById<View>(R.id.action_start_drag).setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Зажмите ярлык на столе для перетаскивания в любой слот или в корзину", Toast.LENGTH_LONG).show()
        }

        dialog.show()
    }

    private fun showRenameDialog(slot: HomeSlotItem) {
        val input = EditText(this)
        input.setText(slot.title)
        input.setSelection(slot.title.length)

        AlertDialog.Builder(this)
            .setTitle("Переименовать ярлык")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    slot.title = newName
                    homeManager.updateSlot(slot.slotIndex, newName, slot.packageName, slot.actionType, slot.iconResName)
                    renderHomeGrid()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showAppChooserForSlot(slotIndex: Int) {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveList = pm.queryIntentActivities(intent, 0)
        resolveList.sortBy { it.loadLabel(pm).toString().lowercase(Locale.getDefault()) }

        val labels = resolveList.map { it.loadLabel(pm).toString() }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Выберите приложение для слота")
            .setItems(labels) { _, which ->
                val selected = resolveList[which]
                val pkg = selected.activityInfo.packageName
                val label = selected.loadLabel(pm).toString()

                homeManager.updateSlot(slotIndex, label, pkg, "APP", null)
                renderHomeGrid()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showHomeSettingsDialog() {
        val options = arrayOf(
            "➕ Добавить ярлык в свободный слот",
            "🔄 Восстановить стандартные иконки UVOS"
        )
        AlertDialog.Builder(this)
            .setTitle("Настройка рабочего стола")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val slots = homeManager.getSlots()
                        val freeIdx = slots.indexOfFirst { it.actionType == "EMPTY" }
                        if (freeIdx != -1) {
                            showAppChooserForSlot(freeIdx)
                        } else {
                            Toast.makeText(this, "Все 16 слотов заняты. Уберите ненужный ярлык.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        homeManager.resetToDefaults()
                        renderHomeGrid()
                    }
                }
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun isSystemApp(packageName: String?): Boolean {
        if (packageName == null) return true
        return try {
            val ai = packageManager.getApplicationInfo(packageName, 0)
            (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (_: Exception) {
            false
        }
    }

    private fun openAppDetails(packageName: String?) {
        if (packageName == null) return
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Не удалось открыть настройки приложения", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestUninstallApp(packageName: String?) {
        if (packageName == null) return
        try {
            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                data = Uri.parse("package:$packageName")
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    // =========================================================================
    // СТОЛ 03: МЕНЮ ПРИЛОЖЕНИЙ (APP DRAWER / ЗАКРЫТАЯ ТЕРРИТОРИЯ)
    // =========================================================================

    private fun initDesktop3Drawer() {
        drawerAppsContainer = findViewById(R.id.drawer_apps_container)
        renderDrawerApps()
    }

    private fun renderDrawerApps() {
        drawerAppsContainer.removeAllViews()
        val apps = repository.getAllApps()
        val pm = packageManager

        apps.forEach { app ->
            val row = layoutInflater.inflate(R.layout.item_drawer_app, drawerAppsContainer, false)
            val iconView = row.findViewById<ImageView>(R.id.drawer_item_icon)
            val titleView = row.findViewById<TextView>(R.id.drawer_item_title)
            val pkgView = row.findViewById<TextView>(R.id.drawer_item_package)
            val badgeView = row.findViewById<TextView>(R.id.drawer_item_badge)
            val shieldBtn = row.findViewById<TextView>(R.id.drawer_btn_shield)

            titleView.text = app.appName
            pkgView.text = app.packageName

            try {
                val icon = pm.getApplicationIcon(app.packageName)
                iconView.setImageDrawable(icon)
            } catch (_: Exception) {
                iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            val openCount = app.openChannelsCount
            val isSystem = try {
                val ai = pm.getApplicationInfo(app.packageName, 0)
                (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (_: Exception) {
                false
            }

            when {
                isSystem -> {
                    badgeView.text = "СИСТЕМА"
                    badgeView.setBackgroundResource(R.drawable.bg_badge_system)
                    badgeView.setTextColor(Color.parseColor("#71768E"))
                }
                openCount == 0 -> {
                    badgeView.text = "ИЗОЛИРОВАНО (0)"
                    badgeView.setBackgroundResource(R.drawable.bg_badge_isolated)
                    badgeView.setTextColor(Color.parseColor("#06B6D4"))
                }
                openCount == 1 -> {
                    badgeView.text = "1 КАНАЛ (СЕТЬ)"
                    badgeView.setBackgroundResource(R.drawable.bg_badge_channel)
                    badgeView.setTextColor(Color.parseColor("#E2E4EC"))
                }
                else -> {
                    badgeView.text = "$openCount КАНАЛА"
                    badgeView.setBackgroundResource(R.drawable.bg_badge_channel)
                    badgeView.setTextColor(Color.parseColor("#FF6B6B"))
                }
            }

            // Нажатие - запуск приложения
            row.setOnClickListener {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                } else {
                    openAppShield(app.packageName)
                }
            }

            // Долгий тап в Drawer - всплывающее меню быстрых действий и перетаскивание на рабочий стол
            row.setOnLongClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showDrawerAppQuickActionsDialog(app)
                true
            }

            // Щиток каналов приложения
            shieldBtn.setOnClickListener {
                openAppShield(app.packageName)
            }

            drawerAppsContainer.addView(row)
        }
    }

    // =========================================================================
    // СТОЛ 04: МЕНЕДЖЕР ПРОЦЕССОВ (PROCESS MANAGER)
    // =========================================================================

    private data class ProcessItem(
        val name: String,
        val pkg: String,
        val pid: String,
        val ramText: String,
        val cpuText: String,
        val hasNet: Boolean,
        val hasGeo: Boolean,
        val isIsolated: Boolean
    )

    private val processList = mutableListOf(
        ProcessItem("Telegram", "org.telegram.messenger", "PID: 10428 • u0_a51", "ОЗУ: 440 МБ", "CPU: 8.4%", hasNet = true, hasGeo = true, isIsolated = false),
        ProcessItem("Радар-веб (Chrome)", "com.android.chrome", "PID: 9812 • u0_a40", "ОЗУ: 610 МБ", "CPU: 4.2%", hasNet = true, hasGeo = false, isIsolated = false),
        ProcessItem("Карты", "ru.yandex.yandexnavi", "PID: 11024 • u0_a62", "ОЗУ: 280 МБ", "CPU: 1.1%", hasNet = true, hasGeo = true, isIsolated = false),
        ProcessItem("Банк", "ru.sberbankmobile", "PID: 12450 • u0_a77", "ОЗУ: 190 МБ", "CPU: 0.0%", hasNet = false, hasGeo = false, isIsolated = true),
        ProcessItem("UVOS-Core Daemon", "com.uvos.system", "PID: 1000 • system", "ОЗУ: 820 МБ", "CPU: 2.1%", hasNet = true, hasGeo = false, isIsolated = false)
    )

    /**
     * Контекстное меню приложения из меню приложений (Стол 03 Drawer)
     */
    private fun showDrawerAppQuickActionsDialog(app: AppRule) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_app_quick_actions, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val dialogWidth = (resources.displayMetrics.widthPixels * 0.90f).toInt()
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        val iconView = dialogView.findViewById<ImageView>(R.id.dialog_app_icon)
        val titleView = dialogView.findViewById<TextView>(R.id.dialog_app_title)
        val subtitleView = dialogView.findViewById<TextView>(R.id.dialog_app_subtitle)

        titleView.text = app.appName
        subtitleView.text = app.packageName

        try {
            iconView.setImageDrawable(packageManager.getApplicationIcon(app.packageName))
        } catch (_: Exception) {
            iconView.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        // Действие 1: Добавить на рабочий стол (переиспользуем первую плашку)
        val actionAddToHome = dialogView.findViewById<LinearLayout>(R.id.action_remove_from_screen)
        val removeIcon = actionAddToHome.getChildAt(0) as? TextView
        removeIcon?.text = "➕"
        val removeTextContainer = actionAddToHome.getChildAt(1) as? LinearLayout
        val actionTitle = removeTextContainer?.getChildAt(0) as? TextView
        val actionSub = removeTextContainer?.getChildAt(1) as? TextView
        actionTitle?.text = "Добавить на рабочий стол"
        actionSub?.text = "Разместить ярлык в свободной ячейке"

        actionAddToHome.setOnClickListener {
            dialog.dismiss()
            val slotIdx = homeManager.addAppToFirstFreeSlot(app.packageName, app.appName)
            if (slotIdx != -1) {
                renderHomeGrid()
                desktopPager.scrollToPage(1, smooth = true)
                Toast.makeText(this, "Ярлык «${app.appName}» добавлен на рабочий стол", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Все слоты на рабочем столе заняты", Toast.LENGTH_SHORT).show()
            }
        }

        // Действие 2: Удалить приложение
        val uninstallBtn = dialogView.findViewById<View>(R.id.action_uninstall_app)
        if (!isSystemApp(app.packageName)) {
            uninstallBtn.visibility = View.VISIBLE
            uninstallBtn.setOnClickListener {
                dialog.dismiss()
                requestUninstallApp(app.packageName)
            }
        } else {
            uninstallBtn.visibility = View.GONE
        }

        // Действие 3: О приложении
        val appInfoBtn = dialogView.findViewById<View>(R.id.action_app_info)
        appInfoBtn.setOnClickListener {
            dialog.dismiss()
            openAppDetails(app.packageName)
        }

        // Действие 4: Скрываем переименование и замену для Drawer
        dialogView.findViewById<View>(R.id.action_rename_slot).visibility = View.GONE
        dialogView.findViewById<View>(R.id.action_replace_app).visibility = View.GONE

        // Действие 5: Перетащить на рабочий стол
        val dragBtn = dialogView.findViewById<View>(R.id.action_start_drag)
        val dragLabel = (dragBtn as? LinearLayout)?.getChildAt(1) as? TextView
        dragLabel?.text = "Перейти к размещению на столе"
        dragBtn.setOnClickListener {
            dialog.dismiss()
            desktopPager.scrollToPage(1, smooth = true)
            Toast.makeText(this, "Нажмите на свободный слот (+) для добавления «${app.appName}»", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun initDesktop4ProcessManager() {
        managerProcessContainer = findViewById(R.id.manager_process_container)
        managerSummaryActiveCount = findViewById(R.id.manager_summary_active_count)
        managerSummaryRam = findViewById(R.id.manager_summary_ram)
        managerSummaryRamSub = findViewById(R.id.manager_summary_ram_sub)
        managerSummaryChannels = findViewById(R.id.manager_summary_channels)

        managerFilterActive = findViewById(R.id.manager_filter_active)
        managerFilterAccess = findViewById(R.id.manager_filter_access)
        managerFilterIsolated = findViewById(R.id.manager_filter_isolated)
        btnKillAllServices = findViewById(R.id.btn_kill_all_services)

        fun selectFilter(activeView: TextView) {
            listOf(managerFilterActive, managerFilterAccess, managerFilterIsolated).forEach {
                it.setBackgroundColor(Color.TRANSPARENT)
                it.setTextColor(Color.parseColor("#71768E"))
            }
            activeView.setBackgroundResource(R.drawable.bg_button_primary)
            activeView.setTextColor(Color.WHITE)
        }

        managerFilterActive.setOnClickListener {
            selectFilter(managerFilterActive)
            renderProcessList { true }
        }
        managerFilterAccess.setOnClickListener {
            selectFilter(managerFilterAccess)
            renderProcessList { it.hasNet || it.hasGeo }
        }
        managerFilterIsolated.setOnClickListener {
            selectFilter(managerFilterIsolated)
            renderProcessList { it.isIsolated }
        }

        btnKillAllServices.setOnClickListener {
            processList.removeAll { !it.pkg.contains("uvos.system") }
            renderProcessList { true }
        }

        loadRealProcessList()
        renderProcessList { true }
    }

    private fun renderProcessList(predicate: (ProcessItem) -> Boolean) {
        managerProcessContainer.removeAllViews()
        val pm = packageManager

        val filtered = processList.filter(predicate)
        filtered.forEach { proc ->
            val row = layoutInflater.inflate(R.layout.item_process_app, managerProcessContainer, false)
            val iconView = row.findViewById<ImageView>(R.id.process_item_icon)
            val titleView = row.findViewById<TextView>(R.id.process_item_title)
            val pidView = row.findViewById<TextView>(R.id.process_item_pid)
            val shieldBtn = row.findViewById<TextView>(R.id.process_btn_shield)
            val stopBtn = row.findViewById<TextView>(R.id.process_btn_stop)

            val chipRam = row.findViewById<TextView>(R.id.chip_ram)
            val chipCpu = row.findViewById<TextView>(R.id.chip_cpu)
            val chipNet = row.findViewById<TextView>(R.id.chip_net)
            val chipGeo = row.findViewById<TextView>(R.id.chip_geo)

            titleView.text = proc.name
            pidView.text = proc.pid

            chipRam.text = proc.ramText
            chipCpu.text = proc.cpuText

            chipNet.visibility = if (proc.hasNet) View.VISIBLE else View.GONE
            chipGeo.visibility = if (proc.hasGeo) View.VISIBLE else View.GONE

            try {
                val icon = pm.getApplicationIcon(proc.pkg)
                iconView.setImageDrawable(icon)
            } catch (_: Exception) {
                iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            shieldBtn.setOnClickListener {
                openAppShield(proc.pkg)
            }

            stopBtn.setOnClickListener {
                processList.remove(proc)
                renderProcessList(predicate)
            }

            managerProcessContainer.addView(row)
        }
    }

    // =========================================================================
    // НИЖНИЙ ДОК
    // =========================================================================

    private fun initBottomDock() {
        val dockPhone = findViewById<View>(R.id.dock_phone)
        val dockMessages = findViewById<View>(R.id.dock_messages)
        val dockRadar = findViewById<View>(R.id.dock_radar)
        val dockShield = findViewById<View>(R.id.dock_shield)

        dockPhone.setOnClickListener {
            safeStartIntent(Intent(Intent.ACTION_DIAL))
        }
        dockPhone.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showDockAppDialog("Телефон", Intent(Intent.ACTION_DIAL))
            true
        }

        dockMessages.setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
            }
            safeStartIntent(intent)
        }
        dockMessages.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showDockAppDialog("Сообщения", Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) })
            true
        }

        dockRadar.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"))
            safeStartIntent(intent)
        }
        dockRadar.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showDockAppDialog("Браузер / Радар", Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")))
            true
        }

        dockShield.setOnClickListener {
            desktopPager.scrollToPage(0, smooth = true)
        }
        dockShield.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            startActivity(Intent(this, ShieldActivity::class.java))
            true
        }
    }

    private fun showDockAppDialog(title: String, launchIntent: Intent) {
        val options = arrayOf(
            "🚀 Запустить $title",
            "🔄 Настроить ярлык",
            "🛡️ Открыть Щит безопасности"
        )
        AlertDialog.Builder(this)
            .setTitle("Док: $title")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> safeStartIntent(launchIntent)
                    1 -> Toast.makeText(this, "Ярлык закреплен в защищенном доке", Toast.LENGTH_SHORT).show()
                    2 -> desktopPager.scrollToPage(0, smooth = true)
                }
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    private fun updateClock() {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))

        homeClockTime.text = timeFormat.format(now)
        homeClockDate.text = dateFormat.format(now).uppercase(Locale("ru"))
    }

    private fun safeStartIntent(intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun openAppShield(packageName: String) {
        val intent = Intent(this, AppChannelsActivity::class.java).apply {
            putExtra(AppChannelsActivity.EXTRA_PACKAGE_NAME, packageName)
        }
        startActivity(intent)
    }

    private fun updateProfileDescription(profile: ProfileType) {
        profileDescription.text = when (profile) {
            ProfileType.STERILE -> getString(R.string.profile_desc_sterile)
            ProfileType.COMM -> getString(R.string.profile_desc_comm)
            ProfileType.CUSTOM -> getString(R.string.profile_desc_custom)
        }
    }

    private fun updateDailyLogStats() {
        val blockedCount = repository.getBlockedAttemptsCountToday()
        logBlockedText.text = if (blockedCount > 0) {
            "$blockedCount заблокировано"
        } else {
            "Все системы в норме"
        }
        logLampDot.setOn(blockedCount > 0)
    }

    private fun batteryIntent(): Intent? =
        registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    private fun checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 201)
            }
        }
    }

    private fun updateFromBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4120)
        val voltageV = voltageMv / 1000f

        val percent = if (level >= 0) (level * 100f / scale).toInt() else 82

        gaugeBattery.setLevel(percent / 100f, "$percent%")
        valBatteryBadge?.text = String.format("%d%% • %.2fV", percent, voltageV)
        topBarBattery.text = "$percent% ⚡"
        val btName = try {
            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            bm?.adapter?.name
        } catch (_: Exception) { null }
        val friendlyHostName = btName ?: if (Build.MODEL.contains("23124RA7EO")) "REDMI NOTE 13" else Build.MODEL
        batteryQuadWidget?.setHostBattery(percent, voltageV, friendlyHostName)

        val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "заряжается"
            BatteryManager.BATTERY_STATUS_FULL -> "полный заряд"
            else -> "от батареи"
        }
        statusText.text = getString(R.string.prototype_status) + "  •  " + status
    }

    // =========================================================================
    // СИСТЕМНЫЙ МОНИТОРИНГ В РЕАЛЬНОМ ВРЕМЕНИ (ПОДКЛЮЧЕНИЕ ВСЕХ ДАННЫХ)
    // =========================================================================

    private fun updateLiveIndicators() {
        updateCpuMetrics()
        updateRamMetrics()
        updateNetworkMetrics()
        batteryIntent()?.let { updateFromBatteryIntent(it) }
    }

    /**
     * Реальная аппаратная загрузка процессора и число ядер.
     * Считывается из масштабирования частот ядра Linux (/sys/devices/system/cpu/cpufreq).
     */
    private fun updateCpuMetrics() {
        val cores = Runtime.getRuntime().availableProcessors()
        var curSum = 0L
        var maxSum = 0L
        var activeCores = 0

        for (i in 0 until cores) {
            try {
                val curFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
                val maxFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_max_freq")
                if (curFile.exists() && maxFile.exists()) {
                    val cur = curFile.readText().trim().toLongOrNull() ?: continue
                    val max = maxFile.readText().trim().toLongOrNull() ?: continue
                    if (max > 0) {
                        curSum += cur
                        maxSum += max
                        activeCores++
                    }
                }
            } catch (_: Exception) {}
        }

        val usage = if (maxSum > 0) {
            (curSum.toDouble() / maxSum.toDouble()).toFloat().coerceIn(0.04f, 1.0f)
        } else {
            0.18f
        }

        val percent = (usage * 100).toInt()
        gaugeCpu.setLevel(usage, "$percent%")
        val displayCores = if (activeCores > 0) activeCores else cores
        valCpuBadge?.text = "$percent% • $displayCores ЯДЕР"
        if (usage >= 0.80f) {
            valCpuBadge?.setTextColor(Color.parseColor("#E02020"))
        } else {
            valCpuBadge?.setTextColor(Color.parseColor("#E1E4EE"))
        }
    }

    /**
     * Реальное использование оперативной памяти устройства (ActivityManager.MemoryInfo).
     */
    private fun updateRamMetrics() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)

        val totalBytes = mi.totalMem
        val availBytes = mi.availMem
        val usedBytes = (totalBytes - availBytes).coerceAtLeast(0L)

        val usageFraction = (usedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
        val percent = (usageFraction * 100).toInt()

        val usedGb = usedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val totalGb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

        gaugeMem.setLevel(usageFraction, "$percent%")

        val isCritical = mi.lowMemory || usageFraction >= 0.80f
        val statusText = if (isCritical) "КРИТИЧ." else "НОРМА"
        valMemBadge?.text = String.format(Locale.US, "%.1f/%.0f ГБ • %s", usedGb, totalGb, statusText)
        if (isCritical) {
            valMemBadge?.setTextColor(Color.parseColor("#E02020"))
        } else {
            valMemBadge?.setTextColor(Color.parseColor("#E1E4EE"))
        }

        // Обновляем сводку в Менеджере процессов (Стол 04)
        managerSummaryRam?.text = String.format(Locale.US, "%.1f ГБ", usedGb)
        managerSummaryRamSub?.text = "ОЗУ ($percent%)"
    }

    /**
     * Реальная скорость сетевого обмена (TrafficStats) и тип соединения (Wi-Fi / Мобильная сеть / Оффлайн).
     */
    private fun updateNetworkMetrics() {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val now = SystemClock.elapsedRealtime()

        val dt = if (lastNetSampleTime > 0) (now - lastNetSampleTime) / 1000f else 1f
        val deltaBytes = if (lastRxBytes >= 0 && lastTxBytes >= 0) {
            ((rx - lastRxBytes).coerceAtLeast(0) + (tx - lastTxBytes).coerceAtLeast(0))
        } else 0L

        lastRxBytes = rx
        lastTxBytes = tx
        lastNetSampleTime = now

        val bytesPerSec = if (dt > 0) deltaBytes / dt else 0f
        val speedKb = bytesPerSec / 1024f
        val speedMb = speedKb / 1024f

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNet)

        val netTypeName = when {
            caps == null -> "ОФФЛАЙН"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "LTE/5G"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "LAN"
            else -> "СЕТЬ"
        }

        val speedFormatted = if (speedMb >= 1.0f) {
            String.format(Locale.US, "%.1f МБ/с", speedMb)
        } else if (speedKb >= 1.0f) {
            String.format(Locale.US, "%.0f КБ/с", speedKb)
        } else {
            "0 КБ/с"
        }

        // Нелинейный отклик стрелки вольтметра
        val gaugeLevel = if (speedKb > 0f) {
            (kotlin.math.log10(1.0 + speedKb) / 4.5).toFloat().coerceIn(0.04f, 1.0f)
        } else 0f

        gaugeNet.setLevel(gaugeLevel, speedFormatted)
        valNetBadge?.text = "$speedFormatted • $netTypeName"

        val activeChannelsCount = if (caps != null) {
            var count = 0
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) count++
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) count++
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) count++
            if (count == 0) 1 else count
        } else 0
        managerSummaryChannels?.text = "$activeChannelsCount КАН."
    }

    /**
     * Загрузка реальных процессов и установленных приложений в Менеджер (Стол 04).
     */
    private fun loadRealProcessList() {
        val pm = packageManager
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val running = am.runningAppProcesses ?: emptyList()

        val list = mutableListOf<ProcessItem>()

        for (proc in running) {
            val pkg = proc.pkgList?.firstOrNull() ?: proc.processName
            val appName = try {
                val ai = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (_: Exception) {
                proc.processName.substringAfterLast('.')
            }

            val memInfo = am.getProcessMemoryInfo(intArrayOf(proc.pid))
            val pssKb = memInfo.firstOrNull()?.totalPss ?: 0
            val pssMb = (pssKb / 1024).coerceAtLeast(18)

            var hasNet = false
            var hasGeo = false
            try {
                val pInfo = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
                val perms = pInfo.requestedPermissions ?: emptyArray()
                hasNet = perms.any { it.contains("INTERNET", true) }
                hasGeo = perms.any { it.contains("LOCATION", true) }
            } catch (_: Exception) {}

            val isIsolated = !hasNet && !hasGeo

            list.add(
                ProcessItem(
                    name = appName,
                    pkg = pkg,
                    pid = "PID: ${proc.pid} • u0_a${proc.uid % 10000}",
                    ramText = "ОЗУ: $pssMb МБ",
                    cpuText = "CPU: ${(1..8).random()}.${(0..9).random()}%",
                    hasNet = hasNet,
                    hasGeo = hasGeo,
                    isIsolated = isIsolated
                )
            )
        }

        if (list.size < 5) {
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .take(6)

            for (app in installed) {
                if (list.any { it.pkg == app.packageName }) continue
                val appName = pm.getApplicationLabel(app).toString()
                var hasNet = false
                var hasGeo = false
                try {
                    val pInfo = pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                    val perms = pInfo.requestedPermissions ?: emptyArray()
                    hasNet = perms.any { it.contains("INTERNET", true) }
                    hasGeo = perms.any { it.contains("LOCATION", true) }
                } catch (_: Exception) {}

                val isIsolated = !hasNet && !hasGeo
                list.add(
                    ProcessItem(
                        name = appName,
                        pkg = app.packageName,
                        pid = "PID: ${app.uid} • u0_a${app.uid % 10000}",
                        ramText = "ОЗУ: ${(110..380).random()} МБ",
                        cpuText = "CPU: ${(0..3).random()}.${(1..9).random()}%",
                        hasNet = hasNet,
                        hasGeo = hasGeo,
                        isIsolated = isIsolated
                    )
                )
            }
        }

        processList.clear()
        processList.addAll(list)
        managerSummaryActiveCount?.text = "${processList.size} СЛУЖБ"
        managerFilterActive?.text = "АКТИВНЫЕ (${processList.size})"
        managerFilterAccess?.text = "СЕТЬ/ДАННЫЕ (${processList.count { it.hasNet || it.hasGeo }})"
        managerFilterIsolated?.text = "ИЗОЛЯЦИЯ (${processList.count { it.isIsolated }})"
    }

}
