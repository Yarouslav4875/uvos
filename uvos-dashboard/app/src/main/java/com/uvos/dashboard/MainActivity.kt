package com.uvos.dashboard

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.content.Intent

/**
 * UVOS Dashboard - прототип приборной панели.
 *
 * Принцип "закрытой территории": приложение запрашивает НОЛЬ разрешений.
 * Данные батареи читаются из sticky-broadcast системы (ACTION_BATTERY_CHANGED),
 * который доступен без разрешений - это единственный источник данных.
 */
class MainActivity : Activity() {

    private lateinit var batteryGauge: GaugeView
    private lateinit var tempGauge: GaugeView
    private lateinit var statusText: TextView

    // Диапазон шкалы температуры прибора, °C
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

        batteryGauge = findViewById(R.id.gauge_battery)
        tempGauge = findViewById(R.id.gauge_temp)
        statusText = findViewById(R.id.status_text)

        // Первичное значение из sticky-броадкаста (без регистрации)
        batteryIntent()?.let { updateFrom(it) }

        findViewById<Button>(R.id.open_shield).setOnClickListener {
            startActivity(Intent(this, ShieldActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
    }

    private fun batteryIntent(): Intent? =
        registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    private fun updateFrom(intent: Intent) {
        // Заряд: 0..100 %
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level >= 0) {
            val percent = level * 100f / scale
            batteryGauge.setLevel(percent / 100f)
        }

        // Температура: десятые доли градуса Цельсия
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        if (tempTenths >= 0) {
            val temp = tempTenths / 10f
            val clamped = temp.coerceIn(tempMin, tempMax)
            tempGauge.setLevel((clamped - tempMin) / (tempMax - tempMin))
        }

        val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "заряжается"
            BatteryManager.BATTERY_STATUS_FULL -> "полный заряд"
            else -> "от батареи"
        }
        statusText.text = getString(R.string.prototype_status) + "  •  " + status
    }
}
