package com.uvos.dashboard

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * UVOS ShieldActivity - прототип контрольного щитка (DESIGN.md §6).
 *
 * ДЕМО-РЕЖИМ: каналы фиктивные, состояние хранится только в памяти экрана.
 * Это иллюстрация дизайна и логики, не реальная система ограничений.
 * По умолчанию все каналы ЗАКРЫТЫ - принцип "закрытой территории".
 */
class ShieldActivity : Activity() {

    // 13 каналов данных из REQUIREMENTS.md / DESIGN.md §6.2.
    // "Сеть" помечена: в реальной системе это отдельный регулятор (§6.3).
    private val channels = listOf(
        "Время", "Батарея", "Сеть (регулятор)", "Геолокация", "Камера",
        "Микрофон", "Контакты", "СМС", "Звонки", "Файлы", "Датчики",
        "Идентификаторы", "Список приложений"
    )

    private val channelStates = BooleanArray(channels.size) { false }
    private lateinit var accessGauge: GaugeView
    private lateinit var channelsList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shield)

        accessGauge = findViewById(R.id.shield_gauge)
        channelsList = findViewById(R.id.shield_channels)
        accessGauge.setLevel(0f)

        channels.forEachIndexed { index, name ->
            addChannelRow(index, name)
        }
    }

    private fun addChannelRow(index: Int, name: String) {
        val row = layoutInflater.inflate(R.layout.shield_row, channelsList, false)
        val nameView = row.findViewById<TextView>(R.id.channel_name)
        val statusView = row.findViewById<TextView>(R.id.channel_status)
        val lamp = row.findViewById<LampView>(R.id.channel_lamp)

        nameView.text = name
        statusView.text = getString(R.string.channel_closed)

        row.setOnClickListener {
            // ДЕМО: мгновенное переключение. В реальной системе отзыв
            // открытого канала требует диалога подтверждения (§6.2).
            channelStates[index] = !channelStates[index]
            lamp.setOn(channelStates[index])
            statusView.text = getString(
                if (channelStates[index]) R.string.channel_open else R.string.channel_closed
            )
            statusView.setTextColor(
                getColor(if (channelStates[index]) R.color.uvos_text else R.color.uvos_text_dim)
            )
            updateAccessGauge()
        }

        channelsList.addView(row)
        // Разделитель между строками
        val divider = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            )
            setBackgroundColor(getColor(R.color.uvos_background))
        }
        channelsList.addView(divider)
    }

    private fun updateAccessGauge() {
        val open = channelStates.count { it }
        accessGauge.setLevel(open / channels.size.toFloat())
    }
}
