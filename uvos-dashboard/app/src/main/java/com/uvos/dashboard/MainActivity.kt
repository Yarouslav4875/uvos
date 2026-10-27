package com.uvos.dashboard

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.uvos.dashboard.model.ProfileType
import com.uvos.dashboard.repository.RuleRepositoryImpl

/**
 * UVOS Dashboard - главная приборная панель системы.
 */
class MainActivity : Activity() {

    private lateinit var batteryGauge: GaugeView
    private lateinit var tempGauge: GaugeView
    private lateinit var statusText: TextView
    private lateinit var profileSelector: SegmentedControl
    private lateinit var profileDescription: TextView
    private lateinit var logCard: LinearLayout
    private lateinit var logLampDot: LampView
    private lateinit var logBlockedText: TextView
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
        statusText = findViewById(R.id.status_text)
        profileSelector = findViewById(R.id.profile_selector)
        profileDescription = findViewById(R.id.profile_description)
        logCard = findViewById(R.id.card_daily_log)
        logLampDot = findViewById(R.id.log_lamp_dot)
        logBlockedText = findViewById(R.id.log_blocked_text)

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
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
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
        if (level >= 0) {
            val percent = (level * 100f / scale).toInt()
            batteryGauge.setLevel(percent / 100f, "$percent%")
        }

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        if (tempTenths >= 0) {
            val temp = tempTenths / 10f
            val clamped = temp.coerceIn(tempMin, tempMax)
            val fraction = (clamped - tempMin) / (tempMax - tempMin)
            tempGauge.setLevel(fraction, String.format("%.1f°C", temp))
        }

        val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "заряжается"
            BatteryManager.BATTERY_STATUS_FULL -> "полный заряд"
            else -> "от батареи"
        }
        statusText.text = getString(R.string.prototype_status) + "  •  " + status
    }
}
