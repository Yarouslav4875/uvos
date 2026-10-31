package com.uvos.dashboard

import android.app.Activity
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
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var homeClockTime: TextView
    private lateinit var homeClockDate: TextView
    private lateinit var homeManager: HomeManager
    private lateinit var homeRow1: LinearLayout
    private lateinit var homeRow2: LinearLayout
    private lateinit var homeRow3: LinearLayout
    private lateinit var homeRow4: LinearLayout

    // Стол 03: App Drawer
    private lateinit var drawerAppsContainer: LinearLayout

    // Стол 04: Process Manager
    private lateinit var managerProcessContainer: LinearLayout
    private lateinit var managerFilterActive: TextView
    private lateinit var managerFilterAccess: TextView
    private lateinit var managerFilterIsolated: TextView
    private lateinit var btnKillAllServices: Button

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
        updateDailyLogStats()
        renderHomeGrid()
        renderDrawerApps()
        updateClock()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
        unregisterReceiver(timeTickReceiver)
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
            // topBarCenter.text = "" (подпись стола отключена по запросу)
            dotPage1.setBackgroundResource(if (page == 0) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            dotPage2.setBackgroundResource(if (page == 1) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            dotPage3.setBackgroundResource(if (page == 2) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            dotPage4.setBackgroundResource(if (page == 3) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
        }

        desktopPager.onPageSelectedListener = { page ->
            updatePageIndicators(page)
        }

        dotPage1.setOnClickListener { desktopPager.scrollToPage(0) }
        dotPage2.setOnClickListener { desktopPager.scrollToPage(1) }
        dotPage3.setOnClickListener { desktopPager.scrollToPage(2) }
        dotPage4.setOnClickListener { desktopPager.scrollToPage(3) }

        // Стартовое состояние - Стол 01 (Щит)
        updatePageIndicators(0)
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

        btnAddGaugeWidget.setOnClickListener {
            showAddGaugeDialog()
        }

        val onGaugeLongClick = View.OnLongClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            Toast.makeText(this, "Прибор активен. Режим калибровки: авто", Toast.LENGTH_SHORT).show()
            true
        }
        gaugeCpu.setOnLongClickListener(onGaugeLongClick)
        gaugeMem.setOnLongClickListener(onGaugeLongClick)
        gaugeBattery.setOnLongClickListener(onGaugeLongClick)
        gaugeNet.setOnLongClickListener(onGaugeLongClick)

        renderCustomGauges()

        // Значения вольтметров
        gaugeCpu.setLevel(0.42f, "42%")
        valCpuBadge?.text = "42% • 8 ЯДЕР"

        gaugeMem.setLevel(0.84f, "84%")
        valMemBadge?.text = "84% • КРИТИЧ."

        gaugeNet.setLevel(0.18f, "18%")
        valNetBadge?.text = "18% • 1.4 МБ/с"

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
                val item = gaugeWidgetManager.addGauge(type)
                renderCustomGauges()
                Toast.makeText(this, "Прибор «${item.title}» добавлен на панель", Toast.LENGTH_SHORT).show()
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
    // СТОЛ 02: ДОМАШНИЙ ЭКРАН (HOME) С РЕДАКТИРОВАНИЕМ ИКОНОК
    // =========================================================================

    private fun initDesktop2Home() {
        homeClockTime = findViewById(R.id.home_clock_time)
        homeClockDate = findViewById(R.id.home_clock_date)

        homeRow1 = findViewById(R.id.home_row_1)
        homeRow2 = findViewById(R.id.home_row_2)
        homeRow3 = findViewById(R.id.home_row_3)
        homeRow4 = findViewById(R.id.home_row_4)

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

                // Клик - запуск
                itemView.setOnClickListener {
                    handleSlotClick(slot)
                }

                // Долгий тап - меню редактирования как на обычном Android
                itemView.setOnLongClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    showSlotOptionsDialog(slot)
                    true
                }
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
            "SYSTEM_CALC" -> Toast.makeText(this, "Калькулятор UVOS", Toast.LENGTH_SHORT).show()
            "SYSTEM_SETTINGS" -> safeStartIntent(Intent(Settings.ACTION_SETTINGS))
            "SYSTEM_SHIELD" -> startActivity(Intent(this, ShieldActivity::class.java))
            "SYSTEM_LOG" -> startActivity(Intent(this, LogActivity::class.java))
            "SYSTEM_OSC" -> Toast.makeText(this, "Осциллограф: мониторинг радиоканалов активен", Toast.LENGTH_SHORT).show()
            "SYSTEM_TERM" -> Toast.makeText(this, "Терминал UVOS: tty1 готов", Toast.LENGTH_SHORT).show()
            "SYSTEM_PACKAGES" -> desktopPager.scrollToPage(2, smooth = true)
        }
    }

    /**
     * Контекстное меню редактирования иконки (как на обычном Android Launcher)
     */
    private fun showSlotOptionsDialog(slot: HomeSlotItem) {
        val options = mutableListOf<String>()
        options.add("✏️ Переименовать ярлык")
        options.add("🔄 Заменить приложение")
        if (slot.actionType == "APP" && slot.packageName != null) {
            options.add("ℹ️ О приложении")
        }
        options.add("🗑️ Убрать с рабочего стола")
        if (slot.actionType == "APP" && slot.packageName != null) {
            options.add("❌ Удалить приложение")
        }

        AlertDialog.Builder(this)
            .setTitle(slot.title)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "✏️ Переименовать ярлык" -> showRenameDialog(slot)
                    "🔄 Заменить приложение" -> showAppChooserForSlot(slot.slotIndex)
                    "ℹ️ О приложении" -> openAppDetails(slot.packageName)
                    "🗑️ Убрать с рабочего стола" -> {
                        homeManager.clearSlot(slot.slotIndex)
                        renderHomeGrid()
                        Toast.makeText(this, "Ярлык удален с рабочего стола", Toast.LENGTH_SHORT).show()
                    }
                    "❌ Удалить приложение" -> requestUninstallApp(slot.packageName)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
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
                Toast.makeText(this, "Ярлык $label добавлен на стол", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this, "Все 16 слотов рабочего стола заняты", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        homeManager.resetToDefaults()
                        renderHomeGrid()
                        Toast.makeText(this, "Иконки сброшены к стандартным", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun openAppDetails(packageName: String?) {
        if (packageName == null) return
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
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

            // Долгий тап в Drawer - меню добавления на рабочий стол как на обычном Android!
            row.setOnLongClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                val options = arrayOf(
                    "➕ Добавить на рабочий стол",
                    "ℹ️ О приложении",
                    "❌ Удалить приложение"
                )
                AlertDialog.Builder(this)
                    .setTitle(app.appName)
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> {
                                val slotIdx = homeManager.addAppToFirstFreeSlot(app.packageName, app.appName)
                                if (slotIdx != -1) {
                                    renderHomeGrid()
                                    Toast.makeText(this, "«${app.appName}» добавлено на рабочий стол", Toast.LENGTH_SHORT).show()
                                    desktopPager.scrollToPage(1, smooth = true)
                                } else {
                                    Toast.makeText(this, "Все слоты заняты! Удалите лишний ярлык на столе", Toast.LENGTH_LONG).show()
                                }
                            }
                            1 -> openAppDetails(app.packageName)
                            2 -> requestUninstallApp(app.packageName)
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
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

    private fun initDesktop4ProcessManager() {
        managerProcessContainer = findViewById(R.id.manager_process_container)
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
            Toast.makeText(this, "Все фоновые службы усыплены. Освобождено 1.5 ГБ ОЗУ.", Toast.LENGTH_LONG).show()
        }

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
                Toast.makeText(this, "Служба ${proc.name} принудительно остановлена", Toast.LENGTH_SHORT).show()
            }

            managerProcessContainer.addView(row)
        }
    }

    // =========================================================================
    // НИЖНИЙ ДОК
    // =========================================================================

    private fun initBottomDock() {
        findViewById<View>(R.id.dock_phone).setOnClickListener {
            safeStartIntent(Intent(Intent.ACTION_DIAL))
        }
        findViewById<View>(R.id.dock_messages).setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
            }
            safeStartIntent(intent)
        }
        findViewById<View>(R.id.dock_radar).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"))
            safeStartIntent(intent)
        }
        findViewById<View>(R.id.dock_shield).setOnClickListener {
            desktopPager.scrollToPage(0, smooth = true)
        }
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
        } catch (e: Exception) {
            Toast.makeText(this, "Приложение недоступно: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun updateFromBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4120)
        val voltageV = voltageMv / 1000f

        val percent = if (level >= 0) (level * 100f / scale).toInt() else 82

        gaugeBattery.setLevel(percent / 100f, "$percent%")
        valBatteryBadge?.text = String.format("%d%% • %.2fV", percent, voltageV)
        topBarBattery.text = "$percent% ⚡"

        val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "заряжается"
            BatteryManager.BATTERY_STATUS_FULL -> "полный заряд"
            else -> "от батареи"
        }
        statusText.text = getString(R.string.prototype_status) + "  •  " + status
    }
}
