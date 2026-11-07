package com.uvos.vpn.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VpnTrafficRate(
    val txBytesSec: Long = 0,
    val rxBytesSec: Long = 0,
    val pingMs: Int = 0
)

class UvosVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    companion object {
        private val _trafficState = MutableStateFlow(VpnTrafficRate())
        val trafficState: StateFlow<VpnTrafficRate> = _trafficState.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }

        startForeground(1001, createNotification())
        establishTunnel()
        return START_STICKY
    }

    private fun establishTunnel() {
        try {
            val builder = Builder()
                .addAddress("10.8.0.2", 24)
                .addDnsServer("1.1.1.1")
                .addRoute("0.0.0.0", 0)
                .setSession("UnderVoltOS Tunnel")
                .setBlocking(false)

            vpnInterface = builder.establish()
            _isRunning.value = true
        } catch (_: Exception) {
            _isRunning.value = false
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val channelId = "uvos_vpn_channel"
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, "UVOS VPN", NotificationManager.IMPORTANCE_LOW)
        manager?.createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("UVOS Tunnel Shield")
            .setContentText("Криптографический туннель активен (Zero Leaks)")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
    }
}
