package com.uvos.dashboard

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class FolderAppItem(
    var title: String,
    var packageName: String,
    var actionType: String = "APP",
    var iconResName: String? = null
)

data class HomeSlotItem(
    val slotIndex: Int,
    var title: String,
    var packageName: String? = null,
    var actionType: String, // SYSTEM_*, APP, EMPTY, FOLDER
    var iconResName: String? = null,
    val folderItems: MutableList<FolderAppItem> = mutableListOf()
)

class HomeManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("uvos_home_prefs", Context.MODE_PRIVATE)
    private val keySlots = "home_slots_v4"

    fun getSlots(): MutableList<HomeSlotItem> {
        val jsonStr = prefs.getString(keySlots, null)
        if (jsonStr != null) {
            try {
                val list = mutableListOf<HomeSlotItem>()
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val slot = HomeSlotItem(
                        slotIndex = obj.getInt("slotIndex"),
                        title = obj.getString("title"),
                        packageName = if (obj.has("packageName") && !obj.isNull("packageName")) obj.getString("packageName") else null,
                        actionType = obj.getString("actionType"),
                        iconResName = if (obj.has("iconResName") && !obj.isNull("iconResName")) obj.getString("iconResName") else null
                    )
                    if (obj.has("folderItems")) {
                        val fArray = obj.getJSONArray("folderItems")
                        for (j in 0 until fArray.length()) {
                            val fObj = fArray.getJSONObject(j)
                            slot.folderItems.add(
                                FolderAppItem(
                                    title = fObj.getString("title"),
                                    packageName = fObj.getString("packageName"),
                                    actionType = fObj.optString("actionType", "APP"),
                                    iconResName = if (fObj.has("iconResName") && !fObj.isNull("iconResName")) fObj.getString("iconResName") else null
                                )
                            )
                        }
                    }
                    list.add(slot)
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
            if (item.actionType == "FOLDER" && item.folderItems.isNotEmpty()) {
                val fArray = JSONArray()
                for (fItem in item.folderItems) {
                    val fObj = JSONObject()
                    fObj.put("title", fItem.title)
                    fObj.put("packageName", fItem.packageName)
                    fObj.put("actionType", fItem.actionType)
                    fObj.put("iconResName", fItem.iconResName)
                    fArray.put(fObj)
                }
                obj.put("folderItems", fArray)
            }
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
            if (actionType != "FOLDER") {
                item.folderItems.clear()
            }
            saveSlots(slots)
        }
    }

    fun clearSlot(slotIndex: Int) {
        val slots = getSlots()
        val item = slots.find { it.slotIndex == slotIndex }
        if (item != null) {
            item.title = "Добавить"
            item.packageName = null
            item.actionType = "EMPTY"
            item.iconResName = null
            item.folderItems.clear()
            saveSlots(slots)
        }
    }

    fun swapOrMoveSlot(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex == toIndex) return false
        val slots = getSlots()
        val fromItem = slots.find { it.slotIndex == fromIndex } ?: return false
        val toItem = slots.find { it.slotIndex == toIndex } ?: return false

        // Если перетаскиваем приложение на другое приложение -> автоматическое объединение в папку!
        if (fromItem.actionType == "APP" && toItem.actionType == "APP" && fromItem.packageName != null && toItem.packageName != null) {
            val folderName = "Папка"
            val items = mutableListOf(
                FolderAppItem(title = toItem.title, packageName = toItem.packageName!!, actionType = toItem.actionType, iconResName = toItem.iconResName),
                FolderAppItem(title = fromItem.title, packageName = fromItem.packageName!!, actionType = fromItem.actionType, iconResName = fromItem.iconResName)
            )
            toItem.title = folderName
            toItem.packageName = null
            toItem.actionType = "FOLDER"
            toItem.iconResName = "ic_sys_folder"
            toItem.folderItems.clear()
            toItem.folderItems.addAll(items)

            fromItem.title = "Добавить"
            fromItem.packageName = null
            fromItem.actionType = "EMPTY"
            fromItem.iconResName = null
            fromItem.folderItems.clear()

            saveSlots(slots)
            return true
        }

        // Если перетаскиваем в существующую папку -> добавляем внутрь папки!
        if (toItem.actionType == "FOLDER" && fromItem.actionType == "APP" && fromItem.packageName != null) {
            toItem.folderItems.add(
                FolderAppItem(title = fromItem.title, packageName = fromItem.packageName!!, actionType = fromItem.actionType, iconResName = fromItem.iconResName)
            )
            fromItem.title = "Добавить"
            fromItem.packageName = null
            fromItem.actionType = "EMPTY"
            fromItem.iconResName = null
            fromItem.folderItems.clear()

            saveSlots(slots)
            return true
        }

        val tempTitle = toItem.title
        val tempPkg = toItem.packageName
        val tempAction = toItem.actionType
        val tempIcon = toItem.iconResName
        val tempFolderItems = ArrayList(toItem.folderItems)

        if (toItem.actionType == "EMPTY") {
            // Перемещение в пустой слот
            toItem.title = fromItem.title
            toItem.packageName = fromItem.packageName
            toItem.actionType = fromItem.actionType
            toItem.iconResName = fromItem.iconResName
            toItem.folderItems.clear()
            toItem.folderItems.addAll(fromItem.folderItems)

            fromItem.title = "Добавить"
            fromItem.packageName = null
            fromItem.actionType = "EMPTY"
            fromItem.iconResName = null
            fromItem.folderItems.clear()
        } else {
            // Обмен местами (swap)
            toItem.title = fromItem.title
            toItem.packageName = fromItem.packageName
            toItem.actionType = fromItem.actionType
            toItem.iconResName = fromItem.iconResName
            toItem.folderItems.clear()
            toItem.folderItems.addAll(fromItem.folderItems)

            fromItem.title = tempTitle
            fromItem.packageName = tempPkg
            fromItem.actionType = tempAction
            fromItem.iconResName = tempIcon
            fromItem.folderItems.clear()
            fromItem.folderItems.addAll(tempFolderItems)
        }

        saveSlots(slots)
        return true
    }

    fun createFolderWithApp(slotIndex: Int, folderName: String, secondPkg: String, secondTitle: String) {
        val slots = getSlots()
        val slot = slots.find { it.slotIndex == slotIndex } ?: return
        val list = mutableListOf<FolderAppItem>()
        if (slot.actionType == "APP" && slot.packageName != null) {
            list.add(FolderAppItem(slot.title, slot.packageName!!, slot.actionType, slot.iconResName))
        }
        list.add(FolderAppItem(secondTitle, secondPkg, "APP", null))

        slot.title = folderName
        slot.packageName = null
        slot.actionType = "FOLDER"
        slot.iconResName = "ic_sys_folder"
        slot.folderItems.clear()
        slot.folderItems.addAll(list)
        saveSlots(slots)
    }

    fun addAppToFolder(slotIndex: Int, app: FolderAppItem) {
        val slots = getSlots()
        val slot = slots.find { it.slotIndex == slotIndex } ?: return
        if (slot.actionType == "FOLDER") {
            slot.folderItems.add(app)
            saveSlots(slots)
        }
    }

    fun removeAppFromFolder(slotIndex: Int, packageName: String) {
        val slots = getSlots()
        val slot = slots.find { it.slotIndex == slotIndex } ?: return
        if (slot.actionType == "FOLDER") {
            slot.folderItems.removeAll { it.packageName == packageName }
            if (slot.folderItems.isEmpty()) {
                clearSlot(slotIndex)
            } else if (slot.folderItems.size == 1) {
                // Если остался один элемент, превращаем обратно в обычный ярлык
                val single = slot.folderItems[0]
                slot.title = single.title
                slot.packageName = single.packageName
                slot.actionType = single.actionType
                slot.iconResName = single.iconResName
                slot.folderItems.clear()
                saveSlots(slots)
            } else {
                saveSlots(slots)
            }
        }
    }

    fun dissolveFolder(slotIndex: Int) {
        val slots = getSlots()
        val slot = slots.find { it.slotIndex == slotIndex } ?: return
        if (slot.actionType == "FOLDER") {
            if (slot.folderItems.isNotEmpty()) {
                val first = slot.folderItems[0]
                slot.title = first.title
                slot.packageName = first.packageName
                slot.actionType = first.actionType
                slot.iconResName = first.iconResName
                slot.folderItems.clear()
            } else {
                clearSlot(slotIndex)
            }
            saveSlots(slots)
        }
    }

    fun removeAppPackage(packageName: String) {
        val slots = getSlots()
        var changed = false
        slots.forEach { item ->
            if (item.packageName == packageName) {
                item.title = "Добавить"
                item.packageName = null
                item.actionType = "EMPTY"
                item.iconResName = null
                item.folderItems.clear()
                changed = true
            } else if (item.actionType == "FOLDER") {
                val before = item.folderItems.size
                item.folderItems.removeAll { it.packageName == packageName }
                if (item.folderItems.size != before) {
                    changed = true
                    if (item.folderItems.isEmpty()) {
                        item.title = "Добавить"
                        item.packageName = null
                        item.actionType = "EMPTY"
                        item.iconResName = null
                    }
                }
            }
        }
        if (changed) {
            saveSlots(slots)
        }
    }

    fun addAppToFirstFreeSlot(packageName: String, label: String): Int {
        val slots = getSlots()
        val freeSlot = slots.find { it.actionType == "EMPTY" }
        return if (freeSlot != null) {
            freeSlot.title = label
            freeSlot.packageName = packageName
            freeSlot.actionType = "APP"
            freeSlot.iconResName = null
            freeSlot.folderItems.clear()
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
