package com.uvos.dashboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.uvos.dashboard.repository.RuleRepositoryImpl

/**
 * UVOS ShieldActivity - экран Контрольного щитка (DESIGN.md §7.1).
 *
 * Отображает:
 * 1. Шапку-прибор «ACCESS» (суммарно открыто каналов во всей системе).
 * 2. Список приложений с количеством открытых каналов («0 кан.»).
 * 3. Кнопку перехода в Журнал попыток.
 */
class ShieldActivity : Activity() {

    private lateinit var accessGauge: GaugeView
    private lateinit var appsList: LinearLayout
    private lateinit var repository: RuleRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shield)

        repository = RuleRepositoryImpl.getInstance(this)
        accessGauge = findViewById(R.id.shield_gauge)
        appsList = findViewById(R.id.shield_apps_list)

        findViewById<Button>(R.id.btn_open_log).setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        renderApps()
    }

    private fun renderApps() {
        appsList.removeAllViews()
        val apps = repository.getAllApps()
        val totalChannels = apps.size * 13
        val totalOpen = repository.getTotalOpenChannels()

        // Обновление прибора ACCESS (DESIGN.md §7.1)
        val fraction = if (totalChannels > 0) totalOpen / totalChannels.toFloat() else 0f
        accessGauge.setLevel(fraction, "$totalOpen / $totalChannels")

        apps.forEach { app ->
            val row = layoutInflater.inflate(R.layout.item_app_row, appsList, false)
            val badge = row.findViewById<TextView>(R.id.app_icon_badge)
            val title = row.findViewById<TextView>(R.id.app_title)
            val pkg = row.findViewById<TextView>(R.id.app_package)
            val openChannels = row.findViewById<TextView>(R.id.app_open_channels)

            badge.text = app.appName.firstOrNull()?.uppercase() ?: "A"
            title.text = app.appName
            pkg.text = app.packageName

            val openCount = app.openChannelsCount
            openChannels.text = "$openCount кан."
            if (openCount > 0) {
                openChannels.setTextColor(getColor(R.color.uvos_red))
            } else {
                openChannels.setTextColor(getColor(R.color.uvos_text_dim))
            }

            row.setOnClickListener {
                val intent = Intent(this, AppChannelsActivity::class.java).apply {
                    putExtra(AppChannelsActivity.EXTRA_PACKAGE_NAME, app.packageName)
                }
                startActivity(intent)
            }

            appsList.addView(row)

            // Разделитель строк 1dp divider (#2A2C36)
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (1 * resources.displayMetrics.density).toInt()
                )
                setBackgroundColor(getColor(R.color.uvos_divider))
            }
            appsList.addView(divider)
        }
    }
}
