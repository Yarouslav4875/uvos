package com.uvos.dashboard

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.uvos.dashboard.model.ProfileType
import com.uvos.dashboard.repository.RuleRepositoryImpl

/**
 * UVOS Dashboard - главная приборная панель и домашняя оболочка системы (Launcher).
 */
class MainActivity : Activity() {

    private lateinit var batteryGauge: GaugeView
    private lateinit var tempGauge: GaugeView
    private var valBatteryBadge: TextView? = null
    private var valTempBadge: TextView? = null
    private lateinit var statusText: TextView
    private lateinit var profileSelector: SegmentedControl
    private lateinit var profileDescription: TextView
    private lateinit var logCard: LinearLayout
    private lateinit var logLampDot: LampView
    private lateinit var logBlockedText: TextView
    private lateinit var launcherAppsContainer: LinearLayout
    private lateinit var launcherAppsCount: TextView
    private lateinit var repository: RuleRepositoryImpl

    private val tempMin = 20f
    private val tempMax = 50f

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateFrom(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = RuleRepositoryImpl.getInstance(this)

        batteryGauge = findViewById(R.id.gauge_battery)
        tempGauge = findViewById(R.id.gauge_temp)
        valBatteryBadge = findViewById(R.id.val_battery_badge)
        valTempBadge = findViewById(R.id.val_temp_badge)
        statusText = findViewById(R.id.status_text)
        profileSelector = findViewById(R.id.profile_selector)
        profileDescription = findViewById(R.id.profile_description)
        logCard = findViewById(R.id.card_daily_log)
        logLampDot = findViewById(R.id.log_lamp_dot)
        logBlockedText = findViewById(R.id.log_blocked_text)
        launcherAppsContainer = findViewById(R.id.launcher_apps_container)
        launcherAppsCount = findViewById(R.id.launcher_apps_count)

        // Настройка селектора профилей (CONTROL-SCHEME.md §4)
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
            renderLauncherApps()
        }

        // Журнал попыток
        logCard.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        // Щиток доступа
        findViewById<Button>(R.id.open_shield).setOnClickListener {
            startActivity(Intent(this, ShieldActivity::class.java))
        }

        batteryIntent()?.let { updateFrom(it) }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        updateDailyLogStats()
        renderLauncherApps()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Возврат на домашний экран оболочки
        renderLauncherApps()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Как домашняя оболочка (Home Shell) UVOS не закрывается по клавише Back
    }

    private fun renderLauncherApps() {
        launcherAppsContainer.removeAllViews()
        val apps = repository.getAllApps()
        launcherAppsCount.text = "${apps.size} ПРИЛОЖЕНИЙ"

        val pm = packageManager

        apps.forEach { app ->
            val row = layoutInflater.inflate(R.layout.item_launcher_app, launcherAppsContainer, false)
            val iconView = row.findViewById<ImageView>(R.id.launcher_app_icon)
            val titleView = row.findViewById<TextView>(R.id.launcher_app_title)
            val pkgView = row.findViewById<TextView>(R.id.launcher_app_package)
            val statusBadge = row.findViewById<TextView>(R.id.launcher_app_status)
            val shieldBtn = row.findViewById<TextView>(R.id.launcher_btn_shield)

            titleView.text = app.appName
            pkgView.text = app.packageName

            // Загрузка реальной иконки приложения из системы
            try {
                val icon = pm.getApplicationIcon(app.packageName)
                iconView.setImageDrawable(icon)
            } catch (_: Exception) {
                iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            // Статус изоляции 13 каналов
            val openCount = app.openChannelsCount
            if (openCount == 0) {
                statusBadge.text = "0/13 ЗАКРЫТО"
                statusBadge.setTextColor(Color.parseColor("#06B6D4")) // UVOS Cyan
            } else {
                statusBadge.text = "$openCount/13 ОТКРЫТО"
                statusBadge.setTextColor(Color.parseColor("#EF4444")) // Alert Red
            }

            // Запуск приложения по нажатию на карточку
            row.setOnClickListener {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                } else {
                    // Если у приложения нет явного лаунчера (системная служба/демо), открываем детальный щиток
                    openAppShield(app.packageName)
                }
            }

            // Долгое нажатие открывает щиток каналов
            row.setOnLongClickListener {
                openAppShield(app.packageName)
                true
            }

            // Кнопка настройки каналов
            shieldBtn.setOnClickListener {
                openAppShield(app.packageName)
            }

            launcherAppsContainer.addView(row)

            // Разделитель
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (1 * resources.displayMetrics.density).toInt()
                )
                setBackgroundColor(getColor(R.color.uvos_divider))
            }
            launcherAppsContainer.addView(divider)
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

    private fun updateFrom(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4120)
        val voltageV = voltageMv / 1000f

        if (level >= 0) {
            val percent = (level * 100f / scale).toInt()
            batteryGauge.setLevel(percent / 100f, "$percent%")
            valBatteryBadge?.text = String.format("%d%% • %.2fV", percent, voltageV)
        }

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        if (tempTenths >= 0) {
            val temp = tempTenths / 10f
            val clamped = temp.coerceIn(tempMin, tempMax)
            val fraction = (clamped - tempMin) / (tempMax - tempMin)
            tempGauge.setLevel(fraction, String.format("%.1f°C", temp))
            val tempStatus = if (temp > 45f) "ПЕРЕГРЕВ" else "НОРМА"
            valTempBadge?.text = String.format("%.1f°C • %s", temp, tempStatus)
        }

        val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "заряжается"
            BatteryManager.BATTERY_STATUS_FULL -> "полный заряд"
            else -> "от батареи"
        }
        statusText.text = getString(R.string.prototype_status) + "  •  " + status
    }
}
