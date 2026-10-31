package com.uvos.dashboard

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Button
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
 * - СТОЛ 02: ДОМАШНИЙ ЭКРАН (ДОМ / HOME - ЧАСЫ, СЕТКА 4x4 ИКОНОК, ДОК)
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

    // Стол 02: Home Clock & Grid
    private lateinit var homeClockTime: TextView
    private lateinit var homeClockDate: TextView

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
        setContentView(R.layout.activity_main)

        repository = RuleRepositoryImpl.getInstance(this)

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
            topBarCenter.text = pageTitles.getOrElse(page) { "UVOS-CORE" }
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

        // Установка значений вольтметров по эталону презентации
        gaugeCpu.setLevel(0.42f, "42%")
        valCpuBadge?.text = "42% • 8 ЯДЕР"

        // ОЗУ находится в красной тревожной зоне (84%)
        gaugeMem.setLevel(0.84f, "84%")
        valMemBadge?.text = "84% • КРИТИЧ."

        // Сеть 18%
        gaugeNet.setLevel(0.18f, "18%")
        valNetBadge?.text = "18% • 1.4 МБ/с"

        // Селектор профилей
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

        // Журнал
        logCard.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        // Щиток доступа
        findViewById<Button>(R.id.open_shield).setOnClickListener {
            startActivity(Intent(this, ShieldActivity::class.java))
        }
    }

    // =========================================================================
    // СТОЛ 02: ДОМАШНИЙ ЭКРАН (HOME)
    // =========================================================================

    private fun initDesktop2Home() {
        homeClockTime = findViewById(R.id.home_clock_time)
        homeClockDate = findViewById(R.id.home_clock_date)

        val pm = packageManager

        // Системные иконки UVOS
        findViewById<View>(R.id.app_phone).setOnClickListener {
            safeStartIntent(Intent(Intent.ACTION_DIAL))
        }
        findViewById<View>(R.id.app_messages).setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
            }
            safeStartIntent(intent)
        }
        findViewById<View>(R.id.app_radar).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"))
            safeStartIntent(intent)
        }
        findViewById<View>(R.id.app_camera).setOnClickListener {
            safeStartIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        }
        findViewById<View>(R.id.app_files).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
            }
            safeStartIntent(intent)
        }
        findViewById<View>(R.id.app_player).setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MUSIC)
            }
            safeStartIntent(intent)
        }
        findViewById<View>(R.id.app_calc).setOnClickListener {
            Toast.makeText(this, "Калькулятор UVOS", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.app_settings).setOnClickListener {
            safeStartIntent(Intent(Settings.ACTION_SETTINGS))
        }

        // Нативные приложения с их подлинными значками
        setupNativeAppSlot(
            layoutId = R.id.home_app_tg,
            iconId = R.id.home_icon_tg,
            possiblePackages = listOf("org.telegram.messenger", "org.thunderdog.challegram", "org.telegram.plus"),
            fallbackAction = { Toast.makeText(this, "Telegram не установлен", Toast.LENGTH_SHORT).show() }
        )

        setupNativeAppSlot(
            layoutId = R.id.home_app_maps,
            iconId = R.id.home_icon_maps,
            possiblePackages = listOf("ru.yandex.yandexnavi", "ru.yandex.yandexmaps", "com.google.android.apps.maps"),
            fallbackAction = { Toast.makeText(this, "Карты не установлены", Toast.LENGTH_SHORT).show() }
        )

        setupNativeAppSlot(
            layoutId = R.id.home_app_bank,
            iconId = R.id.home_icon_bank,
            possiblePackages = listOf("ru.sberbankmobile", "com.idamob.tinkoff.android", "ru.vtb24.mobilebanking"),
            fallbackAction = { Toast.makeText(this, "Банк не установлен", Toast.LENGTH_SHORT).show() }
        )

        // Инженерный блок
        findViewById<View>(R.id.home_app_shield).setOnClickListener {
            startActivity(Intent(this, ShieldActivity::class.java))
        }
        findViewById<View>(R.id.home_app_log).setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }
        findViewById<View>(R.id.home_app_osc).setOnClickListener {
            Toast.makeText(this, "Осциллограф: мониторинг радиоканалов активен", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.home_app_term).setOnClickListener {
            Toast.makeText(this, "Терминал UVOS: tty1 готов", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.home_app_packages).setOnClickListener {
            // Переход на Стол 03 (Пакеты / Меню)
            desktopPager.scrollToPage(2, smooth = true)
        }
    }

    private fun setupNativeAppSlot(
        layoutId: Int,
        iconId: Int,
        possiblePackages: List<String>,
        fallbackAction: () -> Unit
    ) {
        val layout = findViewById<View>(layoutId)
        val iconView = findViewById<ImageView>(iconId)
        val pm = packageManager

        var foundPkg: String? = null
        for (pkg in possiblePackages) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                iconView.setImageDrawable(pm.getApplicationIcon(appInfo))
                foundPkg = pkg
                break
            } catch (_: Exception) {
            }
        }

        layout.setOnClickListener {
            if (foundPkg != null) {
                val launchIntent = pm.getLaunchIntentForPackage(foundPkg)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                    return@setOnClickListener
                }
            }
            fallbackAction()
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

            // Загрузка настоящей системной иконки приложения
            try {
                val icon = pm.getApplicationIcon(app.packageName)
                iconView.setImageDrawable(icon)
            } catch (_: Exception) {
                iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            // Бейджи безопасности UnderVoltOS
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

            // Нажатие запускает приложение
            row.setOnClickListener {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                } else {
                    openAppShield(app.packageName)
                }
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
            // Переключает на Стол 01 (Щит)
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
