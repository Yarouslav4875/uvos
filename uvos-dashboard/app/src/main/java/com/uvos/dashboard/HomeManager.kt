package com.uvos.dashboard

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class HomeSlotItem(
    val slotIndex: Int,
    var title: String,
    var packageName: String? = null,
    var actionType: String, // SYSTEM_*, APP, EMPTY
    var iconResName: String? = null
)

class HomeManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("uvos_home_prefs", Context.MODE_PRIVATE)
    private val keySlots = "home_slots_v3"

    fun getSlots(): MutableList<HomeSlotItem> {
        val jsonStr = prefs.getString(keySlots, null)
        if (jsonStr != null) {
            try {
                val list = mutableListOf<HomeSlotItem>()
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        HomeSlotItem(
                            slotIndex = obj.getInt("slotIndex"),
                            title = obj.getString("title"),
                            packageName = if (obj.has("packageName") && !obj.isNull("packageName")) obj.getString("packageName") else null,
                            actionType = obj.getString("actionType"),
                            iconResName = if (obj.has("iconResName") && !obj.isNull("iconResName")) obj.getString("iconResName") else null
                        )
                    )
                }
                if (list.size == 16) {
                    return list
                }
            } catch (_: Exception) {
            }
        }
        val defaultList = createDefaultSlots()
        saveSlots(defaultList)
        return defaultList
    }

    fun saveSlots(slots: List<HomeSlotItem>) {
        val array = JSONArray()
        for (item in slots) {
            val obj = JSONObject()
            obj.put("slotIndex", item.slotIndex)
            obj.put("title", item.title)
            obj.put("packageName", item.packageName)
            obj.put("actionType", item.actionType)
            obj.put("iconResName", item.iconResName)
            array.put(obj)
        }
        prefs.edit().putString(keySlots, array.toString()).apply()
    }

    fun updateSlot(slotIndex: Int, title: String, packageName: String?, actionType: String, iconResName: String?) {
        val slots = getSlots()
        val item = slots.find { it.slotIndex == slotIndex }
        if (item != null) {
            item.title = title
            item.packageName = packageName
            item.actionType = actionType
            item.iconResName = iconResName
            saveSlots(slots)
        }
    }

    fun clearSlot(slotIndex: Int) {
        updateSlot(slotIndex, "Добавить", null, "EMPTY", null)
    }

    fun addAppToFirstFreeSlot(packageName: String, label: String): Int {
        val slots = getSlots()
        val freeSlot = slots.find { it.actionType == "EMPTY" }
        return if (freeSlot != null) {
            freeSlot.title = label
            freeSlot.packageName = packageName
            freeSlot.actionType = "APP"
            freeSlot.iconResName = null
            saveSlots(slots)
            freeSlot.slotIndex
        } else {
            -1
        }
    }

    fun resetToDefaults(): MutableList<HomeSlotItem> {
        val defaultList = createDefaultSlots()
        saveSlots(defaultList)
        return defaultList
    }

    private fun createDefaultSlots(): MutableList<HomeSlotItem> {
        return mutableListOf(
            // Ряд 1: Системные инструменты
            HomeSlotItem(0, "Телефон", null, "SYSTEM_PHONE", "ic_sys_phone"),
            HomeSlotItem(1, "Сообщения", null, "SYSTEM_MESSAGES", "ic_sys_messages"),
            HomeSlotItem(2, "Радар-сеть", null, "SYSTEM_RADAR", "ic_sys_radar"),
            HomeSlotItem(3, "Камера", null, "SYSTEM_CAMERA", "ic_sys_camera"),

            // Ряд 2: Системные утилиты
            HomeSlotItem(4, "Файлы", null, "SYSTEM_FILES", "ic_sys_files"),
            HomeSlotItem(5, "Плеер", null, "SYSTEM_PLAYER", "ic_sys_player"),
            HomeSlotItem(6, "Счётчик", null, "SYSTEM_CALC", "ic_sys_calc"),
            HomeSlotItem(7, "Настройки", null, "SYSTEM_SETTINGS", "ic_sys_settings"),

            // Ряд 3: Нативные приложения
            HomeSlotItem(8, "Telegram", "org.telegram.messenger", "APP", null),
            HomeSlotItem(9, "Карты", "ru.yandex.yandexnavi", "APP", null),
            HomeSlotItem(10, "Банк", "ru.sberbankmobile", "APP", null),
            HomeSlotItem(11, "Щиток UVOS", null, "SYSTEM_SHIELD", "ic_sys_shield"),

            // Ряд 4: Инженерный блок
            HomeSlotItem(12, "Журнал", null, "SYSTEM_LOG", "ic_sys_messages"),
            HomeSlotItem(13, "Осциллограф", null, "SYSTEM_OSC", "ic_sys_oscilloscope"),
            HomeSlotItem(14, "Терминал", null, "SYSTEM_TERM", "ic_sys_terminal"),
            HomeSlotItem(15, "Пакеты", null, "SYSTEM_PACKAGES", "ic_sys_packages")
        )
    }
}
