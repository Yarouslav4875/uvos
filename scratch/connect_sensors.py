import re

main_activity_path = 'uvos-dashboard/app/src/main/java/com/uvos/dashboard/MainActivity.kt'

with open(main_activity_path, 'r', encoding='utf-8') as f:
    code = f.read()

is_crlf = '\r\n' in code
code = code.replace('\r\n', '\n')

# 1. Add fields
fields_target = """    private lateinit var btnAddGaugeWidget: View"""
fields_replacement = """    private lateinit var btnAddGaugeWidget: View
    private lateinit var sensorTelemetryManager: SensorTelemetryManager
    private val customGaugeHolders = mutableListOf<CustomGaugeViewHolder>()

    private data class CustomGaugeViewHolder(
        val item: CustomGaugeItem,
        val gaugeView: GaugeView,
        val valueView: TextView,
        val subtitleView: TextView,
        val badgeView: TextView
    )"""

assert fields_target in code, "fields_target not found"
code = code.replace(fields_target, fields_replacement, 1)

# 2. In onCreate: init sensorTelemetryManager
oncreate_target = """        repository = RuleRepositoryImpl.getInstance(this)
        homeManager = HomeManager(this)

        initTopBarAndPager()"""

oncreate_replacement = """        repository = RuleRepositoryImpl.getInstance(this)
        homeManager = HomeManager(this)
        sensorTelemetryManager = SensorTelemetryManager(this)

        initTopBarAndPager()"""

assert oncreate_target in code, "oncreate_target not found"
code = code.replace(oncreate_target, oncreate_replacement, 1)

# 3. In onResume: startListening
onresume_target = """        bluetoothDeviceManager?.start()
        systemStatsHandler.post(systemStatsRunnable)"""

onresume_replacement = """        bluetoothDeviceManager?.start()
        sensorTelemetryManager.startListening()
        systemStatsHandler.post(systemStatsRunnable)"""

assert onresume_target in code, "onresume_target not found"
code = code.replace(onresume_target, onresume_replacement, 1)

# 4. In onPause: stopListening
onpause_target = """        bluetoothDeviceManager?.stop()
        systemStatsHandler.removeCallbacks(systemStatsRunnable)"""

onpause_replacement = """        bluetoothDeviceManager?.stop()
        sensorTelemetryManager.stopListening()
        systemStatsHandler.removeCallbacks(systemStatsRunnable)"""

assert onpause_target in code, "onpause_target not found"
code = code.replace(onpause_target, onpause_replacement, 1)

# 5. In updateFromBatteryIntent: update cached battery temp & voltage
battery_target = """        val percent = if (level >= 0) (level * 100f / scale).toInt() else 82

        gaugeBattery.setLevel(percent / 100f, "$percent%")"""

battery_replacement = """        val percent = if (level >= 0) (level * 100f / scale).toInt() else 82

        val battTempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280)
        sensorTelemetryManager.cachedBatteryTempC = battTempRaw / 10f
        sensorTelemetryManager.cachedBatteryVoltageV = voltageV
        sensorTelemetryManager.cachedBatteryPercent = percent

        gaugeBattery.setLevel(percent / 100f, "$percent%")"""

assert battery_target in code, "battery_target not found"
code = code.replace(battery_target, battery_replacement, 1)

# 6. In updateLiveIndicators: updateCustomGaugesLive
live_target = """    private fun updateLiveIndicators() {
        updateCpuMetrics()
        updateRamMetrics()
        updateNetworkMetrics()
        batteryIntent()?.let { updateFromBatteryIntent(it) }
    }"""

live_replacement = """    private fun updateLiveIndicators() {
        updateCpuMetrics()
        updateRamMetrics()
        updateNetworkMetrics()
        batteryIntent()?.let { updateFromBatteryIntent(it) }
        updateCustomGaugesLive()
    }"""

assert live_target in code, "live_target not found"
code = code.replace(live_target, live_replacement, 1)

# 7. updateCpuMetrics using real proc/stat
cpu_target = """    private fun updateCpuMetrics() {
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
    }"""

