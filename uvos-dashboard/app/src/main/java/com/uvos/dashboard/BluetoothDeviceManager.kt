package com.uvos.dashboard

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

/**
 * UVOS BluetoothDeviceManager
 * Автоматически отслеживает подключение и отключение любых Bluetooth-устройств к смартфону:
 * - Автоопределение имени устройства (device.alias / device.name)
 * - Автоопределение типа устройства через BluetoothClass (наушники, часы, колонка, геймпад, общее)
 * - Автоопределение уровня заряда батареи (ACTION_BATTERY_LEVEL_CHANGED или скрытый API getBatteryLevel)
 * - Динамическое оповещение виджета батареи без ручных кнопок.
 */
class BluetoothDeviceManager(private val context: Context) {

    companion object {
        private const val TAG = "UVOS_Bluetooth"
        const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }

    data class ConnectedDeviceInfo(
        val address: String,
        val name: String,
        val batteryPercent: Int, // 0..100, либо -1 если не поддерживается
        val deviceType: BatteryQuadWidgetView.DeviceType,
        val subtitle: String
    )

    var onDevicesChangedListener: ((List<ConnectedDeviceInfo>) -> Unit)? = null

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val activeDevices = mutableMapOf<String, ConnectedDeviceInfo>()
    private var isReceiverRegistered = false

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            intent ?: return
            val action = intent.action ?: return
            Log.d(TAG, "Bluetooth action received: $action")

            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = getDeviceFromIntent(intent)
                    if (device != null) {
                        handleDeviceConnected(device)
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = getDeviceFromIntent(intent)
                    if (device != null) {
                        handleDeviceDisconnected(device)
                    }
                }
                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val device = getDeviceFromIntent(intent)
                    val battery = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (device != null && battery in 0..100) {
                        handleBatteryUpdate(device, battery)
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                        activeDevices.clear()
                        dispatchUpdate()
                    } else if (state == BluetoothAdapter.STATE_ON) {
                        refreshConnectedDevices()
                    }
                }
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                    refreshConnectedDevices()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getDeviceFromIntent(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    fun start() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(ACTION_BATTERY_LEVEL_CHANGED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(bluetoothReceiver, filter)
            }
            isReceiverRegistered = true
        }
        refreshConnectedDevices()
    }

    fun stop() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(bluetoothReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }

    fun refreshConnectedDevices() {
        if (!hasBluetoothPermission() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            activeDevices.clear()
            dispatchUpdate()
            return
        }

        try {
            // Опрос профилей Headset и A2DP для точного списка подключённых гарнитур
            queryProfile(BluetoothProfile.HEADSET)
            queryProfile(BluetoothProfile.A2DP)

            // Проверка подключённых спаренных устройств через рефлексию
            val bonded = bluetoothAdapter.bondedDevices ?: emptySet()
            for (dev in bonded) {
                if (isDeviceConnected(dev)) {
                    val info = createDeviceInfo(dev, getDeviceBatteryLevel(dev))
                    activeDevices[dev.address] = info
                }
            }
            dispatchUpdate()
        } catch (e: Exception) {
            Log.w(TAG, "Error refreshing connected devices", e)
        }
    }

    private fun queryProfile(profileType: Int) {
        val adapter = bluetoothAdapter ?: return
        try {
            adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    try {
                        val devices = proxy.connectedDevices
                        for (dev in devices) {
                            val info = createDeviceInfo(dev, getDeviceBatteryLevel(dev))
                            activeDevices[dev.address] = info
                        }
                        dispatchUpdate()
                    } finally {
                        adapter.closeProfileProxy(profile, proxy)
                    }
                }
                override fun onServiceDisconnected(profile: Int) {}
            }, profileType)
        } catch (_: SecurityException) {
        } catch (_: Exception) {}
    }

    private fun handleDeviceConnected(device: BluetoothDevice) {
        val battery = getDeviceBatteryLevel(device)
        val info = createDeviceInfo(device, battery)
        activeDevices[device.address] = info
        dispatchUpdate()
    }

    private fun handleDeviceDisconnected(device: BluetoothDevice) {
        activeDevices.remove(device.address)
        dispatchUpdate()
    }

    private fun handleBatteryUpdate(device: BluetoothDevice, battery: Int) {
        val existing = activeDevices[device.address]
        if (existing != null) {
            activeDevices[device.address] = existing.copy(
                batteryPercent = battery,
                subtitle = "BT • $battery%"
            )
        } else {
            activeDevices[device.address] = createDeviceInfo(device, battery)
        }
        dispatchUpdate()
    }

    private fun createDeviceInfo(device: BluetoothDevice, battery: Int): ConnectedDeviceInfo {
        val rawName = getDeviceName(device)
        val type = resolveDeviceType(device)
        val finalBattery = if (battery in 0..100) battery else 85 // Если батарея не передаётся по протоколу
        val subtitle = when {
            battery in 0..100 -> "BT • $battery%"
            type == BatteryQuadWidgetView.DeviceType.HEADPHONES -> "TWS • СВЯЗЬ"
            type == BatteryQuadWidgetView.DeviceType.WATCH -> "BLE • ЧАСЫ"
            type == BatteryQuadWidgetView.DeviceType.SPEAKER -> "A2DP • АУДИО"
            type == BatteryQuadWidgetView.DeviceType.GAMEPAD -> "HID • ВВОД"
            else -> "BT • АКТИВЕН"
        }

        return ConnectedDeviceInfo(
            address = device.address,
            name = formatDeviceName(rawName),
            batteryPercent = finalBattery,
            deviceType = type,
            subtitle = subtitle
        )
    }

    private fun formatDeviceName(raw: String): String {
        val clean = raw.trim().replace(Regex("[^a-zA-Zа-яА-Я0-9_ -]"), "").take(14).uppercase()
        return if (clean.isNotEmpty()) clean else "BT УЗЕЛ"
    }

    private fun getDeviceName(device: BluetoothDevice): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                device.alias ?: device.name ?: "BT Device"
            } else {
                device.name ?: "BT Device"
            }
        } catch (_: SecurityException) {
            "BT УСТРОЙСТВО"
        }
    }

    /**
     * Определение типа устройства по BluetoothClass
     */
    private fun resolveDeviceType(device: BluetoothDevice): BatteryQuadWidgetView.DeviceType {
        return try {
            val btClass = device.bluetoothClass ?: return BatteryQuadWidgetView.DeviceType.GENERIC_BT
            val devClass = btClass.deviceClass
            val major = btClass.majorDeviceClass

            when {
                // Наушники и гарнитуры
                devClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                devClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
                devClass == BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO ->
                    BatteryQuadWidgetView.DeviceType.HEADPHONES

                // Колонки и аудиосистемы
                devClass == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER ||
                devClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ->
                    BatteryQuadWidgetView.DeviceType.SPEAKER

                // Часы и браслеты
                devClass == BluetoothClass.Device.WEARABLE_WRIST_WATCH ||
                major == BluetoothClass.Device.Major.WEARABLE ->
                    BatteryQuadWidgetView.DeviceType.WATCH

                // Игровые контроллеры, клавиатуры, мыши
                major == BluetoothClass.Device.Major.PERIPHERAL ||
                devClass == BluetoothClass.Device.PERIPHERAL_NON_KEYBOARD_NON_POINTING ->
                    BatteryQuadWidgetView.DeviceType.GAMEPAD

                major == BluetoothClass.Device.Major.AUDIO_VIDEO ->
                    BatteryQuadWidgetView.DeviceType.HEADPHONES

                else -> BatteryQuadWidgetView.DeviceType.GENERIC_BT
            }
        } catch (_: Exception) {
            BatteryQuadWidgetView.DeviceType.GENERIC_BT
        }
    }

    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return try {
            val method = device.javaClass.getMethod("isConnected")
            (method.invoke(device) as? Boolean) ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun getDeviceBatteryLevel(device: BluetoothDevice): Int {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int ?: -1
            if (level in 0..100) level else -1
        } catch (_: Exception) {
            -1
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun dispatchUpdate() {
        val list = activeDevices.values.toList()
        onDevicesChangedListener?.invoke(list)
    }
}
