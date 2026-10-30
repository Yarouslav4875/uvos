package com.uvos.dashboard.repository

import android.content.Context
import android.content.Intent
import com.uvos.dashboard.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

class RuleRepositoryImpl private constructor(private val context: Context) : RuleRepository {

    private val storageFile = File(context.filesDir, "uvos_rules.json")
    private var activeProfile: ProfileType = ProfileType.STERILE

    private val apps = mutableListOf<AppRule>()
    private val attempts = mutableListOf<AccessAttemptEvent>()

    init {
        loadFromDisk()
        if (apps.isEmpty()) {
            initDefaultDemoApps()
            saveToDisk()
        }
        syncWithPackageManager()
    }

    fun syncWithPackageManager() {
        try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launchableApps = pm.queryIntentActivities(intent, 0)
            var changed = false
            for (resolveInfo in launchableApps) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName == context.packageName) continue
                val existing = apps.find { it.packageName == pkgName }
                if (existing == null) {
                    val label = resolveInfo.loadLabel(pm).toString()
                    val uid = resolveInfo.activityInfo.applicationInfo.uid
                    apps.add(AppRule(pkgName, label, uid))
                    changed = true
                }
            }
            if (changed) {
                saveToDisk()
            }
        } catch (_: Exception) {
            // Тихо пропускаем при ошибках IPC PackageManager
        }
    }

    private fun initDefaultDemoApps() {
        apps.add(AppRule("org.telegram.messenger", "Telegram", 10051))
        apps.add(AppRule("ru.sberbankmobile", "Банк", 10052))
        apps.add(AppRule("ru.yandex.yandexnavi", "Навигатор", 10053))
        apps.add(AppRule("org.mozilla.firefox", "Firefox", 10054))
        apps.add(AppRule("com.google.android.youtube", "Медиаплеер", 10055))

        // Инициализация демо-журнала попыток (DESIGN.md §7.4)
        val now = System.currentTimeMillis()
        attempts.add(
            AccessAttemptEvent(
                id = 1,
                timestampMillis = now - 1000 * 60 * 12,
                packageName = "org.telegram.messenger",
                appName = "Telegram",
                channel = DataChannel.MICROPHONE,
                targetResource = "AudioRecord.startRecording()",
                verdict = AccessVerdict.DENIED_CLOSED,
                isViewed = false
            )
        )
        attempts.add(
            AccessAttemptEvent(
                id = 2,
                timestampMillis = now - 1000 * 60 * 35,
                packageName = "ru.yandex.yandexnavi",
                appName = "Навигатор",
                channel = DataChannel.GEOLOCATION,
                targetResource = "LocationManager.getLastKnownLocation()",
                verdict = AccessVerdict.DENIED_CLOSED,
                isViewed = false
            )
        )
        attempts.add(
            AccessAttemptEvent(
                id = 3,
                timestampMillis = now - 1000 * 60 * 90,
                packageName = "org.mozilla.firefox",
                appName = "Firefox",
                channel = DataChannel.NETWORK,
                targetResource = "metrics.tracking.net",
                verdict = AccessVerdict.DENIED_WHITELIST,
                isViewed = true
            )
        )
    }

    override fun getActiveProfile(): ProfileType = activeProfile

    override fun setActiveProfile(profile: ProfileType) {
        activeProfile = profile
        when (profile) {
            ProfileType.STERILE -> {
                // Все 13 каналов у всех приложений закрыты
                apps.forEach { app ->
                    DataChannel.values().forEach { channel ->
                        app.channelModes[channel] = ChannelMode.CLOSED
                    }
                    app.networkConfig = app.networkConfig.copy(mode = NetworkMode.FULLY_CLOSED)
                }
            }
            ProfileType.COMM -> {
                // Каналы звонков и СМС открыты
                apps.forEach { app ->
                    DataChannel.values().forEach { channel ->
                        app.channelModes[channel] = when (channel) {
                            DataChannel.CALLS, DataChannel.SMS -> ChannelMode.OPEN
                            else -> ChannelMode.CLOSED
                        }
                    }
                }
            }
            ProfileType.CUSTOM -> {
                // Сохраняется текущая ручная конфигурация
            }
        }
        saveToDisk()
    }

    override fun getAllApps(): List<AppRule> {
        syncWithPackageManager()
        return apps
    }

    override fun getAppRule(packageName: String): AppRule? =
        apps.find { it.packageName == packageName }

    override fun setChannelMode(packageName: String, channel: DataChannel, mode: ChannelMode) {
        val app = getAppRule(packageName) ?: return
        app.channelModes[channel] = mode
        if (activeProfile != ProfileType.CUSTOM) {
            activeProfile = ProfileType.CUSTOM
        }
        saveToDisk()
    }

    override fun updateNetworkConfig(packageName: String, config: NetworkConfig) {
        val app = getAppRule(packageName) ?: return
        app.networkConfig = config
        if (activeProfile != ProfileType.CUSTOM) {
            activeProfile = ProfileType.CUSTOM
        }
        saveToDisk()
    }

    override fun getTotalOpenChannels(): Int {
        return apps.sumOf { it.openChannelsCount }
    }

    override fun getBlockedAttemptsCountToday(): Int {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return attempts.count { it.timestampMillis >= startOfDay && it.verdict.isBlocked }
    }

    override fun getAttempts(): List<AccessAttemptEvent> = attempts.sortedByDescending { it.timestampMillis }

    override fun addAttempt(event: AccessAttemptEvent) {
        attempts.add(0, event)
        saveToDisk()
    }

    override fun clearAttempts() {
        attempts.clear()
        saveToDisk()
    }

    override fun markAllAttemptsViewed() {
        attempts.forEach { it.isViewed = true }
        saveToDisk()
    }

    @Synchronized
    private fun saveToDisk() {
        try {
            val root = JSONObject()
            root.put("activeProfile", activeProfile.name)

            val appsArray = JSONArray()
            apps.forEach { app ->
                val appObj = JSONObject()
                appObj.put("packageName", app.packageName)
                appObj.put("appName", app.appName)
                appObj.put("uid", app.uid)

                val modesObj = JSONObject()
                app.channelModes.forEach { (ch, mode) ->
                    modesObj.put(ch.name, mode.name)
                }
                appObj.put("channelModes", modesObj)

                val netObj = JSONObject()
                netObj.put("mode", app.networkConfig.mode.name)
                netObj.put("dailyLimitBytes", app.networkConfig.dailyLimitBytes)
                netObj.put("usedBytesToday", app.networkConfig.usedBytesToday)
                netObj.put("chargingOnly", app.networkConfig.chargingOnly)

                val wlArray = JSONArray()
                app.networkConfig.whitelist.forEach { item ->
                    val wObj = JSONObject()
                    wObj.put("domain", item.domain)
                    wObj.put("isEnabled", item.isEnabled)
                    wlArray.put(wObj)
                }
                netObj.put("whitelist", wlArray)
                appObj.put("networkConfig", netObj)

                appsArray.put(appObj)
            }
            root.put("apps", appsArray)

            val attemptsArray = JSONArray()
            attempts.forEach { att ->
                val aObj = JSONObject()
                aObj.put("id", att.id)
                aObj.put("timestamp", att.timestampMillis)
                aObj.put("packageName", att.packageName)
                aObj.put("appName", att.appName)
                aObj.put("channel", att.channel.name)
                aObj.put("targetResource", att.targetResource)
                aObj.put("verdict", att.verdict.name)
                aObj.put("isViewed", att.isViewed)
                attemptsArray.put(aObj)
            }
            root.put("attempts", attemptsArray)

            context.openFileOutput("uvos_rules.json", Context.MODE_PRIVATE).use {
                it.write(root.toString(2).toByteArray(Charsets.UTF_8))
            }
        } catch (_: Exception) {
            // Ошибки записи обрабатываются тихо согласно политике отказоустойчивости
        }
    }

    @Synchronized
    private fun loadFromDisk() {
        if (!storageFile.exists()) return
        try {
            val content = storageFile.readText(Charsets.UTF_8)
            val root = JSONObject(content)

            if (root.has("activeProfile")) {
                activeProfile = try {
                    ProfileType.valueOf(root.getString("activeProfile"))
                } catch (_: Exception) {
                    ProfileType.STERILE
                }
            }

            if (root.has("apps")) {
                val appsArray = root.getJSONArray("apps")
                apps.clear()
                for (i in 0 until appsArray.length()) {
                    val appObj = appsArray.getJSONObject(i)
                    val pkg = appObj.getString("packageName")
                    val name = appObj.getString("appName")
                    val uid = appObj.getInt("uid")

                    val modesMap = mutableMapOf<DataChannel, ChannelMode>()
                    if (appObj.has("channelModes")) {
                        val modesObj = appObj.getJSONObject("channelModes")
                        DataChannel.values().forEach { ch ->
                            val modeStr = modesObj.optString(ch.name, ChannelMode.CLOSED.name)
                            modesMap[ch] = try {
                                ChannelMode.valueOf(modeStr)
                            } catch (_: Exception) {
                                ChannelMode.CLOSED
                            }
                        }
                    }

                    var netConfig = NetworkConfig()
                    if (appObj.has("networkConfig")) {
                        val netObj = appObj.getJSONObject("networkConfig")
                        val mode = try {
                            NetworkMode.valueOf(netObj.optString("mode", NetworkMode.FULLY_CLOSED.name))
                        } catch (_: Exception) {
                            NetworkMode.FULLY_CLOSED
                        }
                        val limit = netObj.optLong("dailyLimitBytes", 500L * 1024L * 1024L)
                        val used = netObj.optLong("usedBytesToday", 0L)
                        val charging = netObj.optBoolean("chargingOnly", false)

                        val wlList = mutableListOf<WhitelistDomain>()
                        if (netObj.has("whitelist")) {
                            val wlArr = netObj.getJSONArray("whitelist")
                            for (j in 0 until wlArr.length()) {
                                val wObj = wlArr.getJSONObject(j)
                                wlList.add(
                                    WhitelistDomain(
                                        wObj.getString("domain"),
                                        wObj.optBoolean("isEnabled", true)
                                    )
                                )
                            }
                        }
                        netConfig = NetworkConfig(
                            mode = mode,
                            whitelist = wlList,
                            dailyLimitBytes = limit,
                            usedBytesToday = used,
                            chargingOnly = charging
                        )
                    }

                    apps.add(AppRule(pkg, name, uid, modesMap, netConfig))
                }
            }

            if (root.has("attempts")) {
                val attArr = root.getJSONArray("attempts")
                attempts.clear()
                for (i in 0 until attArr.length()) {
                    val aObj = attArr.getJSONObject(i)
                    val id = aObj.getLong("id")
                    val ts = aObj.getLong("timestamp")
                    val pkg = aObj.getString("packageName")
                    val name = aObj.getString("appName")
                    val ch = try {
                        DataChannel.valueOf(aObj.getString("channel"))
                    } catch (_: Exception) {
                        DataChannel.TIME
                    }
                    val target = aObj.getString("targetResource")
                    val verdict = try {
                        AccessVerdict.valueOf(aObj.getString("verdict"))
                    } catch (_: Exception) {
                        AccessVerdict.DENIED_CLOSED
                    }
                    val viewed = aObj.optBoolean("isViewed", true)
                    attempts.add(AccessAttemptEvent(id, ts, pkg, name, ch, target, verdict, viewed))
                }
            }
        } catch (_: Exception) {
            // Повреждённый JSON — восстановление значений по умолчанию
            apps.clear()
            initDefaultDemoApps()
        }
    }

    companion object {
        @Volatile
        private var instance: RuleRepositoryImpl? = null

        fun getInstance(context: Context): RuleRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: RuleRepositoryImpl(context.applicationContext).also { instance = it }
            }
        }
    }
}
