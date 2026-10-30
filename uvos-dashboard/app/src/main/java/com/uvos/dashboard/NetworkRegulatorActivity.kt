package com.uvos.dashboard

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.uvos.dashboard.model.NetworkConfig
import com.uvos.dashboard.model.NetworkMode
import com.uvos.dashboard.model.WhitelistDomain
import com.uvos.dashboard.repository.RuleRepositoryImpl

/**
 * UVOS NetworkRegulatorActivity - подэкран Сетевого регулятора (DESIGN.md §7.3, docs/NETWORK-REGULATOR.md).
 */
class NetworkRegulatorActivity : Activity() {

    private lateinit var trafficGauge: GaugeView
    private lateinit var modeSelector: SegmentedControl
    private lateinit var limitSlider: SliderView
    private lateinit var whitelistCard: LinearLayout
    private lateinit var domainsContainer: LinearLayout
    private lateinit var domainInput: EditText
    private lateinit var btnAddDomain: Button
    private lateinit var toggleCharging: ToggleView

    private lateinit var repository: RuleRepositoryImpl
    private var packageName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_regulator)

        repository = RuleRepositoryImpl.getInstance(this)
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""

        trafficGauge = findViewById(R.id.net_traffic_gauge)
        modeSelector = findViewById(R.id.net_mode_selector)
        limitSlider = findViewById(R.id.net_limit_slider)
        whitelistCard = findViewById(R.id.net_whitelist_card)
        domainsContainer = findViewById(R.id.net_domains_container)
        domainInput = findViewById(R.id.net_domain_input)
        btnAddDomain = findViewById(R.id.net_btn_add_domain)
        toggleCharging = findViewById(R.id.net_toggle_charging)

        val modeNames = listOf(
            getString(R.string.net_mode_closed),
            getString(R.string.net_mode_whitelist),
            getString(R.string.net_mode_open)
        )

        val app = repository.getAppRule(packageName)
        if (app == null) {
            finish()
            return
        }

        val netConfig = app.networkConfig

        // 1. Селектор режимов сети (DESIGN.md §3.10)
        val defaultModeIdx = when (netConfig.mode) {
            NetworkMode.FULLY_CLOSED -> 0
            NetworkMode.WHITELIST -> 1
            NetworkMode.OPEN -> 2
        }
        modeSelector.setItems(modeNames, defaultModeIdx)
        updateModeVisibility(netConfig.mode)

        modeSelector.onSegmentSelected = onSelect@{ idx ->
            val newMode = when (idx) {
                0 -> NetworkMode.FULLY_CLOSED
                1 -> NetworkMode.WHITELIST
                else -> NetworkMode.OPEN
            }
            val currentApp = repository.getAppRule(packageName) ?: return@onSelect
            val updated = currentApp.networkConfig.copy(mode = newMode)
            repository.updateNetworkConfig(packageName, updated)
            updateModeVisibility(newMode)
            updateTrafficGauge(updated)
        }

        // 2. Слайдер суточного лимита
        limitSlider.valueFormatter = { fraction ->
            val mb = (50 + fraction * 950).toInt()
            "$mb МБ"
        }
        val currentLimitMb = (netConfig.dailyLimitBytes / (1024L * 1024L)).toFloat()
        val sliderFrac = ((currentLimitMb - 50f) / 950f).coerceIn(0f, 1f)
        limitSlider.setProgress(sliderFrac)

        limitSlider.onProgressChanged = onProgress@{ frac ->
            val limitMb = (50 + frac * 950).toLong()
            val currentApp = repository.getAppRule(packageName) ?: return@onProgress
            val updated = currentApp.networkConfig.copy(dailyLimitBytes = limitMb * 1024L * 1024L)
            repository.updateNetworkConfig(packageName, updated)
            updateTrafficGauge(updated)
        }

        // 3. Тумблер зарядки
        toggleCharging.setChecked(netConfig.chargingOnly)
        toggleCharging.onToggleChanged = onToggle@{ isCharging ->
            val currentApp = repository.getAppRule(packageName) ?: return@onToggle
            val updated = currentApp.networkConfig.copy(chargingOnly = isCharging)
            repository.updateNetworkConfig(packageName, updated)
        }

        // 4. Добавление домена в белый список
        btnAddDomain.setOnClickListener {
            val text = domainInput.text.toString().trim()
            if (text.isNotEmpty()) {
                val currentApp = repository.getAppRule(packageName) ?: return@setOnClickListener
                val currentWl = currentApp.networkConfig.whitelist.toMutableList()
                if (!currentWl.any { it.domain.equals(text, ignoreCase = true) }) {
                    currentWl.add(WhitelistDomain(text, true))
                    val updated = currentApp.networkConfig.copy(whitelist = currentWl)
                    repository.updateNetworkConfig(packageName, updated)
                    domainInput.text.clear()
                    renderDomains(updated.whitelist)
                }
            }
        }

        updateTrafficGauge(netConfig)
        renderDomains(netConfig.whitelist)
    }

    private fun updateModeVisibility(mode: NetworkMode) {
        whitelistCard.visibility = if (mode == NetworkMode.WHITELIST) View.VISIBLE else View.GONE
    }

    private fun updateTrafficGauge(config: NetworkConfig) {
        val usedMb = config.usedBytesToday / (1024L * 1024L)
        val limitMb = config.dailyLimitBytes / (1024L * 1024L)
        val fraction = if (limitMb > 0) (usedMb.toFloat() / limitMb.toFloat()).coerceIn(0f, 1f) else 0f
        trafficGauge.setLevel(fraction, "$usedMb / $limitMb МБ")
    }

    private fun renderDomains(whitelist: List<WhitelistDomain>) {
        domainsContainer.removeAllViews()
        whitelist.forEachIndexed { index, item ->
            val row = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (44 * resources.displayMetrics.density).toInt()
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_tile_clickable)
                setPadding(
                    (8 * resources.displayMetrics.density).toInt(), 0,
                    (8 * resources.displayMetrics.density).toInt(), 0
                )
            }

            val domainText = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = item.domain
                setTextColor(getColor(if (item.isEnabled) R.color.uvos_text else R.color.uvos_text_dim))
                textSize = 14f
            }

            val lamp = LampView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (16 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt()
                )
                setOn(item.isEnabled)
            }

            row.addView(domainText)
            row.addView(lamp)

            row.setOnClickListener {
                val currentApp = repository.getAppRule(packageName) ?: return@setOnClickListener
                val newWl = currentApp.networkConfig.whitelist.toMutableList()
                val toggled = item.copy(isEnabled = !item.isEnabled)
                newWl[index] = toggled
                val updated = currentApp.networkConfig.copy(whitelist = newWl)
                repository.updateNetworkConfig(packageName, updated)
                renderDomains(newWl)
            }

            domainsContainer.addView(row)
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }
}
