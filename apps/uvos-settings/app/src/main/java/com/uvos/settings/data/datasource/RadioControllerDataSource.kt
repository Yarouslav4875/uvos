package com.uvos.settings.data.datasource

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.hardware.SensorPrivacyManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings
import com.topjohnwu.superuser.Shell
import com.uvos.settings.domain.model.SystemProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RadioControllerDataSource(
    private val context: Context,
    private val wifiManager: WifiManager,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val nfcAdapter: NfcAdapter?,
    private val sensorPrivacyManager: SensorPrivacyManager?
) {
    suspend fun applyProfile(profile: SystemProfile): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            when (profile) {
                SystemProfile.STERILE -> executeSterilePolicy()
                SystemProfile.COMMUNICATION -> executeCommunicationPolicy()
                SystemProfile.CUSTOM -> executeCustomPolicy()
            }
        }
    }

    private fun executeSterilePolicy() {
        // 1. Авиарежим и отключение радиомодулей
        try {
            Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
        } catch (_: Exception) {}

        try {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = false
        } catch (_: Exception) {}

        try {
            @Suppress("DEPRECATION")
            bluetoothAdapter?.disable()
        } catch (_: Exception) {}

        // 2. Блокировка сенсоров (Камера, Микрофон) через SensorPrivacy API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            sensorPrivacyManager?.let { spm ->
                try {
                    if (spm.supportsSensorToggle(SensorPrivacyManager.Sensors.CAMERA)) {
                        spm.setSensorPrivacy(SensorPrivacyManager.Sensors.CAMERA, true)
                    }
                    if (spm.supportsSensorToggle(SensorPrivacyManager.Sensors.MICROPHONE)) {
                        spm.setSensorPrivacy(SensorPrivacyManager.Sensors.MICROPHONE, true)
                    }
                } catch (_: Exception) {}
            }
        }

        // 3. Ядерный файрвол: DROP ALL
        runRootCommands(
            "iptables -F OUTPUT",
            "iptables -P OUTPUT DROP",
            "iptables -F INPUT",
            "iptables -P INPUT DROP",
            "ip link set wlan0 down"
        )
    }

    private fun executeCommunicationPolicy() {
        // Выключение авиарежима для RIL
        try {
            Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        } catch (_: Exception) {}

        try {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = false
        } catch (_: Exception) {}

        try {
            @Suppress("DEPRECATION")
            bluetoothAdapter?.disable()
        } catch (_: Exception) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            sensorPrivacyManager?.let { spm ->
                try {
                    // Разрешить микрофон для голосовых вызовов, заглушить камеру
                    if (spm.supportsSensorToggle(SensorPrivacyManager.Sensors.CAMERA)) {
                        spm.setSensorPrivacy(SensorPrivacyManager.Sensors.CAMERA, true)
                    }
                    if (spm.supportsSensorToggle(SensorPrivacyManager.Sensors.MICROPHONE)) {
                        spm.setSensorPrivacy(SensorPrivacyManager.Sensors.MICROPHONE, false)
                    }
                } catch (_: Exception) {}
            }
        }

        // iptables: разрешить только RIL (uid 1001) и DNS
        runRootCommands(
            "iptables -F OUTPUT",
            "iptables -P OUTPUT DROP",
            "iptables -A OUTPUT -m owner --uid-owner 1001 -j ACCEPT",
            "iptables -A OUTPUT -p udp --dport 53 -j ACCEPT",
            "iptables -P INPUT ACCEPT"
        )
    }

    private fun executeCustomPolicy() {
        try {
            Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        } catch (_: Exception) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            sensorPrivacyManager?.let { spm ->
                try {
                    if (spm.supportsSensorToggle(SensorPrivacyManager.Sensors.CAMERA)) {
                        spm.setSensorPrivacy(SensorPrivacyManager.Sensors.CAMERA, false)
                    }
                    if (spm.supportsSensorToggle(SensorPrivacyManager.Sensors.MICROPHONE)) {
                        spm.setSensorPrivacy(SensorPrivacyManager.Sensors.MICROPHONE, false)
                    }
                } catch (_: Exception) {}
            }
        }

        // Сброс жестких блокировок
        runRootCommands(
            "iptables -P OUTPUT ACCEPT",
            "iptables -P INPUT ACCEPT"
        )
    }

    private fun runRootCommands(vararg commands: String) {
        try {
            if (Shell.isAppGrantedRoot() == true) {
                Shell.cmd(*commands).exec()
            }
        } catch (_: Exception) {
            // Root unavailable or command failed gracefully
        }
    }
}
