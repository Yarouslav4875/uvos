package com.uvos.dashboard

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.os.StatFs
import org.json.JSONArray
import org.json.JSONObject

enum class GaugeWidgetType {
    STORAGE,
    THERMAL,
    SECURITY,
    RADIO
}

data class CustomGaugeItem(
    val id: String,
    val type: GaugeWidgetType,
    val title: String,
    val gaugeTitle: String
)

class GaugeWidgetManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("uvos_gauges_prefs", Context.MODE_PRIVATE)
    private val keyGauges = "custom_gauges_list_v1"

    fun getGauges(): MutableList<CustomGaugeItem> {
        val jsonStr = prefs.getString(keyGauges, null)
        if (jsonStr != null) {
            try {
                val list = mutableListOf<CustomGaugeItem>()
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        CustomGaugeItem(
                            id = obj.getString("id"),
                            type = GaugeWidgetType.valueOf(obj.getString("type")),
                            title = obj.getString("title"),
                            gaugeTitle = obj.getString("gaugeTitle")
                        )
                    )
                }
                return list
            } catch (_: Exception) {
            }
        }
        return mutableListOf()
    }

    fun saveGauges(list: List<CustomGaugeItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("type", item.type.name)
            obj.put("title", item.title)
            obj.put("gaugeTitle", item.gaugeTitle)
            array.put(obj)
        }
        prefs.edit().putString(keyGauges, array.toString()).apply()
    }

    fun addGauge(type: GaugeWidgetType): CustomGaugeItem {
        val list = getGauges()
        val item = when (type) {
            GaugeWidgetType.STORAGE -> CustomGaugeItem("gauge_storage_${System.currentTimeMillis()}", type, "STORAGE FLASH (НАКОПИТЕЛЬ)", "ROM")
            GaugeWidgetType.THERMAL -> CustomGaugeItem("gauge_thermal_${System.currentTimeMillis()}", type, "THERMAL MONITOR (ТЕМПЕРАТУРА)", "TEMP")
            GaugeWidgetType.SECURITY -> CustomGaugeItem("gauge_security_${System.currentTimeMillis()}", type, "SECURITY CHANNELS (ИЗОЛЯЦИЯ)", "SEC")
            GaugeWidgetType.RADIO -> CustomGaugeItem("gauge_radio_${System.currentTimeMillis()}", type, "RADIO RF RSSI (РАДИОКАНАЛ)", "RF")
        }
        list.add(item)
        saveGauges(list)
        return item
    }

    fun removeGauge(id: String) {
        val list = getGauges()
        list.removeAll { it.id == id }
        saveGauges(list)
    }

    /**
     * Считывает реальную статистику хранилища
     */
    fun getStorageStats(): StorageStats {
        return try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            val blockSize = statFs.blockSizeLong
            val totalBlocks = statFs.blockCountLong
            val availableBlocks = statFs.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            val totalGb = totalBytes / (1024f * 1024f * 1024f)
            val usedGb = usedBytes / (1024f * 1024f * 1024f)
            val freeGb = freeBytes / (1024f * 1024f * 1024f)

            val fraction = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0.5f
            val percent = (fraction * 100f).toInt()

            StorageStats(
                fraction = fraction,
                percent = percent,
                usedText = String.format("%.1f ГБ / %.0f ГБ", usedGb, totalGb),
                subtitle = String.format("Свободно %.1f ГБ (%d%%)", freeGb, 100 - percent)
            )
        } catch (_: Exception) {
            StorageStats(0.45f, 45, "58.4 ГБ / 256 ГБ", "Свободно 197.6 ГБ (55%)")
        }
    }

    data class StorageStats(
        val fraction: Float,
        val percent: Int,
        val usedText: String,
        val subtitle: String
    )
}