cpu_replacement = """    private fun updateCpuMetrics() {
        val cpuData = sensorTelemetryManager.getCpuUsageLoad()
        gaugeCpu.setLevel(cpuData.usageFraction, cpuData.formattedValue)
        valCpuBadge?.text = cpuData.subtitle
        if (cpuData.usageFraction >= 0.80f) {
            valCpuBadge?.setTextColor(Color.parseColor("#E02020"))
        } else {
            valCpuBadge?.setTextColor(Color.parseColor("#E1E4EE"))
        }
    }"""

assert cpu_target in code, "cpu_target not found"
code = code.replace(cpu_target, cpu_replacement, 1)

# 8. Replace showAddGaugeDialog and renderCustomGauges
dialog_and_render_pattern = r'    private fun showAddGaugeDialog\(\) \{[\s\S]*?    private fun renderCustomGauges\(\) \{[\s\S]*?        customGaugesContainer\.addView\(row\)\n        \}\n    \}'

new_dialog_and_render = """    private fun showAddGaugeDialog() {
        val options = arrayOf(
            "🗄️ Накопитель UFS/Flash (STORAGE)",
            "🌡️ Термопакет SoC и АКБ (THERMAL)",
            "💡 Датчик освещенности (LIGHT LUX)",
            "📐 Акселерометр и перегрузка (ACCEL G)",
            "🔄 Гироскоп и угловая скорость (GYRO)",
            "🖐️ Датчик приближения (PROXIMITY)",
            "🧭 Магнитометр и датчик Холла (MAGNETIC)",
            "⏱️ Барометр и давление (BAROMETER)",
            "📡 Мощность радиочастот (RADIO RF)",
            "🛡️ Шлюз изоляции каналов (SECURITY)"
        )
        AlertDialog.Builder(this)
            .setTitle("Добавить датчик или прибор")
            .setItems(options) { _, which ->
                val type = when (which) {
                    0 -> GaugeWidgetType.STORAGE
                    1 -> GaugeWidgetType.THERMAL
                    2 -> GaugeWidgetType.LIGHT
                    3 -> GaugeWidgetType.ACCELEROMETER
                    4 -> GaugeWidgetType.GYROSCOPE
                    5 -> GaugeWidgetType.PROXIMITY
                    6 -> GaugeWidgetType.MAGNETIC
                    7 -> GaugeWidgetType.BAROMETER
                    8 -> GaugeWidgetType.RADIO
                    else -> GaugeWidgetType.SECURITY
                }
                gaugeWidgetManager.addGauge(type)
                renderCustomGauges()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun renderCustomGauges() {
        customGaugesContainer.removeAllViews()
        customGaugeHolders.clear()
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
            gaugeView.setTitle(item.gaugeTitle)

            val holder = CustomGaugeViewHolder(item, gaugeView, valueView, subtitleView, badgeView)
            customGaugeHolders.add(holder)
            updateCustomGaugeView(holder)

            btnRemove.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Удалить датчик?")
                    .setMessage("Убрать «${item.title}» с панели приборов?")
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

    private fun updateCustomGaugesLive() {
        customGaugeHolders.forEach { holder ->
            updateCustomGaugeView(holder)
        }
    }

    private fun updateCustomGaugeView(holder: CustomGaugeViewHolder) {
        val item = holder.item
        val gaugeView = holder.gaugeView
        val valueView = holder.valueView
        val subtitleView = holder.subtitleView
        val badgeView = holder.badgeView

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
                val data = sensorTelemetryManager.getThermalData()
                gaugeView.setLevel(data.levelFraction, String.format(Locale.US, "%.0f°C", data.socTempC))
                valueView.text = data.formattedValue
                subtitleView.text = data.subtitle
                badgeView.text = data.statusBadge
                badgeView.setTextColor(data.statusColor)
            }
            GaugeWidgetType.LIGHT -> {
                val data = sensorTelemetryManager.getLightData()
                gaugeView.setLevel(data.levelFraction, "${data.lux.toInt()} LX")
                valueView.text = data.formattedValue
                subtitleView.text = data.subtitle
                badgeView.text = "TMD2755 • ОСВЕЩЕННОСТЬ"
                badgeView.setTextColor(Color.parseColor("#F59E0B"))
            }
            GaugeWidgetType.ACCELEROMETER -> {
                val data = sensorTelemetryManager.getAccelData()
                gaugeView.setLevel(data.levelFraction, String.format(Locale.US, "%.2fG", data.gForce))
                valueView.text = data.formattedValue
                subtitleView.text = data.subtitle
                badgeView.text = "LSM6DSO • АКСЕЛЕРОМЕТР"
                badgeView.setTextColor(Color.parseColor("#06B6D4"))
            }
            GaugeWidgetType.GYROSCOPE -> {
                val data = sensorTelemetryManager.getGyroData()
                gaugeView.setLevel(data.levelFraction, String.format(Locale.US, "%.0f°/с", data.degPerSec))
                valueView.text = data.formattedValue
                subtitleView.text = data.subtitle
                badgeView.text = "LSM6DSO • ГИРОСКОП"
                badgeView.setTextColor(Color.parseColor("#06B6D4"))
            }
            GaugeWidgetType.PROXIMITY -> {
                val data = sensorTelemetryManager.getProximityData()
                gaugeView.setLevel(data.levelFraction, if (data.isNear) "ЗАКРЫТ" else "ОТКРЫТ")
                valueView.text = data.formattedValue
                subtitleView.text = data.subtitle
                badgeView.text = "TMD2755 • ПРИБЛИЖЕНИЕ"
                badgeView.setTextColor(if (data.isNear) Color.parseColor("#E02020") else Color.parseColor("#06B6D4"))
            }
            GaugeWidgetType.MAGNETIC -> {
                val data = sensorTelemetryManager.getMagneticData()
                gaugeView.setLevel(data.levelFraction, String.format(Locale.US, "%.0f µT", data.fluxMicroTesla))
                valueView.text = data.formattedValue
                subtitleView.text = data.subtitle
                badgeView.text = "МАГНИТОМЕТР • ДАТЧИК ХОЛЛА"
                badgeView.setTextColor(Color.parseColor("#E2E4EC"))
            }
            GaugeWidgetType.BAROMETER -> {
                val data = sensorTelemetryManager.getPressureData()
                gaugeView.setLevel(data.levelFraction, String.format(Locale.US, "%.0f гПа", data.pressureHpa))
                valueView.text = data.formattedValue
                subtitleView.text = data.subtitle
                badgeView.text = "БАРОМЕТР • ДАВЛЕНИЕ"
                badgeView.setTextColor(Color.parseColor("#E2E4EC"))
            }
            GaugeWidgetType.RADIO -> {
                val data = sensorTelemetryManager.getRadioData()
                gaugeView.setLevel(data.levelFraction, "${(data.levelFraction * 100).toInt()}%")
                valueView.text = data.formattedValue
                subtitleView.text = data.subtitle
                badgeView.text = data.statusBadge
                badgeView.setTextColor(Color.parseColor("#E2E4EC"))
            }
            GaugeWidgetType.SECURITY -> {
                val data = sensorTelemetryManager.getSecurityData()
                gaugeView.setLevel(data.levelFraction, "${(data.levelFraction * 100).toInt()}%")
                valueView.text = data.formattedValue
                subtitleView.text = data.subtitle
                badgeView.text = data.statusBadge
                badgeView.setTextColor(Color.parseColor("#06B6D4"))
            }
        }
    }"""

assert re.search(dialog_and_render_pattern, code) is not None, "dialog_and_render_pattern not found"
code = re.sub(dialog_and_render_pattern, new_dialog_and_render, code, count=1)

if is_crlf:
    code = code.replace('\n', '\r\n')

with open(main_activity_path, 'w', encoding='utf-8') as f:
    f.write(code)

print("SUCCESS: MainActivity.kt updated!")
